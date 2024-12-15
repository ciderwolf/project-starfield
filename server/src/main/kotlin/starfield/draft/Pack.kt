package starfield.draft

import starfield.model.DraftCard

data class Pack(val id: Int, val cards: MutableList<DraftCard>) {
    val size: Int
        get() = cards.size

    fun remove(card: DraftCard) {
        cards.remove(card)
    }

    fun isEmpty(): Boolean {
        return cards.isEmpty()
    }
}