package com.dailywork.attedance.ui.premium

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.dailywork.attedance.billing.BillingManager
import com.dailywork.attedance.billing.BillingRepository
import com.dailywork.attedance.data.UserPreferencesRepository
import com.dailywork.attedance.premium.PremiumManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PremiumUiState(
    val products: List<ProductDetails> = emptyList(),
    val isPremium: Boolean = false,
    val premiumType: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isBillingReady: Boolean = false
)

class PremiumViewModel(
    private val repository: UserPreferencesRepository,
    private val billingRepository: BillingRepository,
    private val billingManager: BillingManager,
    private val premiumManager: PremiumManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PremiumUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.isPremiumFlow,
                repository.premiumTypeFlow,
                billingRepository.products,
                billingRepository.isBillingReady
            ) { isPremium, premiumType, products, isBillingReady ->
                PremiumUiState(
                    isPremium = isPremium,
                    premiumType = premiumType,
                    products = products,
                    isBillingReady = isBillingReady,
                    isLoading = products.isEmpty() && isBillingReady
                )
            }.collect {
                _uiState.value = it
            }
        }

        refreshProducts()
    }

    fun refreshProducts() {
        viewModelScope.launch {
            if (billingRepository.isBillingReady.value) {
                billingRepository.fetchProductDetails()
            }
        }
    }

    fun buyProduct(activity: Activity, productDetails: ProductDetails) {
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        billingManager.launchBillingFlow(activity, productDetails, offerToken)
    }

    fun restorePurchases() {
        premiumManager.restorePurchases()
    }

    fun manageSubscription(context: Context) {
        val packageName = context.packageName
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/store/account/subscriptions?package=$packageName")
        }
        context.startActivity(intent)
    }
}
