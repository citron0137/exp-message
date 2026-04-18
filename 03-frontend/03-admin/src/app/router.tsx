import { Navigate, createBrowserRouter } from 'react-router-dom';
import LoginPage from '@/pages/login/LoginPage';
import ChannelsPage from '@/pages/channels/ChannelsPage';
import ChannelDetailPage from '@/pages/channels/ChannelDetailPage';
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
      { path: '/channels/:channelId', element: <ChannelDetailPage /> },
      { path: '/inbox', element: <InboxPage /> },
    ],
  },
]);
