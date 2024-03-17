package starfield.data.ingest

import com.opencsv.CSVReader
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.batchUpsert
import org.jetbrains.exposed.sql.transactions.transaction

import starfield.data.table.Cards
import starfield.data.table.Tokens
import java.io.BufferedReader
import java.io.FileReader

object DataIngest {
    @Suppress("UNCHECKED_CAST")
    private fun writeCsvFileToTable(rows: CsvFile, table: Table) {
        transaction {
            table.batchInsert(rows) {
                for(col in table.columns) {
                    val rowVal = rows.getColumn(it, col.name)
                    if (rowVal == "" && col.columnType.nullable) {
                        this[col as Column<Any?>] = null
                    } else {
                        this[col as Column<Any>] = col.columnType.valueToDB(rowVal)!!
                    }
                }
            }
        }
    }

    fun insertCsv(path: String, tableName: String) {
        val table = matchTable(tableName)
        val reader = CSVReader(BufferedReader(FileReader(path)))
        val file = CsvFile(reader)

        val tableColumnNames = table.columns.map { it.name }.toSet()
        val fileColumnNames = file.headers.toSet()

        if (tableColumnNames != fileColumnNames) {
            System.err.println("Missing file columns: ${tableColumnNames - fileColumnNames}")
            System.err.println("Missing table columns: ${fileColumnNames - tableColumnNames}")
            return
        }

        writeCsvFileToTable(file, table)
    }

    private fun matchTable(name: String): Table {
        return when(name) {
            "Card" -> Cards
            "Token" -> Tokens
            "CardOracleIds" -> CardOracleIds
            else -> throw IllegalArgumentException("Unhandled table type $name")
        }
    }

    object CardOracleIds : Table("card_oracle_ids") {
        val card_id = uuid("card_id")
        val oracle_id = uuid("oracle_id")
    }
}