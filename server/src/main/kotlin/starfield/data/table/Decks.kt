package starfield.data.table

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object Decks : UUIDTable("Deck", "DeckId") {
    val ownerId = uuid("OwnerId").references(Users.id)
    val name = varchar("Name", 200)
    val thumbnailId = uuid("ThumbnailId").references(Cards.id).nullable()

}

object DeckCards : Table("DeckCard") {
    val deckId = uuid("DeckId").references(Decks.id, onDelete = ReferenceOption.CASCADE)
    val cardId = uuid("CardId").references(Cards.id)
    val count = byte("Count")
    val startingZone = byte("StartingZone")

    override val primaryKey = PrimaryKey(deckId, cardId, startingZone)
}