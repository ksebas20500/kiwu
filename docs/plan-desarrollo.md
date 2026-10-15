# Kiwu: análisis y plan de desarrollo

Fecha: 2026-10-13 · Enfoque: ecosistema macOS + Android (con Linux ya existente)

---

## 1. Contexto del repositorio

**Qué es.** App de productividad para estudiantes: tareas por materia (lista y tablero Por hacer / En curso / Hecho), calendario, notas por bloques con menú `/`, archivos vinculados, widgets y recordatorios. Autor único: Kevin Sebastián Medina Nava. Licencia MIT. Sitio: kiwu.web.app. Promesa de marca: **«sin cuentas, sin nube, sin publicidad»**.

**Estructura** (≈14.800 líneas, 7 commits, sin CI):

| Plataforma | Stack | Datos | Estado |
| --- | --- | --- | --- |
| `macos/` | Swift, SwiftUI, AppKit (`NSTextView`), WidgetKit, App Intents | Core Data en dos SQLite (`GestorUniversitario.sqlite`, `Notas.sqlite`) | v2.0, `.dmg` universal con firma ad hoc |
| `linux/` | Python 3, GTK4, libadwaita, GtkSourceView | SQLite propio (`kiwu.db`, esquema v1) | v0.1.0, tests `unittest` (lógica, instalador, humo de UI) |
| Android | no existe | n/a | **no hay nada** |

**Modelo de dominio** (idéntico en las dos plataformas): `Materia` → `Deber` (con `estado`, `completado`, `fechaEntrega`, `recordatorioAntesEnHoras`, nota en JSON) y `Cuaderno` → `Pagina` (contenido JSON de bloques). El formato de notas (`NoteDocument` / `NoteBlock` / `TextRun`) es **el único contrato multiplataforma real** y ya está implementado y probado en Swift y Python.

**Hallazgo clave:** el pedido menciona macOS y Android, pero el repo tiene macOS y Linux. Android es trabajo nuevo completo, y la sincronización toca las tres plataformas.

---

## 2. Falencias del proyecto

### Críticas (bloquean crecer o sincronizar)

1. **Sin pruebas en macOS.** El README lo admite («Pendiente»). Toda la lógica de `DeberService` y `ContentView` solo se verifica a mano. Linux tiene ~700 líneas de tests, pero nada garantiza que ambas implementaciones se comporten igual.
2. **Sin CI/CD.** No hay `.github/`. Los `.dmg` y `.tar.gz` se generan a mano; nadie corre los tests antes de publicar.
3. **Modelo de datos no apto para sincronizar:**
   - No hay *tombstones* (borrado lógico): un borrado no se puede propagar.
   - Hay `fechaActualizacion`, pero con reloj de pared (frágil entre dispositivos) y sin control de versión por campo.
   - `Materia.orden` en macOS se calcula con `Int(Date.now.timeIntervalSince1970) % 30000` en `Int16` (`DeberService.swift`): es un hack que colisiona y desborda; en Linux es `MAX+1`. Distinto en cada lado.
   - Los IDs son UUID (bien), pero en Linux se guardan como texto en mayúsculas y en macOS como `UUID`; hay que fijar una forma canónica.
4. **Notas como un solo blob JSON con imágenes embebidas** (`imageData: Data` dentro del JSON). Consecuencias: filas enormes, cualquier edición reescribe todo, y la fusión de conflictos solo puede ser «gana una nota completa». Con sync esto pierde ediciones.
5. **Datos atados a la plataforma:** `bookmark` (security-scoped bookmark de macOS) y la carpeta vinculada por materia no tienen equivalente en Android. Hay que declararlos *locales al dispositivo* y no sincronizarlos.

### Importantes

