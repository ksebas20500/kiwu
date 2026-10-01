"""Pruebas de la lógica (sin interfaz): se pueden ejecutar en cualquier sistema con
`python3 -m unittest discover -s tests`."""

import json
import sqlite3
import sys
import tempfile
import time
import unittest
import uuid
from datetime import datetime, timedelta
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from kiwu import enlaces, importador, lenguajes, recordatorios  # noqa: E402
from kiwu.ajustes import ATAJOS_PREDETERMINADOS, Ajustes  # noqa: E402
from kiwu.db import BaseDeDatos  # noqa: E402
from kiwu.modelos import Bloque, Documento, TextRun, runs_o_nada  # noqa: E402
from kiwu.servicios import (COMPLETADAS, HOY, PROXIMOS, TODOS, Columna, Servicio, columna_de,  # noqa: E402
                            fin_del_dia, inicio_del_dia, materia_item)

# Esquema real de los almacenes de Core Data de Kiwu para macOS
ESQUEMA_COREDATA = """
CREATE TABLE ZCUADERNO ( Z_PK INTEGER PRIMARY KEY, Z_ENT INTEGER, Z_OPT INTEGER, ZORDEN INTEGER, ZFECHAACTUALIZACION TIMESTAMP, ZFECHACREACION TIMESTAMP, ZICONO VARCHAR, ZNOMBRE VARCHAR, ZID BLOB );
CREATE TABLE ZDEBER ( Z_PK INTEGER PRIMARY KEY, Z_ENT INTEGER, Z_OPT INTEGER, ZCOMPLETADO INTEGER, ZESTADO INTEGER, ZRECORDATORIOANTESENHORAS INTEGER, ZMATERIA INTEGER, ZFECHAACTUALIZACION TIMESTAMP, ZFECHACOMPLETADO TIMESTAMP, ZFECHACREACION TIMESTAMP, ZFECHAENTREGA TIMESTAMP, ZTITULO VARCHAR, ZID BLOB, ZCONTENIDONOTA BLOB );
CREATE TABLE ZMATERIA ( Z_PK INTEGER PRIMARY KEY, Z_ENT INTEGER, Z_OPT INTEGER, ZCARPETADISPONIBLE INTEGER, ZORDEN INTEGER, ZFECHAACTUALIZACION TIMESTAMP, ZFECHACREACION TIMESTAMP, ZICONO VARCHAR, ZNOMBRE VARCHAR, ZID BLOB, ZBOOKMARKCARPETA BLOB );
CREATE TABLE ZPAGINA ( Z_PK INTEGER PRIMARY KEY, Z_ENT INTEGER, Z_OPT INTEGER, ZFAVORITA INTEGER, ZCUADERNO INTEGER, ZFECHAACTUALIZACION TIMESTAMP, ZFECHACREACION TIMESTAMP, ZICONO VARCHAR, ZTEXTOPLANO VARCHAR, ZTITULO VARCHAR, ZID BLOB, ZCONTENIDO BLOB );
"""

NOTA_MACOS = (
    '{"blocks":[{"id":"6F1E6C0A-1111-4222-8333-444455556666","kind":"paragraph","text":"Hola mundo",'
    '"indent":0,"isChecked":false,"runs":[{"text":"Hola ","bold":false,"italic":false,"underline":false,'
    '"strike":false,"code":false},{"text":"mundo","bold":true,"italic":false,"underline":false,"strike":false,'
    '"code":false,"color":"rojo"}]}]}'
)


