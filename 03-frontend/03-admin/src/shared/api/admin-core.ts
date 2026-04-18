import { coreRequest } from '@/shared/api/http';

export type ChannelStatus = 'ACTIVE' | 'DISABLED';
export type ChannelIntegrationStatus = 'ACTIVE' | 'DISABLED';
export type ChannelMembershipRole = 'CHANNEL_ADMIN' | 'AGENT';
export type ChannelMembershipStatus = 'ACTIVE' | 'DISABLED';
export type ConversationStatus = 'PENDING' | 'OPEN' | 'DORMANT' | 'CLOSED';

export interface Channel {
  id: string;
  name: string;
  status: ChannelStatus;
  createdAt: string;
  updatedAt: string;
}

export interface ChannelIntegration {
  id: string;
  channelId: string;
  type: string;
  publicKey: string;
  status: ChannelIntegrationStatus;
  allowedOrigins: string[];
  createdAt: string;
  updatedAt: string;
}

export interface ChannelMembership {
  id: string;
  channelId: string;
  userId: string;
  role: ChannelMembershipRole;
  agentStatus: string;
  status: ChannelMembershipStatus;
  createdAt: string;
  updatedAt: string;
}

export interface WorkspaceChannel {
  channel: Channel;
  membership: ChannelMembership | null;
}

export interface IdentityResult {
  userId: string;
  email: string;
  nickname: string;
  temporaryPassword: string | null;
  created: boolean;
}

export interface ConversationListItem {
  id: string;
  channelId: string;
  visitor: {
    id: string;
    externalId: string | null;
    displayName: string | null;
    email: string | null;
  };
  assignee: {
    membershipId: string;
    userId: string;
    role: ChannelMembershipRole;
    agentStatus: string;
  } | null;
  status: ConversationStatus;
  lastMessageSequence: number;
  lastMessageAt: string | null;
  lastMessage: {
    id: string;
    sequence: number | null;
    senderType: string | null;
    content: string | null;
    createdAt: string | null;
  } | null;
  createdAt: string;
  updatedAt: string;
  closedAt: string | null;
}

export interface ConversationDetail {
  id: string;
  channelId: string;
  visitor: ConversationListItem['visitor'];
  assignee: ConversationListItem['assignee'];
  status: ConversationStatus;
  lastMessageSequence: number;
  lastMessageAt: string | null;
  createdAt: string;
  updatedAt: string;
  closedAt: string | null;
}

