import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class LocalizedUnitDAO {

    private static final Set<String> SUPPORTED_LANGUAGES =
            Set.of("en", "fi", "sv", "ja", "ar");

    private static final String FIND_TEMPERATURE_UNITS = """
        SELECT m.id, m.code, tu.symbol,
               COALESCE(localized.name, english.name) AS name,
               COALESCE(localized.description, english.description) AS description
        FROM measurement_unit m
        JOIN temperature_unit_registry registry
          ON registry.measurement_unit_id = m.id
        JOIN temperature_unit tu
          ON tu.id = registry.temperature_unit_id
        LEFT JOIN unit_translation localized
          ON localized.unit_id = m.id AND localized.lang = ?
        LEFT JOIN unit_translation english
          ON english.unit_id = m.id AND english.lang = 'en'
        WHERE m.domain = 'temperature'
        ORDER BY CASE m.code
            WHEN 'fahrenheit' THEN 0
            WHEN 'kelvin' THEN 1
            WHEN 'celsius' THEN 2
            ELSE 3
        END, m.code
        """;

    private LocalizedUnitDAO() {
    }

    public static List<LocalizedUnit> findTemperatureUnits(Locale locale) throws SQLException {
        String language = locale == null ? "en" : locale.getLanguage();
        if (!SUPPORTED_LANGUAGES.contains(language)) {
            language = "en";
        }

        List<LocalizedUnit> units = new ArrayList<>();
        try (Connection connection = DBConnection.connect();
             PreparedStatement statement = connection.prepareStatement(FIND_TEMPERATURE_UNITS)) {
            statement.setString(1, language);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String name = result.getString("name");
                    String description = result.getString("description");
                    if (name == null || description == null) {
                        throw new SQLException(
                                "Missing localized and English translation for temperature unit "
                                        + result.getString("code")
                        );
                    }
                    units.add(new LocalizedUnit(
                            result.getInt("id"),
                            result.getString("code"),
                            result.getString("symbol"),
                            name,
                            description
                    ));
                }
            }
        }

        if (units.isEmpty()) {
            throw new SQLException("No temperature units are registered in the database");
        }
        return List.copyOf(units);
    }

    public record LocalizedUnit(
            int measurementUnitId,
            String code,
            String symbol,
            String name,
            String description
    ) {
        @Override
        public String toString() {
            return name;
        }
    }
}
