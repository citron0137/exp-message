import { apiRequest } from '@/shared/http/client';

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

export interface ChatMessage {
  id: string;
  conversationId: string;
  channelId: string;
  sequence: number;
  senderType: 'VISITOR' | 'AGENT' | 'SYSTEM';
  senderId: string;
  clientMessageId: string;
  type: string;
  content: string;
  status: string;
  createdAt: string;
}

interface MessageListResult {
  messages: ChatMessage[];
  nextAfterSequence: number;
  hasMore: boolean;
}

export async function getMessagesApi(
  publicKey: string,
  visitorSessionToken: string,
  conversationId: string,
  afterSequence = 0,
  limit = 50,
) {
  const payload = await apiRequest<ApiResponse<MessageListResult>>({
    method: 'GET',
    url: `/widget/conversations/${conversationId}/messages`,
    params: {
      publicKey,
      afterSequence,
      limit,
    },
    headers: {
      'X-Visitor-Session': visitorSessionToken,
    },
  });
  return unwrap(payload);
}

export async function sendMessageApi(
  publicKey: string,
  visitorSessionToken: string,
  conversationId: string,
  content: string,
): Promise<ChatMessage> {
  const payload = await apiRequest<ApiResponse<ChatMessage>>({
    method: 'POST',
    url: `/widget/conversations/${conversationId}/messages`,
    data: {
      publicKey,
      visitorSessionToken,
      clientMessageId: crypto.randomUUID(),
      content,
    },
  });

  return unwrap(payload);
}
