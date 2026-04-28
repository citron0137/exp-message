import { coreRequest } from '@/shared/api/http';
import type { GlobalRole } from '@/shared/store/session-store';

interface AuthPayload {
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
  userId: string;
  sessionId: string;
  globalRole: GlobalRole;
}

function parseAuthPayload(payload: unknown): AuthPayload {
  const data = payload as AuthPayload;

  if (!data.accessToken || !data.userId || !data.sessionId || !data.globalRole) {
    throw new Error('Invalid auth payload.');
  }

  return data;
}

export async function loginAdminApi(email: string, password: string) {
  const payload = await coreRequest<unknown>({
    method: 'POST',
    url: '/admin/auth/login',
    data: { email, password },
  });

  const data = parseAuthPayload(payload);
  return {
    accessToken: data.accessToken,
    globalRole: data.globalRole,
    userId: data.userId,
    sessionId: data.sessionId,
  };
}

export async function refreshAdminApi() {
  // Refresh token is expected from HttpOnly cookie.
  const payload = await coreRequest<unknown>({
    method: 'POST',
    url: '/admin/auth/refresh',
    data: {},
  });

  const data = parseAuthPayload(payload);
  return {
    accessToken: data.accessToken,
    globalRole: data.globalRole,
    userId: data.userId,
    sessionId: data.sessionId,
  };
}

export async function logoutAdminApi() {
  await coreRequest<unknown>({
    method: 'POST',
    url: '/admin/auth/logout',
  });
}
