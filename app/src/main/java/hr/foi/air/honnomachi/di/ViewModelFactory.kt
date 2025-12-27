package hr.foi.air.honnomachi.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import hr.foi.air.honnomachi.data.BookRepositoryImpl
import hr.foi.air.honnomachi.ui.home.HomeViewModel
import hr.foi.air.honnomachi.ui.profile.ProfileViewModel

class ViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(BookRepositoryImpl()) as T
        }
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
