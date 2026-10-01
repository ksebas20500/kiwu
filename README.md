<p align="center">
  <img src="docs/media/icono.png" width="128" alt="Icono de Kiwu">
</p>

<h1 align="center">Kiwu</h1>

<p align="center">
  <b>Tareas, tablero, calendario y notas para macOS.</b><br>
  Todo en tu Mac: sin cuentas, sin nube, sin publicidad.
</p>

<p align="center">
  <a href="https://github.com/ksebas20500/kiwu/releases/latest/download/Kiwu.dmg"><b>⬇️ Descargar para macOS</b></a>
  &nbsp;·&nbsp;
  <a href="https://github.com/ksebas20500/kiwu/releases">Todas las versiones</a>
  &nbsp;·&nbsp;
  <a href="#instalación-sin-compilar">Instalación</a>
</p>

<p align="center">
  <a href="https://github.com/ksebas20500/kiwu/releases/latest"><img alt="Última versión" src="https://img.shields.io/github/v/release/ksebas20500/kiwu?label=versi%C3%B3n&color=E8705A"></a>
  <img alt="macOS 13 o superior" src="https://img.shields.io/badge/macOS-13%2B-555?logo=apple&logoColor=white">
  <img alt="Hecho con SwiftUI" src="https://img.shields.io/badge/SwiftUI-Core%20Data%20%C2%B7%20WidgetKit-F05138?logo=swift&logoColor=white">
  <a href="LICENSE"><img alt="Licencia MIT" src="https://img.shields.io/badge/licencia-MIT-2EA44F"></a>
</p>

<p align="center">
  <img src="docs/media/tablero_custom.png" alt="Kiwu con un fondo personalizado, en modo tablero" width="860">
</p>

<p align="center">
  <img src="docs/media/demo.gif" alt="Demostración de Kiwu" width="860">
</p>

