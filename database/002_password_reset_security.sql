-- Apply before starting the new JAR. Additive; does not modify password_reset_tokens.
BEGIN;
ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_requested_at TIMESTAMP(6) WITH TIME ZONE;
COMMIT;
