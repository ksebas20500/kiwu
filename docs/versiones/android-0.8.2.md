# Kiwu para Android 0.8.2 · Google Calendar y portugués completo

Se instala encima de la 0.8.1 y conserva los datos. La base de datos no cambia.

### Novedades

- **📅 Google Calendar en la app:** tus eventos de Google Calendar ahora aparecen en el calendario de Kiwu, tanto en la vista mensual (puntos de color en cada día) como en la agenda diaria, junto a tus tareas. Al abrir el calendario hay un botón para dar el permiso de calendario; sin permiso, Kiwu funciona igual y solo muestra tus tareas.
- **🇵🇹 Portugués al 100%:** la app entera está traducida al portugués (728 textos, los mismos que en español e inglés), con los plurales y la terminología propias del idioma (racha = «sequência»). Ya no quedan frases sueltas en español.
- **⚡ Más rápida y eficiente:** si compraste «Quitar anuncios», el motor de publicidad ya no se carga en absoluto: menos memoria, datos y batería.
- **🔐 Más seguridad:** tus tokens de sesión quedan fuera de las copias automáticas en la nube de Android y de las transferencias entre teléfonos; el resto de tus datos (tareas, notas, horario) sí se respalda como antes.

### Arreglos

- Widgets más consistentes: el refresco interno se unificó en un solo camino, evitando redibujos repetidos.
- Un aviso del lint de Compose marcaba por error el calendario como defectuoso; se corrigió con su explicación documentada.

### Para tener en cuenta

- Los eventos de Google Calendar salen del calendario del teléfono: si tu cuenta no los sincroniza en el dispositivo, Kiwu no puede enseñarlos.
- No hay cambios de base de datos: las copias `.kiwu.json` siguen siendo compatibles entre versiones.
