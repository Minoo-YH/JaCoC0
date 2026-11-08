import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class DBConnectionTest {

    private static final String DATABASE_PATH_PROPERTY = "temperature.db.path";

    @TempDir
    Path temporaryDirectory;

    private String previousDatabasePath;

    @BeforeEach
    void useIsolatedDatabase() {
        previousDatabasePath = System.getProperty(DATABASE_PATH_PROPERTY);
        System.setProperty(
                DATABASE_PATH_PROPERTY,
                temporaryDirectory.resolve("temperature-test.db").toString()
        );
    }

    @AfterEach
    void restoreDatabasePath() {
        if (previousDatabasePath == null) {
            System.clearProperty(DATABASE_PATH_PROPERTY);
        } else {
            System.setProperty(DATABASE_PATH_PROPERTY, previousDatabasePath);
        }
    }

    @Test
    void testDatabaseConnection() throws Exception {
        try (Connection connection = DBConnection.connect()) {
            assertNotNull(connection);
            assertFalse(connection.isClosed());
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("PRAGMA foreign_keys")) {
                assertTrue(result.next());
                assertEquals(1, result.getInt(1));
            }
        }
    }

    @Test
    void testInitializeDatabase() {
        assertDoesNotThrow(DBConnection::initializeDatabase);
    }

    @Test
    void testInitializationIsIdempotentAndSeedsAllLocalizedUnits() throws Exception {
        DBConnection.initializeDatabase();
        DBConnection.initializeDatabase();

        try (Connection connection = DBConnection.connect();
             Statement statement = connection.createStatement()) {
            try (ResultSet result = statement.executeQuery(
                    "SELECT COUNT(*) FROM unit_translation")) {
                assertTrue(result.next());
                assertEquals(60, result.getInt(1));
            }

            try (ResultSet result = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM measurement_unit m
                    WHERE (SELECT COUNT(*) FROM unit_translation t WHERE t.unit_id = m.id) = 5
                    """)) {
                assertTrue(result.next());
                assertEquals(12, result.getInt(1));
            }

            try (ResultSet result = statement.executeQuery("""
                    SELECT name FROM unit_translation t
                    JOIN measurement_unit m ON m.id = t.unit_id
                    WHERE m.domain = 'speed' AND m.code = 'meters_per_second' AND t.lang = 'ja'
                    """)) {
                assertTrue(result.next());
                assertEquals("メートル毎秒", result.getString(1));
            }

            try (ResultSet result = statement.executeQuery("""
                    SELECT name FROM unit_translation t
                    JOIN measurement_unit m ON m.id = t.unit_id
                    WHERE m.domain = 'temperature' AND m.code = 'celsius' AND t.lang = 'ar'
                    """)) {
                assertTrue(result.next());
                assertEquals("درجة مئوية", result.getString(1));
            }

            try (ResultSet violations = statement.executeQuery("PRAGMA foreign_key_check")) {
                assertFalse(violations.next());
            }
        }
    }

    @Test
    void testInitializationPreservesExistingConversionRecords() throws Exception {
        try (Connection connection = DBConnection.connect();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE temperature_unit (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL UNIQUE,
                        symbol TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE temperature_record (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        input_value REAL NOT NULL,
                        result_value REAL NOT NULL,
                        unit_id INTEGER NOT NULL,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (unit_id) REFERENCES temperature_unit(id)
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO temperature_unit (id, name, symbol)
                    VALUES (1, 'Celsius', 'C')
                    """);
            for (int id = 1; id <= 22; id++) {
                statement.executeUpdate(
                        "INSERT INTO temperature_record (id, input_value, result_value, unit_id, created_at) " +
                                "VALUES (" + id + ", " + id + ", " + (id * 2) + ", 1, '2026-01-01 00:00:00')"
                );
            }
        }

        DBConnection.initializeDatabase();

        try (Connection connection = DBConnection.connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT id, input_value, result_value, unit_id, created_at
                     FROM temperature_record
                     ORDER BY id
                     """)) {
            int expectedId = 1;
            while (result.next()) {
                assertEquals(expectedId, result.getInt("id"));
                assertEquals(expectedId, result.getDouble("input_value"));
                assertEquals(expectedId * 2, result.getDouble("result_value"));
                assertEquals(1, result.getInt("unit_id"));
                assertEquals("2026-01-01 00:00:00", result.getString("created_at"));
                expectedId++;
            }
            assertEquals(23, expectedId);
        }
    }
}
