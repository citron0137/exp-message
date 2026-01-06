# 아키텍처 문서

모놀리식 서비스의 아키텍처 설계 문서입니다.

## 개요

모놀리식 서비스는 바운더리 컨텍스트 기반으로 설계되어 향후 마이크로서비스 분리를 용이하게 합니다.

## 바운더리 컨텍스트

### User Context

**패키지**: `site.rahoon.message.__monolitic.user`

**책임**:
- 사용자 인증/인가 (JWT)
- 사용자 정보 관리

**엔티티**:
- User

### Chat Context

**패키지**: `site.rahoon.message.__monolitic.chat`

**책임**:
- 채팅방 생성/관리
- 채팅방 멤버 관리
- 메시지 전송/수신

**엔티티**:
- ChatRoom
- ChatRoomMember
- Message

## 레이어 구조

```
Controller (REST API + DTO)
    ↓
Application Service (어플리케이션 서비스 + Criteria/Result)
    ↓
Domain Service (도메인 서비스 + Command)
    ↓
Domain (도메인 객체)
    ↓
Repository Interface (도메인)
    ↓
Repository Implementation (데이터 접근)
```

## 데이터베이스

- **RDBMS**: MySQL
- **캐싱**: Redis (세션, 사용자 정보, 채팅방 정보)

## API 설계

### User Context

- `POST /api/users/register` - 회원가입
- `POST /api/users/login` - 로그인
- `GET /api/users/me` - 현재 사용자 정보

### Chat Context

- `POST /api/chat/rooms` - 채팅방 생성
- `GET /api/chat/rooms` - 채팅방 목록 조회
- `GET /api/chat/rooms/{id}` - 채팅방 상세 조회
- `POST /api/chat/rooms/{id}/members` - 멤버 추가
- `POST /api/chat/rooms/{id}/messages` - 메시지 전송
- `GET /api/chat/rooms/{id}/messages` - 메시지 목록 조회

## WebSocket

- `WS /ws/chat/{roomId}` - 채팅방 WebSocket 연결

## 향후 확장

Phase 3에서 Message Service로 분리될 때를 대비하여, Chat Context 내부에서도 Message 관련 코드는 독립적으로 관리합니다.

