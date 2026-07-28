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
        assertThat(appliedFlywayVersions(jdbcTemplate)).containsExactly("1", "2", "3");
        assertThat(defaultPortfolioCount(jdbcTemplate)).isEqualTo(1);
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
            VALUES ('AAPL', 'Apple Inc.', 'NASDAQ', 'USD', 'STOCK')
            """);
        Long instrumentId = jdbcTemplate.queryForObject(
            "SELECT id FROM instrument WHERE ticker = 'AAPL'",
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
}
