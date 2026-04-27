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

fun Route.adminRoutes() {
    get("/admin") { call.handleAdminLoad() }
}

private suspend fun ApplicationCall.handleAdminLoad() {
    timed("T0_admin", jsMode()) {
        val pebble = getEngine()
        val loggedState: LoggedInState = loggedIn()

        if (!loggedState.logged_in || loggedState.session == null) {
            respondRedirect("/login")
            return@timed
        }

        val userQuery = UserData.queryByToken(loggedState.session.token)

        if (userQuery.isEmpty()) {
            respondRedirect("/login")
            return@timed
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
            return@timed
        }

        val model = mapOf(
            "title" to "Admin",
            "layout" to "admin",
            "activePage" to "admin",
            "inNav" to true,
            "isAdmin" to true
        )

        val template = pebble.getTemplate("admin/index.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}