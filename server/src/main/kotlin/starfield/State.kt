package starfield

import starfield.model.ActiveCollectionState
import starfield.model.ActiveUserCollection
import starfield.model.Game
import starfield.model.Lobby
import java.util.*

val games: MutableMap<UUID, ActiveUserCollection<ActiveCollectionState>> = Collections.synchronizedMap(mutableMapOf())

val lobbies: MutableMap<UUID, Lobby<ActiveCollectionState, *>> = Collections.synchronizedMap(mutableMapOf())

fun findLobby(userId: UUID): Lobby<ActiveCollectionState, *>? {
    return lobbies.values.find { it.hasPlayer(userId) }
}

fun findGame(userId: UUID): ActiveUserCollection<ActiveCollectionState>? {
    return games.values.find { it.hasPlayer(userId) }
}

fun findBoardGame(userId: UUID): Game? {
    val result = games.values.find { it is Game && it.hasPlayer(userId) }
    return result as? Game
}

fun findAnyBoardGame(gameId: UUID): Game? {
    val result = games.values.find { it is Game && it.id == gameId }
    return result as? Game
}


fun findGameSpectating(userId: UUID): Game? {
    val result = games.values.find { it is Game && it.hasSpectator(userId) }
    return result as? Game
}