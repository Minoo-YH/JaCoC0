import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {

    private static final String DATABASE_PATH_PROPERTY = "temperature.db.path";
    private static final String DEFAULT_DATABASE_PATH = "temperature.db";
    private static final String INITIALIZATION_SCRIPT = "/init.sql";

    public static Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + System.getProperty(
                        DATABASE_PATH_PROPERTY,
                        DEFAULT_DATABASE_PATH
                )
        );

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                e.addSuppressed(closeException);
            }
            throw e;
        }

        return connection;
    }

    public static void initializeDatabase() {
        try {
            String script;
            try (InputStream input = DBConnection.class.getResourceAsStream(INITIALIZATION_SCRIPT)) {
                if (input == null) {
                    throw new SQLException("Database initialization resource not found: " + INITIALIZATION_SCRIPT);
                }
                script = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new SQLException("Could not read database initialization resource: " + INITIALIZATION_SCRIPT, e);
            }

            try (Connection connection = connect();
                 Statement statement = connection.createStatement()) {
                connection.setAutoCommit(false);
                try {
                    for (String sql : script.split(";")) {
                        if (!sql.isBlank()) {
                            statement.execute(sql);
                        }
                    }
                    connection.commit();
                } catch (SQLException | RuntimeException e) {
                    try {
                        connection.rollback();
                    } catch (SQLException rollbackException) {
                        e.addSuppressed(rollbackException);
                    }
                    throw e;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize database using " + INITIALIZATION_SCRIPT, e);
        }
    }

    public static void main(String[] args) {
        initializeDatabase();
    }
}
