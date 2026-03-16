# 🔄 Sequence Diagram / 시퀀스 다이어그램

## 고려사항 / Design Notes

- Focus on actor flow and aggregate interaction.  
  행위자 흐름과 aggregate 상호작용에 집중합니다.
- Describe only the successful path first.  
  우선 성공 경로만 기술합니다.
- Use aggregate/application level participants instead of low-level infra details.  
  저수준 인프라보다 aggregate/application 레벨 참여자를 사용합니다.
- Application layer handles coordination(Orchestration); aggregate handles invariants.  
  Application 레이어가 중심에서 Orchestration을 담당하고, aggregate가 불변식을 처리합니다.
- Center the document around Platform Admin, Channel User, and ChannelEndUser.  
  문서는 Platform Admin, Channel User, ChannelEndUser를 중심으로 구성합니다.
- Follow the aggregate boundaries defined in `aggregate-root-design.md`.  
  `aggregate-root-design.md`에서 정의한 aggregate 경계를 따릅니다.
- Distinguish between aggregate state changes and timeline/event recording.  
  aggregate 상태 변경과 타임라인/이벤트 기록을 구분합니다.
- Ticket-related flows are excluded from the current scope and reserved for a future phase.  
  Ticket 관련 흐름은 현재 범위에서 제외하며 추후 단계로 남겨둡니다.

## 1. Channel Creation / 채널 생성

```mermaid
sequenceDiagram
    actor platform_admin as Platform Admin / 운영 어드민
    participant channel_app as Channel Application / 채널 어플리케이션
    participant channel as Channel AR / 채널 AR
    participant user as User AR / 사용자 AR
    participant membership as ChannelMembership AR / 채널 멤버십 AR

    platform_admin ->> channel_app: create channel / 채널 생성 요청
    note over platform_admin,channel_app: channel name, greeting, default settings / 채널명, 인사말, 기본 설정

    channel_app ->> channel: create channel / 채널 생성
    channel ->> channel_app:

    channel_app ->> user: create initial customer admin user / 초기 고객사 관리자 사용자 생성
    user ->> channel_app:

    channel_app ->> membership: create ADMIN membership / ADMIN 멤버십 생성
    membership ->> channel_app:

    channel_app ->> platform_admin: return created channel / 생성 결과 반환
```

## 2. Invite Channel Staff / 고객사 직원 초대

```mermaid
sequenceDiagram
    actor channel_admin as Channel Admin / 고객사 관리자
    participant invitation_app as Invitation Application / 초대 어플리케이션
    participant membership as ChannelMembership AR / 채널 멤버십 AR
    participant invitation as ChannelInvitation AR / 채널 초대 AR
    participant user as User AR / 사용자 AR

    channel_admin ->> invitation_app: invite staff by email / 이메일로 직원 초대
    note over channel_admin,invitation_app: invitee email, target role / 초대 이메일, 대상 역할

    invitation_app ->> membership: validate inviter role / 초대자 권한 검증
    membership ->> invitation_app:

    invitation_app ->> invitation: create invitation / 초대 생성
    invitation ->> invitation_app:

    invitation_app ->> channel_admin: invitation created / 초대 생성 완료

    note over invitation_app,user: invitee opens email link later / 이후 초대 대상이 이메일 링크 접속

    invitation_app ->> invitation: validate invitation state / 초대 상태 검증
    invitation ->> invitation_app:

    invitation_app ->> user: create or load backoffice user / 기존 사용자 조회 또는 신규 생성
    user ->> invitation_app:

    invitation_app ->> membership: create target membership / 대상 멤버십 생성
    membership ->> invitation_app:

    invitation_app ->> invitation: mark accepted / 초대 수락 처리
    invitation ->> invitation_app:
```

## 3. Start Conversation from Widget / 위젯에서 대화 시작

```mermaid
sequenceDiagram
    actor end_user as ChannelEndUser / 채널 엔드유저
    participant widget_app as Widget Application / 위젯 어플리케이션
    participant enduser as ChannelEndUser AR / 채널 엔드유저 AR
    participant conversation as Conversation AR / 대화 AR

    end_user ->> widget_app: open widget / 위젯 진입
    widget_app ->> end_user: request email before real conversation / 본격 대화 전 이메일 요청

    end_user ->> widget_app: submit email / 이메일 제출
    widget_app ->> enduser: find or create by channel + email / 채널+이메일 기준 조회 또는 생성
    enduser ->> widget_app:

    widget_app ->> conversation: load ongoing conversation / ongoing conversation 조회

    alt conversation exists / 기존 대화 존재
        conversation ->> widget_app: return existing conversation / 기존 대화 반환
    else conversation does not exist / 기존 대화 없음
        conversation ->> widget_app:
        widget_app ->> conversation: create conversation / 대화 생성
    end

    widget_app ->> conversation: append first message / 첫 메시지 추가
    conversation ->> widget_app:

    widget_app ->> end_user: return conversation timeline / 대화 타임라인 반환
```

## 4. Re-enter Existing Conversation / 기존 대화 재진입

```mermaid
sequenceDiagram
    actor end_user as ChannelEndUser / 채널 엔드유저
    participant widget_app as Widget Application / 위젯 어플리케이션
    participant enduser as ChannelEndUser AR / 채널 엔드유저 AR
    participant conversation as Conversation AR / 대화 AR

    end_user ->> widget_app: re-enter widget with same email / 같은 이메일로 재진입
    widget_app ->> enduser: resolve end user by channel + email / 채널+이메일로 엔드유저 식별
    enduser ->> widget_app:

    widget_app ->> conversation: load ongoing conversation / ongoing conversation 조회
    conversation ->> widget_app: existing conversation / 기존 대화 반환

    widget_app ->> conversation: append new message / 새 메시지 추가
    conversation ->> widget_app:

    widget_app ->> end_user: return continued timeline / 이어진 타임라인 반환
```

## 5. Move to Dormant / 휴지기 전환

```mermaid
sequenceDiagram
    participant scheduler as Dormant Scheduler / 휴지기 스케줄러
    participant conversation_app as Conversation Application / 대화 어플리케이션
    participant conversation as Conversation AR / 대화 AR

    scheduler ->> conversation_app: find inactive conversations / 비활성 대화 조회
    note over scheduler,conversation_app: last message older than 7 days / 마지막 메시지가 7일 초과

    conversation_app ->> conversation: mark dormant / dormant 처리
    conversation ->> conversation_app:

    conversation_app ->> scheduler: return dormant count / 처리 건수 반환
```

## 6. Platform Admin Accesses Channel Data / 운영 어드민의 고객사 데이터 접근

```mermaid
sequenceDiagram
    actor platform_admin as Platform Admin / 운영 어드민
    participant admin_app as Admin Application / 운영 어드민 어플리케이션
    participant user as User AR / 사용자 AR
    participant channel as Channel AR / 채널 AR
    participant conversation as Conversation AR / 대화 AR

    platform_admin ->> admin_app: open customer channel dashboard / 고객사 채널 대시보드 접근
    note over platform_admin,admin_app: platform-wide support or operation use case / 운영 또는 지원 목적으로 접근

    admin_app ->> user: validate PLATFORM_ADMIN role / PLATFORM_ADMIN 권한 검증
    user ->> admin_app:

    admin_app ->> channel: load target channel / 대상 채널 조회
    channel ->> admin_app:

    admin_app ->> conversation: load channel conversations / 채널 대화 조회
    conversation ->> admin_app:

    admin_app ->> platform_admin: return channel operational view / 채널 운영 화면 반환
```
