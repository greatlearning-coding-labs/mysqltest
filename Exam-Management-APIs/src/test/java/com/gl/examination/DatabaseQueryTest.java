package com.gl.examination;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class DatabaseQueryTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void testCreateTableAndRunSelectQueries() throws Exception {
        // Read SQL file
        String sqlFile = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("Queries.sql")))
                .lines().collect(Collectors.joining("\n"));

        // Split SQL file into individual statements
        List<String> sqlStatements = new ArrayList<>();
        for (String stmt : sqlFile.split(";")) {
            stmt = stmt.trim();
            if (!stmt.isEmpty()) sqlStatements.add(stmt);
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Clean up before test
            stmt.execute("DROP TABLE IF EXISTS users");

            // Run all statements
            for (String sql : sqlStatements) {
                if (sql.toLowerCase().startsWith("select")) continue; // skip SELECTs for now
                stmt.execute(sql);
            }

            // === Validate table schema ===
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "users", new String[]{"TABLE"});
            assertTrue(tables.next(), "Table 'users' was not created.");

            ResultSet columns = metaData.getColumns(null, null, "users", null);
            boolean hasId = false, hasName = false;

            while (columns.next()) {
                String col = columns.getString("COLUMN_NAME");
                String type = columns.getString("TYPE_NAME");
                if (col.equalsIgnoreCase("id")) {
                    hasId = true;
                    assertTrue(type.toLowerCase().contains("int"), "'id' column should be INT, got: " + type);
                }
                if (col.equalsIgnoreCase("name")) {
                    hasName = true;
                    assertTrue(type.toLowerCase().contains("varchar"), "'name' column should be VARCHAR, got: " + type);
                }
            }

            assertTrue(hasId, "Missing 'id' column");
            assertTrue(hasName, "Missing 'name' column");

            // === Execute and validate SELECT queries ===
            for (String sql : sqlStatements) {
                if (!sql.toLowerCase().startsWith("select")) continue;
                ResultSet rs = stmt.executeQuery(sql);
                assertTrue(rs.next(), "SELECT query returned no result: " + sql);

                // Additional check: if COUNT(*), value should be > 0
                if (sql.toLowerCase().contains("count")) {
                    int count = rs.getInt(1);
                    assertTrue(count > 0, "Expected non-zero count in: " + sql);
                }
            }
        }
    }
}
