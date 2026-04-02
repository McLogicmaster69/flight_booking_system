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
    get("/admin") { call.render("admin/index.peb", "Admin", requireAdmin = true) }
}

private suspend fun ApplicationCall.render(
    template: String,
    title: String,
    inNav: Boolean = true,
    requireAdmin: Boolean = false,
) {
    timed("T0_page_$title", jsMode()) {
        val pebble = getEngine()

        // Placeholder session flags (swap with real auth later)
        val isLoggedIn = request.queryParameters["loggedIn"] == "1"
        val isAdmin = request.queryParameters["admin"] == "1"
        val cookiesAccepted = request.queryParameters["cookies"] == "1"

        if (requireAdmin && !isAdmin) {
            val writer = StringWriter()
            pebble.getTemplate("errors/403.peb").evaluate(
                writer,
                mapOf(
                    "title" to "403",
                    "isLoggedIn" to isLoggedIn,
                    "isAdmin" to isAdmin,
                    "cookiesAccepted" to cookiesAccepted,
                    "inNav" to false,
                    "language" to (request.queryParameters["lang"] ?: "en"),
                    "activePage" to "",
                    "layout" to "",
                    "headerRightText" to "",
                ) + loggedMap(),
            )
            respondText(writer.toString(), ContentType.Text.Html, HttpStatusCode.Forbidden)
            return@timed
        }

        val writer = StringWriter()
        pebble.getTemplate(template).evaluate(
            writer,
            mapOf(
                "title" to title,
                "isLoggedIn" to isLoggedIn,
                "isAdmin" to isAdmin,
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
