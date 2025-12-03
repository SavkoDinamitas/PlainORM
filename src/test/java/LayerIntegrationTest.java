import layering.Employee;
import layering.Department;
import layering.Project;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import raf.thesis.mapper.DefaultMapperImplementation;
import raf.thesis.mapper.RowMapper;
import raf.thesis.metadata.scan.MetadataScanner;
import raf.thesis.query.Join;
import raf.thesis.query.QueryBuilder;
import util.HrScheme;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static raf.thesis.query.ConditionBuilder.*;

public class LayerIntegrationTest {
    private static Connection conn;
    private static final RowMapper rowMapper = new DefaultMapperImplementation();
    @BeforeAll
    static void setupDatabase() throws SQLException, NoSuchFieldException {
        conn = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
        try (Statement stmt = conn.createStatement()) {
            //noinspection SqlSourceToSinkFlow
            stmt.execute(HrScheme.SCRIPT);
        }
        MetadataScanner ms = new MetadataScanner();
        ms.discoverMetadata("layering");
    }

    @AfterAll
    static void closeDatabase() throws SQLException {
        if (conn != null) {
            conn.close();
        }
    }

    @Test
    void testSimpleJoinIntegrationTest() throws SQLException {
        String query = QueryBuilder.select(Department.class).join("employees").build();
        System.out.println(query);
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            List<Department> departments = rowMapper.mapWithRelations(rs, Department.class);
            // Check if a row was returned
            assertFalse(departments.isEmpty());
            //build how objects should look like
            Employee Steven = new Employee(100, "Steven", "King", LocalDate.of(2003, 6, 17));
            Employee Neena = new Employee(101, "Neena", "Kochhar", LocalDate.of(2005, 9, 21));
            Employee Lex = new Employee(102, "Lex", "De Haan", LocalDate.of(2001, 1, 13));
            Employee Alexander = new Employee(103, "Alexander", "Hunold", LocalDate.of(2006, 1, 3));
            Employee Bruce = new Employee(104, "Bruce", "Ernst", LocalDate.of(2007, 5, 21));
            Department Administration = new Department(10, "Administration");
            Department Marketing = new Department(20, "Marketing");
            Department Purchasing = new Department(30, "Purchasing");
            Administration.setEmployees(List.of(Steven));
            Marketing.setEmployees(List.of(Neena, Bruce));
            Purchasing.setEmployees(List.of(Lex, Alexander));
            List<Department> expected = List.of(Administration, Marketing, Purchasing);
            //assertJ recursive comparison
            assertThat(departments).usingRecursiveComparison().isEqualTo(expected);
        }
    }

    @Test
    void testLeftJoinIntegrationTest() throws SQLException {
        String query = QueryBuilder.select(Department.class).join("employees", Join.LEFT).build();
        System.out.println(query);
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            List<Department> departments = rowMapper.mapWithRelations(rs, Department.class);
            // Check if a row was returned
            assertFalse(departments.isEmpty());
            //build how objects should look like
            Employee Steven = new Employee(100, "Steven", "King", LocalDate.of(2003, 6, 17));
            Employee Neena = new Employee(101, "Neena", "Kochhar", LocalDate.of(2005, 9, 21));
            Employee Lex = new Employee(102, "Lex", "De Haan", LocalDate.of(2001, 1, 13));
            Employee Alexander = new Employee(103, "Alexander", "Hunold", LocalDate.of(2006, 1, 3));
            Employee Bruce = new Employee(104, "Bruce", "Ernst", LocalDate.of(2007, 5, 21));
            Department Administration = new Department(10, "Administration");
            Department Marketing = new Department(20, "Marketing");
            Department Purchasing = new Department(30, "Purchasing");
            Department HR = new Department(40, "Human Resources");
            Administration.setEmployees(List.of(Steven));
            Marketing.setEmployees(List.of(Neena, Bruce));
            Purchasing.setEmployees(List.of(Lex, Alexander));
            List<Department> expected = List.of(Administration, Marketing, Purchasing, HR);
            //assertJ recursive comparison
            assertThat(departments).usingRecursiveComparison().isEqualTo(expected);
        }
    }

    @Test
    void testManyToManyJoinIntegrationTest() throws SQLException {
        String query = QueryBuilder.select(Project.class)
                .join("employees")
                .join("employees.projects")
                .orderBy(asc(field("project_id")))
                .build();
        System.out.println(query);
        try (Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(query)) {

            //map result set of single join query
            List<Project> projects = rowMapper.mapWithRelations(rs, Project.class);
            // Check if a row was returned
            assertFalse(projects.isEmpty());
            //build how objects should look like
            Employee Steven = new Employee(100, "Steven", "King", LocalDate.of(2003, 6, 17));
            Employee Neena = new Employee(101, "Neena", "Kochhar", LocalDate.of(2005, 9, 21));
            Employee Lex = new Employee(102, "Lex", "De Haan", LocalDate.of(2001, 1, 13));
            Employee Alexander = new Employee(103, "Alexander", "Hunold", LocalDate.of(2006, 1, 3));
            Employee Bruce = new Employee(104, "Bruce", "Ernst", LocalDate.of(2007, 5, 21));

            Project Hr = new Project(1, "HR Onboarding System");
            Project Payrol = new Project(2, "Internal Payroll Platform");
            Project Website = new Project(3, "Corporate Website Redesign");
            Project Mobile = new Project(4, "Mobile Sales Dashboard");
            Project Cloud = new Project(5, "Cloud Migration Initiative");

            Steven.setProjects(List.of(Hr, Payrol, Cloud));
            Neena.setProjects(List.of(Hr));
            Lex.setProjects(List.of(Website, Cloud));
            Alexander.setProjects(List.of(Mobile));
            Bruce.setProjects(List.of(Payrol, Mobile));

            Hr.setEmployees(List.of(Steven, Neena));
            Payrol.setEmployees(List.of(Steven, Bruce));
            Website.setEmployees(List.of(Lex));
            Mobile.setEmployees(List.of(Alexander, Bruce));
            Cloud.setEmployees(List.of(Steven, Lex));
            List<Project> expected = List.of(Hr, Payrol, Website, Mobile, Cloud);
            //assertJ recursive comparison
            assertThat(projects).usingRecursiveComparison().isEqualTo(expected);
        }
    }

    @Test
    void testRecursiveMultiJoinIntegrationTest() throws SQLException {
        String query = QueryBuilder.select(Employee.class).join("department").join("manager").join("department.employees").build();
        try (Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(query)) {

            List<Employee> employees = rowMapper.mapWithRelations(rs, Employee.class);
            assertFalse(employees.isEmpty());
        }
    }
}
