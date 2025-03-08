package starfield.cli

import com.github.ajalt.clikt.core.CliktCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.batchUpsert
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
            .map { Json.decodeFromString<JsonObject>(it.trim(',')) }
            .filter {
                it["set_type"]!!.jsonPrimitive.content !in listOf("funny", "memorabilia")
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
                this[Cards.preferredPrintingId] = it.preferredPrinting
            }
        }

        DatabaseSingleton.dbQuery {
            Printings.batchUpsert(printings.values.filter { it.cardId in cardIds }) {
                this[Printings.id] = it.id
                this[Printings.cardId] = it.cardId
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
            }
        }

        return cards.size
    }

    private fun parseScryfallEntity(card: JsonObject, printings: Map<UUID, CardDao.Printing>): CardDao.CardEntity {
        return if (card["set_type"]!!.jsonPrimitive.content == "token" || card["type_line"]!!.jsonPrimitive.content.contains("Token")) {
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

    private fun parseToken(card: JsonObject): CardDao.Token {
        val id = UUID.fromString(card["oracle_id"]!!.jsonPrimitive.content)
        val name = card["name"]!!.jsonPrimitive.content

        val types = card["type_line"]!!.jsonPrimitive.content.split(" // ")
            .map { it.split(" — ") }
            .map { Pair(it[0].trim().split(" "), it.getOrElse(1) { "" }.trim().split(" ")) }
            .reduce { acc, pair -> Pair(acc.first + pair.first, acc.second + pair.second) }
        val superTypes = types.first.distinct().map(CardDao::normalizeName)
        val subTypes = types.second.distinct().map(CardDao::normalizeName)
        val pt = if ("power" in card && "toughness" in card) {
            card["power"]!!.jsonPrimitive.content + "/" + card["toughness"]!!.jsonPrimitive.content
        } else {
            null
        }
        var text = if ("oracle_text" in card) {
            card["oracle_text"]!!.jsonPrimitive.content
        } else {
            val faces =card["card_faces"]!!.jsonArray
            faces[0].jsonObject["oracle_text"]!!.jsonPrimitive.content +
                    " // " +
                    faces[1].jsonObject["oracle_text"]!!.jsonPrimitive.content
        }
        if (text.length > 1000) {
            text = text.substring(0, 1000)
        }

        val image = if ("image_uris" !in card) {
            card["card_faces"]!!
                .jsonArray[0]
                .jsonObject["image_uris"]!!
                .jsonObject["normal"]!!
                .jsonPrimitive.content
        } else {
            card["image_uris"]!!
                .jsonObject["normal"]!!
                .jsonPrimitive.content
        }

        val backImage = if ("image_uris" !in card) {
            card["card_faces"]!!
                .jsonArray[1]
                .jsonObject["image_uris"]!!
                .jsonObject["normal"]!!
                .jsonPrimitive.content
        } else {
            null
        }

        val colors = card["color_identity"]!!
            .jsonArray.let {
                if (it.size == 0) {
                    "C"
                } else {
                    it.map { c -> c.jsonPrimitive.content }
                        .distinct().sorted().joinToString("")
                }
            }

        return CardDao.Token(
            id,
            name,
            CardDao.normalizeName(name),
            colors.lowercase(),
            superTypes,
            subTypes,
            pt?.let(CardDao::normalizeName),
            CardDao.normalizeName(text),
            image,
            backImage,
        )
    }

    private fun parseCard(card: JsonObject): CardDao.Card {
        val name = card["name"]!!.jsonPrimitive.content

        val type = card["type_line"]!!.jsonPrimitive.content.split(" — ")[0].split(" ").last().trim()


        val id = UUID.fromString(card["oracle_id"]!!.jsonPrimitive.content)
        val preferredPrintingId = UUID.fromString(card["id"]!!.jsonPrimitive.content)

        return CardDao.Card(
            name,
            CardDao.normalizeName(name),
            type,
            id,
            preferredPrintingId,
            "",
            null,
            "",
            0
        )
    }
}