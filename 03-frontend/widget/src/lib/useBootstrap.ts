import { useEffect, useState } from 'react';
import { bootstrap } from './bootstrap';
import type { WidgetConfig } from './widgetConfig';

export const useBootstrap = (config: WidgetConfig) => {
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        try {
            bootstrap(config);
            setError(null);
        } catch (e) {
            const message = e instanceof Error ? e.message : 'Failed to initialize widget';
            setError(message);
        }
    }, [config]);

    return { error };
};
