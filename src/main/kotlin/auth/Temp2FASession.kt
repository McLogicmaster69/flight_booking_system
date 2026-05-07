package auth

/**
 * Stores the token used in 2FA
 */
data class Temp2FASession(
    val token: String,
)
