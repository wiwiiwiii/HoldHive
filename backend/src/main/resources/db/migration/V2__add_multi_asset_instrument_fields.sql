ALTER TABLE instrument
    ADD COLUMN asset_type VARCHAR(24) NOT NULL DEFAULT 'STOCK';

ALTER TABLE instrument
    ADD COLUMN provider VARCHAR(64);

ALTER TABLE instrument
    ADD COLUMN provider_quote_id VARCHAR(64);

ALTER TABLE instrument
    ADD CONSTRAINT ck_instrument_asset_type
        CHECK (asset_type IN ('STOCK', 'ETF', 'MUTUAL_FUND', 'CRYPTO', 'CASH', 'BANK_DEPOSIT'));

CREATE INDEX idx_instrument_provider_quote_id ON instrument(provider, provider_quote_id);
