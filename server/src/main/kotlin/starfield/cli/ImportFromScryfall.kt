package starfield.cli

import com.github.ajalt.clikt.core.CliktCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.batchUpsert
import starfield.cli.scryfall.Card
import starfield.data.dao.CardDao
import starfield.data.dao.DatabaseSingleton
import starfield.data.table.CardExtras
import starfield.data.table.Cards
import starfield.data.table.Printings
import starfield.data.table.Tokens
import starfield.plugins.UUIDListSerializer
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import java.util.stream.Collectors


object ImportFromScryfall : CliktCommand(help = "Import latest cards from Scryfall", name="scryfall-import") {

    private val AllSuperTypes = setOf("Basic", "Legendary", "World", "Snow", "Ongoing")

    override fun run() {
        DatabaseSingleton.init()
        echo("Loading cards from scryfall...")
        runBlocking {
            val result = download()
            echo("$result cards loaded")
        }
    }

    private suspend fun download(): Int {
        val request = HttpRequest.newBuilder(URI("https://api.scryfall.com/bulk-data/oracle-cards")).build()
        val client = HttpClient.newBuilder()
            .build()
//
        val response = withContext(Dispatchers.IO) {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        }
        val data = Json.decodeFromString<Map<String, JsonElement>>(response.body())
        val bulkDataUrl = data["download_uri"]!!.jsonPrimitive.content
        val bulkResponse = withContext(Dispatchers.IO) {
            client.send(
                HttpRequest.newBuilder(URI(bulkDataUrl)).build(),
                HttpResponse.BodyHandlers.ofLines()
            )
        }
        val printings = downloadTokenIdMap()

        val allCardEntities = bulkResponse.body()
            .filter { it != "[" && it != "]" }
            .map { json.decodeFromString<Card>(it.trim(',')) }
            .filter {
                it.setType !in listOf("funny", "memorabilia")
            }
            .map { parseScryfallEntity(it, printings) }
            .collect(Collectors.toList())

        val cards = allCardEntities.filterIsInstance<CardDao.Card>()
        val tokens = allCardEntities.filterIsInstance<CardDao.Token>()

        val cardIds = cards.map { it.id }.toSet()

        DatabaseSingleton.dbQuery {
            Cards.batchUpsert(cards) {
                this[Cards.id] = it.id
                this[Cards.name] = it.name
                this[Cards.fuzzyName] = CardDao.normalizeName(it.name)
                this[Cards.type] = it.type
                this[Cards.manaValue] = it.manaValue
                this[Cards.manaCost] = it.manaCost
                this[Cards.types] = Json.encodeToString(it.types)
                this[Cards.superTypes] = Json.encodeToString(it.superTypes)
                this[Cards.subTypes] = Json.encodeToString(it.subTypes)
                this[Cards.preferredPrintingId] = it.preferredPrinting
            }
        }

        DatabaseSingleton.dbQuery {
            Printings.batchUpsert(printings.values.filter { it.cardId in cardIds }) {
                this[Printings.id] = it.id
                this[Printings.cardId] = it.cardId
                this[Printings.setCode] = it.setCode
                this[Printings.collectorNumber] = it.collectorNumber
                this[Printings.image] = it.image
                this[Printings.backImage] = it.backImage
                this[Printings.thumbnailImage] = it.thumbnailImage
                this[Printings.src] = it.source
            }
        }

        DatabaseSingleton.dbQuery {
            CardExtras.batchUpsert(cards.mapNotNull { it.extras }) {
                this[CardExtras.cardId] = it.id
                this[CardExtras.manaCosts] = Json.encodeToString(it.costs)
                this[CardExtras.tokens] = Json.encodeToString(UUIDListSerializer, it.tokens)
                this[CardExtras.isSideways] = it.isSideways
                this[CardExtras.entersTapped] = it.entersTapped
                this[CardExtras.counters] = it.counters
            }
        }

        DatabaseSingleton.dbQuery {
            Tokens.batchUpsert(tokens) {
                this[Tokens.id] = it.id
                this[Tokens.name] = it.name
                this[Tokens.fuzzyName] = it.fuzzyName
                this[Tokens.colors] = it.colors
                this[Tokens.superTypes] = it.superTypes.joinToString(" ")
                this[Tokens.subTypes] = it.subTypes.joinToString(" ")
                this[Tokens.text] = it.text
                this[Tokens.pt] = it.pt
                this[Tokens.image] = it.image
                this[Tokens.backImage] = it.backImage
                this[Tokens.src] = 0
            }
        }

        return cards.size
    }

    private fun parseScryfallEntity(card: Card, printings: Map<UUID, CardDao.Printing>): CardDao.CardEntity {
        return if (card.setType == "token" || card.typeLine?.contains("Token") == true) {
            parseToken(card)
        } else {
            val parsed = parseCard(card)
            val extras = parseCardExtras(card, parsed.id, printings)
            if (!extras.isEmpty()) {
                parsed.extras = extras
            }
            parsed
        }
    }

    private fun parseToken(card: Card): CardDao.Token {
        val id = card.oracleId
        val name = card.name

        val types = card.typeLine!!.split(" // ")
            .map { it.split(" — ") }
            .map { Pair(it[0].trim().split(" "), it.getOrElse(1) { "" }.trim().split(" ")) }
            .reduce { acc, pair -> Pair(acc.first + pair.first, acc.second + pair.second) }
        val superTypes = types.first.distinct().map(CardDao::normalizeName)
        val subTypes = types.second.distinct().map(CardDao::normalizeName)
        val pt = if (card.power != null && card.toughness != null) {
            card.power + "/" + card.toughness
        } else {
            null
        }
        var text = card.text()
        if (text.length > 1000) {
            text = text.substring(0, 1000)
        }

        val image = card.image { it.normal }
        val backImage = card.backImage { it.normal }

        val colors = card.colorIdentity?.let {
                if (it.isEmpty()) {
                    "C"
                } else {
                    it.distinct().sorted().joinToString("")
                }
            }

        return CardDao.Token(
            id!!,
            name,
            CardDao.normalizeName(name),
            colors!!.lowercase(),
            superTypes,
            subTypes,
            pt?.let(CardDao::normalizeName),
            CardDao.normalizeName(text),
            image,
            backImage,
        )
    }

    private fun parseCard(card: Card): CardDao.Card {
        val name = card.name
        var type = card.typeLine!!
        if (type.contains(" // ")) {
            type = type.split(" // ")[0]
        }
        val typeSides = type.split(" — ").map { it.trim().split(" ").map { t -> t.trim() } }
        val superTypes = typeSides[0].filter { AllSuperTypes.contains(it) }
        val types = typeSides[0].filter { !AllSuperTypes.contains(it) }
        val subTypes = if (typeSides.size > 1) {
            typeSides[1]
        } else {
            emptyList()
        }
        type = types.last()
        val id = card.oracleId
        val preferredPrintingId = card.id

        return CardDao.Card(
            name,
            CardDao.normalizeName(name),
            type,
            card.cmc?.toInt() ?: 0,
            card.manaCost(),
            types,
            superTypes,
            subTypes,
            id!!,
            preferredPrintingId,
            "",
            null,
            "",
            0
        )
    }
}