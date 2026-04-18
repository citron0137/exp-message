import { Link, Outlet, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { LanguageSwitcher } from '@/shared/ui/LanguageSwitcher';
import { useSessionStore } from '@/shared/store/session-store';
import { useWorkspaceStore } from '@/shared/store/workspace-store';

function NavLink({ href, label, isActive }: { href: string; label: string; isActive: boolean }) {
  return (
    <Link
      to={href}
      className={
        isActive
          ? 'rounded-lg bg-slate-900 px-3 py-2 text-sm font-semibold text-white'
          : 'rounded-lg px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100'
      }
    >
      {label}
    </Link>
  );
}

export function AdminLayout() {
  const { t } = useTranslation();
  const location = useLocation();
  const email = useSessionStore((state) => state.email);
  const globalRole = useSessionStore((state) => state.globalRole);
  const logout = useSessionStore((state) => state.logout);
  const channels = useWorkspaceStore((state) => state.channels);
  const activeChannelId = useWorkspaceStore((state) => state.activeChannelId);
  const setActiveChannelId = useWorkspaceStore((state) => state.setActiveChannelId);
  const loadChannels = useWorkspaceStore((state) => state.loadChannels);

  const activeWorkspace = channels.find((item) => item.channel.id === activeChannelId) ?? null;

  return (
    <div className="min-h-screen bg-zinc-100 text-zinc-950">
      <div className="flex min-h-screen">
        <aside className="flex w-72 flex-col border-r border-zinc-200 bg-white">
          <div className="border-b border-zinc-200 p-5">
            <p className="text-xs font-semibold uppercase text-zinc-500">exp-message</p>
            <h1 className="mt-2 text-xl font-bold">Admin Console</h1>
          </div>

          <div className="border-b border-zinc-200 p-4">
            <div className="flex items-center justify-between">
              <p className="text-xs font-semibold uppercase text-zinc-500">Workspace</p>
              <button className="text-xs font-semibold text-zinc-700 underline" type="button" onClick={() => void loadChannels()}>
                Refresh
              </button>
            </div>
            <select
              value={activeChannelId ?? ''}
              onChange={(event) => setActiveChannelId(event.target.value)}
              className="mt-2 w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm outline-none focus:border-zinc-700"
            >
              {channels.map((item) => (
                <option key={item.channel.id} value={item.channel.id}>
                  {item.channel.name}
                </option>
              ))}
              {channels.length === 0 && <option value="">No workspace</option>}
            </select>
            {activeWorkspace?.membership && (
              <p className="mt-2 text-xs text-zinc-500">
                {activeWorkspace.membership.role} · {activeWorkspace.membership.status}
              </p>
            )}
          </div>

          <nav className="flex flex-1 flex-col gap-2 p-4">
            <NavLink href="/channels" label={t('nav.channels')} isActive={location.pathname.startsWith('/channels')} />
            <NavLink href="/inbox" label={t('nav.inbox')} isActive={location.pathname.startsWith('/inbox')} />
          </nav>
        </aside>

        <div className="flex min-h-screen flex-1 flex-col">
          <header className="flex h-16 items-center justify-between border-b border-zinc-200 bg-white px-6">
            <div>
              <p className="text-xs uppercase text-zinc-500">Current workspace</p>
              <p className="text-sm font-semibold text-zinc-800">{activeWorkspace?.channel.name ?? t('header.workspace')}</p>
            </div>

            <div className="flex items-center gap-3">
              <LanguageSwitcher />
              <div className="text-right">
                <p className="text-xs font-medium text-zinc-500">{globalRole ?? 'UNKNOWN'}</p>
                <p className="text-xs text-zinc-600">{email ?? '-'}</p>
              </div>
              <button
                type="button"
                onClick={() => void logout()}
                className="rounded-lg border border-zinc-300 bg-white px-3 py-2 text-xs font-semibold text-zinc-700 hover:bg-zinc-50"
              >
                {t('header.logout')}
              </button>
            </div>
          </header>

          <main className="flex-1 p-5">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
}
