import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.ResourceBundle;

public class TemperatureApp extends Application {

    @Override
    public void start(Stage stage) {
        DBConnection.initializeDatabase();

        ChoiceDialog<String> languageDialog = new ChoiceDialog<>(
                "English",
                "English", "Finnish", "Swedish", "Japanese"
        );

        languageDialog.setTitle("Language");
        languageDialog.setHeaderText("Select your preferred language");

        String language = languageDialog.showAndWait().orElse("English");

        Locale locale;

        switch (language) {
            case "Finnish":
                locale = new Locale("fi", "FI");
                break;
            case "Swedish":
                locale = new Locale("sv", "SE");
                break;
            case "Japanese":
                locale = new Locale("ja", "JP");
                break;
            default:
                locale = new Locale("en", "US");
        }

        ResourceBundle messages =
                ResourceBundle.getBundle("MessagesBundle", locale);

        TextField input = new TextField();
        input.setPromptText(messages.getString("prompt"));

        Button convertButton =
                new Button(messages.getString("convert"));

        Label resultLabel =
                new Label(messages.getString("result"));

        convertButton.setOnAction(event -> {
            try {
                double celsius = Double.parseDouble(input.getText());

                TemperatureConverter converter =
                        new TemperatureConverter();

                double fahrenheit =
                        converter.celsiusToFahrenheit(celsius);

                TempRecordDAO.saveRecord(celsius, fahrenheit);

                resultLabel.setText(
                        celsius + " " + messages.getString("celsius")
                                + " = "
                                + fahrenheit + " "
                                + messages.getString("fahrenheit")
                );

            } catch (NumberFormatException e) {
                resultLabel.setText(messages.getString("invalid"));
            }
        });

        VBox root =
                new VBox(10, input, convertButton, resultLabel);

        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 400, 200);

        stage.setTitle(messages.getString("title"));
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}