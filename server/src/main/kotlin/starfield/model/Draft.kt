package starfield.model

import starfield.*
import starfield.data.dao.DeckDao
import starfield.draft.*
import starfield.plugins.Id
import starfield.plugins.Location
import starfield.plugins.connections
import java.text.DateFormat
import java.util.*

class Draft(override val id: Id, override val name: String, val set: SetInfo, val players: List<User>, bots: Int) : ActiveUserCollection<DraftState>() {

    private var packId = 0
    private val maxPacks = 3
    private var currentPack = 0
    private var lastActionTime = System.currentTimeMillis()
    private val agents = (players.map { HumanDraftingAgent(it) } + (1..bots).map { RandomDraftingAgent(this) }).shuffled()
    private val pools = agents.map { mutableListOf<DraftCard>() }
    private val packQueue = List(agents.size) { index -> Pair(index, mutableListOf<Pack>()) }.toMap()

    suspend fun start() {
        nextPack()
    }

    suspend fun handleMessage(userId: UUID, message: DraftMessage) {
        lastActionTime = System.currentTimeMillis()
        val player = agents.first { it.id == userId }
        when (message) {
            is PickMessage -> handlePick(player, message.card)
        }
    }

    private suspend fun handlePick(player: DraftingAgent, cardId: Id) {
        println("Player ${player.id} picked card $cardId")

        val playerIndex = agents.indexOf(player)
        println("Player index: $playerIndex")
        val pack = packQueue[playerIndex]?.firstOrNull() ?: return
        println("Pack: ${pack.id} (${pack.size}) cards")
        val card = pack.cards.firstOrNull { it.id == cardId } ?: return
        println("Card: ${card.name}")
        pack.remove(card)
        pools[playerIndex].add(card)
        passPack(playerIndex)
        if (packQueue.all { it.value.isEmpty() }) {
            nextPack()
        } else {
            broadcast(DraftEvent.PackQueueMessage(getPackQueue()))
        }
    }

    private suspend fun nextPack() {
        if (currentPack == maxPacks) {
            endDraft()
            return
        }
        currentPack += 1

        val packs = agents.map { packId += 1; Pack(packId, set.createPack().toMutableList()) }
        packQueue.forEach { (playerIndex, queue) ->
            queue.add(packs[playerIndex])
        }
        resolveQueue()
    }

    private suspend fun passPack(playerIndex: Int) {
        val nextPlayerIndex = (playerIndex + 1) % agents.size
        println("Passing pack: $playerIndex -> $nextPlayerIndex")
        val pack = packQueue[playerIndex]?.removeAt(0) ?: return
        println("Pack ${pack.id} size: ${pack.size}")

        if (pack.isEmpty()) {
            println("Pack ${pack.id} is now empty; don't pass it!")
            return
        }
        packQueue[nextPlayerIndex]?.add(pack)
        println("Player $nextPlayerIndex has ${packQueue[nextPlayerIndex]?.size} packs waiting")

        resolveQueue()
    }

    private suspend fun resolveQueue() {
        do {
            var anyPicked = false
            for ((index, queue) in packQueue) {
                if (queue.isNotEmpty()) {
                    val pack = queue.first()
                    when (val agent = agents[index]) {
                        is HumanDraftingAgent -> {
                            // TODO: don't resend every pack on every queue update
                            agent.notifyOfPack(pack, pickNumber(index), currentPack)
                        }
                        is BotDraftingAgent -> {
                            val picked = agent.makePick(pack)
                            pools[index].add(picked)
                            pack.remove(picked)
                            passPack(index)
                            anyPicked = true
                        }
                    }
                }
            }
        } while (anyPicked)
        broadcast(DraftEvent.PackQueueMessage(getPackQueue()))
    }

    private fun getPackQueue(): Map<Id, Int> {
        return packQueue.map { (index, queue) -> Pair(agents[index].id, queue.size) }.toMap()
    }

    override fun users(): List<User> {
        return players
    }

    override fun currentState(playerId: UUID): DraftState {
        val index = agents.indexOfFirst { it.id == playerId }
        return DraftState(
            id,
            name,
            set.code,
            players.map { UserListing(it.id, it.name) },
            getPackQueue(),
            pools[index],
            packQueue[index]?.firstOrNull()?.cards ?: emptyList(),
            packNumber = currentPack,
            pickNumber = pools[index].size % set.cardsPerPack
        )
    }

    private suspend fun endDraft() {
        val deckDao = DeckDao()
        val todayString = DateFormat.getDateInstance().format(Date())
        val deckName = "${set.code} draft on $todayString"

        for (player in players) {
            val agent = agents.first { it.id == player.id }
            val pool = pools[agents.indexOf(agent)]
            val cardsGroup = pool.groupBy { it.id }.map { it.value.size to it.key }
            val deck = deckDao.createDeck(
                player.id,
                deckName,
                cardsGroup
            )
            player.connection?.send(DraftEventMessage(listOf(DraftEvent.EndDraftMessage(deck))))
        }

        games.remove(id)
        connections.forEach {
            it.send(DeleteListingMessage(id))
        }
    }

    override fun end(user: User): Boolean {
        return players.contains(user)
    }

    override fun lastActionTime() = lastActionTime
    override fun location(): Location {
        return Location.DRAFT
    }

    override fun hasPlayer(userId: UUID): Boolean {
        return players.any { it.id == userId }
    }

    private suspend fun broadcast(vararg event: DraftEvent)  {
        broadcast(DraftEventMessage(event.toList()))
    }

    private fun pickNumber(playerIndex: Int): Int {
        return pools[playerIndex].size % set.cardsPerPack
    }
}

