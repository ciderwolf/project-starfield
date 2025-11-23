package starfield.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class CardAttribute {
    @Serializable
    @SerialName("pivot")
    data class CardPivot(val pivot: Pivot) : CardAttribute()

    @Serializable
    @SerialName("counter")
    data class Counter(val counter: Int) : CardAttribute()

    @Serializable
    @SerialName("transformed")
    data class Transformed(val transformed: Boolean) : CardAttribute()

    @Serializable
    @SerialName("flipped")
    data class Flipped(val flipped: Boolean) : CardAttribute()
}