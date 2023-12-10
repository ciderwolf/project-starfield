package starfield

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import starfield.model.*
import starfield.plugins.Location
import java.util.*

@Serializable
data class GameListing(val id: Id, val name: String, val players: List<Id>, val inProgress: Boolean)

@Serializable
sealed class ServerMessage(val type: String)

@Serializable
data class ListingUpdateMessage(val listing: GameListing) : ServerMessage("listing")

@Serializable
data class DeleteListingMessage(val id: Id) : ServerMessage("delete_listing")

@Serializable
data class CreateGameMessage(val name: String, val players: Int)

@Serializable
data class LocationMessage(val location: Location, val id: Id?) : ServerMessage("location")

@Serializable
data class IdentityMessage(val username: String, val id: Id) : ServerMessage("identity")

@Serializable
data class StateMessage<T>(val roomState: T, val room: String): ServerMessage("state")

@Serializable
data class BoardUpdateMessage(val events: List<BoardDiffEvent>) : ServerMessage("board_update")

@Serializable
data class OracleCardInfoMessage(val cards: Map<CardId, OracleId>) : ServerMessage("oracle_cards")

@Serializable
sealed class ClientMessage

@Serializable
@SerialName("draw_card")
data class DrawCardMessage(val count: Int) : ClientMessage()

@Serializable
@SerialName("special_action")
data class SpecialActionMessage(val action: SpecialAction) : ClientMessage()

enum class SpecialAction {
    MULLIGAN, SCOOP, SHUFFLE
}

@Serializable
@SerialName("change_card_attribute")
data class ChangeCardAttributeMessage(
    val card: CardId,
    val attribute: CardAttribute,
    val newValue: Int) : ClientMessage()

@Serializable
@SerialName("change_player_attribute")
data class ChangePlayerAttributeMessage(
    val attribute: PlayerAttribute,
    val newValue: Int) : ClientMessage()


/*
    TODO:
         Look in deck
         =====
             Find cards
             Scry
             Extirpate (find in opponent's deck)
         Cards from outside the game
         =====
             Add tokens
             Clone cards / gain control
             Play from sideboard
         Other
         =====
         ✓ Reveal cards (to only some players)
         Ask for permission
 */

@Serializable
@SerialName("play_card")
data class PlayCardMessage(
    val card: CardId,
    val x: Double,
    val y: Double,
    val attributes: Map<CardAttribute, Int>) : ClientMessage()

@Serializable
@SerialName("change_position")
data class ChangeCardPositionMessage(
    val card: CardId,
    val x: Double,
    val y: Double) : ClientMessage()

@Serializable
@SerialName("change_zone")
data class ChangeCardZoneMessage(
    val card: CardId,
    val zone: Zone,
    val index: Int) : ClientMessage()

@Serializable
@SerialName("change_index")
data class ChangeCardIndexMessage(
    val card: CardId,
    val index: Int) : ClientMessage()

@Serializable
@SerialName("reveal")
data class RevealCardMessage(
    val card: CardId,
    val revealTo: Id?
) : ClientMessage()