> Creado por **Kevin Sebastián Medina Nava**. Antes se llamaba *TaskFlow*; ver [Actualizar desde TaskFlow](#actualizar-desde-taskflow-versión-1x).

## Capturas

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
| **Calendario** | Mes completo con las entregas en colores pastel; el día se expande con doble clic |
| **Notas** | Cuadernos, páginas con emoji, favoritas, búsqueda y editor por bloques con menú `/` |
| **Archivos** | Carpeta del Finder vinculada a cada lista con explorador integrado, y PDF/Word adjuntos a notas como referencia, sin duplicarlos |
| **Ajustes** | Atajos de teclado editables, tema claro/oscuro y fondo de imagen con translucidez ajustable |
| **Widgets** | *Deberes*, *Nota rápida* y *Calendario de deberes*, configurables y con acciones directas |
| **Privacidad** | Todo local: sin cuentas, sin servidor, sin publicidad ni analíticas |

**Tecnologías:** SwiftUI · Core Data · WidgetKit · App Intents · UserNotifications · App Groups.
**Plataforma:** macOS 13 o superior (widgets en macOS 14 o superior).

## Funciones

**Tareas**
- Listas (una por materia) con deberes, fecha y hora de entrega.
- Recordatorios locales, 24 o 48 horas antes de la entrega.
- Vistas *Hoy*, *Próximos 7 días*, *Todas las tareas* y *Completadas*.
- **Lista o tablero** con tres columnas (Por hacer, En curso, Hecho): arrastra las tarjetas entre columnas.
- Calendario mensual con colores pastel por tarea; doble clic en un día lo expande en su sitio.
- Cada tarea tiene su propia nota y puede llevar PDF o documentos de Word adjuntos.

**Archivos por materia**
- Cada lista se puede vincular con una carpeta del Finder (pestaña *Archivos* junto a *Tareas*).
- Explorador integrado: navega por subcarpetas, busca por nombre y abre cada archivo con su aplicación predeterminada.
- Los archivos no se copian: se leen desde su carpeta original. Si renombras o mueves la carpeta, el vínculo se mantiene; si la borras, la lista muestra un aviso y puedes volver a elegirla.

**Notas**
- Cuadernos con páginas, iconos emoji, favoritas y búsqueda en el texto.
- Editor por bloques: escribe `/` (navega con ↑ ↓ e Intro) para insertar títulos, listas, tareas, citas, código, destacados, imágenes o archivos; también atajos como `# `, `- `, `[] ` o `> `.
- Formato al escribir: negrita, cursiva, subrayado, tachado, código en línea, colores, resaltado y enlaces (selecciona texto para ver la barra; ⌘B ⌘I ⌘U ⌘⇧X ⌘E ⌘K). Tab / ⇧Tab cambian la sangría.
- Enlaces con vista previa (páginas web y vídeos de YouTube/Vimeo reproducibles) y bloques de código con colores como VS Code.
- Adjuntar PDF y Word (se guarda una referencia al archivo, no se duplica) y arrastrar archivos desde el Finder.

**Ajustes** (engranaje, abajo a la izquierda)
- Atajos de teclado editables para las acciones principales, con aviso si dos chocan.
- Tema claro, oscuro o del sistema; imagen de fondo con visibilidad, opacidad de paneles y desenfoque ajustables.

**Widgets** (macOS 14 o superior)
- *Deberes*: pendientes de todas las listas o de una lista concreta; se pueden completar desde el widget.
- *Nota rápida*: botón "Nueva nota" y notas recientes de un cuaderno.
- *Calendario de deberes*: el mes con las fechas de entrega marcadas, con navegación entre meses.

## Actualizar desde TaskFlow (versión 1.x)

TaskFlow pasó a llamarse **Kiwu** en la versión 2.0. Al instalar Kiwu (con el comando de arriba o desde el `.dmg`) se sustituye la app antigua `TaskFlow.app` y, al abrir Kiwu por primera vez, se **copian automáticamente** tus listas, tareas, notas y fondo a la nueva carpeta de datos (`Application Support/Kiwu`). La carpeta antigua no se borra: es tu copia de seguridad. Si usabas widgets, quítalos del escritorio y vuelve a añadirlos (*Editar widgets → Kiwu*), porque macOS los asocia al nombre.

## Privacidad

No hay servidor, cuentas, publicidad ni analíticas. La app solo se conecta a Internet cuando pegas un enlace en una nota, para descargar su vista previa (título, descripción e imagen) o reproducir un vídeo de YouTube/Vimeo; nunca envía tus datos. Los datos viven en el almacenamiento local de la app; los widgets leen una copia de los deberes y notas recientes desde un App Group del propio sistema.

## Requisitos

- macOS 13 (Ventura) o superior para la app; macOS 14 (Sonoma) o superior para los widgets.
- Xcode 26 o superior.
- Una cuenta de Apple (Apple ID) añadida a Xcode. La app usa un App Group para compartir datos con los widgets, y macOS solo permite esa capacidad con una app firmada por un equipo de desarrollo. Sin firma con equipo, la app arranca mal o los widgets no reciben datos.

## Instalación (sin compilar)

Descarga la última versión desde [**Releases**](https://github.com/ksebas20500/kiwu/releases/latest) (enlace directo al instalador: [`Kiwu.dmg`](https://github.com/ksebas20500/kiwu/releases/latest/download/Kiwu.dmg)). Cada versión conserva su propio `.dmg` en la [lista de versiones](https://github.com/ksebas20500/kiwu/releases).

**Opción 1: un solo comando** (recomendada; comprueba la descarga y deja la app lista para abrir)

```bash
curl -fsSL https://raw.githubusercontent.com/ksebas20500/kiwu/main/install.sh | bash
```

**Opción 2: a mano.** Descarga `Kiwu.dmg`, ábrelo y arrastra **Kiwu** a *Aplicaciones*.
La primera vez, macOS avisará de que no puede verificar al desarrollador (la app no está notarizada por Apple, porque eso requiere una cuenta de pago). Para abrirla:
- *Ajustes del Sistema → Privacidad y seguridad → Abrir igualmente*, o
- en la Terminal: `xattr -dr com.apple.quarantine /Applications/Kiwu.app`

El instalador es **universal** (Apple Silicon e Intel), pesa unos pocos MB y necesita macOS 13 o superior (widgets: macOS 14 o superior).

Si prefieres no fiarte de un binario, compílalo tú mismo (siguiente sección) o revisa cómo se genera en [`Tools/crear-instalador.sh`](Tools/crear-instalador.sh).

## Compilar y ejecutar

```bash
git clone <URL-de-este-repositorio>
cd <carpeta-del-repositorio>
cp Config/Signing.example.xcconfig Config/Signing.xcconfig
```

1. Abre `Config/Signing.xcconfig` y pon tu *Team ID* en `DEVELOPMENT_TEAM` (Xcode → Settings → Accounts → tu equipo). Este archivo es local y está en `.gitignore`.
2. Abre `GestorUniversitario.xcodeproj` en Xcode.
3. Si el identificador `com.ksebas.gestoruniversitario` ya está en uso en tu equipo, cámbialo en los dos targets (*Kiwu* y *KiwuWidgets*); el del widget debe empezar por el de la app (por ejemplo `tuid.Widgets`).
4. Elige el esquema **Kiwu** y el destino **My Mac**, y pulsa ⌘R.
5. Para los widgets: clic derecho en el escritorio → *Editar widgets* → *Kiwu*. La app debe haberse abierto al menos una vez para publicar los datos.

Si añades un widget nuevo o cambias sus opciones y macOS no lo muestra, sube el número de compilación (`CURRENT_PROJECT_VERSION`) o cierra sesión y vuelve a entrar: macOS guarda en caché la lista de widgets de cada extensión.

## Estructura del proyecto

```
GestorUniversitario/      La app (el nombre de la carpeta es histórico)
  Models/                 Entidades y formato de las notas (bloques)
  Persistence/            Core Data (tareas y notas en almacenes separados)
  Services/               Reglas de negocio: tareas, cuadernos, recordatorios, widgets
  Views/                  Interfaz SwiftUI (tareas, calendario, notas, editor)
KiwuWidgets/          Extensión de widgets (WidgetKit + App Intents)
Shared/                   Código compartido entre la app y los widgets
Config/                   Ajustes de compilación, Info.plist y permisos
Tools/                    Utilidades (generador del icono)
```

**Cómo se comunican la app y los widgets.** La app escribe una instantánea JSON en el App Group cada vez que guarda datos; los widgets solo la leen. Cuando completas un deber desde un widget, este lo deja en una cola y la app lo aplica a su base de datos al abrirse. Los enlaces `kiwu://…` llevan al deber, nota o día del calendario correspondiente.

## Crear el instalador

```bash
Tools/crear-instalador.sh     # genera dist/Kiwu.dmg (Release, universal, firma ad hoc)
```

La versión del instalador se compila con la opción `DIRECT_DISTRIBUTION`: como macOS no permite usar App Groups sin un equipo de desarrollo, la app y los widgets comparten sus datos por una carpeta (`~/Library/Application Support/Kiwu`) autorizada en el sandbox de ambos. La compilación desde Xcode con tu equipo sigue usando el App Group.

## Icono

El icono se genera con un script de Swift, por si quieres modificarlo:

```bash
swiftc Tools/generar-icono.swift -o /tmp/generar-icono
/tmp/generar-icono GestorUniversitario/Assets.xcassets/AppIcon.appiconset
```

## Pendiente

- Sincronización entre Macs (iCloud).
- Pruebas automatizadas e integración continua.

## Contribuir

Las ideas, los errores y las mejoras son bienvenidos: abre un *issue* o un *pull request*. Antes de enviar cambios, comprueba que el proyecto compila con los dos esquemas (*Kiwu* y *KiwuWidgets*) y que no incluyes datos personales, como tu Team ID.

## Autor y créditos

- **Autor y mantenedor:** Kevin Sebastián Medina Nava.
- Diseño de la interfaz, logo y widgets creados para este proyecto.
- Desarrollado con la ayuda de [Claude Code](https://claude.com/claude-code) (Anthropic) como asistente de programación.
- Gracias a quienes reporten errores, propongan ideas o envíen mejoras.

## Licencia

Copyright © 2026 Kevin Sebastián Medina Nava. Distribuido bajo la licencia [MIT](LICENSE).
