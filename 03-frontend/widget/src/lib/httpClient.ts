import axios, { AxiosError, type AxiosInstance } from 'axios';
import { sessionStorage } from './sessionStorage';
import type { ApiResponse, AuthLoginResponse } from './types';

/**
 * Shared Axios HTTP client factory.
 *
 * Responsibilities:
 * - Attach `Authorization: Bearer <accessToken>` header for authenticated requests
 * - Retry once on 401 by calling `/auth/refresh` and then re-sending the original request
 * - Clear stored session if refresh is not possible or fails
 *
 * Notes:
 * - `bootstrap(config)` must run before any API call so the client is initialized with baseUrl.
 * - Token persistence is handled in `sessionStorage` and `authStore`.
 */

export type CreateHttpClientOptions = {
    baseUrl: string;
};

let inMemoryAccessToken: string | null = null;

export const authTokenAccessor = {
    getAccessToken(): string | null {
        if (inMemoryAccessToken) {
            return inMemoryAccessToken;
        }

        const stored = sessionStorage.get();
        inMemoryAccessToken = stored?.accessToken ?? null;
        return inMemoryAccessToken;
    },
    setAccessToken(token: string | null): void {
        inMemoryAccessToken = token;
    },
    clear(): void {
        inMemoryAccessToken = null;
    },
};

const isApiResponse = (data: unknown): data is ApiResponse<unknown> => {
    if (!data || typeof data !== 'object') {
        return false;
    }

    return 'success' in data;
};

export const createHttpClient = ({ baseUrl }: CreateHttpClientOptions): AxiosInstance => {
    const http = axios.create({
        baseURL: baseUrl,
        headers: {
            'Content-Type': 'application/json',
        },
        // TODO(auth-cookie-migration): When moving to the "Refresh token (httpOnly cookie) approach",
        // enable `withCredentials: true` so the browser sends cookies to the API domain.
        // This also requires backend CORS settings to allow credentials.
        // withCredentials: true,
    });

    http.interceptors.request.use((config) => {
        const accessToken = authTokenAccessor.getAccessToken();
        if (!accessToken) {
            return config;
        }

        config.headers = config.headers ?? {};
        config.headers.Authorization = `Bearer ${accessToken}`;
        return config;
    });

    http.interceptors.response.use(
        (response) => response,
        async (error: AxiosError) => {
            const response = error.response;
            const originalRequest = error.config;

            if (!response || !originalRequest) {
                return Promise.reject(error);
            }

            const shouldTryRefresh =
                response.status === 401 &&
                (originalRequest as unknown as { _retry?: boolean })._retry !== true;

            if (!shouldTryRefresh) {
                return Promise.reject(error);
            }

            const stored = sessionStorage.get();
            const refreshToken = stored?.refreshToken;
            if (!refreshToken) {
                sessionStorage.clear();
                authTokenAccessor.clear();
                return Promise.reject(error);
            }

            try {
                (originalRequest as unknown as { _retry?: boolean })._retry = true;

                const refreshResponse = await axios.post<ApiResponse<AuthLoginResponse>>(
                    `${baseUrl}/auth/refresh`,
                    { refreshToken },
                    { headers: { 'Content-Type': 'application/json' } },
                );

                // TODO(auth-cookie-migration): For the "Refresh token (httpOnly cookie) approach",
                // change the refresh request to NOT send refreshToken in the body.
                // Backend should read refreshToken from the cookie and return a new access token.
                // Also ensure this request is sent with credentials (cookies).

                if (!isApiResponse(refreshResponse.data) || refreshResponse.data.success !== true) {
                    sessionStorage.clear();
                    authTokenAccessor.clear();
                    return Promise.reject(error);
                }

                const newAccessToken = refreshResponse.data.data.accessToken;
                const newRefreshToken = refreshResponse.data.data.refreshToken;

                sessionStorage.set({
                    accessToken: newAccessToken,
                    refreshToken: newRefreshToken,
                });
                authTokenAccessor.setAccessToken(newAccessToken);

                originalRequest.headers = originalRequest.headers ?? {};
                originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;

                return http(originalRequest);
            } catch {
                sessionStorage.clear();
                authTokenAccessor.clear();
                return Promise.reject(error);
            }
        },
    );

    return http;
};
