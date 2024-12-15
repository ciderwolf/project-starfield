import { WebSocketConnection } from '@/api/websocket'
import { WebSocketGameClient } from '@/api/game-client';
import { WebSocketDraftClient } from './api/draft-client';

export const ws = new WebSocketConnection();
export const client = new WebSocketGameClient(ws);
export const draftClient = new WebSocketDraftClient(ws);