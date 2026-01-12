# Auth 관련 Util 개발

## 목표

Auth 관련 유틸을 개발하여, 인증이 필요한 종단 관심사들에 개발을 편하게 한다.

## 단계

1. AuthInfo 객체와 AuthInfoAffect annotation을 선언한다 (주요 인터페이스가 될 예정)
2. annotion의 param에 따라 인증되지 않은 유저 차단기능을 개발한다.
3. annotion의 param에 따라 AuthInfo를 주입해주는 기능을 개발한다.
4. annotion이 달린 객체에 대해 Swagger에서 인증 기능을 보여주도록한다.
