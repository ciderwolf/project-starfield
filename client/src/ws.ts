import { WebSocketConnection } from '@/api/websocket'
import { WebSocketGameClient } from './api/game-client';

export const ws = new WebSocketConnection();
export const client = new WebSocketGameClient(ws);