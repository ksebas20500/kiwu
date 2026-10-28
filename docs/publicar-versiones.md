# Cómo se publican las versiones

macOS, Linux y Android tienen **instaladores y numeración independientes**, pero comparten repositorio y la página de [Releases](https://github.com/ksebas20500/kiwu/releases). Para que no se pisen:

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

## Publicar Android

Android tiene dos canales con el mismo `versionName` y distinto `versionCode` (en `android/app/build.gradle.kts`):

| | APK de GitHub | Google Play |
| --- | --- | --- |
| Rama | `main` | `anuncios-prueba`, en la carpeta hermana `kiwu-play` (un `git worktree` del mismo repositorio) |
| Qué añade | — | anuncios (AdMob), compra «Quitar anuncios» y aviso de versión nueva |
| `versionCode` | 1, 2, 3… (0.4.3 = 7) | 7, 8, 9… (0.4.3 = 12); nunca se reutiliza uno subido a Play |
| Etiqueta y archivo | `android-v0.4.3` con `kiwu-android-0.4.3.apk` y `SHA256SUMS.txt` | AAB firmado con la clave de subida |

Pasos de una versión:

1. Terminar y probar en `main`: `./gradlew testDebugUnitTest lintDebug assembleDebug`, subir `versionCode`/`versionName`, escribir `docs/versiones/android-X.Y.Z.md` y hacer el commit.
2. En `kiwu-play`: `git merge main`. Los conflictos habituales están en `build.gradle.kts` (conservar el `targetSdk` y el `versionCode` de Play), `AjustesScreen.kt` y `ResumenAjustes.kt` (conservar «Quitar anuncios» y «Buscar actualizaciones»). Los textos solo de Play van en `res/values*/strings_play.xml`, en los tres idiomas.
3. En `kiwu-play/android`: `./gradlew testDebugUnitTest lintDebug bundleRelease` y copiar `app/build/outputs/bundle/release/app-release.aab` a `Documents/kiwu-aab/kiwu-X.Y.Z-codigoN.aab`.
4. Subir el AAB en Play Console (prueba cerrada) con la nota de novedades de `docs/play-store.md` (rama de Play) y hacer el commit del merge.

### Anuncios reales (AdMob)

Los IDs reales no van en el repositorio: están en `~/kiwu-claves/admob.properties` (o en la carpeta de `KIWU_CLAVES`), junto a `keystore.properties`:

```
appId=ca-app-pub-6922015805223220~9431045068
banner=ca-app-pub-6922015805223220/4342157887
```

- Solo el build **release** (`bundleRelease`) los usa; depuración y minificado llevan siempre los anuncios de prueba de Google. Sin el archivo, también salen anuncios de prueba. Nunca pulses anuncios reales con tu propia cuenta.
- `docs/app-ads.txt` (editor de AdMob: `pub-6922015805223220`) debe servirse en la **raíz** del dominio que figura como sitio web en Play Console, es decir `https://kiwu.web.app/app-ads.txt`, y no en una subcarpeta. La web vive en el repo `ksebas20500/kiwu-web` (Astro + Firebase Hosting): la copia está en `public/app-ads.txt` y se despliega con `npm run build && firebase deploy --only hosting`. Si cambia una, cambia la otra. Luego, en AdMob → *App settings* → *app-ads.txt*, comprobar que se detecta (puede tardar hasta 24 h).
- La app aparece como «Requires review» hasta que esté publicada en Play y enlazada en *App settings*.

## Para la futura web estática

La página de descargas puede leer `https://api.github.com/repos/ksebas20500/kiwu/releases`, agrupar por prefijo de etiqueta (`v`/`macos-v` → macOS, `linux-v` → Linux) y enlazar al archivo de cada versión. Para un botón de «última versión»:

- macOS: `https://github.com/ksebas20500/kiwu/releases/latest/download/Kiwu.dmg`
- Linux: `https://github.com/ksebas20500/kiwu/releases/download/<etiqueta linux más reciente>/kiwu-linux.tar.gz`
