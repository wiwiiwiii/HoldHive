CREATE TABLE portfolio (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    base_currency CHAR(3) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE instrument (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    name VARCHAR(160),
    exchange VARCHAR(32),
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_instrument_symbol_exchange UNIQUE (symbol, exchange)
);

CREATE TABLE holding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    instrument_id BIGINT NOT NULL,
    quantity DECIMAL(20, 6) NOT NULL,
    average_purchase_price DECIMAL(20, 6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_holding_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id),
    CONSTRAINT fk_holding_instrument FOREIGN KEY (instrument_id) REFERENCES instrument(id),
    CONSTRAINT uq_holding_portfolio_instrument UNIQUE (portfolio_id, instrument_id),
    CONSTRAINT ck_holding_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_holding_average_price_non_negative CHECK (average_purchase_price >= 0)
);

CREATE TABLE price_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    instrument_id BIGINT NOT NULL,
    price DECIMAL(20, 6) NOT NULL,
    source VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    captured_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_price_snapshot_instrument FOREIGN KEY (instrument_id) REFERENCES instrument(id),
    CONSTRAINT ck_price_snapshot_price_non_negative CHECK (price >= 0)
);

CREATE INDEX idx_holding_portfolio_id ON holding(portfolio_id);
CREATE INDEX idx_price_snapshot_instrument_time ON price_snapshot(instrument_id, captured_at);
