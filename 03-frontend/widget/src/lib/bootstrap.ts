import { initApiClient } from './api';
import { setWidgetConfig, type WidgetConfig } from './widgetConfig';

/**
 * Widget bootstrap entry.
 *
 * This file is intentionally small and side-effectful:
 * - It stores the widget runtime config (apiUrl/wsUrl/theme)
 * - It initializes the shared Axios API client using `apiUrl`
 *
 * Every API wrapper (e.g. auth/chatRoom/message APIs) assumes `initApiClient()` was called.
 */
export const bootstrap = (config: WidgetConfig): void => {
    setWidgetConfig(config);

    const apiUrl = config.apiUrl;
    if (!apiUrl) {
        throw new Error('Missing config.apiUrl');
    }

    initApiClient(apiUrl);
};
