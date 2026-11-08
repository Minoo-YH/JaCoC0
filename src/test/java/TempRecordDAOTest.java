import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class TempRecordDAOTest {

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
        DBConnection.initializeDatabase();
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
    void legacySaveMethodStillStoresExistingFields() throws Exception {
        TempRecordDAO.saveRecord(10.0, 50.0);

        try (Connection connection = DBConnection.connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT * FROM temperature_record WHERE input_value = 10.0 AND result_value = 50.0")) {
            assertTrue(result.next());
            assertEquals(10.0, result.getDouble("input_value"), 0.001);
            assertEquals(50.0, result.getDouble("result_value"), 0.001);
        }

        assertTrue(TempRecordDAO.findRecentConversions(Locale.US).get(0).legacy());
    }

    @Test
    void completeConversionPersistsSourceTargetLanguageAndValues() throws Exception {
        int recordId = TempRecordDAO.saveConversion(
                25.0, "celsius", "fahrenheit", 77.0, "fi"
        );

        try (Connection connection = DBConnection.connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT r.input_value, r.result_value, source.code AS source_code,
                            target.code AS target_code, metadata.language, r.created_at
                     FROM temperature_record r
                     JOIN conversion_metadata metadata ON metadata.record_id = r.id
                     JOIN measurement_unit source ON source.id = metadata.source_unit_id
                     JOIN measurement_unit target ON target.id = metadata.target_unit_id
                     WHERE r.id = """ + recordId)) {
            assertTrue(result.next());
            assertEquals(25.0, result.getDouble("input_value"), 0.001);
            assertEquals(77.0, result.getDouble("result_value"), 0.001);
            assertEquals("celsius", result.getString("source_code"));
            assertEquals("fahrenheit", result.getString("target_code"));
            assertEquals("fi", result.getString("language"));
            assertNotNull(result.getString("created_at"));
        }
    }

    @Test
    void recentHistoryIsOrderedByTimestampThenIdAndLimitedToFive() throws Exception {
        for (int i = 1; i <= 7; i++) {
            TempRecordDAO.saveConversion(i, "celsius", "kelvin", i + 273.15, "en");
        }

        try (Connection connection = DBConnection.connect();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    UPDATE temperature_record
                    SET created_at = CASE id
                        WHEN 1 THEN '2025-01-01 00:00:07'
                        WHEN 2 THEN '2025-01-01 00:00:06'
                        WHEN 3 THEN '2025-01-01 00:00:05'
                        WHEN 4 THEN '2025-01-01 00:00:04'
                        WHEN 5 THEN '2025-01-01 00:00:03'
                        WHEN 6 THEN '2025-01-01 00:00:02'
                        WHEN 7 THEN '2025-01-01 00:00:01'
                    END
                    """);
        }

        List<TempRecordDAO.ConversionHistoryEntry> history =
                TempRecordDAO.findRecentConversions(Locale.US);
        assertEquals(5, history.size());
        assertEquals(List.of(1, 2, 3, 4, 5), history.stream()
                .map(TempRecordDAO.ConversionHistoryEntry::id)
                .toList());
        assertFalse(history.get(0).legacy());
    }

    @Test
    void historyFallsBackToEnglishWhenTranslationIsMissing() throws Exception {
        TempRecordDAO.saveConversion(0.0, "celsius", "kelvin", 273.15, "ja");

        try (Connection connection = DBConnection.connect();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    DELETE FROM unit_translation
                    WHERE lang = 'ja' AND unit_id = (
                        SELECT id FROM measurement_unit
                        WHERE domain = 'temperature' AND code = 'kelvin'
                    )
                    """);
        }

        TempRecordDAO.ConversionHistoryEntry history =
                TempRecordDAO.findRecentConversions(Locale.JAPAN).get(0);
        assertEquals("セルシウス度", history.sourceUnitName());
        assertEquals("Kelvin", history.targetUnitName());
    }

    @Test
    void legacyHistoryMarksUnrecordedTargetAndLanguageExplicitly() throws Exception {
        TempRecordDAO.saveRecord(10.0, 50.0);

        TempRecordDAO.ConversionHistoryEntry entry =
                TempRecordDAO.findRecentConversions(Locale.US).get(0);
        ResourceBundle messages = ResourceBundle.getBundle("MessagesBundle", Locale.US);
        String rendered = TemperatureController.formatHistoryEntry(messages, entry, Locale.US);

        assertTrue(entry.legacy());
        assertNull(entry.targetUnitName());
        assertNull(entry.language());
        assertTrue(rendered.contains(messages.getString("legacyIndicator")));
        assertEquals(1, countOccurrences(rendered, messages.getString("legacyIndicator")));
        assertFalse(rendered.contains(messages.getString("legacyUnknown")));
        assertTrue(rendered.contains("Celsius"));
        assertTrue(rendered.contains("50"));
        assertFalse(rendered.contains("Fahrenheit"));
        assertTrue(rendered.matches(".*\\d{1,2}/\\d{1,2} \\d{2}:\\d{2}.*"));
    }

    @Test
    void completeHistoryRowIsCompactAndIncludesValuesUnitsAndShortTimestamp() throws Exception {
        TempRecordDAO.saveConversion(25.0, "celsius", "fahrenheit", 77.0, "en");

        TempRecordDAO.ConversionHistoryEntry entry =
                TempRecordDAO.findRecentConversions(Locale.US).get(0);
        ResourceBundle messages = ResourceBundle.getBundle("MessagesBundle", Locale.US);
        String rendered = TemperatureController.formatHistoryEntry(messages, entry, Locale.US);

        assertTrue(rendered.contains("25"));
        assertTrue(rendered.contains("Celsius"));
        assertTrue(rendered.contains("77"));
        assertTrue(rendered.contains("Fahrenheit"));
        assertTrue(rendered.matches(".*\\d{1,2}/\\d{1,2} \\d{2}:\\d{2}.*"));
        assertFalse(rendered.contains("Unknown"));
        assertTrue(rendered.length() < 80);
    }

    @Test
    void historyFormattingHasMessagesForAllSupportedLanguages() {
        TempRecordDAO.ConversionHistoryEntry entry =
                new TempRecordDAO.ConversionHistoryEntry(
                        9, 0.0, "Celsius", 32.0, "Fahrenheit", "en",
                        "2026-01-01 10:00:00", false
                );
        List<Locale> locales = List.of(
                Locale.US,
                Locale.forLanguageTag("fi-FI"),
                Locale.forLanguageTag("sv-SE"),
                Locale.JAPAN,
                Locale.forLanguageTag("ar-SA")
        );

        for (Locale locale : locales) {
            ResourceBundle messages = ResourceBundle.getBundle("MessagesBundle", locale);
            assertFalse(messages.getString("history").isBlank());
            assertFalse(messages.getString("historyUnavailable").isBlank());
            assertFalse(messages.getString("saveFailed").isBlank());
            assertTrue(TemperatureController.formatHistoryEntry(messages, entry, locale).contains("Fahrenheit"));
        }
    }

    @Test
    void initializationCanRepeatWithoutChangingConversionRowsOrMetadata() throws Exception {
        TempRecordDAO.saveConversion(4.0, "celsius", "kelvin", 277.15, "sv");
        TempRecordDAO.saveRecord(5.0, 41.0);

        DBConnection.initializeDatabase();
        DBConnection.initializeDatabase();

        try (Connection connection = DBConnection.connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT
                       (SELECT COUNT(*) FROM temperature_record) AS record_count,
                       (SELECT COUNT(*) FROM conversion_metadata) AS metadata_count,
                       (SELECT SUM(result_value) FROM temperature_record) AS result_sum
                     """)) {
            assertTrue(result.next());
            assertEquals(2, result.getInt("record_count"));
            assertEquals(1, result.getInt("metadata_count"));
            assertEquals(318.15, result.getDouble("result_sum"), 0.001);
        }
    }

    @Test
    void newSaveReportsDatabaseErrorsAndLegacyApiLogsThem() {
        String validPath = System.getProperty(DATABASE_PATH_PROPERTY);
        System.setProperty(
                DATABASE_PATH_PROPERTY,
                temporaryDirectory.resolve("missing-parent").resolve("cannot-open.db").toString()
        );
        assertThrows(SQLException.class, () ->
                TempRecordDAO.saveConversion(0.0, "celsius", "kelvin", 273.15, "en"));

        Logger logger = Logger.getLogger(TempRecordDAO.class.getName());
        CapturingHandler handler = new CapturingHandler();
        logger.addHandler(handler);
        try {
            assertDoesNotThrow(() -> TempRecordDAO.saveRecord(1.0, 2.0));
            assertTrue(handler.lastRecord != null);
            assertEquals(java.util.logging.Level.SEVERE, handler.lastRecord.getLevel());
        } finally {
            logger.removeHandler(handler);
            System.setProperty(DATABASE_PATH_PROPERTY, validPath);
        }
    }

    private static final class CapturingHandler extends Handler {
        private LogRecord lastRecord;

        @Override
        public void publish(LogRecord record) {
            lastRecord = record;
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    private static int countOccurrences(String value, String search) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(search, index)) >= 0) {
            count++;
            index += search.length();
        }
        return count;
    }
}
