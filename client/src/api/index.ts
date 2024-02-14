import { useAlertsStore } from "@/stores/alerts";
import type { GameListing, LobbyState } from "./message";

interface UserInfo {
  username: string;
  id: string;
}
type JsonResponse<T> = {
  success: true;
  content: T;
} | {
  success: false;
  message: string;
}

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
  return handleApiError(await getJsonRaw<any>(path));
}

export async function postJson(path: string, payload: any): Promise<any> {
  return handleApiError(await postJsonRaw<any>(path, payload));
}

export async function getJsonRaw<T>(path: string): Promise<JsonResponse<T>> {
  const response = await fetch('/api/' + path);
  return response.json();
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
  return handleApiError(await response.json());
}

function handleApiError<T>(response: JsonResponse<T>): T {
  if (!response.success) {
    const alerts = useAlertsStore();
    alerts.addAlert('Encountered an unexpected error', 'Check the console for more information', 'error');
    throw new Error(response.message);
  }
  return response.content;
}