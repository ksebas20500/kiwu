package app.kiwu.android

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Compra única «Quitar anuncios» con Google Play Billing. Google asocia la compra a la cuenta de Play Store del usuario,
 * así que al reinstalar o cambiar de teléfono se recupera consultando a Google (no hay servidor ni cuentas propias).
 * El resultado se recuerda en el teléfono para no mostrar anuncios ni un instante si ya se compró.
 */
class Compras(private val contexto: Context) {
    private val prefs = contexto.getSharedPreferences("kiwu", Context.MODE_PRIVATE)
    private val _sinAnuncios = MutableStateFlow(prefs.getBoolean(CLAVE, false))
    val sinAnuncios: StateFlow<Boolean> = _sinAnuncios
    private val _precio = MutableStateFlow<String?>(null)

    /** Precio ya adaptado al país y la moneda del usuario (por ejemplo «$ 2.900»). Null hasta que Google responde. */
    val precio: StateFlow<String?> = _precio

    private var detalles: ProductDetails? = null
    private var cliente: BillingClient? = null

    private val alActualizar = PurchasesUpdatedListener { resultado, compras ->
        if (resultado.responseCode == BillingClient.BillingResponseCode.OK && compras != null) compras.forEach(::procesar)
    }

    /** Conecta con Google Play y comprueba si ya hay una compra. Se llama una vez, pasado el arranque. */
    fun iniciar() {
        if (cliente != null) return
        val c = BillingClient.newBuilder(contexto).setListener(alActualizar)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()).build()
        cliente = c
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(resultado: BillingResult) {
                if (resultado.responseCode != BillingClient.BillingResponseCode.OK) return
                c.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { r, compras ->
                    // Solo se actualiza si Google respondió bien: sin conexión se mantiene lo recordado.
                    if (r.responseCode == BillingClient.BillingResponseCode.OK) {
                        val tiene = compras.any { it.products.contains(PRODUCTO) && it.purchaseState == Purchase.PurchaseState.PURCHASED }
                        compras.forEach(::procesar)
                        poner(tiene)
                    }
                }
                val consulta = QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCTO).setProductType(BillingClient.ProductType.INAPP).build()
                c.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(consulta)).build()) { r, lista ->
                    if (r.responseCode == BillingClient.BillingResponseCode.OK) {
                        detalles = lista.firstOrNull()
                        _precio.value = detalles?.oneTimePurchaseOfferDetails?.formattedPrice
                    }
                }
            }

            override fun onBillingServiceDisconnected() { cliente = null }
        })
    }

    /** Abre el cuadro de pago de Google Play. */
    fun comprar(actividad: Activity) {
        val d = detalles ?: return
        val producto = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build()
        cliente?.launchBillingFlow(actividad, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(producto)).build())
    }

    /** Solo para pruebas (depuración y minificado): activa o desactiva «sin anuncios» sin pasar por Google Play. */
    fun simular(activo: Boolean) = poner(activo)

    private fun procesar(compra: Purchase) {
        if (!compra.products.contains(PRODUCTO) || compra.purchaseState != Purchase.PurchaseState.PURCHASED) return
        poner(true)
        // Google devuelve el dinero si la compra no se confirma en 3 días.
        if (!compra.isAcknowledged) {
            cliente?.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(compra.purchaseToken).build()) { }
        }
    }

    private fun poner(valor: Boolean) {
        prefs.edit().putBoolean(CLAVE, valor).apply()
        _sinAnuncios.value = valor
    }

    companion object {
        /** Identificador del producto en Play Console (artículo no consumible). */
        const val PRODUCTO = "quitar_anuncios"
        private const val CLAVE = "sin_anuncios"
    }
}
