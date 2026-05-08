package routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.StringWriter
import utils.timed
import utils.jsMode

/**
 * Registers static page routes.
 */
fun Route.pagesRoutes() {
    get("/settings") { call.handleSettingsLoad() }
    get("/help") { call.handleHelpLoad() }
}

/**
 * Renders a generic Pebble page with shared navigation and query-parameter state.
 */
private suspend fun ApplicationCall.render(
    template: String,
    title: String,
    inNav: Boolean = true,
) {
    timed("T0_page_$title", jsMode()) {
        val pebble = getEngine()

        // Read optional display state from query parameters.
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

/**
 * Loads and renders the settings page.
 */
private suspend fun ApplicationCall.handleSettingsLoad() {
    timed("T1_Handle_Settings_Load", jsMode()) {
        val pebble = getEngine()

        val model: Map<String, Any?> =
            mapOf(
                "title" to "Settings",
                "inNav" to java.lang.Boolean.valueOf(true),
                "activePage" to "settings",
            )

        val writer = StringWriter()
        val template = pebble.getTemplate("settings/index.peb")

        fullEvaluate(template, writer, model)

        respondText(writer.toString(), ContentType.Text.Html)
    }
}

/**
 * Loads and renders the help page.
 */
private suspend fun ApplicationCall.handleHelpLoad() {
    timed("T1_Handle_Settings_Load", jsMode()) {
        val pebble = getEngine()

        val model: Map<String, Any?> =
            mapOf(
                "title" to "Help",
                "inNav" to java.lang.Boolean.valueOf(true),
                "activePage" to "help",
            )

        val writer = StringWriter()
        val template = pebble.getTemplate("help/index.peb")

        fullEvaluate(template, writer, model)

        respondText(writer.toString(), ContentType.Text.Html)
    }
}
