# Plan de monetización de Kiwu (Android)

Política del proyecto: **sin pagos ni suscripciones** para los usuarios. La única vía es publicidad, lo menos invasiva posible. Se descartan los anuncios con recompensa porque dan a entender que hay algo «premium» detrás.

## Decisión: un banner pequeño y fijo

Una franja adaptable (~50 dp), de ancho completo, justo encima de la barra de navegación inferior. No se mueve, no interrumpe y no se superpone al contenido.

| Pantalla | ¿Banner? |
| --- | --- |
| Lista de notas | Sí |
| Tareas: Hoy, Próximos, Todas, Completadas y listas | Sí |
| Tareas: Calendario | No |
| Horario | No |
| Editor de notas y detalle de tarea | No (son pantallas aparte) |
| Ajustes | No |
| Menú lateral | No |

## Reglas

- **Primeros 4 días sin anuncios**, contados desde la primera apertura (`primer_uso` en las preferencias `kiwu`).
- **Nunca** con el teclado abierto: el banner se oculta mientras se escribe.
- **Sin hueco:** si el anuncio no carga o no hay conexión, no ocupa espacio.
- **Sin coste de arranque:** el SDK se inicia fuera del hilo principal y solo después de que la pantalla ya se ve. Debe medirse en el teléfono con la variante `minificado` (ver `lecciones-rendimiento.md`).
- Sin intersticiales, sin anuncios al abrir la app, sin temporizadores, sin recompensas.
- Una opción futura, solo si hace falta más ingreso: tarjeta de anuncio nativo en las listas largas.

## Estado de la implementación (rama `anuncios-prueba`)

- Dependencia `play-services-ads`, ID de aplicación y bloque de banner de **PRUEBA** de Google.
- `ui/Anuncios.kt`: composable `BannerAnuncio`; `ui/Raiz.kt` lo coloca sobre la barra inferior.
- Los días sin anuncios son `0` en depuración y minificado (para poder verlos) y `4` en release (`DIAS_SIN_ANUNCIOS`).

## Pendiente antes de publicar en Play Store

1. Cuenta de desarrollador de Google Play (pago único de 25 USD) y cuenta de AdMob (cobro a partir de 100 USD).
2. Sustituir el ID de aplicación y el bloque de banner de prueba por los reales (`AndroidManifest.xml` y `Anuncios.kt`). **No usar nunca anuncios reales en las pruebas.**
3. Consentimiento de privacidad (UMP / GDPR) antes de pedir anuncios.
4. Política de privacidad y formulario «Seguridad de los datos» de Play Console (declarar el identificador de publicidad).
5. Declarar el público (mayores de 13 años) y limitar la clasificación de contenido de los anuncios.
6. Comprobar en el teléfono que el banner no empeora la fluidez ni tapa el botón «+» ni el campo «Añadir tarea».
7. Decidir si el modo sin conexión de la app necesita una nota en la ficha de la tienda («contiene anuncios»).

## Expectativas

Un banner solo rinde poco por usuario; la rentabilidad depende del volumen y de la retención. Medir antes de añadir formatos nuevos.
