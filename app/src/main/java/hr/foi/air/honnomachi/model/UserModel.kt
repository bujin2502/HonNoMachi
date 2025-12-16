package hr.foi.air.honnomachi.model

data class UserModel(
    val name : String = "",
    val email : String = "",
    val uid : String = "",
    val admin: Boolean? = null,
    val suspended: Boolean? = null,
    val street: String? = null,
    val city: String? = null,
    val postNumber: String? = null,
    val phoneNumber: String? = null,
    val isVerified: Boolean = false
)
