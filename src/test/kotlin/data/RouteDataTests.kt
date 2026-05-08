package tests.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.*
import data.DatabaseManager
import data.RouteData
import data.DestinationData
import data.TimezoneData
import data.CountryData
import data.DestinationArgs
import java.time.LocalTime

class RouteDataTests : BaseDatabaseTest() {
    fun insertTestData(): List<Int> {
        val timezoneId =
            TimezoneData(
                name = "TEST TIMEZONE",
                timeOffset = 10f,
            ).insertIntoDatabase()

        val countryId =
            CountryData(
                name = "TEST COUNTRY",
            ).insertIntoDatabase()

        val destination1Id =
            DestinationData(
                countryId = countryId,
                cityName = "TEST CITY 1",
                timezoneId = timezoneId,
            ).insertIntoDatabase()

        val destination2Id =
            DestinationData(
                countryId = countryId,
                cityName = "TEST CITY 2",
                timezoneId = timezoneId,
            ).insertIntoDatabase()

        return listOf(timezoneId, countryId, destination1Id, destination2Id)
    }

    @Test
    fun `check database has been initialised`() {
        assertTrue(DatabaseManager.dbInitialised)
    }

    @Test
    fun `check insert`() {
        val ids = insertTestData()

        val routeId =
            RouteData(
                startDestination = ids[2],
                endDestination = ids[3],
                duration = LocalTime.parse("05:00"),
            ).insertIntoDatabase()

        assertNotEquals(routeId, -1)
    }

    @Test
    fun `check query`() {
        val ids = insertTestData()

        val routeId =
            RouteData(
                startDestination = ids[2],
                endDestination = ids[3],
                duration = LocalTime.parse("05:00"),
            ).insertIntoDatabase()

        val results =
            RouteData.queryDatabase()

        assertNotEquals(results.size, 0)
    }

    @Test
    fun `check update`() {
        val ids = insertTestData()

        val routeId =
            RouteData(
                startDestination = ids[2],
                endDestination = ids[3],
                duration = LocalTime.parse("05:00"),
            ).insertIntoDatabase()

        val results =
            RouteData(
                id = routeId,
                startDestination = ids[2],
                endDestination = ids[3],
                duration = LocalTime.parse("04:00"),
            ).update()

        assertNotEquals(results, 0)
    }

    @Test
    fun `check delete`() {
        val ids = insertTestData()

        val routeId =
            RouteData(
                startDestination = ids[2],
                endDestination = ids[3],
                duration = LocalTime.parse("05:00"),
            ).insertIntoDatabase()

        val results =
            RouteData(
                id = routeId,
                startDestination = ids[2],
                endDestination = ids[3],
                duration = LocalTime.parse("05:00"),
            ).delete()

        assertNotEquals(results, 0)
    }

    @Test
    fun `check get path by layovers`() {
        val ids = insertTestData()

        val routeId =
            RouteData(
                startDestination = ids[2],
                endDestination = ids[3],
                duration = LocalTime.parse("05:00"),
            ).insertIntoDatabase()

        val results =
            RouteData.getPathByLayovers(
                DestinationArgs(
                    ids[2],
                    ids[3],
                ),
            )

        assertEquals(results.size, 1)
    }

    @Test
    fun `check get journey routes`() {
        val ids = insertTestData()

        val routeId =
            RouteData(
                startDestination = ids[2],
                endDestination = ids[3],
                duration = LocalTime.parse("05:00"),
            ).insertIntoDatabase()

        val results =
            RouteData.getJourneyRoutes(
                DestinationArgs(
                    ids[2],
                    ids[3],
                ),
            )

        assertEquals(results.size, 1)
    }

    @Test
    fun `check get duration`() {
        val ids = insertTestData()

        val routeId =
            RouteData(
                startDestination = ids[2],
                endDestination = ids[3],
                duration = LocalTime.parse("05:00"),
            ).insertIntoDatabase()

        val duration =
            RouteData.getDuration(routeId)

        assertEquals(duration, LocalTime.parse("05:00"))
    }

    @Test
    fun `check get route id`() {
        val ids = insertTestData()

        val routeId =
            RouteData(
                startDestination = ids[2],
                endDestination = ids[3],
                duration = LocalTime.parse("05:00"),
            ).insertIntoDatabase()

        val results =
            RouteData.getRouteId(
                DestinationArgs(
                    ids[2],
                    ids[3],
                ),
            )

        assertEquals(routeId, results)
    }
}
