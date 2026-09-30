# TaskFlow

Aplicación nativa para macOS para organizar la vida universitaria: **deberes por materia, calendario, notas al estilo Notion y widgets de escritorio**. Todo funciona sin conexión y sin cuentas: tus datos se quedan en tu Mac.

Hecha con SwiftUI, Core Data y WidgetKit. Gratuita y de código abierto (licencia MIT).

> Creado por **Kevin Sebastián Medina Nava**.

## Qué incluye

| Módulo | Qué ofrece |
| --- | --- |
| **Tareas** | Listas por materia, deberes con fecha y hora, recordatorios locales, vistas *Hoy* / *Próximos 7 días* / *Todas* / *Completadas* |
| **Calendario** | Mes completo con las entregas en colores pastel; el día se expande con doble clic |
| **Notas** | Cuadernos, páginas con emoji, favoritas, búsqueda y editor por bloques con menú `/` |
| **Archivos** | Carpeta del Finder vinculada a cada lista con explorador integrado, y PDF/Word adjuntos a notas como referencia, sin duplicarlos |
| **Widgets** | *Deberes*, *Nota rápida* y *Calendario de deberes*, configurables y con acciones directas |
| **Privacidad** | Todo local: sin cuentas, sin servidor, sin publicidad ni analíticas |

**Tecnologías:** SwiftUI · Core Data · WidgetKit · App Intents · UserNotifications · App Groups.
**Plataforma:** macOS 13 o superior (widgets en macOS 14 o superior).

## Funciones

**Tareas**
- Listas (una por materia) con deberes, fecha y hora de entrega.
- Recordatorios locales, 24 o 48 horas antes de la entrega.
- Vistas *Hoy*, *Próximos 7 días*, *Todas las tareas* y *Completadas*.
- Calendario mensual con colores pastel por tarea; doble clic en un día lo expande en su sitio.
- Cada tarea tiene su propia nota y puede llevar PDF o documentos de Word adjuntos.

**Archivos por materia**
- Cada lista se puede vincular con una carpeta del Finder (pestaña *Archivos* junto a *Tareas*).
- Explorador integrado: navega por subcarpetas, busca por nombre y abre cada archivo con su aplicación predeterminada.
- Los archivos no se copian: se leen desde su carpeta original. Si renombras o mueves la carpeta, el vínculo se mantiene; si la borras, la lista muestra un aviso y puedes volver a elegirla.

**Notas**
- Cuadernos con páginas, iconos emoji, favoritas y búsqueda en el texto.
- Editor por bloques: escribe `/` para insertar títulos, listas, tareas, citas, código, destacados, imágenes o archivos; también atajos como `# `, `- `, `[] ` o `> `.
- Adjuntar PDF y Word (se guarda una referencia al archivo, no se duplica) y arrastrar archivos desde el Finder.

**Widgets** (macOS 14 o superior)
- *Deberes*: pendientes de todas las listas o de una lista concreta; se pueden completar desde el widget.
- *Nota rápida*: botón "Nueva nota" y notas recientes de un cuaderno.
- *Calendario de deberes*: el mes con las fechas de entrega marcadas, con navegación entre meses.

## Privacidad

No hay servidor, cuentas, publicidad ni analíticas, y la app no usa Internet. Los datos viven en el almacenamiento local de la app; los widgets leen una copia de los deberes y notas recientes desde un App Group del propio sistema.

## Requisitos

- macOS 13 (Ventura) o superior para la app; macOS 14 (Sonoma) o superior para los widgets.
- Xcode 26 o superior.
- Una cuenta de Apple (Apple ID) añadida a Xcode. La app usa un App Group para compartir datos con los widgets, y macOS solo permite esa capacidad con una app firmada por un equipo de desarrollo. Sin firma con equipo, la app arranca mal o los widgets no reciben datos.

## Compilar y ejecutar

```bash
git clone <URL-de-este-repositorio>
cd <carpeta-del-repositorio>
cp Config/Signing.example.xcconfig Config/Signing.xcconfig
```

1. Abre `Config/Signing.xcconfig` y pon tu *Team ID* en `DEVELOPMENT_TEAM` (Xcode → Settings → Accounts → tu equipo). Este archivo es local y está en `.gitignore`.
2. Abre `GestorUniversitario.xcodeproj` en Xcode.
3. Si el identificador `com.ksebas.gestoruniversitario` ya está en uso en tu equipo, cámbialo en los dos targets (*TaskFlow* y *TaskFlowWidgets*); el del widget debe empezar por el de la app (por ejemplo `tuid.Widgets`).
4. Elige el esquema **TaskFlow** y el destino **My Mac**, y pulsa ⌘R.
5. Para los widgets: clic derecho en el escritorio → *Editar widgets* → *TaskFlow*. La app debe haberse abierto al menos una vez para publicar los datos.

Si añades un widget nuevo o cambias sus opciones y macOS no lo muestra, sube el número de compilación (`CURRENT_PROJECT_VERSION`) o cierra sesión y vuelve a entrar: macOS guarda en caché la lista de widgets de cada extensión.

## Estructura del proyecto

```
GestorUniversitario/      La app (el nombre de la carpeta es histórico)
  Models/                 Entidades y formato de las notas (bloques)
  Persistence/            Core Data (tareas y notas en almacenes separados)
  Services/               Reglas de negocio: tareas, cuadernos, recordatorios, widgets
  Views/                  Interfaz SwiftUI (tareas, calendario, notas, editor)
TaskFlowWidgets/          Extensión de widgets (WidgetKit + App Intents)
Shared/                   Código compartido entre la app y los widgets
Config/                   Ajustes de compilación, Info.plist y permisos
Tools/                    Utilidades (generador del icono)
```

**Cómo se comunican la app y los widgets.** La app escribe una instantánea JSON en el App Group cada vez que guarda datos; los widgets solo la leen. Cuando completas un deber desde un widget, este lo deja en una cola y la app lo aplica a su base de datos al abrirse. Los enlaces `taskflow://…` llevan al deber, nota o día del calendario correspondiente.

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

Las ideas, los errores y las mejoras son bienvenidos: abre un *issue* o un *pull request*. Antes de enviar cambios, comprueba que el proyecto compila con los dos esquemas (*TaskFlow* y *TaskFlowWidgets*) y que no incluyes datos personales, como tu Team ID.

## Autor y créditos

- **Autor y mantenedor:** Kevin Sebastián Medina Nava.
- Diseño de la interfaz, logo y widgets creados para este proyecto.
- Desarrollado con la ayuda de [Claude Code](https://claude.com/claude-code) (Anthropic) como asistente de programación.
- Gracias a quienes reporten errores, propongan ideas o envíen mejoras.

## Licencia

Copyright © 2026 Kevin Sebastián Medina Nava. Distribuido bajo la licencia [MIT](LICENSE).
