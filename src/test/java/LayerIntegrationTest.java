import layering.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import raf.thesis.ConnectionSupplier;
import raf.thesis.mapper.DefaultMapperImplementation;
import raf.thesis.mapper.RowMapper;
import raf.thesis.metadata.scan.MetadataScanner;
import raf.thesis.query.Join;
import raf.thesis.query.QueryBuilder;
import raf.thesis.query.dialect.ANSISQLDialect;
import raf.thesis.query.dialect.Dialect;
import raf.thesis.query.dialect.MSSQLServerDialect;
import raf.thesis.query.dialect.MariaDBDialect;
import raf.thesis.query.exceptions.ConnectionUnavailableException;
import util.H2HRProvider;
import util.multidb.MultiDBTest;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static raf.thesis.query.ConditionBuilder.*;

@SuppressWarnings("JUnitMalformedDeclaration")
@MultiDBTest
public class LayerIntegrationTest {
    private static final RowMapper rowMapper = new DefaultMapperImplementation();

    private Dialect getDialect(ConnectionSupplier connectionSupplier) {
        try(Connection conn = connectionSupplier.getConnection()){
            String driverName = conn.getMetaData().getDriverName();
            if(driverName.toLowerCase().contains("mariadb"))
                return new MariaDBDialect();
            if(driverName.toLowerCase().contains("mysql"))
                return new MariaDBDialect();
            if(driverName.toLowerCase().contains("microsoft"))
                return new MSSQLServerDialect();
            else
                return new ANSISQLDialect();
        } catch (SQLException e) {
            throw new ConnectionUnavailableException("Given connection supplier doesn't supply connections!");
        }
    }

    @BeforeAll
    static void fillMetadata() throws SQLException, NoSuchFieldException {
        MetadataScanner ms = new MetadataScanner();
        ms.discoverMetadata("layering");
    }

    @Test
    void testSimpleJoinIntegrationTest(ConnectionSupplier cp) throws SQLException {
        String query = QueryBuilder.select(Department.class).join("employees").build(getDialect(cp));
        System.out.println(query);
        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
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
    void testLeftJoinIntegrationTest(ConnectionSupplier cp) throws SQLException {
        String query = QueryBuilder.select(Department.class).join("employees", Join.LEFT).build(getDialect(cp));
        System.out.println(query);
        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
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
    void testManyToManyJoinIntegrationTest(ConnectionSupplier cp) throws SQLException {
        String query = QueryBuilder.select(Project.class)
                .join("employees")
                .join("employees.projects")
                .orderBy(asc(field("project_id")), asc(field("employees.employee_id")))
                .build(getDialect(cp));
        System.out.println(query);
        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

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
    void testRecursiveMultiJoinIntegrationTest(ConnectionSupplier cp) throws SQLException {
        String query = QueryBuilder.select(Employee.class).join("department").join("manager").join("department.employees").build(getDialect(cp));
        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            List<Employee> employees = rowMapper.mapWithRelations(rs, Employee.class);
            assertFalse(employees.isEmpty());
        }
    }

    @Test
    void testLikeIntegration(ConnectionSupplier cp) throws SQLException {
        String query = QueryBuilder.select(Employee.class).where(field("last_name").like("K%")).build(getDialect(cp));
        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            List<Employee> employees = rowMapper.mapWithRelations(rs, Employee.class);
            Employee Steven = new Employee(100, "Steven", "King", LocalDate.of(2003, 6, 17));
            Employee Neena = new Employee(101, "Neena", "Kochhar", LocalDate.of(2005, 9, 21));
            assertThat(employees).usingRecursiveComparison().isEqualTo(List.of(Steven, Neena));
        }
    }

    @Test
    void testInIntegration(ConnectionSupplier cp) throws SQLException {
        String query = QueryBuilder.select(Employee.class).where(field("first_name").in(tuple(lit("Steven"), lit("Neena"), lit("Lex")))).build(getDialect(cp));
        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            List<Employee> employees = rowMapper.mapWithRelations(rs, Employee.class);
            Employee Steven = new Employee(100, "Steven", "King", LocalDate.of(2003, 6, 17));
            Employee Neena = new Employee(101, "Neena", "Kochhar", LocalDate.of(2005, 9, 21));
            Employee Lex = new Employee(102, "Lex", "De Haan", LocalDate.of(2001, 1, 13));
            assertThat(employees).usingRecursiveComparison().isEqualTo(List.of(Steven, Neena, Lex));
        }
    }

