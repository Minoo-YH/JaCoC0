import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

public class TemperatureController {

    @FXML
    private ComboBox<String> languageBox;

    @FXML
    private TextField inputField;

    @FXML
    private ComboBox<String> unitBox;

    @FXML
    private Label resultLabel;

    private ResourceBundle messages;

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

        unitBox.getItems().addAll(
                "Fahrenheit",
                "Kelvin",
                "Celsius"
        );

        unitBox.setValue("Fahrenheit");
    }

    @FXML
    private void convertTemperature() {
        try {
            double celsius = Double.parseDouble(inputField.getText());

            TemperatureConverter converter = new TemperatureConverter();

            String selectedUnit = unitBox.getValue();
            double result;
            String resultUnit;

            switch (selectedUnit) {
                case "Kelvin":
                    result = converter.celsiusToKelvin(celsius);
                    resultUnit = "Kelvin";
                    break;

                case "Celsius":
                    result = celsius;
                    resultUnit = messages.getString("celsius");
                    break;

                default:
                    result = converter.celsiusToFahrenheit(celsius);
                    resultUnit = messages.getString("fahrenheit");
                    break;
            }

            TempRecordDAO.saveRecord(celsius, result);

            resultLabel.setText(
                    celsius + " " + messages.getString("celsius")
                            + " = "
                            + result + " "
                            + resultUnit
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

            Stage stage =
                    (Stage) languageBox.getScene().getWindow();

            stage.setTitle(bundle.getString("title"));
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
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
