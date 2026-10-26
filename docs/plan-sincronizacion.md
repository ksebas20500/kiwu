# Plan de sincronización entre PC y móvil

Detalla el apartado 4 de [plan-desarrollo.md](plan-desarrollo.md) con un requisito añadido: **sincronizar tareas y notas sin que el rendimiento ni el diseño de una plataforma afecten a la otra**. Aún no hay código de sincronización; este documento es el plan.

## 1. Objetivos y límites

| Objetivo | Cómo se garantiza |
| --- | --- |
| Las tareas, listas, cuadernos, notas (y el futuro horario, ver [plan-horario.md](plan-horario.md)) son las mismas en todos mis dispositivos | Protocolo único y fixtures compartidas en `docs/contrato/` |
| Escribir, marcar o abrir una nota **nunca espera a la red** | Local-first: toda escritura va a la base local; la sincronización es un proceso aparte |
| Cada plataforma conserva su propio aspecto | Solo viajan **datos**, nunca decisiones de presentación (sección 2) |
| Sin cuenta, la app funciona igual que hoy | La cuenta es opcional; se activa desde Ajustes |
| No perder texto | Copia de conflicto en notas; borrado lógico (tombstones) en todo |

Fuera de alcance en la primera versión: compartir listas con otras personas (fase posterior), cifrado de extremo a extremo y edición simultánea en tiempo real dentro de una misma nota.

## 2. Qué se sincroniza y qué no

La regla: **se sincroniza lo que la persona escribió; no lo que cada pantalla decide mostrar.**

| Se sincroniza (datos) | No se sincroniza (presentación y dispositivo) |
| --- | --- |
| Listas/materias: nombre, orden, borrado | Tema claro/oscuro/sistema |
| Tareas: título, notas, fecha de entrega, estado, completado, orden, lista | **Color de la app** (acento), fondo de imagen, visibilidad y velo |
| Cuadernos y páginas: título, icono, favorita, contenido por bloques | Vista de tareas (lista/tablero), tamaño de ventana, filtros y pestañas abiertas |
| Contenido de la nota: tipos de bloque, texto, formato en línea con **nombres** de color («rojo», «azul») | Atajos de teclado (macOS/Linux) |
| Clases del horario (día, inicio, fin, materia, aula) | Carpeta vinculada a una lista y sus rutas/bookmarks (son del dispositivo) |
| Imágenes y archivos adjuntos (**nunca se sincronizan**, solo el nombre del archivo) · Colores pastel de las tareas: se calculan localmente a partir del id, así cada plataforma los pinta con su paleta |
| Marcas de borrado | Estado de avisos ya mostrados (`recordado`) y alarmas programadas: son locales |

Consecuencias de diseño:

- El formato de las notas ya guarda colores por **nombre**, no por valor; cada cliente los traduce a su paleta clara u oscura. Eso se mantiene: ningún campo sincronizado puede contener un color en hexadecimal, un tamaño en píxeles ni una fuente.
- Los ajustes de apariencia se guardan en el almacén local de cada plataforma (`DataStore` en Android, `settings.json` en Linux, `UserDefaults` en macOS) y no tienen columna en la nube.
- Las alarmas se reconstruyen en cada dispositivo a partir de las tareas sincronizadas (ya lo hace `ProgramadorAvisos`). Completar una tarea en un dispositivo cancela su aviso en todos al sincronizar.
- Un dispositivo que no entiende un campo nuevo lo **conserva y lo reenvía** sin tocarlo (ya ocurre con las notas: `extra` en Android, campos desconocidos en Linux). Así una versión antigua no borra lo que una nueva escribió.

## 3. Cómo se protege el rendimiento

1. **La base local manda.** La interfaz solo lee y escribe en la base local. Las pantallas no esperan respuestas de red y no muestran «cargando» para sus propios datos.
2. **Proceso aparte, con prioridad baja.** Android: `WorkManager` con restricción de red; Linux: hilo/servicio con `nice`; macOS: tarea en segundo plano de baja prioridad. Nada de trabajo de sincronización en el hilo principal.
3. **Arranque intacto.** La sincronización empieza al menos 3 s después de mostrar la primera pantalla; el tiempo de arranque no cambia.
4. **Cambios, no copias.** Cola de salida (*outbox*) con **compactación**: varias ediciones seguidas de la misma fila se envían como una. Descarga incremental con cursor (`desde`), nunca la base entera después de la primera vez.
5. **Lotes y compresión.** Hasta 200 filas por petición, comprimidas (gzip). Reintentos con espera exponencial (1 s, 2 s, 4 s… hasta 15 min) y sin reintentar si no hay red.
6. **Disparadores sensatos.** Tras un cambio (con 3 s de espera para agrupar), al abrir la app, al volver al primer plano y cada 15 min como máximo en segundo plano. Conexión en vivo (*realtime*) solo con la app visible, para no gastar batería.
7. **Sin imágenes ni archivos.** Solo viaja texto. Antes de enviar una nota se quitan los bloques de imagen, el `imageData`, las vistas previas de enlace en base64 y las rutas del dispositivo (`Documento.paraSincronizar()` en `domain/Sincronizacion.kt`). Una nota sincronizada pesa unos pocos KB.
8. **Transacciones cortas.** Aplicar lo recibido se hace en una sola transacción por lote, sin bloquear las lecturas de la interfaz (SQLite en modo WAL en Linux; Room ya lo hace en Android).
9. **Presupuestos medibles** (se comprueban en las pruebas de cada fase):

