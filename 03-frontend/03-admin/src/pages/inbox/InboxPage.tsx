import { useTranslation } from 'react-i18next';

export default function InboxPage() {
  const { t } = useTranslation();

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-lg font-semibold text-slate-900">{t('inbox.title')}</h2>
      <p className="mt-2 text-sm text-slate-600">{t('inbox.description')}</p>
    </section>
  );
}
