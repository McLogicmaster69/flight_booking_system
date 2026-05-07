package auth

data class StaffLoggedInState(
    val logged_in: Boolean,
    val session: StaffSessionToken?,
    val staffId: Int,
)
