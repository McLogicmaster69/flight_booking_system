package data

object TimezoneColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val NAME = Column<String>("name", "VARCHAR NOT NULL UNIQUE")
    val TIME_OFFSET = Column<Float>("time_offset", "FLOAT NOT NULL")

    val ALL = listOf(ID, NAME, TIME_OFFSET)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class TimezoneData(
    override val id: Int = 0,
    var name: String = "",
    var timeOffset: Float = 0f,
) : DataClass<TimezoneData>(id) {
    override val tableName = "timezones"
    override val tableColumns = TimezoneColumns.ALL

    override val initialRows: List<TimezoneData>
        get() =
            listOf(
                TimezoneData(name = "ACDT", timeOffset = 10.5f),
                TimezoneData(name = "ACST", timeOffset = 9.5f),
                TimezoneData(name = "ACT", timeOffset = -5.0f),
                TimezoneData(name = "ACWST", timeOffset = 8.75f),
                TimezoneData(name = "ADT", timeOffset = -3.0f),
                TimezoneData(name = "AEDT", timeOffset = 11.0f),
                TimezoneData(name = "AEST", timeOffset = 10.0f),
                TimezoneData(name = "AFT", timeOffset = 4.5f),
                TimezoneData(name = "AKDT", timeOffset = -8.0f),
                TimezoneData(name = "AKST", timeOffset = -9.0f),
                TimezoneData(name = "AMST", timeOffset = -3.0f),
                TimezoneData(name = "AMT", timeOffset = -4.0f),
                TimezoneData(name = "AMT", timeOffset = 4.0f),
                TimezoneData(name = "ART", timeOffset = -3.0f),
                TimezoneData(name = "AST", timeOffset = -4.0f),
                TimezoneData(name = "AST", timeOffset = 3.0f),
                TimezoneData(name = "AT", timeOffset = -4.0f),
                TimezoneData(name = "AWST", timeOffset = 8.0f),
                TimezoneData(name = "AZOST", timeOffset = 0.0f),
                TimezoneData(name = "AZOT", timeOffset = -1.0f),
                TimezoneData(name = "AZT", timeOffset = 4.0f),
                TimezoneData(name = "BDT", timeOffset = 8.0f),
                TimezoneData(name = "BIT", timeOffset = -12.0f),
                TimezoneData(name = "BNT", timeOffset = 8.0f),
                TimezoneData(name = "BOT", timeOffset = -4.0f),
                TimezoneData(name = "BRST", timeOffset = -2.0f),
                TimezoneData(name = "BRT", timeOffset = -3.0f),
                TimezoneData(name = "BST", timeOffset = 1.0f),
                TimezoneData(name = "BST", timeOffset = 6.0f),
                TimezoneData(name = "BST", timeOffset = 11.0f),
                TimezoneData(name = "BTT", timeOffset = 6.0f),
                TimezoneData(name = "CAT", timeOffset = 2.0f),
                TimezoneData(name = "CCT", timeOffset = 6.5f),
                TimezoneData(name = "CDT", timeOffset = -5.0f),
                TimezoneData(name = "CDT", timeOffset = -4.0f),
                TimezoneData(name = "CEST", timeOffset = 2.0f),
                TimezoneData(name = "CET", timeOffset = 1.0f),
                TimezoneData(name = "CHADT", timeOffset = 13.75f),
                TimezoneData(name = "CHAST", timeOffset = 12.75f),
                TimezoneData(name = "CHOST", timeOffset = 9.0f),
                TimezoneData(name = "CHOT", timeOffset = 8.0f),
                TimezoneData(name = "CHST", timeOffset = 10.0f),
                TimezoneData(name = "CHUT", timeOffset = 10.0f),
                TimezoneData(name = "CIST", timeOffset = -8.0f),
                TimezoneData(name = "CIT", timeOffset = 8.0f),
                TimezoneData(name = "CKT", timeOffset = -10.0f),
                TimezoneData(name = "CLST", timeOffset = -3.0f),
                TimezoneData(name = "CLT", timeOffset = -4.0f),
                TimezoneData(name = "COST", timeOffset = -4.0f),
                TimezoneData(name = "COT", timeOffset = -5.0f),
                TimezoneData(name = "CST", timeOffset = -6.0f),
                TimezoneData(name = "CST", timeOffset = 8.0f),
                TimezoneData(name = "CST", timeOffset = -5.0f),
                TimezoneData(name = "CT", timeOffset = -6.0f),
                TimezoneData(name = "CVT", timeOffset = -1.0f),
                TimezoneData(name = "CWST", timeOffset = 8.75f),
                TimezoneData(name = "CXT", timeOffset = 7.0f),
                TimezoneData(name = "DAVT", timeOffset = 7.0f),
                TimezoneData(name = "DDUT", timeOffset = 10.0f),
                TimezoneData(name = "EASST", timeOffset = -5.0f),
                TimezoneData(name = "EAST", timeOffset = -6.0f),
                TimezoneData(name = "EAT", timeOffset = 3.0f),
                TimezoneData(name = "ECT", timeOffset = -5.0f),
                TimezoneData(name = "EDT", timeOffset = -4.0f),
                TimezoneData(name = "EEST", timeOffset = 3.0f),
                TimezoneData(name = "EET", timeOffset = 2.0f),
                TimezoneData(name = "EGST", timeOffset = 0.0f),
                TimezoneData(name = "EGT", timeOffset = -1.0f),
                TimezoneData(name = "EIT", timeOffset = 9.0f),
                TimezoneData(name = "EST", timeOffset = -5.0f),
                TimezoneData(name = "ET", timeOffset = -5.0f),
                TimezoneData(name = "FET", timeOffset = 3.0f),
                TimezoneData(name = "FJT", timeOffset = 12.0f),
                TimezoneData(name = "FKST", timeOffset = -3.0f),
                TimezoneData(name = "FKT", timeOffset = -4.0f),
                TimezoneData(name = "FNT", timeOffset = -2.0f),
                TimezoneData(name = "GALT", timeOffset = -6.0f),
                TimezoneData(name = "GAMT", timeOffset = -9.0f),
                TimezoneData(name = "GET", timeOffset = 4.0f),
                TimezoneData(name = "GFT", timeOffset = -3.0f),
                TimezoneData(name = "GILT", timeOffset = 12.0f),
                TimezoneData(name = "GIT", timeOffset = -9.0f),
                TimezoneData(name = "GMT", timeOffset = 0.0f),
                TimezoneData(name = "GST", timeOffset = 4.0f),
                TimezoneData(name = "GST", timeOffset = -2.0f),
                TimezoneData(name = "GYT", timeOffset = -4.0f),
                TimezoneData(name = "HADT", timeOffset = -9.0f),
                TimezoneData(name = "HAST", timeOffset = -10.0f),
                TimezoneData(name = "HKT", timeOffset = 8.0f),
                TimezoneData(name = "HMT", timeOffset = 5.0f),
                TimezoneData(name = "HOVST", timeOffset = 8.0f),
                TimezoneData(name = "HOVT", timeOffset = 7.0f),
                TimezoneData(name = "ICT", timeOffset = 7.0f),
                TimezoneData(name = "IDT", timeOffset = 3.0f),
                TimezoneData(name = "IOT", timeOffset = 6.0f),
                TimezoneData(name = "IRDT", timeOffset = 4.5f),
                TimezoneData(name = "IRKT", timeOffset = 8.0f),
                TimezoneData(name = "IRST", timeOffset = 3.5f),
                TimezoneData(name = "IST", timeOffset = 5.5f),
                TimezoneData(name = "IST", timeOffset = 1.0f),
                TimezoneData(name = "IST", timeOffset = 2.0f),
                TimezoneData(name = "JST", timeOffset = 9.0f),
                TimezoneData(name = "KGT", timeOffset = 6.0f),
                TimezoneData(name = "KOST", timeOffset = 11.0f),
                TimezoneData(name = "KRAT", timeOffset = 7.0f),
                TimezoneData(name = "KST", timeOffset = 9.0f),
                TimezoneData(name = "LHDT", timeOffset = 11.0f),
                TimezoneData(name = "LHST", timeOffset = 10.5f),
                TimezoneData(name = "LINT", timeOffset = 14.0f),
                TimezoneData(name = "MAGT", timeOffset = 11.0f),
                TimezoneData(name = "MART", timeOffset = -9.5f),
                TimezoneData(name = "MAWT", timeOffset = 5.0f),
                TimezoneData(name = "MDT", timeOffset = -6.0f),
                TimezoneData(name = "MHT", timeOffset = 12.0f),
                TimezoneData(name = "MIST", timeOffset = 11.0f),
                TimezoneData(name = "MIT", timeOffset = -9.5f),
                TimezoneData(name = "MMT", timeOffset = 6.5f),
                TimezoneData(name = "MSK", timeOffset = 3.0f),
                TimezoneData(name = "MST", timeOffset = -7.0f),
                TimezoneData(name = "MST", timeOffset = 8.0f),
                TimezoneData(name = "MT", timeOffset = -7.0f),
                TimezoneData(name = "MUT", timeOffset = 4.0f),
                TimezoneData(name = "MVT", timeOffset = 5.0f),
                TimezoneData(name = "MYT", timeOffset = 8.0f),
                TimezoneData(name = "NCT", timeOffset = 11.0f),
                TimezoneData(name = "NDT", timeOffset = -2.5f),
                TimezoneData(name = "NFT", timeOffset = 11.0f),
                TimezoneData(name = "NPT", timeOffset = 5.75f),
                TimezoneData(name = "NRT", timeOffset = 12.0f),
                TimezoneData(name = "NST", timeOffset = -3.5f),
                TimezoneData(name = "NT", timeOffset = -3.5f),
                TimezoneData(name = "NUT", timeOffset = -11.0f),
                TimezoneData(name = "NZDT", timeOffset = 13.0f),
                TimezoneData(name = "NZST", timeOffset = 12.0f),
                TimezoneData(name = "OMST", timeOffset = 6.0f),
                TimezoneData(name = "ORAT", timeOffset = 5.0f),
                TimezoneData(name = "PDT", timeOffset = -7.0f),
                TimezoneData(name = "PET", timeOffset = -5.0f),
                TimezoneData(name = "PETT", timeOffset = 12.0f),
                TimezoneData(name = "PGT", timeOffset = 10.0f),
                TimezoneData(name = "PHOT", timeOffset = 13.0f),
                TimezoneData(name = "PhST", timeOffset = 8.0f),
                TimezoneData(name = "PHT", timeOffset = 8.0f),
                TimezoneData(name = "PKT", timeOffset = 5.0f),
                TimezoneData(name = "PMDT", timeOffset = -2.0f),
                TimezoneData(name = "PMST", timeOffset = -3.0f),
                TimezoneData(name = "PONT", timeOffset = 11.0f),
                TimezoneData(name = "PST", timeOffset = -8.0f),
                TimezoneData(name = "PT", timeOffset = -8.0f),
                TimezoneData(name = "PWT", timeOffset = 9.0f),
                TimezoneData(name = "PYST", timeOffset = -3.0f),
                TimezoneData(name = "PYT", timeOffset = -4.0f),
                TimezoneData(name = "RET", timeOffset = 4.0f),
                TimezoneData(name = "ROTT", timeOffset = -3.0f),
                TimezoneData(name = "SAKT", timeOffset = 11.0f),
                TimezoneData(name = "SAMT", timeOffset = 4.0f),
                TimezoneData(name = "SAST", timeOffset = 2.0f),
                TimezoneData(name = "SBT", timeOffset = 11.0f),
                TimezoneData(name = "SCT", timeOffset = 4.0f),
                TimezoneData(name = "SGT", timeOffset = 8.0f),
                TimezoneData(name = "SLST", timeOffset = 5.5f),
                TimezoneData(name = "SRET", timeOffset = 11.0f),
                TimezoneData(name = "SRT", timeOffset = -3.0f),
                TimezoneData(name = "SST", timeOffset = -11.0f),
                TimezoneData(name = "SYOT", timeOffset = 3.0f),
                TimezoneData(name = "TAHT", timeOffset = -10.0f),
                TimezoneData(name = "TFT", timeOffset = 5.0f),
                TimezoneData(name = "THA", timeOffset = 7.0f),
                TimezoneData(name = "TJT", timeOffset = 5.0f),
                TimezoneData(name = "TKT", timeOffset = 13.0f),
                TimezoneData(name = "TLT", timeOffset = 9.0f),
                TimezoneData(name = "TMT", timeOffset = 5.0f),
                TimezoneData(name = "TOT", timeOffset = 13.0f),
                TimezoneData(name = "TRT", timeOffset = 3.0f),
                TimezoneData(name = "TVT", timeOffset = 12.0f),
                TimezoneData(name = "ULAST", timeOffset = 9.0f),
                TimezoneData(name = "ULAT", timeOffset = 8.0f),
                TimezoneData(name = "USZ1", timeOffset = 2.0f),
                TimezoneData(name = "UTC", timeOffset = 0f),
                TimezoneData(name = "UYST", timeOffset = -2.0f),
                TimezoneData(name = "UYT", timeOffset = -3.0f),
                TimezoneData(name = "UZT", timeOffset = 5.0f),
                TimezoneData(name = "VET", timeOffset = -4.0f),
                TimezoneData(name = "VLAT", timeOffset = 10.0f),
                TimezoneData(name = "VOLT", timeOffset = 4.0f),
                TimezoneData(name = "VOST", timeOffset = 6.0f),
                TimezoneData(name = "VUT", timeOffset = 11.0f),
                TimezoneData(name = "WAKT", timeOffset = 12.0f),
                TimezoneData(name = "WAST", timeOffset = 2.0f),
                TimezoneData(name = "WAT", timeOffset = 1.0f),
                TimezoneData(name = "WEST", timeOffset = 1.0f),
                TimezoneData(name = "WET", timeOffset = 0.0f),
                TimezoneData(name = "WFT", timeOffset = 12.0f),
                TimezoneData(name = "WGST", timeOffset = -2.0f),
                TimezoneData(name = "WGST", timeOffset = -3.0f),
                TimezoneData(name = "WIB", timeOffset = 7.0f),
                TimezoneData(name = "WIT", timeOffset = 9.0f),
                TimezoneData(name = "WST", timeOffset = 8.0f),
                TimezoneData(name = "YAKT", timeOffset = 9.0f),
                TimezoneData(name = "YEKT", timeOffset = 5.0f),
            )

    override fun mapDataToColumns(): Map<Column<*>, Any?> =
        mapOf(
            TimezoneColumns.NAME to name,
            TimezoneColumns.TIME_OFFSET to timeOffset,
        )

    override fun mapRowToData(row: Array<Any?>): TimezoneData =
        TimezoneData(
            id = castRowElement(row, TimezoneColumns.ID),
            name = castRowElement(row, TimezoneColumns.NAME),
            timeOffset = castRowElement(row, TimezoneColumns.TIME_OFFSET),
        )

    override fun debugData() {
        println("Country data: (\"$id\", \"$name\", \"$timeOffset\")")
    }

    companion object {
        val EMPTY: TimezoneData
            get() = TimezoneData()

        fun queryDatabase(
            multipleJoinArgs: MultipleJoinArgs? = null,
            whereArgs: WhereArgs? = null,
            orderByArgs: OrderByArgs? = null,
            limitArgs: LimitArgs? = null,
            groupByArgs: GroupByArgs? = null,
        ): List<QueryResult<TimezoneData>> =
            EMPTY.queryDatabase(multipleJoinArgs, whereArgs, orderByArgs, limitArgs, groupByArgs)

        fun queryDatabase(id: Int): List<QueryResult<TimezoneData>> =
            queryDatabase(whereArgs = WhereArgs("${TimezoneColumns.ID.name} = ?", listOf(id)))

        fun updateTable(
            values: Map<Column<*>, Any?>,
            whereArgs: WhereArgs,
        ): Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id: Int): Int = TimezoneData(id = id).delete()

        fun getTimezoneId(timezone: String): Int {
            val query: List<QueryResult<TimezoneData>> =
                queryDatabase(whereArgs = WhereArgs("${TimezoneColumns.NAME.name} = ?", listOf(timezone)))
            if (query.isEmpty()) {
                println("Could not find timezone $timezone")
                return -1
            }

            return query.first().dataClass.id
        }
    }
}
