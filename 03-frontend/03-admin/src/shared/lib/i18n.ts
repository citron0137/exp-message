import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

const resources = {
  en: {
    translation: {
      nav: {
        channels: 'Channels',
        inbox: 'Inbox',
      },
      header: {
        workspace: 'Operations Workspace',
        logout: 'Logout',
      },
      login: {
        title: 'Admin Sign In',
        subtitle: 'Use your admin credentials to continue.',
        email: 'Email',
        password: 'Password',
        submit: 'Sign In',
        loading: 'Signing in...',
      },
      channels: {
        title: 'Channel Management',
        description: 'Create and manage customer channels here.',
      },
      inbox: {
        title: 'Conversation Inbox',
        description: 'Channel conversations will be listed here.',
      },
      language: {
        label: 'Language',
        english: 'English',
        korean: 'Korean',
      },
    },
  },
  ko: {
    translation: {
      nav: {
        channels: '채널 관리',
        inbox: '인박스',
      },
      header: {
        workspace: '운영 워크스페이스',
        logout: '로그아웃',
      },
      login: {
        title: '운영자 로그인',
        subtitle: '운영 계정으로 계속 진행하세요.',
        email: '이메일',
        password: '비밀번호',
        submit: '로그인',
        loading: '로그인 중...',
      },
      channels: {
        title: '채널 관리',
        description: '고객사 채널 생성 및 관리 화면입니다.',
      },
      inbox: {
        title: '대화 인박스',
        description: '채널별 대화 목록이 여기에 표시됩니다.',
      },
      language: {
        label: '언어',
        english: '영어',
        korean: '한국어',
      },
    },
  },
};

void i18n.use(initReactI18next).init({
  resources,
  lng: 'ko',
  fallbackLng: 'en',
  interpolation: {
    escapeValue: false,
  },
});

export default i18n;
