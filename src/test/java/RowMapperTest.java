import ch.qos.logback.classic.Logger;
import domain.User;
import logger.TestLogAppender;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import raf.thesis.mapper.DefaultMapperImplementation;
import raf.thesis.mapper.RowMapper;
import raf.thesis.metadata.ColumnMetadata;
import raf.thesis.metadata.EntityMetadata;
import raf.thesis.metadata.storage.MetadataStorage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RowMapperTest {
    private static Connection conn;

    @BeforeAll
    static void setupDatabase() throws SQLException {
        conn = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(255), email VARCHAR(255), age INT);");
        }
    }

    @BeforeAll
    static void insertMetaData() throws NoSuchFieldException {
        Map<String, ColumnMetadata> map = new HashMap<>();
        map.put("id", new ColumnMetadata("id", User.class.getDeclaredField("id")));
        map.put("name", new ColumnMetadata("name", User.class.getDeclaredField("name")));
        map.put("email", new ColumnMetadata("email", User.class.getDeclaredField("email")));
        map.put("age", new ColumnMetadata("age", User.class.getDeclaredField("age")));
        MetadataStorage.register(new EntityMetadata("users", User.class, List.of(User.class.getDeclaredField("id")), map, null, List.of(false)));
    }

    @AfterAll
    static void closeDatabase() throws SQLException {
        if (conn != null) {
            conn.close();
        }
    }

    @BeforeEach
    void insertTestData() throws SQLException {
        // Clear and re-insert known data before EACH test to ensure isolation
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM users"); // Clear previous data
            stmt.execute("INSERT INTO users (id, name, email, age) VALUES (1, 'Alice', 'alice@example.com', 21)");
            stmt.execute("INSERT INTO users (id, name, email, age) VALUES (2, 'Bob', 'bob@example.com', 25)");
        }
    }

    @Test
    void testMapperMapsSingleRowCorrectly() throws SQLException {

        RowMapper rowMapper = new DefaultMapperImplementation();
        String sql = "SELECT id, name, email, age FROM users WHERE id = 1";

        try (Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {

            // Check if a row was returned
            assertTrue(rs.next(), "ResultSet should have a row.");

            // Use your mapper to convert the ResultSet row to an object
            User user = rowMapper.map(rs, User.class);

            // Assert that the mapped object fields are correct
            assertEquals(1, user.getId());
            assertEquals("Alice", user.getName());
            assertEquals("alice@example.com", user.getEmail());
            assertEquals(21, user.getAge());

            // Ensure no more rows exist
            assertFalse(rs.next(), "ResultSet should only have one row.");
        }
    }

    @Test
    void testMapperHandlesPartialObjectMapping() throws SQLException {
        RowMapper rowMapper = new DefaultMapperImplementation();
        String sql = "SELECT id, name FROM users WHERE id = 2";

        try (Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {

            // Check if a row was returned
            assertTrue(rs.next(), "ResultSet should have a row.");

            // Use your mapper to convert the ResultSet row to an object
            User user = rowMapper.map(rs, User.class);

            // Assert that the mapped object fields are correct
            assertEquals(2, user.getId());
            assertEquals("Bob", user.getName());
            assertNull(user.getEmail());
            assertEquals(0, user.getAge());
            // Ensure no more rows exist
            assertFalse(rs.next(), "ResultSet should only have one row.");
        }
    }

    @Test
    void testMapperWarnsForSkippingColumns() throws SQLException {
        Logger logger = (Logger) LoggerFactory.getLogger(DefaultMapperImplementation.class);
        TestLogAppender appender = new TestLogAppender();
        appender.start();
        logger.addAppender(appender);

        RowMapper rowMapper = new DefaultMapperImplementation();
        String sql = "SELECT id, name, email AS mail FROM users WHERE id = 2";

        try (Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {

            // Check if a row was returned
            assertTrue(rs.next(), "ResultSet should have a row.");

            // Use your mapper to convert the ResultSet row to an object
            User user = rowMapper.map(rs, User.class);

            // Assert that the mapped object fields are correct
            assertEquals(2, user.getId());
            assertEquals("Bob", user.getName());
            assertNull(user.getEmail());
            assertEquals(0, user.getAge());
            // Ensure no more rows exist
            assertFalse(rs.next(), "ResultSet should only have one row.");

            boolean found = appender.getEvents().stream()
                    .anyMatch(e -> e.getFormattedMessage().contains("Column 'mail' does not exist"));

            assertTrue(found, "Expected warning not found");
        }
    }

    @Test
    void testMapperMapsMultipleRowsCorrectly() throws SQLException {
        RowMapper rowMapper = new DefaultMapperImplementation();
        String sql = "SELECT id, name, email, age FROM users";

        try (Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {

            // Use your mapper to convert the ResultSet row to an object
            List<User> users = rowMapper.mapList(rs, User.class);

            // Assert that the mapped object fields are correct
            assertEquals(1, users.getFirst().getId());
            assertEquals("Alice", users.getFirst().getName());
            assertEquals("alice@example.com", users.getFirst().getEmail());
            assertEquals(21, users.getFirst().getAge());

            assertEquals(2, users.get(1).getId());
            assertEquals("Bob", users.get(1).getName());
            assertEquals("bob@example.com", users.get(1).getEmail());
            assertEquals(25, users.get(1).getAge());

            // Ensure no more rows exist
            assertFalse(rs.next(), "ResultSet is not entirely mapped.");
        }
    }
}
