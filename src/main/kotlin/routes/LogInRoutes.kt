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

fun Route.logInRoutes() {
    get("/login") { call.handleLogInLoad() }
    post("/login") { call.handleLogInPost() }
    get("/logout") { call.handleLogOut() }
    get("/verify") { call.handleVerifyLoad() }
    post("/verify") { call.handleVerifyPost() }
    post("/send-verification") { call.handleSendVerification() }
}

fun ApplicationCall.createLoginStatus(message : String) : String = """<div id="log-in-status" hx-swap-oob="true" role="status" aria-live="polite" aria-atomic="true">$message</div>"""

private suspend fun ApplicationCall.handleLogInLoad() {
    timed("T0_log_in", jsMode()) {
        val pebble = getEngine()
        val logged_state : LoggedInState = loggedIn()

        if (logged_state.logged_in)
            respondRedirect("/")

        val model = mapOf(
            "title" to "Log In / Sign Up"
        )

        val template = pebble.getTemplate("auth/login.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handleLogInPost() {
    timed("T1_log_in_post", jsMode()) {
        val pebble = getEngine()
        val params = receiveParameters()
        val email = params["loginEmail"]
        val password = params["loginPw"]

        if (email == null) {
            respondText(createLoginStatus("Incorrect email or password"), ContentType.Text.Html, status = HttpStatusCode.OK)
            return@timed
        }

        val query : List<QueryResult<UserData>> = UserData.queryByLogIn(email)

        if (query.size == 0) {
            respondText(createLoginStatus("Incorrect email or password"), ContentType.Text.Html, status = HttpStatusCode.OK)
            return@timed
        }

        val result : QueryResult<UserData> = query[0]
        val column : ColumnValue? = result.getColumn(LoginData.EMPTY.tableName, LoginColumns.PASSWORD_HASH.name)

        if (column == null) {
            respondText(createLoginStatus("An error occured, please try again later"), ContentType.Text.Html, status = HttpStatusCode.OK)
            return@timed
        }

        val stored_password : String = column.columnVal as String

        if (!BCrypt.checkpw(password, stored_password)){
            respondText(createLoginStatus("Incorrect email or password"), ContentType.Text.Html, status = HttpStatusCode.OK)
            return@timed
        }

        val code = generate2FACode()
        val hashedCode = BCrypt.hashpw(code, BCrypt.gensalt(10))

        val expiration = Timestamp.from(
            Instant.now().plus(5, ChronoUnit.MINUTES)
        )

        TwoFAData.deleteByUserId(result.dataClass.id)
        val token = TwoFAData.EMPTY.generateToken()

        TwoFAData(
            userId = result.dataClass.id,
            ttl = expiration,
            code_hash = hashedCode,
            attempts = 0,
            sessionToken = token
        ).insertIntoDatabase()

        EmailService.send2FA(email, code)

        sessions.set(Temp2FASession(token))
        response.headers.append("HX-Redirect", "/verify")
        respond(HttpStatusCode.OK)
        return@timed
    }
}

private suspend fun ApplicationCall.handleLogOut() {
    timed("T2_log_out", jsMode()) {
        sessions.clear<SessionToken>()
        respondRedirect("/")
    }
}

private suspend fun ApplicationCall.handleVerifyLoad() {
    timed("T3_verify", jsMode()) {
        val pebble = getEngine()

        val model = mapOf(
            "title" to "2 Factor Authentication"
        )

        val template = pebble.getTemplate("auth/verify.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handleVerifyPost() {
    timed("T4_verify_post", jsMode()) {
        val MAX_ATTEMPTS = 5
        val tempSession = sessions.get<Temp2FASession>()
            ?: return@timed respondRedirect("/login")

        val params = receiveParameters()
        val enteredCode = params["code"] ?: ""

        val query = TwoFAData.queryDatabase(
            whereArgs = WhereArgs(
                "${TwoFAColumns.SESSION_TOKEN.name} = ?",
                listOf(tempSession.token)
            )
        )

        if (query.isEmpty()) {
            respondText(
                "<div class='error-message'>Code Expired, Please Log In Again</div>",
                ContentType.Text.Html
            )
            return@timed
        }

        val record = query[0].dataClass
        val now = Timestamp.from(Instant.now())

        if (record.ttl.before(now) || !BCrypt.checkpw(enteredCode, record.code_hash)) {
            record.attempts += 1
            record.update()

            if (record.attempts >= MAX_ATTEMPTS) {
                TwoFAData.deleteByUserId(record.userId)
                sessions.clear<Temp2FASession>()
                respondText(
                    "<div class='error-message'>Too Many Failed Attempts</div>",
                    ContentType.Text.Html
                )
                return@timed
            }
            
            respondText(
                "<div class='error-message'>Invalid or expired code</div>",
                ContentType.Text.Html
            )
            return@timed
        }

        TwoFAData.deleteByUserId(record.userId)
        sessions.clear<Temp2FASession>()

        val userQuery = UserData.queryDatabase(
            whereArgs = WhereArgs(
                "${UserColumns.ID.name} = ?",
                listOf(record.userId)
            )
        )

        if (userQuery.isEmpty()) {
            return@timed respondText(
                "<div class='error-message'>User not found</div>",
                ContentType.Text.Html
            )
        }

        val user = userQuery.first().dataClass
        
        if (user.verifiedAccount != true) {
            user.verifiedAccount = true
            user.update()
        }

        sessions.set(SessionData.createSession(user.id).toTokenSession())
        response.headers.append("HX-Redirect", "/")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleSendVerification() {
    val userSession = sessions.get<SessionToken>()
        ?: return respondRedirect("/login")

    val userQuery = UserData.queryByToken(userSession.token)

    if (userQuery.isEmpty()) {
        return respondRedirect("/")
    }

    val user = userQuery.first().dataClass

    if (user.verifiedAccount == true) {
        return respondRedirect("/")
    }

    val existing = TwoFAData.queryDatabase(
        whereArgs = WhereArgs("${TwoFAColumns.ID.name} = ?", listOf(user.id))
    )

    if (existing.isNotEmpty()) {
        val record = existing.first().dataClass
        if (record.ttl.after(Timestamp.from(Instant.now()))) {
            return respondRedirect("/verify")
        }
    }

    val code = generate2FACode()
    val hashedCode = BCrypt.hashpw(code, BCrypt.gensalt(10))

    val expiration = Timestamp.from(
        Instant.now().plus(5, ChronoUnit.MINUTES)
    )

    TwoFAData.deleteByUserId(user.id)
    val token = TwoFAData.EMPTY.generateToken()

    TwoFAData(
        userId = user.id,
        ttl = expiration,
        code_hash = hashedCode,
        attempts = 0,
        sessionToken = token
    ).insertIntoDatabase()

    val loginQuery = LoginData.queryDatabase(
        whereArgs = WhereArgs("${LoginColumns.ID.name} = ?", listOf(user.loginId))
    )

    if (loginQuery.isEmpty()) {
        return respondRedirect("/")
    }

    val loginData = loginQuery.first().dataClass

    EmailService.send2FA(loginData.email, code)
    sessions.set(Temp2FASession(token))
    respondRedirect("/verify")
}

fun generate2FACode(): String {
    return (100000..999999).random().toString()
}