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

/**
 * ViewModel za provjeru administratorskog pristupa.
 *
 * Provjerava ima li trenutni korisnik administratorske ovlasti
 * i izlaže taj status putem [isAdminChecked] StateFlow-a.
 * Koristi se kao "guard" za zaštitu admin ruta u navigaciji.
 *
 * @param adminRepository Repozitorij za administratorske operacije.
 */
@HiltViewModel
class AdminViewModel
    @Inject
    constructor(
        private val adminRepository: AdminRepository,
    ) : ViewModel() {
        /**
         * Stanje provjere admin statusa.
         *
         * - `null` = provjera u tijeku (loading)
         * - `true` = korisnik je administrator
         * - `false` = korisnik nije administrator ili je došlo do greške
         */
        private val _isAdminChecked = MutableStateFlow<Boolean?>(null)
        val isAdminChecked: StateFlow<Boolean?> = _isAdminChecked.asStateFlow()

        init {
            checkAdminStatus()
        }

        /**
         * Provjerava admin status trenutnog korisnika putem repozitorija.
         *
         * U slučaju greške, korisnik se tretira kao ne-administrator
         * (sigurnosni pristup - deny by default).
         */
        private fun checkAdminStatus() {
            viewModelScope.launch {
                when (val result = adminRepository.isCurrentUserAdmin()) {
                    is Result.Success -> _isAdminChecked.value = result.data
                    is Result.Error -> _isAdminChecked.value = false
                }
            }
        }
    }
