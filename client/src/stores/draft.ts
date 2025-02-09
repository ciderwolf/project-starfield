import { type PlayerListing, type DraftCard, type DraftEvent, type DraftState } from "@/api/message";
import { defineStore } from "pinia";
import { reactive, ref } from "vue";
import { useDataStore } from "./data";
import { resetReactive } from ".";

export type PoolCard = { card: DraftCard, count: number, sideboard: boolean };

export const useDraftStore = defineStore('draft', () => {
  const packNumber = ref(0);
  const pickNumber = ref(0);
  const currentPack = ref<DraftCard[]>([]);
  const packQueues = reactive<{ [id: string]: number }>({});
  const pool = ref<PoolCard[]>([]);
  const isEnded = ref(false);
  const deckId = ref<string | null>(null);
  const draftId = ref<string | null>(null);
  const players = ref<PlayerListing[]>([]);

  function setDraftState(state: DraftState) {
    currentPack.value = state.pack;
    draftId.value = state.id;
    players.value = state.players;

    resetReactive(packQueues);
    console.log(Object.keys(packQueues), state.packQueues);
    for (const [id, queue] of Object.entries(state.packQueues)) {
      packQueues[id] = queue;
    }

    pool.value = state.picks;

    pickNumber.value = state.pickNumber;
    packNumber.value = state.packNumber;

    isEnded.value = false;
    deckId.value = null;
  }

  function processEvent(event: DraftEvent) {
    const data = useDataStore();
    switch (event.type) {
      case 'receive_pack':
        addPack(event.pack);
        packNumber.value = event.packNumber;
        pickNumber.value = event.pickNumber;
        break;
      case 'move_card':
        setZoneById(event.cardId, event.toSideboard);
        break;
      case 'pack_queue':
        for (const [id, queue] of Object.entries(event.packs)) {
          packQueues[id] = queue;
        }
        // if we have no packs waiting, clear the current pack
        if (event.packs[data.userId ?? ''] === 0) {
          addPack([]);
        }
        break;
      case 'end_draft':
        isEnded.value = true;
        deckId.value = event.deckId;
        break;
      default:
        const _exhaustiveCheck: never = event;
        console.error(_exhaustiveCheck);
    }
  }

  function addPack(pack: DraftCard[]) {
    currentPack.value = pack;
  }

  function pickCard(card: DraftCard) {
    const preexisting = pool.value.find(c => c.card.id === card.id && c.sideboard === false);
    if (preexisting) {
      preexisting.count++;
    } else {
      pool.value.push({ card, count: 1, sideboard: false });
    }
  }

  function setZoneById(cardId: string, sideboard: boolean) {
    const card = pool.value.find(c => c.card.id === cardId && c.sideboard === !sideboard);
    if (!card) {
      console.error('Card not found');
      return;
    }
    setZone(card, sideboard);
  }

  function setZone(card: PoolCard, sideboard: boolean) {
    const preexisting = pool.value.find(c => c.card.id === card.card.id && c.sideboard === sideboard);
    if (preexisting) {
      preexisting.count++;
    } else {
      pool.value.push({ ...card, count: 1, sideboard });
    }
    if (card.count === 1) {
      pool.value = pool.value.filter(c => c.card.id !== card.card.id || c.sideboard !== !sideboard);
    } else {
      card.count--;
    }
  }

  return { draftId, currentPack, packQueues, pool, packNumber, pickNumber, isEnded, deckId, players, addPack, pickCard, setZone, setDraftState, processEvent };

});