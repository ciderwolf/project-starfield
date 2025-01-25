package starfield.data.dao

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import starfield.Config
import starfield.draft.StrategyInfo
import starfield.plugins.Id
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

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

    fun getDraftStrategy(code: String): StrategyInfo? {
        val setPath = Config.storagePath("sets/$code-strategy.json")
        val setFile = File(setPath)
        if (setFile.exists()) {
            return json.decodeFromString<StrategyInfo>(setFile.readText())
        }
        println("Failed to load strategy for $code")
        return null
    }

    suspend fun getBoosterInfo(code: String): BoosterConfig {
        // check file system
        val setPath = Config.storagePath("sets/$code.json")
        val setFile = File(setPath)
        if (setFile.exists()) {
            val setResponse = json.decodeFromString<BoosterConfig>(File(setPath).readText())
            return setResponse
        } else {
            val boosterInfo = downloadBoosterInfo(code)
            setFile.writeText(json.encodeToString(BoosterConfig.serializer(), boosterInfo))
            return boosterInfo
        }
    }

    private suspend fun downloadBoosterInfo(code: String): BoosterConfig {
        val url = setUrl.format(code)
        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build()
        val response = withContext(Dispatchers.IO) {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        }
        val setResponse = json.decodeFromString<SetResponse>(response.body())

        val booster = getDefaultBooster(setResponse.data)
        val cardIdMap = setResponse.data.cards.associate { Pair(it.uuid, it.identifiers.scryfallOracleId) }

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