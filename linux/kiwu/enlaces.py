"""Vista previa de enlaces (páginas web y vídeos de YouTube/Vimeo). Sin dependencias externas.

Solo se conecta a Internet cuando el usuario añade un enlace a una nota. El resultado tiene el mismo
formato que `LinkPreview` en Kiwu para macOS.
"""

from __future__ import annotations

import base64
import json
import re
import urllib.parse
import urllib.request
from html.parser import HTMLParser
from typing import Callable, Optional

AGENTE = ("Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0")
_ID_YOUTUBE = re.compile(r"^[A-Za-z0-9_-]{6,15}$")


def url_desde_texto(texto: str) -> Optional[str]:
    """Normaliza lo escrito por el usuario a una URL http(s); None si no parece una dirección."""
    t = (texto or "").strip()
    if not t or any(c.isspace() for c in t):
        return None
    if "://" not in t:
        t = "https://" + t
    p = urllib.parse.urlparse(t)
    if p.scheme not in ("http", "https") or not p.hostname or "." not in p.hostname:
        return None
    return t


def parece_enlace_suelto(texto: str) -> bool:
    """Un párrafo que es solo una dirección web (se convierte en tarjeta al pulsar Intro)."""
    t = (texto or "").strip()
    return t.startswith(("http://", "https://", "www.")) and url_desde_texto(t) is not None


def video_youtube(url: str) -> Optional[str]:
    p = urllib.parse.urlparse(url)
    host = (p.hostname or "").lower().removeprefix("www.").removeprefix("m.")
    partes = [x for x in p.path.split("/") if x]
    id_: Optional[str] = None
    if host == "youtu.be":
        id_ = partes[0] if partes else None
    elif host.endswith("youtube.com") or host.endswith("youtube-nocookie.com"):
        consulta = urllib.parse.parse_qs(p.query)
        if "v" in consulta:
            id_ = consulta["v"][0]
        elif len(partes) >= 2 and partes[0] in ("shorts", "embed", "live", "v"):
            id_ = partes[1]
    return id_ if id_ and _ID_YOUTUBE.match(id_) else None


def video_vimeo(url: str) -> Optional[str]:
    p = urllib.parse.urlparse(url)
    if not (p.hostname or "").lower().endswith("vimeo.com"):
        return None
    numeros = [x for x in p.path.split("/") if x.isdigit()]
    return numeros[-1] if numeros else None


class _Meta(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.meta: dict[str, str] = {}
        self.titulo = ""
        self._en_titulo = False

    def handle_starttag(self, tag, attrs):
        if tag == "title":
            self._en_titulo = True
        elif tag == "meta":
            a = {k.lower(): (v or "") for k, v in attrs}
            clave = (a.get("property") or a.get("name") or "").lower()
            if clave and a.get("content") and clave not in self.meta:
                self.meta[clave] = a["content"].strip()

    def handle_endtag(self, tag):
        if tag == "title":
            self._en_titulo = False

    def handle_data(self, data):
        if self._en_titulo:
            self.titulo += data


def analizar_html(html: str, url_base: str) -> dict:
    """Extrae título, descripción, sitio e imagen (Open Graph / Twitter / etiquetas básicas)."""
    p = _Meta()
    try:
        p.feed(html[:400_000])
    except Exception:  # HTML roto: nos quedamos con lo que se haya leído
        pass
    m = p.meta
    titulo = m.get("og:title") or m.get("twitter:title") or p.titulo.strip() or None
    descripcion = m.get("og:description") or m.get("twitter:description") or m.get("description")
    sitio = m.get("og:site_name") or (urllib.parse.urlparse(url_base).hostname or "").removeprefix("www.") or None
    imagen = m.get("og:image") or m.get("twitter:image")
    if imagen:
        imagen = urllib.parse.urljoin(url_base, imagen)
    return {"titulo": titulo, "descripcion": descripcion, "sitio": sitio, "imagen_url": imagen}


def _descargar(url: str, max_bytes: int = 1_000_000, timeout: float = 12.0) -> Optional[bytes]:
    peticion = urllib.request.Request(url, headers={"User-Agent": AGENTE, "Accept-Language": "es,en;q=0.8"})
    try:
        with urllib.request.urlopen(peticion, timeout=timeout) as r:  # noqa: S310 (solo http/https, ver url_desde_texto)
            datos = r.read(max_bytes + 1)
    except Exception:
        return None
    return datos if len(datos) <= max_bytes else None


def obtener(url: str, reducir: Optional[Callable[[bytes], Optional[bytes]]] = None,
            descargar: Callable[..., Optional[bytes]] = _descargar) -> Optional[dict]:
    """Devuelve la vista previa (claves: titulo, descripcion, sitio, imagen, proveedor, videoID) o None.

    `reducir` recibe los bytes de la imagen y devuelve una versión pequeña (en la interfaz se usa GdkPixbuf).
    `descargar` se puede sustituir en las pruebas.
    """
    def imagen_de(direccion: Optional[str]) -> Optional[str]:
        if not direccion:
            return None
        datos = descargar(direccion, 5_000_000)
        if not datos:
            return None
        if reducir:
            datos = reducir(datos)
        return base64.b64encode(datos).decode("ascii") if datos else None

    codificada = urllib.parse.quote(url, safe="")
    yt = video_youtube(url)
    if yt:
        vp: dict = {"sitio": "YouTube", "proveedor": "youtube", "videoID": yt}
        crudo = descargar(f"https://www.youtube.com/oembed?format=json&url={codificada}")
        if crudo:
            try:
                j = json.loads(crudo)
                vp["titulo"], vp["descripcion"] = j.get("title"), j.get("author_name")
            except ValueError:
                pass
        for calidad in ("maxresdefault", "hqdefault"):
            img = imagen_de(f"https://img.youtube.com/vi/{yt}/{calidad}.jpg")
            if img:
                vp["imagen"] = img
                break
        return {k: v for k, v in vp.items() if v}

    vm = video_vimeo(url)
    if vm:
        vp = {"sitio": "Vimeo", "proveedor": "vimeo", "videoID": vm}
        crudo = descargar(f"https://vimeo.com/api/oembed.json?url={codificada}")
        if crudo:
            try:
                j = json.loads(crudo)
                vp["titulo"], vp["descripcion"] = j.get("title"), j.get("author_name")
                vp["imagen"] = imagen_de(j.get("thumbnail_url"))
            except ValueError:
                pass
        return {k: v for k, v in vp.items() if v}

    crudo = descargar(url, 1_500_000)
    if not crudo:
        return None
    info = analizar_html(crudo.decode("utf-8", errors="replace"), url)
    if not (info["titulo"] or info["descripcion"]):
        return None
    resultado = {"titulo": info["titulo"], "descripcion": info["descripcion"], "sitio": info["sitio"],
                 "imagen": imagen_de(info["imagen_url"])}
    return {k: v for k, v in resultado.items() if v}


def url_incrustada(proveedor: str, id_: str) -> str:
    """Dirección del reproductor incrustado."""
    if proveedor == "youtube":
        return f"https://www.youtube.com/embed/{id_}?autoplay=1&rel=0&playsinline=1"
    return f"https://player.vimeo.com/video/{id_}?autoplay=1"
