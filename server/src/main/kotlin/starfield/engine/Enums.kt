package starfield.engine

import starfield.plugins.Id

typealias CardId = Int
typealias OracleId = Id

enum class CardOrigin {
    DECK, SIDEBOARD, TOKEN
}

enum class PlayerAttribute {
    LIFE, POISON, ACTIVE_PLAYER
}

enum class Pivot {
    UNTAPPED,
    TAPPED,
    LEFT_TAPPED,
    UPSIDE_DOWN,
}