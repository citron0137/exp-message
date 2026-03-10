import { Navigate, createBrowserRouter } from 'react-router-dom';
import LoginPage from '@/pages/login/LoginPage';
import ChannelsPage from '@/pages/channels/ChannelsPage';
import InboxPage from '@/pages/inbox/InboxPage';
import { AuthGuard } from '@/features/auth/AuthGuard';
import { AdminLayout } from '@/app/layouts/AdminLayout';

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/',
    element: (
      <AuthGuard>
        <AdminLayout />
      </AuthGuard>
    ),
    children: [
      { index: true, element: <Navigate to="/channels" replace /> },
      { path: '/channels', element: <ChannelsPage /> },
      { path: '/inbox', element: <InboxPage /> },
    ],
  },
]);
