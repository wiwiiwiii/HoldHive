ALTER TABLE instrument RENAME COLUMN symbol TO ticker;
ALTER TABLE instrument RENAME COLUMN name TO display_name;
ALTER TABLE instrument RENAME COLUMN exchange TO exchange_code;

UPDATE instrument
SET exchange_code = 'UNKNOWN'
WHERE exchange_code IS NULL OR exchange_code = '';

ALTER TABLE instrument MODIFY COLUMN ticker VARCHAR(32) NOT NULL;
ALTER TABLE instrument MODIFY COLUMN exchange_code VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE instrument MODIFY COLUMN provider VARCHAR(32);
ALTER TABLE instrument ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

ALTER TABLE instrument DROP INDEX uq_instrument_symbol_exchange;
ALTER TABLE instrument
    ADD CONSTRAINT uq_instrument_asset_ticker_exchange
        UNIQUE (asset_type, ticker, exchange_code);

ALTER TABLE holding MODIFY COLUMN quantity DECIMAL(24, 8) NOT NULL;
ALTER TABLE holding MODIFY COLUMN average_purchase_price DECIMAL(24, 8) NOT NULL;
ALTER TABLE holding ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE price_snapshot RENAME COLUMN source TO provider;
ALTER TABLE price_snapshot RENAME COLUMN captured_at TO observed_at;
ALTER TABLE price_snapshot MODIFY COLUMN price DECIMAL(24, 8) NOT NULL;
ALTER TABLE price_snapshot ADD COLUMN currency CHAR(3) NOT NULL DEFAULT 'USD';
ALTER TABLE price_snapshot ADD COLUMN is_demo BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE price_snapshot ADD COLUMN created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

CREATE INDEX idx_holding_portfolio ON holding(portfolio_id);
CREATE INDEX idx_price_latest ON price_snapshot(instrument_id, observed_at DESC);

INSERT INTO portfolio (name, base_currency)
SELECT 'My Portfolio', 'USD'
WHERE NOT EXISTS (
    SELECT 1
    FROM portfolio
);
