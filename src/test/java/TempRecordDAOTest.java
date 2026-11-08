import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class TempRecordDAOTest {

    private static final String DATABASE_PATH_PROPERTY = "temperature.db.path";

    @TempDir
    Path temporaryDirectory;

    private String previousDatabasePath;

    @BeforeEach
    void useIsolatedDatabase() {
        previousDatabasePath = System.getProperty(DATABASE_PATH_PROPERTY);
        System.setProperty(
                DATABASE_PATH_PROPERTY,
                temporaryDirectory.resolve("temperature-test.db").toString()
        );
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
    void testSaveRecord() throws Exception {

        DBConnection.initializeDatabase();

        TempRecordDAO.saveRecord(10.0, 50.0);

        try (Connection connection = DBConnection.connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT * FROM temperature_record " +
                     "WHERE input_value = 10.0 AND result_value = 50.0")) {

            assertTrue(result.next());
            assertEquals(10.0, result.getDouble("input_value"), 0.001);
            assertEquals(50.0, result.getDouble("result_value"), 0.001);
        }
    }
}
