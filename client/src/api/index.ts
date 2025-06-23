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
  code: number;
}

export async function createGame(name: string, players: number = 2): Promise<LobbyState> {
  const response = await postJson('lobbies?kind=game', { name, players });
  return response;
}

export async function createDraft(name: string, players: number, set: string, bots: number): Promise<LobbyState> {
  const response = await postJson('lobbies?kind=draft', { name, players, set, bots });
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
  return handleApiError(await deleteJsonRaw(path));
}

export async function deleteJsonRaw(path: string): Promise<JsonResponse<any>> {
  const response = await fetch('/api/' + path,
    {
      method: 'DELETE',
    });
  return response.json();

}

export async function putJson(path: string): Promise<any> {
  return handleApiError(await putJsonRaw(path));
}

export async function putJsonRaw(path: string): Promise<JsonResponse<any>> {
  const response = await fetch('/api/' + path,
    {
      method: 'PUT',
    });
  return response.json();

}

function handleApiError<T>(response: JsonResponse<T>): T {
  if (!response.success) {
    const alerts = useAlertsStore();
    if (response.code === 422) {
      alerts.addAlert('Encountered an error', response.message, 'error');
    } else {
      alerts.addAlert('Encountered an unexpected error', 'Check the console for more information', 'error');
    }
    throw new Error(response.message);
  }
  return response.content;
}