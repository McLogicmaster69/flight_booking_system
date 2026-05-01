package data

import java.time.LocalTime

object PlaneSeatColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val PLANE_MODEL_ID = Column<Int>("plane_model_id", "INTEGER NOT NULL REFERENCES ${PlaneModelData.EMPTY.tableName}(id)")
    val CLASS_ID = Column<Int>("class_id", "INTEGER NOT NULL REFERENCES ${ClassData.EMPTY.tableName}(id)")
    val ROW = Column<Int>("amount", "INTEGER NOT NULL")
    val LETTER = Column<String>("letter", "STRING NOT NULL")

    val ALL = listOf(ID, PLANE_MODEL_ID, CLASS_ID, ROW, LETTER)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class PlaneSeatData(

    override val id: Int = 0,
    var planeModelId : Int = 0,
    var classId : Int = 0,
    var row : Int = 0,
    var letter : String = ""

) : DataClass<PlaneSeatData>(id) {

    override val tableName = "plane_seats"
    override val tableColumns = PlaneSeatColumns.ALL
    override val tableAdditionalSQL = "UNIQUE (plane_model_id, class_id)"

    override val initialRows : List<PlaneSeatData>
        get() = listOf(
        )

    override val requiredTables : List<DataClass<*>>
        get() = listOf(
            PlaneModelData.EMPTY,
            ClassData.EMPTY
        )

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            PlaneSeatColumns.PLANE_MODEL_ID to planeModelId,
            PlaneSeatColumns.CLASS_ID to classId,
            PlaneSeatColumns.ROW to row,
            PlaneSeatColumns.LETTER to letter
        )

    override fun mapRowToData(row : Array<Any?>) : PlaneSeatData =
        PlaneSeatData(
            id = castRowElement(row, PlaneSeatColumns.ID),
            planeModelId = castRowElement(row, PlaneSeatColumns.PLANE_MODEL_ID),
            classId = castRowElement(row, PlaneSeatColumns.CLASS_ID),
            row = castRowElement(row, PlaneSeatColumns.ROW),
            letter = castRowElement(row, PlaneSeatColumns.LETTER)
        )

    override fun debugData() {
        println("Route data: (\"$id\", \"$planeModelId\", \"$classId\", \"$row\", \"$letter\")")
    }

    companion object {
        val EMPTY : PlaneSeatData
            get() = PlaneSeatData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null
        ) : List<QueryResult<PlaneSeatData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return PlaneSeatData(id = id).delete()
        }
    }
}