class PruebasModelos(unittest.TestCase):
    def test_nota_antigua_sin_campos_nuevos(self):
        viejo = '{"blocks":[{"id":"6F1E6C0A-1111-4222-8333-444455556666","kind":"paragraph","text":"hola","isChecked":false}]}'
        doc = Documento.de_json(viejo)
        self.assertEqual(doc.bloques[0].text, "hola")
        self.assertEqual(doc.bloques[0].indent, 0)

    def test_ida_y_vuelta_conserva_formato_y_campos_desconocidos(self):
        original = json.loads(NOTA_MACOS)
        original["blocks"][0]["campoFuturo"] = {"x": 1}
        doc = Documento.de_json(json.dumps(original))
        vuelta = json.loads(doc.a_json())
        self.assertEqual(vuelta["blocks"][0]["campoFuturo"], {"x": 1})
        self.assertTrue(vuelta["blocks"][0]["runs"][1]["bold"])
        self.assertEqual(vuelta["blocks"][0]["runs"][1]["color"], "rojo")

    def test_json_invalido_da_documento_vacio(self):
        for malo in ("", "no es json", "[1,2]", "null", b"\xff\xfe"):
            self.assertEqual(len(Documento.de_json(malo).bloques), 1)

    def test_runs_se_fusionan_y_sin_formato_es_none(self):
        self.assertIsNone(runs_o_nada([TextRun("a"), TextRun("b")]))
        runs = runs_o_nada([TextRun("a", bold=True), TextRun("b", bold=True), TextRun("c")])
        self.assertEqual([(r.text, r.bold) for r in runs], [("ab", True), ("c", False)])

    def test_todas_las_claves_booleanas_de_textrun_estan_presentes(self):
        # Swift exige estas claves al decodificar TextRun
        d = TextRun("x").a_json()
        for clave in ("text", "bold", "italic", "underline", "strike", "code"):
            self.assertIn(clave, d)


class PruebasBaseDeDatos(unittest.TestCase):
    def setUp(self):
        self.db = BaseDeDatos(":memory:")
        self.s = Servicio(self.db)
        self.m = self.db.crear_materia("Universidad")

    def test_crear_y_listar(self):
        d = self.s.crear("Estudiar", self.m.id)
        self.assertEqual([x.titulo for x in self.db.deberes()], ["Estudiar"])
        self.assertEqual(columna_de(d), Columna.POR_HACER)

    def test_eliminar_materia_borra_sus_tareas(self):
        self.s.crear("A", self.m.id)
        self.db.eliminar_materia(self.m.id)
        self.assertEqual(self.db.deberes(), [])

    def test_campos_invalidos_se_rechazan(self):
        d = self.s.crear("A", self.m.id)
        with self.assertRaises(ValueError):
            self.db.actualizar_deber(d.id, titulo_falso="x")

    def test_observadores(self):
        eventos = []
        self.db.suscribir(eventos.append)
        d = self.s.crear("A", self.m.id)
        self.db.actualizar_deber(d.id, silencioso=True, nota="{}")
        self.db.actualizar_deber(d.id, titulo="B")
        self.assertEqual(eventos, ["deber", "deber"])

    def test_paginas_y_cuadernos(self):
        c = self.db.crear_cuaderno("Notas rápidas")
        p = self.db.crear_pagina(c.id)
        self.db.actualizar_pagina(p.id, titulo="Hola", favorita=True)
        self.assertTrue(self.db.pagina(p.id).favorita)
        self.db.eliminar_cuaderno(c.id)
        self.assertEqual(self.db.paginas(), [])


