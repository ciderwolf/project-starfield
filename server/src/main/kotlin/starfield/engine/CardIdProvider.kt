package starfield.engine

import java.util.concurrent.atomic.AtomicInteger

class CardIdProvider {
    private var index = AtomicInteger(1)

    fun getId(zone: Zone, playerIndex: Int): CardId {
        val idx = index.getAndIncrement() shl 8
        return idx or (zone.ordinal shl 4) or playerIndex
    }
}