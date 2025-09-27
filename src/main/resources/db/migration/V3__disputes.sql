CREATE TABLE disputes (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    buyer_email VARCHAR(255) NOT NULL,
    creator_user_id UUID REFERENCES users(id),
    status VARCHAR(48) NOT NULL,
    escalation_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT disputes_status_check CHECK (status IN (
        'OPEN',
        'SELLER_RESPONDED',
        'PARTIAL_REFUND_OFFERED',
        'BUYER_ACCEPTED_PARTIAL',
        'ESCALATED',
        'RESOLVED_BUYER',
        'RESOLVED_SELLER',
        'CLOSED'
    ))
);

CREATE TABLE dispute_messages (
    id UUID PRIMARY KEY,
    dispute_id UUID NOT NULL REFERENCES disputes(id),
    author_role VARCHAR(16) NOT NULL,
    message TEXT NOT NULL,
    evidence_urls JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT dispute_messages_author_role_check CHECK (author_role IN ('BUYER', 'SELLER', 'ADMIN'))
);

CREATE TABLE partial_refund_offers (
    id UUID PRIMARY KEY,
    dispute_id UUID NOT NULL REFERENCES disputes(id),
    amount_cents INTEGER NOT NULL,
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT partial_refund_offers_status_check CHECK (status IN ('OFFERED', 'ACCEPTED', 'REJECTED', 'EXPIRED')),
    CONSTRAINT partial_refund_offers_amount_check CHECK (amount_cents >= 0),
    CONSTRAINT partial_refund_offers_dispute_unique UNIQUE (dispute_id)
);