6. **Núcleo duplicado.** Reglas de negocio escritas dos veces (Swift y Python) «sincronizadas a mano». Con Android serían tres. Cada regla nueva multiplica el coste y las divergencias.
7. **Persistencia macOS frágil:** `fatalError` si falla la carga de Core Data y `assertionFailure` (que no hace nada en Release) si falla un guardado: **un error al guardar se pierde en silencio**. El modelo se define por código, sin modelo versionado ni migración explícita. Linux tiene `user_version`, pero `_migrar` solo sabe crear la v1.
8. **Distribución sin firma ni notarización.** Los usuarios deben ejecutar `xattr -dr com.apple.quarantine`. Es una barrera seria de confianza y no hay Sparkle/actualización automática. El instalador recomendado es `curl | bash`.
9. **Nombres históricos filtrándose:** bundle `com.ksebas.gestoruniversitario`, App Group `com.ksebas.taskflow`, bases `GestorUniversitario.sqlite`. Funciona por compatibilidad, pero mezcla tres nombres de marca y complica el alta en App Store y Play.
10. **Producto limitado al ámbito universitario:** vocabulario fijo («deberes», «materias») y sin etiquetas, prioridades, subtareas ni repetición. Reduce el mercado.
11. **Linux acotado a Arch + Hyprland** (decisión explícita, pero limita la audiencia) y la guía `port-linux.md` dice una cosa (Tauri) y el código otra (GTK/Python).
12. **Calidad de repo:** el `.gif` de demo pesa 7,5 MB en git; `docs/` tiene `LEEME.md` y `README.md`; el nombre del autor y el usuario `ksebas20500` están fijos en URLs de instalación.
13. **Accesibilidad y localización:** todo en español embebido en el código, sin catálogo de cadenas. Impide ofrecer inglés (necesario para tiendas).
14. **Privacidad bien comunicada, pero sin cifrado local** de la base de datos.

---

## 3. Mejoras propuestas

Ordenadas por retorno / esfuerzo.

**Productividad (producto)**
- Subtareas y repetición (diaria, semanal, por periodo académico).
- Prioridad y etiquetas; vista «Hoy» enriquecida con tareas vencidas.
- Recordatorios múltiples por tarea y notificaciones accionables (completar, posponer).
- Búsqueda global (tareas + notas) con índice SQLite FTS5.
- Plantillas de notas y enlaces entre páginas (`[[página]]`).
- Importar/exportar: CSV, Markdown, calendario `.ics` (suscribible desde otras apps).
- Vocabulario configurable: «Materia/Deber» ↔ «Lista/Tarea», para abrir el producto más allá del ámbito estudiantil.

**Plataforma**
- macOS: App Intents para Atajos/Siri, Spotlight (Core Spotlight), Share Extension, widgets interactivos ya existentes. iOS/iPadOS después, reutilizando casi todo SwiftUI.
- Android: widgets Glance, Quick Settings tile, notificaciones con acciones, «compartir hacia Kiwu», Material You.
- Linux: Flatpak para salir de Arch.

**Ingeniería**
- Pruebas de contrato compartidas (ver §5, fase 0).
- CI en GitHub Actions: macOS (build + tests), Linux (`unittest`), Android (Gradle).
- Localización (es / en) con catálogos de cadenas.
- Notarización de macOS y actualizaciones automáticas (Sparkle).
- Gestión de errores de guardado visible al usuario (no `assertionFailure`).

---

## 4. Cuenta y sincronización entre dispositivos

### Principios

1. **Local-first y opcional.** La app sigue funcionando sin cuenta; la cuenta solo añade sincronización. Se mantiene la promesa «tus datos son tuyos» (cambia el lema de «sin cuentas» a «sin cuenta obligatoria»).
2. **Una sola verdad del protocolo, tres clientes.** macOS, Android y Linux hablan el mismo protocolo.
3. **Compartir tareas:** además de sincronizar *mis* dispositivos, el objetivo de «compartir tareas» exige **listas compartidas entre cuentas** (invitar a otra persona a una materia/lista).

### Opciones evaluadas

| Opción | Pros | Contras | Veredicto |
| --- | --- | --- | --- |
| **CloudKit / iCloud** | Gratis, sin servidor, ya anotado como pendiente | **No existe en Android ni Linux** (solo API web limitada) | Descartada como núcleo; útil solo como extra Apple |
| **Firebase (Auth + Firestore)** | Offline, SDK Swift y Kotlin, ya usan Firebase Hosting | Sin cliente de escritorio para Python; modelo de documentos poco adecuado a datos relacionales; dependencia de proveedor; precios por lectura | Viable, 2.ª opción |
| **Supabase (Postgres + Auth + Realtime)** | SQL relacional, *Row Level Security* para compartir listas, REST/WebSocket usable desde Swift, Kotlin **y Python**, autoalojable, plan gratuito | Hay que escribir la capa de sincronización | **Recomendada** |
| **Servidor propio** | Control total | Más operación y seguridad a cargo de una sola persona | Descartada al inicio |
| **CRDT (Automerge/Yjs) sobre almacenamiento simple** | Fusión sin conflictos, ideal para notas | Complejidad y tamaño; bindings desiguales en Swift/Kotlin/Python | Solo para el texto de las notas, en una fase posterior |

