package hr.foi.air.honnomachi.ui.add

sealed interface AddBookUiState {
    data object Idle : AddBookUiState

    data object Submitting : AddBookUiState

    data object Success : AddBookUiState

    data class Error(
        val message: String,
    ) : AddBookUiState
}
