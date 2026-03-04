package utils

import jakarta.mail.*
import jakarta.mail.internet.*
import java.util.*

object EmailService {

    //using a burner email and password
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
}