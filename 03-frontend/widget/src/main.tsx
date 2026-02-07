import React from 'react';
import ReactDOM from 'react-dom/client';
import Widget from './Widget';
import widgetStyles from './styles/index.css?inline';
import type { WidgetConfig } from './lib/widgetConfig';

// Expose widget initialization function globally
declare global {
    interface Window {
        ChatWidget: {
            init: (config?: WidgetConfig) => void;
        };
    }
}

window.ChatWidget = {
    init: (config: WidgetConfig = {}) => {
        // Create widget container
        const container = document.createElement('div');
        container.id = 'chat-widget-root';
        container.style.cssText = 'position: fixed; bottom: 0; right: 0; z-index: 9999;';
        document.body.appendChild(container);

        // Use Shadow DOM for style isolation
        const shadowRoot = container.attachShadow({ mode: 'open' });
        const mountPoint = document.createElement('div');
        shadowRoot.appendChild(mountPoint);

        // Inject built styles into Shadow DOM
        const style = document.createElement('style');
        style.textContent = widgetStyles;
        shadowRoot.appendChild(style);

        // Render React widget
        ReactDOM.createRoot(mountPoint).render(
            <React.StrictMode>
                <Widget config={config} />
            </React.StrictMode>
        );
    },
};

// Auto-initialize for development
if (import.meta.env.DEV) {
    const initDev = () => {
        const root = document.getElementById('root');
        if (root) {
            ReactDOM.createRoot(root).render(
                <React.StrictMode>
                    <Widget
                        config={{
                            apiUrl: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api',
                            wsUrl: import.meta.env.VITE_WS_URL ?? 'ws://localhost:8080/ws',
                        }}
                    />
                </React.StrictMode>
            );
        }
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initDev);
    } else {
        initDev();
    }
}
