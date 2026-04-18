import { Client, type IFrame, type IMessage, type StompSubscription } from '@stomp/stompjs';

interface ConnectOptions {
  wsUrl: string;
  publicKey: string;
  visitorSessionToken: string;
  conversationId: string;
  onMessage: (message: IMessage) => void;
  onConnectionChange: (state: 'connecting' | 'connected' | 'disconnected' | 'error') => void;
  onStompError: (frame: IFrame) => void;
}

export interface ChatWsClient {
  disconnect: () => void;
  isConnected: () => boolean;
}

export function connectMessageSocket(options: ConnectOptions): ChatWsClient {
  let subscription: StompSubscription | null = null;

  const client = new Client({
    brokerURL: options.wsUrl,
    connectHeaders: {
      publicKey: options.publicKey,
      visitorSessionToken: options.visitorSessionToken,
      origin: window.location.origin,
    },
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  });

  options.onConnectionChange('connecting');

  client.onConnect = () => {
    subscription = client.subscribe(`/topic/widget/conversations/${options.conversationId}/messages`, options.onMessage);
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
      subscription?.unsubscribe();
      subscription = null;
      client.deactivate();
      options.onConnectionChange('disconnected');
    },
    isConnected() {
      return client.connected;
    },
  };
}