    @Test
    void testDateLiteralIntegration(ConnectionSupplier cp) throws SQLException {
        String query = QueryBuilder.select(Employee.class).where(field("hire_date").lt(lit(LocalDate.of(2005, 12, 4)))).build(getDialect(cp));
        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            List<Employee> employees = rowMapper.mapWithRelations(rs, Employee.class);
            Employee Steven = new Employee(100, "Steven", "King", LocalDate.of(2003, 6, 17));
            Employee Neena = new Employee(101, "Neena", "Kochhar", LocalDate.of(2005, 9, 21));
            Employee Lex = new Employee(102, "Lex", "De Haan", LocalDate.of(2001, 1, 13));
            assertThat(employees).usingRecursiveComparison().isEqualTo(List.of(Steven, Neena, Lex));
        }

        query = QueryBuilder.select(Employee.class).where(field("hire_date").in(
                        tuple(
                                lit(LocalDate.of(2003, 6, 17)),
                                lit(LocalDate.of(2005, 9, 21)),
                                lit(LocalDate.of(2001, 1, 13)))))
                .build(getDialect(cp));
        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            List<Employee> employees = rowMapper.mapWithRelations(rs, Employee.class);
            Employee Steven = new Employee(100, "Steven", "King", LocalDate.of(2003, 6, 17));
            Employee Neena = new Employee(101, "Neena", "Kochhar", LocalDate.of(2005, 9, 21));
            Employee Lex = new Employee(102, "Lex", "De Haan", LocalDate.of(2001, 1, 13));
            assertThat(employees).usingRecursiveComparison().isEqualTo(List.of(Steven, Neena, Lex));
        }
    }

    @Test
    void testTimesAndEnumIntegration(ConnectionSupplier cp)throws SQLException{
        String query = QueryBuilder.select(TypeTest.class).build(getDialect(cp));
        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            List<TypeTest> tests = rowMapper.mapWithRelations(rs, TypeTest.class);
            TypeTest first = new TypeTest(1, Status.NEW, LocalDateTime.of(2024, Month.JANUARY, 10, 12, 34, 56, 123 * 1_000_000), LocalTime.of(12, 34, 56));
            TypeTest second = new TypeTest(2, Status.DONE, LocalDateTime.of(2024, Month.JUNE, 1, 8, 0, 0), LocalTime.of(8, 0, 0));
            assertThat(tests).usingRecursiveComparison().isEqualTo(List.of(first, second));
        }
    }

    @Test
    void testLogicalOpIntegration(ConnectionSupplier cp) throws SQLException {
        String query = QueryBuilder.select(Employee.class).where(
                        and(
                                not(field("first_name").eq(lit("Alexander"))),
                                not(field("first_name").eq(lit("Lex"))),
                                not(field("first_name").eq(lit("Bruce")))
                        ))
                .build(getDialect(cp));
        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            List<Employee> employees = rowMapper.mapWithRelations(rs, Employee.class);
            Employee Steven = new Employee(100, "Steven", "King", LocalDate.of(2003, 6, 17));
            Employee Neena = new Employee(101, "Neena", "Kochhar", LocalDate.of(2005, 9, 21));
            assertThat(employees).usingRecursiveComparison().isEqualTo(List.of(Steven, Neena));
        }
        query = QueryBuilder.select(Employee.class).where(
                        or(
                                field("first_name").eq(lit("Steven")),
                                field("first_name").eq(lit("Neena")),
                                field("first_name").eq(lit("Lex"))
                        ))
                .build(getDialect(cp));
        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            List<Employee> employees = rowMapper.mapWithRelations(rs, Employee.class);
            Employee Steven = new Employee(100, "Steven", "King", LocalDate.of(2003, 6, 17));
            Employee Neena = new Employee(101, "Neena", "Kochhar", LocalDate.of(2005, 9, 21));
            Employee Lex = new Employee(102, "Lex", "De Haan", LocalDate.of(2001, 1, 13));
            assertThat(employees).usingRecursiveComparison().isEqualTo(List.of(Steven, Neena, Lex));
        }
    }

    @Test
    void testIsNullIntegration(ConnectionSupplier cp) throws SQLException {
        String query = QueryBuilder.select(Department.class).where(field("manager_id").isNull()).build(getDialect(cp));
        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            List<Department> departments = rowMapper.mapWithRelations(rs, Department.class);
            Department Administration = new Department(10, "Administration");
            Department Marketing = new Department(20, "Marketing");
            Department Purchasing = new Department(30, "Purchasing");
            Department HR = new Department(40, "Human Resources");
            assertThat(departments).usingRecursiveComparison().isEqualTo(List.of(Administration, Marketing, Purchasing, HR));
        }

        query = QueryBuilder.select(Department.class).where(not(field("manager_id").isNull())).build(getDialect(cp));
        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            List<Department> departments = rowMapper.mapWithRelations(rs, Department.class);
            assertTrue(departments.isEmpty());
        }
    }

    @Test
    void testSimpleSubqueryIntegration(ConnectionSupplier cp) throws SQLException {
        String query = QueryBuilder.select(Department.class).where(
                field("department_id").eq(
                        QueryBuilder.subQuery(Department.class, max(field("department_id"))))
        ).build(getDialect(cp));
        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            List<Department> departments = rowMapper.mapWithRelations(rs, Department.class);
            Department HR = new Department(40, "Human Resources");
            assertThat(departments).usingRecursiveComparison().isEqualTo(List.of(HR));
        }
    }

    @Test
    void testGroupByAndHavingClauseInSubqueryIntegration(ConnectionSupplier cp) throws SQLException {
        String query = QueryBuilder.select(Department.class).where(field("department_id").in(
                        QueryBuilder.subQuery(Department.class, field("department_id"))
                                .join("employees")
                                .groupBy(field("department_id"))
                                .having(max(field("employees.employee_id")).gt(lit(102)))
                ))
                .orderBy(asc(field("department_id")))
                .build(getDialect(cp));

        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            List<Department> departments = rowMapper.mapWithRelations(rs, Department.class);
            Department Marketing = new Department(20, "Marketing");
            Department Purchasing = new Department(30, "Purchasing");
            assertThat(departments).usingRecursiveComparison().isEqualTo(List.of(Marketing, Purchasing));
        }
    }

    @Test
    void testPTOIntegration(ConnectionSupplier cp) throws SQLException {
        String query = QueryBuilder.select(
                        Department.class,
                        aliasedColumn(field("department_id"), "department_id"),
                        aliasedColumn(max(field("employees.employee_id")), "maxEmployeeId"))
                .join("employees")
                .groupBy(field("department_id"))
                .having(max(field("employees.employee_id")).gt(lit(102)))
                .orderBy(desc(field("department_id")))
                .build(getDialect(cp));
        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            List<DepartmentsWithMaxEmployeeIdPDO> d = rowMapper.mapList(rs, DepartmentsWithMaxEmployeeIdPDO.class);
            List<DepartmentsWithMaxEmployeeIdPDO> expected = new ArrayList<>();
            expected.add(new DepartmentsWithMaxEmployeeIdPDO(30, 103));
            expected.add(new DepartmentsWithMaxEmployeeIdPDO(20, 104));
            assertThat(d).usingRecursiveComparison().isEqualTo(expected);
        }
    }

    @Test
    void testOffset(ConnectionSupplier cp) throws SQLException {
        String query = QueryBuilder.select(Employee.class).orderBy(asc(field("employee_id"))).offset(2).build(getDialect(cp));
        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            List<Employee> employees = rowMapper.mapWithRelations(rs, Employee.class);
            Employee Lex = new Employee(102, "Lex", "De Haan", LocalDate.of(2001, 1, 13));
            Employee Alexander = new Employee(103, "Alexander", "Hunold", LocalDate.of(2006, 1, 3));
            Employee Bruce = new Employee(104, "Bruce", "Ernst", LocalDate.of(2007, 5, 21));
            List<Employee> expected = List.of(Lex, Alexander, Bruce);
            assertThat(employees).usingRecursiveComparison().isEqualTo(expected);
        }
    }

    @Test
    void testLimit(ConnectionSupplier cp) throws SQLException {
        String query = QueryBuilder.select(Employee.class).orderBy(asc(field("employee_id"))).limit(2).build(getDialect(cp));
        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            List<Employee> employees = rowMapper.mapWithRelations(rs, Employee.class);
            Employee Steven = new Employee(100, "Steven", "King", LocalDate.of(2003, 6, 17));
            Employee Neena = new Employee(101, "Neena", "Kochhar", LocalDate.of(2005, 9, 21));
            List<Employee> expected = List.of(Steven, Neena);
            assertThat(employees).usingRecursiveComparison().isEqualTo(expected);
        }
    }

    @Test
    void testLimitAndOffset(ConnectionSupplier cp) throws SQLException {
        String query = QueryBuilder.select(Employee.class).orderBy(asc(field("employee_id"))).limit(2).offset(2).build(getDialect(cp));
        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            List<Employee> employees = rowMapper.mapWithRelations(rs, Employee.class);
            Employee Lex = new Employee(102, "Lex", "De Haan", LocalDate.of(2001, 1, 13));
            Employee Alexander = new Employee(103, "Alexander", "Hunold", LocalDate.of(2006, 1, 3));
            List<Employee> expected = List.of(Lex, Alexander);
            assertThat(employees).usingRecursiveComparison().isEqualTo(expected);
        }
    }
}
