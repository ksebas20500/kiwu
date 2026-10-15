package app.kiwu.android.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.almacen by preferencesDataStore(name = "ajustes")

/** Preferencias de la persona usuaria (equivalente a `settings.json` en Linux, sin los atajos de teclado). */
data class AjustesUi(
    /** sistema | claro | oscuro */
    val tema: String = "sistema",
    /** Color de la app: nombre de un acento predefinido, `sistema` (colores dinámicos) o `#RRGGBB`. */
    val acento: String = "coral",
    /** lista | tablero */
    val vistaTareas: String = "lista",
    /** Imagen de fondo: dirección `content://` con permiso persistente, o null. */
    val fondoUri: String? = null,
    /** 0..1: cuánto se ve la imagen de fondo. */
    val fondoVisibilidad: Float = 0.5f,
    /** 0..1: velo que cubre la imagen para que el contenido se lea. */
    val opacidadPaneles: Float = 0.45f,
)

class AjustesStore(private val contexto: Context) {
    private object Claves {
        val tema = stringPreferencesKey("tema")
        val acento = stringPreferencesKey("acento")
        val vista = stringPreferencesKey("vista_tareas")
        val fondo = stringPreferencesKey("fondo_uri")
        val visibilidad = floatPreferencesKey("fondo_visibilidad")
        val opacidad = floatPreferencesKey("opacidad_paneles")
    }

    private fun leer(p: Preferences) = AjustesUi(
        tema = p[Claves.tema] ?: "sistema",
        acento = p[Claves.acento] ?: "coral",
        vistaTareas = p[Claves.vista] ?: "lista",
        fondoUri = p[Claves.fondo],
        fondoVisibilidad = (p[Claves.visibilidad] ?: 0.5f).coerceIn(0f, 1f),
        opacidadPaneles = (p[Claves.opacidad] ?: 0.45f).coerceIn(0f, 1f),
    )

    val flujo: Flow<AjustesUi> = contexto.almacen.data.map(::leer)

    suspend fun cambiar(transformar: (AjustesUi) -> AjustesUi) {
        contexto.almacen.edit { p ->
            val nuevo = transformar(leer(p))
            p[Claves.tema] = nuevo.tema
            p[Claves.acento] = nuevo.acento
            p[Claves.vista] = nuevo.vistaTareas
            if (nuevo.fondoUri == null) p.remove(Claves.fondo) else p[Claves.fondo] = nuevo.fondoUri
            p[Claves.visibilidad] = nuevo.fondoVisibilidad
            p[Claves.opacidad] = nuevo.opacidadPaneles
        }
    }
}
