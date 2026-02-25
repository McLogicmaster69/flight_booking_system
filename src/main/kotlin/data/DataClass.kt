package data

abstract class DataClass {
    abstract val tableName: String
    abstract val tableColumns : Map<String, String>

    val tableCreateSQL : String
        get() = tableColumns.entries.joinToString(", ") { (name, defintion) -> "$name $defintion" }

    abstract fun mapDataToKeys () : Map<String, Any?>

    abstract fun insertIntoDatabase()
}
