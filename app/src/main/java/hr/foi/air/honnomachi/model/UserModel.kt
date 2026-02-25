package hr.foi.air.honnomachi.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class UserModel(
    val name: String = "",
    val email: String = "",
    val uid: String = "",
    val admin: Boolean? = null,
    val suspended: Boolean? = null,
    val suspendedAt: Timestamp? = null,
    val suspendedReason: String? = null,
    val suspendedBy: String? = null,
    val reactivatedAt: Timestamp? = null,
    val reactivatedBy: String? = null,
    val street: String? = null,
    val city: String? = null,
    val postNumber: String? = null,
    val phoneNumber: String? = null,
    @field:PropertyName("isVerified")
    @get:PropertyName("isVerified")
    val isVerified: Boolean = false,
    val analyticsEnabled: Boolean = true,
)
