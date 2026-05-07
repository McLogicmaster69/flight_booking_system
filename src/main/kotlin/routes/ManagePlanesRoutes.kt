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
import java.time.LocalDate
import java.time.LocalTime

fun Route.managePlanesRoutes() {
    get("/managePlanes") { call.handleManagePlanesLoad() }
    post("/managePlanes/create") { call.handleCreatePlanePost() }
    post("/managePlanes/update") { call.handleUpdatePlanePost() }
    post("/managePlanes/delete") { call.handleDeletePlanePost() }
}

data class PlaneView(
    val id: Int,
    val registrationCode: String,
    val modelId: Int,
    val modelName: String?,
    val currentLocation: Int,
    val currentLocationName: String?,
    val currentLocationDate: LocalDate,
    val currentLocationTime: LocalTime,
)

private suspend fun ApplicationCall.handleManagePlanesLoad() {
    timed("T0_manage_planes", jsMode()) {
        if (!requireAdmin()) return@timed

        val pebble = getEngine()

        val search = request.queryParameters["search"]?.trim().orEmpty()
        val page = request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val pageSize = 5

        val models = PlaneModelData.queryDatabase().map { it.dataClass }
        val destinations = DestinationData.queryDatabase().map { it.dataClass }

        val allPlanes =
            PlaneData.queryDatabase().map { result ->
                val plane = result.dataClass

                val model =
                    PlaneModelData
                        .queryDatabase(
                            whereArgs =
                                WhereArgs(
                                    "${PlaneModelColumns.ID.name} = ?",
                                    listOf(plane.modelId),
                                ),
                        ).firstOrNull()
                        ?.dataClass

                val destination =
                    DestinationData
                        .queryDatabase(
                            whereArgs =
                                WhereArgs(
                                    "${DestinationColumns.ID.name} = ?",
                                    listOf(plane.currentLocation),
                                ),
                        ).firstOrNull()
                        ?.dataClass

                PlaneView(
                    id = plane.id,
                    registrationCode = plane.registrationCode,
                    modelId = plane.modelId,
                    modelName = model?.name,
                    currentLocation = plane.currentLocation,
                    currentLocationName = destination?.cityName,
                    currentLocationDate = plane.currentLocationDate,
                    currentLocationTime = plane.currentLocationTime,
                )
            }

        val filteredPlanes =
            if (search.isBlank()) {
                allPlanes
            } else {
                allPlanes.filter { plane ->
                    plane.registrationCode.contains(search, ignoreCase = true) ||
                        plane.modelName?.contains(search, ignoreCase = true) == true ||
                        plane.currentLocationName?.contains(search, ignoreCase = true) == true
                }
            }

        val totalPages = ((filteredPlanes.size + pageSize - 1) / pageSize).coerceAtLeast(1)
        val safePage = page.coerceAtMost(totalPages)

        val planes =
            filteredPlanes
                .drop((safePage - 1) * pageSize)
                .take(pageSize)

        val viewModel =
            mapOf(
                "title" to "Manage Planes",
                "layout" to "admin",
                "activePage" to "managePlanes",
                "inNav" to true,
                "isAdmin" to true,
                "planes" to planes,
                "models" to models,
                "destinations" to destinations,
                "search" to search,
                "page" to safePage,
                "totalPages" to totalPages,
            )

        val template = pebble.getTemplate("admin/managePlanes.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, viewModel)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handleCreatePlanePost() {
    timed("T1_create_plane", jsMode()) {
        if (!requireAdmin()) return@timed

        val params = receiveParameters()

        val modelId = params["modelId"]?.toIntOrNull()
        val registrationCode = params["registrationCode"]?.trim()
        val currentLocation = params["currentLocation"]?.toIntOrNull()
        val currentLocationDate = params["currentLocationDate"]?.let { LocalDate.parse(it) }
        val currentLocationTime = params["currentLocationTime"]?.let { LocalTime.parse(it) }

        if (
            registrationCode.isNullOrBlank() ||
            modelId == null ||
            currentLocation == null ||
            currentLocationDate == null ||
            currentLocationTime == null
        ) {
            respondText("Please fill in all plane fields", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val existing =
            PlaneData.queryDatabase(
                whereArgs =
                    WhereArgs(
                        "${PlaneColumns.REGISTRATION_CODE.name} = ?",
                        listOf(registrationCode),
                    ),
            )

        if (existing.isNotEmpty()) {
            respondText("That plane already exists", status = HttpStatusCode.BadRequest)
            return@timed
        }

        PlaneData(
            registrationCode = registrationCode,
            modelId = modelId,
            currentLocation = currentLocation,
            currentLocationDate = currentLocationDate,
            currentLocationTime = currentLocationTime,
        ).insertIntoDatabase()

        response.headers.append("HX-Redirect", "/managePlanes")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleUpdatePlanePost() {
    timed("T2_update_plane", jsMode()) {
        if (!requireAdmin()) return@timed

        val params = receiveParameters()

        val planeId = params["planeId"]?.toIntOrNull()
        val registrationCode = params["registrationCode"]?.trim()
        val modelId = params["modelId"]?.toIntOrNull()
        val currentLocation = params["currentLocation"]?.toIntOrNull()
        val currentLocationDate = params["currentLocationDate"]?.let { LocalDate.parse(it) }
        val currentLocationTime = params["currentLocationTime"]?.let { LocalTime.parse(it) }

        if (
            planeId == null ||
            registrationCode.isNullOrBlank() ||
            modelId == null ||
            currentLocation == null ||
            currentLocationDate == null ||
            currentLocationTime == null
        ) {
            respondText("Invalid plane details", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val duplicate =
            PlaneData.queryDatabase(
                whereArgs =
                    WhereArgs(
                        "${PlaneColumns.REGISTRATION_CODE.name} = ? AND ${PlaneColumns.ID.name} != ?",
                        listOf(registrationCode, planeId),
                    ),
            )

        if (duplicate.isNotEmpty()) {
            respondText("That plane already exists", status = HttpStatusCode.BadRequest)
            return@timed
        }

        PlaneData.updateTable(
            values =
                mapOf(
                    PlaneColumns.REGISTRATION_CODE to registrationCode,
                    PlaneColumns.MODEL_ID to modelId,
                    PlaneColumns.CURRENT_LOCATION to currentLocation,
                    PlaneColumns.CURRENT_LOCATION_DATE to currentLocationDate.toString(),
                    PlaneColumns.CURRENT_LOCATION_TIME to currentLocationTime.toString(),
                ),
            whereArgs =
                WhereArgs(
                    "${PlaneColumns.ID.name} = ?",
                    listOf(planeId),
                ),
        )

        response.headers.append("HX-Redirect", "/managePlanes")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleDeletePlanePost() {
    timed("T3_delete_plane", jsMode()) {
        if (!requireAdmin()) return@timed

        val planeId = receiveParameters()["planeId"]?.toIntOrNull()

        if (planeId == null) {
            respondText("Invalid plane", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val flightsUsingPlane =
            FlightData.queryDatabase(
                whereArgs =
                    WhereArgs(
                        "${FlightColumns.PLANE_ID.name} = ?",
                        listOf(planeId),
                    ),
            )

        if (flightsUsingPlane.isNotEmpty()) {
            respondText(
                "Cannot delete this plane because one or more flights use it",
                status = HttpStatusCode.BadRequest,
            )
            return@timed
        }

        PlaneData.delete(planeId)

        response.headers.append("HX-Redirect", "/managePlanes")
        respond(HttpStatusCode.OK)
    }
}
