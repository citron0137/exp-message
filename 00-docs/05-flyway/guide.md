# Flyway Usage Guide

## Purpose

Flyway manages database schema changes with versioned SQL files.

In this project:

- `01-db-migrations` applies schema changes.
- `00-monolitic` validates schema with JPA/Hibernate.
- `00-monolitic` must not run Flyway migrations.
- Hibernate must not mutate the schema automatically.

## Current Setup

Migration module:

```text
02-backend/01-db-migrations
```

Application module:

```text
02-backend/00-monolitic
```

Migration directory:

```text
02-backend/01-db-migrations/src/main/resources/db/migration
```

Development migration container:

```text
dev-00-monolitic-db-migrations
```

The monolith Docker Compose file already runs this container before the application starts.

## Runtime Ownership

Use this ownership rule:

```text
01-db-migrations = schema migration source of truth
00-monolitic = application runtime and JPA schema validation
```

Do not add Flyway migrations to:

```text
02-backend/00-monolitic/src/main/resources/db/migration
```

Do not add Flyway dependencies to `00-monolitic` unless the architecture is intentionally changed.

## How Flyway Runs

In local Docker Compose:

```text
1. MySQL starts
2. db-migrations container runs
3. Flyway applies pending migrations
4. db-migrations container exits
5. 00-monolitic starts
6. Hibernate validates entity mappings
```

Flyway records applied migrations in:

```text
flyway_schema_history
```

Applied migrations are not executed again.

## Migration File Naming

Use this format:

```text
V{version}__{description}.sql
```

The separator between version and description is two underscores.

Current project style:

```text
V20260418_01__core_phase0_admin_foundation.sql
V20260418_02__core_phase1a_channel_integrations.sql
V20260418_03__core_phase2_visitor_conversation_entry.sql
V20260418_04__core_phase3_message_persistence.sql
```

Do not use:

```text
V20260418_04_core_phase3_message_persistence.sql
```

## Adding a Schema Change

When an entity change requires a database change, add a new migration to `01-db-migrations`.

Example: add `last_message_at` to `cv_channel_conversations`.

1. Update the entity in `00-monolitic`.

```kotlin
@Column(name = "last_message_at")
var lastMessageAt: LocalDateTime?
```

2. Create a new migration file in `01-db-migrations`.

```text
02-backend/01-db-migrations/src/main/resources/db/migration/V20260418_05__add_last_message_at_to_channel_conversations.sql
```

3. Write the SQL.

```sql
ALTER TABLE cv_channel_conversations
    ADD COLUMN last_message_at DATETIME(6) NULL;

CREATE INDEX idx_cv_channel_conversations_last_message_at
    ON cv_channel_conversations (last_message_at);
```

4. Run the migration container or Docker Compose.

5. Start `00-monolitic` and let Hibernate validate the schema.

## Do Not Edit Applied Migrations

After a migration has been applied to any shared or important database, do not edit it.

Flyway stores a checksum for each migration. If the file changes later, Flyway can fail with a checksum mismatch.

Bad:

```text
Edit V20260418_04__core_phase3_message_persistence.sql after it has been applied.
```

Good:

```text
Create V20260418_05__fix_phase3_schema_gap.sql.
```

During early local-only work, editing an unapplied migration is acceptable. Once a migration reaches a shared database, treat it as immutable.

## Baseline Rule

`01-db-migrations` currently uses:

```properties
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=20260113.00
```

This allows Flyway to work with an existing schema that already had legacy tables before the migration module was introduced.

Do not change the baseline version casually. It affects which migrations Flyway considers already applied when initializing a non-empty schema.

## Running Migrations

From the migration module:

```bash
cd /Users/do1/Documents/java/exp-message/02-backend/01-db-migrations
./gradlew bootRun
```

In local Docker Compose, use the existing migration service:

```bash
cd /Users/do1/Documents/java/exp-message/02-backend/00-monolitic
docker compose up db-migrations
```

Or start the full local stack:

```bash
cd /Users/do1/Documents/java/exp-message/02-backend/00-monolitic
docker compose up
```

## Checking Applied Migrations

After migration runs, check:

```sql
SELECT version, description, script, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Check core tables:

```sql
SHOW TABLES LIKE 'iam_%';
SHOW TABLES LIKE 'cv_%';
```

## Verification Commands

Migration module:

```bash
cd /Users/do1/Documents/java/exp-message/02-backend/01-db-migrations
./gradlew test
./gradlew ktlintCheck
```

Application module:

```bash
cd /Users/do1/Documents/java/exp-message/02-backend/00-monolitic
./gradlew compileKotlin
./gradlew test --tests 'site.rahoon.message.monolithic.core.*'
./gradlew ktlintCheck
```

Runtime migration verification requires a running MySQL database.

## Team Rules

- Every schema-changing entity update needs a Flyway migration in `01-db-migrations`.
- Keep `00-monolitic` as JPA validate only.
- Do not rely on Hibernate `update` for schema changes.
- Do not edit migrations after they are applied to shared databases.
- Keep entity mappings, indexes, unique constraints, and migrations synchronized.
- Legacy tables remain in existing migrations unless a separate migration plan explicitly changes them.

