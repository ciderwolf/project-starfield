package starfield

import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import starfield.plugins.Location
import starfield.plugins.connections
import java.util.*
import kotlin.concurrent.schedule
import kotlin.properties.Delegates


object EntityPurge {

    fun init() {
        if (Config.entityPurgeTimeout == -1L) {
            return
        }
        val timer = Timer()
        timer.schedule(Config.entityPurgeTimeout, Config.entityPurgeTimeout) {
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
            if (timePassed > Config.entityPurgeTimeout) {
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

        val allLobbies = lobbies.values.toList()
        for(lobby in allLobbies) {
            val timePassed = now - lobby.lastActionTime()
            if (timePassed > Config.entityPurgeTimeout) {
                // purge lobby
                println("Purging lobby ${lobby.id} after $timePassed ms")
                lobbies.remove(lobby.id)
                lobby.users().forEach {
                    it.connection?.send(LocationMessage(Location.HOME, null))
                }
                connections.forEach {
                    it.send(DeleteListingMessage(lobby.id))
                }
            }
        }

        val toRemove = connections.filter {
            val timePassed = now - it.lastMessageTime()
            timePassed > Config.entityPurgeTimeout
        }.toMutableList()

        while(toRemove.isNotEmpty()) {
            val ws = toRemove.removeAt(toRemove.size - 1)
            val timePassed = now - ws.lastMessageTime()
            println("Purging connection ${ws.id} after $timePassed ms")
            ws.ws.close(CloseReason(CloseReason.Codes.NORMAL,"Closed due to inactivity"))
        }

    }

}