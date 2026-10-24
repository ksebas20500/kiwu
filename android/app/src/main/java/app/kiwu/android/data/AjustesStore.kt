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
    /** Segundo aviso: minutos antes de la entrega (0 = sin segundo aviso). Vale para todas las tareas con fecha. */
    val segundoAvisoMin: Int = 0,
    /** Aspecto del menú de Tareas: plegable | compacto | color. */
    val menuAspecto: String = "plegable",
    /** Orden de las listas del menú: manual | alfabetico | pendientes. */
    val menuOrden: String = "manual",
    /** Si el menú muestra el número de tareas pendientes. */
    val menuContadores: Boolean = true,
    /** Si el menú de Tareas muestra la racha. */
    val rachaVisible: Boolean = true,
    /** Aviso «No pierdas tu racha» a las 20:00 si hoy aún no se ha completado nada. */
    val rachaAviso: Boolean = true,
    /** Si ya se vio (o se saltó) la guía de inicio. */
    val guiaVista: Boolean = false,
    /** Último día (ISO) en que se celebró la racha: la celebración sale una vez al día. */
    val rachaCelebrada: String? = null,
)

class AjustesStore(private val contexto: Context) {
    private object Claves {
        val tema = stringPreferencesKey("tema")
        val acento = stringPreferencesKey("acento")
        val vista = stringPreferencesKey("vista_tareas")
        val fondo = stringPreferencesKey("fondo_uri")
        val visibilidad = floatPreferencesKey("fondo_visibilidad")
        val opacidad = floatPreferencesKey("opacidad_paneles")
        val segundoAviso = androidx.datastore.preferences.core.intPreferencesKey("segundo_aviso_min")
        val menuAspecto = stringPreferencesKey("menu_aspecto")
        val menuOrden = stringPreferencesKey("menu_orden")
        val menuContadores = androidx.datastore.preferences.core.booleanPreferencesKey("menu_contadores")
        val rachaVisible = androidx.datastore.preferences.core.booleanPreferencesKey("racha_visible")
        val rachaAviso = androidx.datastore.preferences.core.booleanPreferencesKey("racha_aviso")
        val guiaVista = androidx.datastore.preferences.core.booleanPreferencesKey("guia_vista")
        val rachaCelebrada = stringPreferencesKey("racha_celebrada")
    }

    private fun leer(p: Preferences) = AjustesUi(
        tema = p[Claves.tema] ?: "sistema",
        acento = p[Claves.acento] ?: "coral",
        vistaTareas = p[Claves.vista] ?: "lista",
        fondoUri = p[Claves.fondo],
        fondoVisibilidad = (p[Claves.visibilidad] ?: 0.5f).coerceIn(0f, 1f),
        opacidadPaneles = (p[Claves.opacidad] ?: 0.45f).coerceIn(0f, 1f),
        segundoAvisoMin = (p[Claves.segundoAviso] ?: 0).coerceAtLeast(0),
        menuAspecto = (p[Claves.menuAspecto] ?: "plegable").takeIf { it in setOf("plegable", "compacto", "color") } ?: "plegable",
        menuOrden = (p[Claves.menuOrden] ?: "manual").takeIf { it in setOf("manual", "alfabetico", "pendientes") } ?: "manual",
        menuContadores = p[Claves.menuContadores] ?: true,
        rachaVisible = p[Claves.rachaVisible] ?: true,
        rachaAviso = p[Claves.rachaAviso] ?: true,
        guiaVista = p[Claves.guiaVista] ?: false,
        rachaCelebrada = p[Claves.rachaCelebrada],
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
            p[Claves.segundoAviso] = nuevo.segundoAvisoMin
            p[Claves.menuAspecto] = nuevo.menuAspecto
            p[Claves.menuOrden] = nuevo.menuOrden
            p[Claves.menuContadores] = nuevo.menuContadores
            p[Claves.rachaVisible] = nuevo.rachaVisible
            p[Claves.rachaAviso] = nuevo.rachaAviso
            p[Claves.guiaVista] = nuevo.guiaVista
            if (nuevo.rachaCelebrada == null) p.remove(Claves.rachaCelebrada) else p[Claves.rachaCelebrada] = nuevo.rachaCelebrada
        }
    }
}
