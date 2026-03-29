package hr.foi.air.honnomachi.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.foi.air.honnomachi.ads.AdBannerState
import hr.foi.air.honnomachi.ads.AdFrequencyTracker
import hr.foi.air.honnomachi.ads.AdManager
import hr.foi.air.honnomachi.data.BookRepository
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class HomeViewModel
    @Inject
    constructor(
        private val bookRepository: BookRepository,
        val adManager: AdManager,
        private val adFrequencyTracker: AdFrequencyTracker,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
        open val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

        init {
            getBooks()
            checkAdAvailability()
        }

        open fun getBooks() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                bookRepository.getBooks().collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    books = result.data,
                                    errorMessage = null,
                                )
                            }
                        }

                        is Result.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    books = emptyList(),
                                    errorMessage = result.exception.message,
                                )
                            }
                        }
                    }
                }
            }
        }

        open fun onSearchQueryChange(newQuery: String) {
            _uiState.update { it.copy(searchQuery = newQuery) }
        }

        private fun checkAdAvailability() {
            if (!adFrequencyTracker.canShowAd()) {
                _uiState.update { it.copy(adState = AdBannerState.Capped) }
            }
        }

        fun onAdLoaded() {
            adFrequencyTracker.recordImpression()
            _uiState.update { it.copy(adState = AdBannerState.Loaded) }
        }

        fun onAdFailed(reason: String) {
            _uiState.update { it.copy(adState = AdBannerState.Failed(reason)) }
        }
    }
