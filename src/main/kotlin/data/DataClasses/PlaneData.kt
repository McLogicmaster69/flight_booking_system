package data

import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime

object PlaneColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val REGISTRATION_CODE = Column<String>("registration_code", "STRING NOT NULL UNIQUE")
    val MODEL_ID = Column<Int>("model_id", "INTEGER NOT NULL REFERENCES ${PlaneModelData.EMPTY.tableName}(id)")
    val CURRENT_LOCATION = Column<Int>("current_location", "INTEGER NOT NULL REFERENCES ${DestinationData.EMPTY.tableName}(id)")
    val CURRENT_LOCATION_DATE = Column<String>("current_location_date", "STRING NOT NULL")
    val CURRENT_LOCATION_TIME = Column<String>("current_location_time", "STRING NOT NULL")

    val ALL = listOf(ID, REGISTRATION_CODE, MODEL_ID, CURRENT_LOCATION, CURRENT_LOCATION_DATE, CURRENT_LOCATION_TIME)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class PlaneData(

    override val id: Int = 0,
    var modelId : Int = 0,
    var registrationCode: String = "",
    var currentLocation : Int = 0,
    var currentLocationDate : LocalDate = LocalDate.parse("1970-01-01"),
    var currentLocationTime : LocalTime = LocalTime.parse("00:00")

) : DataClass<PlaneData>(id) {

    override val tableName = "planes"
    override val tableColumns = PlaneColumns.ALL

    override val indexes : List<IndexArgs> = listOf(
        IndexArgs("inx_planes_model_id", PlaneColumns.MODEL_ID.name),
        IndexArgs("inx_planes_current_location", PlaneColumns.CURRENT_LOCATION.name)
    )

    override val initialRows: List<PlaneData>
        get() = listOf(
            PlaneData(registrationCode = "G-BOE738", modelId = PlaneModelData.getPlaneModelId("Boeing 737-800"), currentLocation = DestinationData.getDestinationId("Luton")),
            PlaneData(registrationCode = "G-AIRA321", modelId = PlaneModelData.getPlaneModelId("Airbus A321"), currentLocation = DestinationData.getDestinationId("Luton"))
        )

    override val requiredTables: List<DataClass<*>>
        get() = listOf(
            PlaneModelData.EMPTY,
            DestinationData.EMPTY
        )

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            PlaneColumns.REGISTRATION_CODE to registrationCode,
            PlaneColumns.MODEL_ID to modelId,
            PlaneColumns.CURRENT_LOCATION to currentLocation,
            PlaneColumns.CURRENT_LOCATION_DATE to currentLocationDate,
            PlaneColumns.CURRENT_LOCATION_TIME to currentLocationTime
        )

    override fun mapRowToData(row : Array<Any?>) : PlaneData =
        PlaneData(
            id = castRowElement(row, PlaneColumns.ID),
            registrationCode = castRowElement(row, PlaneColumns.REGISTRATION_CODE),
            modelId = castRowElement(row, PlaneColumns.MODEL_ID),
            currentLocation = castRowElement(row, PlaneColumns.CURRENT_LOCATION),
            currentLocationDate = castDateRowElement(row, PlaneColumns.CURRENT_LOCATION_DATE),
            currentLocationTime = castTimeRowElement(row, PlaneColumns.CURRENT_LOCATION_TIME)
        )

    override fun debugData() {
        println("Plane data: (\"$id\", \"$registrationCode\", \"$modelId\", \"$currentLocation\", \"$currentLocationDate\", \"$currentLocationTime\")")
    }

    fun updateLocation(location : Int, date : LocalDate, time : LocalTime) {
        currentLocation = location
        currentLocationDate = date
        currentLocationTime = time
        update()
    }

    companion object {
        val EMPTY : PlaneData
            get() = PlaneData()

        fun queryDatabase (
            multipleJoinArgs : MultipleJoinArgs? = null,
            whereArgs : WhereArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null,
            groupByArgs : GroupByArgs? = null   
        ) : List<QueryResult<PlaneData>> {
            return EMPTY.queryDatabase(multipleJoinArgs, whereArgs, orderByArgs, limitArgs, groupByArgs)
        }

        fun queryDatabase(id : Int) : List<QueryResult<PlaneData>> = queryDatabase(whereArgs = WhereArgs("${PlaneColumns.ID.name} = ?", listOf(id)))

        fun getPlaneId(modelName: String): Int {
            val modelId = PlaneModelData.getPlaneModelId(modelName)
            return queryDatabase(
                whereArgs = WhereArgs("${PlaneColumns.MODEL_ID.name} = ?", listOf(modelId))
            ).firstOrNull()?.dataClass?.id ?: -1
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return PlaneData(id = id).delete()
        }

        fun getAvailablePlane (
            modelId : Int,
            locationId : Int,
            date : LocalDate,
            time : LocalTime
        ) : PlaneData? {
            val availablePlanes : List<QueryResult<PlaneData>> = PlaneData.queryDatabase(
                whereArgs = WhereArgs(
                    whereClause = "${PlaneColumns.MODEL_ID.name} = ?",
                    whereArgs = listOf(modelId)
                )
            )

            val planeAvailableAtTime : List<QueryResult<PlaneData>> = availablePlanes.filter {  plane ->
                LocalDateTime.of(
                    plane.dataClass.currentLocationDate, 
                    plane.dataClass.currentLocationTime
                ).isBefore(LocalDateTime.of(date, time))
                && plane.dataClass.currentLocation == locationId
            }
            
            return planeAvailableAtTime.firstOrNull()?.dataClass
        }
    }
}