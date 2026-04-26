package starfield.routing

import io.ktor.http.*
import io.ktor.server.request.receive
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import starfield.data.dao.CardDao
import starfield.data.dao.CubeCobraDao
import starfield.data.dao.CubeDao
import starfield.data.dao.DraftSetDao
import starfield.data.dao.MtgJsonDao
import starfield.draft.StrategyInfo
import starfield.plugins.UserSession
import starfield.plugins.respondError
import starfield.plugins.respondSuccess
import starfield.plugins.tryParseUuid

fun Route.cubeRouting() {

    get("/") {
        val session = call.sessions.get<UserSession>() ?: return@get call.respondError(
            "You must be logged to view cubes",
            status = HttpStatusCode.Unauthorized
        )

        val cubeDao = CubeDao()
        val cubes = cubeDao.getCubeListings()

        call.respondSuccess(cubes)
    }

    get("/{id}") {
        val uuid = tryParseUuid(call.parameters["id"]) ?: return@get call.respondError("Invalid id")
        val session = call.sessions.get<UserSession>() ?: return@get call.respondError(
            "You must be logged to view cubes",
            status = HttpStatusCode.Unauthorized
        )

        val cubeDao = CubeDao()
        val cube = cubeDao.getCube(uuid)
            ?: return@get call.respondError("Cube not found")

        call.respondSuccess(cube)
    }

    get("/{id}/draft-info") {
        val uuid = tryParseUuid(call.parameters["id"]) ?: return@get call.respondError("Invalid id")
        val session = call.sessions.get<UserSession>() ?: return@get call.respondError(
            "You must be logged to view cubes",
            status = HttpStatusCode.Unauthorized
        )

        val cubeDao = CubeDao()
        cubeDao.getCube(uuid, session.id)
            ?: return@get call.respondError("Cube not found")

        val draftSetDao = DraftSetDao()
        val draftSet = draftSetDao.getDraftSet(uuid)
            ?: return@get call.respondError("Draft info not found")

        call.respondSuccess(draftSet.strategy)
    }

    post("/{id}/draft-info") {
        val uuid = tryParseUuid(call.parameters["id"]) ?: return@post call.respondError("Invalid id")
        val session = call.sessions.get<UserSession>() ?: return@post call.respondError(
            "You must be logged to view cubes",
            status = HttpStatusCode.Unauthorized
        )

        val cubeDao = CubeDao()
        val cube = cubeDao.getCube(uuid, session.id)
            ?: return@post call.respondError("Cube not found")

        val strategyInfo = call.receive<StrategyInfo>()

        val draftSetDao = DraftSetDao()
        val draftSet = draftSetDao.getDraftSet(cube.id)
            ?: return@post call.respondError("Draft info not found")

        draftSetDao.upsertDraftSetFromCube(cube.id, cube.name, draftSet.boosterInfo, strategyInfo)

        call.respondSuccess(cube)
    }

    delete("/{id}") {
        val uuid = tryParseUuid(call.parameters["id"]) ?: return@delete call.respondError("Invalid id")
        val session = call.sessions.get<UserSession>() ?: return@delete call.respondError(
            "You must be logged to view cubes",
            status = HttpStatusCode.Unauthorized
        )

        val cubeDao = CubeDao()

        if (cubeDao.deleteCube(session.id, uuid)) {
            val draftSetDao = DraftSetDao()
            draftSetDao.deleteDraftSet(uuid)
            call.respondSuccess("Cube deleted successfully")
        } else {
            call.respondError("Cube not found", status = HttpStatusCode.NotFound)
        }
    }

    post("/import/cube-cobra") {
        val cubeId = call.parameters["id"] ?: return@post call.respondError("Cube ID is required")

        val session = call.sessions.get<UserSession>() ?: return@post call.respondError(
            "You must be logged to view cubes",
            status = HttpStatusCode.Unauthorized
        )

        val cubeCobraDao = CubeCobraDao()
        val cubeData = cubeCobraDao.getCube(cubeId)
            ?: return@post call.respondError("Cube not found")

        val cubeDao = CubeDao()
        val createdId = cubeDao.createCube(
            session.id,
            cubeData.id,
            CubeDao.CubeSource.CubeCobra,
            cubeData.name,
            cubeData.image.uri,
            cubeData.cards.mainboard.map { it.cardID }
        )
        val createdCube = cubeDao.getCube(createdId, session.id)
        val draftStrategy = createDraftStrategy(cubeData)
        setBoosterInfo(createdCube!!, draftStrategy)
        call.respondSuccess(createdCube)
    }

    put("/sync/cube-cobra") {
        val cubeId = tryParseUuid(call.parameters["id"]) ?: return@put call.respondError("Cube ID is required")
        val session = call.sessions.get<UserSession>() ?: return@put call.respondError(
            "You must be logged to view cubes",
            status = HttpStatusCode.Unauthorized
        )
        val overrideDraftStrategy = call.request.queryParameters["overrideDraftStrategy"] == "true"

        val cubeCobraDao = CubeCobraDao()
        val cubeDao = CubeDao()

        val storedCube = cubeDao.getCube(session.id, cubeId)
            ?: return@put call.respondError("Cube not found")

        if (storedCube.remoteId == null || storedCube.source != CubeDao.CubeSource.CubeCobra) {
            return@put call.respondError("Cube is not from Cube Cobra")
        }

        val cubeData = cubeCobraDao.getCube(storedCube.remoteId)
            ?: return@put call.respondError("Cube not found on Cube Cobra")

        cubeDao.updateCube(
            session.id,
            cubeId,
            cubeData.name,
            cubeData.image.uri,
            cubeData.cards.mainboard.map { it.cardID }
        )

        val dbCubeData = cubeDao.getCube(session.id, cubeId)
        val draftStrategy = if(overrideDraftStrategy) { createDraftStrategy(cubeData) } else { null }
        setBoosterInfo(dbCubeData!!, draftStrategy)
        call.respondSuccess(dbCubeData)
    }
}

