package starfield.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import starfield.plugins.Id
import starfield.data.dao.CardDao
import starfield.findGame
import starfield.engine.OracleId
import starfield.plugins.UserSession
import starfield.plugins.respondError
import starfield.plugins.respondSuccess

@Serializable
data class VirtualIdsMessage(
    val virtualIds: Map<Id, OracleId>,
    val oracleInfo: Map<OracleId, CardDao.OracleCard>
)

fun Route.engineRouting() {
    get("virtual-ids/library") {
        val session = call.sessions.get<UserSession>() ?: return@get call.respondError(
            "You must be logged in",
            status = HttpStatusCode.Unauthorized
        )

        val game = findGame(session.id) ?: return@get call.respondError("Not in a game")
        val ids = game.getVirtualIds(session.id)
        val oracleInfo = CardDao().getCards(ids.values).associate {
            val card = it.toOracleCard()
            Pair(card.id, card)
        }

        call.respondSuccess(VirtualIdsMessage(ids, oracleInfo))
    }

    get("search/tokens") {
        val dao = CardDao()
        val name = call.parameters["name"]
        val type = call.parameters["type"]
        val pt = call.parameters["pt"]
        val text = call.parameters["text"]
        val color = call.parameters["colors"]

        val subTypes = mutableListOf<String>()
        val superTypes = mutableListOf<String>()

        if (type != null) {
            val types = type.split(" - ")
            subTypes.addAll(types.getOrElse(1) { "" }.split(" "))
            superTypes.addAll(types[0].split(" "))
        }

        val tokens = dao.searchForTokens(name, color, superTypes, subTypes, text, pt)
        call.respondSuccess(tokens.map { it.toOracleCard() })
    }
}