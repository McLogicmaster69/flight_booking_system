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
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDate
import java.time.LocalTime

fun Route.adminRoutes() {
    get("/admin") { call.handleAdminLoad() }

    get("/manageStaff") { call.handleManageStaffLoad() }
    post("/manageStaff/create") { call.handleCreateStaffPost() }
    post("/manageStaff/update") { call.handleUpdateStaffPost() }

    get("/managePlaneData") { call.handleManagePlaneDataLoad() }
    post("/managePlaneData/model/create") { call.handleCreatePlaneModelPost() }
    post("/managePlaneData/model/update") { call.handleUpdatePlaneModelPost() }
    post("/managePlaneData/model/delete") { call.handleDeletePlaneModelPost() }
    post("/managePlaneData/manufacturer/create") { call.handleCreateManufacturerPost() }
    post("/managePlaneData/manufacturer/update") { call.handleUpdateManufacturerPost() }
    post("/managePlaneData/manufacturer/delete") { call.handleDeleteManufacturerPost() }

    get("/managePlanes") { call.handleManagePlanesLoad() }
    post("/managePlanes/create") { call.handleCreatePlanePost() }
    post("/managePlanes/update") { call.handleUpdatePlanePost() }
    post("/managePlanes/delete") { call.handleDeletePlanePost() }

}

data class StaffAccountView(
    val id: Int,
    val firstName: String?,
    val lastName: String?,
    val positionId: Int,
    val positionName: String?,
    val email: String?
)

data class PlaneModelView(
    val id: Int,
    val name: String,
    val manufacturerId: Int,
    val manufacturerName: String?,
    val capacity: Int,
    val pilots: Int,
    val attendants: Int
)

data class PlaneView(
    val id: Int,
    val registrationCode: String,
    val modelId: Int,
    val modelName: String?,
    val currentLocation: Int,
    val currentLocationName: String?,
    val currentLocationDate: LocalDate,
    val currentLocationTime: LocalTime
)

