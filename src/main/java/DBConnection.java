import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {

    private static final String URL = "jdbc:sqlite:temperature.db";

    public static Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(URL);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }

        return connection;
    }

    public static void initializeDatabase() {
        String createUnits = """
            CREATE TABLE IF NOT EXISTS temperature_unit (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                symbol TEXT NOT NULL
            )
            """;

        String createRecords = """
            CREATE TABLE IF NOT EXISTS temperature_record (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                input_value REAL NOT NULL,
                result_value REAL NOT NULL,
                unit_id INTEGER NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (unit_id) REFERENCES temperature_unit(id)
            )
            """;

        String insertUnits = """
            INSERT OR IGNORE INTO temperature_unit (name, symbol)
            VALUES
                ('Celsius', 'C'),
                ('Fahrenheit', 'F'),
                ('Kelvin', 'K')
            """;

        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {

            statement.execute(createUnits);
            statement.execute(createRecords);
            statement.executeUpdate(insertUnits);

            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        initializeDatabase();
    }
}
