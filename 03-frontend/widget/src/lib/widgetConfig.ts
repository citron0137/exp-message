export type WidgetConfig = {
    apiUrl?: string;
    wsUrl?: string;
    theme?: 'light' | 'dark';
};

let currentConfig: WidgetConfig = {};

export const setWidgetConfig = (config: WidgetConfig): void => {
    currentConfig = config;
};

export const getWidgetConfig = (): WidgetConfig => {
    return currentConfig;
};
