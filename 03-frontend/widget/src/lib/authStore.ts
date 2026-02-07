import { create } from 'zustand';
import { authApi, type LoginBody, type SignUpBody } from './authApi';
import { authTokenAccessor } from './httpClient';
import { sessionStorage } from './sessionStorage';
import type { UserMe } from './types';

/**
 * Auth store (Zustand).
 *
 * High-level flow:
 * - `hydrate()` runs when the widget opens (see `ChatWindow`). It loads tokens from localStorage
 *   and calls `fetchMe()` to validate the session.
 * - `login()` calls `/auth/login`, persists tokens, and then calls `fetchMe()`.
 * - `logout()` clears localStorage + in-memory access token and resets state.
 *
 * Status state machine:
 * - `unknown`: bootstrap/hydrate not finished yet
 * - `anonymous`: no valid session
 * - `authenticated`: session is valid and `me` is populated
 */

type AuthStatus = 'unknown' | 'anonymous' | 'authenticated';

type AuthState = {
    status: AuthStatus;
    me: UserMe | null;
    accessToken: string | null;
    refreshToken: string | null;
    errorMessage: string | null;

    hydrate: () => Promise<void>;
    signUp: (body: SignUpBody) => Promise<boolean>;
    login: (body: LoginBody) => Promise<boolean>;
    logout: () => void;
    fetchMe: () => Promise<boolean>;
    clearError: () => void;
};

const getErrorMessage = (response: unknown, fallback: string): string => {
    if (!response || typeof response !== 'object') {
        return fallback;
    }

    const maybe = response as { success?: unknown; error?: unknown };
    if (maybe.success === false && maybe.error && typeof maybe.error === 'object') {
        const errorObj = maybe.error as { message?: unknown };
        if (typeof errorObj.message === 'string' && errorObj.message.trim().length > 0) {
            return errorObj.message;
        }
    }

    return fallback;
};

export const useAuthStore = create<AuthState>((set, get) => ({
    status: 'unknown',
    me: null,
    accessToken: null,
    refreshToken: null,
    errorMessage: null,

    clearError: () => set({ errorMessage: null }),

    hydrate: async () => {
        const stored = sessionStorage.get();
        if (!stored) {
            set({ status: 'anonymous', me: null, accessToken: null, refreshToken: null });
            return;
        }

        authTokenAccessor.setAccessToken(stored.accessToken);
        set({ accessToken: stored.accessToken, refreshToken: stored.refreshToken });

        const ok = await get().fetchMe();
        if (!ok) {
            sessionStorage.clear();
            authTokenAccessor.clear();
            set({ status: 'anonymous', me: null, accessToken: null, refreshToken: null });
        }
    },

    fetchMe: async () => {
        try {
            const res = await authApi.me();
            if (res.success !== true) {
                set({ status: 'anonymous', me: null });
                return false;
            }

            set({ status: 'authenticated', me: res.data });
            return true;
        } catch {
            set({ status: 'anonymous', me: null });
            return false;
        }
    },

    signUp: async (body) => {
        set({ errorMessage: null });
        try {
            const res = await authApi.signUp(body);
            if (res.success !== true) {
                set({ errorMessage: getErrorMessage(res, 'Sign up failed') });
                return false;
            }
            return true;
        } catch {
            set({ errorMessage: 'Sign up failed' });
            return false;
        }
    },

    login: async (body) => {
        set({ errorMessage: null });
        try {
            const res = await authApi.login(body);
            if (res.success !== true) {
                set({ errorMessage: getErrorMessage(res, 'Login failed') });
                return false;
            }

            const accessToken = res.data.accessToken;
            const refreshToken = res.data.refreshToken;

            // TODO(auth-cookie-migration): For the "Refresh token (httpOnly cookie) approach",
            // backend should set refreshToken as a cookie during login.
            // In that case, do not store refreshToken in localStorage.
            sessionStorage.set({ accessToken, refreshToken });
            authTokenAccessor.setAccessToken(accessToken);

            set({ accessToken, refreshToken });

            const ok = await get().fetchMe();
            if (!ok) {
                set({ errorMessage: 'Login succeeded but failed to load user profile' });
                return false;
            }

            return true;
        } catch {
            set({ errorMessage: 'Login failed' });
            return false;
        }
    },

    logout: () => {
        sessionStorage.clear();
        authTokenAccessor.clear();
        set({ status: 'anonymous', me: null, accessToken: null, refreshToken: null, errorMessage: null });
    },
}));
