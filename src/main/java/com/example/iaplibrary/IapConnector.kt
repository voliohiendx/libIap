package com.example.iaplibrary

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.*
import com.example.iaplibrary.model.IapIdModel
import com.example.iaplibrary.model.ProductModel
import com.example.iaplibrary.model.ProductModel.Companion.convertDataToProduct
import kotlinx.coroutines.*

class IapConnector : PurchasesUpdatedListener {

    private var activity: Activity? = null
    private var pathJson: String? = null

    private var billingClient: BillingClient? = null

    private val listProductModel = mutableListOf<ProductModel>()
    private val productDetailsList = mutableListOf<ProductDetails>()
    private val listID = mutableListOf<IapIdModel>()

    private var jobCountTimeConnectIap: Job? = null

    private var inApp: InApp? = null
    private var subs: Subs? = null

    val isPurchasesIap = MutableLiveData<Boolean?>(null)
    val subscribeSuccess = MutableLiveData<ProductModel?>(null)
    val subscribeError = MutableLiveData<String?>(null)

    fun initIap(activity: Activity, pathJson: String) {

        this.activity = activity
        this.pathJson = pathJson

        billingClient = BillingClient.newBuilder(activity).setListener(this)
            .enablePendingPurchases().build()

        listID.addAll(IapIdModel.getDataInput(activity, pathJson))

        jobCountTimeConnectIap = CoroutineScope(Dispatchers.IO).launch {
            delay(3000)
            isPurchasesIap.postValue(false)
        }

        inApp = InApp(billingClient, informationProduct = {
            productDetailsList.addAll(it)
            listProductModel.addAll(convertDataToProduct(it))
        }, subscribeIap = {
            for (purchase in it) {
                CoroutineScope(Dispatchers.IO).launch {
                    handlePurchase(purchase, false)
                }
            }
        })

        subs = Subs(billingClient, informationProduct = {
            productDetailsList.addAll(it)
            listProductModel.addAll(convertDataToProduct(it))
        }, subscribeIap = {
            for (purchase in it) {
                CoroutineScope(Dispatchers.IO).launch {
                    handlePurchase(purchase, false)
                }
            }
        })

        startConnection()
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                CoroutineScope(Dispatchers.IO).launch {
                    handlePurchase(purchase, true)
                }
            }
        } else {
            subscribeError.postValue(logEventFailed(billingResult.responseCode))
        }
    }

    private fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val inappsubs = listID.filter { it.type == "inapp" }
                        if (inappsubs.isNotEmpty()) {
                            inApp?.getInformation(inappsubs)
                        }
                        val subssubs = listID.filter { it.type == "subs" }
                        if(subssubs.isNotEmpty()){
                            subs?.getInformation(listID.filter { it.type == "subs" })
                        }
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
            }
        })
    }

    fun buyIap(productId: String) {
        productDetailsList.find { it.productId == productId }?.let { productDetails ->
            val billingFlowParam = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)

            if (productDetails.productType == BillingClient.ProductType.SUBS) {
                billingFlowParam.setOfferToken(
                    productDetails.subscriptionOfferDetails?.get(0)?.offerToken ?: ""
                )
            }

            val productDetailsParamsList =
                listOf(billingFlowParam.build())

            val billingFlowParams =
                BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

            activity?.let { billingClient?.launchBillingFlow(it, billingFlowParams) }
        }
    }

    private fun handlePurchase(purchase: Purchase, isSubscriptions: Boolean) {
        if (!purchase.isAcknowledged) {
            val acknowledgePurchaseParams =
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken)
            CoroutineScope(Dispatchers.Default).launch() {
                withContext(Dispatchers.IO) {
                    billingClient?.acknowledgePurchase(acknowledgePurchaseParams.build()) { p0 ->
                        if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
                            setDataCallBackSuccess(purchase, isSubscriptions)
                        } else {
                            subscribeError.postValue(logEventFailed(p0.responseCode))
                        }
                    }
                }
            }
        } else {
            setDataCallBackSuccess(purchase, isSubscriptions)
        }
    }

    fun resetIap() {
        subs?.unSubscribeIap()
        inApp?.unSubscribeIap()
    }

    private fun setDataCallBackSuccess(purchase: Purchase, isSubscriptions: Boolean) {
        jobCountTimeConnectIap?.cancel()
        isPurchasesIap.postValue(true)

        listProductModel.find { purchase.products.contains(it.productId) }?.let {
            it.isPurchase = true
            it.purchaseTime = it.purchaseTime
            if (isSubscriptions) {
                subscribeSuccess.postValue(it)
                //   iapCallBack?.subscribeSuccess(it)
            }
        }
    }

    private fun logEventFailed(code: Int): String {
        return when (code) {
            -3 -> "Service_Timeout"
            -2 -> "Feature_Not_Supported"
            -1 -> "Service_Disconnected"
            1 -> "User_Canceled"
            2 -> "Service_Unavailable"
            3 -> "Billing_Unavailable"
            4 -> "Item_Unavailable"
            5 -> "Developer_Error"
            6 -> "Error"
            7 -> "Item_Already_Owned"
            8 -> "Item_Not_Owned"
            else -> ""
        }
    }

    fun getAllProductModel(): List<ProductModel> {
        return listProductModel
    }

    companion object {

        private var INSTANCE: IapConnector? = null

        fun getInstance(): IapConnector {
            if (INSTANCE == null) {
                INSTANCE = IapConnector()
            }
            return INSTANCE!!
        }
    }
}