### Arquitectura recomendada

```
 macOS (Core Data)   Android (Room)   Linux (SQLite)
        │                  │               │
   ┌────▼──────────────────▼───────────────▼────┐
   │  Capa de sync local: cola de cambios (outbox)│
   │  + cursor de último cambio recibido          │
   └────────────────────┬─────────────────────────┘
                        │ HTTPS (push/pull por lotes) + Realtime
              ┌─────────▼──────────┐
              │ Supabase           │
              │ Auth · Postgres    │
              │ RLS · Storage      │
              └────────────────────┘
```

**Autenticación.** Supabase Auth con *Sign in with Apple* (obligatorio si se ofrece login de terceros en App Store), Google (natural en Android) y correo con enlace mágico (cubre Linux). Tokens en Keychain / Android Keystore / Secret Service.

**Modelo de sincronización (record-level, LWW por campo):**
- Cada fila sincronizable añade: `updated_at` (reloj híbrido lógico, no hora de pared), `deleted_at` (tombstone), `device_id`, `version`.
- Resolución: *last-writer-wins por campo* para tareas (`titulo`, `estado`, `fechaEntrega`…), de modo que dos dispositivos que editan campos distintos no se pisan.
- Sincronización incremental: `pull` con cursor `since` + `push` de la *outbox*; idempotente por UUID.
- Notas: en v1 LWW por página con **copia de conflicto** («Página (conflicto 13-oct)») para no perder texto; en v2 fusión por bloque (cada `NoteBlock` ya tiene `id`), y solo si hace falta, CRDT dentro de bloque.
- **Imágenes y adjuntos fuera del JSON**: a Supabase Storage con hash de contenido (deduplicación); la nota guarda solo la referencia. Los `bookmark` y rutas locales **no se sincronizan** (se marcan `solo_local`).

**Compartir tareas.**
- Tablas `lista_miembro(lista_id, usuario_id, rol)` con roles *propietario / editor / lector*.
- RLS: un usuario solo ve filas de listas donde es miembro.
- Invitación por enlace o correo; asignar tareas a miembros (campo `asignado_a`).
- Vista «Compartido conmigo» en la barra lateral.

**Seguridad y privacidad.**
- RLS en todas las tablas; sin claves de servicio en los clientes.
- Política de privacidad actualizada, exportación de datos y **borrado de cuenta** (requisito de Apple y Google).
- Cifrado de extremo a extremo como opción futura (conflicta con búsqueda en servidor y con compartir; tratarlo como decisión de producto, no de v1).

**Migración de usuarios existentes.** Primera vez que se activa la cuenta: se sube todo lo local con los UUID actuales (ya son estables), sin duplicados.

---

## 5. Análisis de rebranding (Kiwu)

**Situación.** «Kiwu» ya es el nombre público (repo, sitio `kiwu.web.app`, instaladores) tras cambiar desde *TaskFlow* en la 2.0. Un segundo cambio tendría un coste real: enlaces, instaladores, esquema `kiwu://`, bundle IDs históricos.

**Hallazgos de la búsqueda** (limitada, no sustituye una revisión legal):
- No encontré una app de tareas llamada «Kiwu» ni marcas registradas «KIWU» en las búsquedas realizadas; los resultados devolvieron «Kiwi» (p. ej. el navegador Kiwi, retirado y luego restaurado en Google Play). Esto **no** es una confirmación de disponibilidad.
- «Kiwu» está a una letra de «Kiwi», un nombre saturado (navegador, aerolínea/buscador de vuelos, apps de fitness, etc.). Riesgos: confusión de búsqueda en tiendas, SEO débil y posible oposición de marcas «Kiwi» en clases 9 / 42 (software).

**Fortalezas del nombre actual:** corto, pronunciable en español/inglés, identidad ya construida (icono, color coral `E8705A`, sitio).
**Debilidades:** parece un error de escritura de «Kiwi»; no comunica productividad; el icono/paleta no refuerzan «tareas + notas»; el espacio de nombres es bajo en dominio (`kiwu.web.app` es un subdominio gratuito de Firebase, no un dominio propio).

