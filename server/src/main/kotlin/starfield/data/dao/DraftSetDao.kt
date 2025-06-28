package starfield.data.dao

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.upsert
import starfield.data.table.DraftSets
import starfield.draft.StrategyInfo
import starfield.plugins.Id
import starfield.plugins.toEnum
import java.util.UUID

class DraftSetDao {

    @Serializable
    enum class DraftSetType {
        SCRYFALL,
        CUBE
    }

    data class DraftSet(
        val id: Id,
        val name: String,
        val setType: DraftSetType,
        val image: String?,
        val boosterInfo: BoosterConfig,
        val strategy: StrategyInfo?
    )

    @Serializable
    data class DraftSetListing(
        val id: Id,
        val name: String,
        val setType: DraftSetType,
        val image: String?,
    )

    suspend fun getDraftSet(id: UUID) = DatabaseSingleton.dbQuery {
        DraftSets.selectAll()
            .where { DraftSets.id eq id }
            .map { row ->
                DraftSet(
                    id = row[DraftSets.id].value,
                    name = row[DraftSets.name],
                    image = row[DraftSets.image],
                    setType = row[DraftSets.setType].toEnum<DraftSetType>()!!,
                    boosterInfo = row[DraftSets.boosterInfo].let { Json.decodeFromString<BoosterConfig>(it) },
                    strategy = row[DraftSets.strategy]?.let { Json.decodeFromString<StrategyInfo>(it) }
                )
            }.firstOrNull()
    }

    suspend fun getDraftSetListingByName(name: String) = DatabaseSingleton.dbQuery {
        DraftSets.selectAll()
            .where { DraftSets.name eq name }
            .map { row ->
                DraftSetListing(
                    id = row[DraftSets.id].value,
                    name = row[DraftSets.name],
                    image = row[DraftSets.image],
                    setType = row[DraftSets.setType].toEnum<DraftSetType>()!!,
                )
            }.firstOrNull()
    }

    suspend fun createDraftSetFromScryfall(name: String, setCode: String, boosterInfo: BoosterConfig) = DatabaseSingleton.dbQuery {
        val preexisting = getDraftSetListingByName(name)
        if (preexisting != null) {
            return@dbQuery preexisting
        }
        val id = UUID.randomUUID()
        val imageUrl = "https://svgs.scryfall.io/sets/${setCode.lowercase()}.svg"

        DraftSets.insert {
            it[DraftSets.id] = id
            it[DraftSets.name] = name
            it[DraftSets.setType] = DraftSetType.SCRYFALL.ordinal
            it[DraftSets.image] = imageUrl
            it[DraftSets.boosterInfo] = Json.encodeToString(boosterInfo)
        }
        return@dbQuery DraftSetListing(
            id = id,
            name = name,
            setType = DraftSetType.SCRYFALL,
            image = imageUrl
        )
    }

    suspend fun upsertDraftSetFromCube(id: UUID, name: String, boosterInfo: BoosterConfig) = DatabaseSingleton.dbQuery {
        DraftSets.upsert {
            it[DraftSets.id] = id
            it[DraftSets.name] = name
            it[DraftSets.setType] = DraftSetType.CUBE.ordinal
            it[DraftSets.image] = "" // No image for cubes
            it[DraftSets.boosterInfo] = Json.encodeToString(boosterInfo)
        }
    }

    suspend fun getDraftSetListings() = DatabaseSingleton.dbQuery {
        DraftSets.select(DraftSets.id, DraftSets.name, DraftSets.setType, DraftSets.image)
            .map {
                DraftSetListing(
                    id = it[DraftSets.id].value,
                    name = it[DraftSets.name],
                    setType = it[DraftSets.setType].toEnum<DraftSetType>()!!,
                    image = it[DraftSets.image]
                )
            }
    }

    suspend fun deleteDraftSet(cubeId: UUID) = DatabaseSingleton.dbQuery {
        DraftSets.deleteWhere { id eq cubeId }
    }
}