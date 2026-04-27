package routes

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
import data.*
import auth.UserSession
import auth.LoggedInState
import org.mindrot.jbcrypt.BCrypt

fun Route.signUpRoutes() {
    get("/signup") { call.handleSignUpLoad() }
    post("/signup") { call.handleSignUpPost() }
}

fun ApplicationCall.createSignUpStatus(message : String) : String = """<div id="sign-up-status" hx-swap-oob="true" role="status" aria-live="polite" aria-atomic="true">$message</div>"""

private suspend fun ApplicationCall.handleSignUpLoad() {
    timed("T0_sign_in", jsMode()) {
        val pebble = getEngine()
        
        val model = mapOf(
            "title" to "Sign Up",
            "inNav" to true,
        )

        val template = pebble.getTemplate("auth/signup.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handleSignUpPost() {
    timed("T1_sign_up_post", jsMode()) {
        val pebble = getEngine()
        val params = receiveParameters()

        val firstname = params["signupFirstName"]
        val lastname = params["signupLastName"]
        val email = params["signupEmail"]
        val password = params["signupPassword"]

        if (firstname.isNullOrBlank()) {
            respondText(createSignUpStatus("Please fill in a first name"), ContentType.Text.Html, status = HttpStatusCode.OK)
            return@timed
        }

        if (lastname.isNullOrBlank()) {
            respondText(createSignUpStatus("Please fill in a last name"), ContentType.Text.Html, status = HttpStatusCode.OK)
            return@timed
        }

        if (email.isNullOrBlank()) {
            respondText(createSignUpStatus("Please fill in an email"), ContentType.Text.Html, status = HttpStatusCode.OK)
            return@timed
        }

        if (password.isNullOrBlank()) {
            respondText(createSignUpStatus("Please fill in a password"), ContentType.Text.Html, status = HttpStatusCode.OK)
            return@timed
        }
        
        val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())
        val query : List<QueryResult<UserData>> = UserData.queryByLogIn(email)

        if (query.size > 0) {
            respondText(createSignUpStatus("User already exists with that email"), ContentType.Text.Html, status = HttpStatusCode.OK)
            return@timed
        }
        
        val login_id : Int = LoginData(email = email, passwordHash = passwordHash).insertIntoDatabase()
        val userData : UserData = UserData(firstName = firstname, lastName = lastname, verifiedAccount = false, loginId = login_id)
        val user_id : Int = userData.insertIntoDatabase()
        
        sessions.set(SessionData.createSession(user_id).toTokenSession())
        response.headers.append("HX-Redirect", "/")
        respond(HttpStatusCode.OK)
    }
}
