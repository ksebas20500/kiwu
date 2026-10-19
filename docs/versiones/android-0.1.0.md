## Kiwu para Android 0.1.0 · primera versión de prueba

Primera versión de Kiwu para Android: las mismas funciones que la versión de escritorio (sin widgets) más un **horario semanal** nuevo. Es una **versión de prueba**: todavía no está en Google Play y el identificador y el nombre de la app son provisionales.

### Archivos

| Archivo | Para qué sirve |
| --- | --- |
| `kiwu-android-0.1.0.apk` | **Recomendado.** Versión optimizada con R8 (~2 MB), la que se comporta como la versión final. Identificador `app.kiwu.android.min` |
| `kiwu-android-0.1.0-debug.apk` | Versión de depuración (~18 MB). Más lenta; solo para diagnosticar fallos. Identificador `app.kiwu.android` |
| `SHA256SUMS.txt` | Sumas de verificación de los dos APK |

Las dos versiones se pueden instalar a la vez: tienen identificadores distintos y **no comparten datos**.

### Instalación

1. Requisitos: Android 8.0 o superior (API 26).
2. Descarga el APK en el teléfono y ábrelo. Android pedirá permiso para «instalar apps de fuentes desconocidas» para tu navegador o gestor de archivos.
3. Al abrir la app por primera vez, concede el permiso de notificaciones si quieres recordatorios. En Android 12 o superior, para que los avisos suenen a la hora exacta, activa «Alarmas exactas» desde Ajustes → Recordatorios.
4. Comprueba la descarga: `sha256sum -c SHA256SUMS.txt` (Linux/macOS) o `Get-FileHash <archivo>` (PowerShell).

> Los APK van firmados con la clave de depuración, no con una clave de publicación. Un APK de otra procedencia (por ejemplo, compilado en otro equipo) no se podrá instalar encima de este sin desinstalar antes, y al desinstalar se pierden los datos: **haz antes una copia desde Ajustes → Exportar**.

### Qué incluye

**Tareas**
- Listas (materias), tareas con fecha y hora de entrega, y las vistas Hoy, Próximos 7 días, Todas y Completadas.
- Botón de calendario en «Añadir tarea» para elegir la fecha de entrega al crearla.
- Tablero (Por hacer · En curso · Hecho) con arrastre en pantallas anchas; en el teléfono siempre se ve la lista.
- Detalle de tarea con nota enriquecida, fecha, aviso y estado.

**Calendario**
- Mes completo con las entregas como puntos de colores pastel; el detalle del día elegido va debajo y desde ahí se puede añadir una tarea con su hora.

**Notas**
- Cuadernos, páginas, favoritas y búsqueda.
- Editor por bloques con menú «/»: párrafos, títulos, listas con viñetas, numeradas y de tareas con sangría, citas, destacados, divisores y bloques de código con colores.
- Formato en línea (negrita, cursiva, subrayado, tachado, código, color, resaltado y enlaces), enlaces con vista previa, vídeo, imágenes y archivos adjuntos.

**Horario** (novedad)
- Varios horarios (Universidad, Trabajo, Ocio… con nombre e icono) y vista de todos juntos.
- Clases o actividades en varios días a la vez, con hora de inicio y de fin (ruedas de 5 en 5 minutos, como la alarma del reloj), aula, notas y aviso opcional (5 a 60 minutos antes).
- Vista por día (teléfono) o por semana, tarjeta «Ahora / Siguiente» y aviso de choques entre clases, también entre horarios distintos.

**Archivos por lista**
- Carpeta del dispositivo vinculada a cada lista, con explorador integrado; los archivos se abren con otras apps y no se copian.

**Recordatorios**
- Notificaciones con las acciones «Completar» y «Posponer 1 h», alarmas exactas con alternativa, y reprogramación al reiniciar el teléfono.

**Ajustes**
- Tema claro, oscuro o del sistema; **color de la app** a elección (8 colores, colores del sistema en Android 12+ y selector libre); fondo de imagen con visibilidad y velo.
- Copia de seguridad `.kiwu.json`, el mismo formato que usa Kiwu para Linux.

### Cambios desde las primeras pruebas

- El editor ya no se cierra al mantener pulsado «borrar» en un bloque vacío.
- El arranque ya no parpadea: pantalla de arranque con el tema correcto y ajustes cargados antes del primer fotograma.
- Menú lateral a pantalla completa (cubre la barra inferior) y con cabecera; en cada módulo, el menú se cierra con su animación antes de cambiar de pantalla.
- Corregido: Horario → Notas abría el menú de Notas por sí solo.
- Corregido: la app se cerraba al editar una clase que termina a las 24:00; las horas de las clases a veces quedaban en 11:59 o 12:01.
- La foto de fondo se prepara una vez a la medida de la pantalla (menos memoria y menos trabajo por fotograma); ya no puede cerrar la app si falta memoria.
- Mejoras del calendario (puntos de color por tarea, detalle del día con desplazamiento) y del contraste de los colores de la app.

### Datos y compatibilidad

- Todos los datos se guardan solo en el dispositivo (sin cuentas, sin servidor, sin publicidad ni analíticas). Internet se usa únicamente para descargar la vista previa de los enlaces que pegas en una nota.
- Base de datos en versión 2 (añade horarios y clases) con migración desde la versión anterior, comprobada con una prueba automática.
- El formato de las notas y de la copia `.kiwu.json` es el de Linux y macOS (`docs/contrato`). La copia admite ahora las claves opcionales `horarios` y `clases`; las copias antiguas siguen importándose.

### Verificación

- 104 pruebas automáticas (dominio, contrato con macOS y Linux, copia de seguridad, Room y migración, editor, horario, tablero y colores), sin fallos; lint sin errores.
- Variante optimizada (R8) compilada y probada en un emulador y, en parte, en un teléfono real (vivo V2418, Android 16).

### Actualización posterior (mismo 0.1.0)

- Transiciones suaves entre Tareas, Notas y Horario, entre la vista de día y la de semana, y entre Calendario y el resto de secciones de Tareas.
- Arreglo: al elegir una fecha en «Añadir tarea», el recuadro ya no se ensancha a lo alto.

### Limitaciones conocidas

- **Sin sincronización** entre dispositivos todavía: está planificada con Supabase (ver `docs/plan-sincronizacion.md`).
- El tablero no se ofrece en teléfonos (solo en pantallas anchas).
- Sin probar en un dispositivo: avisos de clase y de tareas tras reiniciar, vista semanal en tabletas, adjuntos (imágenes y archivos), vista previa de enlaces y vídeo, y TalkBack.
- Los textos están escritos en español en el código: aún no hay traducción.
- La fluidez en el teléfono con la versión de depuración es notablemente peor que con la optimizada; para evaluarla, usa `kiwu-android-0.1.0.apk`.
- Mientras se decide el nombre definitivo de la marca (ver `docs/marca-nombres.md`), el nombre y el identificador pueden cambiar.

### Documentación

- Plan y estado de Android: `docs/android-plan.md` y `docs/android-ruta-correccion.md`
- Horario: `docs/plan-horario.md` · Sincronización: `docs/plan-sincronizacion.md` · Marca: `docs/marca-nombres.md`
- Contrato de datos compartido: `docs/contrato/README.md`
