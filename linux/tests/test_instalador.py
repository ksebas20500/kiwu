"""Pruebas de los scripts de instalación y empaquetado de Linux (se ejecutan en cualquier sistema con bash)."""

import os
import subprocess
import tarfile
import tempfile
import unittest
from pathlib import Path

LINUX = Path(__file__).resolve().parents[1]
APP_ID = "io.github.ksebas20500.Kiwu"


def bash(*args, **kw):
    return subprocess.run(["bash", *map(str, args)], capture_output=True, text=True, **kw)


class PruebasInstalador(unittest.TestCase):
    def test_instalar_desde_el_repositorio_y_desinstalar(self):
        with tempfile.TemporaryDirectory() as tmp:
            r = bash(LINUX / "install.sh", "--forzar", "--prefix", tmp)
            self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
            lanzador = Path(tmp) / "bin" / "kiwu"
            self.assertTrue(os.access(lanzador, os.X_OK))
            texto = lanzador.read_text()
            self.assertIn(str(Path(tmp) / "share/kiwu/app"), texto)
            self.assertIn("python3 -m kiwu", texto)
            self.assertTrue((Path(tmp) / "share/kiwu/app/kiwu/__init__.py").exists())
            self.assertFalse(list((Path(tmp) / "share/kiwu/app").rglob("__pycache__")))
            desktop = (Path(tmp) / f"share/applications/{APP_ID}.desktop").read_text()
            self.assertIn(f"Exec={lanzador}\n", desktop)
            self.assertIn(f"Exec={lanzador} --nueva-tarea", desktop)
            self.assertTrue((Path(tmp) / f"share/icons/hicolor/256x256/apps/{APP_ID}.png").exists())
            self.assertTrue((Path(tmp) / f"share/metainfo/{APP_ID}.metainfo.xml").exists())

            r = bash(LINUX / "install.sh", "--forzar", "--prefix", tmp, "--desinstalar")
            self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
            self.assertFalse(lanzador.exists())
            self.assertFalse((Path(tmp) / "share/kiwu").exists())

    def test_reinstalar_sustituye_la_version_anterior(self):
        with tempfile.TemporaryDirectory() as tmp:
            bash(LINUX / "install.sh", "--forzar", "--prefix", tmp)
            sobrante = Path(tmp) / "share/kiwu/app/kiwu/modulo_viejo.py"
            sobrante.write_text("x = 1")
            bash(LINUX / "install.sh", "--forzar", "--prefix", tmp)
            self.assertFalse(sobrante.exists())

    def test_opcion_desconocida(self):
        r = bash(LINUX / "install.sh", "--loquesea")
        self.assertNotEqual(r.returncode, 0)

    def test_paquete_y_su_instalador(self):
        r = bash(LINUX / "Tools/crear-paquete.sh")
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        paquete = LINUX / "dist/kiwu-linux.tar.gz"
        self.assertTrue(paquete.exists())
        suma = (LINUX / "dist/kiwu-linux.tar.gz.sha256").read_text().split()[0]
        self.assertEqual(len(suma), 64)
        with tempfile.TemporaryDirectory() as tmp:
            with tarfile.open(paquete) as t:
                nombres = t.getnames()
                t.extractall(tmp)
            raiz = nombres[0].split("/")[0]
            self.assertTrue(raiz.startswith("kiwu-linux-"))
            for esperado in ("install.sh", "kiwu/__init__.py", "kiwu/recursos/esquemas/kiwu-dark.xml",
                             f"data/{APP_ID}.desktop", "LICENSE", "PKGBUILD", "README.md"):
                self.assertIn(f"{raiz}/{esperado}", nombres)
            self.assertFalse([n for n in nombres if "__pycache__" in n or ".DS_Store" in n or "/._" in n])
            # El instalador del paquete funciona por sí solo (sin el repositorio)
            destino = Path(tmp) / "inst"
            r = bash(Path(tmp) / raiz / "install.sh", "--forzar", "--prefix", destino)
            self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
            self.assertTrue((destino / "bin/kiwu").exists())


if __name__ == "__main__":
    unittest.main()
