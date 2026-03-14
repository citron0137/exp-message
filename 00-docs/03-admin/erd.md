# 🗺️ ERD / 전체 ERD

## 📝 Overview / 개요

This document shows the high-level entity relationship diagram for the admin and messaging domain.  
이 문서는 어드민 및 메시징 도메인의 상위 엔티티 관계도를 보여줍니다.

The model is centered on three actors: Platform Admin, Channel User, and EndUser.  
모델은 Platform Admin, Channel User, EndUser의 세 가지 존재를 중심으로 구성됩니다.

## 📊 ERD

```mermaid
erDiagram
    T_USER {
        string user_id
        enum user_role
    }

    T_CHANNEL {
        string channel_id
        enum status
    }

    T_CHANNEL_MEMBERSHIP {
        string channel_membership_id
        string channel_id
        string user_id
        enum membership_role
        enum agent_status
    }

    T_CHANNEL_INVITATION {
        string channel_invitation_id
        string channel_id
        string inviter_user_id
        string invitee_email
        enum target_role
        enum invitation_status
    }

    T_END_USER {
        string end_user_id
        string channel_id
        string email
        enum status
    }

    T_CONVERSATION {
        string conversation_id
        string channel_id
        string end_user_id
        enum status
    }

    T_CONVERSATION_MESSAGE {
        string conversation_message_id
        string conversation_id
        enum sender_type
        enum message_type
    }

    T_TICKET {
        string ticket_id
        string conversation_id
        string created_by_membership_id
        enum status
    }

    T_CHANNEL ||--o{ T_CHANNEL_MEMBERSHIP: has
    T_USER ||--o{ T_CHANNEL_MEMBERSHIP: belongs_to

    T_CHANNEL ||--o{ T_CHANNEL_INVITATION: issues
    T_USER ||--o{ T_CHANNEL_INVITATION: invites

    T_CHANNEL ||--o{ T_END_USER: owns
    T_CHANNEL ||--o{ T_CONVERSATION: contains
    T_END_USER ||--o{ T_CONVERSATION: starts

    T_CONVERSATION ||--|{ T_CONVERSATION_MESSAGE: owns
    T_CONVERSATION ||--o{ T_TICKET: contains
    T_CHANNEL_MEMBERSHIP ||--o{ T_TICKET: creates
```

## 📌 Notes / 설명

- `T_USER` stores only backoffice identity and global role.  
  `T_USER`는 백오피스 식별과 전역 역할만 저장합니다.

- Channel-scoped authorization is resolved through `T_CHANNEL_MEMBERSHIP`.  
  채널 범위 권한은 `T_CHANNEL_MEMBERSHIP`을 통해 해석합니다.

- `T_END_USER` is separated from `T_USER` and belongs to a channel.  
  `T_END_USER`는 `T_USER`와 분리되어 있으며 채널에 속합니다.

- `T_CONVERSATION_MESSAGE` is an internal entity owned by `T_CONVERSATION`.  
  `T_CONVERSATION_MESSAGE`는 `T_CONVERSATION`이 소유하는 내부 엔티티입니다.

- `T_TICKET` is an internal operational aggregate inside a conversation, created by channel staff.  
  `T_TICKET`은 conversation 내부의 운영용 aggregate이며 채널 직원이 생성합니다.
