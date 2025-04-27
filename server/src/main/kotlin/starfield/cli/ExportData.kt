package starfield.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.opencsv.CSVWriter
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import starfield.data.dao.DatabaseSingleton
import java.io.BufferedWriter
import java.io.FileWriter

class ExportData : CliktCommand(help = "Export data from DB", name = "export") {

    private val query by mutuallyExclusiveOptions(
        option("--query", help = "SQL query to run").convert { it },
        option("--file", help = "SQL file to run").file(mustExist = true, canBeDir = false).convert { it.readText() },
    ).single().required()

    private val output by option("-o", "--output", help = "Destination file for results")
        .file(canBeDir = false).required()

    override fun run() {
        DatabaseSingleton.init()
        transaction {
            val conn = TransactionManager.current().connection
            val statement = conn.prepareStatement(query, false)
            val results = statement.executeQuery()
            val writer = CSVWriter(BufferedWriter(FileWriter(output)))

            val colCount = results.metaData.columnCount
            val headers = (1..colCount).map { results.metaData.getColumnLabel(it) }.toTypedArray()
            writer.writeNext(headers)

            while(results.next()) {
                val row = (1..colCount).map {
                    results.getObject(it)?.toString()
                }.toTypedArray()
                writer.writeNext(row)
            }

            writer.close()
        }
    }
}