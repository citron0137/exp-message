import { getApiClient } from './api';
import type { ApiResponse } from './types';
import type { ChatRoomCreateRequest, ChatRoomCreateResponse, ChatRoomListItem } from './chatRoomTypes';

export const chatRoomApi = {
    async listMine(): Promise<ApiResponse<ChatRoomListItem[]>> {
        const http = getApiClient();
        const res = await http.get<ApiResponse<ChatRoomListItem[]>>('/chat-rooms');
        return res.data;
    },

    async create(body: ChatRoomCreateRequest): Promise<ApiResponse<ChatRoomCreateResponse>> {
        const http = getApiClient();
        const res = await http.post<ApiResponse<ChatRoomCreateResponse>>('/chat-rooms', body);
        return res.data;
    },
};
