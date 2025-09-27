ALTER TABLE disputes
    ADD COLUMN return_refund_amount_cents INTEGER;

ALTER TABLE disputes
    ADD CONSTRAINT disputes_return_refund_amount_check CHECK (return_refund_amount_cents IS NULL OR return_refund_amount_cents >= 0);

ALTER TABLE disputes
    DROP CONSTRAINT IF EXISTS disputes_status_check;

ALTER TABLE disputes
    ADD CONSTRAINT disputes_status_check CHECK (status IN (
        'OPEN',
        'SELLER_RESPONDED',
        'PARTIAL_REFUND_OFFERED',
        'BUYER_ACCEPTED_PARTIAL',
        'ESCALATED',
        'RETURN_REQUESTED',
        'RETURN_IN_TRANSIT',
        'RETURN_DELIVERED',
        'RETURN_DISPUTED',
        'RESOLVED_BUYER',
        'RESOLVED_SELLER',
        'CLOSED'
    ));

CREATE TABLE return_shipments (
    id UUID PRIMARY KEY,
    dispute_id UUID NOT NULL REFERENCES disputes(id) ON DELETE CASCADE,
    carrier_code VARCHAR(64) NOT NULL,
    tracking_number VARCHAR(128) NOT NULL,
    aftership_id VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    raw JSONB NOT NULL DEFAULT '{}'::jsonb,
    delivered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT return_shipments_status_check CHECK (status IN ('CREATED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'EXCEPTION')),
    CONSTRAINT return_shipments_dispute_unique UNIQUE (dispute_id)
);

CREATE INDEX return_shipments_tracking_number_idx ON return_shipments(tracking_number);