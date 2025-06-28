package starfield.data.dao

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import starfield.Config
import starfield.draft.StrategyInfo
import starfield.plugins.Id
import starfield.routing.DeckCard
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Serializable
enum class PackAlgorithm {
    SHEET_SAMPLING, CUBE
}

@Serializable
private data class SetResponse(
    val data: SetData
)

@Serializable
private data class SetData(
    val booster: Map<String, BoosterConfig>,
    val cards: List<SetCard>
)

@Serializable
private data class SetCard(
    val identifiers: CardIdentifiers,
    val uuid: Id
)

@Serializable
private data class CardIdentifiers(
    val scryfallId: Id,
    val scryfallOracleId: Id
)

@Serializable
data class BoosterConfig(
    val boosters: List<BoosterPack>,
    val boostersTotalWeight: Int,
    val name: String?,
    val sheets: Map<String, BoosterSheet>,
    val packType: PackAlgorithm = PackAlgorithm.SHEET_SAMPLING
)

@Serializable
data class BoosterPack(
    val contents: Map<String, Int>,
    val weight: Int
)

@Serializable
data class BoosterSheet(
    val allowDuplicates: Boolean? = null,
    val balanceColors: Boolean? = null,
    var foil: Boolean,
    val cards: Map<Id, Int>,
    val totalWeight: Int
)

class MtgJsonDao {
    private val setUrl = "https://mtgjson.com/api/v5/%s.json"
    private val json = Json { ignoreUnknownKeys = true }

    fun cardListToBoosterInfo(name: String, cards: List<DeckCard>): BoosterConfig {
        val boosterInfo = BoosterConfig(
            boosters = listOf(
                BoosterPack(
                    weight = 1,
                    contents = mapOf("cube" to 15)
                )
            ),
            boostersTotalWeight = 1,
            name = "Cube Booster",
            sheets = mapOf("cube" to BoosterSheet(
                foil = false,
                totalWeight = 1,
                cards = cards.associate { it.id to it.count }
            )),
            packType = PackAlgorithm.CUBE
        )
        return boosterInfo
    }

    suspend fun downloadBoosterInfo(code: String): BoosterConfig {
        val url = setUrl.format(code.uppercase())
        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build()
        val response = withContext(Dispatchers.IO) {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        }
        val setResponse = json.decodeFromString<SetResponse>(response.body())

        val booster = getDefaultBooster(setResponse.data)
        val cardIdMap = setResponse.data.cards.associate { Pair(it.uuid, it.identifiers.scryfallId) }

        val result = booster.copy(sheets = booster.sheets.mapValues { (_, sheet) ->
            val cards = sheet.cards.map { (id, weight) -> Pair(cardIdMap[id], weight) }
                .filter { it.first != null }
                .map { Pair(it.first!!, it.second) }
            sheet.copy(cards = cards.toMap())
        }.filter { it.value.cards.isNotEmpty() })

        return result.copy(boosters = result.boosters.map { pack ->
            val contents = pack.contents.filterKeys { sheetName ->
                result.sheets[sheetName]?.cards?.isNotEmpty() ?: false
            }
            pack.copy(contents = contents)
        })
    }

    private fun getDefaultBooster(set: SetData): BoosterConfig {
        return if ("draft" in set.booster) {
            set.booster["draft"]!!
        } else if ("play" in set.booster) {
            set.booster["play"]!!
        } else {
            set.booster.values.first()
        }
    }
}