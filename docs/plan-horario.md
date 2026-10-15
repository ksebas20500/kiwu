# Horario de clases y horario general: viabilidad y plan

## 1. Respuesta corta

**Es muy viable** y encaja con lo que ya existe: las materias ya son «listas», hay recordatorios con alarmas, la barra inferior admite una tercera pestaña y las tres plataformas ya comparten el contrato de datos. Es una función **independiente del calendario**: el calendario muestra entregas con fecha concreta; el horario muestra una **semana que se repite** (por ejemplo, «Cálculo: lunes y miércoles, 8:00–10:00»).

Esfuerzo orientativo: Android 1,5 a 2 semanas; Linux 1 a 1,5; macOS 1 a 1,5 (sin poder probarlo desde Windows). Es una fase corta y conviene hacerla **antes de la sincronización** para que el modelo viaje a la nube desde el principio.

## 2. Qué se podría hacer

| Función | Primera versión | Después |
| --- | --- | --- |
| Pestaña «Horario» abajo, junto a Tareas y Notas | ✅ | |
| Crear una clase: materia, días de la semana, hora de inicio y de fin, aula (opcional) | ✅ | Profesor, notas |
| Una misma clase en varios días a la vez (lunes y miércoles) | ✅ | |
| Actividades que no son clases («Gimnasio», «Trabajo») con el mismo formulario | ✅ | Categorías con icono |
| Vista semanal con bloques de color por materia | ✅ (en tableta) | |
| Vista de teléfono: un día a la vez (pestañas L M X J V S D) con deslizamiento lateral | ✅ | Vista de 3 días |
| Tarjeta «Ahora» y «Siguiente clase» arriba | ✅ | |
| Aviso antes de cada clase (por ejemplo, 10 minutos) | ✅ opcional por clase | |
| Aviso de choque si dos clases se pisan | ✅ (advertencia, no bloquea) | |
| Vincular la clase a una lista de tareas existente | ✅ | Abrir las tareas de esa materia desde la clase |
| Periodos (semestre con fecha de inicio y fin) | | ✅ para ocultar clases de periodos pasados |
| Mostrar las clases también en el calendario | | ✅ opcional, desactivado por defecto |
| Importar desde un archivo de horario (`.ics`) | | ✅ |

## 3. Modelo de datos

Dos entidades: `Horario` (nombre, icono, orden) y `Clase`, que pertenece a un horario. En la copia `.kiwu.json` se llaman `horarios` y `clases`. La clase:

| Campo | Tipo | Notas |
| --- | --- | --- |
| `id` | UUID en mayúsculas | Igual que el resto |
| `horarioId` | UUID | El horario al que pertenece (Universidad, Trabajo…) |
| `materiaId` | UUID o nulo | Lista de tareas relacionada; nulo si es una actividad suelta |
| `titulo` | texto | Se rellena con el nombre de la materia si hay una vinculada |
| `diaSemana` | 1–7 (lunes = 1) | **Una fila por día**: una clase de lunes y miércoles son dos filas con la misma agrupación |
| `grupo` | UUID | Une las filas de un mismo horario para editarlas a la vez |
| `inicioMin` / `finMin` | minutos desde las 00:00 (0–1439) | **Hora «flotante»**, sin zona horaria: «8:00» es 8:00 donde estés |
| `aula`, `notas` | texto | Opcionales |
| `avisoMin` | entero o nulo | Minutos de antelación; nulo = sin aviso |
| `periodoDesde` / `periodoHasta` | fechas o nulas | Para semestres (fase posterior) |
| `eliminada`, `actualizada` | borrado lógico y fecha | Para la sincronización |

Decisiones:

- **Hora flotante en minutos**, no instantes: un horario escolar no cambia al cambiar de huso, y evita errores de horario de verano.
- **El color no se guarda**: sale de la materia (o del id de la clase), calculado por cada plataforma con su paleta, igual que los pasteles de las tareas. Así el diseño de una plataforma no condiciona a la otra y no hay que sincronizar estilos.
- Validación: fin posterior al inicio; cruzar la medianoche se resuelve en dos filas.

## 4. Pantallas (Android)

