export type MessageDetail = {
    id: string;
    chatRoomId: string;
    userId: string;
    content: string;
    createdAt: string;
};

export type MessageCreateRequest = {
    chatRoomId: string;
    content: string;
};

export type MessageCreateResponse = {
    id: string;
    chatRoomId: string;
    userId: string;
    content: string;
    createdAt: string;
};

export type ApiPageResponse<T> = {
    success: true;
    data: T[];
    pageInfo: {
        nextCursor: string | null;
        limit: number;
    };
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

export type ApiPageResult<T> = ApiPageResponse<T> | ApiErrorResponse;
