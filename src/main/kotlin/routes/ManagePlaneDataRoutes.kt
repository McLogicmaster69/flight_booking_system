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

fun Route.managePlaneDataRoutes() {
    get("/managePlaneData") { call.handleManagePlaneDataLoad() }
    get("/managePlaneData/model/edit/{id}") { call.handleEditPlaneModelLoad() }
    get("/managePlaneData/manufacturer/edit/{id}") { call.handleEditManufacturerLoad() }
    post("/managePlaneData/model/create") { call.handleCreatePlaneModelPost() }
    post("/managePlaneData/model/update") { call.handleUpdatePlaneModelPost() }
    post("/managePlaneData/model/delete") { call.handleDeletePlaneModelPost() }
    post("/managePlaneData/manufacturer/create") { call.handleCreateManufacturerPost() }
    post("/managePlaneData/manufacturer/update") { call.handleUpdateManufacturerPost() }
    post("/managePlaneData/manufacturer/delete") { call.handleDeleteManufacturerPost() }
}

data class PlaneModelView(
    val id: Int,
    val name: String,
    val manufacturerId: Int,
    val manufacturerName: String?,
    val capacity: Int,
    val pilots: Int,
    val attendants: Int,
)

private suspend fun ApplicationCall.handleManagePlaneDataLoad() {
    timed("T0_manage_plane_data", jsMode()) {
        if (!requireAdmin()) return@timed

        val pebble = getEngine()

        val modelSearch = request.queryParameters["modelSearch"]?.trim().orEmpty()
        val modelPage = request.queryParameters["modelPage"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1

        val manufacturerSearch = request.queryParameters["manufacturerSearch"]?.trim().orEmpty()
        val manufacturerPage = request.queryParameters["manufacturerPage"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1

        val pageSize = 5

        val allManufacturers = ManufacturerData.queryDatabase().map { it.dataClass }

        val allModels =
            PlaneModelData.queryDatabase().map { result ->
                val model = result.dataClass

                val manufacturer =
                    ManufacturerData
                        .queryDatabase(
                            whereArgs =
                                WhereArgs(
                                    "${ManufacturerColumns.ID.name} = ?",
                                    listOf(model.manufacturerId),
                                ),
                        ).firstOrNull()
                        ?.dataClass

                PlaneModelView(
                    id = model.id,
                    name = model.name,
                    manufacturerId = model.manufacturerId,
                    manufacturerName = manufacturer?.name,
                    capacity = model.capacity,
                    pilots = model.pilots,
                    attendants = model.attendants,
                )
            }

        val filteredModels =
            if (modelSearch.isBlank()) {
                allModels
            } else {
                allModels.filter { model ->
                    model.name.contains(modelSearch, ignoreCase = true) ||
                        model.manufacturerName?.contains(modelSearch, ignoreCase = true) == true
                }
            }

        val filteredManufacturers =
            if (manufacturerSearch.isBlank()) {
                allManufacturers
            } else {
                allManufacturers.filter { manufacturer ->
                    manufacturer.name?.contains(manufacturerSearch, ignoreCase = true) == true
                }
            }

        val totalModelPages = ((filteredModels.size + pageSize - 1) / pageSize).coerceAtLeast(1)
        val safeModelPage = modelPage.coerceAtMost(totalModelPages)

        val totalManufacturerPages = ((filteredManufacturers.size + pageSize - 1) / pageSize).coerceAtLeast(1)
        val safeManufacturerPage = manufacturerPage.coerceAtMost(totalManufacturerPages)

        val models =
            filteredModels
                .drop((safeModelPage - 1) * pageSize)
                .take(pageSize)

        val manufacturers =
            filteredManufacturers
                .drop((safeManufacturerPage - 1) * pageSize)
                .take(pageSize)

        val viewModel =
            mapOf(
                "title" to "Manage Planes",
                "layout" to "admin",
                "activePage" to "managePlaneData",
                "inNav" to true,
                "isAdmin" to true,
                "models" to models,
                "manufacturers" to manufacturers,
                "allManufacturers" to allManufacturers,
                "modelSearch" to modelSearch,
                "modelPage" to safeModelPage,
                "totalModelPages" to totalModelPages,
                "manufacturerSearch" to manufacturerSearch,
                "manufacturerPage" to safeManufacturerPage,
                "totalManufacturerPages" to totalManufacturerPages,
            )

        val template = pebble.getTemplate("admin/managePlaneData.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, viewModel)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handleCreatePlaneModelPost() {
    timed("T1_create_plane_model", jsMode()) {
        if (!requireAdmin()) return@timed

        val params = receiveParameters()

        val name = params["name"]?.trim()
        val manufacturerId = params["manufacturerId"]?.toIntOrNull()
        val capacity = params["capacity"]?.toIntOrNull()
        val pilots = params["pilots"]?.toIntOrNull()
        val attendants = params["attendants"]?.toIntOrNull()

        if (
            name.isNullOrBlank() ||
            manufacturerId == null ||
            capacity == null ||
            pilots == null ||
            attendants == null
        ) {
            respondText("Please fill in all model fields", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val existing =
            PlaneModelData.queryDatabase(
                whereArgs =
                    WhereArgs(
                        "${PlaneModelColumns.NAME.name} = ? AND ${PlaneModelColumns.MANUFACTURER_ID.name} = ?",
                        listOf(name, manufacturerId),
                    ),
            )

        if (existing.isNotEmpty()) {
            respondText("That plane model already exists for this manufacturer", status = HttpStatusCode.BadRequest)
            return@timed
        }

        PlaneModelData(
            name = name,
            manufacturerId = manufacturerId,
            capacity = capacity,
            pilots = pilots,
            attendants = attendants,
        ).insertIntoDatabase()

        response.headers.append("HX-Redirect", "/managePlaneData")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleUpdatePlaneModelPost() {
    timed("T2_update_plane_model", jsMode()) {
        if (!requireAdmin()) return@timed

        val params = receiveParameters()

        val modelId = params["modelId"]?.toIntOrNull()
        val name = params["name"]?.trim()
        val manufacturerId = params["manufacturerId"]?.toIntOrNull()
        val capacity = params["capacity"]?.toIntOrNull()
        val pilots = params["pilots"]?.toIntOrNull()
        val attendants = params["attendants"]?.toIntOrNull()

        if (
            modelId == null ||
            name.isNullOrBlank() ||
            manufacturerId == null ||
            capacity == null ||
            pilots == null ||
            attendants == null
        ) {
            respondText("Invalid model details", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val duplicate =
            PlaneModelData.queryDatabase(
                whereArgs =
                    WhereArgs(
                        "${PlaneModelColumns.NAME.name} = ? AND ${PlaneModelColumns.MANUFACTURER_ID.name} = ? AND ${PlaneModelColumns.ID.name} != ?",
                        listOf(name, manufacturerId, modelId),
                    ),
            )

        if (duplicate.isNotEmpty()) {
            respondText("That plane model already exists for this manufacturer", status = HttpStatusCode.BadRequest)
            return@timed
        }

        PlaneModelData.updateTable(
            values =
                mapOf(
                    PlaneModelColumns.NAME to name,
                    PlaneModelColumns.MANUFACTURER_ID to manufacturerId,
                    PlaneModelColumns.CAPACITY to capacity,
                    PlaneModelColumns.PILOTS to pilots,
                    PlaneModelColumns.ATTENDANTS to attendants,
                ),
            whereArgs =
                WhereArgs(
                    "${PlaneModelColumns.ID.name} = ?",
                    listOf(modelId),
                ),
        )

        response.headers.append("HX-Redirect", "/managePlaneData")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleDeletePlaneModelPost() {
    timed("T3_delete_plane_model", jsMode()) {
        if (!requireAdmin()) return@timed

        val modelId = receiveParameters()["modelId"]?.toIntOrNull()

        if (modelId == null) {
            respondText("Invalid model", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val planesUsingModel =
            PlaneData.queryDatabase(
                whereArgs =
                    WhereArgs(
                        "${PlaneColumns.MODEL_ID.name} = ?",
                        listOf(modelId),
                    ),
            )

        if (planesUsingModel.isNotEmpty()) {
            respondText(
                "Cannot delete this model because one or more planes use it",
                status = HttpStatusCode.BadRequest,
            )
            return@timed
        }

        PlaneModelData.delete(modelId)

        response.headers.append("HX-Redirect", "/managePlaneData")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleCreateManufacturerPost() {
    timed("T4_create_manufacturer", jsMode()) {
        if (!requireAdmin()) return@timed

        val name = receiveParameters()["name"]

        if (name.isNullOrBlank()) {
            respondText("Please enter a manufacturer name", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val existing =
            ManufacturerData.queryDatabase(
                whereArgs =
                    WhereArgs(
                        "${ManufacturerColumns.NAME.name} = ?",
                        listOf(name),
                    ),
            )

        if (existing.isNotEmpty()) {
            respondText("That manufacturer already exists", status = HttpStatusCode.BadRequest)
            return@timed
        }

        ManufacturerData(name = name).insertIntoDatabase()

        response.headers.append("HX-Redirect", "/managePlaneData")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleUpdateManufacturerPost() {
    timed("T5_update_manufacturer", jsMode()) {
        if (!requireAdmin()) return@timed

        val params = receiveParameters()

        val manufacturerId = params["manufacturerId"]?.toIntOrNull()
        val name = params["name"]?.trim()

        if (manufacturerId == null || name.isNullOrBlank()) {
            respondText("Invalid manufacturer details", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val duplicate =
            ManufacturerData.queryDatabase(
                whereArgs =
                    WhereArgs(
                        "${ManufacturerColumns.NAME.name} = ? AND ${ManufacturerColumns.ID.name} != ?",
                        listOf(name, manufacturerId),
                    ),
            )

        if (duplicate.isNotEmpty()) {
            respondText("That manufacturer already exists", status = HttpStatusCode.BadRequest)
            return@timed
        }

        ManufacturerData.updateTable(
            values =
                mapOf(
                    ManufacturerColumns.NAME to name,
                ),
            whereArgs =
                WhereArgs(
                    "${ManufacturerColumns.ID.name} = ?",
                    listOf(manufacturerId),
                ),
        )

        response.headers.append("HX-Redirect", "/managePlaneData")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleDeleteManufacturerPost() {
    timed("T6_delete_manufacturer", jsMode()) {
        if (!requireAdmin()) return@timed

        val manufacturerId = receiveParameters()["manufacturerId"]?.toIntOrNull()

        if (manufacturerId == null) {
            respondText("Invalid manufacturer", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val modelsUsingManufacturer =
            PlaneModelData.queryDatabase(
                whereArgs =
                    WhereArgs(
                        "${PlaneModelColumns.MANUFACTURER_ID.name} = ?",
                        listOf(manufacturerId),
                    ),
            )

        if (modelsUsingManufacturer.isNotEmpty()) {
            respondText(
                "Cannot delete this manufacturer because one or more models use it",
                status = HttpStatusCode.BadRequest,
            )
            return@timed
        }

        ManufacturerData.delete(manufacturerId)

        response.headers.append("HX-Redirect", "/managePlaneData")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleEditPlaneModelLoad() {
    timed("T7_edit_plane_model_load", jsMode()) {
        if (!requireAdmin()) return@timed

        val modelId = parameters["id"]?.toIntOrNull()
        if (modelId == null) {
            respondRedirect("/managePlaneData")
            return@timed
        }

        val model =
            PlaneModelData
                .queryDatabase(
                    whereArgs = WhereArgs("${PlaneModelColumns.ID.name} = ?", listOf(modelId)),
                ).firstOrNull()
                ?.dataClass

        if (model == null) {
            respondRedirect("/managePlaneData")
            return@timed
        }

        val manufacturers = ManufacturerData.queryDatabase().map { it.dataClass }

        val viewModel =
            mapOf(
                "title" to "Edit Plane Model",
                "layout" to "admin",
                "activePage" to "managePlaneData",
                "inNav" to true,
                "isAdmin" to true,
                "model" to model,
                "manufacturers" to manufacturers,
            )

        val template = getEngine().getTemplate("admin/editPlaneModel.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, viewModel)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handleEditManufacturerLoad() {
    timed("T8_edit_manufacturer_load", jsMode()) {
        if (!requireAdmin()) return@timed

        val manufacturerId = parameters["id"]?.toIntOrNull()
        if (manufacturerId == null) {
            respondRedirect("/managePlaneData")
            return@timed
        }

        val manufacturer =
            ManufacturerData
                .queryDatabase(
                    whereArgs = WhereArgs("${ManufacturerColumns.ID.name} = ?", listOf(manufacturerId)),
                ).firstOrNull()
                ?.dataClass

        if (manufacturer == null) {
            respondRedirect("/managePlaneData")
            return@timed
        }

        val viewModel =
            mapOf(
                "title" to "Edit Manufacturer",
                "layout" to "admin",
                "activePage" to "managePlaneData",
                "inNav" to true,
                "isAdmin" to true,
                "manufacturer" to manufacturer,
            )

        val template = getEngine().getTemplate("admin/editManufacturer.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, viewModel)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}
