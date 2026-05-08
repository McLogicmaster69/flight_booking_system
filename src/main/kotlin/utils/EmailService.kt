package utils

import io.github.cdimascio.dotenv.dotenv
import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

/**
 * Handles all outgoing application email functionality.
 */
object EmailService {
    private val dotenv = dotenv()

    // Email account used to send all application emails.
    private val FROM_EMAIL =
        dotenv["EMAIL_USER"]
            ?: throw IllegalStateException("EMAIL_USER not set")

    // Gmail app password used for SMTP authentication.
    private val APP_PASSWORD =
        dotenv["EMAIL_APP_PASSWORD"]
            ?: throw IllegalStateException("EMAIL_APP_PASSWORD not set")

    /**
     * Creates and configures a Jakarta Mail SMTP session.
     */
    private fun createSession(): Session {
        val props =
            Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.host", "smtp.gmail.com")
                put("mail.smtp.port", "587")
            }

        return Session.getInstance(
            props,
            object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication =
                    PasswordAuthentication(FROM_EMAIL, APP_PASSWORD)
            },
        )
    }

    /**
     * Sends a two-factor authentication verification code email.
     */
    fun send2FA(
        to: String,
        code: String,
    ) {
        val session = createSession()

        val message =
            MimeMessage(session).apply {
                setFrom(InternetAddress(FROM_EMAIL))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                subject = "Your verification code"

                setText(
                    "Your login verification code is: $code\n\nIt expires in 5 minutes.",
                )
            }

        Transport.send(message)
    }

    /**
     * Sends a booking confirmation email with booking and passenger details.
     */
    fun sendBookingConfirmation(
        to: String,
        reference: String,
        startLocation: String,
        destination: String,
        dateTime: String,
        passengers: List<String>,
        rewards: List<String> = emptyList(),
    ) {
        val session = createSession()

        val passengerList = passengers.joinToString("\n") { "    - $it" }
        val rewardList = rewards.joinToString("\n") { "    - $it" }

        // Build the email body dynamically based on rewards and passengers.
        val body =
            buildString {
                appendLine("Booking Confirmation")
                appendLine()
                appendLine("Reference: $reference")
                appendLine()
                appendLine("Route:")
                appendLine("$startLocation -> $destination")
                appendLine()
                appendLine("Departure:")
                appendLine(dateTime)
                appendLine()
                appendLine("Passengers:")
                appendLine(passengerList)
                appendLine()

                if (rewards.isNotEmpty()) {
                    appendLine("Loyalty Rewards:")
                    appendLine(rewardList)
                    appendLine()
                }

                appendLine("Please keep this reference for check-in.")
                appendLine()
                append("Thank you for booking with us.")
            }

        val message =
            MimeMessage(session).apply {
                setFrom(InternetAddress(FROM_EMAIL))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                subject = "Booking Confirmation - Ref $reference"
                setText(body)
            }

        Transport.send(message)
    }

    /**
     * Sends a booking refund confirmation email.
     */
    fun sendRefundConfirmation(
        to: String,
        reference: String,
        refundAmount: Long,
        refundId: String,
    ) {
        val session = createSession()

        // Convert refund amount from pence/cents into currency format.
        val formattedAmount = "£%.2f".format(refundAmount / 100.0)

        val body =
            """
            Refund Confirmation

            Reference: $reference

            Your booking has been cancelled.
            A full refund of $formattedAmount has been requested.

            Refund ID: $refundId

            Your booked seats have now been released.

            Thank you.
            """.trimIndent()

        val message =
            MimeMessage(session).apply {
                setFrom(InternetAddress(FROM_EMAIL))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                subject = "Refund Confirmation - Ref $reference"
                setText(body)
            }

        Transport.send(message)
    }

    /**
     * Sends a support request or bug report email to the application support inbox.
     */
    fun sendSupportEmail(
        userEmail: String,
        messageText: String,
    ) {
        val session = createSession()

        val message =
            MimeMessage(session).apply {
                setFrom(InternetAddress(FROM_EMAIL))

                // Support emails are sent directly back to the application's inbox.
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(FROM_EMAIL))

                subject = "Bug Report / Support Request from $userEmail"

                setText(
                    "From: $userEmail\n\nMessage:\n$messageText",
                )
            }

        Transport.send(message)
    }
}
