package hr.foi.air.honnomachi.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.foi.air.honnomachi.data.AdminRepository
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel
    @Inject
    constructor(
        private val adminRepository: AdminRepository,
    ) : ViewModel() {
        private val _isAdminChecked = MutableStateFlow<Boolean?>(null)
        val isAdminChecked: StateFlow<Boolean?> = _isAdminChecked.asStateFlow()

        init {
            checkAdminStatus()
        }

        private fun checkAdminStatus() {
            viewModelScope.launch {
                when (val result = adminRepository.isCurrentUserAdmin()) {
                    is Result.Success -> _isAdminChecked.value = result.data
                    is Result.Error -> _isAdminChecked.value = false
                }
            }
        }
    }
