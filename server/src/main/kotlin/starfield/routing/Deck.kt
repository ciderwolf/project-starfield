package starfield.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import starfield.Id
import starfield.data.CardDatabase
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
    val name: String,
    val id: Id
)

val decks: MutableMap<Id, Deck> = Collections.synchronizedMap(mutableMapOf())

fun Route.deckRouting() {

    get("/") {
        val session = call.sessions.get<UserSession>() ?: return@get call.respondError(
            "You must be logged to view decks",
            status = HttpStatusCode.Unauthorized
        )

        val myDecks = decks.values.filter { it.owner == session.id }
            .map { DeckListing(it.name, it.id) }

        call.respondSuccess(myDecks)
    }

    get("/{id}") {
        val uuid = tryParseUuid(call.parameters["id"]) ?: return@get call.respondError("Invalid id")
        val session = call.sessions.get<UserSession>() ?: return@get call.respondError(
            "You must be logged to view decks",
            status = HttpStatusCode.Unauthorized
        )

        val deck = decks[uuid] ?: return@get call.respondError("Deck not found")
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
        val deck = Deck(
            UUID.randomUUID(),
            session.id,
            "New Deck",
            listOf(),
            listOf(),
        )
        decks[deck.id] = deck
        call.respondSuccess(deck)
    }

    post("/{id}") {
        val session = call.sessions.get<UserSession>() ?: return@post call.respondError(
            "You must be logged to edit decks",
            status = HttpStatusCode.Unauthorized
        )
        val deckId = tryParseUuid(call.parameters["id"]) ?: return@post call.respondError("Invalid deck")
        val deck = decks[deckId] ?: return@post call.respondError("Deck not found")

        if (deck.owner != session.id) {
            return@post call.respondError("This deck does not belong to you",
            status = HttpStatusCode.Forbidden)
        }

        val upload = call.receive<DeckUpload>()
        val (maindeck, sideboard) = validateDeck(upload.maindeck, upload.sideboard)
        decks[deckId] = Deck(
            deckId,
            deck.owner,
            upload.name,
            maindeck,
            sideboard
        )

        call.respondSuccess(decks[deckId]!!)
    }
}

fun validateDeck(main: List<String>, side: List<String>): Pair<List<DeckCard>, List<DeckCard>> {
    val mainCards = main.map(::splitCardLine).map(::lookupCard)
    val sideCards = side.map(::splitCardLine).map(::lookupCard)

    return Pair(mainCards, sideCards)
}

fun lookupCard(card: Pair<Int, String>): DeckCard {
    val (count, name) = card

    val normalizedName = CardDatabase.normalizeName(name)
    val cardData = CardDatabase.instance()[normalizedName]

    val type = cardData?.type ?: ""
    val image = cardData?.image ?: ""
    val fullName = cardData?.name ?: name
    val id = cardData?.id ?: UUID.randomUUID()

    return DeckCard(
        fullName,
        type,
        image,
        id,
        cardData?.backImage,
        count
    )
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