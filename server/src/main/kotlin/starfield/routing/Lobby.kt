package starfield.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import starfield.*
import starfield.data.dao.DeckDao
import starfield.draft.SetInfo
import starfield.model.*
import starfield.plugins.*
import java.util.*

@Serializable
data class DeckSelection(val deckId: Id)

@Serializable
data class CreateGameMessage(val name: String, val players: Int)

@Serializable
data class CreateDraftMessage(val name: String, val players: Int, val bots: Int, val set: Id)

fun Route.gameRouting() {

    get("/") {
        val listings = games.values.map { game ->
            GameListing(game.id, game.location(), game.name, game.userListings(), true)
        } + lobbies.values.map { lobby ->
            GameListing(lobby.id, Location.LOBBY, lobby.name, lobby.userListings(), false)
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

        val kind = call.parameters["kind"]
        val lobby = if (kind == "draft") {
            // create draft
            val definition = call.receive<CreateDraftMessage>()
            if (definition.players < 1 || definition.players > 8) {
                return@post call.respondValidationError("Invalid number of players")
            }

            if (definition.bots >= definition.players) {
                return@post call.respondValidationError("Invalid number of bots")
            }

            val setInfo = try {
                SetInfo.create(definition.set)
            } catch (e: Exception) {
                e.printStackTrace()
                return@post call.respondValidationError("Invalid set")
            }
            DraftLobby(UUID.randomUUID(), session.user(), definition.name, definition.players, setInfo, definition.bots)
        }
        else {
            val definition = call.receive<CreateGameMessage>()
            if (definition.players < 1 || definition.players > 4) {
                return@post call.respondValidationError("Invalid number of players")
            }
            GameLobby(UUID.randomUUID(), session.user(), definition.name, definition.players)
        }

        lobbies[lobby.id] = lobby
        val listing = GameListing(lobby.id, Location.LOBBY, lobby.name, lobby.userListings(), false)
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

        if (lobby !is GameLobby) {
            return@post call.respondError("You must be in a game lobby to chose a deck")
        }

        val body = call.receive<DeckSelection>()
        val deckDao = DeckDao()
        val deck = deckDao.getDeck(body.deckId) ?: return@post call.respondError("Deck not found")

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
            val listing = GameListing(lobby.id, Location.LOBBY, lobby.name, lobby.userListings(), false)
            connections.forEach {
                it.send(ListingUpdateMessage(listing))
            }
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
                val listing = GameListing(lobby.id, Location.LOBBY, lobby.name, lobby.userListings(), false)
                connections.forEach {
                    it.send(ListingUpdateMessage(listing))
                }
            }

            return@post call.respondSuccess("OK")
        } else {
            return@post call.respondError("Can't join that game")
        }
    }


    post("/{id}/end") {
        val session = call.sessions.get<UserSession>() ?: return@post call.respondError(
            "You must be logged in to end a game",
            status = HttpStatusCode.Unauthorized
        )
        val uuid = tryParseUuid(call.parameters["id"]) ?: return@post call.respondError("Invalid id")

        val game = games[uuid] ?: return@post call.respondError(
            "Game not found",
            status = HttpStatusCode.NotFound)

        if (game.end(session.user())) {
            games.remove(game.id)
            game.users().forEach {
                it.connection?.send(LocationMessage(Location.HOME, null))
            }
            connections.forEach {
                it.send(DeleteListingMessage(game.id))
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
        if (game is Draft) {
            game.start()
        }
        
        lobbies.remove(lobby.id)
        games[game.id] = game

        val listing = ListingUpdateMessage(GameListing(game.id, game.location(), game.name, game.userListings(), true))
        connections.forEach {
            it.send(listing)
        }

        game.users().forEach {
            it.connection?.send(StateMessage(game.currentState(it.id), game.location()))
            it.connection?.send(LocationMessage(game.location(), game.id))
        }

        call.respondSuccess("OK")
    }
}