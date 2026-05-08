package routes

import data.*
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import utils.EmailService

private val dotenv = dotenv()
val apiKey = dotenv["GEMINI_API_KEY"] ?: throw IllegalStateException("GEMINI_API_KEY not set")
val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

fun Route.chatRoutes() {
    post("/api/chat") {
        try {
            // Check if this is a Form submission from the Help page
            val isForm = call.request.contentType().match(ContentType.Application.FormUrlEncoded)

            if (isForm) {
                val params = call.receiveParameters()
                val email = params["email"] ?: "Unknown"
                val message = params["message"] ?: ""

                // Send an email to the admin
                EmailService.sendSupportEmail(email, message)

                // Return plain HTML for the HTMX form swap
                call.respondText(
                    "Thank you! Your bug report has been successfully sent to our support team.",
                    ContentType.Text.Html
                )
                return@post
            }

            // Otherwise, handle it as a JSON payload for the Chat Widget
            val requestBody = call.receiveText()
            val userMessage = Json.parseToJsonElement(requestBody).jsonObject["message"]?.jsonPrimitive?.content ?: ""

            val upcomingFlights = FlightData
                .queryDatabase()
                .map { it.dataClass }
                .take(30)

            val flightContext =
                upcomingFlights.joinToString("\n") { flight ->
                    val route = RouteData.queryDatabase(flight.routeId).firstOrNull()?.dataClass
                    val start = route?.startDestination?.let { DestinationData.getDestinationName(it) } ?: "Unknown"
                    val end = route?.endDestination?.let { DestinationData.getDestinationName(it) } ?: "Unknown"
                    val durationMins = route?.let { RouteData.getDurationMinutes(it.id) } ?: 0L
                    val price = "%.2f".format((durationMins * 50L) / 100.0)

                    "Flight from $start to $end on ${flight.date} at ${flight.time}. Estimated base price: £$price."
                }

            val promptText =
                """
                You are an official customer support agent for SkyBridge Airways.
                You help users book flights, manage trips, and understand the loyalty rewards program.
                Be polite, concise, and never break character.

                Here is the current available flight data you must use to answer questions:
                $flightContext

                The user says: $userMessage
                """.trimIndent()

            val jsonPayload =
                buildJsonObject {
                    put(
                        "contents",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put(
                                        "parts",
                                        buildJsonArray {
                                            add(
                                                buildJsonObject {
                                                    put("text", promptText)
                                                },
                                            )
                                        },
                                    )
                                },
                            )
                        },
                    )
                }.toString()

            val client = HttpClient.newHttpClient()
            val request =
                HttpRequest
                    .newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build()

            var response: HttpResponse<String>? = null
            var attempts = 0
            val maxAttempts = 3
            var success = false

            while (attempts < maxAttempts && !success) {
                response = client.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() == 200) {
                    success = true
                } else if (response.statusCode() == 503) {
                    attempts++
                    if (attempts < maxAttempts) {
                        delay(2000L)
                    }
                } else {
                    break
                }
            }

            if (success && response != null) {
                val root = Json.parseToJsonElement(response.body()).jsonObject
                val text =
                    root["candidates"]
                        ?.jsonArray
                        ?.get(0)?.jsonObject
                        ?.get("content")?.jsonObject
                        ?.get("parts")
                        ?.jsonArray
                        ?.get(0)?.jsonObject
                        ?.get("text")
                        ?.jsonPrimitive
                        ?.content ?: "Error: Could not parse response."

                call.respondText(buildJsonObject { put("reply", text) }.toString(), ContentType.Application.Json)

            } else if (response != null) {
                val errorMsg = "Google API Error (${response.statusCode()}): ${response.body()}"
                call.respondText(buildJsonObject { put("reply", errorMsg) }.toString(), ContentType.Application.Json)
            } else {
                val errorMsg = "Failed to connect to AI service."
                call.respondText(buildJsonObject { put("reply", errorMsg) }.toString(), ContentType.Application.Json)
            }
        } catch (e: Exception) {
            val errorMsg = "Server Exception: ${e.message}"
            call.respondText(
                buildJsonObject { put("reply", errorMsg) }.toString(),
                ContentType.Application.Json,
            )
        }
    }
}
