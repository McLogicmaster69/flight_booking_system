package routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.StringWriter
import utils.timed
import utils.jsMode
import auth.*
import data.*
import io.ktor.server.request.*
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDate
import java.time.LocalTime
import com.stripe.Stripe
import com.stripe.model.Refund
import com.stripe.param.RefundCreateParams
import utils.EmailService

fun Route.adminRoutes() {
    get("/admin") { call.handleAdminLoad() }
}


private suspend fun ApplicationCall.handleAdminLoad() {
    timed("T0_admin", jsMode()) {
        if (!requireAdmin()) return@timed

        val pebble = getEngine()

        val model =
            mapOf(
                "title" to "Admin",
                "layout" to "admin",
                "activePage" to "admin",
                "inNav" to true,
                "isAdmin" to true,
            )

        val template = pebble.getTemplate("admin/index.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}
