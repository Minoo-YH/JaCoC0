import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class TemperatureApp extends Application {

    @Override
    public void start(Stage stage) {
        TextField input = new TextField();
        input.setPromptText("Enter Celsius");

        Button convertButton = new Button("Convert to Fahrenheit");
        Label resultLabel = new Label("Result:");

        convertButton.setOnAction(event -> {
            try {
                double celsius = Double.parseDouble(input.getText());
                TemperatureConverter converter = new TemperatureConverter();
                double fahrenheit = converter.celsiusToFahrenheit(celsius);
                TempRecordDAO.saveRecord(celsius, fahrenheit);

                resultLabel.setText(
                    celsius + " Celsius = " + fahrenheit + " Fahrenheit"
                );
            } catch (NumberFormatException e) {
                resultLabel.setText("Please enter a valid number.");
            }
        });

        VBox root = new VBox(10, input, convertButton, resultLabel);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 400, 200);

        stage.setTitle("Temperature Converter");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
