import ChatHeader from './ChatHeader';
import MessageList from './MessageList';
import MessageInput from './MessageInput';

interface ChatWindowProps {
    onClose: () => void;
    config: {
        apiUrl?: string;
        wsUrl?: string;
        theme?: 'light' | 'dark';
    };
}

export default function ChatWindow({ onClose, config }: ChatWindowProps) {
    return (
        <div className="fixed bottom-20 right-5 w-96 h-[600px] bg-white rounded-xl shadow-2xl flex flex-col overflow-hidden z-[9998]">
            <ChatHeader />
            <MessageList />
            <MessageInput />
        </div>
    );
}
