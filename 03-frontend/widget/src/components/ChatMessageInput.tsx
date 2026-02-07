import { KeyboardEvent, useMemo, useState } from 'react';
import { useMessageStore } from '../lib/messageStore';

type ChatMessageInputProps = {
    chatRoomId: string;
};

export default function ChatMessageInput({ chatRoomId }: ChatMessageInputProps) {
    const [message, setMessage] = useState('');

    const roomState = useMessageStore((s) => s.rooms[chatRoomId]);
    const sendMessage = useMessageStore((s) => s.sendMessage);

    const isSending = roomState?.isSending ?? false;

    const canSend = useMemo(() => {
        if (isSending) {
            return false;
        }
        return message.trim().length > 0;
    }, [isSending, message]);

    const handleSend = async () => {
        if (!canSend) {
            return;
        }

        const content = message;
        setMessage('');

        const ok = await sendMessage(chatRoomId, content);
        if (!ok) {
            setMessage(content);
        }
    };

    const handleKeyPress = (e: KeyboardEvent<HTMLTextAreaElement>) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            void handleSend();
        }
    };

    return (
        <div className="p-4 bg-white border-t border-gray-200">
            <div className="flex gap-2 items-end">
                <textarea
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    onKeyDown={handleKeyPress}
                    placeholder="Type a message..."
                    className="flex-1 resize-none border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent max-h-32 text-sm"
                    rows={1}
                    disabled={isSending}
                />
                <button
                    onClick={() => void handleSend()}
                    disabled={!canSend}
                    className="bg-blue-600 hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed text-white rounded-lg px-4 py-2 transition-colors font-medium text-sm"
                >
                    {isSending ? '...' : 'Send'}
                </button>
            </div>
        </div>
    );
}
