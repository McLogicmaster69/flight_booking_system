package utils

import jakarta.mail.*
import jakarta.mail.internet.*
import java.util.*

object EmailService {

    private const val FROM_EMAIL = "alidos37pro@gmail.com"
    private const val APP_PASSWORD = "oyeq zeqm bmvv qvpn"

    fun send2FA(to: String, code: String) {

        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(FROM_EMAIL, APP_PASSWORD)
            }
        })

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(FROM_EMAIL))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            subject = "Your verification code"
            setText("Your login verification code is: $code\n\nIt expires in 5 minutes.")
        }

        Transport.send(message)
    }

    fun sendBookingConfirmation(
        to: String,
        reference: String,
        startLocation: String,
        destination: String,
        dateTime: String,
        passengers: List<String>
    ) {

        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(FROM_EMAIL, APP_PASSWORD)
            }
        })

        val passengerList = passengers.joinToString("\n") { "    - $it" }

        val body = buildString {
            appendLine("Booking Confirmation")
            appendLine()
            appendLine("Reference: $reference")
            appendLine()
            appendLine("Route:")
            appendLine("$startLocation → $destination")
            appendLine()
            appendLine("Departure:")
            appendLine(dateTime)
            appendLine()
            appendLine("Passengers:")
            appendLine(passengerList)
            appendLine()
            appendLine("Please keep this reference for check-in.")
            appendLine()
            append("Thank you for booking with us.")
        }

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(FROM_EMAIL))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            subject = "Booking Confirmation - Ref $reference"
            setText(body)
        }

        Transport.send(message)
    }
    
}