package routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.StringWriter
import utils.timed
import utils.jsMode

/**
 * UI template routes (server-rendered Pebble pages).
 *
 * Notes:
 * - These are *template scaffolds* only: they intentionally use placeholder data.
 * - Later, the team can wire them to real DB/API endpoints.
 */
fun Route.pagesRoutes() {
    get("/settings") { call.render("settings/index.peb", "Settings") }
    get("/help") { call.render("help/index.peb", "Help") }
}

private suspend fun ApplicationCall.render(
    template: String,
    title: String,
    inNav: Boolean = true,
) {
    timed("T0_page_$title", jsMode()) {
        val pebble = getEngine()

        val isLoggedIn = request.queryParameters["loggedIn"] == "1"
        val cookiesAccepted = request.queryParameters["cookies"] == "1"

        val writer = StringWriter()
        pebble.getTemplate(template).evaluate(
            writer,
            mapOf(
                "title" to title,
                "isLoggedIn" to isLoggedIn,
                "cookiesAccepted" to cookiesAccepted,
                "inNav" to inNav,
                "language" to (request.queryParameters["lang"] ?: "en"),
                "activePage" to "",
                "layout" to "",
                "headerRightText" to "",
            ) + loggedMap(),
        )

        respondText(writer.toString(), ContentType.Text.Html)
    }
}
