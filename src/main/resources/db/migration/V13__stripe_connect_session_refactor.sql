ALTER TABLE stripe_connect_sessions
    DROP CONSTRAINT IF EXISTS stripe_connect_sessions_user_id_fkey;

ALTER TABLE stripe_connect_sessions
    DROP COLUMN IF EXISTS user_id;

ALTER TABLE stripe_connect_sessions
    ADD COLUMN IF NOT EXISTS target_role VARCHAR(32) NOT NULL DEFAULT 'MERCHANT';

ALTER TABLE stripe_connect_sessions
    ADD COLUMN IF NOT EXISTS return_path VARCHAR(512);

UPDATE stripe_connect_sessions SET target_role = 'MERCHANT' WHERE target_role IS NULL;