1. **Horario** (tercera pestaña): arriba «Ahora / Siguiente»; debajo, pestañas de día con la lista de clases ordenadas por hora; botón «Añadir clase». En tableta, cuadrícula semanal con eje de horas.
2. **Añadir/editar clase:** materia (elegir una lista o escribir un nombre nuevo), días (siete botones que se pueden activar a la vez), hora de inicio y fin, aula, aviso. Si choca con otra, aviso en el propio formulario.
3. **Detalle:** al tocar un bloque, editar, duplicar o eliminar (confirmación).
4. **Ajustes:** día en que empieza la semana y rango de horas visible (por ejemplo, 6:00–22:00).

Rendimiento: la pantalla lee una consulta pequeña por día; el dibujo de bloques usa posiciones calculadas una vez. Sin trabajo en el hilo principal.

## 5. Avisos

- Una alarma exacta por clase y día, reprogramada tras cada disparo y al reiniciar, con la misma estructura que los avisos de tareas (`ProgramadorAvisos`).
- Para no saturar: máximo 7 días por delante y recalcular al abrir la app.
- Respeta el permiso de notificaciones y el de alarmas exactas que ya pide la app.

## 6. Plan de desarrollo

| Fase | Contenido | Criterio de cierre |
| --- | --- | --- |
| **H0. Contrato** | Definir `scheduleBlock` en `docs/contrato/` con fixtures y copiar/importar `.kiwu.json` | Las tres plataformas leen y escriben la misma fixture |
| **H1. Android: datos** | Entidad, migración 2→3 de Room, repositorio, reglas (choques, «ahora/siguiente») con pruebas unitarias | Pruebas verdes; migración probada desde la versión actual |
| **H2. Android: pantallas** | Pestaña, lista por día, formulario, detalle, vista semanal en tableta | Probado en un teléfono: crear, editar, borrar y ver el cambio de día |
| **H3. Avisos** | Alarmas por clase | La notificación llega a la hora (probado en dispositivo) y sobrevive a reiniciar |
| **H4. Linux** | Tabla, vista semanal en GTK, formulario | Pruebas y comprobación manual |
| **H5. macOS** | Entidad en Core Data, vista semanal en SwiftUI | Probado en un Mac |
| **H6. Mejoras** | Periodos, `.ics`, mostrar en el calendario | Según prioridad |

## 7. Riesgos

- **Distinguir «horario» de «calendario»** para la persona usuaria: se resuelve con nombres claros («Horario semanal» frente a «Calendario de entregas») y con la tarjeta «Ahora».
- **Migración de datos:** al añadir una tabla, Android y Linux deben migrar sin perder nada (ya hay pruebas de migración como modelo).
- **macOS** no se puede probar desde Windows: se programa con el mismo contrato y se verifica cuando haya un Mac.
- **Pantallas pequeñas:** una cuadrícula semanal no cabe en un teléfono; por eso la vista del teléfono es por día.

## 8. Decisiones (13 oct 2026)

- **Varios horarios:** cada persona puede crear los que quiera («Universidad», «Trabajo», «Ocio»…), cada uno con nombre e icono, y verlos por separado o todos juntos.
- **Actividades que no son clases:** sí, con el mismo formulario y la materia opcional.
- **Avisos:** desactivados por defecto; se activan por clase (5, 10, 15, 30 o 60 minutos antes).
- **Plataforma:** primero Android (donde se puede probar); Linux y macOS después, con el mismo contrato.

## 9. Estado

| Fase | Estado |
| --- | --- |
| H0. Contrato | Hecho para la copia `.kiwu.json` (claves `horarios` y `clases`, ver [contrato](contrato/README.md)); faltan fixtures compartidas con Linux y macOS |
| H1. Android: datos | Hecho: tablas `horario` y `clase`, migración 1→2 probada, repositorio, reglas con pruebas |
| H2. Android: pantallas | Hecho y comprobado en un emulador: pestaña Horario, chips de horarios, vista por día y por semana, formulario, «Ahora / Siguiente», aviso de choques |
| H3. Avisos | Implementado (alarma por clase, se reprograma cada semana); **sin probar en dispositivo** |
| H4. Linux | Pendiente |
| H5. macOS | Pendiente |
| H6. Mejoras | Pendiente (periodos, `.ics`, mostrar en el calendario) |


- ¿«Horario general» incluye actividades que no son clases (gimnasio, trabajo) con el mismo formulario? Propuesta: **sí**, con la materia opcional.
- ¿Los avisos son por defecto (10 min antes) o desactivados? Propuesta: **desactivados** hasta que la persona los active por clase.
