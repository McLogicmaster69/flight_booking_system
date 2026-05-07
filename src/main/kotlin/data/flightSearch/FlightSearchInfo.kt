package data

import java.time.LocalDateTime

data class FlightSearchInfo(
    val search: FlightSearchData,
    val flights: List<FlightSearchFlightData>,
) {
    fun getStartDestinationName(): String = DestinationData.getDestinationName(search.startDestination)

    fun getEndDestinationName(): String = DestinationData.getDestinationName(search.endDestination)

    fun getDate(): String = search.date.toString()

    fun getLayovers(): Int = flights.size - 1

    fun getFlightInfo(): List<FlightInfo> {
        return flights.map { flight ->
            val flightQuery = FlightData.queryDatabase(flight.flightId)
            if (flightQuery.isEmpty()) {
                return@map FlightInfo.EMPTY
            }

            val flightData = flightQuery.first()
            val routeQuery = RouteData.queryDatabase(flightData.dataClass.routeId)
            if (routeQuery.isEmpty()) {
                return@map FlightInfo.EMPTY
            }

            val routeData = routeQuery.first()
            FlightInfo(
                DestinationData.getDestinationName(routeData.dataClass.startDestination),
                DestinationData.getDestinationName(routeData.dataClass.endDestination),
                LocalDateTime.of(flightData.dataClass.date, flightData.dataClass.time).toString(),
            )
        }
    }
}
