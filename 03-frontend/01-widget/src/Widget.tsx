import { useEffect, useState } from 'react';
import FloatingButton from './components/FloatingButton';
import type { WidgetConfig } from './types/config';
import ChatRoomView from './features/chat-rooms/ChatRoomView';
import { configureHttpClient } from './shared/http/client';
import StatusBadge from './shared/ui/StatusBadge';
import { useRoomSessionStore } from './features/chat-rooms/store';

interface WidgetProps {
    config: WidgetConfig;
}

export default function Widget({ config }: WidgetProps) {
    const [isOpen, setIsOpen] = useState(false);
    const publicKey = useRoomSessionStore((state) => state.publicKey);
    const visitorSessionToken = useRoomSessionStore((state) => state.visitorSessionToken);
    const conversationId = useRoomSessionStore((state) => state.conversationId);
    const channelName = useRoomSessionStore((state) => state.channelName);
    const roomStatus = useRoomSessionStore((state) => state.roomStatus);
    const roomErrorMessage = useRoomSessionStore((state) => state.errorMessage);
    const ensureConversation = useRoomSessionStore((state) => state.ensureConversation);
    const isReady = roomStatus === 'ready';

    useEffect(() => {
        configureHttpClient(config.apiUrl);
    }, [config.apiUrl]);

    useEffect(() => {
        if (isOpen) {
            void ensureConversation(config);
        }
    }, [config, ensureConversation, isOpen]);

    return (
        <div className="chat-widget">
            <FloatingButton
                onClick={() => setIsOpen((prev) => !prev)}
                isOpen={isOpen}
                isAuthorized={isReady}
            />

            {isOpen && (
                <div className="fixed bottom-24 right-6 flex h-[560px] w-96 flex-col overflow-hidden rounded-xl bg-white shadow-2xl">
                    <div className="flex items-center justify-between bg-blue-600 px-4 py-3 text-white">
                        <div>
                            <h3 className="text-sm font-semibold">r-message Chat Support</h3>
                            <p className="text-xs text-blue-100">Always-on status</p>
                        </div>
                        <StatusBadge isAuthorized={isReady} />
                    </div>

                    {roomStatus === 'idle' && (
                        <div className="flex flex-1 items-center justify-center bg-gray-50 p-4 text-sm text-gray-600">
                            Preparing chat...
                        </div>
                    )}

                    {roomStatus === 'loading' && (
                        <div className="flex flex-1 items-center justify-center bg-gray-50 p-4 text-sm text-gray-600">
                            Preparing chat room...
                        </div>
                    )}

                    {roomStatus === 'error' && (
                        <div className="flex flex-1 flex-col items-center justify-center gap-3 bg-gray-50 p-4">
                            <p className="text-center text-sm text-red-600">{roomErrorMessage ?? 'Unable to prepare chat room'}</p>
                            <button
                                type="button"
                                className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-blue-700"
                                onClick={() => void ensureConversation(config)}
                            >
                                Retry
                            </button>
                        </div>
                    )}

                    {roomStatus === 'ready' && publicKey && visitorSessionToken && conversationId && channelName && (
                        <ChatRoomView
                            publicKey={publicKey}
                            visitorSessionToken={visitorSessionToken}
                            conversationId={conversationId}
                            roomName={channelName}
                            wsUrl={config.wsUrl}
                        />
                    )}
                </div>
            )}
        </div>
    );
}
