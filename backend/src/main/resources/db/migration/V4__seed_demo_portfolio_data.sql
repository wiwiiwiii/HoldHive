INSERT INTO instrument (ticker, display_name, exchange_code, currency, asset_type, provider, provider_quote_id)
SELECT 'AAPL', 'Apple Inc.', 'NASDAQ', 'USD', 'STOCK', 'EASTMONEY', '105.AAPL'
WHERE NOT EXISTS (
    SELECT 1 FROM instrument WHERE asset_type = 'STOCK' AND ticker = 'AAPL' AND exchange_code = 'NASDAQ'
);

INSERT INTO instrument (ticker, display_name, exchange_code, currency, asset_type, provider, provider_quote_id)
SELECT 'MSFT', 'Microsoft Corp.', 'NASDAQ', 'USD', 'STOCK', 'EASTMONEY', '105.MSFT'
WHERE NOT EXISTS (
    SELECT 1 FROM instrument WHERE asset_type = 'STOCK' AND ticker = 'MSFT' AND exchange_code = 'NASDAQ'
);

INSERT INTO instrument (ticker, display_name, exchange_code, currency, asset_type, provider, provider_quote_id)
SELECT 'NVDA', 'NVIDIA Corp.', 'NASDAQ', 'USD', 'STOCK', 'EASTMONEY', '105.NVDA'
WHERE NOT EXISTS (
    SELECT 1 FROM instrument WHERE asset_type = 'STOCK' AND ticker = 'NVDA' AND exchange_code = 'NASDAQ'
);

INSERT INTO instrument (ticker, display_name, exchange_code, currency, asset_type, provider, provider_quote_id)
SELECT '600519', 'Kweichow Moutai Co., Ltd.', 'SH', 'CNY', 'STOCK', 'EASTMONEY', '1.600519'
WHERE NOT EXISTS (
    SELECT 1 FROM instrument WHERE asset_type = 'STOCK' AND ticker = '600519' AND exchange_code = 'SH'
);

INSERT INTO instrument (ticker, display_name, exchange_code, currency, asset_type, provider, provider_quote_id)
SELECT 'VOO', 'Vanguard S&P 500 ETF', 'NYSE', 'USD', 'ETF', 'EASTMONEY', '105.VOO'
WHERE NOT EXISTS (
    SELECT 1 FROM instrument WHERE asset_type = 'ETF' AND ticker = 'VOO' AND exchange_code = 'NYSE'
);

INSERT INTO instrument (ticker, display_name, exchange_code, currency, asset_type, provider, provider_quote_id)
SELECT 'QQQ', 'Invesco QQQ Trust', 'NASDAQ', 'USD', 'ETF', 'DEMO', '105.QQQ'
WHERE NOT EXISTS (
    SELECT 1 FROM instrument WHERE asset_type = 'ETF' AND ticker = 'QQQ' AND exchange_code = 'NASDAQ'
);

INSERT INTO instrument (ticker, display_name, exchange_code, currency, asset_type, provider, provider_quote_id)
SELECT 'FXAIX', 'Fidelity 500 Index Fund', 'FUND', 'USD', 'MUTUAL_FUND', 'DEMO', 'MF:FXAIX'
WHERE NOT EXISTS (
    SELECT 1 FROM instrument WHERE asset_type = 'MUTUAL_FUND' AND ticker = 'FXAIX' AND exchange_code = 'FUND'
);

INSERT INTO instrument (ticker, display_name, exchange_code, currency, asset_type, provider, provider_quote_id)
SELECT '005827', 'E Fund Blue Chip Selected Mixed Fund', 'FUND', 'CNY', 'MUTUAL_FUND', 'EASTMONEY', 'FUND:005827'
WHERE NOT EXISTS (
    SELECT 1 FROM instrument WHERE asset_type = 'MUTUAL_FUND' AND ticker = '005827' AND exchange_code = 'FUND'
);

INSERT INTO instrument (ticker, display_name, exchange_code, currency, asset_type, provider, provider_quote_id)
SELECT 'BTC', 'Bitcoin', 'CRYPTO', 'USD', 'CRYPTO', 'COINGECKO', 'CRYPTO:BTC'
WHERE NOT EXISTS (
    SELECT 1 FROM instrument WHERE asset_type = 'CRYPTO' AND ticker = 'BTC' AND exchange_code = 'CRYPTO'
);

