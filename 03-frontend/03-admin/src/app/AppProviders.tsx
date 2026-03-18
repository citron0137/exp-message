import { PropsWithChildren, useEffect } from 'react';
import { QueryClientProvider } from '@tanstack/react-query';
import { queryClient } from '@/shared/lib/query-client';
import { configureHttpAuth } from '@/shared/api/http';
import { useSessionStore } from '@/shared/store/session-store';

export function AppProviders({ children }: PropsWithChildren) {
  useEffect(() => {
    configureHttpAuth({
      getAccessToken: () => useSessionStore.getState().accessToken,
      refreshAccessToken: () => useSessionStore.getState().refresh(),
      onUnauthorized: () => useSessionStore.getState().clearSession(),
    });

    void useSessionStore.getState().bootstrap();
  }, []);

  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
