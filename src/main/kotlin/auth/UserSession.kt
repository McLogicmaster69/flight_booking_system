package auth

@Deprecated("This class is no longer used, use SessionToken instead")
data class UserSession(
    var firstName: String,
    var lastName: String,
    var loyaltyPoints: Int,
)
