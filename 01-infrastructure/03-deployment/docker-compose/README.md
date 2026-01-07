# Docker Compose - MySQL

로컬 개발 환경을 위한 MySQL Docker Compose 설정입니다.

## 구성

- **MySQL 8.0**: 최신 MySQL 버전 사용
- **Binlog 활성화**: ROW 기반 binlog로 특정 시점 복구 지원
- **데이터 영속성**: 볼륨을 통한 데이터 보존

## 사용 방법

### MySQL 시작

```bash
docker-compose up -d
```

### MySQL 중지

```bash
docker-compose down
```

### 데이터까지 삭제하고 중지

```bash
docker-compose down -v
```

### 로그 확인

```bash
docker-compose logs -f mysql
```

### MySQL 접속

```bash
# 컨테이너 내부에서 접속
docker-compose exec mysql mysql -u root -prootpassword

# 또는 외부에서 접속
mysql -h 127.0.0.1 -P 3306 -u root -prootpassword
```

## 설정 정보

### 데이터베이스 정보

- **Root 비밀번호**: `rootpassword`
- **데이터베이스명**: `message_db`
- **사용자명**: `message_user`
- **사용자 비밀번호**: `message_password`
- **포트**: `3306`

### Binlog 설정

- **Format**: ROW (가장 정확한 복구 지원)
- **보관 기간**: 7일
- **최대 크기**: 100MB
- **로그 파일**: `mysql-bin.*`

## Binlog 확인 방법

### Binlog 파일 목록 확인

```sql
SHOW BINARY LOGS;
```

### 현재 Binlog 위치 확인

```sql
SHOW MASTER STATUS;
```

### Binlog 내용 확인

```bash
# 컨테이너 내부에서
docker-compose exec mysql mysqlbinlog /var/lib/mysql/mysql-bin.000001

# 또는 외부에서
docker-compose exec mysql mysqlbinlog --read-from-remote-server --host=localhost --user=root --password=rootpassword mysql-bin.000001
```

## 특정 시점 복구 예시

1. **Binlog 위치 기록** (데이터 변경 전)
   ```sql
   SHOW MASTER STATUS;
   -- Position 값을 기록
   ```

2. **데이터 변경 수행**

3. **복구 실행**
   ```bash
   # 특정 시점까지 복구
   docker-compose exec mysql mysqlbinlog --stop-datetime="2026-01-07 14:00:00" /var/lib/mysql/mysql-bin.000001 | mysql -u root -prootpassword message_db
   ```

