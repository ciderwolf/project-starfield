package starfield.data.table

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table


object Cards : Table("Card") {

    val id = uuid("card_id")
    val name = varchar("name", 200)
    val fuzzyName = varchar("fuzzy_name", 200).index(isUnique = false)
    val type = varchar("type", 50)
    val preferredPrintingId = uuid("preferred_printing_id").references(Printings.id).nullable()

    override val primaryKey = PrimaryKey(id)
}

object Printings : Table("Printing") {
    val id = uuid("printing_id")
    val cardId = uuid("card_id").references(Cards.id)
    val image = varchar("image", 100)
    val backImage = varchar("back_image", 100).nullable()
    val thumbnailImage = varchar("thumbnail_image", 100)
    val src = integer("source").references(CardSources.id)

    override val primaryKey = PrimaryKey(id)
}

object Tokens : Table("Token") {
    val id = uuid("token_id")
    val name = varchar("name", 100)
    val fuzzyName = varchar("fuzzy_name", 100)
    val colors = varchar("colors", 10)
    val superTypes = varchar("super_types", 100)
    val subTypes = varchar("sub_types", 100)
    val pt = varchar("pt", 10).nullable()
    val text = varchar("text", 1000)
    val image = varchar("image", 100)
    val backImage = varchar("back_image", 100).nullable()
    val src = integer("source").references(CardSources.id)

    override val primaryKey = PrimaryKey(id)
}

object CardSources : IntIdTable("CardSource", "card_source_id") {
    val name = varchar("name", 100)
    val code = varchar("code", 5)
}

object CardExtras : Table("CardExtra") {
    val cardId = uuid("card_id").references(Cards.id)
    val manaCosts = varchar("mana_costs", 1000)
    val tokens = varchar("tokens", 1000)
    val entersTapped = bool("enters_tapped")
    val isSideways = bool("is_sideways")
    val counters = integer("counters").nullable()

    override val primaryKey = PrimaryKey(cardId)
}