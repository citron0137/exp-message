import { getApiClient } from './api';
import type { ApiResponse } from './types';
import type {
    ApiPageResult,
    MessageCreateRequest,
    MessageCreateResponse,
    MessageDetail,
} from './messageTypes';

export const messageApi = {
    async listByChatRoom(params: {
        chatRoomId: string;
        cursor?: string | null;
        limit?: number | null;
    }): Promise<ApiPageResult<MessageDetail>> {
        const http = getApiClient();
        const res = await http.get<ApiPageResult<MessageDetail>>('/messages', {
            params: {
                chatRoomId: params.chatRoomId,
                cursor: params.cursor ?? undefined,
                limit: params.limit ?? undefined,
            },
        });
        return res.data;
    },

    async create(body: MessageCreateRequest): Promise<ApiResponse<MessageCreateResponse>> {
        const http = getApiClient();
        const res = await http.post<ApiResponse<MessageCreateResponse>>('/messages', body);
        return res.data;
    },
};
