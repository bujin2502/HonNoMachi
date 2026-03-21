package hr.foi.air.honnomachi.ui.suspension

import androidx.compose.runtime.compositionLocalOf

val LocalIsSuspended = compositionLocalOf { false }
val LocalSuspendedReason = compositionLocalOf<String?> { null }
