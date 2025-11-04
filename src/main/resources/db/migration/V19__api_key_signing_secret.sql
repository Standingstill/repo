-- Add signing secret used to compute webhook HMAC.
-- Keep nullable for safety during backfill; service guarantees a value for new keys.
ALTER TABLE api_keys ADD COLUMN IF NOT EXISTS signing_secret VARCHAR(255);

-- Backfill any existing rows with a random-looking value derived from UUID.
-- (Cross‑DB compatible; adequate entropy for dev/test. In prod, rotate via KMS.)
UPDATE api_keys
SET signing_secret = REPLACE(CAST(RANDOM_UUID() AS VARCHAR(36)), '-', '')
WHERE signing_secret IS NULL OR signing_secret = '';

