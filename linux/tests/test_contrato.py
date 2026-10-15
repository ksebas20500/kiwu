"""Contrato de datos: las fixtures de `docs/contrato/notas` deben leerse igual en todos los clientes.

El equivalente para macOS es `macos/Tools/probar-contrato.sh`.
"""

import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from kiwu.modelos import Documento  # noqa: E402

CARPETA = Path(__file__).resolve().parents[2] / "docs" / "contrato" / "notas"


def resumen(doc: Documento) -> dict:
    return {
        "texto_plano": doc.texto_plano(),
        "tipos": [b.kind for b in doc.bloques],
        "sangrias": [b.indent for b in doc.bloques],
    }


class PruebasContrato(unittest.TestCase):
    def fixtures(self):
        archivos = sorted(p for p in CARPETA.glob("*.json") if not p.name.endswith(".esperado.json"))
        self.assertTrue(archivos, f"No hay fixtures en {CARPETA}")
        return archivos

    def test_cada_fixture_se_lee_como_se_espera(self):
        for ruta in self.fixtures():
            with self.subTest(fixture=ruta.name):
                esperado = json.loads(ruta.with_suffix(".esperado.json").read_text(encoding="utf-8"))
                doc = Documento.de_json(ruta.read_text(encoding="utf-8"))
                self.assertEqual(resumen(doc), esperado)

    def test_escribir_y_volver_a_leer_no_cambia_nada(self):
        for ruta in self.fixtures():
            with self.subTest(fixture=ruta.name):
                doc = Documento.de_json(ruta.read_text(encoding="utf-8"))
                vuelta = Documento.de_json(doc.a_json())
                self.assertEqual(resumen(vuelta), resumen(doc))

    def test_cada_fixture_tiene_su_resultado_esperado(self):
        for ruta in self.fixtures():
            self.assertTrue(ruta.with_suffix(".esperado.json").exists(), ruta.name)


if __name__ == "__main__":
    unittest.main()
