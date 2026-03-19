package hr.foi.air.honnomachi.model

import com.google.firebase.Timestamp

data class SuspensionHistoryEntry(
    val action: String = "",
    val adminUserId: String = "",
    val reason: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val previousState: Map<String, Any?> = emptyMap(),
)
