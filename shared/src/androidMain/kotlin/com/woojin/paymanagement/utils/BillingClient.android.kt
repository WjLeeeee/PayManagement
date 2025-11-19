package com.woojin.paymanagement.utils

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult as GoogleBillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.android.billingclient.api.BillingClient as GoogleBillingClient

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

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                if (purchases != null && purchases.isNotEmpty()) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                    // 결제 성공
                    pendingPurchaseResult?.invoke(BillingResult.Success)
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
            .enablePendingPurchases()
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

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                productDetailsCache = productDetailsList.associateBy { it.productId }
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
                purchases.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        // 미처리된 구매 모두 소비
                        val consumeParams = ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()

                        billingClient?.consumeAsync(consumeParams) { _, _ ->
                            // 자동 정리 완료
                        }
                    }
                }
            }
        }
    }

    /**
     * 구매 처리 및 소비
     * 팁주기는 일회성 소모품이므로 consume 처리
     */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
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
     * 연결 종료
     */
    actual fun disconnect() {
        billingClient?.endConnection()
        billingClient = null
    }
}
