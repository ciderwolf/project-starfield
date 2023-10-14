package starfield

import starfield.model.Game
import starfield.model.Lobby
import java.util.*

val games: MutableMap<UUID, Game> = Collections.synchronizedMap(mutableMapOf())

val lobbies: MutableMap<UUID, Lobby> = Collections.synchronizedMap(mutableMapOf())

fun findLobby(userId: UUID): Lobby? {
    return lobbies.values.find { it.hasPlayer(userId) }
}

fun findGame(userId: UUID): Game? {
    return games.values.find { it.hasPlayer(userId) }
}