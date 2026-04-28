import { Client, type IFrame, type IMessage, type StompSubscription } from '@stomp/stompjs';
import type { ConversationChangedEvent, ConversationMessage } from '@/shared/api/admin-core';

type ConnectionState = 'connecting' | 'connected' | 'disconnected' | 'error';

interface ConnectOptions {
  channelId: string;
  conversationId: string | null;
  accessToken: string;
  onInboxEvent: (event: ConversationChangedEvent) => void;
  onMessage: (message: ConversationMessage) => void;
  onConnectionChange: (state: ConnectionState) => void;
  onStompError: (frame: IFrame) => void;
}

export interface AdminWsClient {
  disconnect: () => void;
}

function resolveWsUrl() {
  const explicit = import.meta.env.VITE_WS_URL as string | undefined;
  if (explicit) {
    return explicit;
  }
  const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '';
  if (!apiBaseUrl) {
    return `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws`;
  }
  const apiUrl = new URL(apiBaseUrl, window.location.origin);
  apiUrl.protocol = apiUrl.protocol === 'https:' ? 'wss:' : 'ws:';
  apiUrl.pathname = '/ws';
  apiUrl.search = '';
  return apiUrl.toString();
}

function parseJson<T>(message: IMessage): T | null {
  try {
    return JSON.parse(message.body) as T;
  } catch {
    return null;
  }
}

export function connectAdminConversationSocket(options: ConnectOptions): AdminWsClient {
  const subscriptions: StompSubscription[] = [];
  const client = new Client({
    brokerURL: resolveWsUrl(),
    connectHeaders: {
      Authorization: `Bearer ${options.accessToken}`,
    },
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  });

  options.onConnectionChange('connecting');

  client.onConnect = () => {
    subscriptions.push(
      client.subscribe(`/topic/admin/channels/${options.channelId}/conversations`, (message) => {
        const payload = parseJson<ConversationChangedEvent>(message);
        if (payload) {
          options.onInboxEvent(payload);
        }
      }),
    );
    if (options.conversationId) {
      subscriptions.push(
        client.subscribe(`/topic/admin/channels/${options.channelId}/conversations/${options.conversationId}/messages`, (message) => {
          const payload = parseJson<ConversationMessage>(message);
          if (payload) {
            options.onMessage(payload);
          }
        }),
      );
    }
    options.onConnectionChange('connected');
  };

  client.onStompError = (frame) => {
    options.onConnectionChange('error');
    options.onStompError(frame);
  };

  client.onWebSocketError = () => {
    options.onConnectionChange('error');
  };

  client.onWebSocketClose = () => {
    options.onConnectionChange('disconnected');
  };

  client.activate();

  return {
    disconnect() {
      subscriptions.forEach((subscription) => subscription.unsubscribe());
      subscriptions.length = 0;
      client.deactivate();
      options.onConnectionChange('disconnected');
    },
  };
}
