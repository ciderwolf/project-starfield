package starfield.draft

import starfield.model.DraftCard
import starfield.model.PoolCard
import starfield.plugins.Id

class DraftPool {
    private val main = mutableMapOf<Id, PoolCard>()
    private val side = mutableMapOf<Id, PoolCard>()

    val mainCards: List<PoolCard>
        get() = main.values.toList()

    val sideboardCards: List<PoolCard>
        get() = side.values.toList()

    val size: Int
        get() = main.size + side.size


    fun pickCard(card: DraftCard, intoSideboard: Boolean = false) {
        val location = if(intoSideboard) side else main
        val poolCard = location[card.id]
        if(poolCard == null) {
            location[card.id] = PoolCard(card, 1, intoSideboard)
        } else {
            poolCard.count += 1
        }
    }

    fun moveCard(cardId: Id, toSideboard: Boolean) {
        val currentCardLocation = if(toSideboard) main else side
        val currentCard = currentCardLocation[cardId]
        if(currentCard == null) {
            println("Card not found in pool, card=${cardId}, sideboard=$toSideboard")
        } else {
            currentCard.count -= 1
            if (currentCard.count == 0) {
                currentCardLocation.remove(cardId)
            }
            pickCard(currentCard.card, toSideboard)
        }
    }


}