INSERT INTO instrument (ticker, display_name, exchange_code, currency, asset_type, provider, provider_quote_id)
SELECT 'ETH', 'Ethereum', 'CRYPTO', 'USD', 'CRYPTO', 'COINGECKO', 'CRYPTO:ETH'
WHERE NOT EXISTS (
    SELECT 1 FROM instrument WHERE asset_type = 'CRYPTO' AND ticker = 'ETH' AND exchange_code = 'CRYPTO'
);

INSERT INTO instrument (ticker, display_name, exchange_code, currency, asset_type, provider, provider_quote_id)
SELECT 'USD', 'US Dollar Cash', 'CASH', 'USD', 'CASH', 'FIXED', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM instrument WHERE asset_type = 'CASH' AND ticker = 'USD' AND exchange_code = 'CASH'
);

INSERT INTO instrument (ticker, display_name, exchange_code, currency, asset_type, provider, provider_quote_id)
SELECT 'USD_DEPOSIT', 'USD Bank Deposit', 'BANK', 'USD', 'BANK_DEPOSIT', 'FIXED', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM instrument WHERE asset_type = 'BANK_DEPOSIT' AND ticker = 'USD_DEPOSIT' AND exchange_code = 'BANK'
);

INSERT INTO instrument (ticker, display_name, exchange_code, currency, asset_type, provider, provider_quote_id)
SELECT 'PRIVATE_NOTE', 'Private Credit Note', 'OTC', 'USD', 'STOCK', 'DEMO', 'UNKNOWN:PRIVATE'
WHERE NOT EXISTS (
    SELECT 1 FROM instrument WHERE asset_type = 'STOCK' AND ticker = 'PRIVATE_NOTE' AND exchange_code = 'OTC'
);

INSERT INTO holding (portfolio_id, instrument_id, quantity, average_purchase_price)
SELECT p.id, i.id, 12.00000000, 175.00000000
FROM portfolio p, instrument i
WHERE p.name = 'My Portfolio'
  AND i.asset_type = 'STOCK'
  AND i.ticker = 'AAPL'
  AND i.exchange_code = 'NASDAQ'
  AND NOT EXISTS (SELECT 1 FROM holding h WHERE h.portfolio_id = p.id AND h.instrument_id = i.id);

INSERT INTO holding (portfolio_id, instrument_id, quantity, average_purchase_price)
SELECT p.id, i.id, 6.00000000, 295.00000000
FROM portfolio p, instrument i
WHERE p.name = 'My Portfolio'
  AND i.asset_type = 'STOCK'
  AND i.ticker = 'MSFT'
  AND i.exchange_code = 'NASDAQ'
  AND NOT EXISTS (SELECT 1 FROM holding h WHERE h.portfolio_id = p.id AND h.instrument_id = i.id);

INSERT INTO holding (portfolio_id, instrument_id, quantity, average_purchase_price)
SELECT p.id, i.id, 8.00000000, 720.00000000
FROM portfolio p, instrument i
WHERE p.name = 'My Portfolio'
  AND i.asset_type = 'STOCK'
  AND i.ticker = 'NVDA'
  AND i.exchange_code = 'NASDAQ'
  AND NOT EXISTS (SELECT 1 FROM holding h WHERE h.portfolio_id = p.id AND h.instrument_id = i.id);

INSERT INTO holding (portfolio_id, instrument_id, quantity, average_purchase_price)
SELECT p.id, i.id, 1.00000000, 1500.00000000
FROM portfolio p, instrument i
WHERE p.name = 'My Portfolio'
  AND i.asset_type = 'STOCK'
  AND i.ticker = '600519'
  AND i.exchange_code = 'SH'
  AND NOT EXISTS (SELECT 1 FROM holding h WHERE h.portfolio_id = p.id AND h.instrument_id = i.id);

INSERT INTO holding (portfolio_id, instrument_id, quantity, average_purchase_price)
SELECT p.id, i.id, 3.00000000, 450.00000000
FROM portfolio p, instrument i
WHERE p.name = 'My Portfolio'
  AND i.asset_type = 'ETF'
  AND i.ticker = 'VOO'
  AND i.exchange_code = 'NYSE'
  AND NOT EXISTS (SELECT 1 FROM holding h WHERE h.portfolio_id = p.id AND h.instrument_id = i.id);

