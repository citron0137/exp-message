import { format } from 'date-fns';

interface Message {
  id: string;
  content: string;
  senderId: string;
  senderName: string;
  timestamp: Date;
  isOwn: boolean;
}

interface MessageBubbleProps {
  message: Message;
}

export default function MessageBubble({ message }: MessageBubbleProps) {
  const { content, senderName, timestamp, isOwn } = message;

  return (
    <div className={`flex ${isOwn ? 'justify-end' : 'justify-start'}`}>
      <div className={`max-w-[70%] ${isOwn ? 'items-end' : 'items-start'} flex flex-col gap-1`}>
        {!isOwn && <span className="text-xs text-gray-600 px-2">{senderName}</span>}
        <div
          className={`px-4 py-2 rounded-2xl ${
            isOwn
              ? 'bg-blue-600 text-white rounded-br-sm'
              : 'bg-white text-gray-800 rounded-bl-sm shadow-sm'
          }`}
        >
          <p className="text-sm whitespace-pre-wrap break-words">{content}</p>
        </div>
        <span className="text-xs text-gray-500 px-2">{format(timestamp, 'HH:mm')}</span>
      </div>
    </div>
  );
}
