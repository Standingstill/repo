CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(32) NOT NULL,
    password_hash VARCHAR(255),
    stripe_account_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'MERCHANT', 'BUYER'))
);

CREATE TABLE api_keys (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL REFERENCES users(id),
    key_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT api_keys_status_check CHECK (status IN ('ACTIVE', 'REVOKED'))
);

CREATE TABLE merchants (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    business_name VARCHAR(255) NOT NULL,
    support_email VARCHAR(255) NOT NULL,
    dispute_window_hours INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT merchants_user_unique UNIQUE (user_id),
    CONSTRAINT merchants_dispute_window_hours_check CHECK (dispute_window_hours IS NULL OR dispute_window_hours > 0)
);
