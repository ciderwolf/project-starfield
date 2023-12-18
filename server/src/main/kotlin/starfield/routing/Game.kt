package starfield.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import starfield.Id
import starfield.data.CardDatabase
import starfield.findGame
import starfield.model.OracleId
import starfield.plugins.UserSession
import starfield.plugins.respondError
import starfield.plugins.respondSuccess

@Serializable
data class VirtualIdsMessage(
    val virtualIds: Map<Id, OracleId>,
    val oracleInfo: Map<OracleId, CardDatabase.OracleCard>
)

fun Route.engineRouting() {
    get("virtual-ids") {
        val session = call.sessions.get<UserSession>() ?: return@get call.respondError(
            "You must be logged in to end a game",
            status = HttpStatusCode.Unauthorized
        )

        val game = findGame(session.id) ?: return@get call.respondError("Not in a game")
        val ids = game.getVirtualIds(session.id)
        val db = CardDatabase.instance()
        val oracleInfo = ids.values.associateWith { db[it]!!.toOracleCard() }

        call.respondSuccess(VirtualIdsMessage(ids, oracleInfo))
    }
}