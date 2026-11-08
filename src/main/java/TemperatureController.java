import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class TemperatureController {

    @FXML
    private ComboBox<String> languageBox;

    @FXML
    private TextField inputField;

    @FXML
    private ComboBox<LocalizedUnitDAO.LocalizedUnit> unitBox;

    @FXML
    private Label resultLabel;

    @FXML
    private Button convertButton;

    private ResourceBundle messages;

    private List<LocalizedUnitDAO.LocalizedUnit> temperatureUnits;

    @FXML
    public void initialize() {
        messages = ResourceBundle.getBundle(
                "MessagesBundle",
                Locale.getDefault()
        );

        languageBox.getItems().addAll(
                "English",
                "Finnish",
                "Swedish",
                "Japanese",
                "Arabic"
        );

        languageBox.setValue(getLanguageName(Locale.getDefault()));

        loadTemperatureUnits(Locale.getDefault());
        unitBox.valueProperty().addListener(
                (observable, oldUnit, newUnit) -> updateConversionButtonLabel()
        );
        updateConversionButtonLabel();
    }

    @FXML
    private void convertTemperature() {
        try {
            double celsius = Double.parseDouble(inputField.getText());

            LocalizedUnitDAO.LocalizedUnit selectedUnit = unitBox.getValue();
            if (selectedUnit == null) {
                selectedUnit = findUnitByCode("fahrenheit");
            }
            double result = convertCelsius(celsius, selectedUnit.code());

            TempRecordDAO.saveRecord(celsius, result);

            String celsiusName = findUnitByCode("celsius").name();
            resultLabel.setText(
                    celsius + " " + celsiusName
                            + " = "
                            + result + " "
                            + selectedUnit.name()
            );

        } catch (NumberFormatException e) {
            resultLabel.setText(messages.getString("invalid"));
        }
    }

    @FXML
    private void changeLanguage() {
        if (languageBox.getValue() == null) {
            return;
        }

        Locale locale = switch (languageBox.getValue()) {
            case "Finnish" -> new Locale("fi", "FI");
            case "Swedish" -> new Locale("sv", "SE");
            case "Japanese" -> new Locale("ja", "JP");
            case "Arabic" -> new Locale("ar", "SA");
            default -> new Locale("en", "US");
        };

        Locale.setDefault(locale);

        try {
            ResourceBundle bundle =
                    ResourceBundle.getBundle("MessagesBundle", locale);

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/temperature-view.fxml"),
                    bundle
            );

            Parent root = loader.load();
            root.setNodeOrientation(
                    locale.getLanguage().equals("ar")
                            ? javafx.geometry.NodeOrientation.RIGHT_TO_LEFT
                            : javafx.geometry.NodeOrientation.LEFT_TO_RIGHT
            );

            Stage stage =
                    (Stage) languageBox.getScene().getWindow();

            stage.setTitle(bundle.getString("title"));
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadTemperatureUnits(Locale locale) {
        try {
            temperatureUnits = LocalizedUnitDAO.findTemperatureUnits(locale);
            unitBox.getItems().setAll(temperatureUnits);
            unitBox.setValue(findUnitByCode("fahrenheit"));
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Could not load localized temperature units for " + locale, e
            );
        }
    }

    @FXML
    private void updateConversionButtonLabel() {
        LocalizedUnitDAO.LocalizedUnit selectedUnit = unitBox.getValue();
        if (selectedUnit != null) {
            convertButton.setText(conversionButtonLabel(messages, selectedUnit));
        }
    }

    static String conversionButtonLabel(
            ResourceBundle messages,
            LocalizedUnitDAO.LocalizedUnit selectedUnit
    ) {
        return java.text.MessageFormat.format(
                messages.getString("convertTo"),
                selectedUnit.name()
        );
    }

    static double convertCelsius(double celsius, String unitCode) {
        TemperatureConverter converter = new TemperatureConverter();
        return switch (unitCode) {
            case "celsius" -> celsius;
            case "fahrenheit" -> converter.celsiusToFahrenheit(celsius);
            case "kelvin" -> converter.celsiusToKelvin(celsius);
            default -> throw new IllegalArgumentException(
                    "Unsupported temperature unit: " + unitCode
            );
        };
    }

    private LocalizedUnitDAO.LocalizedUnit findUnitByCode(String code) {
        return temperatureUnits.stream()
                .filter(unit -> unit.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Temperature unit is not available: " + code
                ));
    }

    private String getLanguageName(Locale locale) {
        return switch (locale.getLanguage()) {
            case "fi" -> "Finnish";
            case "sv" -> "Swedish";
            case "ja" -> "Japanese";
            case "ar" -> "Arabic";
            default -> "English";
        };
    }
}
