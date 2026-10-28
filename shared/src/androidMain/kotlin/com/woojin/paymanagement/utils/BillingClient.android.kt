package com.woojin.paymanagement.utils

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.android.billingclient.api.BillingClient as GoogleBillingClient
import com.android.billingclient.api.BillingResult as GoogleBillingResult

/**
 * Android 인앱 결제 클라이언트 구현
 */
actual class BillingClient(
    private val context: Context,
    private val activityProvider: () -> Activity?
) {
    private var billingClient: GoogleBillingClient? = null
    private var productDetailsCache: Map<String, ProductDetails> = emptyMap()
    private var pendingPurchaseResult: ((BillingResult) -> Unit)? = null
    private val preferencesManager = PreferencesManager(context)
    // 광고 제거를 이미 반영한 결제 토큰 (같은 결제가 두 번 반영되지 않게)
    private val grantedPurchases = context.getSharedPreferences("billing_granted_purchases", Context.MODE_PRIVATE)

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                if (purchases != null && purchases.isNotEmpty()) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                    // 결제 대기(PENDING)는 아직 돈이 나간 게 아니라 성공으로 보지 않음. 완료되면 이 리스너가 다시 불려 반영됨
                    val completed = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    pendingPurchaseResult?.invoke(
                        if (completed) BillingResult.Success
                        else BillingResult.Error("결제 승인을 기다리고 있어요. 결제가 완료되면 자동으로 반영됩니다.")
                    )
                    pendingPurchaseResult = null
                }
            }
            BillingResponseCode.USER_CANCELED -> {
                // 사용자 취소
                pendingPurchaseResult?.invoke(BillingResult.Canceled)
                pendingPurchaseResult = null
            }
            else -> {
                // 결제 실패
                pendingPurchaseResult?.invoke(BillingResult.Error("결제 실패: ${billingResult.debugMessage}"))
                pendingPurchaseResult = null
            }
        }
    }

    /**
     * 결제 초기화
     */
    actual fun initialize(onReady: () -> Unit) {
        billingClient = GoogleBillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: GoogleBillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    // 연결 성공 - 상품 정보 조회
                    queryProductDetails()
                    // 미처리된 구매 내역 정리
                    consumePendingPurchases()
                    onReady()
                }
            }

            override fun onBillingServiceDisconnected() {
                // 연결 끊김 시 재연결 시도
                initialize(onReady)
            }
        })
    }

    /**
     * 상품 정보 조회
     */
    private fun queryProductDetails() {
        val productList = listOf(
            // 팁 상품들
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(TipProductId.COFFEE.productId)
                .setProductType(com.android.billingclient.api.BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(TipProductId.LUNCH.productId)
                .setProductType(com.android.billingclient.api.BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(TipProductId.DINNER.productId)
                .setProductType(com.android.billingclient.api.BillingClient.ProductType.INAPP)
                .build(),
            // 광고 제거 상품들
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(AdRemovalProductId.ONE_DAY.productId)
                .setProductType(com.android.billingclient.api.BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(AdRemovalProductId.THREE_DAYS.productId)
                .setProductType(com.android.billingclient.api.BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(AdRemovalProductId.SEVEN_DAYS.productId)
                .setProductType(com.android.billingclient.api.BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(AdRemovalProductId.THIRTY_DAYS.productId)
                .setProductType(com.android.billingclient.api.BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                productDetailsCache = queryProductDetailsResult.productDetailsList.associateBy { it.productId }
            }
        }
    }

    /**
     * 팁 구매 시작
     */
    actual suspend fun purchaseTip(productId: TipProductId): BillingResult {
        return suspendCancellableCoroutine { continuation ->
            val activity = activityProvider()
            if (activity == null) {
                continuation.resume(BillingResult.Error("Activity not available"))
                return@suspendCancellableCoroutine
            }

            if (billingClient?.isReady != true) {
                continuation.resume(BillingResult.Error("결제 서비스에 연결되지 않았습니다. 잠시 후 다시 시도해주세요."))
                return@suspendCancellableCoroutine
            }

            val productDetails = productDetailsCache[productId.productId]
            if (productDetails == null) {
                continuation.resume(BillingResult.Error("상품 정보를 찾을 수 없습니다"))
                return@suspendCancellableCoroutine
            }

            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build()
            )

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            // 결제 결과를 받을 콜백 저장
            pendingPurchaseResult = { result ->
                continuation.resume(result)
            }

            // 결제 시작
            val result = billingClient?.launchBillingFlow(activity, billingFlowParams)

            when (result?.responseCode) {
                BillingResponseCode.OK -> {
                    // 결제 다이얼로그 표시됨
                    // 실제 결과는 purchasesUpdatedListener에서 처리됨
                    // continuation은 여기서 resume하지 않음!
                }
                else -> {
                    // 결제 다이얼로그 표시 실패
                    pendingPurchaseResult = null
                    continuation.resume(BillingResult.Error("결제 시작 실패: ${result?.debugMessage}"))
                }
            }

            // 취소 시 정리
            continuation.invokeOnCancellation {
                pendingPurchaseResult = null
            }
        }
    }

    /**
     * 광고 제거 구매 시작
     */
    actual suspend fun launchPurchaseFlow(productId: AdRemovalProductId): BillingResult {
        return suspendCancellableCoroutine { continuation ->
            val activity = activityProvider()
            if (activity == null) {
                continuation.resume(BillingResult.Error("Activity not available"))
                return@suspendCancellableCoroutine
            }

            if (billingClient?.isReady != true) {
                continuation.resume(BillingResult.Error("결제 서비스에 연결되지 않았습니다. 잠시 후 다시 시도해주세요."))
                return@suspendCancellableCoroutine
            }

            val productDetails = productDetailsCache[productId.productId]
            if (productDetails == null) {
                continuation.resume(BillingResult.Error("상품 정보를 찾을 수 없습니다"))
                return@suspendCancellableCoroutine
            }

            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build()
            )

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            // 결제 결과를 받을 콜백 저장
            pendingPurchaseResult = { result ->
                continuation.resume(result)
            }

            // 결제 시작
            val result = billingClient?.launchBillingFlow(activity, billingFlowParams)

            when (result?.responseCode) {
                BillingResponseCode.OK -> {
                    // 결제 다이얼로그 표시됨
                    // 실제 결과는 purchasesUpdatedListener에서 처리됨
                }
                else -> {
                    // 결제 다이얼로그 표시 실패
                    pendingPurchaseResult = null
                    continuation.resume(BillingResult.Error("결제 시작 실패: ${result?.debugMessage}"))
                }
            }

            // 취소 시 정리
            continuation.invokeOnCancellation {
                pendingPurchaseResult = null
            }
        }
    }

    /**
     * 미처리된 구매 내역 소비 (앱 시작 시 자동 정리)
     */
    private fun consumePendingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(com.android.billingclient.api.BillingClient.ProductType.INAPP)
            .build()

        billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                // 앱이 꺼진 사이 완료된 결제도 광고 제거를 반영한 뒤 소비
                purchases.forEach { handlePurchase(it) }
            }
        }
    }

    /**
     * 구매 처리 및 소비
     * 팁주기는 일회성 소모품이므로 consume 처리
     */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            grantAdRemoval(purchase)

            // 일회성 소모품은 consume 처리 (반복 구매 가능)
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient?.consumeAsync(consumeParams) { billingResult, _ ->
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    // 구매 소비 완료 - 다시 구매 가능
                }
            }
        }
    }

    /**
     * 광고 제거 상품이면 남은 기간에 이어서 기간을 더함 (다시 사도 남은 기간이 사라지지 않음)
     */
    private fun grantAdRemoval(purchase: Purchase) {
        val days = purchase.products.sumOf { id -> AdRemovalProductId.entries.find { it.productId == id }?.days ?: 0 }
        if (days == 0) return
        val granted = grantedPurchases.getStringSet(KEY_GRANTED_TOKENS, emptySet()).orEmpty()
        if (purchase.purchaseToken in granted) return

        val now = System.currentTimeMillis()
        val base = maxOf(now, preferencesManager.getAdRemovalExpiryTime())
        preferencesManager.setAdRemovalExpiryTime(base + days * DAY_MILLIS)
        grantedPurchases.edit().putStringSet(KEY_GRANTED_TOKENS, granted + purchase.purchaseToken).apply()
    }

    /**
     * 연결 종료
     */
    actual fun disconnect() {
        billingClient?.endConnection()
        billingClient = null
    }

    private companion object {
        const val KEY_GRANTED_TOKENS = "granted_tokens"
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
