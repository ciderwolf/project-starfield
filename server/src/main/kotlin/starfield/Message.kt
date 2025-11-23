package starfield

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import starfield.data.dao.CardDao
import starfield.draft.DraftEvent
import starfield.engine.*
import starfield.plugins.Id
import starfield.plugins.Location

@Serializable
data class UserListing(val id: Id, val name: String)

@Serializable
data class GameListing(val id: Id, val type: Location, val name: String, val players: List<UserListing>, val inProgress: Boolean)

@Serializable
sealed class ServerMessage(val type: String)

@Serializable
data class ListingUpdateMessage(val listing: GameListing) : ServerMessage("listing")

@Serializable
data class DeleteListingMessage(val id: Id) : ServerMessage("delete_listing")

@Serializable
data class LocationMessage(val location: Location, val id: Id?) : ServerMessage("location")

@Serializable
data class IdentityMessage(val username: String, val id: Id) : ServerMessage("identity")

@Serializable
data class StateMessage<T>(val roomState: T, val room: Location): ServerMessage("state")

@Serializable
data class BoardUpdateMessage(val events: List<BoardDiffEvent>) : ServerMessage("board_update")

@Serializable
data class OracleCardInfoMessage(val cards: Map<CardId, OracleId>, val oracleInfo: Map<OracleId, CardDao.OracleCard>, val cardsToHide: List<CardId>) : ServerMessage("oracle_cards")

@Serializable
data class GameLogMessage<T : LogInfoMessage>(val owner: Id, val message: T) : ServerMessage("log")

@Serializable
sealed class LogInfoMessage()

@Serializable
@SerialName("find_card")
class FindCardLogMessage() : LogInfoMessage()
@Serializable
@SerialName("sideboard")
class SideboardLogMessage() : LogInfoMessage()
@Serializable
@SerialName("scry")
data class ScryLogMessage(val count: Int) : LogInfoMessage()
@Serializable
@SerialName("reveal")
data class RevealLogMessage(val card: CardId, val revealTo: Id?) : LogInfoMessage()
@Serializable
@SerialName("roll_die")
data class RollDieLogMessage(val sides: Int, val result: Int) : LogInfoMessage()


@Serializable
data class DraftEventMessage(val events: List<DraftEvent>) : ServerMessage("draft_event")

@Serializable
sealed class ClientMessage


@Serializable
sealed class DraftMessage : ClientMessage()

@Serializable
@SerialName("pick")
data class PickMessage(val card: Id) : DraftMessage()

@Serializable
@SerialName("move_zone")
data class MoveDraftZoneMessage(val card: Id, val sideboard: Boolean) : DraftMessage()


@Serializable
sealed class GameMessage : ClientMessage()

@Serializable
@SerialName("draw_card")
data class DrawCardMessage(val count: Int, val to: Zone, val fromBottom: Boolean) : GameMessage()

@Serializable
@SerialName("special_action")
data class SpecialActionMessage(val action: SpecialAction) : GameMessage()

enum class SpecialAction {
    MULLIGAN, SCOOP, SHUFFLE, UNTAP_ALL, END_TURN
}

@Serializable
@SerialName("change_card_attribute")
data class ChangeCardAttributeMessage(
    val card: CardId,
    val attribute: CardAttribute) : GameMessage()

@Serializable
@SerialName("change_player_attribute")
data class ChangePlayerAttributeMessage(
    val attribute: PlayerAttribute,
    val newValue: Int) : GameMessage()

@Serializable
@SerialName("play_card")
data class PlayCardMessage(
    val card: CardId,
    val x: Double,
    val y: Double,
    val attributes: List<CardAttribute>) : GameMessage()

@Serializable
@SerialName("change_position")
data class ChangeCardPositionMessage(
    val card: CardId,
    val x: Double,
    val y: Double) : GameMessage()

@Serializable
@SerialName("change_zone")
data class ChangeCardZoneMessage(
    val card: CardId,
    val zone: Zone,
    val index: Int) : GameMessage()

@Serializable
@SerialName("change_zones")
data class ChangeCardZonesMessage(
    val cards: List<CardId>,
    val zone: Zone,
    val index: Int) : GameMessage()

@Serializable
@SerialName("change_index")
data class ChangeCardIndexMessage(
    val card: CardId,
    val index: Int) : GameMessage()

@Serializable
@SerialName("reveal")
data class RevealCardMessage(
    val card: CardId,
    val revealTo: Id?,
    val reveal: Boolean
) : GameMessage()

@Serializable
@SerialName("scry")
data class ScryMessage(
    val count: Int
) : GameMessage()

@Serializable
@SerialName("create_card")
data class CreateCardMessage(
    val id: OracleId
) : GameMessage()

@Serializable
@SerialName("create_clone")
data class CreateCloneMessage(
    val id: CardId,
    val attributes: List<CardAttribute>
) : GameMessage()

@Serializable
@SerialName("clone_card")
data class CloneCardMessage(
    val id: CardId
) : GameMessage()

@Serializable
@SerialName("create_token")
data class CreateTokenMessage(
    val id: OracleId
) : GameMessage()

@Serializable
@SerialName("move_virtual")
data class MoveCardVirtualMessage(
    val ids: List<Id>,
    val zone: Zone,
    val index: Int
) : GameMessage()

@Serializable
@SerialName("sideboard")
data class SideboardMessage(
    val main: List<Id>,
    val side: List<Id>
) : GameMessage()

