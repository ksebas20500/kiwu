<p align="center">
  <img src="docs/media/icono.png" width="128" alt="Icono de Kiwu">
</p>

<h1 align="center">Kiwu</h1>

<p align="center">
  <b>Tus tareas, tus notas y tu horario en un solo lugar.</b><br>
  Para Android, macOS y Linux. Tus datos se quedan contigo: sin cuentas y sin nube.
</p>

<p align="center">
  <a href="https://github.com/ksebas20500/kiwu/releases/download/android-v0.3.2/kiwu-android-0.3.2.apk"><b>⬇️ Descargar para Android</b></a>
  &nbsp;·&nbsp;
  <a href="https://github.com/ksebas20500/kiwu/releases/latest/download/Kiwu.dmg"><b>🍎 Descargar para macOS</b></a>
  &nbsp;·&nbsp;
  <a href="#linux"><b>🐧 Instalar en Linux</b></a>
  &nbsp;·&nbsp;
  <a href="https://github.com/ksebas20500/kiwu/releases">Todas las versiones</a>
  &nbsp;·&nbsp;
  <a href="https://kiwu.web.app/">🌐 Sitio web</a>
</p>

<p align="center">
  <a href="https://github.com/ksebas20500/kiwu/releases/latest"><img alt="Última versión" src="https://img.shields.io/github/v/release/ksebas20500/kiwu?label=versi%C3%B3n&color=E8705A"></a>
  <img alt="Android 8 o superior" src="https://img.shields.io/badge/Android-8%2B-3DDC84?logo=android&logoColor=white">
  <img alt="macOS 13 o superior" src="https://img.shields.io/badge/macOS-13%2B-555?logo=apple&logoColor=white">
  <img alt="Linux con GTK4" src="https://img.shields.io/badge/Linux-GTK4%20%C2%B7%20libadwaita-4A86CF?logo=linux&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Swift" src="https://img.shields.io/badge/Swift-SwiftUI-F05138?logo=swift&logoColor=white">
  <img alt="Python" src="https://img.shields.io/badge/Python-3-3776AB?logo=python&logoColor=white">
  <a href="LICENSE"><img alt="Licencia MIT" src="https://img.shields.io/badge/licencia-MIT-2EA44F"></a>
</p>

<p align="center">
  <img src="docs/media/tablero_custom.png" alt="Kiwu con un fondo personalizado, en modo tablero" width="860">
</p>

<p align="center">
  <img src="docs/media/demo.gif" alt="Demostración de Kiwu" width="860">
</p>

## En tu móvil

<p align="center">
  <img src="docs/media/android/tareas.png" alt="Lista de tareas en Android" width="30%">
  <img src="docs/media/android/calendario.png" alt="Calendario en Android" width="30%">
  <img src="docs/media/android/notas.png" alt="Editor de notas en Android" width="30%">
</p>
<p align="center">
  <img src="docs/media/android/horario-dia.png" alt="Horario por día en Android" width="30%">
  <img src="docs/media/android/horario-semana.png" alt="Horario por semana en Android" width="30%">
  <img src="docs/media/android/ajustes.png" alt="Ajustes en Android" width="30%">
</p>

Kiwu para Android es una app nativa, rápida y pensada para el día a día de un estudiante: apuntas una tarea en segundos, miras tus entregas en el calendario, escribes notas con bloques y consultas tu horario por día o por semana. Todo funciona sin conexión y sin crear ninguna cuenta.

