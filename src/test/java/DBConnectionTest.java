import org.junit.jupiter.api.Test;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

public class DBConnectionTest {

    @Test
    void testDatabaseConnection() throws Exception {
        try (Connection connection = DBConnection.connect()) {
            assertNotNull(connection);
            assertFalse(connection.isClosed());
        }
    }

    @Test
    void testInitializeDatabase() {
        assertDoesNotThrow(DBConnection::initializeDatabase);
    }
}
