package data

import java.time.LocalDate
import java.time.LocalTime

object PlaneColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val MODEL_ID = Column<Int>("model_id", "INTEGER NOT NULL REFERENCES plane_models(id)")
    val CURRENT_LOCATION = Column<Int>("current_location", "INTEGER NOT NULL")
    val CURRENT_LOCATION_DATE = Column<String>("current_location_date", "STRING NOT NULL")
    val CURRENT_LOCATION_TIME = Column<String>("current_location_time", "STRING NOT NULL")

    val ALL = listOf(ID, MODEL_ID, CURRENT_LOCATION, CURRENT_LOCATION_DATE, CURRENT_LOCATION_TIME)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class PlaneData(

    override val id: Int = 0,
    var modelId : Int = 0,
    var currentLocation : Int = 0,
    var currentLocationDate : LocalDate = LocalDate.parse("1970-01-01"),
    var currentLocationTime : LocalTime = LocalTime.parse("00:00")

) : DataClass<PlaneData>(id) {

    override val tableName = "planes"
    override val tableColumns = PlaneColumns.ALL

    override val initialRows: List<PlaneData>
        get() = listOf(
            PlaneData(modelId = PlaneModelData.getPlaneModelId("Boeing 737-800")),
            PlaneData(modelId = PlaneModelData.getPlaneModelId("Airbus A321"))
        )

    override val requiredTables: List<DataClass<*>>
        get() = listOf(
            PlaneModelData.EMPTY
        )

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            PlaneColumns.MODEL_ID to modelId,
            PlaneColumns.CURRENT_LOCATION to currentLocation,
            PlaneColumns.CURRENT_LOCATION_DATE to currentLocationDate,
            PlaneColumns.CURRENT_LOCATION_TIME to currentLocationTime
        )

    override fun mapRowToData(row : Array<Any?>) : PlaneData =
        PlaneData(
            id = castRowElement(row, PlaneColumns.ID),
            modelId = castRowElement(row, PlaneColumns.MODEL_ID),
            currentLocation = castRowElement(row, PlaneColumns.CURRENT_LOCATION),
            currentLocationDate = castDateRowElement(row, PlaneColumns.CURRENT_LOCATION_DATE),
            currentLocationTime = castTimeRowElement(row, PlaneColumns.CURRENT_LOCATION_TIME)
        )

    override fun debugData() {
        println("Plane data: (\"$id\", \"$modelId\", \"$currentLocation\", \"$currentLocationDate\", \"$currentLocationTime\")")
    }

    companion object {
        val EMPTY : PlaneData
            get() = PlaneData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null
        ) : List<QueryResult<PlaneData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun getPlaneId(modelName: String): Int {
            val modelId = PlaneModelData.getPlaneModelId(modelName)
            return queryDatabase(
                whereArgs = WhereArgs("${PlaneColumns.MODEL_ID} = ?", listOf(modelId))
            ).firstOrNull()?.dataClass?.id ?: -1
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return PlaneData(id = id).delete()
        }
    }
}