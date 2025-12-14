package starfield.routing

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import starfield.Config
import starfield.data.dao.CardDao
import starfield.data.dao.CardSearchDao
import starfield.plugins.respondError
import starfield.plugins.respondSuccess
import starfield.plugins.tryParseUuid
import starfield.search.SearchException
import starfield.search.SearchParseException
import java.io.File

@Serializable
data class CardDetailsInfo(
    val oracleCard: CardDao.OracleCard,
    val parts: List<CardSearchDao.CardDetail>
)

fun Route.cardRouting() {

    get("/{id}/image") {
        val uuid = tryParseUuid(call.parameters["id"]) ?: return@get call.respondError("Invalid id")
        val type = call.parameters["type"]?.lowercase() ?: return@get call.respondError("Invalid image type")
        if (type !in listOf("front", "back", "thumbnail")) {
            return@get call.respondError("Invalid image type")
        }

        call.respondFile(File(Config.storagePath + "cards/$uuid/$type.jpg"))
    }

    get("/{id}") {
        val uuid = tryParseUuid(call.parameters["id"]) ?: return@get call.respondError("Invalid id")

        val dao = CardDao()
        val card = dao.getCard(uuid) ?: return@get call.respondError("Card not found", HttpStatusCode.NotFound)
        call.respondSuccess(card)
    }

    get("/{id}/parts") {
        val uuid = tryParseUuid(call.parameters["id"])
            ?: return@get call.respondError("Invalid id")

        val dao = CardDao()
        val cardSearchDao = CardSearchDao()
        val card = dao.getCard(uuid)
            ?: return@get call.respondError("Card not found", HttpStatusCode.NotFound)
        val parts = cardSearchDao.getDetails(uuid)
        call.respondSuccess(CardDetailsInfo(card.toOracleCard(), parts))
    }

    get("/{set}/search") {
        val query = call.parameters["q"]
            ?: return@get call.respondError("Missing query parameter 'q'")
        val setCode = call.parameters["set"]?.lowercase()
            ?: return@get call.respondError("Invalid set code")

        try {
            val cardDao = CardDao()
            val searchDao = CardSearchDao()
            val source = cardDao.getSourceByCode(setCode)
                ?: return@get call.respondError("Set not found", HttpStatusCode.NotFound)
            val searchResult = searchDao.search(query, source.id)
            val cardPartsByCardId = searchResult.results.groupBy { it.cardId }
            val cards = cardDao.getCards(cardPartsByCardId.keys).map { it.toOracleCard() }
            // order cards by search result order
            val cardMap = cards.associateBy { it.id }
            val orderedCards = cardPartsByCardId.mapNotNull { CardDetailsInfo(cardMap[it.key]!!, it.value) }
            call.respondSuccess(orderedCards)
        } catch (e: SearchParseException) {
            call.respondError("Search syntax error: ${e.message}", HttpStatusCode.UnprocessableEntity)
        } catch (e: SearchException) {
            call.respondError("Search error: ${e.message}", HttpStatusCode.UnprocessableEntity)
        } catch (e: Exception) {
            call.respondError("Internal error: ${e.message}", HttpStatusCode.InternalServerError)
            e.printStackTrace()
        }
    }
}