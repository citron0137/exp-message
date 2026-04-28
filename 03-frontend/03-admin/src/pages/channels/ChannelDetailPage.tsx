import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  changeMembershipRole,
  createMembership,
  createWidgetIntegration,
  disableIntegration,
  disableMembership,
  enableIntegration,
  enableMembership,
  getChannel,
  listIntegrations,
  listMemberships,
  updateIntegrationOrigins,
  type Channel,
  type ChannelIntegration,
  type ChannelMembership,
  type ChannelMembershipRole,
  type IdentityResult,
} from '@/shared/api/admin-core';
import { toApiErrorMessage } from '@/shared/api/http';

function splitOrigins(raw: string) {
  return raw
    .split('\n')
    .map((item) => item.trim())
    .filter(Boolean);
}

function CopyButton({ value, label = 'Copy' }: { value: string; label?: string }) {
  return (
    <button
      type="button"
      onClick={() => void navigator.clipboard.writeText(value)}
      className="rounded-lg border border-emerald-300 px-3 py-2 text-xs font-semibold text-emerald-700 hover:bg-emerald-50"
    >
      {label}
    </button>
  );
}

export default function ChannelDetailPage() {
  const { channelId } = useParams();
  const [channel, setChannel] = useState<Channel | null>(null);
  const [integrations, setIntegrations] = useState<ChannelIntegration[]>([]);
  const [memberships, setMemberships] = useState<ChannelMembership[]>([]);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [integrationSecret, setIntegrationSecret] = useState<string | null>(null);
  const [createdIdentity, setCreatedIdentity] = useState<IdentityResult | null>(null);
  const [origins, setOrigins] = useState('*');
  const [memberEmail, setMemberEmail] = useState('');
  const [memberNickname, setMemberNickname] = useState('');
  const [memberRole, setMemberRole] = useState<ChannelMembershipRole>('AGENT');

  const widgetIntegration = useMemo(() => integrations.find((item) => item.type === 'WIDGET') ?? null, [integrations]);
  const embedSnippet = widgetIntegration
    ? `<script src="https://cdn.example.com/exp-message/widget.iife.js" data-public-key="${widgetIntegration.publicKey}"></script>`
    : '';

  async function loadAll() {
    if (!channelId) {
      return;
    }
    setLoading(true);
    setErrorMessage(null);
    try {
      const [nextChannel, nextIntegrations, nextMemberships] = await Promise.all([
        getChannel(channelId),
        listIntegrations(channelId),
        listMemberships(channelId),
      ]);
      setChannel(nextChannel);
      setIntegrations(nextIntegrations.items);
      setMemberships(nextMemberships.items);
      setOrigins(nextIntegrations.items.find((item) => item.type === 'WIDGET')?.allowedOrigins.join('\n') || '*');
    } catch (error) {
      setErrorMessage(toApiErrorMessage(error, 'Unable to load channel detail.'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadAll();
  }, [channelId]);

  async function mutate(action: () => Promise<unknown>, fallback: string) {
    setErrorMessage(null);
    try {
      await action();
      await loadAll();
    } catch (error) {
      setErrorMessage(toApiErrorMessage(error, fallback));
    }
  }

  async function onCreateWidget(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!channelId) {
      return;
    }
    setIntegrationSecret(null);
    await mutate(async () => {
      const result = await createWidgetIntegration(channelId, splitOrigins(origins));
      setIntegrationSecret(result.secret);
    }, 'Unable to create widget integration.');
  }

  async function onUpdateOrigins(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!channelId || !widgetIntegration) {
      return;
    }
    await mutate(() => updateIntegrationOrigins(channelId, widgetIntegration.id, splitOrigins(origins)), 'Unable to update origins.');
  }

  async function onCreateMembership(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!channelId || !memberEmail.trim() || !memberNickname.trim()) {
      return;
    }
    setCreatedIdentity(null);
    await mutate(async () => {
      const result = await createMembership(channelId, {
        email: memberEmail.trim(),
        nickname: memberNickname.trim(),
        role: memberRole,
      });
      setCreatedIdentity(result.identity);
      setMemberEmail('');
      setMemberNickname('');
      setMemberRole('AGENT');
    }, 'Unable to create membership.');
  }

  if (!channelId) {
    return <p className="text-sm text-red-600">Channel id is missing.</p>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <div>
          <Link className="text-sm font-semibold text-slate-700 underline" to="/channels">
            Back to channels
          </Link>
          <h2 className="mt-2 text-xl font-semibold text-slate-900">{channel?.name ?? 'Channel'}</h2>
          <p className="break-all text-xs text-slate-500">{channelId}</p>
        </div>
        <button
          type="button"
          onClick={() => void loadAll()}
          className="rounded-lg border border-slate-300 px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50"
        >
          Refresh
        </button>
      </div>

      {loading && <p className="text-sm text-slate-500">Loading...</p>}
      {errorMessage && <p className="text-sm text-red-600">{errorMessage}</p>}

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="rounded-lg border border-zinc-200 bg-white p-5 shadow-sm">
          <div className="flex items-start justify-between gap-3">
            <div>
              <h3 className="text-base font-semibold text-zinc-950">Widget Integration</h3>
              <p className="mt-1 text-sm text-zinc-500">Use this key in the customer chat widget.</p>
            </div>
            {widgetIntegration && (
              <span
                className={`rounded-lg px-2 py-1 text-xs font-semibold ${
                  widgetIntegration.status === 'ACTIVE' ? 'bg-emerald-100 text-emerald-700' : 'bg-zinc-200 text-zinc-700'
                }`}
              >
                {widgetIntegration.status}
              </span>
            )}
          </div>
          {widgetIntegration ? (
            <div className="mt-3 space-y-3 text-sm">
              <div className="rounded-lg border border-zinc-200 bg-zinc-50 p-3">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-xs font-semibold text-zinc-500">Public key</p>
                    <p className="mt-1 break-all font-mono text-xs text-zinc-800">{widgetIntegration.publicKey}</p>
                  </div>
                  <CopyButton value={widgetIntegration.publicKey} />
                </div>
              </div>
              <div className="rounded-lg border border-zinc-200 bg-zinc-50 p-3">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-xs font-semibold text-zinc-500">Embed snippet</p>
                    <p className="mt-1 break-all font-mono text-xs text-zinc-800">{embedSnippet}</p>
                  </div>
                  <CopyButton value={embedSnippet} />
                </div>
              </div>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => void mutate(() => enableIntegration(channelId, widgetIntegration.id), 'Unable to enable integration.')}
                  className="rounded-lg border border-zinc-300 px-3 py-2 text-xs font-semibold text-zinc-700 hover:bg-zinc-50"
                >
                  Enable
                </button>
                <button
                  type="button"
                  onClick={() => void mutate(() => disableIntegration(channelId, widgetIntegration.id), 'Unable to disable integration.')}
                  className="rounded-lg border border-zinc-300 px-3 py-2 text-xs font-semibold text-zinc-700 hover:bg-zinc-50"
                >
                  Disable
                </button>
              </div>
              <form className="space-y-2" onSubmit={onUpdateOrigins}>
                <label className="block">
                  <span className="mb-1 block text-xs font-semibold text-zinc-600">Allowed origins</span>
                  <textarea
                    value={origins}
                    onChange={(event) => setOrigins(event.target.value)}
                    rows={4}
                    className="w-full rounded-lg border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-zinc-700"
                  />
                </label>
                <button className="rounded-lg bg-zinc-950 px-4 py-2 text-sm font-semibold text-white" type="submit">
                  Save origins
                </button>
              </form>
            </div>
          ) : (
            <form className="mt-3 space-y-3" onSubmit={onCreateWidget}>
              <label className="block">
                <span className="mb-1 block text-xs font-semibold text-zinc-600">Allowed origins</span>
                <textarea
                  value={origins}
                  onChange={(event) => setOrigins(event.target.value)}
                  rows={4}
                  className="w-full rounded-lg border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-zinc-700"
                />
              </label>
              <button className="rounded-lg bg-zinc-950 px-4 py-2 text-sm font-semibold text-white" type="submit">
                Create widget integration
              </button>
            </form>
          )}
          {integrationSecret && (
            <div className="mt-3 rounded-lg border border-amber-300 bg-amber-50 p-3">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-xs font-semibold text-amber-900">Secret</p>
                  <p className="mt-1 break-all font-mono text-xs text-amber-900">{integrationSecret}</p>
                </div>
                <CopyButton value={integrationSecret} label="Copy secret" />
              </div>
            </div>
          )}
        </section>

        <section className="rounded-lg border border-zinc-200 bg-white p-5 shadow-sm">
          <h3 className="text-base font-semibold text-zinc-950">Create Membership</h3>
          <form className="mt-3 grid gap-3 sm:grid-cols-2" onSubmit={onCreateMembership}>
            <input
              type="email"
              placeholder="email"
              value={memberEmail}
              onChange={(event) => setMemberEmail(event.target.value)}
              className="rounded-lg border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-zinc-700"
              required
            />
            <input
              placeholder="nickname"
              value={memberNickname}
              onChange={(event) => setMemberNickname(event.target.value)}
              className="rounded-lg border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-zinc-700"
              required
            />
            <select
              value={memberRole}
              onChange={(event) => setMemberRole(event.target.value as ChannelMembershipRole)}
              className="rounded-lg border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-zinc-700"
            >
              <option value="AGENT">AGENT</option>
              <option value="CHANNEL_ADMIN">CHANNEL_ADMIN</option>
            </select>
            <button className="rounded-lg bg-zinc-950 px-4 py-2 text-sm font-semibold text-white" type="submit">
              Create
            </button>
          </form>
          {createdIdentity && (
            <div className="mt-3 rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">
              <p className="font-semibold">{createdIdentity.email}</p>
              {createdIdentity.temporaryPassword && (
                <div className="mt-2 flex items-start justify-between gap-3">
                  <p className="break-all font-mono text-xs">Temporary password: {createdIdentity.temporaryPassword}</p>
                  <CopyButton value={createdIdentity.temporaryPassword} label="Copy" />
                </div>
              )}
            </div>
          )}
        </section>
      </div>

      <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
        <h3 className="text-base font-semibold text-slate-900">Memberships</h3>
        <div className="mt-3 overflow-hidden rounded-lg border border-slate-200">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-3 py-2">User</th>
                <th className="px-3 py-2">Role</th>
                <th className="px-3 py-2">Status</th>
                <th className="px-3 py-2">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {memberships.map((membership) => (
                <tr key={membership.id}>
                  <td className="px-3 py-3">
                    <p className="break-all text-xs text-slate-500">{membership.id}</p>
                    <p className="break-all text-xs text-slate-500">user: {membership.userId}</p>
                  </td>
                  <td className="px-3 py-3">{membership.role}</td>
                  <td className="px-3 py-3">{membership.status}</td>
                  <td className="px-3 py-3">
                    <div className="flex flex-wrap gap-2">
                      <button
                        type="button"
                        onClick={() =>
                          void mutate(
                            () =>
                              changeMembershipRole(
                                channelId,
                                membership.id,
                                membership.role === 'AGENT' ? 'CHANNEL_ADMIN' : 'AGENT',
                              ),
                            'Unable to change role.',
                          )
                        }
                        className="rounded-lg border border-slate-300 px-2 py-1 text-xs font-semibold"
                      >
                        Toggle role
                      </button>
                      <button
                        type="button"
                        onClick={() =>
                          void mutate(
                            () =>
                              membership.status === 'ACTIVE'
                                ? disableMembership(channelId, membership.id)
                                : enableMembership(channelId, membership.id),
                            'Unable to change status.',
                          )
                        }
                        className="rounded-lg border border-slate-300 px-2 py-1 text-xs font-semibold"
                      >
                        {membership.status === 'ACTIVE' ? 'Disable' : 'Enable'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
