-- V12__users_stripe_only.sql
-- Make every statement standalone for H2 compatibility

-- 1) Drop legacy auth columns (each in its own statement)
ALTER TABLE users DROP COLUMN IF EXISTS email;
ALTER TABLE users DROP COLUMN IF EXISTS password_hash;
ALTER TABLE users DROP COLUMN IF EXISTS magic_link_token;
ALTER TABLE users DROP COLUMN IF EXISTS magic_link_expires_at;

-- 2) Ensure stripe_account_id exists and is nullable (so we can backfill first)
ALTER TABLE users ADD COLUMN IF NOT EXISTS stripe_account_id VARCHAR(255);

-- 3) Ensure role column exists and default/backfill before making NOT NULL
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(32);
UPDATE users SET role = 'MERCHANT' WHERE role IS NULL;

-- 4) Backfill stripe_account_id for any existing rows that are NULL
--    (dev-only placeholder values; in prod, these would be set via OAuth flow)
UPDATE users
SET stripe_account_id = CONCAT('acct_dev_', REPLACE(CAST(RANDOM_UUID() AS VARCHAR(36)), '-', ''))
WHERE stripe_account_id IS NULL;

-- 5) Add uniqueness on stripe_account_id
--    (H2 & Postgres both support IF NOT EXISTS on index creation)
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_stripe_account
ON users (stripe_account_id);

-- 6) Now make columns NOT NULL safely
ALTER TABLE users ALTER COLUMN stripe_account_id SET NOT NULL;
ALTER TABLE users ALTER COLUMN role SET NOT NULL;

-- 7) (Optional) Seed an admin mapped to a dev Stripe account id if absent
INSERT INTO users (id, stripe_account_id, role, created_at, updated_at)
SELECT RANDOM_UUID(), 'acct_1234', 'ADMIN', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM users WHERE stripe_account_id = 'acct_1234'
);
