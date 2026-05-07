package auth

/**
 * Stores the state of a user being logged in or not.
 */
data class LoggedInState(
    val logged_in: Boolean,
    val session: SessionToken?,
)
