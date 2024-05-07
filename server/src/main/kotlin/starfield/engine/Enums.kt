package starfield.engine

import kotlinx.serialization.Serializable
import starfield.plugins.Id
import starfield.plugins.PivotSerializer

typealias CardId = Int
typealias OracleId = Id

enum class CardOrigin {
    DECK, SIDEBOARD, TOKEN
}

enum class PlayerAttribute {
    LIFE, POISON, ACTIVE_PLAYER
}

@Serializable(with = PivotSerializer::class)
enum class Pivot {
    UNTAPPED,
    TAPPED,
    LEFT_TAPPED,
    UPSIDE_DOWN,
}

enum class CardAttribute {
    PIVOT,
    COUNTER,
    TRANSFORMED,
    FLIPPED
}