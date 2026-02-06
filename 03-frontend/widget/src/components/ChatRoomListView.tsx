import { useEffect, useMemo, useState } from 'react';
import { useChatRoomStore } from '../lib/chatRoomStore';

export default function ChatRoomListView() {
    const rooms = useChatRoomStore((s) => s.rooms);
    const isLoading = useChatRoomStore((s) => s.isLoading);
    const errorMessage = useChatRoomStore((s) => s.errorMessage);
    const fetchRooms = useChatRoomStore((s) => s.fetchRooms);
    const createRoom = useChatRoomStore((s) => s.createRoom);
    const selectRoom = useChatRoomStore((s) => s.selectRoom);

    const [newRoomName, setNewRoomName] = useState('');

    useEffect(() => {
        if (rooms.length > 0) {
            return;
        }
        void fetchRooms();
    }, [fetchRooms, rooms.length]);

    const canCreate = useMemo(() => {
        if (isLoading) {
            return false;
        }
        return newRoomName.trim().length > 0;
    }, [isLoading, newRoomName]);

    const onCreate = async () => {
        if (!canCreate) {
            return;
        }
        const ok = await createRoom(newRoomName);
        if (!ok) {
            return;
        }
        setNewRoomName('');
    };

    return (
        <div className="flex-1 p-4 bg-white flex flex-col gap-4">
            <div>
                <h3 className="text-lg font-semibold">Chat rooms</h3>
                <p className="text-sm text-gray-500">Select a room to start chatting</p>
            </div>

            <div className="flex gap-2">
                <input
                    value={newRoomName}
                    onChange={(e) => setNewRoomName(e.target.value)}
                    placeholder="New chat room name"
                    className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <button
                    onClick={onCreate}
                    disabled={!canCreate}
                    className="bg-blue-600 hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed text-white rounded-lg px-4 py-2 transition-colors font-medium text-sm"
                >
                    Create
                </button>
            </div>

            {errorMessage && (
                <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
                    {errorMessage}
                </div>
            )}

            <div className="flex-1 overflow-y-auto">
                {isLoading && rooms.length === 0 ? (
                    <div className="text-sm text-gray-600">Loading chat rooms...</div>
                ) : rooms.length === 0 ? (
                    <div className="text-sm text-gray-600">No chat rooms yet. Create one.</div>
                ) : (
                    <div className="space-y-2">
                        {rooms.map((room) => (
                            <button
                                key={room.id}
                                onClick={() => selectRoom(room.id)}
                                className="w-full text-left border border-gray-200 hover:border-blue-300 hover:bg-blue-50 rounded-lg px-3 py-3 transition-colors"
                            >
                                <div className="font-medium text-sm text-gray-900">{room.name}</div>
                                <div className="text-xs text-gray-500">Room ID: {room.id}</div>
                            </button>
                        ))}
                    </div>
                )}
            </div>

            <div className="flex justify-end">
                <button
                    onClick={() => void fetchRooms()}
                    disabled={isLoading}
                    className="text-sm text-blue-600 hover:text-blue-700 disabled:text-gray-400"
                >
                    Refresh
                </button>
            </div>
        </div>
    );
}
