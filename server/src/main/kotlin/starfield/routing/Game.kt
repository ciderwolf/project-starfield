package starfield.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import starfield.Id
import starfield.data.dao.CardDao
import starfield.findGame
import starfield.model.OracleId
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
}