package starfield.data.ingest

import starfield.data.dao.DatabaseSingleton

fun main() {
    DatabaseSingleton.init()

    DataIngest.insertCsv("../card_oracle_ids.csv", "CardOracleIds")
}