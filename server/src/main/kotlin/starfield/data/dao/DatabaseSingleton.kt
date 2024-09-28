package starfield.data.dao

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import org.jetbrains.exposed.sql.transactions.experimental.*
import starfield.Config
import starfield.data.table.*

object DatabaseSingleton {
    fun init() {
        val config = HikariConfig()
        config.jdbcUrl = Config.connectionString
        config.minimumIdle = 3
        val dataSource = HikariDataSource(config)
        val database = Database.connect(dataSource)
        transaction(database) {
            SchemaUtils.create(Cards, Tokens, Users, Decks, DeckCards, CardSources, CardExtras)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}