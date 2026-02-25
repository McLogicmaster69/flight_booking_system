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

fun ApplicationCall.isHtmx(): Boolean = request.headers["HX-Request"]?.equals("true", ignoreCase = true) == true

fun ApplicationCall.getEngine() : PebbleEngine = PebbleEngine.Builder().loader(
    io.pebbletemplates.pebble.loader.ClasspathLoader().apply {
        prefix = "templates/"
    }).build()