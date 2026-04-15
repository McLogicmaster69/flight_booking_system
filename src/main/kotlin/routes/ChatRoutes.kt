package routes

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

fun Route.chatRoutes() {
    post("/api/chat") {
        try {
            val requestBody = call.receiveText()
            val userMessage = Json.parseToJsonElement(requestBody).jsonObject["message"]?.jsonPrimitive?.content ?: ""

            val apiKey = "AIzaSyAXdJc6G5jfV1WKYJnh-2590sPqhnGHySQ"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val promptText = "You are an official customer support agent for SkyBridge Airways. You help users book flights, check routes like Luton to Tokyo, manage trips, and understand the loyalty rewards program. Be polite, concise, and never break character. The user says: $userMessage"

            val jsonPayload = buildJsonObject {
                put("contents", buildJsonArray {
                    add(buildJsonObject {
                        put("parts", buildJsonArray {
                            add(buildJsonObject {
                                put("text", promptText)
                            })
                        })
                    })
                })
            }.toString()

            val client = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder()
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
                val text = root["candidates"]?.jsonArray?.get(0)?.jsonObject?.get("content")?.jsonObject?.get("parts")?.jsonArray?.get(0)?.jsonObject?.get("text")?.jsonPrimitive?.content ?: "Error: Could not parse response."
                call.respondText(buildJsonObject { put("reply", text) }.toString(), ContentType.Application.Json)
            } else if (response != null) {
                val errorMsg = "Google API Error (${response.statusCode()}): ${response.body()}"
                call.respondText(buildJsonObject { put("reply", errorMsg) }.toString(), ContentType.Application.Json)
            } else {
                call.respondText(buildJsonObject { put("reply", "Failed to connect to AI service.") }.toString(), ContentType.Application.Json)
            }
        } catch (e: Exception) {
            call.respondText(buildJsonObject { put("reply", "Server Exception: ${e.message}") }.toString(), ContentType.Application.Json)
        }
    }
}