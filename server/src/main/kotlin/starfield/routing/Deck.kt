package starfield.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import starfield.plugins.Id
import starfield.data.dao.CardDao
import starfield.data.dao.DeckDao
import starfield.plugins.UserSession
import starfield.plugins.respondError
import starfield.plugins.respondSuccess
import starfield.plugins.tryParseUuid
import java.util.*

@Serializable
data class DeckCard(
    val name: String,
    val type: String,
    val image: String,
    val id: Id,
    val backImage: String?,
    val count: Int
)

@Serializable
data class Deck(
    val id: Id,
    val owner: Id,
    val name: String,
    val thumbnailId: Id?,
    val maindeck: List<DeckCard>,
    val sideboard: List<DeckCard>,
)

@Serializable
data class DeckUpload(
    val name: String,
    val sideboard: List<String>,
    val maindeck: List<String>
)

@Serializable
data class DeckListing(
    val id: Id,
    val name: String,
    val thumbnailId: Id?
)

fun Route.deckRouting() {

    get("/") {
        val session = call.sessions.get<UserSession>() ?: return@get call.respondError(
            "You must be logged to view decks",
            status = HttpStatusCode.Unauthorized
        )

        val deckDao = DeckDao()
        val myDecks = deckDao.getDecks(session.id)
            .map { DeckListing(it.id, it.name, it.thumbnailId) }

        call.respondSuccess(myDecks)
    }

    get("/{id}") {
        val uuid = tryParseUuid(call.parameters["id"]) ?: return@get call.respondError("Invalid id")
        val session = call.sessions.get<UserSession>() ?: return@get call.respondError(
            "You must be logged to view decks",
            status = HttpStatusCode.Unauthorized
        )

        val deckDao = DeckDao()
        val deck = deckDao.getDeck(uuid) ?: return@get call.respondError("Deck not found")
        if (deck.owner != session.id) {
            return@get call.respondError("You don't have access to that deck",
            status = HttpStatusCode.Forbidden)
        }

        call.respondSuccess(deck)
    }

    post("/new") {
        val session = call.sessions.get<UserSession>() ?: return@post call.respondError(
            "You must be logged to create a deck",
            status = HttpStatusCode.Unauthorized
        )
        val deckDao = DeckDao()
        val deck = deckDao.createDeck(session.id)
        call.respondSuccess(deck)
    }

    post("/{id}") {
        val session = call.sessions.get<UserSession>() ?: return@post call.respondError(
            "You must be logged to edit decks",
            status = HttpStatusCode.Unauthorized
        )
        val deckId = tryParseUuid(call.parameters["id"]) ?: return@post call.respondError("Invalid deck")

        val deckDao = DeckDao()
        val deck = deckDao.getDeck(deckId) ?: return@post call.respondError("Deck not found")

        if (deck.owner != session.id) {
            return@post call.respondError("This deck does not belong to you",
            status = HttpStatusCode.Forbidden)
        }

        val upload = call.receive<DeckUpload>()
        val (maindeck, sideboard) = validateDeck(upload.maindeck, upload.sideboard)
        val thumbnailCard = maindeck
            .filter { it.backImage == null && it.image != "" }
            .maxByOrNull { it.name.length }
        val updatedDeck = Deck(
            deckId,
            deck.owner,
            upload.name,
            thumbnailCard?.id,
            maindeck.filter { it.image != "" },
            sideboard.filter { it.image != "" }
        )
        deckDao.updateDeck(updatedDeck)

        call.respondSuccess(Deck(
            deckId,
            deck.owner,
            upload.name,
            thumbnailCard?.id,
            maindeck,
            sideboard
        ))
    }

    delete("/{id}") {
        val uuid = tryParseUuid(call.parameters["id"]) ?: return@delete call.respondError("Invalid id")
        val session = call.sessions.get<UserSession>() ?: return@delete call.respondError(
            "You must be logged to delete a deck",
            status = HttpStatusCode.Unauthorized
        )

        val deckDao = DeckDao()
        val deleteCount = deckDao.deleteDeck(uuid, session.id)

        call.respondSuccess(deleteCount == 1)
    }
}

suspend fun validateDeck(main: List<String>, side: List<String>): Pair<List<DeckCard>, List<DeckCard>> {
    val mainCards = lookupCards(main.map(::splitCardLine))
    val sideCards = lookupCards(side.map(::splitCardLine))

    return Pair(mainCards, sideCards)
}

suspend fun lookupCards(cards: List<Pair<Int, String>>): List<DeckCard> {
    val dedupedCards = cards.groupBy { it.second }.entries.map { kv -> Pair(kv.value.sumOf { it.first }, kv.key) }
    val dao = CardDao()
    val cardData = dao.tryFindCards(dedupedCards.map { it.second })

    return dedupedCards.zip(cardData).map {
        val (entry, card) = it
        val (count, name) = entry

        val type = card?.type ?: ""
        val image = card?.image ?: ""
        val fullName = card?.name ?: name
        val id = card?.id ?: UUID.randomUUID()

        DeckCard(
            fullName,
            type,
            image,
            id,
            card?.backImage,
            count
        )
    }
}

fun splitCardLine(line: String): Pair<Int, String> {
    val components = line.split(" ")
    val maybeCount = components[0].toIntOrNull()
    var count = 1
    var name = line
    if (maybeCount != null) {
        count = maybeCount
        name = components.subList(1, components.size).joinToString(" ")
    }

    return Pair(count, name)
}