package data

import java.time.LocalDateTime

data class JourneyFlightTimePath(
    val destinationIds: List<Int>,
    val locationNames: List<String>,
    val flightIds: List<Int>,
    val localDateTimes: List<LocalDateTime>,
    val timezones: List<String>,
    val totalMinutes: Long,
)
