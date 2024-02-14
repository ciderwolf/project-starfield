package starfield

import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import starfield.plugins.Location
import starfield.plugins.connections
import java.util.*
import kotlin.concurrent.schedule


object EntityPurge {
    private const val THIRTY_MINUTES = 30 * 60 * 1000L

    fun init() {
        val timer = Timer()
        timer.schedule(THIRTY_MINUTES, THIRTY_MINUTES) {
            runBlocking {
                purgeEntities()
            }

        }
    }

    private suspend fun purgeEntities() {
        val now = System.currentTimeMillis()
        val allGames = games.values.toList()
        for(game in allGames) {
            val timePassed = now - game.lastActionTime()
            if (timePassed > THIRTY_MINUTES) {
                // purge game
                println("Purging game ${game.id} after $timePassed ms")
                games.remove(game.id)
                game.users().forEach {
                    it.connection?.send(LocationMessage(Location.HOME, null))
                }
                connections.forEach {
                    it.send(DeleteListingMessage(game.id))
                }
            }
        }

        val toRemove = connections.filter {
            val timePassed = now - it.lastMessageTime()
            timePassed > THIRTY_MINUTES
        }.toMutableList()

        while(toRemove.isNotEmpty()) {
            val ws = toRemove.removeAt(toRemove.size - 1)
            val timePassed = now - ws.lastMessageTime()
            println("Purging connection ${ws.id} after $timePassed ms")
            ws.ws.close(CloseReason(CloseReason.Codes.NORMAL,"Closed due to inactivity"))
        }

    }

}