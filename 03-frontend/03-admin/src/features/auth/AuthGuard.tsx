import { PropsWithChildren } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useSessionStore } from '@/shared/store/session-store';

export function AuthGuard({ children }: PropsWithChildren) {
  const status = useSessionStore((state) => state.status);
  const location = useLocation();

  if (status === 'loading') {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-100 text-sm text-slate-600">
        Checking session...
      </div>
    );
  }

  if (status !== 'authenticated') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return <>{children}</>;
}
