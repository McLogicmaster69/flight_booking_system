package routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.StringWriter
import utils.timed
import utils.jsMode
import auth.*
import data.*
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.max

fun Route.adminRoutes() {
    get("/admin") {
        call.handleAdminLoad()
    }
}

private suspend fun ApplicationCall.handleAdminLoad() {
    timed("T0_admin", jsMode()) {
        if (!requireAdmin()) return@timed

        val periodDays = request.queryParameters["period"]?.toLongOrNull() ?: 30L
        val now = LocalDate.now()
        val startDate = now.minusDays(periodDays)
        val inSevenDays = now.plusDays(7)

        val popularTimesLabels = (0..23).map { "${it.toString().padStart(2, '0')}:00" }
        val popularTimesData = (0..23).map { hour ->
            FlightData.getTimePopularity(LocalTime.of(hour, 0), startDate, inSevenDays).toDouble()
        }

        val popularRoutesList = RouteData.getPopularRoutes(10, periodDays)
        val popularRoutesLabels = popularRoutesList.map { result ->
            val route = result.dataClass
            val start = DestinationData.getDestinationName(route.startDestination).split("-")[0].trim()
            val end = DestinationData.getDestinationName(route.endDestination).split("-")[0].trim()
            "$start to $end"
        }
        val popularRoutesData = popularRoutesList.map { result ->
            result.dataClass.getRoutePopularity(startDate, inSevenDays).toDouble()
        }

        val predictedTimesData = predictNextValues(popularTimesData, popularTimesData.size)
        val predictedRoutesData = predictNextValues(popularRoutesData, popularRoutesData.size)

        val pebble = getEngine()
        val model = mapOf(
            "title" to "Admin Analytics Dashboard",
            "layout" to "admin",
            "activePage" to "admin",
            "inNav" to true,
            "isAdmin" to true,
            "selectedPeriod" to periodDays,
            "popularTimesLabelsJson" to Json.encodeToString(popularTimesLabels),
            "popularTimesDataJson" to Json.encodeToString(popularTimesData),
            "predictedTimesDataJson" to Json.encodeToString(predictedTimesData),
            "popularRoutesLabelsJson" to Json.encodeToString(popularRoutesLabels),
            "popularRoutesDataJson" to Json.encodeToString(popularRoutesData),
            "predictedRoutesDataJson" to Json.encodeToString(predictedRoutesData),
        )

        val template = pebble.getTemplate("admin/index.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private fun predictNextValues(historicalData: List<Double>, numPredictions: Int): List<Double> {
    if (historicalData.isEmpty()) return List(numPredictions) { 0.0 }
    if (historicalData.size == 1) return List(numPredictions) { historicalData.first() }

    val n = historicalData.size
    var sumX = 0.0
    var sumY = 0.0
    var sumXY = 0.0
    var sumX2 = 0.0

    for (i in 0 until n) {
        val x = i.toDouble()
        val y = historicalData[i]
        sumX += x
        sumY += y
        sumXY += x * y
        sumX2 += x * x
    }

    val denominator = (n * sumX2) - (sumX * sumX)
    if (denominator == 0.0) return List(numPredictions) { historicalData.average() }

    val slope = ((n * sumXY) - (sumX * sumY)) / denominator
    val intercept = (sumY - (slope * sumX)) / n

    return (0 until numPredictions).map { i ->
        max(0.0, (slope * (n + i).toDouble()) + intercept)
    }
}
