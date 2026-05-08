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

fun Route.adminRoutes() {
    get("/admin") {
        call.handleAdminLoad()
    }
}

private suspend fun ApplicationCall.handleAdminLoad() {
    timed("T0_admin", jsMode()) {
        if (!requireAdmin()) return@timed

        // Parse the period query parameter
        val periodDays = request.queryParameters["period"]?.toLongOrNull() ?: 30L

        val now = LocalDate.now()
        val startDate = now.minusDays(periodDays)
        val inSevenDays = now.plusDays(7)

        // Prepare Data for Most Popular Times
        val popularTimesLabels = (0..23).map { "${it.toString().padStart(2, '0')}:00" }
        val popularTimesData =
            (0..23).map { hour ->
                FlightData.getTimePopularity(LocalTime.of(hour, 0), startDate, inSevenDays)
            }

        // Prepare Data for Most Popular Routes
        val popularRoutesList = RouteData.getPopularRoutes(10, periodDays)
        val popularRoutesLabels =
            popularRoutesList.map { result ->
                val route = result.dataClass
                val start = DestinationData.getDestinationName(route.startDestination).split("-")[0].trim()
                val end = DestinationData.getDestinationName(route.endDestination).split("-")[0].trim()
                "$start to $end"
            }
        val popularRoutesData =
            popularRoutesList.map { result ->
                result.dataClass.getRoutePopularity(startDate, inSevenDays)
            }

        val pebble = getEngine()
        val model =
            mapOf(
                "title" to "Admin Analytics Dashboard",
                "layout" to "admin",
                "activePage" to "admin",
                "inNav" to true,
                "isAdmin" to true,
                "selectedPeriod" to periodDays, // Track this to set the dropdown default
                "popularTimesLabelsJson" to Json.encodeToString(popularTimesLabels),
                "popularTimesDataJson" to Json.encodeToString(popularTimesData),
                "popularRoutesLabelsJson" to Json.encodeToString(popularRoutesLabels),
                "popularRoutesDataJson" to Json.encodeToString(popularRoutesData),
            )

        val template = pebble.getTemplate("admin/index.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}
