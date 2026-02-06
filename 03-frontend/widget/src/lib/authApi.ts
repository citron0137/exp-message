import type { ApiResponse, AuthLoginResponse, UserMe } from './types';
import { getApiClient } from './api';

export type SignUpBody = {
    email: string;
    password: string;
    nickname: string;
};

export type LoginBody = {
    email: string;
    password: string;
};

export const authApi = {
    async signUp(body: SignUpBody): Promise<ApiResponse<{ id: string; email: string; nickname: string }>> {
        const http = getApiClient();
        const res = await http.post<ApiResponse<{ id: string; email: string; nickname: string }>>('/users', body);
        return res.data;
    },
    async login(body: LoginBody): Promise<ApiResponse<AuthLoginResponse>> {
        const http = getApiClient();
        const res = await http.post<ApiResponse<AuthLoginResponse>>('/auth/login', body);
        return res.data;
    },
    async me(): Promise<ApiResponse<UserMe>> {
        const http = getApiClient();
        const res = await http.get<ApiResponse<UserMe>>('/users/me');
        return res.data;
    },
};
