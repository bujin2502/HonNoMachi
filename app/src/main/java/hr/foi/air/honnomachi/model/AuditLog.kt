package hr.foi.air.honnomachi.model

import com.google.firebase.Timestamp

/**
 * Model revizijskog zapisa za administratorske akcije.
 *
 * Pohranjuje se u Firestore kolekciju `audit_logs` pri
 * suspenziji ili reaktivaciji korisničkog računa.
 */
data class AuditLog(
    val action: String = "",
    val targetUserId: String = "",
    val adminUserId: String = "",
    val reason: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
)
