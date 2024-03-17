package starfield.data.ingest

import com.opencsv.CSVReader

class CsvFile(private val reader: CSVReader): Iterator<Array<String>>, Iterable<Array<String>> {
    val headers: Array<String> = reader.readNext()
    private var nextRow: Array<String>? = null

    override fun hasNext(): Boolean {
        return if (nextRow != null) {
            true
        } else {
            nextRow = reader.readNext()
            nextRow != null
        }
    }

    override fun next(): Array<String> {
        return nextRow() ?: throw NoSuchElementException()
    }

    override fun iterator(): Iterator<Array<String>> {
        return this
    }

    private fun nextRow(): Array<String>? {
        return if (nextRow != null) {
            val returnVal = nextRow
            nextRow = null
            returnVal
        } else {
            reader.readNext()
        }
    }

    fun getColumn(row: Array<String>, column: String): String? {
        val colIndex = headers.indexOf(column) ?: return null
        return row[colIndex]
    }
}