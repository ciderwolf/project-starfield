package starfield.data.dao

import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import org.jetbrains.exposed.sql.transactions.experimental.*
import starfield.Config
import starfield.data.table.*

object DatabaseSingleton {
    fun init() {
        val driverClassName = "org.postgresql.Driver"
        val database = Database.connect(Config.connectionString, driverClassName)
        transaction(database) {
            SchemaUtils.create(Cards, Tokens, Users, Decks, DeckCards, CardSources)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}