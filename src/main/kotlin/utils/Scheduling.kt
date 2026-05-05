package utils

import data.FlightSearchData
import data.ScheduleData
import java.time.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun cleanup () {
    FlightSearchData.deleteOld()
}

fun scheduleFlights() {
    ScheduleData.updateAllFlights()
}

fun initialTasks() {
    cleanup()
    scheduleFlights()
}

fun scheduleDailyTasks() {
    val scheduler = Executors.newSingleThreadScheduledExecutor()

    val now = ZonedDateTime.now()
    val updateTime = now
        .withHour(12)
        .withMinute(0)
        .withSecond(0)
        .withNano(0)
        .let {
            if (it.isBefore(now)) it.plusDays(1) else it
        }

    val initialDelay = Duration.between(now, updateTime).seconds
    val period = TimeUnit.DAYS.toSeconds(1)

    scheduler.scheduleAtFixedRate(
        {
            println("Cleaning database")
            try {
                cleanup()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            println("Cleaning complete")

            println("Scheduling flights")
            try {
                scheduleFlights()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            println("Scheduling complete")
        },
        initialDelay,
        period,
        TimeUnit.SECONDS
    )
}