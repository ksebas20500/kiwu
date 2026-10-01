# Kiwu para macOS

Aplicación nativa de macOS (SwiftUI, Core Data y WidgetKit). Para ver qué hace y cómo se ve, consulta el [README principal](../README.md). Aquí está lo técnico de esta versión: instalar, compilar, estructura y cómo crear el instalador.

Versión actual: **2.0** · Etiquetas de versión: `v2.x` (las anteriores, `v1.0.0`, son de TaskFlow).

## Requisitos

- macOS 13 (Ventura) o superior para la app; macOS 14 (Sonoma) o superior para los widgets.
- Xcode 26 o superior.
- Una cuenta de Apple (Apple ID) añadida a Xcode. La app usa un App Group para compartir datos con los widgets, y macOS solo permite esa capacidad con una app firmada por un equipo de desarrollo. Sin firma con equipo, la app arranca mal o los widgets no reciben datos.

## Instalación (sin compilar)

Descarga la última versión para macOS desde [**Releases**](https://github.com/ksebas20500/kiwu/releases/latest) (enlace directo al instalador: [`Kiwu.dmg`](https://github.com/ksebas20500/kiwu/releases/latest/download/Kiwu.dmg)). Cada versión conserva su propio `.dmg` en la [lista de versiones](https://github.com/ksebas20500/kiwu/releases).

**Opción 1: un solo comando** (recomendada; comprueba la descarga y deja la app lista para abrir)

```bash
curl -fsSL https://raw.githubusercontent.com/ksebas20500/kiwu/main/macos/install.sh | bash
```

(El comando de la raíz del repositorio, `…/main/install.sh`, hace lo mismo: elige el instalador de tu sistema.)

```bash
# equivalente, desde la raíz:
curl -fsSL https://raw.githubusercontent.com/ksebas20500/kiwu/main/install.sh | bash
```

**Opción 2: a mano.** Descarga `Kiwu.dmg`, ábrelo y arrastra **Kiwu** a *Aplicaciones*.
La primera vez, macOS avisará de que no puede verificar al desarrollador (la app no está notarizada por Apple, porque eso requiere una cuenta de pago). Para abrirla:
- *Ajustes del Sistema → Privacidad y seguridad → Abrir igualmente*, o
- en la Terminal: `xattr -dr com.apple.quarantine /Applications/Kiwu.app`

El instalador es **universal** (Apple Silicon e Intel), pesa unos pocos MB y necesita macOS 13 o superior (widgets: macOS 14 o superior).

Si prefieres no fiarte de un binario, compílalo tú mismo (siguiente sección) o revisa cómo se genera en [`Tools/crear-instalador.sh`](Tools/crear-instalador.sh).

## Actualizar desde TaskFlow (versión 1.x)

TaskFlow pasó a llamarse **Kiwu** en la versión 2.0. Al instalar Kiwu (con el comando de arriba o desde el `.dmg`) se sustituye la app antigua `TaskFlow.app` y, al abrir Kiwu por primera vez, se **copian automáticamente** tus listas, tareas, notas y fondo a la nueva carpeta de datos (`Application Support/Kiwu`). La carpeta antigua no se borra: es tu copia de seguridad. Si usabas widgets, quítalos del escritorio y vuelve a añadirlos (*Editar widgets → Kiwu*), porque macOS los asocia al nombre.

## Compilar y ejecutar

```bash
git clone https://github.com/ksebas20500/kiwu.git
cd kiwu/macos
cp Config/Signing.example.xcconfig Config/Signing.xcconfig
```

1. Abre `Config/Signing.xcconfig` y pon tu *Team ID* en `DEVELOPMENT_TEAM` (Xcode → Settings → Accounts → tu equipo). Este archivo es local y está en `.gitignore`.
2. Abre `Kiwu.xcodeproj` en Xcode.
3. Si el identificador `com.ksebas.gestoruniversitario` ya está en uso en tu equipo, cámbialo en los dos targets (*Kiwu* y *KiwuWidgets*); el del widget debe empezar por el de la app (por ejemplo `tuid.Widgets`).
4. Elige el esquema **Kiwu** y el destino **My Mac**, y pulsa ⌘R.
5. Para los widgets: clic derecho en el escritorio → *Editar widgets* → *Kiwu*. La app debe haberse abierto al menos una vez para publicar los datos.

Si añades un widget nuevo o cambias sus opciones y macOS no lo muestra, sube el número de compilación (`CURRENT_PROJECT_VERSION`) o cierra sesión y vuelve a entrar: macOS guarda en caché la lista de widgets de cada extensión.

## Estructura de esta carpeta

```
macos/
  Kiwu.xcodeproj/     Proyecto de Xcode (esquemas: Kiwu y KiwuWidgets)
  Kiwu/               La app
    Models/             Entidades y formato de las notas (bloques)
    Persistence/        Core Data (tareas y notas en almacenes separados)
    Services/           Reglas de negocio: tareas, cuadernos, recordatorios, widgets
    Views/              Interfaz SwiftUI (tareas, calendario, notas, editor)
  KiwuWidgets/        Extensión de widgets (WidgetKit + App Intents)
  Shared/             Código compartido entre la app y los widgets
  Config/             Ajustes de compilación, Info.plist y permisos
  Tools/              Crear el instalador (.dmg), generar el icono
  install.sh          Instalador por terminal (descarga el .dmg del último release de macOS)
  dist/               Salida del instalador (no se sube al repositorio)
```

**Cómo se comunican la app y los widgets.** La app escribe una instantánea JSON en el App Group cada vez que guarda datos; los widgets solo la leen. Cuando completas un deber desde un widget, este lo deja en una cola y la app lo aplica a su base de datos al abrirse. Los enlaces `kiwu://…` llevan al deber, nota o día del calendario correspondiente.

**Nombres que se conservan por compatibilidad.** El identificador de la app (`com.ksebas.gestoruniversitario`), el App Group y los archivos de las bases de datos (`GestorUniversitario.sqlite`, `Notas.sqlite`) mantienen su nombre histórico para que las actualizaciones no pierdan datos.

## Crear el instalador

```bash
Tools/crear-instalador.sh     # genera dist/Kiwu.dmg (Release, universal, firma ad hoc)
```

La versión del instalador se compila con la opción `DIRECT_DISTRIBUTION`: como macOS no permite usar App Groups sin un equipo de desarrollo, la app y los widgets comparten sus datos por una carpeta (`~/Library/Application Support/Kiwu`) autorizada en el sandbox de ambos. La compilación desde Xcode con tu equipo sigue usando el App Group.

## Icono

El icono se genera con un script de Swift, por si quieres modificarlo:

```bash
swiftc Tools/generar-icono.swift -o /tmp/generar-icono
/tmp/generar-icono Kiwu/Assets.xcassets/AppIcon.appiconset
```

## Pendiente

- Sincronización entre Macs (iCloud).
- Pruebas automatizadas e integración continua.

## Licencia

MIT, ver [LICENSE](../LICENSE).
