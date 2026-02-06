import { useEffect, useMemo, useRef } from 'react';
import { useAuthStore } from '../lib/authStore';
import { useMessageStore } from '../lib/messageStore';
import ChatMessageBubble from './ChatMessageBubble';

type ChatMessageListProps = {
    chatRoomId: string;
};

export default function ChatMessageList({ chatRoomId }: ChatMessageListProps) {
    const me = useAuthStore((s) => s.me);
    const roomState = useMessageStore((s) => s.rooms[chatRoomId]);
    const fetchInitial = useMessageStore((s) => s.fetchInitial);
    const fetchMore = useMessageStore((s) => s.fetchMore);

    const messagesEndRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        void fetchInitial(chatRoomId);
    }, [chatRoomId, fetchInitial]);

    const messages = roomState?.messages ?? [];
    const isLoading = roomState?.isLoading ?? false;
    const nextCursor = roomState?.nextCursor ?? null;
    const errorMessage = roomState?.errorMessage ?? null;

    const canLoadMore = useMemo(() => {
        if (isLoading) {
            return false;
        }
        return nextCursor != null;
    }, [isLoading, nextCursor]);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages.length]);

    return (
        <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-gray-50">
            {errorMessage ? (
                <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
                    {errorMessage}
                </div>
            ) : null}

            {canLoadMore ? (
                <button
                    onClick={() => void fetchMore(chatRoomId)}
                    className="w-full text-xs text-blue-600 hover:text-blue-700"
                >
                    Load older messages
                </button>
            ) : null}

            {isLoading && messages.length === 0 ? (
                <div className="text-sm text-gray-600">Loading messages...</div>
            ) : messages.length === 0 ? (
                <div className="text-sm text-gray-600">No messages yet. Say hi.</div>
            ) : (
                messages.map((m) => (
                    <ChatMessageBubble
                        key={m.id}
                        content={m.content}
                        createdAt={m.createdAt}
                        isOwn={me?.id != null && m.userId === me.id}
                        senderLabel={m.userId}
                    />
                ))
            )}

            <div ref={messagesEndRef} />
        </div>
    );
}
