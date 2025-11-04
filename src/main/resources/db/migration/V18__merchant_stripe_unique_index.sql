ALTER TABLE merchants ADD COLUMN IF NOT EXISTS stripe_account_id VARCHAR(255);

-- Normalize existing data: convert empty strings to NULL
UPDATE merchants SET stripe_account_id = NULL WHERE stripe_account_id = '';

-- Disallow empty strings going forward (permit NULLs)
ALTER TABLE merchants ADD CONSTRAINT merchants_stripe_id_nonempty
    CHECK (stripe_account_id IS NULL OR stripe_account_id <> '');

-- Ensure uniqueness of non-null values (NULLs can repeat)
CREATE UNIQUE INDEX IF NOT EXISTS uq_merchants_stripe_account
    ON merchants (stripe_account_id);
