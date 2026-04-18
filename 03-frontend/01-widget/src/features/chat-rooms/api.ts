import { apiRequest } from '@/shared/http/client';
import type { WidgetConfig } from '@/types/config';

interface ApiResponse<T> {
  success: boolean;
  data?: T | null;
  error?: {
    message?: string;
  } | null;
}

function unwrap<T>(payload: ApiResponse<T>): T {
  if (!payload.success || payload.data == null) {
    throw new Error(payload.error?.message ?? 'Request failed.');
  }
  return payload.data;
}

export interface WidgetBootstrap {
  channel: {
    id: string;
    name: string;
  };
  integration: {
    id: string;
    type: string;
    publicKey: string;
  };
}

export interface VisitorSession {
  visitor: {
    id: string;
    channelId: string;
    externalId: string | null;
    displayName: string | null;
    email: string | null;
    metadata: Record<string, string>;
  };
  session: {
    token: string;
    expiresAt: string;
  };
}

export interface WidgetConversationEntry {
  visitor: VisitorSession['visitor'];
  conversation: {
    id: string;
    channelId: string;
    visitorId: string;
    status: string;
  };
}

export async function bootstrapWidget(publicKey: string) {
  const payload = await apiRequest<ApiResponse<WidgetBootstrap>>({
    method: 'POST',
    url: '/widget/bootstrap',
    data: { publicKey },
  });
  return unwrap(payload);
}

export async function createVisitorSession(publicKey: string, visitor?: WidgetConfig['visitor']) {
  const payload = await apiRequest<ApiResponse<VisitorSession>>({
    method: 'POST',
    url: '/widget/visitor-sessions',
    data: { publicKey, visitor },
  });
  return unwrap(payload);
}

export async function enterConversation(publicKey: string, visitorSessionToken: string) {
  const payload = await apiRequest<ApiResponse<WidgetConversationEntry>>({
    method: 'POST',
    url: '/widget/conversations',
    data: { publicKey, visitorSessionToken },
  });
  return unwrap(payload);
}