class PruebasServicio(unittest.TestCase):
    def setUp(self):
        self.db = BaseDeDatos(":memory:")
        self.s = Servicio(self.db)
        self.m = self.db.crear_materia("Uni")
        self.otra = self.db.crear_materia("Casa")
        # fecha fija: miércoles 30 de septiembre de 2026, 12:00 locales
        self.ahora = datetime(2026, 9, 30, 12, 0).timestamp()

    def en(self, dias=0, horas=0):
        return (datetime(2026, 9, 30, 12, 0) + timedelta(days=dias, hours=horas)).timestamp()

    def test_hoy_incluye_vencidas_y_hoy_pero_no_manana_ni_sin_fecha(self):
        self.s.crear("vencida", self.m.id, self.en(-3))
        self.s.crear("hoy", self.m.id, self.en(0, 5))
        self.s.crear("mañana", self.m.id, self.en(1))
        self.s.crear("sin fecha", self.m.id)
        self.assertEqual({d.titulo for d in self.s.pendientes(HOY, self.ahora)}, {"vencida", "hoy"})

    def test_proximos_7_dias(self):
        self.s.crear("vencida", self.m.id, self.en(-1))
        self.s.crear("hoy", self.m.id, self.en(0))
        self.s.crear("dia6", self.m.id, self.en(6, 3))
        self.s.crear("dia7", self.m.id, self.en(7))
        self.assertEqual({d.titulo for d in self.s.pendientes(PROXIMOS, self.ahora)}, {"hoy", "dia6"})

    def test_orden_por_fecha_y_sin_fecha_al_final(self):
        self.s.crear("c", self.m.id)
        self.s.crear("b", self.m.id, self.en(2))
        self.s.crear("a", self.m.id, self.en(1))
        self.assertEqual([d.titulo for d in self.s.pendientes(TODOS, self.ahora)], ["a", "b", "c"])

    def test_flujo_de_columnas(self):
        d = self.s.crear("x", self.m.id)
        self.s.mover(self.db.deber(d.id), Columna.EN_CURSO)
        d = self.db.deber(d.id)
        self.assertEqual(columna_de(d), Columna.EN_CURSO)
        self.s.mover(d, Columna.HECHO)
        d = self.db.deber(d.id)
        self.assertEqual(columna_de(d), Columna.HECHO)
        self.assertIsNotNone(d.fecha_completado)
        # volver de Hecho a Por hacer
        self.s.mover(d, Columna.POR_HACER)
        d = self.db.deber(d.id)
        self.assertEqual(columna_de(d), Columna.POR_HACER)
        self.assertIsNone(d.fecha_completado)
        # y de En curso de nuevo a Por hacer
        self.s.mover(d, Columna.EN_CURSO)
        self.s.mover(self.db.deber(d.id), Columna.POR_HACER)
        self.assertEqual(columna_de(self.db.deber(d.id)), Columna.POR_HACER)

    def test_marcar_y_desmarcar_conserva_en_curso(self):
        d = self.s.crear("x", self.m.id)
        self.s.mover(d, Columna.EN_CURSO)
        d = self.db.deber(d.id)
        self.s.marcar(d, True)
        d = self.db.deber(d.id)
        self.assertEqual(columna_de(d), Columna.HECHO)
        self.s.marcar(d, False)
        self.assertEqual(columna_de(self.db.deber(d.id)), Columna.EN_CURSO)

    def test_completadas_y_hoy(self):
        a = self.s.crear("a", self.m.id)
        self.s.marcar(a, True)
        self.assertEqual(len(self.s.completados(COMPLETADAS)), 1)
        self.assertEqual(len(self.s.completados(HOY)), 1)       # completada hoy
        self.assertEqual(len(self.s.completados(PROXIMOS)), 0)
        self.assertEqual(self.s.conteo(COMPLETADAS), 0)

    def test_por_materia_y_conteo(self):
        self.s.crear("a", self.m.id)
        self.s.crear("b", self.otra.id)
        self.assertEqual(self.s.conteo(materia_item(self.m.id)), 1)
        self.assertEqual(self.s.conteo(TODOS), 2)

    def test_del_dia(self):
        self.s.crear("a", self.m.id, self.en(0, 3))
        self.s.crear("b", self.m.id, self.en(1))
        self.assertEqual([d.titulo for d in self.s.del_dia(self.ahora and datetime(2026, 9, 30).date())], ["a"])

    def test_fin_del_dia(self):
        f = fin_del_dia(self.ahora)
        self.assertEqual(datetime.fromtimestamp(f).strftime("%H:%M:%S"), "23:59:59")
        self.assertEqual(datetime.fromtimestamp(inicio_del_dia(self.ahora)).strftime("%H:%M"), "00:00")


