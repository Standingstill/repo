-- Add integration tracking fields to merchants
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS stripe_account_id VARCHAR(255);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS is_integrated BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill stripe accounts from associated users
UPDATE merchants m
SET stripe_account_id = u.stripe_account_id
FROM users u
WHERE m.user_id = u.id
  AND u.stripe_account_id IS NOT NULL
  AND (m.stripe_account_id IS NULL OR m.stripe_account_id = '');

-- Derive integration flag from existing checklist progress
UPDATE merchants m
SET is_integrated = TRUE
FROM integration_checklist ic
WHERE ic.merchant_id = m.id
  AND ic.stripe_connected = TRUE
  AND ic.webhook_registered = TRUE
  AND ic.test_charge_passed = TRUE;
