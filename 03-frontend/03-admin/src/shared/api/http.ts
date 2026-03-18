import axios, { AxiosError, InternalAxiosRequestConfig, type AxiosRequestConfig } from 'axios';

interface RetryConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

type GetAccessToken = () => string | null;
type RefreshAccessToken = () => Promise<string>;
type OnUnauthorized = () => void;

let getAccessToken: GetAccessToken | null = null;
let refreshAccessToken: RefreshAccessToken | null = null;
let onUnauthorized: OnUnauthorized | null = null;
let refreshPromise: Promise<string> | null = null;

const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 10_000,
  withCredentials: true,
});

function isAuthRoute(url?: string) {
  if (!url) {
    return false;
  }
  return (
    url.includes('/admin/auth/login') ||
    url.includes('/admin/auth/refresh') ||
    url.includes('/admin/auth/logout') ||
    url.includes('/admin/web/auth/login') ||
    url.includes('/admin/web/auth/refresh') ||
    url.includes('/admin/web/auth/logout')
  );
}

httpClient.interceptors.request.use((config) => {
  const token = getAccessToken?.();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

httpClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetryConfig | undefined;

    if (!original || error.response?.status !== 401 || original._retry || isAuthRoute(original.url)) {
      return Promise.reject(error);
    }

    if (!refreshAccessToken) {
      onUnauthorized?.();
      return Promise.reject(error);
    }

    original._retry = true;

    try {
      refreshPromise = refreshPromise ?? refreshAccessToken();
      const newAccessToken = await refreshPromise;
      refreshPromise = null;

      original.headers = original.headers ?? {};
      original.headers.Authorization = `Bearer ${newAccessToken}`;

      return httpClient.request(original);
    } catch (refreshError) {
      refreshPromise = null;
      onUnauthorized?.();
      return Promise.reject(refreshError);
    }
  }
);

export function configureHttpAuth(config: {
  getAccessToken: GetAccessToken;
  refreshAccessToken: RefreshAccessToken;
  onUnauthorized: OnUnauthorized;
}) {
  getAccessToken = config.getAccessToken;
  refreshAccessToken = config.refreshAccessToken;
  onUnauthorized = config.onUnauthorized;
}

export async function apiRequest<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await httpClient.request<T>(config);
  return response.data;
}

export function toApiErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError(error)) {
    const payload = error.response?.data as { message?: string } | undefined;
    if (payload?.message) {
      return payload.message;
    }
  }
  return fallback;
}
