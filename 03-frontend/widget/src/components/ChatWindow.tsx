import { useEffect } from 'react';
import ChatHeader from './ChatHeader';
import AuthView from './AuthView';
import BootstrapError from './BootstrapError';
import { useAuthStore } from '../lib/authStore';
import { useBootstrap } from '../lib/useBootstrap';
import ChatRoomListView from './ChatRoomListView';
import ChatRoomView from './ChatRoomView';
import { useChatRoomStore } from '../lib/chatRoomStore';
import { wsClient } from '../lib/wsClient';
import { useMessageStore } from '../lib/messageStore';

/**
 * Main widget container.
 *
 * This component acts as a small state machine:
 * - Bootstrap: initialize API client using widget config (apiUrl)
 * - Auth: hydrate session -> show AuthView if anonymous
 * - App: after authentication, fetch chat rooms and switch views based on selectedRoomId
 */

interface ChatWindowProps {
    // onClose: () => void;
    config: {
        apiUrl?: string;
        wsUrl?: string;
        theme?: 'light' | 'dark';
    };
}

export default function ChatWindow({
    // onClose,
    config }: ChatWindowProps) {
    const { error: bootstrapError } = useBootstrap(config);
    const status = useAuthStore((s) => s.status);
    const hydrate = useAuthStore((s) => s.hydrate);
    const me = useAuthStore((s) => s.me);
    const accessToken = useAuthStore((s) => s.accessToken);
    const selectedRoomId = useChatRoomStore((s) => s.selectedRoomId);
    const fetchRooms = useChatRoomStore((s) => s.fetchRooms);
    const resetChatRooms = useChatRoomStore((s) => s.reset);
    const receiveMessage = useMessageStore((s) => s.receiveMessage);

    useEffect(() => {
        if (bootstrapError) {
            return;
        }

        if (status !== 'unknown') {
            return;
        }

        void hydrate();
    }, [bootstrapError, hydrate, status]);

    useEffect(() => {
        if (bootstrapError) {
            return;
        }

        if (status !== 'authenticated') {
            resetChatRooms();
            return;
        }

        void fetchRooms();
    }, [bootstrapError, fetchRooms, resetChatRooms, status]);

    useEffect(() => {
        if (bootstrapError) {
            return;
        }

        const wsUrl = config.wsUrl;
        if (status !== 'authenticated' || !wsUrl || !me?.id || !accessToken) {
            void wsClient.disconnect();
            return;
        }

        void wsClient.connect({
            wsUrl,
            accessToken,
            userId: me.id,
            onMessage: (msg) => {
                receiveMessage({
                    id: msg.id,
                    chatRoomId: msg.chatRoomId,
                    userId: msg.userId,
                    content: msg.content,
                    createdAt: msg.createdAt,
                });
            },
        });

        return () => {
            void wsClient.disconnect();
        };
    }, [accessToken, bootstrapError, config.wsUrl, me?.id, receiveMessage, status]);

    return (
        <div className="fixed bottom-20 right-5 w-96 h-[600px] bg-white rounded-xl shadow-2xl flex flex-col overflow-hidden z-[9998]">
            <ChatHeader />
            {bootstrapError ? (
                <BootstrapError message={bootstrapError} />
            ) : status === 'unknown' ? (
                <div className="flex-1 p-4 bg-white text-sm text-gray-600">Loading...</div>
            ) : status === 'anonymous' ? (
                <AuthView />
            ) : (
                <>{selectedRoomId ? <ChatRoomView /> : <ChatRoomListView />}</>
            )}
        </div>
    );
}
