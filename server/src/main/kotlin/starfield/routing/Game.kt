package starfield.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import starfield.data.dao.CardDao
import starfield.findGame
import starfield.engine.OracleId
import starfield.engine.Zone
import starfield.games
import starfield.model.AccountableAction
import starfield.plugins.*

@Serializable
data class VirtualIdsMessage(
    val virtualIds: Map<Id, OracleId>,
    val oracleInfo: Map<OracleId, CardDao.OracleCard>
)

@Serializable
data class SideboardingVirtualIdsMessage(
    val main: Map<Id, OracleId>,
    val side: Map<Id, OracleId>,
    val oracleInfo: Map<OracleId, CardDao.OracleCard>
)

fun Route.engineRouting() {
    get("virtual-ids/library") {
        val session = call.sessions.get<UserSession>() ?: return@get call.respondError(
            "You must be logged in",
            status = HttpStatusCode.Unauthorized
        )

        val game = findGame(session.id) ?: return@get call.respondError("Not in a game")
        val ids = game.assignVirtualIds(session.id)[Zone.LIBRARY]!!
        game.sendAccountabilityMessage(AccountableAction.FIND_CARD, session.id)
        val oracleInfo = CardDao().getCards(ids.values).associate {
            val card = it.toOracleCard()
            Pair(card.id, card)
        }

        call.respondSuccess(VirtualIdsMessage(ids, oracleInfo))
    }

    get("virtual-ids/sideboarding") {
        val session = call.sessions.get<UserSession>() ?: return@get call.respondError(
            "You must be logged in",
            status = HttpStatusCode.Unauthorized
        )

        val game = findGame(session.id) ?: return@get call.respondError("Not in a game")
        val ids = game.assignVirtualIds(session.id, scoop=true)
        game.sendAccountabilityMessage(AccountableAction.SIDEBOARD, session.id)
        val oracleInfo = CardDao().getCards(ids.values.flatMap { it.values }).associate {
            val card = it.toOracleCard()
            Pair(card.id, card)
        }

        val main = ids.entries
            .filter { it.key != Zone.SIDEBOARD }
            .map { it.value.entries }
            .flatten()
            .associate { Pair(it.key, it.value) }

        val side = ids[Zone.SIDEBOARD]!!

        call.respondSuccess(SideboardingVirtualIdsMessage(main, side, oracleInfo))
    }

    post("{id}/spectate") {
        val session = call.sessions.get<UserSession>() ?: return@post call.respondError(
            "You must be logged in",
            status = HttpStatusCode.Unauthorized
        )
        val preexistingGame = findGame(session.id)
        if (preexistingGame != null) {
            return@post call.respondError("Can't spectate while in another game")
        }
        val gameId = tryParseUuid(call.parameters["id"]) ?: return@post call.respondError("Invalid game id")
        val game = games[gameId] ?: return@post call.respondError("Game not found", HttpStatusCode.NotFound)
        game.addSpectator(session.user())
        call.respondSuccess(true)
    }

    delete("{id}/spectate") {
        val session = call.sessions.get<UserSession>() ?: return@delete call.respondError(
            "You must be logged in",
            status = HttpStatusCode.Unauthorized
        )
        val gameId = tryParseUuid(call.parameters["id"]) ?: return@delete call.respondError("Invalid game id")
        val game = games[gameId] ?: return@delete call.respondError("Game not found", HttpStatusCode.NotFound)
        game.removeSpectator(session.id)
        call.respondSuccess(true)
    }

    get("search/tokens") {
        val dao = CardDao()
        val name = call.parameters["name"]
        val type = call.parameters["type"]
        val pt = call.parameters["pt"]
        val text = call.parameters["text"]
        val color = call.parameters["colors"]

        val types = mutableListOf<String>()

        if (type != null) {
            val rawTypes = type.split(" - ")
            if (rawTypes.size == 2) {
                types.addAll(rawTypes[1].split(" "))
                types.addAll(rawTypes[0].split(" "))
            }
            else {
                types.addAll(rawTypes[0].split(" "))
            }
        }

        val tokens = dao.searchForTokens(name, color, types, text, pt)
        call.respondSuccess(tokens.map { it.toOracleCard() })
    }

    get("search/cards") {
        val dao = CardDao()
        val name = call.parameters["name"] ?: return@get call.respondError("Must specify a name")
        val cards = dao.searchForCards(name)
        call.respondSuccess(cards.map { it.toOracleCard() })
    }
}