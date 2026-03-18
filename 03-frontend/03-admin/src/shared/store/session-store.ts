import { create } from 'zustand';
import { loginAdminApi, logoutAdminApi, refreshAdminApi } from '@/features/auth/api';
import { toApiErrorMessage } from '@/shared/api/http';

export type AdminRole = 'OWNER' | 'ADMIN' | 'AGENT';
export type SessionStatus = 'loading' | 'authenticated' | 'anonymous';

interface SessionState {
  status: SessionStatus;
  accessToken: string | null;
  email: string | null;
  userId: string | null;
  role: AdminRole | null;
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
  role: null,
  errorMessage: null,

  async login(email, password) {
    set({ status: 'loading', errorMessage: null });

    try {
      const auth = await loginAdminApi(email, password);
      set({
        status: 'authenticated',
        accessToken: auth.accessToken,
        role: auth.role,
        userId: auth.userId,
        email,
        errorMessage: null,
      });
    } catch (error) {
      set({
        status: 'anonymous',
        accessToken: null,
        role: null,
        userId: null,
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
      role: auth.role,
      userId: auth.userId,
    });

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
      role: null,
      errorMessage: null,
    });
  },
}));
