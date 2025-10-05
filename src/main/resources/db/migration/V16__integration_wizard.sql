-- Integration wizard schema upgrade

-- Normalize api_keys to reference merchants directly and track revocation
ALTER TABLE api_keys ADD COLUMN merchant_uuid UUID;
UPDATE api_keys ak
SET merchant_uuid = m.id
FROM merchants m
WHERE m.user_id = ak.merchant_id;
ALTER TABLE api_keys ALTER COLUMN merchant_uuid SET NOT NULL;
ALTER TABLE api_keys ADD CONSTRAINT fk_api_keys_merchants FOREIGN KEY (merchant_uuid) REFERENCES merchants(id);
ALTER TABLE api_keys ADD COLUMN revoked BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE api_keys DROP CONSTRAINT IF EXISTS api_keys_status_check;
ALTER TABLE api_keys DROP COLUMN status;
ALTER TABLE api_keys DROP COLUMN merchant_id;
ALTER TABLE api_keys RENAME COLUMN merchant_uuid TO merchant_id;
CREATE INDEX IF NOT EXISTS idx_api_keys_merchant ON api_keys(merchant_id);

-- Update integration checklist columns for wizard flow
ALTER TABLE integration_checklist RENAME COLUMN connected_stripe TO stripe_connected;
ALTER TABLE integration_checklist RENAME COLUMN webhook_configured TO webhook_registered;
ALTER TABLE integration_checklist RENAME COLUMN test_charge_done TO test_charge_passed;
ALTER TABLE integration_checklist RENAME COLUMN last_checked_at TO updated_at;
ALTER TABLE integration_checklist DROP COLUMN IF EXISTS aftership_configured;
ALTER TABLE integration_checklist DROP COLUMN IF EXISTS aftership_api_key;
UPDATE integration_checklist SET updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP);
ALTER TABLE integration_checklist ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE integration_checklist ALTER COLUMN updated_at SET NOT NULL;

-- Webhook endpoint registry
CREATE TABLE webhook_endpoints (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    url TEXT NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT webhook_endpoints_merchant_unique UNIQUE (merchant_id)
);

CREATE TABLE webhook_events (
    id UUID PRIMARY KEY,
    webhook_id UUID NOT NULL REFERENCES webhook_endpoints(id) ON DELETE CASCADE,
    event_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    delivered BOOLEAN NOT NULL DEFAULT FALSE,
    "timestamp" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_webhook_events_webhook ON webhook_events(webhook_id);
CREATE INDEX idx_webhook_events_timestamp ON webhook_events("timestamp");

