package auth

data class UserSession(
    val id: Int = 0,
    var firstName: String? = null,
    var lastName: String? = null,
    var verifiedAccount: Boolean? = null,
    var loginId: Int = 0
)
