package com.ensureback;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FlywayMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads_andFlywayMigrationsApplied() throws Exception {
        try (var conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();

            boolean hasHistory = tableExists(meta, "FLYWAY_SCHEMA_HISTORY");
            boolean hasUsers = tableExists(meta, "USERS");
            boolean hasMerchants = tableExists(meta, "MERCHANTS");

            assertThat(hasHistory).as("flyway schema history exists").isTrue();
            assertThat(hasUsers).as("users table exists").isTrue();
            assertThat(hasMerchants).as("merchants table exists").isTrue();
        }
    }

    private boolean tableExists(DatabaseMetaData meta, String tableName) throws SQLException {
        try (ResultSet rs = meta.getTables(null, null, tableName, null)) {
            if (rs.next()) return true;
        }
        // Some drivers require lower-case lookups depending on identifier handling
        try (ResultSet rs = meta.getTables(null, null, tableName.toLowerCase(), null)) {
            return rs.next();
        }
    }
}

