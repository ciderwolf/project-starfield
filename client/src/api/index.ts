import type { GameListing, LobbyState } from "./message";

export async function createGame(name: string): Promise<LobbyState> {
  const response = await postJson('lobbies', { name, players: 2 });
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

interface UserInfo {
  username: string;
  id: string;
}

export async function login(name: string, password: string): Promise<JsonResponse<UserInfo>> {
  return postJsonRaw<UserInfo>('login', { name, password });
}

export async function createAccount(name: string, password: string): Promise<JsonResponse<UserInfo>> {
  return postJsonRaw<UserInfo>('create-account', { name, password });
}

export async function authenticate(): Promise<boolean> {
  const response = await getJson('authenticate');
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

type JsonResponse<T> = {
  success: true;
  content: T;
} | {
  success: false;
  message: string;
}

export async function postJsonRaw<T>(path: string, payload: any): Promise<JsonResponse<T>> {
  const response = await fetch('/api/' + path,
  {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload),
  });
  return response.json();
}

export async function deleteJson(path: string): Promise<any> {
  const response = await fetch('/api/' + path,
  {
    method: 'DELETE',
  });
  return (await response.json()).content;
}