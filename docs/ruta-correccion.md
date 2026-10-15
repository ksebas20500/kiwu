# Ruta de corrección de falencias

Ordenada de **más grave a más leve**. Los números de falencia remiten a la sección 2 de [plan-desarrollo.md](plan-desarrollo.md). Estado a 2026-10-13.

Leyenda: ✅ resuelta en el repo · 🟡 mitigada, falta verificar en un Mac · ⏳ pendiente (requiere decisión, cuenta de pago o trabajo grande)

> **Verificación.** Este trabajo se hizo en un equipo Windows sin Python, Swift ni Xcode, así que **nada se ejecutó localmente**. Las pruebas nuevas corren en CI (`.github/workflows/`). El primer paso al volver a un Mac/Linux es mirar el resultado de esos flujos y corregir lo que falle.

| # | Gravedad | Falencia | Corrección | Estado |
| --- | --- | --- | --- | --- |
| 1 | Crítica | Sin CI/CD (falencia 2) | Flujos de GitHub Actions: pruebas de Linux, compilación de macOS y prueba de contrato | ✅ |
| 2 | Crítica | Sin pruebas en macOS y núcleo duplicado (1, 6) | Fixtures comunes en `docs/contrato/` que leen Python (`unittest`) y Swift (`Tools/probar-contrato.sh`), con resultado esperado idéntico | ✅ (Swift 🟡) |
| 3 | Crítica | Datos no aptos para sincronizar (3) | Linux: esquema v2 con **borrado lógico** (`eliminada`), migración desde v1 y borrado en cascada lógico | ✅ (Linux) |
| 3b | Crítica | Ídem en macOS | Añadir `eliminada` al modelo de Core Data. **No se hizo a ciegas**: el modelo se define por código y sin versión previa guardada, una migración errónea impediría abrir la base de datos de los usuarios. Necesita Xcode y una copia real de datos | ⏳ |
| 4 | Crítica | `Materia.orden` por reloj en macOS (3) | `max(orden)+1`, igual que Linux | 🟡 |
| 5 | Crítica | Notas con imágenes embebidas en un único JSON (4) | Requiere almacén de blobs + migración + sync. Depende de 3b | ⏳ |
| 6 | Crítica | Datos locales de plataforma (bookmarks) mezclados con los sincronizables (5) | Se resuelve con el campo `solo_local` al diseñar la sync (fase 2 del plan) | ⏳ |
| 7 | Importante | Errores de persistencia en macOS silenciosos o con `fatalError` (7) | Aviso al usuario al fallar la carga y al guardar, con registro del error | 🟡 |
| 8 | Importante | Esquema de Linux sin migraciones (7) | `_migrar` por pasos (v1 → v2) con prueba | ✅ |
| 9 | Importante | Distribución sin firma ni notarización (8) | Requiere Apple Developer (99 USD/año). Se deja el flujo de CI listo para añadir los secretos | ⏳ |
| 10 | Importante | Tres nombres históricos en los identificadores (9) | Decisión de marca y de identificador definitivo **antes** de publicar en tiendas | ⏳ |
| 11 | Importante | Producto solo para el ámbito universitario (10) | Decisión de producto + vocabulario configurable | ⏳ |
| 12 | Media | Cadenas en español dentro del código (13) | Catálogos es/en. Cambio transversal y grande; mejor tras estabilizar el modelo | ⏳ |
| 13 | Media | Linux solo Arch + Hyprland (11) | Flatpak, tras el CI | ⏳ |
| 14 | Leve | `.gif` de 7,5 MB en git (12) | Reducirlo con una herramienta de imagen (no disponible aquí) | ⏳ |
| 15 | Leve | URLs fijas al usuario `ksebas20500` (12) | Se resuelven al fijar dominio propio | ⏳ |
| 16 | Leve | Sin cifrado de la base local (14) | Decisión de producto; ver plan §4 | ⏳ |

Correcciones al diagnóstico original: `docs/media/LEEME.md` no es un duplicado del README (es una guía para sacar capturas), así que no hace falta unificarlo.

## Qué cambió en el repo

- `.github/workflows/linux.yml`, `macos.yml`: CI.
- `docs/contrato/`: fixtures y reglas del formato común de notas.
- `linux/tests/test_contrato.py`, `linux/tests/test_logica.py`: pruebas de contrato y de borrado lógico/migración.
- `linux/kiwu/db.py`: esquema v2.
- `macos/Tools/probar-contrato.sh`, `macos/Tools/probar-contrato.swift`: prueba de contrato en Swift.
- `macos/Kiwu/Services/DeberService.swift`: orden de las listas.
- `macos/Kiwu/Persistence/PersistenceController.swift`: errores de persistencia.

## Siguiente paso recomendado

1. Subir los cambios y revisar los tres flujos de CI; corregir lo que falle.
2. En un Mac: abrir el proyecto, compilar y ejecutar `macos/Tools/probar-contrato.sh`. Probar abrir la app con una copia de tus datos reales.
3. Diseñar y probar en Xcode la migración de Core Data de la fila 3b, con una copia de seguridad previa.
4. Decidir marca, dominio y backend (filas 10, 11 y las preguntas del plan §7) para abrir la fase de servidor.