| Medida | Límite |
| --- | --- |
| Latencia de una escritura local con la sincronización activa | Igual a la actual (sin sincronización) |
| Primera sincronización de 5 000 tareas y 500 notas sin imágenes | Menos de 30 s en Wi-Fi, sin bloquear la interfaz |
| Sincronización incremental de una edición | Menos de 2 s |
| Memoria extra del proceso de sincronización | Menos de 30 MB |
| Consumo en segundo plano | Sin activaciones periódicas más frecuentes que cada 15 min |

## 4. Diseño técnico

> **Decisión (25 oct 2026):** se usa **Firebase con el plan Spark (gratis, sin tarjeta)**: Authentication + Cloud Firestore. Sustituye a la decisión del 13 oct (Supabase gratis), porque ese plan **se pausa tras una semana sin uso** y no tiene copias. Spark no se pausa y nunca cobra: al agotar la cuota, Firestore rechaza las peticiones hasta el día siguiente. El protocolo, los conflictos y las fases no cambian; solo el servidor.

### 4.0 Plan Spark de Firebase: límites y cómo se cubren

Cuotas aproximadas del plan Spark (**releerlas en [firebase.google.com/pricing](https://firebase.google.com/pricing) antes de lanzar**; no se han verificado aquí):

| Límite | Valor aproximado | Qué implica para Kiwu | Medida |
| --- | --- | --- | --- |
| Almacenamiento de Firestore | 1 GiB por proyecto | Solo viaja texto (tareas, listas, notas, horario), que ocupa muy poco | **Las imágenes y los archivos nunca se sincronizan** (decisión del 25 oct 2026): `Documento.paraSincronizar()` los quita |
| Escrituras | 20 000 al día **por proyecto** | Una primera subida de 5 000 tareas gasta un cuarto del día | Outbox compactada; primera subida en lotes repartidos; reintento al día siguiente |
| Lecturas | 50 000 al día por proyecto | Cada fila descargada cuenta; una consulta vacía cuenta 1 | `pull` incremental con cursor; sondeo cada 15 min como mínimo; sin escuchas en vivo salvo con la app visible |
| Salida de datos | 10 GiB al mes | Sobra con sincronización incremental | Compresión y descarga de imágenes bajo demanda |
| Firebase Storage | No disponible sin plan Blaze | No hace falta: no se suben archivos | — |
| **Copias de seguridad** | La exportación gestionada exige Blaze | Un error puede perder datos del servidor | **La copia local de cada dispositivo es la principal** (`.kiwu.json`); la nube nunca es el único sitio con los datos |
| Cuota diaria agotada | Error `RESOURCE_EXHAUSTED`, sin cobro | La sincronización se detiene unas horas | La app sigue 100 % local y avisa «límite diario del servidor, reintentando mañana» |

La cuota es **del proyecto, no de cada persona**: con decenas de cuentas nuevas el mismo día se agota. Ese es el momento de pasar a Blaze (con alerta de gasto); el protocolo no cambia. Cloud SQL, AlloyDB, Spanner, Bigtable y Memorystore se descartan: no tienen capa gratuita permanente.

### 4.1 Servidor

Firebase: **Authentication** y **Cloud Firestore** en el proyecto que ya aloja `kiwu.web.app`. Los tres clientes usan la **API REST** (Identity Toolkit y Firestore), así Python/Linux no necesita SDK; Android y macOS pueden usar el SDK solo para el inicio de sesión con Google o Apple y pasar el token al cliente REST.

Una colección por persona: `usuarios/{uid}/filas/{tabla}_{id}`. Cada documento lleva:

- `tabla` (`materia`, `deber`, `cuaderno`, `pagina`, `clase`, `falta`…), `datos` (los campos de la fila), `campos` (reloj híbrido de cada campo), `eliminada`, `dispositivo`.
- `servidor`: marca puesta por el servidor (`REQUEST_TIME`); es el cursor del `pull`.

Seguridad en `firestore.rules`, en lugar de RLS: solo `request.auth.uid == uid` lee y escribe su rama, con validación de las claves permitidas y del tamaño. Sin claves de servicio en los clientes.

### 4.2 Protocolo

- `push(cambios[])`: cada cambio lleva `tabla`, `id` (UUID, el mismo que hoy), `campos` modificados con su marca de tiempo híbrida y `eliminada`. Se envía con `commit` por lotes (hasta 200 escrituras); para el último-escritor-gana por campo se lee y escribe en una transacción que solo cambia los campos más recientes. Es **idempotente**: reenviar el mismo lote no cambia nada.
- `pull(desde)`: consulta `servidor > desde`, ordenada por `servidor`, en lotes de 200, y devuelve el nuevo cursor.
- Versión de protocolo en cada petición, para que un cliente antiguo reciba un «actualiza la app» y no datos a medias.

### 4.3 Resolución de conflictos

| Dato | Regla |
| --- | --- |
| Tareas, listas, cuadernos, clases | **Último escritor gana por campo** (reloj híbrido): editar el título en el móvil y la fecha en el PC conserva ambos |
| Borrado frente a edición | Gana la operación más reciente; el borrado es lógico y se puede restaurar durante 30 días |
| Contenido de una nota | Fusión **por bloque** (cada bloque tiene `id`): ediciones en bloques distintos se combinan; si el mismo bloque cambió en dos dispositivos, gana el más reciente y el otro texto se guarda como **copia de conflicto** («Título (conflicto 13 oct)») |
| Orden de tareas | Por posición fraccionaria, para que reordenar en un dispositivo no reescriba todas las filas |

El reloj híbrido (hora + contador + id del dispositivo) evita que un teléfono con la hora mal puesta gane siempre.

### 4.4 Cambios en cada plataforma

| Plataforma | Cambios previos | Cliente de sincronización |
| --- | --- | --- |
| **Android** (Room) | Migración 2→3: columnas de reloj y dispositivo, tabla `outbox`, tabla `estado_sync`; sin cambios en adjuntos | Kotlin + `WorkManager`; cliente HTTP ligero |
| **Linux** (SQLite) | Migración 2→3 equivalente | Python (`httpx`/`urllib`) en hilo propio |
| **macOS** (Core Data) | **Borrado lógico todavía sin hacer** (`eliminada` en Core Data) y migración ligera; requiere Xcode | Swift con `URLSession`; tarea en segundo plano |

El esquema local de Android y Linux ya tiene UUID estables y borrado lógico; macOS es el único que necesita ese paso antes de poder sincronizar.

### 4.5 Cuenta

- Pantalla «Cuenta» en Ajustes: iniciar sesión (correo y contraseña en los tres, porque el enlace por correo es incómodo en Linux; Google en Android; Apple en macOS) o seguir sin cuenta.
- Primera activación: se suben los datos locales con sus UUID (sin duplicados) y se descargan los del servidor; si ambos lados tienen datos, se **combinan** por UUID y se muestra el resumen antes de aplicar.
- Cerrar sesión no borra nada local. Borrar la cuenta borra los datos del servidor (requisito de Apple y Google): sin Cloud Functions en Spark, el propio cliente borra sus documentos por lotes y después el usuario.
- Tokens en Android Keystore / Keychain / Secret Service.

## 5. Fases, pruebas y criterios de cierre

| Fase | Contenido | Criterio de cierre |
| --- | --- | --- |
| **S0. Requisitos previos** | Adjuntos fuera del JSON de la nota; borrado lógico en macOS; modelo del horario definido; reloj híbrido y orden fraccionario | Las fixtures de `docs/contrato/` se leen igual en las tres plataformas; ninguna nota supera 100 KB por imágenes |
| **S1. Servidor** | Colección `filas`, `firestore.rules`, `push`/`pull`, límites y pruebas con el Firebase Emulator Suite | Dos clientes de prueba se sincronizan; reenviar un lote no duplica |
| **S2. Protocolo común** | Vectores de prueba en `docs/contrato/sync/` (conflictos de campo, borrado vs edición, nota con dos ediciones, campos desconocidos) que pasan las tres implementaciones | Misma salida en Kotlin, Python y Swift |
| **S3. Android** | Outbox, cliente, `WorkManager`, pantalla Cuenta | Presupuestos de la sección 3; la interfaz no pierde fotogramas durante una sincronización |
| **S4. Linux** | Igual en Python | Mismos presupuestos |
| **S5. macOS** | Igual en Swift | Mismos presupuestos; probado en un Mac |
| **S6. Notas** | Fusión por bloque, copia de conflicto | Edición simultánea de dos bloques distintos en dos dispositivos no pierde texto |
| **S7. Compartir (opcional)** | Listas compartidas con roles | Ver plan-desarrollo, fase 4 |

Pruebas transversales:

- **Independencia visual:** una prueba automática comprueba que los ajustes de apariencia no aparecen en ningún cambio enviado ni recibido.
- **Dos dispositivos simulados** en CI contra un servidor local: sin red, con red intermitente, con relojes desfasados y con versiones distintas de la app.
- **Rendimiento:** medición de la latencia de escritura y del arranque con y sin sincronización, en un teléfono real.

## 6. Orden recomendado

1. Definir el modelo del horario (solo el modelo) para que S0 lo incluya.
2. S0: sacar imágenes del JSON y arreglar el borrado en macOS.
3. S1 y S2 (servidor y protocolo con vectores de prueba).
4. S3 Android, que es donde hoy se puede probar, y S4 Linux.
5. S5 cuando haya un Mac.

Estimación orientativa para una persona: S0 2 semanas, S1–S2 3 semanas, S3 3 semanas, S4 2 semanas, S5 3 semanas, S6 2 semanas.

## 7. Decisiones

**Cerradas**

1. Servidor: **Firebase (Auth + Firestore) en plan Spark**, sin tarjeta (ver 4.0). Sustituye a Supabase, que se pausa por inactividad.
2. Coste: ninguno; se pasa a Blaze solo si la cuota diaria se queda corta.
3. Alcance de la primera versión: **solo sincronización propia**; compartir listas y la racha con amigos van después.

**Abiertas**

1. Si «compartir» entra en la primera versión de la cuenta o después.
2. Qué hacer con las imágenes ya incrustadas en notas existentes: migrarlas al primer arranque tras actualizar.
3. Marca y dominio antes de abrir cuentas a público (ver [marca-nombres.md](marca-nombres.md)): cambiar el nombre después de tener cuentas es más caro. El proyecto de Firebase se renombra o se crea con el nombre definitivo.

## 8. Estado de la implementación en Android (25 oct 2026)

Hecho y probado en un emulador con Firebase real (Spark): cuenta por correo y contraseña, subida de lo que ya había, descarga de lo que escribe otro dispositivo (en menos de un minuto con la app abierta), borrado de cuenta con datos y reglas de Firestore que rechazan a otros usuarios.

- **Simplificación de la primera versión:** gana **la fila completa** del último que la editó (`actualizada`), no cada campo. Una cola de SQLite llenada por *triggers* (`SyncSql`) marca qué filas cambiaron; no se tocó `KiwuDao`. `FusionSync` (por campo) queda escrito y probado para cuando se quiera pasar a campos.
- **Orden:** el motor (`MotorSync`) **baja primero y sube después**, para que un dispositivo que estuvo sin conexión no pise lo más reciente.
- **Cuándo sincroniza:** unos 3 s después de un cambio, al abrir la app y cada minuto mientras está a la vista. Con la app oculta no consulta.
- **No viaja:** imágenes, archivos, rutas del dispositivo (`materia.carpeta`), avisos ya mostrados (`deber.recordado`) ni las **faltas de clase** (no tienen marca de tiempo ni borrado lógico todavía).
- **Límites conocidos:** cada escritura propia vuelve en la siguiente descarga (1 lectura de cuota por cambio); entre bajar y subir hay una ventana corta en la que otro dispositivo podría escribir; la sesión se guarda en `SharedPreferences` privadas (falta Keystore); sin inicio con Google todavía; el orden fraccionario de tareas queda pendiente.
- **Configuración:** `firebase.apiKey` y `firebase.projectId` en `android/local.properties` (ver `local.properties.example`). Sin ellos la app compila y funciona sin cuenta.

### Google y Drive (Android)

- **Entrar con Google:** Credential Manager entrega un token de Google y Firebase lo canjea (`accounts:signInWithIdp`). Necesita `google.webClientId` en `android/local.properties` y, en Firebase, la app Android registrada con la huella SHA-1 de la clave con que se firma (la de depuración para probar, la de Play para publicar).
- **Copias en Drive, no en Firebase:** el mismo archivo `.kiwu.json` de la copia manual se guarda como un único archivo visible en «Mi unidad» (alcance `drive.file`: Kiwu solo ve lo que él creó). Funciona con cualquier cuenta de Google del teléfono, haya o no sesión de Kiwu. Requiere activar la API de Google Drive en el proyecto de Google Cloud y, mientras la pantalla de consentimiento esté en «pruebas», añadir las cuentas como usuarios de prueba.
