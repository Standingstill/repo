CREATE TABLE orders (
    id UUID PRIMARY KEY,
    order_number VARCHAR(64) NOT NULL UNIQUE,
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    buyer_email VARCHAR(255) NOT NULL,
    buyer_user_id UUID REFERENCES users(id),
    product_name VARCHAR(255) NOT NULL,
    product_description TEXT,
    quantity INTEGER NOT NULL,
    unit_price_cents INTEGER NOT NULL,
    currency VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    expected_delivery_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT orders_quantity_check CHECK (quantity > 0),
    CONSTRAINT orders_status_check CHECK (status IN ('PENDING_FULFILLMENT', 'SHIPPED', 'DELIVERED', 'CLOSED', 'CANCELLED'))
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    stripe_payment_intent_id VARCHAR(255) NOT NULL,
    stripe_charge_id VARCHAR(255),
    platform_charge_id TEXT NOT NULL,
    transfer_group TEXT,
    ensureback_fee_cents INTEGER NOT NULL,
    gross_amount_cents INTEGER NOT NULL,
    net_amount_cents INTEGER NOT NULL,
    currency VARCHAR(16) NOT NULL,
    escrow_status VARCHAR(32) NOT NULL,
    capture_mode VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT transactions_amounts_check CHECK (gross_amount_cents >= 0 AND net_amount_cents >= 0),
    CONSTRAINT transactions_escrow_status_check CHECK (escrow_status IN ('HELD', 'RELEASED', 'REFUNDED')),
    CONSTRAINT transactions_capture_mode_check CHECK (capture_mode = 'IMMEDIATE_CAPTURE_AND_HOLD_TRANSFER')
);

CREATE TABLE transfers (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES transactions(id),
    stripe_transfer_id VARCHAR(255) NOT NULL,
    amount_cents INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT transfers_amount_check CHECK (amount_cents >= 0),
    CONSTRAINT transfers_status_check CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED'))
);
