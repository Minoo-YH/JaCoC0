import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class TempRecordDAOTest {

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
