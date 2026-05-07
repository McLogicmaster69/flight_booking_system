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

fun Route.manageStaffRoutes() {
    get("/manageStaff") { call.handleManageStaffLoad() }
    get("/manageStaff/edit/{id}") { call.handleEditStaffLoad() }
    post("/manageStaff/create") { call.handleCreateStaffPost() }
    post("/manageStaff/update") { call.handleUpdateStaffPost() }
}

data class StaffAccountView(
    val id: Int,
    val firstName: String?,
    val lastName: String?,
    val positionId: Int,
    val positionName: String?,
    val email: String?,
)

private suspend fun ApplicationCall.handleManageStaffLoad() {
    timed("T0_manage_staff", jsMode()) {
        if (!requireAdmin()) return@timed

        val pebble = getEngine()

        val search = request.queryParameters["search"]?.trim().orEmpty()
        val page = request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val pageSize = 5

        val allStaff =
            StaffData.queryDatabase().map { result ->
                val staffMember = result.dataClass

                val position =
                    StaffPositionData
                        .queryDatabase(
                            whereArgs =
                                WhereArgs(
                                    "${StaffPositionColumns.ID.name} = ?",
                                    listOf(staffMember.positionId),
                                ),
                        ).firstOrNull()
                        ?.dataClass

                val login =
                    LoginData
                        .queryDatabase(
                            whereArgs =
                                WhereArgs(
                                    "${LoginColumns.ID.name} = ?",
                                    listOf(staffMember.loginId),
                                ),
                        ).firstOrNull()
                        ?.dataClass

                StaffAccountView(
                    id = staffMember.id,
                    firstName = staffMember.firstName,
                    lastName = staffMember.lastName,
                    positionId = staffMember.positionId,
                    positionName = position?.name,
                    email = login?.email,
                )
            }

        val filteredStaff =
            if (search.isBlank()) {
                allStaff
            } else {
                allStaff.filter { member ->
                    member.firstName?.contains(search, ignoreCase = true) == true ||
                        member.lastName?.contains(search, ignoreCase = true) == true
                }
            }

        val totalPages = ((filteredStaff.size + pageSize - 1) / pageSize).coerceAtLeast(1)
        val safePage = page.coerceAtMost(totalPages)

        val staff =
            filteredStaff
                .drop((safePage - 1) * pageSize)
                .take(pageSize)

        val positions = StaffPositionData.queryDatabase().map { it.dataClass }
        val countries = CountryData.queryDatabase().map { it.dataClass }
        val destinations = DestinationData.queryDatabase().map { it.dataClass }

        val model =
            mapOf(
                "title" to "Manage Staff",
                "layout" to "admin",
                "activePage" to "manageStaff",
                "inNav" to true,
                "isAdmin" to true,
                "staff" to staff,
                "positions" to positions,
                "countries" to countries,
                "destinations" to destinations,
                "search" to search,
                "page" to safePage,
                "totalPages" to totalPages,
            )

        val template = pebble.getTemplate("admin/manageStaff.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handleCreateStaffPost() {
    timed("T1_create_staff", jsMode()) {
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

        val loginId =
            LoginData(
                email = email,
                passwordHash = passwordHash,
            ).insertIntoDatabase()

        StaffData(
            firstName = firstName,
            lastName = lastName,
            positionId = positionId,
            loginId = loginId,
            homeId = homeId,
            currentLocation = currentLocation,
        ).insertIntoDatabase()

        response.headers.append("HX-Redirect", "/manageStaff")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleUpdateStaffPost() {
    timed("T2_update_staff", jsMode()) {
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
            values =
                mapOf(
                    StaffColumns.FIRSTNAME to firstName,
                    StaffColumns.LASTNAME to lastName,
                    StaffColumns.POSITION_ID to positionId,
                ),
            whereArgs =
                WhereArgs(
                    "${StaffColumns.ID.name} = ?",
                    listOf(staffId),
                ),
        )

        response.headers.append("HX-Redirect", "/manageStaff")
        respond(HttpStatusCode.OK)
    }
}

private suspend fun ApplicationCall.handleEditStaffLoad() {
    timed("T3_edit_staff_load", jsMode()) {
        if (!requireAdmin()) return@timed

        val staffId = parameters["id"]?.toIntOrNull()
        if (staffId == null) {
            respondRedirect("/manageStaff")
            return@timed
        }

        val staff =
            StaffData
                .queryDatabase(
                    whereArgs =
                        WhereArgs(
                            "${StaffColumns.ID.name} = ?",
                            listOf(staffId),
                        ),
                ).firstOrNull()
                ?.dataClass

        if (staff == null) {
            respondRedirect("/manageStaff")
            return@timed
        }

        val login =
            LoginData
                .queryDatabase(
                    whereArgs =
                        WhereArgs(
                            "${LoginColumns.ID.name} = ?",
                            listOf(staff.loginId),
                        ),
                ).firstOrNull()
                ?.dataClass

        val positions = StaffPositionData.queryDatabase().map { it.dataClass }

        val model =
            mapOf(
                "title" to "Edit Staff",
                "layout" to "admin",
                "activePage" to "manageStaff",
                "inNav" to true,
                "isAdmin" to true,
                "staff" to staff,
                "email" to login?.email,
                "positions" to positions,
            )

        val template = getEngine().getTemplate("admin/editStaff.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}
