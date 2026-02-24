/// <reference types="vite/client" />

// CSS module type declarations
declare module '*.css' {
    const content: string;
    export default content;
}

// Vite environment variables
interface ImportMetaEnv {
    readonly DEV: boolean;
    readonly PROD: boolean;
    readonly SSR: boolean;
    readonly WIDGET_API_BASE_URL?: string;
    readonly WIDGET_API_WEBSOCKET_URL?: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}

// Runtime configuration injected by Docker
interface WidgetRuntimeConfig {
    apiUrl: string;
    wsUrl: string;
    timeout: number;
    containerId: string;
    debug: boolean;
}

declare global {
    interface Window {
        __WIDGET_CONFIG__?: WidgetRuntimeConfig;
    }
}

export { };
