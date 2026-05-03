package results

data class StaffAssignmentResults (
    val flightId : Int,
    val pilotIds : List<Int>,
    val attendantIds : List<Int>,
    val returnMessage : String? = null
)