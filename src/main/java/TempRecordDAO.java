import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TempRecordDAO {

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
            statement.executeUpdate();

            System.out.println("Conversion saved to database.");

        } catch (SQLException e) {
            System.err.println("Could not save record: " + e.getMessage());
        }
    }
}
