import { create } from 'zustand';
import { messageApi } from './messageApi';
import type { MessageCreateResponse, MessageDetail } from './messageTypes';

/**
 * Message store (Zustand).
 *
 * Data model:
 * - Messages are cached per chat room in `rooms[chatRoomId]`.
 * - Each room holds `messages` and a `nextCursor` for cursor pagination.
 *
 * Responsibilities:
 * - `fetchInitial(chatRoomId)`: load the first page and reset room state
 * - `fetchMore(chatRoomId)`: load older messages using `nextCursor`
 * - `sendMessage(chatRoomId, content)`: send via REST and append the created message
 *
 * Notes:
 * - This milestone is REST-only. Real-time updates via WebSocket will be added later.
 */

type RoomState = {
    messages: MessageDetail[];
    nextCursor: string | null;
    isLoading: boolean;
    isSending: boolean;
    errorMessage: string | null;
};

type MessageState = {
    rooms: Record<string, RoomState | undefined>;

    fetchInitial: (chatRoomId: string) => Promise<boolean>;
    fetchMore: (chatRoomId: string) => Promise<boolean>;
    sendMessage: (chatRoomId: string, content: string) => Promise<boolean>;
    receiveMessage: (message: MessageDetail) => void;
    reset: () => void;
};

const DEFAULT_LIMIT = 20;

const mergeUniqueByIdAsc = (items: MessageDetail[]): MessageDetail[] => {
    const map = new Map<string, MessageDetail>();
    for (const item of items) {
        map.set(item.id, item);
    }

    return [...map.values()].sort((a, b) => {
        const at = new Date(a.createdAt).getTime();
        const bt = new Date(b.createdAt).getTime();
        return at - bt;
    });
};

const getErrorMessage = (response: unknown, fallback: string): string => {
    if (!response || typeof response !== 'object') {
        return fallback;
    }

    const maybe = response as { success?: unknown; error?: unknown };
    if (maybe.success === false && maybe.error && typeof maybe.error === 'object') {
        const errorObj = maybe.error as { message?: unknown };
        if (typeof errorObj.message === 'string' && errorObj.message.trim().length > 0) {
            return errorObj.message;
        }
    }

    return fallback;
};

const getRoomState = (state: MessageState, chatRoomId: string): RoomState => {
    const existing = state.rooms[chatRoomId];
    if (existing) {
        return existing;
    }

    return {
        messages: [],
        nextCursor: null,
        isLoading: false,
        isSending: false,
        errorMessage: null,
    };
};

export const useMessageStore = create<MessageState>((set, get) => ({
    rooms: {},

    reset: () => set({ rooms: {} }),

    receiveMessage: (message) => {
        const chatRoomId = message.chatRoomId;
        const current = getRoomState(get(), chatRoomId);
        const merged = mergeUniqueByIdAsc([...current.messages, message]);

        set({
            rooms: {
                ...get().rooms,
                [chatRoomId]: {
                    ...current,
                    messages: merged,
                },
            },
        });
    },

    fetchInitial: async (chatRoomId) => {
        const current = getRoomState(get(), chatRoomId);
        if (current.isLoading) {
            return false;
        }

        set({
            rooms: {
                ...get().rooms,
                [chatRoomId]: {
                    ...current,
                    isLoading: true,
                    errorMessage: null,
                    messages: [],
                    nextCursor: null,
                },
            },
        });

        try {
            const res = await messageApi.listByChatRoom({ chatRoomId, cursor: null, limit: DEFAULT_LIMIT });
            if (res.success !== true) {
                const failed = getRoomState(get(), chatRoomId);
                set({
                    rooms: {
                        ...get().rooms,
                        [chatRoomId]: {
                            ...failed,
                            isLoading: false,
                            errorMessage: getErrorMessage(res, 'Failed to load messages'),
                        },
                    },
                });
                return false;
            }

            const loaded = mergeUniqueByIdAsc(res.data);

            set({
                rooms: {
                    ...get().rooms,
                    [chatRoomId]: {
                        ...getRoomState(get(), chatRoomId),
                        isLoading: false,
                        messages: loaded,
                        nextCursor: res.pageInfo.nextCursor,
                        errorMessage: null,
                    },
                },
            });

            return true;
        } catch {
            const failed = getRoomState(get(), chatRoomId);
            set({
                rooms: {
                    ...get().rooms,
                    [chatRoomId]: {
                        ...failed,
                        isLoading: false,
                        errorMessage: 'Failed to load messages',
                    },
                },
            });
            return false;
        }
    },

    fetchMore: async (chatRoomId) => {
        const current = getRoomState(get(), chatRoomId);
        if (current.isLoading) {
            return false;
        }

        if (!current.nextCursor) {
            return false;
        }

        set({
            rooms: {
                ...get().rooms,
                [chatRoomId]: {
                    ...current,
                    isLoading: true,
                    errorMessage: null,
                },
            },
        });

        try {
            const res = await messageApi.listByChatRoom({
                chatRoomId,
                cursor: current.nextCursor,
                limit: DEFAULT_LIMIT,
            });

            if (res.success !== true) {
                const failed = getRoomState(get(), chatRoomId);
                set({
                    rooms: {
                        ...get().rooms,
                        [chatRoomId]: {
                            ...failed,
                            isLoading: false,
                            errorMessage: getErrorMessage(res, 'Failed to load more messages'),
                        },
                    },
                });
                return false;
            }

            const merged = mergeUniqueByIdAsc([...current.messages, ...res.data]);

            set({
                rooms: {
                    ...get().rooms,
                    [chatRoomId]: {
                        ...getRoomState(get(), chatRoomId),
                        isLoading: false,
                        messages: merged,
                        nextCursor: res.pageInfo.nextCursor,
                        errorMessage: null,
                    },
                },
            });

            return true;
        } catch {
            const failed = getRoomState(get(), chatRoomId);
            set({
                rooms: {
                    ...get().rooms,
                    [chatRoomId]: {
                        ...failed,
                        isLoading: false,
                        errorMessage: 'Failed to load more messages',
                    },
                },
            });
            return false;
        }
    },

    sendMessage: async (chatRoomId, content) => {
        const trimmed = content.trim();
        if (!trimmed) {
            return false;
        }

        const current = getRoomState(get(), chatRoomId);
        if (current.isSending) {
            return false;
        }

        set({
            rooms: {
                ...get().rooms,
                [chatRoomId]: {
                    ...current,
                    isSending: true,
                    errorMessage: null,
                },
            },
        });

        try {
            const res = await messageApi.create({ chatRoomId, content: trimmed });
            if (res.success !== true) {
                const failed = getRoomState(get(), chatRoomId);
                set({
                    rooms: {
                        ...get().rooms,
                        [chatRoomId]: {
                            ...failed,
                            isSending: false,
                            errorMessage: getErrorMessage(res, 'Failed to send message'),
                        },
                    },
                });
                return false;
            }

            const created = res.data as MessageCreateResponse;
            const merged = mergeUniqueByIdAsc([...current.messages, created]);

            set({
                rooms: {
                    ...get().rooms,
                    [chatRoomId]: {
                        ...getRoomState(get(), chatRoomId),
                        messages: merged,
                        isSending: false,
                        errorMessage: null,
                    },
                },
            });

            return true;
        } catch {
            const failed = getRoomState(get(), chatRoomId);
            set({
                rooms: {
                    ...get().rooms,
                    [chatRoomId]: {
                        ...failed,
                        isSending: false,
                        errorMessage: 'Failed to send message',
                    },
                },
            });
            return false;
        }
    },
}));
