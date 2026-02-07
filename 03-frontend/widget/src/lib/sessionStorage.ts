const STORAGE_KEY = 'exp-message-widget.auth';

export type StoredAuthSession = {
    accessToken: string;
    // TODO(auth-cookie-migration): For the "Refresh token (httpOnly cookie) approach",
    // remove refreshToken from localStorage and rely on cookies for refresh.
    refreshToken: string | null;
};

export const sessionStorage = {
    get(): StoredAuthSession | null {
        try {
            const raw = localStorage.getItem(STORAGE_KEY);
            if (!raw) {
                return null;
            }
            const parsed = JSON.parse(raw) as StoredAuthSession;
            if (!parsed?.accessToken) {
                return null;
            }
            return parsed;
        } catch {
            return null;
        }
    },
    set(session: StoredAuthSession): void {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    },
    clear(): void {
        localStorage.removeItem(STORAGE_KEY);
    },
};
