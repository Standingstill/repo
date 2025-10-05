ALTER TABLE stripe_connect_sessions
    ADD COLUMN IF NOT EXISTS user_id UUID;

DELETE FROM stripe_connect_sessions WHERE user_id IS NULL;

ALTER TABLE stripe_connect_sessions
    ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE stripe_connect_sessions
    ADD CONSTRAINT IF NOT EXISTS stripe_connect_sessions_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_stripe_connect_sessions_user_id ON stripe_connect_sessions(user_id);
