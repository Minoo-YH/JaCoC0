import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.ResourceBundle;

public class TemperatureApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        DBConnection.initializeDatabase();

        Locale locale = new Locale("en", "US");
        Locale.setDefault(locale);

        ResourceBundle bundle =
                ResourceBundle.getBundle("MessagesBundle", locale);

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/temperature-view.fxml"),
                bundle
        );

        Parent root = loader.load();
        root.setNodeOrientation(
                locale.getLanguage().equals("ar")
                        ? NodeOrientation.RIGHT_TO_LEFT
                        : NodeOrientation.LEFT_TO_RIGHT
        );

        Scene scene = new Scene(root);

        stage.setTitle(bundle.getString("title"));
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
