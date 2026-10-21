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
| Imágenes y archivos adjuntos (como blobs, ver 4.4) | Colores pastel de las tareas: se calculan localmente a partir del id, así cada plataforma los pinta con su paleta |
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
7. **Imágenes aparte.** Los adjuntos no viajan dentro del JSON de la nota (hoy `imageData` va incrustado en base64). Se suben una vez a almacenamiento por hash de contenido y la nota guarda solo la referencia; en el otro dispositivo se descargan al abrir la nota y se guardan en una caché con tope de tamaño. Es el requisito previo más importante para que una nota no pese megabytes en cada sincronización.
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

> **Decisión (13 oct 2026):** se usa **Supabase con el plan gratuito**; por ahora no se necesitan más procesos ni gasto. La sección 4.0 recoge lo que ese plan exige.

### 4.0 Plan gratuito de Supabase: límites y cómo se cubren

Límites del plan gratuito, leídos en [supabase.com/pricing](https://supabase.com/pricing) el 13 oct 2026 (conviene releerlos antes de lanzar):

| Límite | Valor | Qué implica para Kiwu | Medida |
| --- | --- | --- | --- |
| Base de datos | 500 MB por proyecto | Texto de tareas y notas ocupa muy poco: 5 000 tareas y 500 notas sin imágenes son unos pocos MB | Imágenes fuera de la base; vigilar el tamaño con una consulta mensual |
| Almacenamiento de archivos | 1 GB | Aquí irían las imágenes y los adjuntos de todas las personas | Reducir las imágenes antes de subirlas (como ya se hace con el fondo: máximo 1 600 px), tope de 5 MB por archivo y de 100 MB por cuenta, sin duplicados por hash |
| Salida de datos (*egress*) | 5 GB al mes | Cada sincronización descarga solo lo nuevo | Sincronización incremental, compresión y descarga de imágenes bajo demanda |
| Usuarios activos | 50 000 al mes | Sobra para empezar | — |
| **Pausa por inactividad** | **El proyecto se pausa tras 1 semana sin uso** | Si nadie lo usa en una semana (en pruebas, vacaciones), el servidor se apaga y la primera sincronización falla | Una tarea programada gratuita (GitHub Actions, cada 3 días) que hace una consulta sencilla; y la app muestra «servidor en pausa, reintentando» en vez de un error |
| **Sin copias de seguridad automáticas** | No incluidas | Un error del servidor o una migración mala puede perder datos | Copia semanal automática de la base (`pg_dump`) guardada en un repositorio privado o disco propio, y **la copia local de cada dispositivo sigue siendo la principal**: la nube nunca es el único sitio con los datos |
| Proyectos activos | 2 | Uno de pruebas y uno de producción | Dos proyectos: `kiwu-dev` y `kiwu-prod` |

Si el uso crece, el paso siguiente es el plan de pago de Supabase (copias diarias, sin pausa); el protocolo no cambia.

### 4.1 Servidor

Supabase (Postgres, Auth, Storage, Realtime), como ya se recomendó. Alternativa: Firebase, descartada por no tener cliente para Python/Linux y por su modelo de documentos. Decisión pendiente de confirmar (ver 7).

Tablas (todas con `usuario_id`, `actualizada` reloj híbrido, `eliminada`, `dispositivo`, `seq` incremental global por usuario):

- `materia`, `deber`, `cuaderno`, `pagina`, `clase` (horario), `blob` (adjuntos).
- Seguridad: *Row Level Security* en todas; cada persona solo ve sus filas. Sin claves de servicio en los clientes.

### 4.2 Protocolo

- `push(cambios[])`: cada cambio lleva `tabla`, `id` (UUID, el mismo que hoy), `campos` modificados con su marca de tiempo híbrida y `eliminada`. El servidor es **idempotente**: reenviar el mismo lote no cambia nada.
- `pull(desde)`: devuelve las filas con `seq > desde` y el nuevo cursor.
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
| **Android** (Room) | Migración 2→3: columnas de reloj y dispositivo, tabla `outbox`, tabla `estado_sync`; adjuntos a archivos con hash | Kotlin + `WorkManager`; cliente HTTP ligero |
| **Linux** (SQLite) | Migración 2→3 equivalente; adjuntos a archivos | Python (`httpx`/`urllib`) en hilo propio |
| **macOS** (Core Data) | **Borrado lógico todavía sin hacer** (`eliminada` en Core Data) y migración ligera; requiere Xcode | Swift con `URLSession`; tarea en segundo plano |

El esquema local de Android y Linux ya tiene UUID estables y borrado lógico; macOS es el único que necesita ese paso antes de poder sincronizar.

### 4.5 Cuenta

- Pantalla «Cuenta» en Ajustes: iniciar sesión (correo con enlace; Google en Android; Apple en macOS/iOS) o seguir sin cuenta.
- Primera activación: se suben los datos locales con sus UUID (sin duplicados) y se descargan los del servidor; si ambos lados tienen datos, se **combinan** por UUID y se muestra el resumen antes de aplicar.
- Cerrar sesión no borra nada local. Borrar la cuenta borra los datos del servidor (requisito de Apple y Google).
- Tokens en Android Keystore / Keychain / Secret Service.

## 5. Fases, pruebas y criterios de cierre

| Fase | Contenido | Criterio de cierre |
| --- | --- | --- |
| **S0. Requisitos previos** | Adjuntos fuera del JSON de la nota; borrado lógico en macOS; modelo del horario definido; reloj híbrido y orden fraccionario | Las fixtures de `docs/contrato/` se leen igual en las tres plataformas; ninguna nota supera 100 KB por imágenes |
| **S1. Servidor** | Esquema, RLS, `push`/`pull`, límites y pruebas con una base local de desarrollo | Dos clientes de prueba se sincronizan; reenviar un lote no duplica |
| **S2. Protocolo común** | Vectores de prueba en `docs/contrato/sync/` (conflictos de campo, borrado vs edición, nota con dos ediciones, campos desconocidos) que pasan las tres implementaciones | Misma salida en Kotlin, Python y Swift |
| **S3. Android** | Outbox, cliente, `WorkManager`, pantalla Cuenta | Presupuestos de la sección 3; la interfaz no pierde fotogramas durante una sincronización |
| **S4. Linux** | Igual en Python | Mismos presupuestos |
| **S5. macOS** | Igual en Swift | Mismos presupuestos; probado en un Mac |
| **S6. Notas e imágenes** | Fusión por bloque, copia de conflicto, caché de adjuntos | Edición simultánea de dos bloques distintos en dos dispositivos no pierde texto |
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

1. Servidor: **Supabase**, con el **plan gratuito** (ver 4.0). Firebase queda descartado.
2. Coste: ninguno por ahora; se revisa si hay uso amplio.

**Abiertas**

1. Si «compartir» entra en la primera versión de la cuenta o después.
2. Qué hacer con las imágenes ya incrustadas en notas existentes: migrarlas al primer arranque tras actualizar.
3. Marca y dominio antes de abrir cuentas a público (ver [marca-nombres.md](marca-nombres.md)): cambiar el nombre después de tener cuentas es más caro. El proyecto de Supabase se crea con el nombre definitivo.

## Alternativa futura: copia y sincronización con Google Drive

Idea para una mejora posterior, junto con las cuentas de usuario. No está decidida ni empezada.

- **Cómo:** la app guarda sus datos en la carpeta oculta de la aplicación en el Drive del usuario (`appDataFolder`, permiso `drive.appdata`) y se inicia sesión con la cuenta de Google del teléfono. Referencia: otras apps de tareas ofrecen «Sincronización de cuenta» con un interruptor de sincronización automática.
- **Coste:** sin servidor propio. Los datos usan la cuota de Drive del usuario. Solo requiere un proyecto gratuito de Google Cloud con la API de Drive, la pantalla de consentimiento de OAuth y un ID de cliente de Android.
- **Ventajas:** gratis, privado (los datos quedan en el Drive del usuario) y sin cuentas ni contraseñas propias.
- **Desventajas:** no es en tiempo real, hay que resolver los conflictos de edición en dos dispositivos y cada plataforma (Android, macOS, Linux) debe implementarlo por separado.
- **Orden sugerido:** 1) copia manual a Drive («Guardar copia» / «Restaurar») usando el formato de `docs/contrato/README.md`; 2) sincronización automática con resolución de conflictos; 3) decidir entonces si se mantiene Supabase o se usa Drive.
- **Relación con la monetización:** la compra de «quitar anuncios» es independiente; Google Play la asocia a la cuenta de Play Store del usuario sin necesitar este sistema (ver `plan-monetizacion.md`).
