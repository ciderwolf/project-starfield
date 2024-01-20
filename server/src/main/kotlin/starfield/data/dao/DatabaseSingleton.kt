package starfield.data.dao

import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import org.jetbrains.exposed.sql.transactions.experimental.*
import starfield.data.table.Cards
import starfield.data.table.DeckCards
import starfield.data.table.Decks
import starfield.data.table.Users

object DatabaseSingleton {
    fun init() {
        val driverClassName = "org.h2.Driver"
        val jdbcURL = "jdbc:h2:./build/db;AUTO_SERVER=TRUE"
        val database = Database.connect(jdbcURL, driverClassName)
        transaction(database) {
            SchemaUtils.create(Cards, Users, Decks, DeckCards)

        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}