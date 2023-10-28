package starfield.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import starfield.*
import starfield.model.Lobby
import starfield.plugins.*
import java.util.*

@Serializable
data class DeckSelection(val deckId: Id)

fun Route.gameRouting() {

    get("/") {
        val listings = games.values.map { game ->
            GameListing(game.id, game.name, game.users().map { it.id }, true)
        } + lobbies.values.map { lobby ->
            GameListing(lobby.id, lobby.name, lobby.users().map { it.id }, false)
        }

        call.respondSuccess(listings)
    }

    post("/") {
        val session = call.sessions.get<UserSession>() ?: return@post call.respondError(
            "You must be logged in to create a game",
            status = HttpStatusCode.Unauthorized
        )
        if(findGame(session.id) != null || findLobby(session.id) != null) {
            return@post call.respondError("Already in a different game")
        }

        val definition = call.receive<CreateGameMessage>()
        val lobby = Lobby(UUID.randomUUID(), session.user(), definition.name)
        lobbies[lobby.id] = lobby
        val listing = GameListing(lobby.id, lobby.name, lobby.users().map { it.id }, false)
        val message = ListingUpdateMessage(listing)
        connections.forEach {
            it.send(message)
        }
        lobby.owner.connection?.send(LocationMessage(Location.LOBBY, lobby.id))
        call.respondSuccess(lobby.currentState(session.id))
    }

    post("/chooseDeck") {
        val session = call.sessions.get<UserSession>() ?: return@post call.respondError(
            "You must be logged in to create a game",
            status = HttpStatusCode.Unauthorized
        )

        val lobby = findLobby(session.id) ?: return@post call.respondError(
            "You must be in a lobby to chose a deck")

        val body = call.receive<DeckSelection>()
        val deck = decks[body.deckId] ?: return@post call.respondError("Deck not found")

        lobby.setDeck(session.user(), deck)
        call.respondSuccess(body)
    }

    post("/{id}/join") {
        val session = call.sessions.get<UserSession>() ?: return@post call.respondError(
            "You must be logged in to join a game",
            status = HttpStatusCode.Unauthorized
        )
        if(findGame(session.id) != null || findLobby(session.id) != null) {
            return@post call.respondError("Already in a different game")
        }

        val uuid = tryParseUuid(call.parameters["id"]) ?: return@post call.respondError("Invalid id")
        val lobby = lobbies[uuid] ?: return@post call.respondError(
            "Game not found",
            status = HttpStatusCode.NotFound)

        if (lobby.canJoin()) {
            val user = session.user()
            lobby.join(user)
            user.connection?.send(LocationMessage(Location.LOBBY, lobby.id))
            return@post call.respondSuccess(lobby.currentState(user.id))
        } else {
            return@post call.respondError("Can't join that game")
        }
    }

    post("/{id}/leave") {
        val session = call.sessions.get<UserSession>() ?: return@post call.respondError(
            "You must be logged in to leave a game",
            status = HttpStatusCode.Unauthorized
        )
        val uuid = tryParseUuid(call.parameters["id"]) ?: return@post call.respondError("Invalid id")

        val lobby = lobbies[uuid] ?: return@post call.respondError(
            "Game not found",
            status = HttpStatusCode.NotFound)

        if (lobby.leave(session.user())) {

            if (lobby.owner.id == session.id) {
                lobbies.remove(lobby.id)
                lobby.users().forEach {
                    it.connection?.send(LocationMessage(Location.HOME, null))
                }
                connections.forEach {
                    it.send(DeleteListingMessage(lobby.id))
                }
            } else {
                val listing = GameListing(lobby.id, lobby.name, lobby.users().map { it.id }, false)
                connections.forEach {
                    it.send(ListingUpdateMessage(listing))
                }
            }

            return@post call.respondSuccess("OK")
        } else {
            return@post call.respondError("Can't join that game")
        }
    }

    post("/{id}/kick") {
        val session = call.sessions.get<UserSession>() ?: return@post call.respondError(
            "You must be logged in to leave a game",
            status = HttpStatusCode.Unauthorized
        )
        val gameId = tryParseUuid(call.parameters["id"]) ?: return@post call.respondError("Invalid game id")
        val lobby = lobbies[gameId] ?: return@post call.respondError("Game not found", HttpStatusCode.NotFound)
        if (lobby.owner.id != session.id) {
            return@post call.respondError("Must be the game owner to kick players", HttpStatusCode.Forbidden)
        }

        val playerId = tryParseUuid(call.parameters["player"]) ?: return@post call.respondError("Invalid player id")

        if (lobby.remove(playerId)) {
            connections.find { it.id == playerId }?.send(LocationMessage(Location.HOME, null))
            return@post call.respondSuccess("OK")
        } else {
            return@post call.respondError("Could not remove player")
        }
    }

    post("/{id}/start") {
        val session = call.sessions.get<UserSession>() ?: return@post call.respondError(
            "You must be logged in to start a game",
            status = HttpStatusCode.Unauthorized
        )
        val gameId = tryParseUuid(call.parameters["id"]) ?: return@post call.respondError("Invalid game id")
        val lobby = lobbies[gameId] ?: return@post call.respondError("Game not found", HttpStatusCode.NotFound)
        if (lobby.owner.id != session.id) {
            return@post call.respondError("Must be the game owner to start it", HttpStatusCode.Forbidden)
        }

        val game = lobby.startGame() ?: return@post call.respondError("Unable to start game")
        val listing = ListingUpdateMessage(GameListing(game.id, game.name, game.users().map { it.id }, false))
        connections.forEach {
            it.send(listing)
        }

        game.users().forEach {
            it.connection?.send(StateMessage(game.currentState(it.id), "game"))
            it.connection?.send(LocationMessage(Location.GAME, game.id))
        }

        call.respondSuccess("OK")
    }
}