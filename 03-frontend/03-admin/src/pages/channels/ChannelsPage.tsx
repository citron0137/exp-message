import { FormEvent, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { createChannel, listChannels, type Channel, type IdentityResult } from '@/shared/api/admin-core';
import { toApiErrorMessage } from '@/shared/api/http';
import { useSessionStore } from '@/shared/store/session-store';

function formatDate(value: string) {
  return new Date(value).toLocaleString();
}

export default function ChannelsPage() {
  const globalRole = useSessionStore((state) => state.globalRole);
  const [channels, setChannels] = useState<Channel[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [createdAdmin, setCreatedAdmin] = useState<IdentityResult | null>(null);
  const [name, setName] = useState('');
  const [adminEmail, setAdminEmail] = useState('');
  const [adminNickname, setAdminNickname] = useState('');

  async function loadChannels() {
    setLoading(true);
    setErrorMessage(null);
    try {
      const result = await listChannels();
      setChannels(result.items);
    } catch (error) {
      setErrorMessage(toApiErrorMessage(error, 'Unable to load channels.'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (globalRole === 'PLATFORM_ADMIN') {
      void loadChannels();
    }
  }, [globalRole]);

  async function onCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!name.trim() || !adminEmail.trim() || !adminNickname.trim()) {
      return;
    }

    setSubmitting(true);
    setErrorMessage(null);
    setCreatedAdmin(null);
    try {
      const result = await createChannel({
        name: name.trim(),
        adminEmail: adminEmail.trim(),
        adminNickname: adminNickname.trim(),
      });
      setChannels((current) => [result.channel, ...current.filter((item) => item.id !== result.channel.id)]);
      setCreatedAdmin(result.initialAdmin);
      setName('');
      setAdminEmail('');
      setAdminNickname('');
    } catch (error) {
      setErrorMessage(toApiErrorMessage(error, 'Unable to create channel.'));
    } finally {
      setSubmitting(false);
    }
  }

  if (globalRole !== 'PLATFORM_ADMIN') {
    return (
      <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-slate-900">Channel Management</h2>
        <p className="mt-2 text-sm text-slate-600">
          Platform admin authority is required for channel list and creation. Use the inbox with a channel id for channel
          operations.
        </p>
      </section>
    );
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[360px_1fr]">
      <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="text-lg font-semibold text-slate-900">Create Channel</h2>
        <form className="mt-4 space-y-3" onSubmit={onCreate}>
          <label className="block">
            <span className="mb-1 block text-xs font-semibold text-slate-600">Channel name</span>
            <input
              value={name}
              onChange={(event) => setName(event.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-slate-700"
              required
            />
          </label>
          <label className="block">
            <span className="mb-1 block text-xs font-semibold text-slate-600">Initial admin email</span>
            <input
              type="email"
              value={adminEmail}
              onChange={(event) => setAdminEmail(event.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-slate-700"
              required
            />
          </label>
          <label className="block">
            <span className="mb-1 block text-xs font-semibold text-slate-600">Initial admin nickname</span>
            <input
              value={adminNickname}
              onChange={(event) => setAdminNickname(event.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-slate-700"
              required
            />
          </label>
          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-lg bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700 disabled:bg-slate-400"
          >
            {submitting ? 'Creating...' : 'Create'}
          </button>
        </form>

        {createdAdmin && (
          <div className="mt-4 rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">
            <p className="font-semibold">Initial admin</p>
            <p className="mt-1">{createdAdmin.email}</p>
            <p>{createdAdmin.nickname}</p>
            {createdAdmin.temporaryPassword && (
              <p className="mt-2 break-all font-mono text-xs">Temporary password: {createdAdmin.temporaryPassword}</p>
            )}
          </div>
        )}
      </section>

      <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">Channels</h2>
            <p className="mt-1 text-sm text-slate-600">Manage customer channels and widget integrations.</p>
          </div>
          <button
            type="button"
            onClick={() => void loadChannels()}
            className="rounded-lg border border-slate-300 px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50"
          >
            Refresh
          </button>
        </div>

        {errorMessage && <p className="mt-4 text-sm text-red-600">{errorMessage}</p>}
        {loading && <p className="mt-4 text-sm text-slate-500">Loading channels...</p>}

        <div className="mt-4 overflow-hidden rounded-lg border border-slate-200">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-3 py-2">Name</th>
                <th className="px-3 py-2">Status</th>
                <th className="px-3 py-2">Created</th>
                <th className="px-3 py-2" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {channels.map((channel) => (
                <tr key={channel.id}>
                  <td className="px-3 py-3">
                    <p className="font-medium text-slate-900">{channel.name}</p>
                    <p className="break-all text-xs text-slate-500">{channel.id}</p>
                  </td>
                  <td className="px-3 py-3">{channel.status}</td>
                  <td className="px-3 py-3 text-slate-600">{formatDate(channel.createdAt)}</td>
                  <td className="px-3 py-3 text-right">
                    <Link className="text-sm font-semibold text-slate-900 underline" to={`/channels/${channel.id}`}>
                      Open
                    </Link>
                  </td>
                </tr>
              ))}
              {channels.length === 0 && !loading && (
                <tr>
                  <td className="px-3 py-6 text-center text-sm text-slate-500" colSpan={4}>
                    No channels yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
