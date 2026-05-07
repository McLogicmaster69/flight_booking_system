package results

data class CreateFlightResults(
    val flightId: Int? = null,
    val returnMessage: String? = null,
    val staffResults: StaffAssignmentResults? = null,
)
