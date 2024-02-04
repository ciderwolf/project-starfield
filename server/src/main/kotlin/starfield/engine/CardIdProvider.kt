package starfield.engine

import starfield.plugins.toEnum
import java.util.concurrent.atomic.AtomicInteger

class CardIdProvider {
    private var index = AtomicInteger(1)

    fun getId(zone: Zone, playerIndex: Int): CardId {
        val idx = index.getAndIncrement() shl 8
        return idx or (zone.ordinal shl 4) or playerIndex
    }

    companion object {
        fun getPlayerIndex(id: Int): Int {
            return id and 0xF
        }

        fun getZone(id: Int): Zone {
            val zoneIndex = (id shr 4) and 0xF
            return zoneIndex.toEnum<Zone>()!!
        }
    }
}