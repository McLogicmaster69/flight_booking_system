package data

object PlaneColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val MODEL_ID = Column<Int>("model_id", "INTEGER NOT NULL REFERENCES plane_models(id)")

    val ALL = listOf(ID, MODEL_ID)
}

data class PlaneData(

    override val id: Int = 0,
    var modelId : Int = 0,

) : DataClass<PlaneData>(id) {

    override val tableName = "planes"
    override val tableColumns = PlaneColumns.ALL

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            PlaneColumns.MODEL_ID to modelId
        )

    override fun mapRowToData(row : Array<Any?>) : PlaneData =
        PlaneData(
            id = castRowElement(row, PlaneColumns.ID),
            modelId = castRowElement(row, PlaneColumns.MODEL_ID)
        )

    override fun debugData() {
        println("Plane data: (\"$id\", \"$modelId\")")
    }

    companion object {
        val EMPTY : PlaneData
            get() = PlaneData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null) : List<QueryResult<PlaneData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs)
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
