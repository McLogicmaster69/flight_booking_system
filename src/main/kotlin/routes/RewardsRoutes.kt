package routes

import data.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.pebbletemplates.pebble.PebbleEngine
import java.io.StringWriter
import utils.jsMode
import utils.logValidationError
import utils.timed
import auth.*
import org.mindrot.jbcrypt.BCrypt
import utils.EmailService
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit

fun Route.rewardsRoutes() {
    get("/rewards") { call.handleRewardsLoad() }
}

private suspend fun ApplicationCall.handleRewardsLoad() {
    timed("T0_rewards", jsMode()) {
        val logged_state : LoggedInState = loggedIn()
        if (!logged_state.logged_in)
            respondRedirect("/login")
       
        val model = mapOf(
            "title" to "Loyalty Rewards",
            "inNav" to true
        )

        val pebble = getEngine()
        val template = pebble.getTemplate("rewards/index.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}
