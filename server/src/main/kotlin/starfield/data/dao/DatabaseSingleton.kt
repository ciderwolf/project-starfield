package starfield.data.dao

import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import org.jetbrains.exposed.sql.transactions.experimental.*
import starfield.data.table.*

object DatabaseSingleton {
    fun init() {
        val storagePath = System.getenv("STORAGE_PATH") ?: "./"

        val driverClassName = "org.h2.Driver"
        val jdbcURL = "jdbc:h2:${storagePath}db;AUTO_SERVER=TRUE"
        val database = Database.connect(jdbcURL, driverClassName)
        transaction(database) {
            SchemaUtils.create(Cards, Tokens, Users, Decks, DeckCards)

        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}