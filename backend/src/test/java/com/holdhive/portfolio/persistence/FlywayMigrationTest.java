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
        assertThat(columnExists(jdbcTemplate, "instrument", "asset_type")).isTrue();
        assertThat(columnExists(jdbcTemplate, "instrument", "provider")).isTrue();
        assertThat(columnExists(jdbcTemplate, "instrument", "provider_quote_id")).isTrue();
        assertThat(appliedFlywayVersions(jdbcTemplate)).containsExactly("1", "2");
    }

    @Test
    void enforcesInstrumentAssetTypeConstraint() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        assertThatThrownBy(() -> jdbcTemplate.update("""
            INSERT INTO instrument (symbol, name, exchange, currency, asset_type)
            VALUES ('BAD1', 'Unsupported Asset', 'TEST', 'USD', 'OPTION')
            """))
            .hasRootCauseInstanceOf(SQLException.class);
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
}
