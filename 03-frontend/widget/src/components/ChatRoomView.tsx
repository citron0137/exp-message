import { useMemo } from 'react';
import { useChatRoomStore } from '../lib/chatRoomStore';
import ChatMessageInput from './ChatMessageInput';
import ChatMessageList from './ChatMessageList';

export default function ChatRoomView() {
    const rooms = useChatRoomStore((s) => s.rooms);
    const selectedRoomId = useChatRoomStore((s) => s.selectedRoomId);
    const backToList = useChatRoomStore((s) => s.backToList);

    const selectedRoom = useMemo(() => {
        if (!selectedRoomId) {
            return null;
        }
        return rooms.find((r) => r.id === selectedRoomId) ?? null;
    }, [rooms, selectedRoomId]);

    if (!selectedRoomId) {
        return null;
    }

    return (
        <div className="flex-1 bg-white flex flex-col">
            <div className="flex items-center justify-between">
                <div className="p-4">
                    <h3 className="text-lg font-semibold">{selectedRoom?.name ?? 'Chat room'}</h3>
                    <p className="text-sm text-gray-500">Room ID: {selectedRoomId}</p>
                </div>
                <button
                    onClick={backToList}
                    className="text-sm text-blue-600 hover:text-blue-700"
                >
                    Back
                </button>
            </div>

            <ChatMessageList chatRoomId={selectedRoomId} />
            <ChatMessageInput chatRoomId={selectedRoomId} />
        </div>
    );
}
