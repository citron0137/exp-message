import { useEffect, useRef } from 'react';
import MessageBubble from './MessageBubble';

// Temporary mock data for development
const mockMessages = [
    {
        id: '1',
        content: 'Hello! How can I help you today?',
        senderId: 'support',
        senderName: 'Support',
        timestamp: new Date(Date.now() - 3600000),
        isOwn: false,
    },
    {
        id: '2',
        content: 'Hi! I have a question about your service.',
        senderId: 'user',
        senderName: 'You',
        timestamp: new Date(Date.now() - 3000000),
        isOwn: true,
    },
    {
        id: '3',
        content: 'Of course! I\'d be happy to help. What would you like to know?',
        senderId: 'support',
        senderName: 'Support',
        timestamp: new Date(Date.now() - 2400000),
        isOwn: false,
    },
];

export default function MessageList() {
    const messagesEndRef = useRef<HTMLDivElement>(null);

    // Auto-scroll to bottom when new messages arrive
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [mockMessages]);

    return (
        <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-gray-50">
            {mockMessages.map((message) => (
                <MessageBubble key={message.id} message={message} />
            ))}
            <div ref={messagesEndRef} />
        </div>
    );
}