export interface ConversationMessage {
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

export interface ConversationChangedEvent {
  channelId: string;
  conversationId: string;
  reason: string;
  lastMessageSequence: number;
  occurredAt: string;
}

export async function listChannels() {
  return coreRequest<{ items: Channel[] }>({
    method: 'GET',
    url: '/admin/channels',
  });
}

export async function listMyChannels() {
  return coreRequest<{ items: WorkspaceChannel[] }>({
    method: 'GET',
    url: '/admin/me/channels',
  });
}

export async function createChannel(input: { name: string; adminEmail: string; adminNickname: string }) {
  return coreRequest<{ channel: Channel; initialAdmin: IdentityResult }>({
    method: 'POST',
    url: '/admin/channels',
    data: input,
  });
}

export async function getChannel(channelId: string) {
  return coreRequest<Channel>({
    method: 'GET',
    url: `/admin/channels/${channelId}`,
  });
}

export async function listIntegrations(channelId: string) {
  return coreRequest<{ items: ChannelIntegration[] }>({
    method: 'GET',
    url: `/admin/channels/${channelId}/integrations`,
  });
}

export async function createWidgetIntegration(channelId: string, allowedOrigins: string[]) {
  return coreRequest<{ integration: ChannelIntegration; secret: string }>({
    method: 'POST',
    url: `/admin/channels/${channelId}/integrations/widget`,
    data: { allowedOrigins },
  });
}

export async function enableIntegration(channelId: string, integrationId: string) {
  return coreRequest<ChannelIntegration>({
    method: 'PATCH',
    url: `/admin/channels/${channelId}/integrations/${integrationId}/enable`,
  });
}

export async function disableIntegration(channelId: string, integrationId: string) {
  return coreRequest<ChannelIntegration>({
    method: 'PATCH',
    url: `/admin/channels/${channelId}/integrations/${integrationId}/disable`,
  });
}

export async function updateIntegrationOrigins(channelId: string, integrationId: string, allowedOrigins: string[]) {
  return coreRequest<ChannelIntegration>({
    method: 'PATCH',
    url: `/admin/channels/${channelId}/integrations/${integrationId}/allowed-origins`,
    data: { allowedOrigins },
  });
}

export async function listMemberships(channelId: string, params?: { role?: string; status?: string }) {
  return coreRequest<{ items: ChannelMembership[] }>({
    method: 'GET',
    url: `/admin/channels/${channelId}/memberships`,
    params,
  });
}

export async function createMembership(
  channelId: string,
  input: { email: string; nickname: string; role: ChannelMembershipRole },
) {
  return coreRequest<{ membership: ChannelMembership; identity: IdentityResult }>({
    method: 'POST',
    url: `/admin/channels/${channelId}/memberships`,
    data: input,
  });
}

export async function changeMembershipRole(channelId: string, membershipId: string, role: ChannelMembershipRole) {
  return coreRequest<ChannelMembership>({
    method: 'PATCH',
    url: `/admin/channels/${channelId}/memberships/${membershipId}/role`,
    data: { role },
  });
}

export async function enableMembership(channelId: string, membershipId: string) {
  return coreRequest<ChannelMembership>({
    method: 'PATCH',
    url: `/admin/channels/${channelId}/memberships/${membershipId}/enable`,
  });
}

export async function disableMembership(channelId: string, membershipId: string) {
  return coreRequest<ChannelMembership>({
    method: 'PATCH',
    url: `/admin/channels/${channelId}/memberships/${membershipId}/disable`,
  });
}

export async function listConversations(
  channelId: string,
  params?: {
    status?: ConversationStatus | '';
    assigneeMembershipId?: string;
    unassigned?: boolean;
    cursor?: string | null;
    limit?: number;
  },
) {
  return coreRequest<{ items: ConversationListItem[]; nextCursor: string | null; hasMore: boolean }>({
    method: 'GET',
    url: `/admin/channels/${channelId}/conversations`,
    params: {
      ...params,
      status: params?.status || undefined,
      cursor: params?.cursor ?? undefined,
    },
  });
}

export async function getConversation(channelId: string, conversationId: string) {
  return coreRequest<ConversationDetail>({
    method: 'GET',
    url: `/admin/channels/${channelId}/conversations/${conversationId}`,
  });
}

export async function listConversationMessages(channelId: string, conversationId: string, afterSequence = 0, limit = 50) {
  return coreRequest<{ messages: ConversationMessage[]; nextAfterSequence: number; hasMore: boolean }>({
    method: 'GET',
    url: `/admin/channels/${channelId}/conversations/${conversationId}/messages`,
    params: { afterSequence, limit },
  });
}

export async function changeConversationStatus(channelId: string, conversationId: string, status: ConversationStatus) {
  return coreRequest<ConversationDetail>({
    method: 'PATCH',
    url: `/admin/channels/${channelId}/conversations/${conversationId}/status`,
    data: { status },
  });
}

export async function changeConversationAssignee(channelId: string, conversationId: string, assigneeMembershipId: string | null) {
  return coreRequest<ConversationDetail>({
    method: 'PATCH',
    url: `/admin/channels/${channelId}/conversations/${conversationId}/assignee`,
    data: { assigneeMembershipId },
  });
}

export async function sendAdminReply(channelId: string, conversationId: string, content: string) {
  return coreRequest<ConversationMessage>({
    method: 'POST',
    url: `/admin/channels/${channelId}/conversations/${conversationId}/messages`,
    data: {
      clientMessageId: crypto.randomUUID(),
      content,
    },
  });
}
