import { create } from 'zustand';
import { chatRoomApi } from './chatRoomApi';
import type { ChatRoomListItem } from './chatRoomTypes';

/**
 * Chat room store (Zustand).
 *
 * Responsibilities:
 * - Fetch "my chat rooms" via `GET /chat-rooms`
 * - Create a chat room via `POST /chat-rooms`
 * - Track the currently selected room (`selectedRoomId`)
 *
 * Orchestration:
 * - `ChatWindow` triggers `fetchRooms()` when auth becomes `authenticated`.
 * - UI switches between `ChatRoomListView` and `ChatRoomView` based on `selectedRoomId`.
 */

type ChatRoomState = {
    rooms: ChatRoomListItem[];
    selectedRoomId: string | null;
    isLoading: boolean;
    errorMessage: string | null;

    fetchRooms: () => Promise<boolean>;
    createRoom: (name: string) => Promise<boolean>;
    selectRoom: (roomId: string) => void;
    backToList: () => void;
    clearError: () => void;
    reset: () => void;
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

export const useChatRoomStore = create<ChatRoomState>((set, get) => ({
    rooms: [],
    selectedRoomId: null,
    isLoading: false,
    errorMessage: null,

    clearError: () => set({ errorMessage: null }),

    reset: () => set({ rooms: [], selectedRoomId: null, isLoading: false, errorMessage: null }),

    fetchRooms: async () => {
        if (get().isLoading) {
            return false;
        }

        set({ isLoading: true, errorMessage: null });
        try {
            const res = await chatRoomApi.listMine();
            if (res.success !== true) {
                set({ isLoading: false, errorMessage: getErrorMessage(res, 'Failed to load chat rooms') });
                return false;
            }

            set({ rooms: res.data, isLoading: false });
            return true;
        } catch {
            set({ isLoading: false, errorMessage: 'Failed to load chat rooms' });
            return false;
        }
    },

    createRoom: async (name: string) => {
        const trimmed = name.trim();
        if (!trimmed) {
            return false;
        }

        if (get().isLoading) {
            return false;
        }

        set({ isLoading: true, errorMessage: null });
        try {
            const res = await chatRoomApi.create({ name: trimmed });
            if (res.success !== true) {
                set({ isLoading: false, errorMessage: getErrorMessage(res, 'Failed to create chat room') });
                return false;
            }

            const createdId = res.data.id;
            await get().fetchRooms();
            set({ selectedRoomId: createdId, isLoading: false });
            return true;
        } catch {
            set({ isLoading: false, errorMessage: 'Failed to create chat room' });
            return false;
        }
    },

    selectRoom: (roomId: string) => set({ selectedRoomId: roomId }),

    backToList: () => set({ selectedRoomId: null }),
}));
