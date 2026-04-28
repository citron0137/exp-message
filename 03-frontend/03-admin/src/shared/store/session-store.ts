import { create } from 'zustand';
import { loginAdminApi, logoutAdminApi, refreshAdminApi } from '@/features/auth/api';
import { toApiErrorMessage } from '@/shared/api/http';
import { useWorkspaceStore } from '@/shared/store/workspace-store';

export type GlobalRole = 'PLATFORM_ADMIN' | 'CHANNEL_USER';
export type SessionStatus = 'loading' | 'authenticated' | 'anonymous';

interface SessionState {
  status: SessionStatus;
  accessToken: string | null;
  email: string | null;
  userId: string | null;
  sessionId: string | null;
  globalRole: GlobalRole | null;
  errorMessage: string | null;
  login: (email: string, password: string) => Promise<void>;
  refresh: () => Promise<string>;
  bootstrap: () => Promise<void>;
  logout: () => Promise<void>;
  clearSession: () => void;
}

export const useSessionStore = create<SessionState>((set, get) => ({
  status: 'loading',
  accessToken: null,
  email: null,
  userId: null,
  sessionId: null,
  globalRole: null,
  errorMessage: null,

  async login(email, password) {
    set({ status: 'loading', errorMessage: null });

    try {
      const auth = await loginAdminApi(email, password);
      set({
        status: 'authenticated',
        accessToken: auth.accessToken,
        globalRole: auth.globalRole,
        userId: auth.userId,
        sessionId: auth.sessionId,
        email,
        errorMessage: null,
      });
      void useWorkspaceStore.getState().loadChannels();
    } catch (error) {
      set({
        status: 'anonymous',
        accessToken: null,
        globalRole: null,
        userId: null,
        sessionId: null,
        errorMessage: toApiErrorMessage(error, 'Login failed.'),
      });
      throw error;
    }
  },

  async refresh() {
    const auth = await refreshAdminApi();

    set({
      status: 'authenticated',
      accessToken: auth.accessToken,
      globalRole: auth.globalRole,
      userId: auth.userId,
      sessionId: auth.sessionId,
    });
    void useWorkspaceStore.getState().loadChannels();

    return auth.accessToken;
  },

  async bootstrap() {
    set({ status: 'loading' });

    try {
      await get().refresh();
    } catch {
      get().clearSession();
    }
  },

  async logout() {
    try {
      await logoutAdminApi();
    } finally {
      get().clearSession();
    }
  },

  clearSession() {
    set({
      status: 'anonymous',
      accessToken: null,
      email: null,
      userId: null,
      sessionId: null,
      globalRole: null,
      errorMessage: null,
    });
    useWorkspaceStore.setState({ channels: [], activeChannelId: null, loading: false, errorMessage: null });
  },
}));
