package app.kiwu.android.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Gestiona los pagos y compras dentro de la app (Google Play Billing Library v8) para «Quitar anuncios». */
class BillingManager(private val context: Context) {
    private val _comprado = MutableStateFlow(false)
    val comprado: StateFlow<Boolean> = _comprado

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
        .enableOneTimeProducts()
        .build()

    private val billingClient = BillingClient.newBuilder(context)
        .setListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }
        .enablePendingPurchases(pendingPurchasesParams)
        .build()

    init {
        conectar()
    }

    private fun conectar() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    verificarCompras()
                }
            }
            override fun onBillingServiceDisconnected() {
                // Reintentar conexión si es necesario
            }
        })
    }

    fun verificarCompras() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val tiene = purchases.any { it.products.contains("quitar_anuncios") && it.purchaseState == Purchase.PurchaseState.PURCHASED }
                _comprado.value = tiene
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _comprado.value = true
                    }
                }
            } else {
                _comprado.value = true
            }
        }
    }

    fun comprar(activity: Activity) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("quitar_anuncios")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        alcance.launch {
            try {
                val result = billingClient.queryProductDetails(params)
                val list = result.productDetailsList
                if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK && !list.isNullOrEmpty()) {
                    val productDetails = list[0]
                    val productDetailsParamsList = listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .build()
                    )
                    val billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build()
                    withContext(Dispatchers.Main) {
                        billingClient.launchBillingFlow(activity, billingFlowParams)
                    }
                }
            } catch (_: Exception) {
                // Si falla la consulta de detalles, no debe romper la app
            }
        }
    }
}
