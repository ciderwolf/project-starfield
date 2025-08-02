package starfield.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import starfield.UserListing
import starfield.data.dao.CardDao
import starfield.engine.OracleId
import starfield.plugins.Id

@Serializable
sealed class ActiveCollectionState

@Serializable
@SerialName("draft_state")
data class DraftState(
    val id: Id,
    val name: String,
    val set: String,
    val players: List<UserListing>,
    val packQueues: Map<Id, Int>,
    val picks: List<PoolCard>,
    val pack: List<DraftCard>,
    val pickNumber: Int,
    val packNumber: Int) : ActiveCollectionState()

@Serializable
data class DraftCard(
    val name: String,
    val id: Id,
    val oracleId: Id,
    val foil: Boolean,
    val type: String,
    val manaValue: Int,
    val manaCost: String,
    val types: List<String>,
    val superTypes: List<String>,
    val subTypes: List<String>,
    val image: String,
    val backImage: String?)

@Serializable
data class PoolCard(val card: DraftCard, var count: Int, val sideboard: Boolean)

@Serializable
@SerialName("game_state")
data class GameState(
    val id: Id,
    val name: String,
    val players: List<PlayerState>,
    val spectators: List<UserState>,
    val oracleInfo: Map<OracleId, CardDao.OracleCard>,
    val currentPlayer: Int) : ActiveCollectionState()


@Serializable
sealed class LobbyState

@Serializable
@SerialName("game_lobby")
data class GameLobbyState(
    val id: Id,
    val name: String,
    val users: List<UserListing>,
    val decks: List<Id?>,
) : LobbyState()

@Serializable
@SerialName("draft_lobby")
data class DraftLobbyState(
    val id: Id,
    val name: String,
    val users: List<UserListing>,
    val bots: Int,
) : LobbyState()