class PruebasRecordatorios(unittest.TestCase):
    def setUp(self):
        self.db = BaseDeDatos(":memory:")
        self.s = Servicio(self.db)
        self.m = self.db.crear_materia("Uni")
        self.t0 = 1_800_000_000.0

    def test_avisa_en_la_ventana_correcta_y_solo_una_vez(self):
        d = self.s.crear("entrega", self.m.id, self.t0 + 30 * 3600)   # vence en 30 h, aviso 24 h antes
        self.assertEqual(recordatorios.pendientes_de_aviso(self.db, self.t0), [])               # faltan 30 h
        self.assertEqual(len(recordatorios.pendientes_de_aviso(self.db, self.t0 + 7 * 3600)), 1)  # faltan 23 h
        recordatorios.marcar_avisado(self.db, d, self.t0 + 7 * 3600)
        self.assertEqual(recordatorios.pendientes_de_aviso(self.db, self.t0 + 8 * 3600), [])

    def test_no_avisa_de_completadas_ni_muy_vencidas(self):
        d = self.s.crear("vieja", self.m.id, self.t0)
        self.assertEqual(recordatorios.pendientes_de_aviso(self.db, self.t0 + 3 * 86400), [])
        self.s.marcar(d, True)
        self.assertEqual(recordatorios.pendientes_de_aviso(self.db, self.t0 - 3600), [])

    def test_cambiar_la_fecha_rearma_el_aviso(self):
        d = self.s.crear("x", self.m.id, self.t0 + 3600)
        recordatorios.marcar_avisado(self.db, d, self.t0)
        self.s.poner_fecha(self.db.deber(d.id), self.t0 + 5 * 3600)
        self.assertEqual(len(recordatorios.pendientes_de_aviso(self.db, self.t0 + 60)), 1)

    def test_texto(self):
        d = self.s.crear("x", self.m.id, self.t0 + 5 * 3600)
        self.assertEqual(recordatorios.texto_aviso(d, self.t0), "Vence en 5 h")
        self.assertEqual(recordatorios.texto_aviso(d, self.t0 + 4.5 * 3600), "Vence en 30 min")
        self.assertEqual(recordatorios.texto_aviso(d, self.t0 + 5 * 3600 + 600), "Venció hace 10 min")


