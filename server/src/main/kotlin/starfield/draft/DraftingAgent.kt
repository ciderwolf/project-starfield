package starfield.draft

import starfield.DraftEventMessage
import starfield.model.Draft
import starfield.model.DraftCard
import starfield.model.User
import starfield.plugins.Id
import java.util.*

sealed class DraftingAgent {

    abstract val id: Id
}

class HumanDraftingAgent(val user: User) : DraftingAgent() {
    override val id: Id = user.id

    suspend fun notifyOfPack(pack: Pack, pickNumber: Int, packNumber: Int) {
        println("Sending pack (${pack.size} cards) to ${user.name}")
        user.connection?.send(DraftEventMessage(listOf(DraftEvent.ReceivePackMessage(pack.cards, pickNumber, packNumber))))
    }
}

abstract class BotDraftingAgent(protected val draft: Draft) : DraftingAgent() {
    override val id: Id = UUID.randomUUID()

    abstract fun makePick(pack: Pack): DraftCard
}

class RandomDraftingAgent(draft: Draft) : BotDraftingAgent(draft) {
    override fun makePick(pack: Pack): DraftCard {
        return pack.cards.random()
    }
}