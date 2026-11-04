package app.kiwu.android.notify

import app.kiwu.android.KiwuApp
import app.kiwu.android.domain.AvisosDeTareas

/** La barra del sistema y las alarmas de `notify/`, tal como las usa la app (en las pruebas se usa [AvisosDeTareas.Nada]). */
class AvisosAndroid(private val app: KiwuApp) : AvisosDeTareas {
    override fun olvidar(id: String) = Avisos.olvidar(app, id)

    override fun revisar() = app.revisarAvisos()
}