> Creado por **Kevin Sebastián Medina Nava**. Antes se llamaba *TaskFlow*; si vienes de la versión 1 de macOS, mira [Actualizar desde TaskFlow](macos/README.md#actualizar-desde-taskflow-versión-1x).

## Descargas e instalación

Cada sistema tiene su propio instalador y su propia numeración de versiones (más detalles en [docs/publicar-versiones.md](docs/publicar-versiones.md)). Puedes instalar de **dos formas**: con un comando en la terminal o descargando el archivo y haciéndolo a mano.

| Sistema | Archivo para instalar a mano | Más información |
| --- | --- | --- |
| 🍎 **macOS** 13 o superior | [`Kiwu.dmg`](https://github.com/ksebas20500/kiwu/releases/latest/download/Kiwu.dmg) (universal: Apple Silicon e Intel) | [macos/README.md](macos/README.md) |
| 🤖 **Android** 8 o superior | [`kiwu-android-0.3.2.apk`](https://github.com/ksebas20500/kiwu/releases/download/android-v0.3.2/kiwu-android-0.3.2.apk) | [android/README.md](android/README.md) |
| 🐧 **Linux** (Arch + Hyprland / Wayland) | `kiwu-linux.tar.gz`, en los [releases `linux-v…`](https://github.com/ksebas20500/kiwu/releases?q=linux-v) | [linux/README.md](linux/README.md) |

Descarga la última versión desde [Releases](https://github.com/ksebas20500/kiwu/releases/latest). Cada versión conserva su propio archivo en la [lista de versiones](https://github.com/ksebas20500/kiwu/releases).

### 🤖 Android

1. Descarga [`kiwu-android-0.3.2.apk`](https://github.com/ksebas20500/kiwu/releases/download/android-v0.3.2/kiwu-android-0.3.2.apk) desde el teléfono (o pásalo desde el ordenador).
2. Ábrelo. Si Android te lo pide, permite **instalar apps de esta fuente** solo para esa vez.
3. Abre **Kiwu** y empieza por crear tu primera lista (la materia o el trabajo de tus tareas).

Es una versión de prueba (`0.3.2`), así que puede cambiar con el tiempo. Si quieres comprobar la descarga, la lista de sumas de verificación está en [`SHA256SUMS.txt`](https://github.com/ksebas20500/kiwu/releases/download/android-v0.3.2/SHA256SUMS.txt) del mismo release. Los avisos de tareas y clases funcionan mejor si permites las notificaciones y las **alarmas exactas** cuando la app te lo pida.

El release trae también `kiwu-android-0.3.2-debug.apk`, que es solo para desarrollo y va más lento: usa siempre el primero. Para compilarlo tú mismo, mira [android/README.md](android/README.md).

### 🍎 macOS

**Opción 1: un solo comando** (recomendada; comprueba la descarga y deja la app lista para abrir)

```bash
curl -fsSL https://raw.githubusercontent.com/ksebas20500/kiwu/main/install.sh | bash
```

El mismo comando sirve en cualquier sistema: detecta macOS o Linux y ejecuta el instalador que le corresponde.

**Opción 2: a mano.** Descarga `Kiwu.dmg`, ábrelo y arrastra **Kiwu** a *Aplicaciones*.
La primera vez, macOS avisará de que no puede verificar al desarrollador (la app no está notarizada por Apple, porque eso requiere una cuenta de pago). Para abrirla:

- *Ajustes del Sistema → Privacidad y seguridad → Abrir igualmente*, o
- en la Terminal: `xattr -dr com.apple.quarantine /Applications/Kiwu.app`

El instalador es **universal** (Apple Silicon e Intel), pesa unos pocos MB y necesita macOS 13 o superior (widgets: macOS 14 o superior).

Si prefieres no fiarte de un binario, compílalo tú mismo o revisa cómo se genera en [`macos/Tools/crear-instalador.sh`](macos/Tools/crear-instalador.sh).

### 🐧 Linux

Necesitas `python-gobject`, `gtk4`, `libadwaita` y `gtksourceview5` (en Arch: `sudo pacman -S python python-gobject gtk4 libadwaita gtksourceview5`).

**Opción 1: un solo comando** (recomendada; comprueba la descarga, instala en tu carpeta personal sin `sudo` y crea la entrada en el menú de aplicaciones)

```bash
curl -fsSL https://raw.githubusercontent.com/ksebas20500/kiwu/main/install.sh | bash
```

**Opción 2: a mano.** Descarga `kiwu-linux.tar.gz` de [Releases](https://github.com/ksebas20500/kiwu/releases?q=linux-v), descomprímelo y ejecuta su instalador:

```bash
tar -xzf kiwu-linux.tar.gz
cd kiwu-linux-*
./install.sh
```

También puedes comprobar antes la descarga con el archivo `kiwu-linux.tar.gz.sha256` (`sha256sum -c kiwu-linux.tar.gz.sha256`). Para desinstalar: `./install.sh --desinstalar` (no borra tus datos). En Arch también puedes usar el `PKGBUILD`; los detalles, la configuración de Hyprland y las opciones del instalador están en [linux/README.md](linux/README.md).

## Capturas de escritorio

| | |
| --- | --- |
| ![Lista](docs/media/portada.png) | ![Tablero](docs/media/tablero.png) |
| **Lista** · vistas Hoy, Próximos 7 días y por materia | **Tablero** · Por hacer, En curso y Hecho, con arrastrar y soltar |
| ![Notas](docs/media/notas.png) | ![Calendario](docs/media/calendario.png) |
| **Notas** · editor por bloques con menú `/`, formato, código con colores y enlaces con vista previa | **Calendario** · tus entregas del mes |
| ![Ajustes](docs/media/ajustes.png) | |
| **Ajustes** · tema, atajos editables y fondo translúcido | |

**Widgets de escritorio**

| | |
| --- | --- |
| ![Widget Deberes](docs/media/widgets_deberes.png) | ![Widget Calendario](docs/media/widgets_calendario.png) |
| **Deberes** · se completan desde el widget | **Calendario de deberes** · el mes y las próximas entregas |


## Qué incluye

| Módulo | Qué ofrece |
| --- | --- |
| **Tareas** | Listas por materia, deberes con fecha y hora, recordatorios locales, vistas *Hoy* / *Próximos 7 días* / *Todas* / *Completadas*, en **lista o tablero** (Por hacer · En curso · Hecho) |
| **Calendario** | Mes completo con un punto de color por entrega y, debajo, el detalle del día para añadir tareas con hora |
| **Notas** | Cuadernos, páginas con emoji, favoritas, búsqueda (sin distinguir mayúsculas ni tildes) y editor por bloques con menú `/` |
| **Horario** (macOS y Android) | Varios horarios (Universidad, Trabajo, Ocio…), clases en varios días, vista de semana y de día, «Ahora / Siguiente», aviso de choques, avisos opcionales antes de cada clase y, en Android, faltas por materia con un máximo y cuántas quedan |
| **Archivos** | Carpeta de tu equipo vinculada a cada lista con explorador integrado, y PDF/Word adjuntos a notas como referencia, sin duplicarlos |
| **Ajustes** | Atajos de teclado editables, tema claro/oscuro, color de la app a elegir y fondo de imagen con translucidez ajustable |
| **Copia de seguridad** | Exportar e importar un archivo `.kiwu.json` que sirve entre macOS, Linux y Android |
| **Widgets** | macOS: *Deberes*, *Nota rápida* y *Calendario de deberes*. Android: *Deberes*, *Horario* y *Nota rápida*. Configurables y con acciones directas |
| **Idiomas** (Android) | Español, inglés y portugués, según el idioma del teléfono o el que elijas para Kiwu |
| **Privacidad** | Todo local: sin cuentas, sin servidor y sin analíticas |

**Android:** Kotlin · Jetpack Compose · Room · Glance (Android 8 o superior).
**macOS:** SwiftUI · Core Data · WidgetKit (macOS 13 o superior; widgets en macOS 14+).
**Linux:** Python · GTK4 · libadwaita · SQLite (sin widgets por ahora).

## Tecnologías

| | 🤖 Android | 🍎 macOS | 🐧 Linux |
| --- | --- | --- | --- |
| **Lenguaje** | Kotlin | Swift | Python 3 |
| **Interfaz** | Jetpack Compose (Material 3) | SwiftUI (con AppKit para el editor de texto) | GTK4 + libadwaita (PyGObject), nativa en Wayland |
| **Datos** | Room (SQLite) | Core Data (SQLite) | SQLite |
| **Editor de notas** | Editor por bloques propio en Compose | `NSTextView` con texto enriquecido | `GtkTextView` con etiquetas de formato |
| **Código con colores** | Resaltador propio | Resaltador propio (colores de VS Code Dark+/Light+) | GtkSourceView 5 con temas Dark+/Light+ |
| **Recordatorios** | AlarmManager + notificaciones | UserNotifications | Notificaciones del escritorio (GNotification / D-Bus) |
| **Widgets** | — (no incluidos por ahora) | WidgetKit + App Intents + App Groups | — (no incluidos por ahora) |
| **Vídeos incrustados** | Vista previa de enlaces (YouTube, Vimeo) | WebKit (`WKWebView`) | WebKitGTK 6 (opcional) |
| **Atajos globales** | No aplican en el móvil | Menús de la app | Órdenes `kiwu --nueva-tarea` enlazadas en Hyprland (Wayland no permite capturar teclas globales) |
| **Instalador** | `.apk` en GitHub Releases | `.dmg` universal (Apple Silicon e Intel) | `.tar.gz` con instalador, `PKGBUILD` y metadatos AppStream |
| **Pruebas** | JUnit + Robolectric: dominio, contrato compartido, editor y Room | XCTest: reglas del horario, copia de seguridad, borrado lógico y migraciones | Python `unittest`: lógica, instaladores y prueba de humo de la interfaz |

Las tres comparten el **formato de las notas** (JSON con bloques y formato) y el mismo diseño; los datos se pueden mover entre macOS, Linux y Android con la copia de seguridad `.kiwu.json`, y los de macOS también se pueden importar directamente en Linux.

## Privacidad

No hay servidor propio, cuentas ni analíticas. La app solo se conecta a Internet cuando pegas un enlace en una nota, para descargar su vista previa (título, descripción e imagen) o reproducir un vídeo de YouTube/Vimeo; nunca envía tus datos. Los datos viven en el almacenamiento local de la app; en macOS, los widgets leen una copia de los deberes y notas recientes desde un App Group del propio sistema. Puedes llevarte todo con la copia de seguridad `.kiwu.json`.

## Estructura del repositorio

```
kiwu/
  README.md        Esta página
  LICENSE          Licencia MIT (común)
  install.sh       Instalador por terminal que elige el de tu sistema
  docs/            Documentación y capturas comunes
  android/         App de Android (Kotlin + Jetpack Compose) con su APK en Releases
  macos/           App de macOS (SwiftUI) con su propio instalador (.dmg)
  linux/           App de Linux (GTK4 + libadwaita) con su propio instalador
```

Cada carpeta de plataforma es independiente: tiene su código, sus pruebas, su instalador y su documentación.

## Sitio web

Guías, requisitos e instalación en **[kiwu.web.app](https://kiwu.web.app/)**: [funciones](https://kiwu.web.app/funciones/), [instalar](https://kiwu.web.app/instalar/), [macOS](https://kiwu.web.app/macos/), [Linux](https://kiwu.web.app/linux/) y [privacidad](https://kiwu.web.app/privacidad/).

## Contribuir

Las ideas, los errores y las mejoras son bienvenidos: abre un *issue* o un *pull request*. Indica la plataforma (Android, macOS o Linux) en el título. Antes de enviar cambios, comprueba que compilan o pasan las pruebas de la plataforma que tocas (ver su README) y que no incluyes datos personales, como tu Team ID.

## Autor y créditos

- **Autor y mantenedor:** Kevin Sebastián Medina Nava.
- Diseño de la interfaz, logo y widgets creados para este proyecto.
- Desarrollado con la ayuda de [Claude Code](https://claude.com/claude-code) (Anthropic) como asistente de programación.
- Gracias a quienes reporten errores, propongan ideas o envíen mejoras.

## Licencia

Copyright © 2026 Kevin Sebastián Medina Nava. Distribuido bajo la licencia [MIT](LICENSE).
