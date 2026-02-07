export type ApiSuccessResponse<T> = {
    success: true;
    data: T;
    error: null;
};

export type ApiErrorResponse = {
    success: false;
    data: null;
    error: {
        code: string;
        message: string;
        details?: Record<string, unknown> | null;
        occurredAt?: string | null;
        path?: string | null;
    };
};

export type ApiResponse<T> = ApiSuccessResponse<T> | ApiErrorResponse;

export type UserMe = {
    id: string;
    email: string;
    nickname: string;
    createdAt: string;
    updatedAt: string;
};

export type AuthLoginResponse = {
    accessToken: string;
    accessTokenExpiresAt: string;
    refreshToken: string | null;
    refreshTokenExpiresAt: string | null;
    userId: string;
    sessionId: string;
};
