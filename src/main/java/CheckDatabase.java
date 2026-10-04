import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckDatabase {
    public static void main(String[] args) throws Exception {
        try (Connection connection = DBConnection.connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                 SELECT r.id, r.input_value, r.result_value, u.name
                 FROM temperature_record r
                 JOIN temperature_unit u ON r.unit_id = u.id
                 ORDER BY r.id
             """)) {

            while (result.next()) {
                System.out.println(
                    result.getInt("id") + " | " +
                    result.getDouble("input_value") + " | " +
                    result.getDouble("result_value") + " | " +
                    result.getString("name")
                );
            }
        }
    }
}
