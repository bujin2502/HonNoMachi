package hr.foi.air.honnomachi.ui.cart

import hr.foi.air.honnomachi.model.CartItemModel

sealed interface CartUiState {
    data object Loading : CartUiState

    data class Success(
        val items: List<CartItemModel> = emptyList(),
        val totalPrice: Double = 0.0,
    ) : CartUiState

    data class Error(
        val message: String,
    ) : CartUiState
}
