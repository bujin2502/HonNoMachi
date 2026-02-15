package hr.foi.air.honnomachi.model

import com.google.firebase.firestore.PropertyName

data class UserModel(
    val name: String = "",
    val email: String = "",
    val uid: String = "",
    val admin: Boolean? = null,
    val suspended: Boolean? = null,
    val street: String? = null,
    val city: String? = null,
    val postNumber: String? = null,
    val phoneNumber: String? = null,
    @field:PropertyName("isVerified")
    @get:PropertyName("isVerified")
    val isVerified: Boolean = false,
    val analyticsEnabled: Boolean = true,
)
