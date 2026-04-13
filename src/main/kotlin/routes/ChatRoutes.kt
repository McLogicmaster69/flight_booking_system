package routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun Route.chatRoutes() {
    post("/api/chat") {
        val requestBody = call.receiveText()
        val userMessage = Json.parseToJsonElement(requestBody).jsonObject["message"]?.jsonPrimitive?.content ?: ""

        val apiKey = System.getenv("GEMINI_API_KEY") ?: ""
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

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

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() == 200) {
            val root = Json.parseToJsonElement(response.body()).jsonObject
            val text = root["candidates"]?.jsonArray?.get(0)?.jsonObject?.get("content")?.jsonObject?.get("parts")?.jsonArray?.get(0)?.jsonObject?.get("text")?.jsonPrimitive?.content ?: "Error"
            call.respondText(buildJsonObject { put("reply", text) }.toString(), ContentType.Application.Json)
        } else {
            call.respondText(buildJsonObject { put("reply", "The support service is currently unavailable.") }.toString(), ContentType.Application.Json)
        }
    }
}
