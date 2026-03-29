package hr.foi.air.honnomachi.ads

sealed interface AdBannerState {
    data object Loading : AdBannerState

    data object Loaded : AdBannerState

    data class Failed(val reason: String) : AdBannerState

    data object Capped : AdBannerState
}
