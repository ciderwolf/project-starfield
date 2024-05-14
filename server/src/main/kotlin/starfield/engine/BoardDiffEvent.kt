package starfield.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import starfield.plugins.Id
import starfield.model.*


@Serializable
sealed class BoardDiffEvent {
    @Serializable
    @SerialName("change_zone")
    data class ChangeZone(val card: CardId, val newZone: Zone, val oldCardId: CardId) : BoardDiffEvent()
    @Serializable
    @SerialName("change_index")
    data class ChangeIndex(val card: CardId, val newIndex: Int) : BoardDiffEvent()
    @Serializable
    @SerialName("change_position")
    data class ChangePosition(val card: CardId, val x: Double, val y: Double) : BoardDiffEvent()

    @Serializable
    @SerialName("reveal_card")
    data class RevealCard(val card: CardId, val players: List<Id>) : BoardDiffEvent()
    @Serializable
    @SerialName("hide_card")
    data class HideCard(val card: CardId, val players: List<Id>) : BoardDiffEvent()
    @Serializable
    @SerialName("change_attribute")
    data class ChangeAttribute(val card: CardId, val attribute: CardAttribute, val newValue: Int) : BoardDiffEvent()
    @Serializable
    @SerialName("change_player_attribute")
    data class ChangePlayerAttribute(val player: Id, val attribute: PlayerAttribute, val newValue: Int) : BoardDiffEvent()

    @Serializable
    @SerialName("shuffle_deck")
    data class ShuffleDeck(val playerId: Id, val newIds: List<CardId>) : BoardDiffEvent()

    @Serializable
    @SerialName("scoop_deck")
    data class ScoopDeck(val playerId: Id, val newIds: List<CardId>) : BoardDiffEvent()

    @Serializable
    @SerialName("create_card")
    data class CreateCard(val state: CardState) : BoardDiffEvent()

    @Serializable
    @SerialName("destroy_card")
    data class DestroyCard(val card: CardId) : BoardDiffEvent()

    @Serializable
    @SerialName("spectator_join")
    data class SpectatorJoin(val user: UserState) : BoardDiffEvent()

    @Serializable
    @SerialName("spectator_leave")
    data class SpectatorLeave(val user: Id) : BoardDiffEvent()
}