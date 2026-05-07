package routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.pebbletemplates.pebble.PebbleEngine
import java.io.StringWriter
import utils.jsMode
import utils.logValidationError
import utils.timed
import data.*

fun Route.homepageRoutes() {
    get("/") { call.handleLoadPage() }
}

private suspend fun ApplicationCall.handleLoadPage() {
    timed("T0_homepage_load", jsMode()) {
        val pebble = getEngine()

        val model: Map<String, Any?> = mapOf(
            "title" to "Homepage",
            "inNav" to java.lang.Boolean.valueOf(true),
            "activePage" to "home",
            "layout" to "",
            "headerRightText" to "",
            "destinations" to DestinationData.getDestinationNames()
        )

        val writer = StringWriter()
        val template = pebble.getTemplate("homepage/index.peb")

        fullEvaluate(template, writer, model)

        respondText(writer.toString(), ContentType.Text.Html)
    }
}
