import { postJson } from ".";

export function submitDeckChoice(deckId: string) {
  return postJson('lobbies/chooseDeck', { deckId });
}

export function kickPlayer(gameId: string, playerId: string) {
  return postJson('lobbies/' + gameId + '/kick?player=' + playerId, {});
}

export function leaveGame(gameId: string) {
  return postJson('lobbies/' + gameId + '/leave', {});
}

export function endGame(gameId: string) {
  return postJson('lobbies/' + gameId + '/end', {});
}


export function startGame(gameId: string) {
  return postJson('lobbies/' + gameId + '/start', {});
}