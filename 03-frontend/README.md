# Frontend

프론트엔드 디렉토리입니다. 채팅 서비스의 사용자 인터페이스를 관리합니다.

## 디렉토리 구조

```
03-frontend/
├── widget/              # 임베디드 채팅 위젯 (Vite + React)
│   ├── src/
│   │   ├── components/  # UI 컴포넌트
│   │   ├── lib/         # API, WebSocket 클라이언트
│   │   ├── hooks/       # Custom hooks
│   │   └── styles/      # 스타일
│   ├── public/
│   └── package.json
│
└── web/                 # 독립 웹 애플리케이션 (Next.js) - 향후 추가
```

## Widget

채널톡, 인터콤 스타일의 임베디드 채팅 위젯입니다.

### 특징
- 어떤 웹사이트에도 삽입 가능
- 우측 하단 플로팅 버튼
- 실시간 메시지 수신 (WebSocket)
- Shadow DOM으로 스타일 격리

### 기술 스택
- **빌드 도구**: Vite
- **프레임워크**: React 18 + TypeScript
- **스타일링**: Tailwind CSS
- **상태 관리**: Zustand
- **WebSocket**: @stomp/stompjs
- **HTTP 클라이언트**: Axios

### 사용 방법

```html
<!-- 고객 웹사이트에 삽입 -->
<script src="https://message.rahoon.site/widget.js"></script>
<script>
  ChatWidget.init({
    apiUrl: 'https://message.rahoon.site/api',
    wsUrl: 'wss://message.rahoon.site/ws'
  });
</script>
```

## 개발 가이드

### Widget 개발

```bash
cd 03-frontend/widget

# 의존성 설치
npm install

# 개발 서버 실행
npm run dev

# 빌드
npm run build
```

## 배포

빌드된 위젯은 `widget/dist/widget.iife.js` 파일로 생성되며, CDN 또는 정적 파일 서버에 배포합니다.
