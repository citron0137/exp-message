import { apiRequest } from '@/shared/api/http';
import type { AdminRole } from '@/shared/store/session-store';

interface AuthPayload {
  accessToken: string;
  role: string;
  userId: string;
}

interface WrappedResponse {
  data?: AuthPayload;
}

function parseAuthPayload(payload: unknown): AuthPayload {
  const wrapped = payload as WrappedResponse;
  const data = wrapped.data ?? (payload as AuthPayload);

  if (!data.accessToken || !data.userId || !data.role) {
    throw new Error('Invalid auth payload.');
  }

  return {
    accessToken: data.accessToken,
    role: data.role,
    userId: data.userId,
  };
}

function toAdminRole(role: string): AdminRole | null {
  if (role === 'OWNER' || role === 'ADMIN' || role === 'AGENT') {
    return role;
  }
  if (role === 'USER') {
    return 'AGENT';
  }
  return null;
}

export async function loginAdminApi(email: string, password: string) {
  const payload = await apiRequest<unknown>({
    method: 'POST',
    url: '/admin/web/auth/login',
    data: { email, password },
  });

  const data = parseAuthPayload(payload);
  return {
    accessToken: data.accessToken,
    role: toAdminRole(data.role),
    userId: data.userId,
  };
}

export async function refreshAdminApi() {
  // Refresh token is expected from HttpOnly cookie.
  const payload = await apiRequest<unknown>({
    method: 'POST',
    url: '/admin/web/auth/refresh',
    data: {},
  });

  const data = parseAuthPayload(payload);
  return {
    accessToken: data.accessToken,
    role: toAdminRole(data.role),
    userId: data.userId,
  };
}

export async function logoutAdminApi() {
  await apiRequest<unknown>({
    method: 'POST',
    url: '/admin/web/auth/logout',
  });
}
