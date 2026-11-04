# Kiwu: kotlinx.serialization y Room traen sus propias reglas.

# Glance guarda en disco qué widget corresponde a cada receptor usando el nombre de la clase (`javaClass.canonicalName`).
# Sin esta regla R8 fusiona DeberesWidget, HorarioWidget y NotasWidget en una sola clase: los tres comparten nombre y
# `updateAll` de uno dibuja su contenido en los tres widgets puestos.
-keep class * extends androidx.glance.appwidget.GlanceAppWidget
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver
