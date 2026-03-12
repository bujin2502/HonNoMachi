package hr.foi.air.honnomachi.viewmodel

import com.stripe.android.paymentsheet.PaymentSheetResult
import hr.foi.air.honnomachi.data.WalletBalanceModel
import hr.foi.air.honnomachi.data.WalletHistoryItemModel
import hr.foi.air.honnomachi.data.WalletHistoryType
import hr.foi.air.honnomachi.data.WalletRepository
import hr.foi.air.honnomachi.data.WalletTopupIntentModel
import hr.foi.air.honnomachi.data.WalletTopupModel
import hr.foi.air.honnomachi.data.WalletTopupStatus
import hr.foi.air.honnomachi.ui.wallet.WalletViewModel
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeWalletRepository : WalletRepository {
    val balanceFlow = MutableSharedFlow<Result<WalletBalanceModel>>(replay = 1)
    val historyFlow = MutableSharedFlow<Result<List<WalletHistoryItemModel>>>(replay = 1)
    val topupStatusFlow = MutableSharedFlow<Result<WalletTopupModel>>(replay = 1)

    var createTopupResult: Result<WalletTopupIntentModel> =
        Result.Success(
            WalletTopupIntentModel(
                topupId = "topup_1",
                paymentIntentId = "pi_test",
                clientSecret = "pi_test_secret",
                amountMinor = 500,
                currency = "eur",
            ),
        )

    override suspend fun createWalletTopupIntent(
        amountMinor: Int,
        currency: String,
        idempotencyKey: String,
    ): Result<WalletTopupIntentModel> = createTopupResult

    override fun observeWalletBalance(): Flow<Result<WalletBalanceModel>> = balanceFlow

    override fun observeTopupStatus(topupId: String): Flow<Result<WalletTopupModel>> = topupStatusFlow

    override fun observeWalletHistory(): Flow<Result<List<WalletHistoryItemModel>>> = historyFlow
}

@ExperimentalCoroutinesApi
class WalletViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeWalletRepository
    private lateinit var viewModel: WalletViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeWalletRepository()
        viewModel = WalletViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has correct defaults`() =
        runTest(testDispatcher) {
            assertTrue(viewModel.uiState.value.isBalanceLoading)
            assertTrue(viewModel.uiState.value.isHistoryLoading)
            assertEquals(500, viewModel.uiState.value.selectedTopupAmountMinor)
            assertNull(viewModel.uiState.value.activeTopupId)
        }

    @Test
    fun `selectTopupAmount updates selected amount`() =
        runTest(testDispatcher) {
            viewModel.selectTopupAmount(2000)

            assertEquals(2000, viewModel.uiState.value.selectedTopupAmountMinor)
        }

    @Test
    fun `observeWalletBalance success updates balance state`() =
        runTest(testDispatcher) {
            fakeRepository.balanceFlow.emit(
                Result.Success(WalletBalanceModel(balanceMinor = 5000, currency = "eur")),
            )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isBalanceLoading)
            assertEquals(5000, state.balanceMinor)
            assertEquals("eur", state.currency)
        }

    @Test
    fun `observeWalletBalance error clears loading and shows message`() =
        runTest(testDispatcher) {
            fakeRepository.balanceFlow.emit(Result.Error(Exception("Balance error")))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isBalanceLoading)
            assertEquals("Wallet nije učitan: Balance error", viewModel.actionMessage.value)
        }

    @Test
    fun `observeWalletHistory success updates history state`() =
        runTest(testDispatcher) {
            val historyItems =
                listOf(
                    WalletHistoryItemModel(
                        id = "h1",
                        type = WalletHistoryType.TOPUP,
                        amountMinor = 1000,
                        currency = "eur",
                        createdAtEpochMillis = null,
                    ),
                )
            fakeRepository.historyFlow.emit(Result.Success(historyItems))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isHistoryLoading)
            assertEquals(1, state.history.size)
            assertEquals("h1", state.history[0].id)
        }

    @Test
    fun `observeWalletHistory error clears loading and shows message`() =
        runTest(testDispatcher) {
            fakeRepository.historyFlow.emit(Result.Error(Exception("History error")))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isHistoryLoading)
            assertEquals(
                "Povijest wallet transakcija nije učitana: History error",
                viewModel.actionMessage.value,
            )
        }

    @Test
    fun `startTopup success emits payment sheet request and updates state`() =
        runTest(testDispatcher) {
            val emittedRequests = mutableListOf<WalletTopupIntentModel>()
            val job =
                backgroundScope.launch {
                    viewModel.paymentSheetRequests.collect { emittedRequests += it }
                }
            advanceTimeBy(1_000)
            runCurrent()

            viewModel.startTopup()
            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(1, emittedRequests.size)
            assertEquals("topup_1", emittedRequests[0].topupId)
            assertEquals("topup_1", viewModel.uiState.value.activeTopupId)
            assertFalse(viewModel.uiState.value.isCreatingTopup)

            job.cancel()
        }

    @Test
    fun `startTopup error shows error message and clears state`() =
        runTest(testDispatcher) {
            fakeRepository.createTopupResult = Result.Error(Exception("Topup failed"))

            viewModel.startTopup()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.activeTopupId)
            assertFalse(viewModel.uiState.value.isCreatingTopup)
            assertEquals(
                "Pokretanje wallet topup-a nije uspjelo: Topup failed",
                viewModel.actionMessage.value,
            )
        }

    @Test
    fun `startTopup when activeTopupId set does nothing`() =
        runTest(testDispatcher) {
            viewModel.startTopup()
            advanceUntilIdle()
            assertEquals("topup_1", viewModel.uiState.value.activeTopupId)

            viewModel.startTopup()
            advanceUntilIdle()

            assertEquals("topup_1", viewModel.uiState.value.activeTopupId)
        }

    @Test
    fun `onPaymentSheetResult Completed shows confirmation message`() =
        runTest(testDispatcher) {
            viewModel.onPaymentSheetResult(PaymentSheetResult.Completed)

            assertEquals("Plaćanje zaprimljeno. Čekam potvrdu...", viewModel.actionMessage.value)
        }

    @Test
    fun `onPaymentSheetResult Canceled clears topup state and shows message`() =
        runTest(testDispatcher) {
            viewModel.onPaymentSheetResult(PaymentSheetResult.Canceled)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.activeTopupId)
            assertFalse(state.isCreatingTopup)
            assertNull(state.activeTopupStatus)
            assertEquals("Plaćanje je otkazano.", viewModel.actionMessage.value)
        }

    @Test
    fun `onPaymentSheetResult Failed clears topup state and shows error`() =
        runTest(testDispatcher) {
            viewModel.onPaymentSheetResult(PaymentSheetResult.Failed(Exception("Card declined")))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.activeTopupId)
            assertFalse(state.isCreatingTopup)
            assertNull(state.activeTopupStatus)
            assertTrue(viewModel.actionMessage.value!!.contains("Card declined"))
        }

    @Test
    fun `onPaymentSheetPresentationFailed clears state and shows error`() =
        runTest(testDispatcher) {
            viewModel.onPaymentSheetPresentationFailed(Exception("Stripe init failed"))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.activeTopupId)
            assertFalse(state.isCreatingTopup)
            assertNull(state.activeTopupStatus)
            assertTrue(viewModel.actionMessage.value!!.contains("Stripe init failed"))
        }

    @Test
    fun `consumeMessage clears action message`() =
        runTest(testDispatcher) {
            viewModel.onPaymentSheetResult(PaymentSheetResult.Completed)
            assertEquals("Plaćanje zaprimljeno. Čekam potvrdu...", viewModel.actionMessage.value)

            viewModel.consumeMessage()

            assertNull(viewModel.actionMessage.value)
        }

    @Test
    fun `observeTopupStatus SUCCEEDED clears activeTopupId and shows success message`() =
        runTest(testDispatcher) {
            viewModel.startTopup()
            advanceUntilIdle()
            assertEquals("topup_1", viewModel.uiState.value.activeTopupId)

            fakeRepository.topupStatusFlow.emit(
                Result.Success(
                    WalletTopupModel(
                        topupId = "topup_1",
                        status = WalletTopupStatus.SUCCEEDED,
                        failureMessage = null,
                    ),
                ),
            )
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.activeTopupId)
            assertEquals("Wallet uspješno nadopunjen.", viewModel.actionMessage.value)
        }

    @Test
    fun `observeTopupStatus FAILED shows failure message`() =
        runTest(testDispatcher) {
            viewModel.startTopup()
            advanceUntilIdle()

            fakeRepository.topupStatusFlow.emit(
                Result.Success(
                    WalletTopupModel(
                        topupId = "topup_1",
                        status = WalletTopupStatus.FAILED,
                        failureMessage = "Insufficient funds",
                    ),
                ),
            )
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.activeTopupId)
            assertTrue(viewModel.actionMessage.value!!.contains("Insufficient funds"))
        }

    @Test
    fun `observeTopupStatus CANCELED shows canceled message`() =
        runTest(testDispatcher) {
            viewModel.startTopup()
            advanceUntilIdle()

            fakeRepository.topupStatusFlow.emit(
                Result.Success(
                    WalletTopupModel(
                        topupId = "topup_1",
                        status = WalletTopupStatus.CANCELED,
                        failureMessage = null,
                    ),
                ),
            )
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.activeTopupId)
            assertEquals("Wallet topup je otkazan.", viewModel.actionMessage.value)
        }

    @Test
    fun `observeTopupStatus REFUNDED shows refunded message`() =
        runTest(testDispatcher) {
            viewModel.startTopup()
            advanceUntilIdle()

            fakeRepository.topupStatusFlow.emit(
                Result.Success(
                    WalletTopupModel(
                        topupId = "topup_1",
                        status = WalletTopupStatus.REFUNDED,
                        failureMessage = null,
                    ),
                ),
            )
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.activeTopupId)
            assertEquals("Wallet topup je refundiran.", viewModel.actionMessage.value)
        }

    @Test
    fun `observeTopupStatus error clears state and shows message`() =
        runTest(testDispatcher) {
            viewModel.startTopup()
            advanceUntilIdle()

            fakeRepository.topupStatusFlow.emit(Result.Error(Exception("Status error")))
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.activeTopupId)
            assertEquals(
                "Praćenje wallet topup-a nije uspjelo: Status error",
                viewModel.actionMessage.value,
            )
        }
}
