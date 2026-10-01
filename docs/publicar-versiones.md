# Cómo se publican las versiones

macOS y Linux tienen **instaladores y numeración independientes**, pero comparten repositorio y la página de [Releases](https://github.com/ksebas20500/kiwu/releases). Para que no se pisen:

| | macOS | Linux |
| --- | --- | --- |
| Carpeta | `macos/` | `linux/` |
| Etiqueta | `v2.0.0`, `v2.1.0`… (las nuevas pueden llamarse `macos-v2.1.0`; ambas se reconocen) | `linux-v0.1.0`, `linux-v0.2.0`… |
| Archivos del release | `Kiwu.dmg` + `Kiwu.dmg.sha256` | `kiwu-linux.tar.gz` + `kiwu-linux.tar.gz.sha256` |
| Se genera con | `macos/Tools/crear-instalador.sh` | `linux/Tools/crear-paquete.sh` |
| Instalador por terminal | `macos/install.sh` | `linux/install.sh` |
| Versión en el código | `MARKETING_VERSION` en `macos/Kiwu.xcodeproj` | `VERSION` en `linux/kiwu/__init__.py` (y `linux/pyproject.toml`) |

> Los nombres de los archivos son **fijos** (sin la versión dentro). Así los enlaces `…/releases/latest/download/Kiwu.dmg` y los instaladores siguen funcionando con cada versión nueva.

## La regla importante: qué es «latest»

GitHub llama «latest» al release más reciente. Para que el enlace `releases/latest/download/Kiwu.dmg` del README siga apuntando a macOS:

- los releases de **macOS** se crean con `--latest`;
- los releases de **Linux** se crean con `--latest=false`.

Los instaladores por terminal no dependen de «latest»: buscan la etiqueta más reciente de su plataforma (`v…`/`macos-v…` o `linux-v…`).

## Publicar macOS

```bash
cd macos
Tools/crear-instalador.sh                     # genera dist/Kiwu.dmg y su .sha256
gh release create v2.1.0 dist/Kiwu.dmg dist/Kiwu.dmg.sha256 \
  --repo ksebas20500/kiwu --target main --latest --title "Kiwu 2.1" --notes-file notas.md
```

## Publicar Linux

```bash
cd linux
python3 -m unittest discover -s tests         # las pruebas deben pasar
Tools/crear-paquete.sh                        # genera dist/kiwu-linux.tar.gz y su .sha256
gh release create linux-v0.1.0 dist/kiwu-linux.tar.gz dist/kiwu-linux.tar.gz.sha256 \
  --repo ksebas20500/kiwu --target main --latest=false --title "Kiwu para Linux 0.1.0" --notes-file notas.md
```

Después, actualiza en `linux/PKGBUILD` la suma de verificación (`sha256sums`) del archivo de código de la etiqueta.

## Para la futura web estática

La página de descargas puede leer `https://api.github.com/repos/ksebas20500/kiwu/releases`, agrupar por prefijo de etiqueta (`v`/`macos-v` → macOS, `linux-v` → Linux) y enlazar al archivo de cada versión. Para un botón de «última versión»:

- macOS: `https://github.com/ksebas20500/kiwu/releases/latest/download/Kiwu.dmg`
- Linux: `https://github.com/ksebas20500/kiwu/releases/download/<etiqueta linux más reciente>/kiwu-linux.tar.gz`
