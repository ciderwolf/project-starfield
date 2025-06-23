package starfield.routing

import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import starfield.data.dao.CubeCobraDao
import starfield.data.dao.CubeDao
import starfield.data.dao.MtgJsonDao
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
        val myCubes = cubeDao.getCubeListings(session.id)

        call.respondSuccess(myCubes)
    }

    get("/{id}") {
        val uuid = tryParseUuid(call.parameters["id"]) ?: return@get call.respondError("Invalid id")
        val session = call.sessions.get<UserSession>() ?: return@get call.respondError(
            "You must be logged to view cubes",
            status = HttpStatusCode.Unauthorized
        )

        val cubeDao = CubeDao()
        val cube = cubeDao.getCube(session.id, uuid) ?: return@get call.respondError("Cube not found")

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
        val createdCube = cubeDao.getCube(session.id, createdId)
        setBoosterInfo(createdCube!!)
        call.respondSuccess(createdCube)
    }

    put("/sync/cube-cobra") {
        val cubeId = tryParseUuid(call.parameters["id"]) ?: return@put call.respondError("Cube ID is required")
        val session = call.sessions.get<UserSession>() ?: return@put call.respondError(
            "You must be logged to view cubes",
            status = HttpStatusCode.Unauthorized
        )

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
        setBoosterInfo(dbCubeData!!)
        call.respondSuccess(dbCubeData)
    }
}

fun setBoosterInfo(cube: CubeDao.Cube) {
    val setInfoDao = MtgJsonDao()
    setInfoDao.cardListToBoosterInfo(cube.id.toString(), cube.cards)
}