package data

object ClassColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val NAME = Column<String>("name", "VARCHAR NOT NULL UNIQUE")

    val ALL = listOf(ID, NAME)
    val COLUMN_NAMES = ALL.map { it.name }
}

object Classes {
    val FIRST_CLASS = "First Class"
    val BUSINESS = "Business"
    val ECONOMY = "Economy"
}

data class ClassData(

    override val id: Int = 0,
    var name : String? = null,

) : DataClass<ClassData>(id) {

    override val tableName = "classes"
    override val tableColumns = ClassColumns.ALL

    override val initialRows: List<ClassData>
        get() = listOf(
            ClassData(name = Classes.FIRST_CLASS),
            ClassData(name = Classes.BUSINESS),
            ClassData(name = Classes.ECONOMY)
        )

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            ClassColumns.NAME to name
        )

    override fun mapRowToData(row : Array<Any?>) : ClassData =
        ClassData(
            id = castRowElement(row, ClassColumns.ID),
            name = castRowElement(row, ClassColumns.NAME)
        )

    override fun debugData() {
        println("Class data: (\"$id\", \"$name\")")
    }

    companion object {
        val EMPTY : ClassData
            get() = ClassData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null
        ) : List<QueryResult<ClassData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs, orderByArgs, limitArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return ClassData(id = id).delete()
        }
    }
}
