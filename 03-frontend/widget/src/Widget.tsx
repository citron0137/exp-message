import { useState } from 'react';
import FloatingButton from './components/FloatingButton';
import ChatWindow from './components/ChatWindow';
import type { WidgetConfig } from './lib/widgetConfig';


interface WidgetProps {
    config: WidgetConfig;
}

export default function Widget({ config }: WidgetProps) {
    const [isOpen, setIsOpen] = useState(false);

    return (
        <div className="chat-widget">
            <FloatingButton onClick={() => setIsOpen(!isOpen)} isOpen={isOpen} />
            {isOpen && <ChatWindow onClose={() => setIsOpen(false)} config={config} />}
        </div>
    );
}
