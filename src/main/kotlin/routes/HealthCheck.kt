package routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*

fun Routing.configureHealthCheck() {
    get("/health") {
        call.respondText(
            """
            {
              "status": "ok",
              "service": "Flight Booking System",
              "timestamp": "${System.currentTimeMillis()}",
              "version": "1.0-SNAPSHOT"
            }
            """.trimIndent(),
            ContentType.Application.Json,
        )
    }
}