INSERT INTO holding (portfolio_id, instrument_id, quantity, average_purchase_price)
SELECT p.id, i.id, 2.00000000, 430.00000000
FROM portfolio p, instrument i
WHERE p.name = 'My Portfolio'
  AND i.asset_type = 'ETF'
  AND i.ticker = 'QQQ'
  AND i.exchange_code = 'NASDAQ'
  AND NOT EXISTS (SELECT 1 FROM holding h WHERE h.portfolio_id = p.id AND h.instrument_id = i.id);

INSERT INTO holding (portfolio_id, instrument_id, quantity, average_purchase_price)
SELECT p.id, i.id, 7.00000000, 190.00000000
FROM portfolio p, instrument i
WHERE p.name = 'My Portfolio'
  AND i.asset_type = 'MUTUAL_FUND'
  AND i.ticker = 'FXAIX'
  AND i.exchange_code = 'FUND'
  AND NOT EXISTS (SELECT 1 FROM holding h WHERE h.portfolio_id = p.id AND h.instrument_id = i.id);

INSERT INTO holding (portfolio_id, instrument_id, quantity, average_purchase_price)
SELECT p.id, i.id, 500.00000000, 1.85000000
FROM portfolio p, instrument i
WHERE p.name = 'My Portfolio'
  AND i.asset_type = 'MUTUAL_FUND'
  AND i.ticker = '005827'
  AND i.exchange_code = 'FUND'
  AND NOT EXISTS (SELECT 1 FROM holding h WHERE h.portfolio_id = p.id AND h.instrument_id = i.id);

INSERT INTO holding (portfolio_id, instrument_id, quantity, average_purchase_price)
SELECT p.id, i.id, 0.08000000, 52000.00000000
FROM portfolio p, instrument i
WHERE p.name = 'My Portfolio'
  AND i.asset_type = 'CRYPTO'
  AND i.ticker = 'BTC'
  AND i.exchange_code = 'CRYPTO'
  AND NOT EXISTS (SELECT 1 FROM holding h WHERE h.portfolio_id = p.id AND h.instrument_id = i.id);

INSERT INTO holding (portfolio_id, instrument_id, quantity, average_purchase_price)
SELECT p.id, i.id, 1.25000000, 3200.00000000
FROM portfolio p, instrument i
WHERE p.name = 'My Portfolio'
  AND i.asset_type = 'CRYPTO'
  AND i.ticker = 'ETH'
  AND i.exchange_code = 'CRYPTO'
  AND NOT EXISTS (SELECT 1 FROM holding h WHERE h.portfolio_id = p.id AND h.instrument_id = i.id);

INSERT INTO holding (portfolio_id, instrument_id, quantity, average_purchase_price)
SELECT p.id, i.id, 3500.00000000, 1.00000000
FROM portfolio p, instrument i
WHERE p.name = 'My Portfolio'
  AND i.asset_type = 'CASH'
  AND i.ticker = 'USD'
  AND i.exchange_code = 'CASH'
  AND NOT EXISTS (SELECT 1 FROM holding h WHERE h.portfolio_id = p.id AND h.instrument_id = i.id);

INSERT INTO holding (portfolio_id, instrument_id, quantity, average_purchase_price)
SELECT p.id, i.id, 6500.00000000, 1.00000000
FROM portfolio p, instrument i
WHERE p.name = 'My Portfolio'
  AND i.asset_type = 'BANK_DEPOSIT'
  AND i.ticker = 'USD_DEPOSIT'
  AND i.exchange_code = 'BANK'
  AND NOT EXISTS (SELECT 1 FROM holding h WHERE h.portfolio_id = p.id AND h.instrument_id = i.id);

INSERT INTO holding (portfolio_id, instrument_id, quantity, average_purchase_price)
SELECT p.id, i.id, 1.00000000, 1000.00000000
FROM portfolio p, instrument i
WHERE p.name = 'My Portfolio'
  AND i.asset_type = 'STOCK'
  AND i.ticker = 'PRIVATE_NOTE'
  AND i.exchange_code = 'OTC'
  AND NOT EXISTS (SELECT 1 FROM holding h WHERE h.portfolio_id = p.id AND h.instrument_id = i.id);

