package auth

/**
 * Stores the state of a staff members being logged in or not.
 */
data class StaffLoggedInState(
    val logged_in: Boolean,
    val session: StaffSessionToken?,
    val staffId: Int,
)
