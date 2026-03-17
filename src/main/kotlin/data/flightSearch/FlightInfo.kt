package data

data class FlightInfo (
    val startDestination : String,
    val endDestination : String,
    val dateTime : String
) {
    companion object {
        val EMPTY : FlightInfo
            get() = FlightInfo("", "", "")
    }
}