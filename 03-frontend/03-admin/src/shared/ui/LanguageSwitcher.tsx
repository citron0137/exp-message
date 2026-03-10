import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/react';
import { useTranslation } from 'react-i18next';

export function LanguageSwitcher() {
  const { t, i18n } = useTranslation();

  function setLanguage(nextLanguage: 'en' | 'ko') {
    void i18n.changeLanguage(nextLanguage);
  }

  return (
    <Menu as="div" className="relative inline-block text-left">
      <MenuButton className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50">
        {t('language.label')}: {i18n.language.toUpperCase()}
      </MenuButton>

      <MenuItems anchor="bottom end" className="mt-2 w-36 rounded-lg border border-slate-200 bg-white p-1 shadow-lg">
        <MenuItem>
          {({ focus }) => (
            <button
              type="button"
              onClick={() => setLanguage('en')}
              className={`w-full rounded-md px-3 py-2 text-left text-sm ${focus ? 'bg-slate-100' : ''}`}
            >
              {t('language.english')}
            </button>
          )}
        </MenuItem>
        <MenuItem>
          {({ focus }) => (
            <button
              type="button"
              onClick={() => setLanguage('ko')}
              className={`w-full rounded-md px-3 py-2 text-left text-sm ${focus ? 'bg-slate-100' : ''}`}
            >
              {t('language.korean')}
            </button>
          )}
        </MenuItem>
      </MenuItems>
    </Menu>
  );
}
