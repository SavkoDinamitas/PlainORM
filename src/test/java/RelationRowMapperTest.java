import domain.User;
import domain.hr.Department;
import domain.hr.Employee;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import raf.thesis.mapper.DefaultMapperImplementation;
import raf.thesis.mapper.RowMapper;
import util.HrScheme;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RelationRowMapperTest {
    private static Connection conn;
    private static final RowMapper rowMapper = new DefaultMapperImplementation();

    @BeforeAll
    static void setupDatabase() throws SQLException, NoSuchFieldException {
        conn = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
        try (Statement stmt = conn.createStatement()) {
            //noinspection SqlSourceToSinkFlow
            stmt.execute(HrScheme.SCRIPT);
        }
        //fill metadata
        HrScheme.fillMetadataManually();
    }

    @AfterAll
    static void closeDatabase() throws SQLException {
        if (conn != null) {
            conn.close();
        }
    }

    @Test
    void testSingleJoinRelationRowMapper() throws SQLException {
        String sql = HrScheme.TESTJOIN1;

        try (Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {

            //map result set of single join query
            List<Department> departments = rowMapper.mapWithRelations(rs, Department.class);
            // Check if a row was returned
            assertFalse(departments.isEmpty());
            //check department fields
            Department department = departments.get(0);
            assertEquals(30, department.getDepartment_id());
            assertEquals("Purchasing", department.getDepartment_name());
            List<Employee> employees = department.getEmployees();
            assertFalse(employees.isEmpty());
            //check Lex De Haan employee
            assertEquals(102, employees.get(0).getEmployee_id());
            assertEquals("Lex", employees.get(0).getFirst_name());
            assertEquals("De Haan", employees.get(0).getLast_name());
            assertEquals(LocalDate.of(2001, 1, 13), employees.get(0).getHire_date());
            //check Alexander Hunold employee
            assertEquals(103, employees.get(1).getEmployee_id());
            assertEquals("Alexander", employees.get(1).getFirst_name());
            assertEquals("Hunold", employees.get(1).getLast_name());
            assertEquals(LocalDate.of(2006, 1, 3), employees.get(1).getHire_date());
        }
    }
}
