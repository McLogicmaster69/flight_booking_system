package auth

import routes.loggedIn
import data.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

suspend fun ApplicationCall.requireAdmin(): Boolean {
    val loggedState: LoggedInState = loggedIn()

    if (!loggedState.logged_in || loggedState.session == null) {
        respondRedirect("/login")
        return false
    }

    val userQuery = UserData.queryByToken(loggedState.session.token)

    if (userQuery.isEmpty()) {
        respondRedirect("/login")
        return false
    }

    val user = userQuery.first().dataClass

    val adminQuery = AdminData.queryDatabase(
        whereArgs = WhereArgs(
            "${AdminColumns.LOGIN_ID.name} = ?",
            listOf(user.loginId)
        )
    )

    if (adminQuery.isEmpty()) {
        respondText("403 Forbidden", ContentType.Text.Plain, HttpStatusCode.Forbidden)
        return false
    }

    return true
}