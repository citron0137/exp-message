import { create } from 'zustand';
import { toApiErrorMessage } from '@/shared/http/client';
import { getMessagesApi, sendMessageApi, type ChatMessage } from './message-api';
import { connectMessageSocket, type ChatWsClient } from './ws-client';

type ConnectionStatus = 'idle' | 'connecting' | 'connected' | 'disconnected' | 'error';

interface InitRoomParams {
  publicKey: string;
  visitorSessionToken: string;
  conversationId: string;
  wsUrl?: string;
}

interface ChatStoreState {
  conversationId: string | null;
  messages: ChatMessage[];
  nextAfterSequence: number;
  hasMore: boolean;
  loadingInitial: boolean;
  loadingMore: boolean;
  sending: boolean;
  errorMessage: string | null;
  connectionStatus: ConnectionStatus;
  initRoom: (params: InitRoomParams) => Promise<void>;
  loadMore: (publicKey: string, visitorSessionToken: string) => Promise<void>;
  sendMessage: (publicKey: string, visitorSessionToken: string, content: string) => Promise<void>;
  teardown: () => void;
}

let wsClient: ChatWsClient | null = null;

function mergeUnique(messages: ChatMessage[], incoming: ChatMessage[]) {
  const map = new Map<string, ChatMessage>();
  for (const message of [...messages, ...incoming]) {
    map.set(message.id, message);
  }
  return Array.from(map.values()).sort((a, b) => a.sequence - b.sequence);
}

function parseWsMessage(body: string): ChatMessage | null {
  try {
    const parsed = JSON.parse(body) as Partial<ChatMessage>;
    if (!parsed.id || !parsed.conversationId || !parsed.senderId || !parsed.content || !parsed.createdAt) {
      return null;
    }
    return parsed as ChatMessage;
  } catch {
    return null;
  }
}

export const useChatStore = create<ChatStoreState>((set, get) => ({
  conversationId: null,
  messages: [],
  nextAfterSequence: 0,
  hasMore: false,
  loadingInitial: false,
  loadingMore: false,
  sending: false,
  errorMessage: null,
  connectionStatus: 'idle',
  async initRoom({ publicKey, visitorSessionToken, conversationId, wsUrl }) {
    const current = get();
    if (current.loadingInitial) {
      return;
    }

    if (current.conversationId !== conversationId) {
      wsClient?.disconnect();
      wsClient = null;
      set({
        conversationId,
        messages: [],
        nextAfterSequence: 0,
        hasMore: false,
      });
    }

    set({ loadingInitial: true, errorMessage: null });

    try {
      const firstPage = await getMessagesApi(publicKey, visitorSessionToken, conversationId, 0, 50);
      set((state) => ({
        messages: mergeUnique(state.messages, firstPage.messages),
        nextAfterSequence: firstPage.nextAfterSequence,
        hasMore: firstPage.hasMore,
      }));
    } catch (error) {
      set({ errorMessage: toApiErrorMessage(error, 'Unable to load messages') });
    } finally {
      set({ loadingInitial: false });
    }

    if (!wsUrl || wsClient) {
      return;
    }

    wsClient = connectMessageSocket({
      wsUrl,
      publicKey,
      visitorSessionToken,
      conversationId,
      onConnectionChange: (status) => set({ connectionStatus: status }),
      onStompError: () => set({ connectionStatus: 'error' }),
      onMessage: (frame) => {
        const message = parseWsMessage(frame.body);
        if (!message || message.conversationId !== get().conversationId) {
          return;
        }
        set((state) => ({ messages: mergeUnique(state.messages, [message]) }));
      },
    });
  },
  async loadMore(publicKey, visitorSessionToken) {
    const state = get();
    if (!state.conversationId || !state.hasMore || state.loadingMore) {
      return;
    }

    set({ loadingMore: true });
    try {
      const page = await getMessagesApi(publicKey, visitorSessionToken, state.conversationId, state.nextAfterSequence, 50);
      set((current) => ({
        messages: mergeUnique(current.messages, page.messages),
        nextAfterSequence: page.nextAfterSequence,
        hasMore: page.hasMore,
      }));
    } catch (error) {
      set({ errorMessage: toApiErrorMessage(error, 'Unable to load more messages') });
    } finally {
      set({ loadingMore: false });
    }
  },
  async sendMessage(publicKey, visitorSessionToken, content) {
    const state = get();
    if (!state.conversationId || !content.trim() || state.sending) {
      return;
    }

    set({ sending: true, errorMessage: null });
    try {
      const sent = await sendMessageApi(publicKey, visitorSessionToken, state.conversationId, content.trim());
      set((current) => ({ messages: mergeUnique(current.messages, [sent]) }));
    } catch (error) {
      set({ errorMessage: toApiErrorMessage(error, 'Unable to send message') });
    } finally {
      set({ sending: false });
    }
  },
  teardown() {
    wsClient?.disconnect();
    wsClient = null;
    set({
      conversationId: null,
      messages: [],
      nextAfterSequence: 0,
      hasMore: false,
      loadingInitial: false,
      loadingMore: false,
      sending: false,
      errorMessage: null,
      connectionStatus: 'idle',
    });
  },
}));
