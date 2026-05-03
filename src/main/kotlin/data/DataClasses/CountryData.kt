package data

object CountryColumns {
    val ID = Column<Int>("id", "INTEGER PRIMARY KEY AUTOINCREMENT")
    val NAME = Column<String>("name", "VARCHAR NOT NULL UNIQUE")

    val ALL = listOf(ID, NAME)
    val COLUMN_NAMES = ALL.map { it.name }
}

data class CountryData(

    override val id: Int = 0,
    var name : String = ""

) : DataClass<CountryData>(id) {

    override val tableName = "countries"
    override val tableColumns = CountryColumns.ALL

    override val initialRows : List<CountryData>
        get() = listOf(
            CountryData(name = "Afghanistan"),
            CountryData(name = "Albania"),
            CountryData(name = "Algeria"),
            CountryData(name = "Andorra"),
            CountryData(name = "Angola"),
            CountryData(name = "Antigua and Barbuda"),
            CountryData(name = "Argentina"),
            CountryData(name = "Armenia"),
            CountryData(name = "Australia"),
            CountryData(name = "Austria"),
            CountryData(name = "Azerbaijan"),
            CountryData(name = "The Bahamas"),
            CountryData(name = "Bahrain"),
            CountryData(name = "Bangladesh"),
            CountryData(name = "Barbados"),
            CountryData(name = "Belarus"),
            CountryData(name = "Belgium"),
            CountryData(name = "Belize"),
            CountryData(name = "Benin"),
            CountryData(name = "Bhutan"),
            CountryData(name = "Bolivia"),
            CountryData(name = "Bosnia and Herzegovina"),
            CountryData(name = "Botswana"),
            CountryData(name = "Brazil"),
            CountryData(name = "Brunei"),
            CountryData(name = "Bulgaria"),
            CountryData(name = "Burkina Faso"),
            CountryData(name = "Burundi"),
            CountryData(name = "Cambodia"),
            CountryData(name = "Cameroon"),
            CountryData(name = "Canada"),
            CountryData(name = "Cape Verde"),
            CountryData(name = "Central African Republic"),
            CountryData(name = "Chad"),
            CountryData(name = "Chile"),
            CountryData(name = "China"),
            CountryData(name = "Colombia"),
            CountryData(name = "Comoros"),
            CountryData(name = "Congo, Republic of the"),
            CountryData(name = "Congo, Democratic Republic of the"),
            CountryData(name = "Costa Rica"),
            CountryData(name = "Cote d'Ivoire"),
            CountryData(name = "Croatia"),
            CountryData(name = "Cuba"),
            CountryData(name = "Cyprus"),
            CountryData(name = "Czech Republic"),
            CountryData(name = "Denmark"),
            CountryData(name = "Djibouti"),
            CountryData(name = "Dominica"),
            CountryData(name = "Dominican Republic"),
            CountryData(name = "East Timor (Timor-Leste)"),
            CountryData(name = "Ecuador"),
            CountryData(name = "Egypt"),
            CountryData(name = "El Salvador"),
            CountryData(name = "Equatorial Guinea"),
            CountryData(name = "Eritrea"),
            CountryData(name = "Estonia"),
            CountryData(name = "Ethiopia"),
            CountryData(name = "Fiji"),
            CountryData(name = "Finland"),
            CountryData(name = "France"),
            CountryData(name = "Gabon"),
            CountryData(name = "The Gambia"),
            CountryData(name = "Georgia"),
            CountryData(name = "Germany"),
            CountryData(name = "Ghana"),
            CountryData(name = "Greece"),
            CountryData(name = "Grenada"),
            CountryData(name = "Guatemala"),
            CountryData(name = "Guinea"),
            CountryData(name = "Guinea-Bissau"),
            CountryData(name = "Guyana"),
            CountryData(name = "Haiti"),
            CountryData(name = "Honduras"),
            CountryData(name = "Hungary"),
            CountryData(name = "Iceland"),
            CountryData(name = "India"),
            CountryData(name = "Indonesia"),
            CountryData(name = "Iran"),
            CountryData(name = "Iraq"),
            CountryData(name = "Ireland"),
            CountryData(name = "Israel"),
            CountryData(name = "Italy"),
            CountryData(name = "Jamaica"),
            CountryData(name = "Japan"),
            CountryData(name = "Jordan"),
            CountryData(name = "Kazakhstan"),
            CountryData(name = "Kenya"),
            CountryData(name = "Kiribati"),
            CountryData(name = "Korea, North"),
            CountryData(name = "Korea, South"),
            CountryData(name = "Kosovo"),
            CountryData(name = "Kuwait"),
            CountryData(name = "Kyrgyzstan"),
            CountryData(name = "Laos"),
            CountryData(name = "Latvia"),
            CountryData(name = "Lebanon"),
            CountryData(name = "Lesotho"),
            CountryData(name = "Liberia"),
            CountryData(name = "Libya"),
            CountryData(name = "Liechtenstein"),
            CountryData(name = "Lithuania"),
            CountryData(name = "Luxembourg"),
            CountryData(name = "Macedonia"),
            CountryData(name = "Madagascar"),
            CountryData(name = "Malawi"),
            CountryData(name = "Malaysia"),
            CountryData(name = "Maldives"),
            CountryData(name = "Mali"),
            CountryData(name = "Malta"),
            CountryData(name = "Marshall Islands"),
            CountryData(name = "Mauritania"),
            CountryData(name = "Mauritius"),
            CountryData(name = "Mexico"),
            CountryData(name = "Micronesia, Federated States of"),
            CountryData(name = "Moldova"),
            CountryData(name = "Monaco"),
            CountryData(name = "Mongolia"),
            CountryData(name = "Montenegro"),
            CountryData(name = "Morocco"),
            CountryData(name = "Mozambique"),
            CountryData(name = "Myanmar (Burma)"),
            CountryData(name = "Namibia"),
            CountryData(name = "Nauru"),
            CountryData(name = "Nepal"),
            CountryData(name = "Netherlands"),
            CountryData(name = "New Zealand"),
            CountryData(name = "Nicaragua"),
            CountryData(name = "Niger"),
            CountryData(name = "Nigeria"),
            CountryData(name = "Norway"),
            CountryData(name = "Oman"),
            CountryData(name = "Pakistan"),
            CountryData(name = "Palau"),
            CountryData(name = "Panama"),
            CountryData(name = "Papua New Guinea"),
            CountryData(name = "Paraguay"),
            CountryData(name = "Peru"),
            CountryData(name = "Philippines"),
            CountryData(name = "Poland"),
            CountryData(name = "Portugal"),
            CountryData(name = "Qatar"),
            CountryData(name = "Romania"),
            CountryData(name = "Russia"),
            CountryData(name = "Rwanda"),
            CountryData(name = "Saint Kitts and Nevis"),
            CountryData(name = "Saint Lucia"),
            CountryData(name = "Saint Vincent and the Grenadines"),
            CountryData(name = "Samoa"),
            CountryData(name = "San Marino"),
            CountryData(name = "Sao Tome and Principe"),
            CountryData(name = "Saudi Arabia"),
            CountryData(name = "Senegal"),
            CountryData(name = "Serbia"),
            CountryData(name = "Seychelles"),
            CountryData(name = "Sierra Leone"),
            CountryData(name = "Singapore"),
            CountryData(name = "Slovakia"),
            CountryData(name = "Slovenia"),
            CountryData(name = "Solomon Islands"),
            CountryData(name = "Somalia"),
            CountryData(name = "South Africa"),
            CountryData(name = "South Sudan"),
            CountryData(name = "Spain"),
            CountryData(name = "Sri Lanka"),
            CountryData(name = "Sudan"),
            CountryData(name = "Suriname"),
            CountryData(name = "Swaziland"),
            CountryData(name = "Sweden"),
            CountryData(name = "Switzerland"),
            CountryData(name = "Syria"),
            CountryData(name = "Taiwan"),
            CountryData(name = "Tajikistan"),
            CountryData(name = "Tanzania"),
            CountryData(name = "Thailand"),
            CountryData(name = "Togo"),
            CountryData(name = "Tonga"),
            CountryData(name = "Trinidad and Tobago"),
            CountryData(name = "Tunisia"),
            CountryData(name = "Turkey"),
            CountryData(name = "Turkmenistan"),
            CountryData(name = "Tuvalu"),
            CountryData(name = "Uganda"),
            CountryData(name = "Ukraine"),
            CountryData(name = "United Arab Emirates"),
            CountryData(name = "United Kingdom"),
            CountryData(name = "United States of America"),
            CountryData(name = "Uruguay"),
            CountryData(name = "Uzbekistan"),
            CountryData(name = "Vanuatu"),
            CountryData(name = "Vatican City (Holy See)"),
            CountryData(name = "Venezuela"),
            CountryData(name = "Vietnam"),
            CountryData(name = "Yemen"),
            CountryData(name = "Zambia"),
            CountryData(name = "Zimbabwe")
        )

    override fun mapDataToColumns () : Map<Column<*>, Any?> =
        mapOf(
            CountryColumns.NAME to name
        )

    override fun mapRowToData(row : Array<Any?>) : CountryData =
        CountryData(
            id = castRowElement(row, CountryColumns.ID),
            name = castRowElement(row, CountryColumns.NAME)
        )

    override fun debugData() {
        println("Country data: (\"$id\", \"$name\")")
    }

    companion object {
        val EMPTY : CountryData
            get() = CountryData()

        fun queryDatabase (
            joinArgs : JoinArgs? = null,
            whereArgs : WhereArgs? = null,
            orderByArgs : OrderByArgs? = null,
            limitArgs : LimitArgs? = null
        ) : List<QueryResult<CountryData>> {
            return EMPTY.queryDatabase(joinArgs, whereArgs, orderByArgs, limitArgs)
        }

        fun updateTable (
            values : Map<Column<*>, Any?>,
            whereArgs : WhereArgs
        ) : Int = EMPTY.updateTable(values, whereArgs)

        fun delete(id : Int) : Int {
            return CountryData(id = id).delete()
        }

        fun getCountryId (country : String) : Int {
            val query : List<QueryResult<CountryData>> = queryDatabase(whereArgs = WhereArgs("${CountryColumns.NAME.name} = ?", listOf(country)))
            if (query.isEmpty()) {
                println("Could not find country $country")
                return -1
            }

            return query.first().dataClass.id
        }
    }
}