private suspend fun ApplicationCall.handleAdminLoad() {
    timed("T0_admin", jsMode()) {
        if (!requireAdmin()) return@timed

        val pebble = getEngine()

        val model = mapOf(
            "title" to "Admin",
            "layout" to "admin",
            "activePage" to "admin",
            "inNav" to true,
            "isAdmin" to true
        )

        val template = pebble.getTemplate("admin/index.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handleManageStaffLoad() {
    timed("T1_manage_staff", jsMode()) {
        if (!requireAdmin()) return@timed

        val pebble = getEngine()

        val staff = StaffData.queryDatabase().map { result ->
        val staffMember = result.dataClass

        val position = StaffPositionData.queryDatabase(
            whereArgs = WhereArgs(
                "${StaffPositionColumns.ID.name} = ?",
                listOf(staffMember.positionId)
            )
        ).firstOrNull()?.dataClass

        val login = LoginData.queryDatabase(
            whereArgs = WhereArgs(
                "${LoginColumns.ID.name} = ?",
                listOf(staffMember.loginId)
            )
        ).firstOrNull()?.dataClass

        StaffAccountView(
            id = staffMember.id,
            firstName = staffMember.firstName,
            lastName = staffMember.lastName,
            positionId = staffMember.positionId,
            positionName = position?.name,
            email = login?.email
        )
    }

    val positions = StaffPositionData.queryDatabase().map { it.dataClass }
    val countries = CountryData.queryDatabase().map { it.dataClass }
    val destinations = DestinationData.queryDatabase().map { it.dataClass }

    val model = mapOf(
        "title" to "Manage Staff",
        "layout" to "admin",
        "activePage" to "manageStaff",
        "inNav" to true,
        "isAdmin" to true,
        "staff" to staff,
        "positions" to positions,
        "countries" to countries,
        "destinations" to destinations
    )

        val template = pebble.getTemplate("admin/manageStaff.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handleCreateStaffPost() {
    timed("T2_create_staff", jsMode()) {
        if (!requireAdmin()) return@timed

        val params = receiveParameters()

        val firstName = params["firstName"]?.trim()
        val lastName = params["lastName"]?.trim()
        val positionId = params["positionId"]?.toIntOrNull()
        val email = params["email"]?.trim()
        val password = params["password"]?.trim()
        val homeId = params["homeId"]?.toIntOrNull()
        val currentLocation = params["currentLocation"]?.toIntOrNull()

        if (
            firstName.isNullOrBlank() ||
            lastName.isNullOrBlank() ||
            positionId == null ||
            homeId == null ||
            currentLocation == null ||
            email.isNullOrBlank() ||
            password.isNullOrBlank()
        ) {
            respondText("Please fill in all fields", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val existingUser = UserData.queryByLogIn(email)
        val existingStaff = StaffData.queryByLogIn(email)

        if (existingUser.isNotEmpty() || existingStaff.isNotEmpty()) {
            respondText("An account already exists with that email", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())

        val loginId = LoginData(
            email = email,
            passwordHash = passwordHash
        ).insertIntoDatabase()

        StaffData(
            firstName = firstName,
            lastName = lastName,
            positionId = positionId,
            loginId = loginId,
            homeId = homeId,
            currentLocation = currentLocation
        ).insertIntoDatabase()

        response.headers.append("HX-Redirect", "/manageStaff")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleUpdateStaffPost() {
    timed("T3_update_staff", jsMode()) {
        if (!requireAdmin()) return@timed

        val params = receiveParameters()

        val staffId = params["staffId"]?.toIntOrNull()
        val firstName = params["firstName"]?.trim()
        val lastName = params["lastName"]?.trim()
        val positionId = params["positionId"]?.toIntOrNull()

        if (
            staffId == null ||
            firstName.isNullOrBlank() ||
            lastName.isNullOrBlank() ||
            positionId == null
        ) {
            respondText("Invalid staff details", status = HttpStatusCode.BadRequest)
            return@timed
        }

        StaffData.updateTable(
            values = mapOf(
                StaffColumns.FIRSTNAME to firstName,
                StaffColumns.LASTNAME to lastName,
                StaffColumns.POSITION_ID to positionId
            ),
            whereArgs = WhereArgs(
                "${StaffColumns.ID.name} = ?",
                listOf(staffId)
            )
        )

        response.headers.append("HX-Redirect", "/manageStaff")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleManagePlaneDataLoad() {
    timed("T4_manage_plane_data", jsMode()) {
        if (!requireAdmin()) return@timed

        val pebble = getEngine()

        val manufacturers = ManufacturerData.queryDatabase().map { it.dataClass }

        val models = PlaneModelData.queryDatabase().map { result ->
            val model = result.dataClass

            val manufacturer = ManufacturerData.queryDatabase(
                whereArgs = WhereArgs(
                    "${ManufacturerColumns.ID.name} = ?",
                    listOf(model.manufacturerId)
                )
            ).firstOrNull()?.dataClass

            PlaneModelView(
                id = model.id,
                name = model.name,
                manufacturerId = model.manufacturerId,
                manufacturerName = manufacturer?.name,
                capacity = model.capacity,
                pilots = model.pilots,
                attendants = model.attendants
            )
        }

        val viewModel = mapOf(
            "title" to "Manage Planes",
            "layout" to "admin",
            "activePage" to "managePlaneData",
            "inNav" to true,
            "isAdmin" to true,
            "models" to models,
            "manufacturers" to manufacturers
        )

        val template = pebble.getTemplate("admin/managePlaneData.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, viewModel)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handleCreatePlaneModelPost() {
    timed("T5_create_plane_model", jsMode()) {
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

        val existing = PlaneModelData.queryDatabase(
            whereArgs = WhereArgs(
                "${PlaneModelColumns.NAME.name} = ? AND ${PlaneModelColumns.MANUFACTURER_ID.name} = ?",
                listOf(name, manufacturerId)
            )
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
            attendants = attendants
        ).insertIntoDatabase()

        response.headers.append("HX-Redirect", "/managePlaneData")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleUpdatePlaneModelPost() {
    timed("T6_update_plane_model", jsMode()) {
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

        val duplicate = PlaneModelData.queryDatabase(
            whereArgs = WhereArgs(
                "${PlaneModelColumns.NAME.name} = ? AND ${PlaneModelColumns.MANUFACTURER_ID.name} = ? AND ${PlaneModelColumns.ID.name} != ?",
                listOf(name, manufacturerId, modelId)
            )
        )

        if (duplicate.isNotEmpty()) {
            respondText("That plane model already exists for this manufacturer", status = HttpStatusCode.BadRequest)
            return@timed
        }

        PlaneModelData.updateTable(
            values = mapOf(
                PlaneModelColumns.NAME to name,
                PlaneModelColumns.MANUFACTURER_ID to manufacturerId,
                PlaneModelColumns.CAPACITY to capacity,
                PlaneModelColumns.PILOTS to pilots,
                PlaneModelColumns.ATTENDANTS to attendants
            ),
            whereArgs = WhereArgs(
                "${PlaneModelColumns.ID.name} = ?",
                listOf(modelId)
            )
        )

        response.headers.append("HX-Redirect", "/managePlaneData")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleDeletePlaneModelPost() {
    timed("T7_delete_plane_model", jsMode()) {
        if (!requireAdmin()) return@timed

        val modelId = receiveParameters()["modelId"]?.toIntOrNull()

        if (modelId == null) {
            respondText("Invalid model", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val planesUsingModel = PlaneData.queryDatabase(
            whereArgs = WhereArgs(
                "${PlaneColumns.MODEL_ID.name} = ?",
                listOf(modelId)
            )
        )

        if (planesUsingModel.isNotEmpty()) {
            respondText("Cannot delete this model because one or more planes use it", status = HttpStatusCode.BadRequest)
            return@timed
        }

        PlaneModelData.delete(modelId)

        response.headers.append("HX-Redirect", "/managePlaneData")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleCreateManufacturerPost() {
    timed("T8_create_manufacturer", jsMode()) {
        if (!requireAdmin()) return@timed

        val name = receiveParameters()["name"]

        if (name.isNullOrBlank()) {
            respondText("Please enter a manufacturer name", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val existing = ManufacturerData.queryDatabase(
            whereArgs = WhereArgs(
                "${ManufacturerColumns.NAME.name} = ?",
                listOf(name)
            )
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
    timed("T9_update_manufacturer", jsMode()) {
        if (!requireAdmin()) return@timed

        val params = receiveParameters()

        val manufacturerId = params["manufacturerId"]?.toIntOrNull()
        val name = params["name"]?.trim()

        if (manufacturerId == null || name.isNullOrBlank()) {
            respondText("Invalid manufacturer details", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val duplicate = ManufacturerData.queryDatabase(
            whereArgs = WhereArgs(
                "${ManufacturerColumns.NAME.name} = ? AND ${ManufacturerColumns.ID.name} != ?",
                listOf(name, manufacturerId)
            )
        )

        if (duplicate.isNotEmpty()) {
            respondText("That manufacturer already exists", status = HttpStatusCode.BadRequest)
            return@timed
        }

        ManufacturerData.updateTable(
            values = mapOf(
                ManufacturerColumns.NAME to name
            ),
            whereArgs = WhereArgs(
                "${ManufacturerColumns.ID.name} = ?",
                listOf(manufacturerId)
            )
        )

        response.headers.append("HX-Redirect", "/managePlaneData")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleDeleteManufacturerPost() {
    timed("T10_delete_manufacturer", jsMode()) {
        if (!requireAdmin()) return@timed

        val manufacturerId = receiveParameters()["manufacturerId"]?.toIntOrNull()

        if (manufacturerId == null) {
            respondText("Invalid manufacturer", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val modelsUsingManufacturer = PlaneModelData.queryDatabase(
            whereArgs = WhereArgs(
                "${PlaneModelColumns.MANUFACTURER_ID.name} = ?",
                listOf(manufacturerId)
            )
        )

        if (modelsUsingManufacturer.isNotEmpty()) {
            respondText("Cannot delete this manufacturer because one or more models use it", status = HttpStatusCode.BadRequest)
            return@timed
        }

        ManufacturerData.delete(manufacturerId)

        response.headers.append("HX-Redirect", "/managePlaneData")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleManagePlanesLoad() {
    timed("T11_manage_planes", jsMode()) {
        if (!requireAdmin()) return@timed

        val pebble = getEngine()

        val models = PlaneModelData.queryDatabase().map { it.dataClass }
        val destinations = DestinationData.queryDatabase().map { it.dataClass }

        val planes = PlaneData.queryDatabase().map { result ->
            val plane = result.dataClass

            val model = PlaneModelData.queryDatabase(
                whereArgs = WhereArgs(
                    "${PlaneModelColumns.ID.name} = ?",
                    listOf(plane.modelId)
                )
            ).firstOrNull()?.dataClass

            val destination = DestinationData.queryDatabase(
                whereArgs = WhereArgs(
                    "${DestinationColumns.ID.name} = ?",
                    listOf(plane.currentLocation)
                )
            ).firstOrNull()?.dataClass

            PlaneView(
                id = plane.id,
                registrationCode = plane.registrationCode,
                modelId = plane.modelId,
                modelName = model?.name,
                currentLocation = plane.currentLocation,
                currentLocationName = destination?.cityName,
                currentLocationDate = plane.currentLocationDate,
                currentLocationTime = plane.currentLocationTime
            )
        }

        val viewModel = mapOf(
            "title" to "Manage Planes",
            "layout" to "admin",
            "activePage" to "managePlanes",
            "inNav" to true,
            "isAdmin" to true,
            "planes" to planes,
            "models" to models,
            "destinations" to destinations
        )

        val template = pebble.getTemplate("admin/managePlanes.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, viewModel)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handleCreatePlanePost() {
    timed("T12_create_plane", jsMode()) {
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

        val existing = PlaneData.queryDatabase(
            whereArgs = WhereArgs(
                "${PlaneColumns.REGISTRATION_CODE.name} = ?",
                listOf(registrationCode)
            )
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
            currentLocationTime = currentLocationTime
        ).insertIntoDatabase()

        response.headers.append("HX-Redirect", "/managePlanes")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleUpdatePlanePost() {
    timed("T13_update_plane", jsMode()) {
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

        val duplicate = PlaneData.queryDatabase(
            whereArgs = WhereArgs(
                "${PlaneColumns.REGISTRATION_CODE.name} = ? AND ${PlaneColumns.ID.name} != ?",
                listOf(registrationCode, planeId)
            )
        )

        if (duplicate.isNotEmpty()) {
            respondText("That plane already exists", status = HttpStatusCode.BadRequest)
            return@timed
        }

        PlaneData.updateTable(
            values = mapOf(
                PlaneColumns.REGISTRATION_CODE to registrationCode,
                PlaneColumns.MODEL_ID to modelId,
                PlaneColumns.CURRENT_LOCATION to currentLocation,
                PlaneColumns.CURRENT_LOCATION_DATE to currentLocationDate.toString(),
                PlaneColumns.CURRENT_LOCATION_TIME to currentLocationTime.toString()
            ),
            whereArgs = WhereArgs(
                "${PlaneColumns.ID.name} = ?",
                listOf(planeId)
            )
        )

        response.headers.append("HX-Redirect", "/managePlanes")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleDeletePlanePost() {
    timed("T14_delete_plane", jsMode()) {
        if (!requireAdmin()) return@timed

        val planeId = receiveParameters()["planeId"]?.toIntOrNull()

        if (planeId == null) {
            respondText("Invalid plane", status = HttpStatusCode.BadRequest)
            return@timed
        }

        val flightsUsingPlane = FlightData.queryDatabase(
            whereArgs = WhereArgs(
                "${FlightColumns.PLANE_ID.name} = ?",
                listOf(planeId)
            )
        )

        if (flightsUsingPlane.isNotEmpty()) {
            respondText("Cannot delete this plane because one or more flights use it", status = HttpStatusCode.BadRequest)
            return@timed
        }

        PlaneData.delete(planeId)

        response.headers.append("HX-Redirect", "/managePlanes")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.requireAdmin(): Boolean {
    val loggedState: LoggedInState = loggedIn()

    if (!loggedState.logged_in || loggedState.session == null) {
        respondRedirect("/login")
        return false
    }

    val userQuery = UserData.queryByToken(loggedState.session.token)

    if (userQuery.isEmpty()) {
        respondRedirect("/login")
        return false
    }

    val user = userQuery.first().dataClass

    val adminQuery = AdminData.queryDatabase(
        whereArgs = WhereArgs(
            "${AdminColumns.LOGIN_ID.name} = ?",
            listOf(user.loginId)
        )
    )

    if (adminQuery.isEmpty()) {
        respondText("403 Forbidden", ContentType.Text.Plain, HttpStatusCode.Forbidden)
        return false
    }

    return true
}