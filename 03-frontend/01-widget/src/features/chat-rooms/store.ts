import { create } from 'zustand';
import { bootstrapWidget, createVisitorSession, enterConversation } from './api';
import { toApiErrorMessage } from '@/shared/http/client';
import type { WidgetConfig } from '@/types/config';

type RoomStatus = 'idle' | 'loading' | 'ready' | 'error';

interface RoomSessionState {
  publicKey: string | null;
  channelId: string | null;
  channelName: string | null;
  visitorId: string | null;
  visitorSessionToken: string | null;
  conversationId: string | null;
  conversationStatus: string | null;
  roomStatus: RoomStatus;
  errorMessage: string | null;
  ensureConversation: (config: WidgetConfig) => Promise<void>;
  resetRoomSession: () => void;
}

function storageKey(publicKey: string) {
  return `exp-message-widget-session:${publicKey}`;
}

function readStoredSession(publicKey: string) {
  if (typeof window === 'undefined') {
    return null;
  }
  const raw = window.localStorage.getItem(storageKey(publicKey));
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as { token?: string; expiresAt?: string };
  } catch {
    window.localStorage.removeItem(storageKey(publicKey));
    return null;
  }
}

function writeStoredSession(publicKey: string, token: string, expiresAt: string) {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.setItem(storageKey(publicKey), JSON.stringify({ token, expiresAt }));
}

export const useRoomSessionStore = create<RoomSessionState>((set, get) => ({
  publicKey: null,
  channelId: null,
  channelName: null,
  visitorId: null,
  visitorSessionToken: null,
  conversationId: null,
  conversationStatus: null,
  roomStatus: 'idle',
  errorMessage: null,
  async ensureConversation(config) {
    const publicKey = config.publicKey?.trim();
    if (!publicKey) {
      set({ roomStatus: 'error', errorMessage: 'Widget public key is missing.' });
      return;
    }

    const current = get();
    if (current.roomStatus === 'loading' || (current.publicKey === publicKey && current.conversationId)) {
      return;
    }

    set({ publicKey, roomStatus: 'loading', errorMessage: null });

    try {
      const bootstrap = await bootstrapWidget(publicKey);
      const stored = readStoredSession(publicKey);
      const session =
        stored?.token && stored.expiresAt && new Date(stored.expiresAt).getTime() > Date.now()
          ? null
          : await createVisitorSession(publicKey, config.visitor);
      const visitorSessionToken = stored?.token && !session ? stored.token : session?.session.token;

      if (!visitorSessionToken) {
        throw new Error('Visitor session is missing.');
      }

      if (session) {
        writeStoredSession(publicKey, session.session.token, session.session.expiresAt);
      }

      const entry = await enterConversation(publicKey, visitorSessionToken);

      set({
        publicKey,
        channelId: bootstrap.channel.id,
        channelName: bootstrap.channel.name,
        visitorId: entry.visitor.id,
        visitorSessionToken,
        conversationId: entry.conversation.id,
        conversationStatus: entry.conversation.status,
        roomStatus: 'ready',
        errorMessage: null,
      });
    } catch (error) {
      set({
        channelId: null,
        channelName: null,
        visitorId: null,
        visitorSessionToken: null,
        conversationId: null,
        conversationStatus: null,
        roomStatus: 'error',
        errorMessage: toApiErrorMessage(error, 'Unable to prepare conversation'),
      });
    }
  },
  resetRoomSession() {
    set({
      publicKey: null,
      channelId: null,
      channelName: null,
      visitorId: null,
      visitorSessionToken: null,
      conversationId: null,
      conversationStatus: null,
      roomStatus: 'idle',
      errorMessage: null,
    });
  },
}));