class PruebasImportacion(unittest.TestCase):
    def _crear_coredata(self, ruta: Path):
        con = sqlite3.connect(ruta)
        con.executescript(ESQUEMA_COREDATA)
        return con

    def test_importar_bases_de_macos(self):
        with tempfile.TemporaryDirectory() as tmp:
            tmp = Path(tmp)
            ref = 978307200.0
            id_m, id_d1, id_d2, id_c, id_p = (uuid.uuid4() for _ in range(5))
            t = self._crear_coredata(tmp / "GestorUniversitario.sqlite")
            t.execute("INSERT INTO ZMATERIA (Z_PK, ZORDEN, ZFECHACREACION, ZFECHAACTUALIZACION, ZNOMBRE, ZID) VALUES (1, 0, ?, ?, 'Universidad', ?)",
                      (1000.0, 2000.0, id_m.bytes))
            fecha_entrega = datetime(2026, 10, 3, 18, 0).timestamp() - ref
            t.execute("INSERT INTO ZDEBER (Z_PK, ZCOMPLETADO, ZESTADO, ZRECORDATORIOANTESENHORAS, ZMATERIA, ZFECHAENTREGA, ZFECHACREACION, ZFECHAACTUALIZACION, ZTITULO, ZID, ZCONTENIDONOTA) VALUES (1, 0, 1, 24, 1, ?, 1000, 2000, 'Quiz', ?, ?)",
                      (fecha_entrega, id_d1.bytes, NOTA_MACOS.encode()))
            # tarea antigua: ZESTADO nulo (antes de añadir el tablero) y completada
            t.execute("INSERT INTO ZDEBER (Z_PK, ZCOMPLETADO, ZESTADO, ZMATERIA, ZFECHACOMPLETADO, ZFECHACREACION, ZTITULO, ZID, ZCONTENIDONOTA) VALUES (2, 1, NULL, 1, 3000, 1000, 'Vieja', ?, NULL)",
                      (id_d2.bytes,))
            # tarea huérfana (materia inexistente): se ignora
            t.execute("INSERT INTO ZDEBER (Z_PK, ZMATERIA, ZTITULO, ZID) VALUES (3, 99, 'Huerfana', ?)", (uuid.uuid4().bytes,))
            t.commit(); t.close()
            n = self._crear_coredata(tmp / "Notas.sqlite")
            n.execute("INSERT INTO ZCUADERNO (Z_PK, ZORDEN, ZFECHACREACION, ZNOMBRE, ZICONO, ZID) VALUES (1, 0, 1000, 'Notas rápidas', '📝', ?)", (id_c.bytes,))
            n.execute("INSERT INTO ZPAGINA (Z_PK, ZCUADERNO, ZFAVORITA, ZTITULO, ZTEXTOPLANO, ZFECHACREACION, ZID, ZCONTENIDO) VALUES (1, 1, 1, 'Hola', 'Hola mundo', 1000, ?, ?)",
                      (id_p.bytes, NOTA_MACOS.encode()))
            n.commit(); n.close()

            db = BaseDeDatos(":memory:")
            resumen = importador.importar_macos(db, tmp / "GestorUniversitario.sqlite", tmp / "Notas.sqlite")
            self.assertEqual(resumen, {"materias": 1, "deberes": 2, "cuadernos": 1, "paginas": 1})

            d1 = db.deber(str(id_d1).upper())
            self.assertEqual(d1.titulo, "Quiz")
            self.assertEqual(d1.estado, 1)
            self.assertAlmostEqual(d1.fecha_entrega, datetime(2026, 10, 3, 18, 0).timestamp(), places=3)
            self.assertEqual(Documento.de_json(d1.nota).bloques[0].runs[1].color, "rojo")
            d2 = db.deber(str(id_d2).upper())
            self.assertTrue(d2.completado)
            self.assertEqual(d2.estado, 0)
            self.assertEqual(len(Documento.de_json(d2.nota).bloques), 1)   # nota nula -> documento vacío
            p = db.pagina(str(id_p).upper())
            self.assertTrue(p.favorita)
            self.assertEqual(db.cuaderno(str(id_c).upper()).icono, "📝")

            # Importar dos veces no duplica
            again = importador.importar_macos(db, tmp / "GestorUniversitario.sqlite", tmp / "Notas.sqlite")
            self.assertEqual(sum(again.values()), 0)

    def test_archivo_inexistente(self):
        with self.assertRaises(importador.ErrorImportacion):
            importador.importar_macos(BaseDeDatos(":memory:"), "/no/existe.sqlite")

    def test_copia_json_ida_y_vuelta(self):
        origen = BaseDeDatos(":memory:")
        s = Servicio(origen)
        m = origen.crear_materia("Uni")
        d = s.crear("Quiz", m.id, time.time() + 86400)
        origen.actualizar_deber(d.id, nota=Documento.de_json(NOTA_MACOS).a_json())
        s.mover(origen.deber(d.id), Columna.EN_CURSO)
        c = origen.crear_cuaderno("Cuaderno")
        p = origen.crear_pagina(c.id)
        origen.actualizar_pagina(p.id, titulo="T", contenido=Documento.de_json(NOTA_MACOS).a_json(), favorita=True)
        with tempfile.TemporaryDirectory() as tmp:
            ruta = Path(tmp) / "copia.kiwu.json"
            importador.exportar_copia(origen, ruta)
            destino = BaseDeDatos(":memory:")
            resumen = importador.importar_copia(destino, ruta)
        self.assertEqual(resumen, {"materias": 1, "deberes": 1, "cuadernos": 1, "paginas": 1})
        d2 = destino.deber(d.id)
        self.assertEqual((d2.titulo, d2.estado), ("Quiz", 1))
        self.assertAlmostEqual(d2.fecha_entrega, origen.deber(d.id).fecha_entrega, delta=1)
        self.assertEqual(Documento.de_json(d2.nota).bloques[0].runs[1].bold, True)
        self.assertTrue(destino.pagina(p.id).favorita)

    def test_copia_que_no_es_de_kiwu(self):
        with tempfile.TemporaryDirectory() as tmp:
            ruta = Path(tmp) / "x.json"
            ruta.write_text('{"otra": 1}')
            with self.assertRaises(importador.ErrorImportacion):
                importador.importar_copia(BaseDeDatos(":memory:"), ruta)


