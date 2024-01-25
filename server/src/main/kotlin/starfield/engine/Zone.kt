package starfield.engine

enum class Zone {
    LIBRARY,
    HAND,
    BATTLEFIELD,
    GRAVEYARD,
    EXILE,
    FACE_DOWN,
    SIDEBOARD;

    val isPublic: Boolean
        get() {
            return this == BATTLEFIELD || this == GRAVEYARD || this == EXILE
        }
}