INSERT INTO price_snapshot (instrument_id, price, currency, provider, status, is_demo, observed_at)
SELECT i.id, 210.25000000, i.currency, 'SEED_CACHE', 'CACHED', FALSE, CURRENT_TIMESTAMP(6)
FROM instrument i
WHERE i.asset_type = 'STOCK' AND i.ticker = 'AAPL' AND i.exchange_code = 'NASDAQ';

INSERT INTO price_snapshot (instrument_id, price, currency, provider, status, is_demo, observed_at)
SELECT i.id, 330.00000000, i.currency, 'SEED_CACHE', 'CACHED', FALSE, CURRENT_TIMESTAMP(6)
FROM instrument i
WHERE i.asset_type = 'STOCK' AND i.ticker = 'MSFT' AND i.exchange_code = 'NASDAQ';

INSERT INTO price_snapshot (instrument_id, price, currency, provider, status, is_demo, observed_at)
SELECT i.id, 940.00000000, i.currency, 'SEED_CACHE', 'CACHED', FALSE, CURRENT_TIMESTAMP(6)
FROM instrument i
WHERE i.asset_type = 'STOCK' AND i.ticker = 'NVDA' AND i.exchange_code = 'NASDAQ';

INSERT INTO price_snapshot (instrument_id, price, currency, provider, status, is_demo, observed_at)
SELECT i.id, 1680.00000000, i.currency, 'SEED_CACHE', 'CACHED', FALSE, CURRENT_TIMESTAMP(6)
FROM instrument i
WHERE i.asset_type = 'STOCK' AND i.ticker = '600519' AND i.exchange_code = 'SH';

INSERT INTO price_snapshot (instrument_id, price, currency, provider, status, is_demo, observed_at)
SELECT i.id, 510.40000000, i.currency, 'SEED_CACHE', 'CACHED', FALSE, CURRENT_TIMESTAMP(6)
FROM instrument i
WHERE i.asset_type = 'ETF' AND i.ticker = 'VOO' AND i.exchange_code = 'NYSE';

INSERT INTO price_snapshot (instrument_id, price, currency, provider, status, is_demo, observed_at)
SELECT i.id, 485.75000000, i.currency, 'SEED_CACHE', 'CACHED', FALSE, CURRENT_TIMESTAMP(6)
FROM instrument i
WHERE i.asset_type = 'ETF' AND i.ticker = 'QQQ' AND i.exchange_code = 'NASDAQ';

INSERT INTO price_snapshot (instrument_id, price, currency, provider, status, is_demo, observed_at)
SELECT i.id, 205.35000000, i.currency, 'SEED_CACHE', 'CACHED', FALSE, CURRENT_TIMESTAMP(6)
FROM instrument i
WHERE i.asset_type = 'MUTUAL_FUND' AND i.ticker = 'FXAIX' AND i.exchange_code = 'FUND';

INSERT INTO price_snapshot (instrument_id, price, currency, provider, status, is_demo, observed_at)
SELECT i.id, 2.11000000, i.currency, 'SEED_CACHE', 'CACHED', FALSE, CURRENT_TIMESTAMP(6)
FROM instrument i
WHERE i.asset_type = 'MUTUAL_FUND' AND i.ticker = '005827' AND i.exchange_code = 'FUND';

INSERT INTO price_snapshot (instrument_id, price, currency, provider, status, is_demo, observed_at)
SELECT i.id, 67500.00000000, i.currency, 'SEED_CACHE', 'CACHED', FALSE, CURRENT_TIMESTAMP(6)
FROM instrument i
WHERE i.asset_type = 'CRYPTO' AND i.ticker = 'BTC' AND i.exchange_code = 'CRYPTO';

INSERT INTO price_snapshot (instrument_id, price, currency, provider, status, is_demo, observed_at)
SELECT i.id, 3650.00000000, i.currency, 'SEED_CACHE', 'CACHED', FALSE, CURRENT_TIMESTAMP(6)
FROM instrument i
WHERE i.asset_type = 'CRYPTO' AND i.ticker = 'ETH' AND i.exchange_code = 'CRYPTO';
