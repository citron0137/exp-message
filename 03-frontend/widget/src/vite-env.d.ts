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
    readonly VITE_ENV?: string;
    readonly VITE_API_URL?: string;
    readonly VITE_WS_URL?: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}
