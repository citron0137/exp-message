import { useEffect, useRef, useState } from 'react';
import { Send } from 'lucide-react';
import { useChatStore } from './chat-store';

interface ChatRoomViewProps {
  publicKey: string;
  visitorSessionToken: string;
  conversationId: string;
  roomName: string;
  wsUrl?: string;
}

export default function ChatRoomView({ publicKey, visitorSessionToken, conversationId, roomName, wsUrl }: ChatRoomViewProps) {
  const [message, setMessage] = useState('');
  const listRef = useRef<HTMLDivElement | null>(null);
  const messages = useChatStore((state) => state.messages);
  const loadingInitial = useChatStore((state) => state.loadingInitial);
  const loadingMore = useChatStore((state) => state.loadingMore);
  const hasMore = useChatStore((state) => state.hasMore);
  const sending = useChatStore((state) => state.sending);
  const errorMessage = useChatStore((state) => state.errorMessage);
  const connectionStatus = useChatStore((state) => state.connectionStatus);
  const initRoom = useChatStore((state) => state.initRoom);
  const loadMore = useChatStore((state) => state.loadMore);
  const sendMessage = useChatStore((state) => state.sendMessage);

  useEffect(() => {
    void initRoom({
      publicKey,
      visitorSessionToken,
      conversationId,
      wsUrl,
    });
  }, [conversationId, initRoom, publicKey, visitorSessionToken, wsUrl]);

  useEffect(() => {
    const element = listRef.current;
    if (element) {
      element.scrollTop = element.scrollHeight;
    }
  }, [messages.length]);

  async function handleSend() {
    if (!message.trim()) {
      return;
    }
    await sendMessage(publicKey, visitorSessionToken, message);
    setMessage('');
  }

  return (
    <div className="flex flex-1 flex-col bg-gray-50">
      <div className="border-b border-gray-200 bg-white px-4 py-3">
        <div className="flex items-center justify-between gap-2">
          <div>
            <p className="text-xs text-gray-500">Channel</p>
            <p className="text-sm font-semibold text-gray-900">{roomName}</p>
          </div>
          <span
            className={`rounded-full px-2 py-1 text-xs font-medium ${
              connectionStatus === 'connected'
                ? 'bg-green-100 text-green-700'
                : connectionStatus === 'connecting'
                  ? 'bg-amber-100 text-amber-700'
                  : 'bg-gray-100 text-gray-600'
            }`}
          >
            {connectionStatus}
          </span>
        </div>
      </div>

      <div className="flex min-h-0 flex-1 flex-col gap-2 p-4">
        {loadingInitial && <div className="text-sm text-gray-500">Loading messages...</div>}
        {!loadingInitial && hasMore && (
          <button
            type="button"
            className="mx-auto block rounded-md border border-gray-300 px-3 py-1 text-xs text-gray-600"
            onClick={() => void loadMore(publicKey, visitorSessionToken)}
            disabled={loadingMore}
          >
            {loadingMore ? 'Loading...' : 'Load more'}
          </button>
        )}
        {messages.length === 0 && !loadingInitial && (
          <div className="rounded-lg bg-white px-3 py-2 text-sm text-gray-700 shadow-sm">
            Chat is ready. Start your conversation.
          </div>
        )}
        <div ref={listRef} className="flex flex-1 flex-col gap-2 overflow-y-auto">
          {messages.map((item) => {
            const isMine = item.senderType === 'VISITOR';
            return (
              <div key={item.id} className={`flex ${isMine ? 'justify-end' : 'justify-start'}`}>
                <div
                  className={`max-w-[80%] rounded-lg px-3 py-2 text-sm shadow-sm ${
                    isMine ? 'bg-blue-600 text-white' : 'bg-white text-gray-800'
                  }`}
                >
                  {item.content}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <div className="border-t border-gray-200 bg-white p-3">
        {errorMessage && <p className="mb-2 text-xs text-red-600">{errorMessage}</p>}
        <div className="flex gap-2">
          <input
            type="text"
            value={message}
            onChange={(event) => setMessage(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                void handleSend();
              }
            }}
            placeholder="Type a message..."
            className="flex-1 rounded-full border border-gray-300 px-4 py-2 text-sm text-gray-900 outline-none focus:border-blue-500"
          />
          <button
            type="button"
            onClick={() => void handleSend()}
            className="flex h-10 w-10 items-center justify-center rounded-full bg-blue-600 text-white transition hover:bg-blue-700 disabled:bg-blue-300"
            aria-label="Send message"
            disabled={sending}
          >
            <Send className="h-5 w-5" />
          </button>
        </div>
      </div>
    </div>
  );
}
