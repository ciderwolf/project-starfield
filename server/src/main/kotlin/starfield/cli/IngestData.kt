package starfield.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import starfield.data.dao.DatabaseSingleton
import starfield.data.ingest.DataIngest
import java.lang.IllegalArgumentException

class IngestData : CliktCommand(name="ingest", help="Import data into DB") {
    val data by option(help = "Path to CSV to upload").file(mustExist = true, canBeDir = false).required()
    val table by option(help = "Destination SQL table").required()

    override fun run() {
        DatabaseSingleton.init()

        try {
            DataIngest.insertCsv(data, table)
        } catch (e: IllegalArgumentException) {
            echo(e.message, err = true)
        }
    }
}