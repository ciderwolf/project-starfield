package starfield.data.table

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object Decks : UUIDTable("Deck", "deck_id") {
    val ownerId = uuid("owner_id").references(Users.id)
    val name = varchar("name", 200)
    val thumbnailId = uuid("thumbnail_id").references(Cards.id).nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    var modifiedAt = datetime("modified_at").defaultExpression(CurrentDateTime)

}

object DeckCards : Table("DeckCard") {
    val deckId = uuid("deck_id").references(Decks.id, onDelete = ReferenceOption.CASCADE)
    val cardId = uuid("card_id").references(Cards.id)
    val count = byte("count")
    val startingZone = byte("starting_zone")
    val conflictResolutionStrategy = byte("conflict_resolution_strategy")

    override val primaryKey = PrimaryKey(deckId, cardId, startingZone)
}