suspend fun createDraftStrategy(cube: CubeCobraDao.Cube): StrategyInfo {
    val cardDao = CardDao()
    val cards = cardDao
        .getCardPrintings(cube.cards.mainboard.map { it.cardID })
        .associateBy { it.id }

    val curve = cards.values
        .groupBy { it.manaValue }
        .map { Pair(it.key, 23 * it.value.count() / cards.size) }
        .toMap()

    val sortedElos = cube.cards.mainboard.map { it.details.elo }.sorted()
    // Percentile cutoffs: cards with elo < percentiles[i] map to score 1 + i*0.5 (1..5 in 0.5 steps)
    val percentiles = (1..9).map { i ->
        val pos = i * sortedElos.size / 10.0
        val idx = pos.toInt().coerceAtMost(sortedElos.size - 1)
        sortedElos[idx]
    }

    fun eloToScore(elo: Double): Double {
        var score = 0
        for (cutoff in percentiles) {
            if (elo >= cutoff) score++ else break
        }
        return 1.0 + score * 0.5
    }

    val allColorCombinations = cube.cards.mainboard
        .groupBy { it.details.colorIdentity.sorted().joinToString("") }
        .filter { it.value.count() > 2 }
        .map { it.key }
        .filter { it.count() > 1 }
        .distinct()
    val numColors = allColorCombinations.maxBy { it.length }.length
    val colorCombinations = allColorCombinations.filter { it.length == numColors }

    return StrategyInfo(
        cards = cube.cards.mainboard.map {
            val card = cards[it.cardID] ?: error("Card not found: ${it.cardID}")
            StrategyInfo.BotCard(
                id = card.id,
                type = card.types,
                colors = it.details.colorIdentity,
                cmc = card.manaValue,
                synergyScore = eloToScore(it.details.elo)
            )
        },
        targetCurve = curve,
        archetypeColors = numColors,
        manaFixers = listOf(),
        targetFixing = numColors * 1.5,
        colorCombinations = colorCombinations.map { it.toList().map { c -> c.toString() } }
    )
}

suspend fun setBoosterInfo(cube: CubeDao.Cube, strategyInfo: StrategyInfo?) {
    val setInfoDao = MtgJsonDao()
    val boosterInfo = setInfoDao.cardListToBoosterInfo(cube.id.toString(), cube.cards)
    val draftSetDao = DraftSetDao()
    draftSetDao.upsertDraftSetFromCube(cube.id, cube.name, boosterInfo, strategyInfo)
}