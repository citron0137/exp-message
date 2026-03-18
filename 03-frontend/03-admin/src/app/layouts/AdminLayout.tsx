import { Link, Outlet, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { LanguageSwitcher } from '@/shared/ui/LanguageSwitcher';
import { useSessionStore } from '@/shared/store/session-store';

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
  const role = useSessionStore((state) => state.role);
  const logout = useSessionStore((state) => state.logout);

  return (
    <div className="min-h-screen bg-slate-100 text-slate-900">
      <div className="mx-auto flex min-h-screen max-w-7xl">
        <aside className="w-64 border-r border-slate-200 bg-white p-4">
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">exp-message</p>
          <h1 className="mt-2 text-lg font-bold">Admin Console</h1>
          <nav className="mt-6 flex flex-col gap-2">
            <NavLink href="/channels" label={t('nav.channels')} isActive={location.pathname.startsWith('/channels')} />
            <NavLink href="/inbox" label={t('nav.inbox')} isActive={location.pathname.startsWith('/inbox')} />
          </nav>
        </aside>

        <div className="flex min-h-screen flex-1 flex-col">
          <header className="flex h-16 items-center justify-between border-b border-slate-200 bg-white px-6">
            <div>
              <p className="text-xs uppercase tracking-wide text-slate-500">Admin</p>
              <p className="text-sm font-semibold text-slate-800">{t('header.workspace')}</p>
            </div>

            <div className="flex items-center gap-3">
              <LanguageSwitcher />
              <div className="text-right">
                <p className="text-xs font-medium text-slate-500">{role ?? 'UNKNOWN'}</p>
                <p className="text-xs text-slate-600">{email ?? '-'}</p>
              </div>
              <button
                type="button"
                onClick={() => void logout()}
                className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50"
              >
                {t('header.logout')}
              </button>
            </div>
          </header>

          <main className="flex-1 p-6">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
}
