import { FormEvent, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useSessionStore } from '@/shared/store/session-store';

interface LoginLocationState {
  from?: string;
}

export default function LoginPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();

  const status = useSessionStore((state) => state.status);
  const login = useSessionStore((state) => state.login);
  const errorMessage = useSessionStore((state) => state.errorMessage);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const state = location.state as LoginLocationState | null;
  const nextPath = state?.from || '/channels';

  useEffect(() => {
    if (status === 'authenticated') {
      navigate(nextPath, { replace: true });
    }
  }, [navigate, nextPath, status]);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!email.trim() || !password.trim()) {
      return;
    }

    try {
      setSubmitting(true);
      await login(email.trim(), password);
      navigate(nextPath, { replace: true });
    } catch {
      // Error is stored in session store.
    } finally {
      setSubmitting(false);
    }
  }

  if (status === 'authenticated') {
    return <Navigate to={nextPath} replace />;
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 p-4">
      <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h1 className="text-xl font-bold text-slate-900">{t('login.title')}</h1>
        <p className="mt-1 text-sm text-slate-600">{t('login.subtitle')}</p>

        <form className="mt-6 space-y-4" onSubmit={onSubmit}>
          <label className="block">
            <span className="mb-1 block text-xs font-semibold text-slate-600">{t('login.email')}</span>
            <input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none transition focus:border-slate-700"
            />
          </label>

          <label className="block">
            <span className="mb-1 block text-xs font-semibold text-slate-600">{t('login.password')}</span>
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none transition focus:border-slate-700"
            />
          </label>

          {errorMessage && <p className="text-xs font-medium text-red-600">{errorMessage}</p>}

          <button
            type="submit"
            disabled={submitting || status === 'loading'}
            className="w-full rounded-lg bg-slate-900 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-400"
          >
            {submitting ? t('login.loading') : t('login.submit')}
          </button>
        </form>
      </div>
    </div>
  );
}
