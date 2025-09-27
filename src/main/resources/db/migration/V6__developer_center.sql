CREATE TABLE integration_checklist (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    connected_stripe BOOLEAN NOT NULL DEFAULT FALSE,
    webhook_configured BOOLEAN NOT NULL DEFAULT FALSE,
    aftership_configured BOOLEAN NOT NULL DEFAULT FALSE,
    test_charge_done BOOLEAN NOT NULL DEFAULT FALSE,
    last_checked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT integration_checklist_merchant_unique UNIQUE (merchant_id)
);

CREATE TABLE api_audit_logs (
    id UUID PRIMARY KEY,
    api_key_id UUID REFERENCES api_keys(id),
    method VARCHAR(16) NOT NULL,
    path TEXT NOT NULL,
    status INTEGER NOT NULL,
    ip VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