**Recomendación: no renombrar ahora; decidir con una puerta de verificación antes del lanzamiento público en tiendas.** Razón: el coste de cambiar es mínimo hoy (pocos usuarios) y crece tras publicar en App Store / Play, así que la decisión debe tomarse **antes** de la fase de tiendas, no después.

Pasos de la verificación (1 semana, bajo coste):
1. Búsqueda en USPTO / EUIPO / OEPM / WIPO Global Brand DB de «KIWU» y variantes fonéticas («Kiwi», «Kiwu», «Quiwu») en clases 9, 42 y 41.
2. Búsqueda en App Store, Google Play, GitHub, npm, PyPI, dominios `.app`/`.com`/`.io`.
3. Comprobar significados negativos en español, inglés y quechua/otros idiomas del público objetivo.
4. Comprar un dominio propio (`kiwu.app` o el finalista) **ya**, independientemente de la decisión, para dejar de depender de `web.app`.

Criterio de decisión: si hay marca registrada conflictiva en clases 9/42, o dominio/handle inalcanzables → renombrar. Si no → mantener y registrar la marca.

**Si se renombra**, candidatos a evaluar (todos por validar legalmente; son ideas, no disponibilidad confirmada): **Kiwu** (mantener), **Nidal / Nido** (lugar donde se guardan las cosas), **Tareo**, **Plumi**, **Foliar**, **Anota**. Criterios: pronunciable, ≤3 sílabas, sin parecido a marcas grandes, dominio libre, sentido neutro entre idiomas.

**Plan técnico de un renombre** (si ocurre): cambiar *nombre visible* (`CFBundleDisplayName`, `.desktop`, ficha de Play) sin tocar los identificadores internos (`com.ksebas.gestoruniversitario`, App Group, nombres de SQLite), para no romper datos; mantener `kiwu://` como alias; redirigir sitio y repo (GitHub redirige solo); actualizar instaladores y README; regenerar icono con `generar-icono.swift`.

Importante, independiente del nombre: **fijar ya un identificador de aplicación definitivo y neutro** para Android y para App Store (p. ej. `app.<dominio-propio>.<nombre>`), porque no se puede cambiar después de publicar.

---

## 6. Plan de desarrollo

Estimación orientada a **una persona a ~10–15 h/semana** (la realidad del repo). Los plazos se revisan al terminar cada fase.

### Fase 0: Cimientos (semanas 1–3)
Sin esto, lo demás es construir sobre arena.
- [ ] Crear `.github/workflows`: tests Python en Linux, build + tests macOS, lint.
- [ ] Añadir **target de tests en Xcode**; cubrir `DeberService`, `ColumnaFlujo`, serialización de `NoteDocument`, `CarpetaDatos.preparar`.
- [ ] Definir `docs/contrato/`: especificación del modelo, **fixtures JSON comunes** (notas y tareas de ejemplo) que los tres clientes deben leer y escribir idénticos. Esta es la prueba de paridad entre plataformas.
- [ ] Sustituir `fatalError` / `assertionFailure` de persistencia por manejo de errores visible; corregir `orden` de `Materia` (macOS) para que coincida con Linux.
- [ ] Decisión de marca (§5) + dominio propio + identificador de app definitivo.
- [ ] Mover el `.gif` pesado a Releases/sitio; unificar `LEEME.md`/`README.md`; actualizar `port-linux.md` al stack real.

**Hecho cuando:** CI verde en cada PR y las fixtures pasan en macOS y Linux.

### Fase 1: Preparar los datos para sincronizar (semanas 4–7)
- [ ] Migración de esquema v2 en macOS y Linux: `deleted_at`, `updated_at` (HLC), `device_id`, campo `solo_local`. Migración reversible y con copia previa de la base.
- [ ] Borrado lógico en todos los servicios; limpieza periódica de tombstones.
- [ ] Extraer imágenes/adjuntos del JSON de la nota a un almacén de blobs con hash; compatibilidad de lectura con el formato antiguo.
- [ ] Capa **outbox** (registro de cambios locales) en ambos clientes.
- [ ] Localización es/en con catálogos de cadenas.

**Hecho cuando:** los datos existentes migran sin pérdida y todos los cambios pasan por la outbox.

