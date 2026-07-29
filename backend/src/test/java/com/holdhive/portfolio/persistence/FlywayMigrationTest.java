package com.holdhive.portfolio.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void appliesPortfolioSchemaAndMultiAssetInstrumentMigration() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        assertThat(tableExists(jdbcTemplate, "portfolio")).isTrue();
        assertThat(tableExists(jdbcTemplate, "instrument")).isTrue();
        assertThat(tableExists(jdbcTemplate, "holding")).isTrue();
        assertThat(tableExists(jdbcTemplate, "price_snapshot")).isTrue();
        assertThat(columnExists(jdbcTemplate, "instrument", "ticker")).isTrue();
        assertThat(columnExists(jdbcTemplate, "instrument", "exchange_code")).isTrue();
        assertThat(columnExists(jdbcTemplate, "instrument", "display_name")).isTrue();
        assertThat(columnExists(jdbcTemplate, "instrument", "asset_type")).isTrue();
        assertThat(columnExists(jdbcTemplate, "instrument", "provider")).isTrue();
        assertThat(columnExists(jdbcTemplate, "instrument", "provider_quote_id")).isTrue();
        assertThat(columnExists(jdbcTemplate, "holding", "version")).isTrue();
        assertThat(columnExists(jdbcTemplate, "price_snapshot", "provider")).isTrue();
        assertThat(columnExists(jdbcTemplate, "price_snapshot", "observed_at")).isTrue();
        assertThat(columnExists(jdbcTemplate, "price_snapshot", "is_demo")).isTrue();
        assertThat(appliedFlywayVersions(jdbcTemplate)).containsExactly("1", "2", "3", "4");
        assertThat(defaultPortfolioCount(jdbcTemplate)).isEqualTo(1);
    }

    @Test
    void seedsDefaultPortfolioWithDemoHoldingsAcrossAllSupportedAssetTypes() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Long portfolioId = jdbcTemplate.queryForObject(
            "SELECT id FROM portfolio WHERE name = 'My Portfolio' ORDER BY id LIMIT 1",
            Long.class
        );

        assertThat(portfolioId).isNotNull();
        assertThat(defaultPortfolioHoldingCount(jdbcTemplate, portfolioId)).isGreaterThanOrEqualTo(12);
        assertThat(defaultPortfolioAssetTypes(jdbcTemplate, portfolioId))
            .contains("STOCK", "ETF", "MUTUAL_FUND", "CRYPTO", "CASH", "BANK_DEPOSIT");
        assertThat(providerQuoteIds(jdbcTemplate, portfolioId))
            .contains("105.AAPL", "105.MSFT", "105.VOO", "MF:FXAIX", "CRYPTO:BTC", "CRYPTO:ETH")
            .contains("UNKNOWN:PRIVATE");
        assertThat(latestPriceSnapshotCount(jdbcTemplate, portfolioId)).isGreaterThanOrEqualTo(8);
        assertThat(fixedAssetAveragePurchasePrices(jdbcTemplate, portfolioId)).containsOnly("1.00000000");
    }

    @Test
    void enforcesInstrumentAssetTypeConstraint() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        assertThatThrownBy(() -> jdbcTemplate.update("""
            INSERT INTO instrument (ticker, display_name, exchange_code, currency, asset_type)
            VALUES ('BAD1', 'Unsupported Asset', 'TEST', 'USD', 'OPTION')
            """))
            .hasRootCauseInstanceOf(SQLException.class);
    }

    @Test
    void enforcesHoldingUniquenessAndRetainsInstrumentAfterDelete() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Long portfolioId = jdbcTemplate.queryForObject(
            "SELECT id FROM portfolio ORDER BY id LIMIT 1",
            Long.class
        );
        jdbcTemplate.update("""
            INSERT INTO instrument (ticker, display_name, exchange_code, currency, asset_type)
            VALUES ('FLYWAY_DUP_TEST', 'Flyway Duplicate Test', 'NASDAQ', 'USD', 'STOCK')
            """);
        Long instrumentId = jdbcTemplate.queryForObject(
            "SELECT id FROM instrument WHERE ticker = 'FLYWAY_DUP_TEST'",
            Long.class
        );

        jdbcTemplate.update(
            "INSERT INTO holding (portfolio_id, instrument_id, quantity, average_purchase_price) VALUES (?, ?, ?, ?)",
            portfolioId,
            instrumentId,
            "10.00000000",
            "175.50000000"
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO holding (portfolio_id, instrument_id, quantity, average_purchase_price) VALUES (?, ?, ?, ?)",
            portfolioId,
            instrumentId,
            "5.00000000",
            "100.00000000"
        )).hasRootCauseInstanceOf(SQLException.class);

        jdbcTemplate.update("DELETE FROM holding WHERE instrument_id = ?", instrumentId);

        Integer instrumentCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM instrument WHERE id = ?",
            Integer.class,
            instrumentId
        );
        assertThat(instrumentCount).isEqualTo(1);
    }

    private static boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = SCHEMA()
              AND TABLE_NAME = ?
            """, Integer.class, tableName);
        return count != null && count == 1;
    }

    private static boolean columnExists(JdbcTemplate jdbcTemplate, String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = SCHEMA()
              AND TABLE_NAME = ?
              AND COLUMN_NAME = ?
            """, Integer.class, tableName, columnName);
        return count != null && count == 1;
    }

    private static String[] appliedFlywayVersions(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForList("""
            SELECT version
            FROM flyway_schema_history
            WHERE success = TRUE
              AND version IS NOT NULL
            ORDER BY installed_rank
            """, String.class).toArray(String[]::new);
    }

    private static int defaultPortfolioCount(JdbcTemplate jdbcTemplate) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM portfolio
            WHERE name = 'My Portfolio'
              AND base_currency = 'USD'
            """, Integer.class);
        return count == null ? 0 : count;
    }

    private static int defaultPortfolioHoldingCount(JdbcTemplate jdbcTemplate, Long portfolioId) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM holding
            WHERE portfolio_id = ?
            """, Integer.class, portfolioId);
        return count == null ? 0 : count;
    }

    private static String[] defaultPortfolioAssetTypes(JdbcTemplate jdbcTemplate, Long portfolioId) {
        return jdbcTemplate.queryForList("""
            SELECT DISTINCT i.asset_type
            FROM holding h
            JOIN instrument i ON i.id = h.instrument_id
            WHERE h.portfolio_id = ?
            ORDER BY i.asset_type
            """, String.class, portfolioId).toArray(String[]::new);
    }

    private static String[] providerQuoteIds(JdbcTemplate jdbcTemplate, Long portfolioId) {
        return jdbcTemplate.queryForList("""
            SELECT i.provider_quote_id
            FROM holding h
            JOIN instrument i ON i.id = h.instrument_id
            WHERE h.portfolio_id = ?
              AND i.provider_quote_id IS NOT NULL
            ORDER BY i.provider_quote_id
            """, String.class, portfolioId).toArray(String[]::new);
    }

    private static int latestPriceSnapshotCount(JdbcTemplate jdbcTemplate, Long portfolioId) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM price_snapshot ps
            JOIN holding h ON h.instrument_id = ps.instrument_id
            WHERE h.portfolio_id = ?
            """, Integer.class, portfolioId);
        return count == null ? 0 : count;
    }

    private static String[] fixedAssetAveragePurchasePrices(JdbcTemplate jdbcTemplate, Long portfolioId) {
        return jdbcTemplate.queryForList("""
            SELECT CAST(h.average_purchase_price AS VARCHAR)
            FROM holding h
            JOIN instrument i ON i.id = h.instrument_id
            WHERE h.portfolio_id = ?
              AND i.asset_type IN ('CASH', 'BANK_DEPOSIT')
            ORDER BY i.asset_type
            """, String.class, portfolioId).toArray(String[]::new);
    }
}
