import React from 'react';
import ReactDOM from 'react-dom/client';
import Widget from './Widget';
import './styles/index.css';

// Global interface for widget configuration
interface WidgetConfig {
    apiUrl?: string;
    wsUrl?: string;
    theme?: 'light' | 'dark';
}

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

        // Inject Tailwind styles into Shadow DOM
        const style = document.createElement('style');
        style.textContent = `
      @import url('https://cdn.jsdelivr.net/npm/tailwindcss@3.4.17/dist/tailwind.min.css');
    `;
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
                            apiUrl: 'http://localhost:8080/api',
                            wsUrl: 'ws://localhost:8080/ws',
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
