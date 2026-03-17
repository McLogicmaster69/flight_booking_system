package routes

import data.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.pebbletemplates.pebble.PebbleEngine
import java.io.StringWriter
import utils.jsMode
import utils.logValidationError
import utils.timed
import auth.*
import org.mindrot.jbcrypt.BCrypt
import utils.EmailService
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.LocalDate
import java.time.LocalTime

const val MAX_RESULTS = 10
const val MAX_LAYOVERS = 10

fun representAsTime(minutes : Long) : String {
    val hours = minutes / 60L
    return "${hours.toString().padStart(2, '0')}h ${(minutes - hours * 60L).toString().padStart(2, '0')}m"
}

fun getResultHTML(result : JourneyFlightTimePath, index : Int) : String {
    val searchData : FlightSearchData = FlightSearchData.queryOrAddFlightPath(result)
    return """
    <div class="flight-result-container">
        <a href="/book/${searchData.token}" class="flight-result-button">
            <h2>${result.locationNames.first()} to ${result.locationNames.last()}</h2>
            <div class="flight-result-info">
                <p>Duration: ${representAsTime(result.totalMinutes)}</p>
                <p>${if (result.locationNames.size <= 2) "Direct" else "Layovers: ${result.locationNames.size - 2}"}</p>
            </div>
        </a>
    </div>
    <br>
    """
}

fun getResultsHTML(results : List<JourneyFlightTimePath>) : String {
    if (results.isEmpty())
        return """
        <div id="flight-results">
            <h4>No Flights Found</h4>
        </div>
        """

    var output = """<div id="flight-results">"""

    for (i in 0..((if (results.size > MAX_RESULTS) MAX_RESULTS else results.size) - 1)) {
        output += getResultHTML(results[i], i)
        output += "\n"
    }

    return output + "</div>"
}

fun Route.bookRoutes() {
    get("/book") { call.handleBookLoad() }
    post("/book-search") { call.handleSearchResults() }
    get("/book/{token}") { call.handleGetResult() }
}

private suspend fun ApplicationCall.handleBookLoad() {
    timed("T0_book", jsMode()) {       
        val model = mapOf(
            "title" to "Book",
            "isNav" to true,
            "destinations" to DestinationData.getDestinationNames()
        )

        val pebble = getEngine()
        val template = pebble.getTemplate("book/index.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handleSearchResults() {
    timed("T1_book_search", jsMode()) {
        val parameters = receiveParameters()
        val start = parameters["from"].orEmpty().trim()
        val end = parameters["to"].orEmpty().trim()
        val time = parameters["depart"].orEmpty().trim()
        val maxLayovers = parameters["maxLayovers"].orEmpty().trim()
        val timeElements = time.split("/")
        val layovers = when(maxLayovers) {
            "Direct Only" -> 0
            "1" -> 1
            "2" -> 2
            "Any" -> MAX_LAYOVERS
            else -> MAX_LAYOVERS
        }

        val results : List<JourneyFlightTimePath> = FlightData.getJourneyFlight(
            start,
            end,
            LocalDate.of(timeElements[2].toInt(), timeElements[1].toInt(), timeElements[0].toInt()),
            layovers
        )

        respondText(getResultsHTML(results), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handleGetResult() {
    timed("T2_book_result", jsMode()) {
        val writer = StringWriter()
        val pebble = getEngine()
        val token = parameters["token"]
        
        if (token == null) {
            val template = pebble.getTemplate("book/pageNotFound.peb")

            val errorModel = mapOf(
                "title" to "Error",
                "isNav" to true
            )

            fullEvaluate(template, writer, errorModel)
            return@timed respondText(writer.toString(), ContentType.Text.Html)
        }

        val search : FlightSearchInfo? = FlightSearchData.queryByToken(token)
        if (search == null) {
            val template = pebble.getTemplate("book/pageNotFound.peb")

            val errorModel = mapOf(
                "title" to "Error",
                "isNav" to true
            )

            fullEvaluate(template, writer, errorModel)
            return@timed respondText(writer.toString(), ContentType.Text.Html)
        }

        val model = mapOf(
            "title" to "Result",
            "isNav" to true,
            "start" to search.search.startDestination,
            "end" to search.search.endDestination
        )

        val template = pebble.getTemplate("book/result.peb")
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}
