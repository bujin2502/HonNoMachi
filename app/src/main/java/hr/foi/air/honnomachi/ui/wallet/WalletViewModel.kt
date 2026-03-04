package hr.foi.air.honnomachi.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.paymentsheet.PaymentSheetResult
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.foi.air.honnomachi.data.WalletRepository
import hr.foi.air.honnomachi.data.WalletTopupIntentModel
import hr.foi.air.honnomachi.data.WalletTopupStatus
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class WalletUiState(
    val balanceMinor: Int = 0,
    val currency: String = "eur",
    val isBalanceLoading: Boolean = true,
    val selectedTopupAmountMinor: Int = 500,
    val isCreatingTopup: Boolean = false,
    val activeTopupId: String? = null,
    val activeTopupStatus: WalletTopupStatus? = null,
)

@HiltViewModel
class WalletViewModel
    @Inject
    constructor(
        private val walletRepository: WalletRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(WalletUiState())
        val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

        private val _actionMessage = MutableStateFlow<String?>(null)
        val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

        private val _paymentSheetRequests =
            MutableSharedFlow<WalletTopupIntentModel>(extraBufferCapacity = 1)
        val paymentSheetRequests: SharedFlow<WalletTopupIntentModel> =
            _paymentSheetRequests.asSharedFlow()

        private var activeTopupObservationJob: Job? = null
        private var lastTerminalTopupId: String? = null

        init {
            observeWalletBalance()
        }

        fun selectTopupAmount(amountMinor: Int) {
            _uiState.update { it.copy(selectedTopupAmountMinor = amountMinor) }
        }

        fun startTopup() {
            val currentState = _uiState.value
            if (currentState.isCreatingTopup || currentState.activeTopupId != null) {
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isCreatingTopup = true, activeTopupStatus = WalletTopupStatus.PENDING) }

                when (
                    val result =
                        walletRepository.createWalletTopupIntent(
                            amountMinor = currentState.selectedTopupAmountMinor,
                            currency = DEFAULT_CURRENCY,
                            idempotencyKey = UUID.randomUUID().toString(),
                        )
                ) {
                    is Result.Success -> {
                        observeTopupStatus(result.data.topupId)
                        _uiState.update {
                            it.copy(
                                isCreatingTopup = false,
                                activeTopupId = result.data.topupId,
                                activeTopupStatus = WalletTopupStatus.PENDING,
                            )
                        }
                        _paymentSheetRequests.tryEmit(result.data)
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isCreatingTopup = false,
                                activeTopupStatus = null,
                            )
                        }
                        val message = result.exception.message ?: "Nepoznata greška."
                        _actionMessage.value = "Pokretanje wallet topup-a nije uspjelo: $message"
                    }
                }
            }
        }

        fun onPaymentSheetResult(result: PaymentSheetResult) {
            when (result) {
                PaymentSheetResult.Completed -> {
                    _actionMessage.value = "Plaćanje zaprimljeno. Čekam potvrdu..."
                }
                PaymentSheetResult.Canceled -> {
                    activeTopupObservationJob?.cancel()
                    activeTopupObservationJob = null
                    _uiState.update {
                        it.copy(
                            activeTopupId = null,
                            isCreatingTopup = false,
                            activeTopupStatus = null,
                        )
                    }
                    _actionMessage.value = "Plaćanje je otkazano."
                }
                is PaymentSheetResult.Failed -> {
                    activeTopupObservationJob?.cancel()
                    activeTopupObservationJob = null
                    _uiState.update {
                        it.copy(
                            activeTopupId = null,
                            isCreatingTopup = false,
                            activeTopupStatus = null,
                        )
                    }
                    val error = result.error.localizedMessage ?: "Nepoznata greška."
                    _actionMessage.value = "Plaćanje nije uspjelo: $error"
                }
            }
        }

        fun onPaymentSheetPresentationFailed(throwable: Throwable) {
            activeTopupObservationJob?.cancel()
            activeTopupObservationJob = null
            _uiState.update {
                it.copy(
                    activeTopupId = null,
                    isCreatingTopup = false,
                    activeTopupStatus = null,
                )
            }
            val error = throwable.localizedMessage ?: "Nepoznata greška."
            _actionMessage.value = "Prikaz Stripe naplate nije uspio: $error"
        }

        fun consumeMessage() {
            _actionMessage.value = null
        }

        private fun observeWalletBalance() {
            viewModelScope.launch {
                walletRepository.observeWalletBalance().collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _uiState.update {
                                it.copy(
                                    isBalanceLoading = false,
                                    balanceMinor = result.data.balanceMinor,
                                    currency = result.data.currency,
                                )
                            }
                        }
                        is Result.Error -> {
                            _uiState.update { it.copy(isBalanceLoading = false) }
                            val message = result.exception.message ?: "Nepoznata greška."
                            _actionMessage.value = "Wallet nije učitan: $message"
                        }
                    }
                }
            }
        }

        private fun observeTopupStatus(topupId: String) {
            activeTopupObservationJob?.cancel()
            activeTopupObservationJob =
                viewModelScope.launch {
                    walletRepository.observeTopupStatus(topupId).collect { result ->
                        when (result) {
                            is Result.Success -> {
                                val topup = result.data
                                _uiState.update { it.copy(activeTopupStatus = topup.status) }

                                if (topup.status.isTerminal()) {
                                    _uiState.update {
                                        it.copy(
                                            activeTopupId = null,
                                            isCreatingTopup = false,
                                        )
                                    }

                                    if (lastTerminalTopupId != topup.topupId) {
                                        lastTerminalTopupId = topup.topupId
                                        _actionMessage.value =
                                            when (topup.status) {
                                                WalletTopupStatus.SUCCEEDED -> "Wallet uspješno nadopunjen."
                                                WalletTopupStatus.FAILED -> {
                                                    val message =
                                                        topup.failureMessage ?: "Plaćanje nije prošlo."
                                                    "Wallet topup nije uspio: $message"
                                                }
                                                WalletTopupStatus.CANCELED -> "Wallet topup je otkazan."
                                                WalletTopupStatus.REFUNDED -> "Wallet topup je refundiran."
                                                else -> null
                                            }
                                    }

                                    activeTopupObservationJob?.cancel()
                                    activeTopupObservationJob = null
                                }
                            }
                            is Result.Error -> {
                                _uiState.update {
                                    it.copy(
                                        activeTopupId = null,
                                        isCreatingTopup = false,
                                        activeTopupStatus = null,
                                    )
                                }
                                val message = result.exception.message ?: "Nepoznata greška."
                                _actionMessage.value = "Praćenje wallet topup-a nije uspjelo: $message"
                                activeTopupObservationJob = null
                            }
                        }
                    }
                }
        }

        private fun WalletTopupStatus.isTerminal(): Boolean =
            this == WalletTopupStatus.SUCCEEDED ||
                this == WalletTopupStatus.FAILED ||
                this == WalletTopupStatus.CANCELED ||
                this == WalletTopupStatus.REFUNDED

        override fun onCleared() {
            activeTopupObservationJob?.cancel()
            super.onCleared()
        }

        companion object {
            private const val DEFAULT_CURRENCY = "eur"
        }
    }
