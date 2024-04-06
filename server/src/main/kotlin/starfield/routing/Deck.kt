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
import starfield.data.dao.DeckDao.ConflictResolutionStrategy
import starfield.plugins.UserSession
import starfield.plugins.respondError
import starfield.plugins.respondSuccess
import starfield.plugins.tryParseUuid
import java.util.*
import java.util.regex.Pattern

data class ParseDeckCardResult(
    val count: Int, 
    val card: CardDao.Card?, 
    val name: String,
    val source: String,
    val conflictResolutionStrategy: ConflictResolutionStrategy = ConflictResolutionStrategy.NoConflict)

@Serializable
data class DeckCard(
    val name: String,
    val type: String,
    val image: String,
    val id: Id,
    val backImage: String?,
    val count: Int,
    val source: String,
    val conflictResolutionStrategy: ConflictResolutionStrategy
) {
    private constructor(count: Int, card: CardDao.Card, source: String, conflictResolutionStrategy: ConflictResolutionStrategy)
            : this(card.name, card.type, card.image, card.id, card.backImage, count, source, conflictResolutionStrategy)
    
    private constructor(count: Int, name: String, source: String, conflictResolutionStrategy: ConflictResolutionStrategy)
            : this(name, "", "", UUID.randomUUID(), null, count, source, conflictResolutionStrategy)

    companion object {
        fun new(result: ParseDeckCardResult): DeckCard {
            return if (result.card == null) {
                DeckCard(result.count, result.name, result.source, result.conflictResolutionStrategy)
            } else {
                DeckCard(result.count, result.card, result.source, result.conflictResolutionStrategy)
            }
        }
    }
}

@Serializable
data class Deck(
    val id: Id,
    val owner: Id,
    val name: String,
    val thumbnailImage: String?,
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
    val thumbnailImage: String?
)

fun Route.deckRouting() {

    get("/") {
        val session = call.sessions.get<UserSession>() ?: return@get call.respondError(
            "You must be logged to view decks",
            status = HttpStatusCode.Unauthorized
        )

        val deckDao = DeckDao()
        val myDecks = deckDao.getDecks(session.id)
            .map { DeckListing(it.id, it.name, it.thumbnailImage) }

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
            .mapNotNull { it.card }
            .filter { it.backImage == null && it.image != "" }
            .maxByOrNull { it.name.length }


        val maindeckCards = maindeck.filter { it.card != null }.map { DeckCard.new(it) }
        val sideboardCards = sideboard.filter { it.card != null }.map { DeckCard.new(it) }
        deckDao.updateDeck(deckId, upload.name, thumbnailCard?.id, maindeckCards, sideboardCards)

        call.respondSuccess(Deck(
            deckId,
            deck.owner,
            upload.name,
            thumbnailCard?.thumbnailImage,
            maindeck.map { DeckCard.new(it) },
            sideboard.map { DeckCard.new(it) }
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

suspend fun validateDeck(main: List<String>, side: List<String>): Pair<List<ParseDeckCardResult>, List<ParseDeckCardResult>> {
    val mainCards = lookupCards(main.map(::splitCardLine))
    val sideCards = lookupCards(side.map(::splitCardLine))

    return Pair(mainCards, sideCards)
}

suspend fun lookupCards(cards: List<Triple<Int, String, String>>): List<ParseDeckCardResult> {
    if (cards.isEmpty()) {
        return listOf()
    }
    val dedupedCards = cards
        .groupBy { Pair(it.second, it.third.uppercase()) }
        .entries
        .map { kv -> Triple(kv.value.sumOf { it.first }, kv.key.first, kv.key.second) }
    val dao = CardDao()
    val cardData = dao.tryFindCards(dedupedCards.map { it.second })

    val sourceCounts = cardData.values.flatten().groupingBy { it.source }.eachCount()
    val largestCount = sourceCounts.maxBy { it.value }.value
    val defaultSource = sourceCounts.filterValues { it == largestCount }.keys.min()

    val sources = dao.getSources(sourceCounts.keys)
    val sourcesById = sources.associateBy { it.id }

    return dedupedCards
        .map { card ->
            // TODO: What if a card was created as unique but now has conflicts
            val (count, name, sourceCode) = card
            val normalName = CardDao.normalizeName(name)
            val candidates = cardData[normalName]
                ?: return@map ParseDeckCardResult(count, null, name, sourceCode)
            if (sourceCode != "") {
                val source = sources.find { it.code == sourceCode }
                    ?: return@map ParseDeckCardResult(count, null, name, sourceCode)
                val print = candidates.find { it.source == source.id }
                    ?: return@map ParseDeckCardResult(count, null, name, sourceCode)
                return@map ParseDeckCardResult(count, print, print.name, sourceCode, ConflictResolutionStrategy.Pinned)
            }
            if (candidates.size == 1) {
                return@map ParseDeckCardResult(count, candidates[0], candidates[0].name, sourcesById[candidates[0].source]!!.code, ConflictResolutionStrategy.NoConflict)
            }
            val defaultPrint = candidates.find { it.source == defaultSource }
            if (defaultPrint == null) {
                val bestPrint = candidates.minBy { it.source }
                return@map ParseDeckCardResult(count, bestPrint, bestPrint.name, sourcesById[bestPrint.source]!!.code, ConflictResolutionStrategy.Best)
            } else {
                return@map ParseDeckCardResult(count, defaultPrint, defaultPrint.name, sourcesById[defaultPrint.source]!!.code, ConflictResolutionStrategy.Default)
            }
        }
}

fun splitCardLine(line: String): Triple<Int, String, String> {
    val pattern = Pattern.compile("^(?:(?<count>\\d+) )?(?<name>[\\w ,\\-+'/!&.:?\"()®]*?)(?: \\((?<source>[A-Z]{3,5})\\))?$")
    val matcher = pattern.matcher(line)
    return if (matcher.find()) {
        val count = matcher.group("count")?.toIntOrNull() ?: 1
        val name = matcher.group("name") ?: ""
        val source = matcher.group("source") ?: ""
        Triple(count, name, source)
    }
    else {
        Triple(1, line, "")
    }
}