class PruebasEnlaces(unittest.TestCase):
    def test_url_desde_texto(self):
        self.assertEqual(enlaces.url_desde_texto("example.com/a"), "https://example.com/a")
        self.assertIsNone(enlaces.url_desde_texto("hola mundo"))
        self.assertIsNone(enlaces.url_desde_texto("ftp://example.com"))
        self.assertIsNone(enlaces.url_desde_texto("localhost"))

    def test_enlace_suelto(self):
        self.assertTrue(enlaces.parece_enlace_suelto("https://example.com"))
        self.assertTrue(enlaces.parece_enlace_suelto("www.example.com"))
        self.assertFalse(enlaces.parece_enlace_suelto("hola.mundo"))

    def test_videos(self):
        for url in ("https://www.youtube.com/watch?v=dQw4w9WgXcQ", "https://youtu.be/dQw4w9WgXcQ?t=3",
                    "https://www.youtube.com/shorts/dQw4w9WgXcQ", "https://m.youtube.com/embed/dQw4w9WgXcQ"):
            self.assertEqual(enlaces.video_youtube(url), "dQw4w9WgXcQ", url)
        self.assertIsNone(enlaces.video_youtube("https://example.com/watch?v=dQw4w9WgXcQ"))
        self.assertEqual(enlaces.video_vimeo("https://vimeo.com/76979871"), "76979871")

    def test_analizar_html(self):
        html = ('<html><head><title>Título viejo</title><meta property="og:title" content="Título OG &amp; más">'
                '<meta name="description" content="Descripción"><meta property="og:image" content="/img/a.png"></head></html>')
        info = enlaces.analizar_html(html, "https://www.sitio.com/pagina")
        self.assertEqual(info["titulo"], "Título OG & más")
        self.assertEqual(info["descripcion"], "Descripción")
        self.assertEqual(info["sitio"], "sitio.com")
        self.assertEqual(info["imagen_url"], "https://www.sitio.com/img/a.png")

    def test_obtener_web_con_descarga_simulada(self):
        paginas = {
            "https://sitio.com/": b'<title>Hola</title><meta property="og:image" content="https://sitio.com/i.png">',
            "https://sitio.com/i.png": b"PNGDATA",
        }
        vp = enlaces.obtener("https://sitio.com/", descargar=lambda u, m=0: paginas.get(u), reducir=lambda b: b[:3])
        self.assertEqual(vp["titulo"], "Hola")
        self.assertEqual(vp["imagen"], "UE5H")   # base64 de «PNG»

    def test_obtener_youtube_con_descarga_simulada(self):
        def falsa(url, m=0):
            if "oembed" in url:
                return json.dumps({"title": "Un vídeo", "author_name": "Canal"}).encode()
            if "maxresdefault" in url:
                return None                      # no existe: se usa hqdefault
            return b"IMG"
        vp = enlaces.obtener("https://youtu.be/dQw4w9WgXcQ", descargar=falsa)
        self.assertEqual((vp["proveedor"], vp["videoID"], vp["titulo"]), ("youtube", "dQw4w9WgXcQ", "Un vídeo"))
        self.assertIn("imagen", vp)

    def test_pagina_sin_datos_devuelve_none(self):
        self.assertIsNone(enlaces.obtener("https://sitio.com/", descargar=lambda u, m=0: b"<html></html>"))
        self.assertIsNone(enlaces.obtener("https://sitio.com/", descargar=lambda u, m=0: None))


class PruebasLenguajesYAjustes(unittest.TestCase):
    def test_deteccion(self):
        casos = {
            "import SwiftUI\nstruct A: View {}": "swift", "def f(x):\n    return x": "python", '{"a": 1}': "json",
            "SELECT * FROM t WHERE a=1": "sql", "const a = () => 1": "javascript", "#include <stdio.h>": "c",
            "<div>hola</div>": "html", "texto cualquiera": "plano", "": "plano",
            "sudo pacman -Syu": "bash", "fn main() { let mut x = 1; }": "rust",
        }
        for codigo, esperado in casos.items():
            self.assertEqual(lenguajes.detectar(codigo), esperado, codigo)

    def test_ajustes_persisten_y_toleran_archivos_malos(self):
        with tempfile.TemporaryDirectory() as tmp:
            ruta = Path(tmp) / "s.json"
            ruta.write_text("{ esto no es json")
            a = Ajustes(ruta)
            self.assertEqual(a["tema"], "sistema")
            a["tema"] = "oscuro"
            a.fijar_atajo("nueva-tarea", "<Control><Shift>n")
            b = Ajustes(ruta)
            self.assertEqual(b["tema"], "oscuro")
            self.assertEqual(b.atajo("nueva-tarea"), "<Control><Shift>n")
            self.assertEqual(b.atajo("ir-notas"), ATAJOS_PREDETERMINADOS["ir-notas"])
            self.assertEqual(b.conflicto("<Control>2", excepto="nueva-tarea"), "ir-notas")
            b.restaurar_atajos()
            self.assertEqual(b.atajo("nueva-tarea"), ATAJOS_PREDETERMINADOS["nueva-tarea"])
            with self.assertRaises(KeyError):
                b["inventada"] = 1


if __name__ == "__main__":
    unittest.main()
