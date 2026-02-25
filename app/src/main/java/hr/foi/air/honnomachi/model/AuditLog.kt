package hr.foi.air.honnomachi.model

import com.google.firebase.Timestamp

data class AuditLog(
    val action: String = "",
    val targetUserId: String = "",
    val adminUserId: String = "",
    val reason: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
)
