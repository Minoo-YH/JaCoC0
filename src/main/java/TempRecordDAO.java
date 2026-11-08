import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TempRecordDAO {

    private static final Logger LOGGER = Logger.getLogger(TempRecordDAO.class.getName());
    private static final Set<String> SUPPORTED_LANGUAGES =
            Set.of("en", "fi", "sv", "ja", "ar");

    private static final String FIND_RECENT_CONVERSIONS = """
        SELECT r.id, r.input_value, r.result_value, r.created_at,
               COALESCE(source_localized.name, source_english.name) AS source_name,
               COALESCE(target_localized.name, target_english.name) AS target_name,
               metadata.language,
               metadata.record_id IS NULL AS legacy
        FROM temperature_record r
        LEFT JOIN conversion_metadata metadata ON metadata.record_id = r.id
        LEFT JOIN temperature_unit legacy_source ON legacy_source.id = r.unit_id
        LEFT JOIN temperature_unit_registry legacy_registry
          ON legacy_registry.temperature_unit_id = legacy_source.id
        LEFT JOIN measurement_unit source_unit
          ON source_unit.id = COALESCE(metadata.source_unit_id, legacy_registry.measurement_unit_id)
        LEFT JOIN unit_translation source_localized
          ON source_localized.unit_id = source_unit.id AND source_localized.lang = ?
        LEFT JOIN unit_translation source_english
          ON source_english.unit_id = source_unit.id AND source_english.lang = 'en'
        LEFT JOIN measurement_unit target_unit ON target_unit.id = metadata.target_unit_id
        LEFT JOIN unit_translation target_localized
          ON target_localized.unit_id = target_unit.id AND target_localized.lang = ?
        LEFT JOIN unit_translation target_english
          ON target_english.unit_id = target_unit.id AND target_english.lang = 'en'
        ORDER BY r.created_at DESC, r.id DESC
        LIMIT 5
        """;

    public static void saveRecord(double inputValue, double resultValue) {
        String sql = """
            INSERT INTO temperature_record (input_value, result_value, unit_id)
            SELECT ?, ?, id
            FROM temperature_unit
            WHERE name = 'Celsius'
            """;

        try (Connection connection = DBConnection.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setDouble(1, inputValue);
            statement.setDouble(2, resultValue);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Celsius unit was not found; conversion record was not saved");
            }

            LOGGER.info("Conversion saved to database using the legacy record API.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not save legacy conversion record", e);
        }
    }

    public static int saveConversion(
            double inputValue,
            String sourceUnitCode,
            String targetUnitCode,
            double resultValue,
            String language
    ) throws SQLException {
        String normalizedLanguage = normalizeLanguage(language);
        String sourceUnitSql = """
            SELECT tu.id, measurement.id
            FROM measurement_unit measurement
            JOIN temperature_unit_registry registry
              ON registry.measurement_unit_id = measurement.id
            JOIN temperature_unit tu
              ON tu.id = registry.temperature_unit_id
            WHERE measurement.domain = 'temperature' AND measurement.code = ?
            """;
        String targetUnitSql = """
            SELECT id FROM measurement_unit
            WHERE domain = 'temperature' AND code = ?
            """;
        String insertRecordSql = """
            INSERT INTO temperature_record (input_value, result_value, unit_id)
            VALUES (?, ?, ?)
            """;
        String insertMetadataSql = """
            INSERT INTO conversion_metadata (record_id, source_unit_id, target_unit_id, language)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection connection = DBConnection.connect()) {
            connection.setAutoCommit(false);
            try {
                int legacySourceUnitId;
                int canonicalSourceUnitId;
                try (PreparedStatement statement = connection.prepareStatement(sourceUnitSql)) {
                    statement.setString(1, sourceUnitCode);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Unknown temperature source unit: " + sourceUnitCode);
                        }
                        legacySourceUnitId = result.getInt(1);
                        canonicalSourceUnitId = result.getInt(2);
                    }
                }

                int canonicalTargetUnitId;
                try (PreparedStatement statement = connection.prepareStatement(targetUnitSql)) {
                    statement.setString(1, targetUnitCode);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Unknown temperature target unit: " + targetUnitCode);
                        }
                        canonicalTargetUnitId = result.getInt(1);
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(insertRecordSql)) {
                    statement.setDouble(1, inputValue);
                    statement.setDouble(2, resultValue);
                    statement.setInt(3, legacySourceUnitId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Conversion record insert did not insert exactly one row");
                    }
                }

                int recordId;
                try (Statement statement = connection.createStatement();
                     ResultSet result = statement.executeQuery("SELECT last_insert_rowid()")) {
                    if (!result.next()) {
                        throw new SQLException("Could not retrieve the new conversion record ID");
                    }
                    recordId = result.getInt(1);
                }

                try (PreparedStatement statement = connection.prepareStatement(insertMetadataSql)) {
                    statement.setInt(1, recordId);
                    statement.setInt(2, canonicalSourceUnitId);
                    statement.setInt(3, canonicalTargetUnitId);
                    statement.setString(4, normalizedLanguage);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Conversion metadata insert did not insert exactly one row");
                    }
                }

                connection.commit();
                return recordId;
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw e;
            }
        }
    }

    public static List<ConversionHistoryEntry> findRecentConversions(Locale locale) throws SQLException {
        String language = normalizeLanguage(locale == null ? null : locale.getLanguage());
        List<ConversionHistoryEntry> entries = new ArrayList<>();

        try (Connection connection = DBConnection.connect();
             PreparedStatement statement = connection.prepareStatement(FIND_RECENT_CONVERSIONS)) {
            statement.setString(1, language);
            statement.setString(2, language);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    entries.add(new ConversionHistoryEntry(
                            result.getInt("id"),
                            result.getDouble("input_value"),
                            result.getString("source_name"),
                            result.getDouble("result_value"),
                            result.getString("target_name"),
                            result.getString("language"),
                            result.getString("created_at"),
                            result.getBoolean("legacy")
                    ));
                }
            }
        }
        return List.copyOf(entries);
    }

    private static String normalizeLanguage(String language) {
        return language != null && SUPPORTED_LANGUAGES.contains(language)
                ? language
                : "en";
    }

    public record ConversionHistoryEntry(
            int id,
            double inputValue,
            String sourceUnitName,
            double resultValue,
            String targetUnitName,
            String language,
            String createdAt,
            boolean legacy
    ) {
    }
}
