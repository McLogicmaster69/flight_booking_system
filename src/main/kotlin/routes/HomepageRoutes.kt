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

fun Route.homepageRoutes() {
    get("/") { call.handleLoadPage() }
}

private suspend fun ApplicationCall.handleLoadPage() {
    timed("T0_homepage_load", jsMode()) {
        val pebble = getEngine()

        val model = mapOf(
            "title" to "Homepage"
        )
        
        val template = pebble.getTemplate("homepage/index.peb")
        val writer = StringWriter()
        template.evaluate(writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}
