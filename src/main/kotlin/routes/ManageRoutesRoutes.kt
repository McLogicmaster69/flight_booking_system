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
import io.ktor.server.request.*
import java.time.LocalTime

fun Route.manageRoutesRoutes() {
    get("/manageRoutes") { call.handleManageRoutesLoad() }
    post("/manageRoutes/create") { call.handleCreateRoutePost() }
    post("/manageRoutes/update") { call.handleUpdateRoutePost() }
    post("/manageRoutes/delete") { call.handleDeleteRoutePost() }
}

data class RouteView(
    val id: Int,
    val startDestination: Int,
    val startDestinationName: String?,
    val endDestination: Int,
    val endDestinationName: String?,
    val duration: LocalTime,
)

private suspend fun ApplicationCall.handleManageRoutesLoad() {
    timed("T0_manage_routes", jsMode()) {
        if (!requireAdmin()) return@timed

        val pebble = getEngine()

        val search = request.queryParameters["search"]?.trim().orEmpty()
        val page = request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val pageSize = 5

        val destinations = DestinationData.queryDatabase().map { it.dataClass }

        val allRoutes =
            RouteData.queryDatabase().map { result ->
                val route = result.dataClass

                val start = DestinationData.queryDatabase(route.startDestination).firstOrNull()?.dataClass
                val end = DestinationData.queryDatabase(route.endDestination).firstOrNull()?.dataClass

                RouteView(
                    id = route.id,
                    startDestination = route.startDestination,
                    startDestinationName = start?.cityName,
                    endDestination = route.endDestination,
                    endDestinationName = end?.cityName,
                    duration = route.duration,
                )
            }

        val filteredRoutes =
            if (search.isBlank()) {
                allRoutes
            } else {
                allRoutes.filter { route ->
                    route.startDestinationName?.contains(search, ignoreCase = true) == true ||
                        route.endDestinationName?.contains(search, ignoreCase = true) == true
                }
            }

        val totalPages = ((filteredRoutes.size + pageSize - 1) / pageSize).coerceAtLeast(1)
        val safePage = page.coerceAtMost(totalPages)

        val routes =
            filteredRoutes
                .drop((safePage - 1) * pageSize)
                .take(pageSize)

        val model =
            mapOf(
                "title" to "Manage Routes",
                "layout" to "admin",
                "activePage" to "manageRoutes",
                "inNav" to true,
                "isAdmin" to true,
                "routes" to routes,
                "destinations" to destinations,
                "search" to search,
                "page" to safePage,
                "totalPages" to totalPages,
            )

        val template = pebble.getTemplate("admin/manageRoutes.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handleCreateRoutePost() {
    timed("T1_create_route", jsMode()) {
        if (!requireAdmin()) return@timed

        val params = receiveParameters()

        val startDestination = params["startDestination"]?.toIntOrNull()
        val endDestination = params["endDestination"]?.toIntOrNull()
        val duration = params["duration"]?.let { LocalTime.parse(it) }

        if (startDestination == null || endDestination == null || duration == null) {
            respondText("Please fill in all route fields", status = HttpStatusCode.BadRequest)
            return@timed
        }

        if (startDestination == endDestination) {
            respondText("Start and end destination cannot be the same", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val existing =
            RouteData.queryDatabase(
                whereArgs =
                    WhereArgs(
                        "${RouteColumns.START_DESTINATION.name} = ? AND ${RouteColumns.END_DESTINATION.name} = ?",
                        listOf(startDestination, endDestination),
                    ),
            )

        if (existing.isNotEmpty()) {
            respondText("That route already exists", status = HttpStatusCode.BadRequest)
            return@timed
        }

        RouteData(
            startDestination = startDestination,
            endDestination = endDestination,
            duration = duration,
        ).insertIntoDatabase()

        response.headers.append("HX-Redirect", "/manageRoutes")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleUpdateRoutePost() {
    timed("T2_update_route", jsMode()) {
        if (!requireAdmin()) return@timed

        val params = receiveParameters()

        val routeId = params["routeId"]?.toIntOrNull()
        val startDestination = params["startDestination"]?.toIntOrNull()
        val endDestination = params["endDestination"]?.toIntOrNull()
        val duration = params["duration"]?.let { LocalTime.parse(it) }

        if (routeId == null || startDestination == null || endDestination == null || duration == null) {
            respondText("Invalid route details", status = HttpStatusCode.BadRequest)
            return@timed
        }

        if (startDestination == endDestination) {
            respondText("Start and end destination cannot be the same", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val duplicate =
            RouteData.queryDatabase(
                whereArgs =
                    WhereArgs(
                        "${RouteColumns.START_DESTINATION.name} = ? AND ${RouteColumns.END_DESTINATION.name} = ? AND ${RouteColumns.ID.name} != ?",
                        listOf(startDestination, endDestination, routeId),
                    ),
            )

        if (duplicate.isNotEmpty()) {
            respondText("That route already exists", status = HttpStatusCode.BadRequest)
            return@timed
        }

        RouteData.updateTable(
            values =
                mapOf(
                    RouteColumns.START_DESTINATION to startDestination,
                    RouteColumns.END_DESTINATION to endDestination,
                    RouteColumns.DURATION to duration.toString(),
                ),
            whereArgs =
                WhereArgs(
                    "${RouteColumns.ID.name} = ?",
                    listOf(routeId),
                ),
        )

        response.headers.append("HX-Redirect", "/manageRoutes")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleDeleteRoutePost() {
    timed("T3_delete_route", jsMode()) {
        if (!requireAdmin()) return@timed

        val routeId = receiveParameters()["routeId"]?.toIntOrNull()

        if (routeId == null) {
            respondText("Invalid route", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val flightsUsingRoute =
            FlightData.queryDatabase(
                whereArgs =
                    WhereArgs(
                        "${FlightColumns.ROUTE_ID.name} = ?",
                        listOf(routeId),
                    ),
            )

        if (flightsUsingRoute.isNotEmpty()) {
            respondText(
                "Cannot delete this route because one or more flights use it",
                status = HttpStatusCode.BadRequest,
            )
            return@timed
        }

        RouteData.delete(routeId)

        response.headers.append("HX-Redirect", "/manageRoutes")
        respond(HttpStatusCode.OK)
    }
}
