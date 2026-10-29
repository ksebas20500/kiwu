## Kiwu para Android 0.7.1 · tarjetas en el apunte y arreglos

Se instala encima de la 0.7.0 y conserva los datos. La base de datos no cambia.

### Novedades

- **Tarjeta de repaso dentro del apunte:** escribe `/tarjeta` en una nota y elige «Tarjeta de repaso». Tiene la pregunta arriba y la respuesta abajo; Intro en la pregunta pasa a la respuesta. Para quitarla, su menú ⋮ → «Eliminar bloque».
- **`/repasar`:** abre el repaso de la nota sin salir del editor. Allí salen juntas las tarjetas escritas en el apunte («En el apunte») y las añadidas a mano.
- **Generar tarjetas desde el apunte (sin IA):** en el repaso, «Generar desde el apunte» propone tarjetas a partir de cómo está escrito: un título con su explicación debajo, una pregunta que termina en «?» seguida de su respuesta, líneas «Término: definición» y palabras en **negrita** (la frase queda con un hueco). Llegan todas marcadas; desmarcas las que no quieras y pulsas «Añadir».
- **Barra de formato más corta:** se quitaron los botones de código en línea y de sangría, que no ayudaban al tomar apuntes.

### Arreglos

- **Notificaciones que se quedaban:** al completar o borrar una tarea (también desde otro dispositivo), su aviso desaparece de la barra de notificaciones.
- **Tiempo vencido en horas y días:** el aviso decía «Venció hace 840 min». Ahora cuenta minutos hasta 59, luego horas («hace 1 hora», «hace 14 horas») y desde las 24 horas, días («hace 2 días»).
- **Calendario:** al escribir una tarea el teclado ya no aplasta el campo ni oculta el texto, y el mes sigue a la vista; cambiar de lista ya no hace parpadear la pantalla.
- **Notas:**
  - el aviso «Escribe aquí o usa / para insertar bloques» ya no sale dos veces;
  - borrar una línea vacía ya no cierra y vuelve a abrir el teclado;
  - al volver del repaso, la nota muestra lo último escrito: antes podía aparecer la versión del momento de abrirla y, si seguías escribiendo, lo nuevo se perdía.

### Para tener en cuenta

- Las tarjetas generadas son propuestas: dependen de que el apunte tenga títulos, preguntas, «Término: definición» o negritas.
- Con el teclado abierto, el campo de añadir del calendario tapa la lista del día (el mes sigue visible).
- Una tarjeta con algo escrito no se borra con la tecla de borrar, para no perder la respuesta; se quita desde su menú ⋮.

### Para quien compila

Sin cambios respecto a la 0.5.0.