### Fase 2: Servidor y cuenta (semanas 8–11)
- [ ] Proyecto Supabase: tablas, índices, RLS, Storage, política de retención.
- [ ] Pruebas automáticas de RLS (un usuario no ve datos de otro).
- [ ] Autenticación: Apple, Google, correo mágico; pantalla «Cuenta» opcional en ajustes (macOS y Linux).
- [ ] Motor de sync (push/pull/realtime) en macOS primero; resolución LWW por campo; copia de conflicto en notas.
- [ ] Pruebas de sync con simulación de dos clientes, desconexiones y relojes desfasados.
- [ ] Política de privacidad, exportación y borrado de cuenta.

**Hecho cuando:** dos Macs distintos convergen tras editar sin conexión y reconectar.

### Fase 3: Android (semanas 10–20, solapa con la 2)
- [ ] Stack: **Kotlin + Jetpack Compose + Room + WorkManager**; módulo de dominio en Kotlin puro, testeable sin Android.
- [ ] Paridad v1: tareas (lista + tablero), calendario, notas por bloques (editor Compose propio; empezar con bloques de texto, checklist, código y enlaces), recordatorios (`AlarmManager` exacto + permisos de Android 13/14), ajustes y tema Material You.
- [ ] Lectura/escritura del formato de notas contra las **fixtures comunes**.
- [ ] Sync desde el primer día (no hay datos locales previos que migrar).
- [ ] Widget Glance (deberes) y acción «Compartir hacia Kiwu».
- [ ] Beta cerrada en Google Play (se exigen testers y periodo mínimo de prueba para cuentas nuevas personales; verificar requisitos vigentes en Play Console).

**Hecho cuando:** crear una tarea en Android la muestra en macOS en <10 s y viceversa.

### Fase 4: Compartir (semanas 18–24)
- [ ] Listas compartidas: invitaciones, roles, `asignado_a`, vista «Compartido conmigo».
- [ ] Notificaciones push (APNs/FCM) para cambios de otros usuarios.
- [ ] Revisión de abuso y límites (tamaño, tasa).

### Fase 5: Distribución y pulido (semanas 22–30)
- [ ] macOS: cuenta Apple Developer, firma, **notarización**, Sparkle; evaluar Mac App Store (App Group real, sandbox).
- [ ] Android: ficha de Play, capturas, política de datos.
- [ ] Linux: Flatpak además de PKGBUILD.
- [ ] Sitio con dominio propio; documentación de sync.
- [ ] Accesibilidad (VoiceOver, TalkBack, contraste) y rendimiento con 10 000 tareas.

### Fase 6: Después (backlog)
iOS/iPadOS (reutiliza SwiftUI), fusión por bloque / CRDT en notas, cifrado de extremo a extremo opcional, suscripción «Pro» (sync + almacenamiento) o donaciones, subtareas y repetición, importadores (Todoist, Notion, Apple Notes).

---

## 7. Riesgos y decisiones abiertas

| Riesgo | Impacto | Mitigación |
| --- | --- | --- |
| Un solo desarrollador, tres plataformas | Alto | Núcleo con fixtures comunes, CI, alcance v1 estricto; no iniciar Android hasta cerrar Fase 1 |
| Perder datos al migrar el esquema | Alto | Copia previa automática, tests de migración con bases reales anonimizadas |
| Conflictos que pierden texto de notas | Alto | Copia de conflicto en v1; fusión por bloque en v2 |
| Coste y operación del backend | Medio | Plan gratuito de Supabase al inicio, límites de tamaño, posible modelo Pro |
| Requisitos de las tiendas (borrado de cuenta, Sign in with Apple, pruebas cerradas en Play) | Medio | Planificarlos en Fases 2 y 5; no dejarlos para el final |
| Cambiar el lema «sin cuentas» | Medio | Cuenta opcional, comunicarlo en README, sitio y política |
| Renombrar tarde | Alto | Puerta de verificación de marca **antes** de la Fase 5 |

**Decisiones que necesito de ti:**
1. ¿Supabase como backend (recomendado) o prefieres Firebase?
2. ¿Compartir tareas con **otras personas** entra en la v1 de sync, o solo sincronizar tus propios dispositivos? (Cambia el alcance de la Fase 4.)
3. ¿Mantienes el foco universitario o abres el producto a cualquier usuario? (Afecta nombre, vocabulario y mercado.)
4. ¿Hay presupuesto para Apple Developer (99 USD/año) y Google Play (25 USD, pago único)? Sin ellos no hay notarización ni tiendas.
5. ¿Vas a hacer la verificación de marca tú mismo o con ayuda profesional?
