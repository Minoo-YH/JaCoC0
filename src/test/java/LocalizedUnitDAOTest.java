import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalizedUnitDAOTest {

    private static final String DATABASE_PATH_PROPERTY = "temperature.db.path";

    @TempDir
    Path temporaryDirectory;

    private String previousDatabasePath;

    @BeforeEach
    void initializeIsolatedDatabase() {
        previousDatabasePath = System.getProperty(DATABASE_PATH_PROPERTY);
        System.setProperty(
                DATABASE_PATH_PROPERTY,
                temporaryDirectory.resolve("localized-units-test.db").toString()
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
    void returnsLocalizedNamesAndDescriptionsForAllSupportedLanguages() throws Exception {
        Map<String, String> expectedCelsiusNames = Map.of(
                "en", "Celsius",
                "fi", "Celsius-aste",
                "sv", "Celsiusgrad",
                "ja", "セルシウス度",
                "ar", "درجة مئوية"
        );

        for (Map.Entry<String, String> expected : expectedCelsiusNames.entrySet()) {
            List<LocalizedUnitDAO.LocalizedUnit> units =
                    LocalizedUnitDAO.findTemperatureUnits(Locale.forLanguageTag(expected.getKey()));
            Map<String, LocalizedUnitDAO.LocalizedUnit> byCode = units.stream()
                    .collect(Collectors.toMap(
                            LocalizedUnitDAO.LocalizedUnit::code,
                            unit -> unit
                    ));

            assertEquals(List.of("fahrenheit", "kelvin", "celsius"),
                    units.stream().map(LocalizedUnitDAO.LocalizedUnit::code).toList());
            assertEquals(expected.getValue(), byCode.get("celsius").name());
            assertNotNull(byCode.get("celsius").description());
            assertFalse(byCode.get("celsius").description().isBlank());
            assertEquals(1, byCode.get("celsius").measurementUnitId());
        }
    }

    @Test
    void fallsBackToEnglishWhenRequestedTranslationIsMissing() throws Exception {
        int celsiusId;
        try (Connection connection = DBConnection.connect();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id FROM measurement_unit
                     WHERE domain = 'temperature' AND code = 'celsius'
                     """);
             ResultSet result = statement.executeQuery()) {
            assertTrue(result.next());
            celsiusId = result.getInt(1);
        }

        try (Connection connection = DBConnection.connect();
             PreparedStatement statement = connection.prepareStatement("""
                     DELETE FROM unit_translation
                     WHERE unit_id = ? AND lang = 'fi'
                     """)) {
            statement.setInt(1, celsiusId);
            assertEquals(1, statement.executeUpdate());
        }

        LocalizedUnitDAO.LocalizedUnit celsius = LocalizedUnitDAO
                .findTemperatureUnits(Locale.forLanguageTag("fi"))
                .stream()
                .filter(unit -> unit.code().equals("celsius"))
                .findFirst()
                .orElseThrow();

        assertEquals("Celsius", celsius.name());
        assertEquals(
                "Temperature scale with water freezing at 0 °C and boiling at 100 °C at standard pressure.",
                celsius.description()
        );
        assertEquals(celsiusId, celsius.measurementUnitId());
    }

    @Test
    void unsupportedLocalesUseEnglish() throws Exception {
        LocalizedUnitDAO.LocalizedUnit celsius = LocalizedUnitDAO
                .findTemperatureUnits(Locale.forLanguageTag("de"))
                .stream()
                .filter(unit -> unit.code().equals("celsius"))
                .findFirst()
                .orElseThrow();

        assertEquals("Celsius", celsius.name());
    }

    @Test
    void conversionButtonLabelUsesSelectedLocalizedUnitForEachLanguage() throws Exception {
        Map<String, String> expectedCelsiusLabels = Map.of(
                "en", "Convert to Celsius",
                "fi", "Muunna yksikköön Celsius-aste",
                "sv", "Konvertera till Celsiusgrad",
                "ja", "セルシウス度に変換",
                "ar", "حوّل إلى درجة مئوية"
        );

        for (Map.Entry<String, String> expected : expectedCelsiusLabels.entrySet()) {
            Locale locale = Locale.forLanguageTag(switch (expected.getKey()) {
                case "en" -> "en-US";
                case "fi" -> "fi-FI";
                case "sv" -> "sv-SE";
                case "ja" -> "ja-JP";
                case "ar" -> "ar-SA";
                default -> throw new IllegalStateException("Unexpected language");
            });
            ResourceBundle messages = ResourceBundle.getBundle("MessagesBundle", locale);
            List<LocalizedUnitDAO.LocalizedUnit> units =
                    LocalizedUnitDAO.findTemperatureUnits(locale);

            LocalizedUnitDAO.LocalizedUnit celsius = units.stream()
                    .filter(unit -> unit.code().equals("celsius"))
                    .findFirst()
                    .orElseThrow();
            LocalizedUnitDAO.LocalizedUnit kelvin = units.stream()
                    .filter(unit -> unit.code().equals("kelvin"))
                    .findFirst()
                    .orElseThrow();

            assertEquals(expected.getValue(),
                    TemperatureController.conversionButtonLabel(messages, celsius));
            assertFalse(TemperatureController.conversionButtonLabel(messages, kelvin)
                    .equals(expected.getValue()));
        }
    }

    @Test
    void conversionUsesSelectedUnitCodeAndExistingFormulas() {
        assertEquals(25.0, TemperatureController.convertCelsius(25.0, "celsius"), 0.0);
        assertEquals(77.0, TemperatureController.convertCelsius(25.0, "fahrenheit"), 0.001);
        assertEquals(298.15, TemperatureController.convertCelsius(25.0, "kelvin"), 0.001);
    }
}
