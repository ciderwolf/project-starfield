package starfield.data.dao

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import starfield.data.table.Cards
import starfield.data.table.CubeCards
import starfield.data.table.Cubes
import starfield.data.table.Printings
import starfield.plugins.Id
import starfield.plugins.toEnum
import starfield.routing.DeckCard
import starfield.routing.DeckListing
import java.util.*

class CubeDao {

    private fun mapDbCubeListing(row: ResultRow): DeckListing {
        return DeckListing(
            id = row[Cubes.id].value,
            name = row[Cubes.name],
            thumbnailImage = row[Cubes.thumbnailImage]
        )
    }

    suspend fun createCube(
        ownerId: UUID,
        cubeId: String,
        source: CubeSource,
        name: String,
        thumbnailImage: String,
        cards: List<UUID>
    ) = DatabaseSingleton.dbQuery {
        val cube = Cubes.insertAndGetId {
            it[Cubes.name] = name
            it[remoteId] = cubeId
            it[Cubes.thumbnailImage] = thumbnailImage
            it[src] = source.ordinal.toByte()
            it[Cubes.ownerId] = ownerId
        }.value

        val cardCounts = cards.groupingBy { it }.eachCount()

        CubeCards.batchInsert(cardCounts.entries) { (cardId, count) ->
            this[CubeCards.cubeId] = cube
            this[CubeCards.printingId] = cardId
            this[CubeCards.count] = count
        }

        return@dbQuery cube
    }

    suspend fun getCubeListings(userId: UUID) = DatabaseSingleton.dbQuery {
        Cubes.selectAll().where {
            Cubes.ownerId eq userId
        }.map(::mapDbCubeListing)
    }

    suspend fun getCubeListing(userId: UUID, cubeId: UUID) = DatabaseSingleton.dbQuery {
        Cubes.selectAll().where {
            (Cubes.id eq cubeId) and (Cubes.ownerId eq userId)
        }.firstNotNullOfOrNull(::mapDbCubeListing)
    }

    suspend fun getCube(ownerId: UUID, cubeId: UUID) = DatabaseSingleton.dbQuery {
        Cubes.selectAll().where {
            (Cubes.id eq cubeId) and (Cubes.ownerId eq ownerId)
        }.firstNotNullOfOrNull { row ->
            val cards = CubeCards
                .innerJoin(Printings)
                .innerJoin(Cards, { Cards.id }, { Printings.cardId })
                .selectAll()
                .where { CubeCards.cubeId eq cubeId }
                .map { cardRow ->
                    DeckCard(
                        name = cardRow[Cards.name],
                        type = cardRow[Cards.type],
                        image = cardRow[Printings.image],
                        id = cardRow[Printings.id],
                        backImage = cardRow[Printings.backImage],
                        count = cardRow[CubeCards.count], // Assuming each card is counted as 1 for simplicity
                        source = "Cube",
                        conflictResolutionStrategy = DeckDao.ConflictResolutionStrategy.NoConflict
                    )
                }
            Cube(
                id = row[Cubes.id].value,
                name = row[Cubes.name],
                ownerId = row[Cubes.ownerId],
                remoteId = row[Cubes.remoteId],
                source = row[Cubes.src].toEnum<CubeSource>()!!,
                thumbnailImage = row[Cubes.thumbnailImage],
                cards = cards
            )
        }
    }

    suspend fun updateCube(ownerId: UUID, cubeId: UUID, name: String, thumbnailImage: String, cards: List<Id>) = DatabaseSingleton.dbQuery {
        val rowsUpdated = Cubes.update({ (Cubes.id eq cubeId) and (Cubes.ownerId eq ownerId) }) {
            it[Cubes.name] = name
            it[Cubes.thumbnailImage] = thumbnailImage
        }

        if (rowsUpdated == 0) {
            return@dbQuery
        }

        val cardCounts = cards.groupingBy { it }.eachCount()

        CubeCards.deleteWhere { CubeCards.cubeId eq cubeId }
        CubeCards.batchInsert(cardCounts.entries) { (printingId, count) ->
            this[CubeCards.cubeId] = cubeId
            this[CubeCards.printingId] = printingId
            this[CubeCards.count] = count
        }
    }

    suspend fun deleteCube(ownerId: UUID, cubeId: UUID) = DatabaseSingleton.dbQuery {
        getCubeListing(ownerId, cubeId) ?: return@dbQuery false

        CubeCards.deleteWhere { CubeCards.cubeId eq cubeId }
        Cubes.deleteWhere { (Cubes.id eq cubeId) and (Cubes.ownerId eq ownerId) }
        return@dbQuery true
    }

    enum class CubeSource {
        CubeCobra
    }

    @Serializable
    data class Cube(
        val id: Id,
        val name: String,
        val ownerId: Id,
        val remoteId: String?,
        val source: CubeSource,
        val thumbnailImage: String,
        val cards: List<DeckCard>
    )
}