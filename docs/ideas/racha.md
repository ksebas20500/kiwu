# Racha: de «tareas completadas» a «al día»

## Problema

¿Cómo podríamos reforzar el hábito de organizarse con Kiwu sin premiar tareas triviales ni castigar los días en que no hay nada pendiente?

Hoy (`android/.../domain/Racha.kt`, Android 0.4.1) un día cuenta si se completa al menos una tarea. Fallos detectados: se mantiene marcando una tarea tonta, los días libres rompen la racha, notas y horario no suman, y el aviso de las 20:00 genera presión.

Público: estudiantes. Éxito: que abran la app a diario, sin ansiedad.

## Dirección recomendada

Tres ideas que se componen en capas, no compiten:

- **3 · «Al día» (qué vale un día).** Un día cuenta si al terminar no queda nada vencido. Mide planificación, no volumen: marcar una tarea trivial deja de servir.
- **2 · Días exigentes (qué días cuentan).** Un día sin clases ni entregas ni cuenta ni rompe (es neutro). Usa datos que Kiwu ya tiene: horario y fechas de entrega.
- **1 · Semanal (cómo se agrega).** La constancia se lee por semana (por ejemplo ≥ N días cumplidos de los exigentes). Un mal día no rompe nada y hace innecesario el «protector de racha» pendiente de 0.4.1.

Orden de construcción: 3 + 2 primero (cambian la regla), 1 después (cambia la presentación), con datos reales en mano.

## Supuestos a validar

- [ ] «Vencido el día d» se puede reconstruir del historial sin campo nuevo: vencida si `fecha_entrega < d` y (no completada, o `fecha_completado` posterior a d). Comprobar que los borrados lógicos conservan esas fechas.
- [x] Una racha de «al día» sigue empujando a abrir la app a diario. Resuelto en la prueba: crear un horario cumplía días solo. Ahora un día con clase solo cuenta si ese día se completa o se añade una tarea (límite conocido: una tarea trivial basta).
- [ ] Las tareas sin fecha de entrega son la mayoría. Si es así, «al día» casi nunca se rompe y la racha pierde sentido. Medir en datos propios.
- [ ] Quien tiene el horario vacío o pocas fechas no se queda con todos los días neutros y una racha que no avanza.
- [ ] Quitar la presión nocturna no mata el hábito: el aviso pasa a ser solo «tienes N cosas vencidas», no «no pierdas tu racha».

## MVP

Dentro:
- `Rachas.calcular` con la regla «sin vencidos al cierre del día» y días neutros (sin clases ni entregas).
- Mismo calendario, metas y celebración que ya existen.
- Aviso reformulado (solo si hay vencidos), mismo canal, ajuste existente.
- Tests puros en `RachaTest`: vencido reconstruido, día neutro, tarea completada tarde, borrada, zona horaria.

Fuera:
- Vista semanal (capa 1), a decidir tras ver el comportamiento de la regla.

## No haremos (por ahora)

- **Actividad amplia (notas, horario cuentan como señal):** más señales no arreglan una métrica manipulable.
- **Sin racha, solo huella:** queda como salida si «al día» no convence.
- **Racha por materia:** cambia a quién mide; es otro producto.
- **Ritual semanal / cierre de domingo:** buena idea, pero es otro tema.
- **Racha con amigos:** sigue aplazada (`docs/plan-0.4.1.md`); no se diseña hasta fijar la regla.
- **macOS y Linux:** hoy no tienen racha; no se portan hasta que la regla esté estable en Android.

## Preguntas abiertas

- ¿Un día con tareas completadas pero también vencidos cuenta o no?
- ¿Qué pasa con las tareas sin fecha: cuentan como «vencibles» o se ignoran?
- ¿La regla se retroactiva sobre la racha actual de los usuarios (puede bajarles o subirles la cifra)?
- ¿«Exigente» incluye los eventos con fecha o solo tareas y clases?
- ¿Se conserva el nombre «racha» o pasa a «al día»?

## Ruta (próximos pasos)

1. Comprobar el supuesto 1 sobre el esquema Room (`KiwuDao`, borrado lógico).
2. Medir en datos propios: % de tareas con fecha de entrega.
3. Decidir las preguntas abiertas 1 a 3 (afectan a la regla).
4. `/agent-skills:spec` de la regla nueva, luego `/agent-skills:plan` y TDD sobre `RachaTest`.
5. Evaluar la capa semanal con la regla ya en uso.
