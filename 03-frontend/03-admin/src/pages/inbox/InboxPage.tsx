import { FormEvent, useEffect, useMemo, useState } from 'react';
import {
  changeConversationAssignee,
  changeConversationStatus,
  listConversationMessages,
  listConversations,
  listMemberships,
  sendAdminReply,
  type ChannelMembership,
  type ConversationListItem,
  type ConversationMessage,
  type ConversationStatus,
} from '@/shared/api/admin-core';
import { connectAdminConversationSocket } from '@/shared/api/admin-ws';
import { toApiErrorMessage } from '@/shared/api/http';
import { useSessionStore } from '@/shared/store/session-store';
import { useWorkspaceStore } from '@/shared/store/workspace-store';

const statuses: Array<ConversationStatus | ''> = ['', 'PENDING', 'OPEN', 'DORMANT', 'CLOSED'];

function formatDate(value: string | null) {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleString();
}

function visitorName(conversation: ConversationListItem) {
  return conversation.visitor.displayName || conversation.visitor.email || conversation.visitor.externalId || conversation.visitor.id;
}

export default function InboxPage() {
  const accessToken = useSessionStore((state) => state.accessToken);
  const activeChannelId = useWorkspaceStore((state) => state.activeChannelId);
  const workspaces = useWorkspaceStore((state) => state.channels);
  const loadWorkspaces = useWorkspaceStore((state) => state.loadChannels);
  const [status, setStatus] = useState<ConversationStatus | ''>('');
  const [unassigned, setUnassigned] = useState(false);
  const [assigneeMembershipId, setAssigneeMembershipId] = useState('');
  const [conversations, setConversations] = useState<ConversationListItem[]>([]);
  const [memberships, setMemberships] = useState<ChannelMembership[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [selectedConversationId, setSelectedConversationId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ConversationMessage[]>([]);
  const [reply, setReply] = useState('');
  const [socketState, setSocketState] = useState<'connecting' | 'connected' | 'disconnected' | 'error'>('disconnected');
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const selectedConversation = useMemo(
    () => conversations.find((item) => item.id === selectedConversationId) ?? null,
    [conversations, selectedConversationId],
  );
  const activeWorkspace = workspaces.find((item) => item.channel.id === activeChannelId) ?? null;

  async function loadMemberships(channelId: string) {
    const result = await listMemberships(channelId, { status: 'ACTIVE' });
    setMemberships(result.items);
  }

  async function loadConversations(channelId: string, cursor?: string | null, append = false) {
    setLoading(true);
    setErrorMessage(null);
    try {
      const result = await listConversations(channelId, {
        status,
        assigneeMembershipId: assigneeMembershipId || undefined,
        unassigned,
        cursor,
        limit: 30,
      });
      setConversations((current) => (append ? [...current, ...result.items] : result.items));
      setNextCursor(result.nextCursor);
      if (!append) {
        setSelectedConversationId(result.items[0]?.id ?? null);
      }
    } catch (error) {
      setErrorMessage(toApiErrorMessage(error, 'Unable to load conversations.'));
    } finally {
      setLoading(false);
    }
  }

  async function refreshConversationsSilently(channelId: string) {
    try {
      const result = await listConversations(channelId, {
        status,
        assigneeMembershipId: assigneeMembershipId || undefined,
        unassigned,
        limit: 30,
      });
      setConversations(result.items);
      setNextCursor(result.nextCursor);
      setSelectedConversationId((current) => current ?? result.items[0]?.id ?? null);
    } catch (error) {
      setErrorMessage(toApiErrorMessage(error, 'Unable to refresh conversations.'));
    }
  }

  async function loadMessages(channelId: string, conversationId: string) {
    setDetailLoading(true);
    setErrorMessage(null);
    try {
      const result = await listConversationMessages(channelId, conversationId, 0, 100);
      setMessages(result.messages);
    } catch (error) {
      setErrorMessage(toApiErrorMessage(error, 'Unable to load messages.'));
    } finally {
      setDetailLoading(false);
    }
  }

  useEffect(() => {
    if (!activeChannelId) {
      setConversations([]);
      setMessages([]);
      setSelectedConversationId(null);
      return;
    }
    setConversations([]);
    setMessages([]);
    setSelectedConversationId(null);
    void Promise.all([loadMemberships(activeChannelId), loadConversations(activeChannelId)]);
  }, [activeChannelId]);

  useEffect(() => {
    if (activeChannelId && selectedConversationId) {
      void loadMessages(activeChannelId, selectedConversationId);
    } else {
      setMessages([]);
    }
  }, [activeChannelId, selectedConversationId]);

  useEffect(() => {
    if (!activeChannelId || !accessToken) {
      setSocketState('disconnected');
      return undefined;
    }
    const client = connectAdminConversationSocket({
      channelId: activeChannelId,
      conversationId: selectedConversationId,
      accessToken,
      onInboxEvent: (event) => {
        if (event.channelId === activeChannelId) {
          void refreshConversationsSilently(activeChannelId);
        }
      },
      onMessage: (message) => {
        if (message.conversationId !== selectedConversationId) {
          return;
        }
        setMessages((current) => {
          if (current.some((item) => item.id === message.id || item.clientMessageId === message.clientMessageId)) {
            return current;
          }
          return [...current, message].sort((left, right) => left.sequence - right.sequence);
        });
      },
      onConnectionChange: setSocketState,
      onStompError: (frame) => {
        setErrorMessage(frame.headers.message ?? 'Realtime connection failed.');
      },
    });
    return () => client.disconnect();
  }, [activeChannelId, accessToken, selectedConversationId, status, assigneeMembershipId, unassigned]);

  async function refreshCurrent() {
    if (!activeChannelId) {
      return;
    }
    await Promise.all([loadMemberships(activeChannelId), loadConversations(activeChannelId)]);
  }

  async function mutateConversation(action: () => Promise<unknown>, fallback: string) {
    if (!activeChannelId || !selectedConversationId) {
      return;
    }
    setErrorMessage(null);
    try {
      await action();
      await loadConversations(activeChannelId);
      await loadMessages(activeChannelId, selectedConversationId);
    } catch (error) {
      setErrorMessage(toApiErrorMessage(error, fallback));
    }
  }

  async function onApplyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (activeChannelId) {
      await loadConversations(activeChannelId);
    }
  }

  async function onReply(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!activeChannelId || !selectedConversationId || !reply.trim()) {
      return;
    }
    const content = reply.trim();
    setReply('');
    await mutateConversation(() => sendAdminReply(activeChannelId, selectedConversationId, content), 'Unable to send reply.');
  }

  if (!activeChannelId) {
    return (
      <section className="flex min-h-[520px] items-center justify-center rounded-lg border border-zinc-200 bg-white p-8 text-center shadow-sm">
        <div>
          <h2 className="text-lg font-semibold text-zinc-950">No workspace selected</h2>
          <p className="mt-2 text-sm text-zinc-600">Select a workspace from the sidebar or refresh your available channels.</p>
          <button className="mt-4 rounded-lg bg-zinc-950 px-4 py-2 text-sm font-semibold text-white" onClick={() => void loadWorkspaces()}>
            Refresh workspaces
          </button>
        </div>
      </section>
    );
  }

  return (
    <div className="grid h-[calc(100vh-104px)] min-h-[680px] overflow-hidden rounded-lg border border-zinc-200 bg-white shadow-sm xl:grid-cols-[360px_1fr]">
      <section className="flex min-h-0 flex-col border-r border-zinc-200">
        <div className="border-b border-zinc-200 p-4">
          <div className="flex items-start justify-between gap-3">
            <div>
              <div className="flex items-center gap-2">
                <p className="text-xs uppercase text-zinc-500">Inbox</p>
                <span
                  className={`rounded-lg px-2 py-1 text-xs font-semibold ${
                    socketState === 'connected'
                      ? 'bg-emerald-100 text-emerald-700'
                      : socketState === 'error'
                        ? 'bg-red-100 text-red-700'
                        : 'bg-zinc-200 text-zinc-600'
                  }`}
                >
                  {socketState === 'connected' ? 'Live' : socketState}
                </span>
              </div>
              <h2 className="mt-1 text-lg font-semibold text-zinc-950">{activeWorkspace?.channel.name ?? 'Workspace'}</h2>
            </div>
            <button className="rounded-lg border border-zinc-300 px-3 py-2 text-xs font-semibold" type="button" onClick={() => void refreshCurrent()}>
              Refresh
            </button>
          </div>
          <form className="mt-4 grid grid-cols-2 gap-2" onSubmit={onApplyFilters}>
            <select
              value={status}
              onChange={(event) => setStatus(event.target.value as ConversationStatus | '')}
              className="rounded-lg border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-zinc-700"
            >
              {statuses.map((item) => (
                <option key={item || 'ALL'} value={item}>
                  {item || 'ALL'}
                </option>
              ))}
            </select>
            <select
              value={assigneeMembershipId}
              onChange={(event) => {
                setAssigneeMembershipId(event.target.value);
                setUnassigned(false);
              }}
              className="rounded-lg border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-zinc-700"
            >
              <option value="">Any assignee</option>
              {memberships.map((membership) => (
                <option key={membership.id} value={membership.id}>
                  {membership.role} {membership.id.slice(0, 8)}
                </option>
              ))}
            </select>
            <label className="flex items-center gap-2 rounded-lg border border-zinc-300 px-3 py-2 text-sm">
              <input
                type="checkbox"
                checked={unassigned}
                onChange={(event) => {
                  setUnassigned(event.target.checked);
                  setAssigneeMembershipId('');
                }}
              />
              Unassigned
            </label>
            <button className="rounded-lg bg-zinc-950 px-4 py-2 text-sm font-semibold text-white" type="submit">
              Apply
            </button>
          </form>
          {errorMessage && <p className="mt-3 text-sm text-red-600">{errorMessage}</p>}
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto">
          {loading && <p className="p-4 text-sm text-zinc-500">Loading conversations...</p>}
          {conversations.map((conversation) => (
            <button
              key={conversation.id}
              type="button"
              onClick={() => setSelectedConversationId(conversation.id)}
              className={`block w-full border-b border-zinc-200 p-4 text-left hover:bg-zinc-50 ${
                selectedConversationId === conversation.id ? 'bg-zinc-100' : ''
              }`}
            >
              <div className="flex items-center justify-between gap-2">
                <p className="truncate text-sm font-semibold text-zinc-950">{visitorName(conversation)}</p>
                <span
                  className={`rounded-lg px-2 py-1 text-xs font-semibold ${
                    conversation.status === 'OPEN'
                      ? 'bg-emerald-100 text-emerald-700'
                      : conversation.status === 'PENDING'
                        ? 'bg-amber-100 text-amber-800'
                        : conversation.status === 'DORMANT'
                          ? 'bg-sky-100 text-sky-700'
                          : 'bg-zinc-200 text-zinc-700'
                  }`}
                >
                  {conversation.status}
                </span>
              </div>
              <p className="mt-2 line-clamp-2 text-sm text-zinc-600">{conversation.lastMessage?.content || 'No messages yet.'}</p>
              <p className="mt-2 text-xs text-zinc-400">{formatDate(conversation.lastMessageAt)}</p>
            </button>
          ))}
          {conversations.length === 0 && !loading && <p className="p-4 text-sm text-zinc-500">No conversations found.</p>}
          {nextCursor && (
            <button
              type="button"
              onClick={() => void loadConversations(activeChannelId, nextCursor, true)}
              className="w-full p-3 text-sm font-semibold text-zinc-900 underline"
            >
              Load more
            </button>
          )}
        </div>
      </section>

      <section className="flex min-h-0 flex-col bg-zinc-50">
        {selectedConversation ? (
          <>
            <div className="border-b border-zinc-200 bg-white p-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <h2 className="text-base font-semibold text-zinc-950">{visitorName(selectedConversation)}</h2>
                  <p className="break-all text-xs text-zinc-500">{selectedConversation.id}</p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <select
                    value={selectedConversation.status}
                    onChange={(event) =>
                      void mutateConversation(
                        () => changeConversationStatus(activeChannelId, selectedConversation.id, event.target.value as ConversationStatus),
                        'Unable to change status.',
                      )
                    }
                    className="rounded-lg border border-zinc-300 px-3 py-2 text-sm"
                    disabled={selectedConversation.status === 'CLOSED'}
                  >
                    <option value="PENDING">PENDING</option>
                    <option value="OPEN">OPEN</option>
                    <option value="DORMANT">DORMANT</option>
                    <option value="CLOSED">CLOSED</option>
                  </select>
                  <select
                    value={selectedConversation.assignee?.membershipId ?? ''}
                    onChange={(event) =>
                      void mutateConversation(
                        () => changeConversationAssignee(activeChannelId, selectedConversation.id, event.target.value || null),
                        'Unable to change assignee.',
                      )
                    }
                    className="rounded-lg border border-zinc-300 px-3 py-2 text-sm"
                    disabled={selectedConversation.status === 'CLOSED'}
                  >
                    <option value="">Unassigned</option>
                    {memberships.map((membership) => (
                      <option key={membership.id} value={membership.id}>
                        {membership.role} {membership.id.slice(0, 8)}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            </div>

            <div className="flex min-h-0 flex-1 flex-col gap-3 overflow-y-auto p-5">
              {detailLoading && <p className="text-sm text-zinc-500">Loading messages...</p>}
              {messages.map((message) => (
                <div key={message.id} className={`flex ${message.senderType === 'VISITOR' ? 'justify-start' : 'justify-end'}`}>
                  <div
                    className={`max-w-[72%] rounded-lg px-3 py-2 text-sm shadow-sm ${
                      message.senderType === 'VISITOR' ? 'bg-white text-zinc-800' : 'bg-zinc-950 text-white'
                    }`}
                  >
                    <p>{message.content}</p>
                    <p className="mt-1 text-xs opacity-70">{formatDate(message.createdAt)}</p>
                  </div>
                </div>
              ))}
              {messages.length === 0 && !detailLoading && <p className="text-sm text-zinc-500">No messages yet.</p>}
            </div>

            <form className="border-t border-zinc-200 bg-white p-4" onSubmit={onReply}>
              <div className="flex gap-2">
                <input
                  value={reply}
                  onChange={(event) => setReply(event.target.value)}
                  placeholder={selectedConversation.status === 'CLOSED' ? 'Closed conversations cannot receive replies.' : 'Reply'}
                  disabled={selectedConversation.status === 'CLOSED'}
                  className="flex-1 rounded-lg border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-zinc-700 disabled:bg-zinc-100"
                />
                <button
                  type="submit"
                  disabled={selectedConversation.status === 'CLOSED'}
                  className="rounded-lg bg-zinc-950 px-4 py-2 text-sm font-semibold text-white disabled:bg-zinc-400"
                >
                  Send
                </button>
              </div>
            </form>
          </>
        ) : (
          <div className="flex flex-1 items-center justify-center p-6 text-sm text-zinc-500">Select a conversation.</div>
        )}
      </section>
    </div>
  );
}
