import type { GameListing, LobbyState } from "./message";

export async function createGame(name: string): Promise<LobbyState> {
  const response = await postJson('lobbies', { name });
  return response;
}

export async function joinGame(gameId: string): Promise<LobbyState> {
  const response = await postJson('lobbies/' + gameId + '/join', {});
  return response;
}

export async function getGames(): Promise<GameListing[]> {
  const response = await getJson('lobbies');
  return response;
}

export async function login(name: string) {
  const response = await postJson('login', { name });
  return response;
}

export async function getJson(path: string): Promise<any> {
  const response = await fetch('/api/' + path);
  return (await response.json()).content;
}

export async function postJson(path: string, payload: any): Promise<any> {
  const response = await fetch('/api/' + path,
  {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload),
  });
  return (await response.json()).content;
}