package starfield.engine

import kotlinx.serialization.Serializable
import starfield.plugins.Id
import starfield.data.dao.CardDao
import java.util.*

@Serializable
data class CardState(
    val id: CardId,
    val x: Double,
    val y: Double,
    val index: Int,
    val pivot: Pivot,
    val counter: Int,
    val transformed: Boolean,
    val flipped: Boolean,
    val zone: Zone
)

class BoardCard(val card: CardDao.CardEntity,
                private val idProvider: CardIdProvider,
                private val playerIndex: Int,
                val origin: CardOrigin) {
    var virtualId: UUID? = null
    val visibility = mutableSetOf<Id>()
    var x = 0.0
    var y = 0.0
    var pivot = Pivot.UNTAPPED
    var counter = 0
    var transformed = false
    var flipped = false
    var id: CardId = idProvider.getId(Zone.LIBRARY, playerIndex)
    var zone = Zone.LIBRARY
        set(value) {
            id = idProvider.getId(value, playerIndex)
            field = value
        }

    fun reset(clearVisibility: Boolean = true) {
        x = 0.0
        y = 0.0
        pivot = Pivot.UNTAPPED
        counter = 0
        if (clearVisibility) {
            visibility.clear()
        }
        id = idProvider.getId(Zone.LIBRARY, playerIndex)
        transformed = false
        flipped = false
        zone = Zone.LIBRARY
        virtualId = null
    }

    fun getState(index: Int): CardState {
        return CardState(id, x, y, index, pivot, counter, transformed, flipped, zone)
    }

    fun clone(origin: CardOrigin): BoardCard {
        val card = BoardCard(this.card, idProvider, playerIndex, origin)
        card.visibility.addAll(visibility)
        card.x = x
        card.y = y
        card.pivot = pivot
        card.counter = counter
        card.transformed = transformed
        card.flipped = flipped
        card.zone = zone
        return card
    }
}
