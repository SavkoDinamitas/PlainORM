import layering.*;
import org.junit.jupiter.api.Test;
import raf.thesis.Session;
import raf.thesis.query.QueryBuilder;
import util.multidb.MultiDBTest;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static raf.thesis.query.ConditionBuilder.*;

@SuppressWarnings("JUnitMalformedDeclaration")
@MultiDBTest
public class SessionTest {

    //insert tests
    @Test
    void testSimpleInsert(Session session) throws SQLException {
        Employee me = new Employee(105, "Salko", "Dinamitas", LocalDate.of(2002, 10, 10));
        session.insert(me);
        List<Employee> employees = session.executeSelect(QueryBuilder.select(Employee.class).where(field("employee_id").eq(lit(105))), Employee.class);
        assertFalse(employees.isEmpty());
        assertThat(employees.getFirst()).usingRecursiveComparison().isEqualTo(me);
    }

    @Test
    void testSpecificTypeInserts(Session session) throws SQLException{
        TypeTest tt = new TypeTest(3, Status.RUNNING, LocalDateTime.of(2025, 1, 20, 15, 37, 41, 456*1_000_000), LocalTime.of(16, 23, 0));
        session.insert(tt);
        List<TypeTest> tests = session.executeSelect(QueryBuilder.select(TypeTest.class).where(field("id").eq(lit(3))), TypeTest.class);
        assertFalse(tests.isEmpty());
        assertThat(tests.getFirst()).usingRecursiveComparison().isEqualTo(tt);
    }

    @Test
    void testInsertWithManyToOneRelations(Session session) throws SQLException {
        Employee Steven = new Employee(100, "Steven", "King", LocalDate.of(2003, 6, 17));
        Department Marketing = new Department(20, "Marketing");
        Employee me = new Employee(105, "Salko", "Dinamitas", LocalDate.of(2002, 10, 10));
        me.setManager(Steven);
        me.setDepartment(Marketing);
        session.insert(me);
        QueryBuilder qb = QueryBuilder.select(Employee.class)
                .join("manager")
                .join("department")
                .where(field("employee_id").eq(lit(105)));
        List<Employee> employees = session.executeSelect(qb, Employee.class);
        assertThat(employees.getFirst()).usingRecursiveComparison().isEqualTo(me);
    }

    @Test
    void testInsertWithOneToOneRelations(Session session) throws SQLException{
        Employee me = new Employee(105, "Salko", "Dinamitas", LocalDate.of(2002, 10, 10));
        session.insert(me);
        Performance performance = new Performance(6, 9.1, me);
        session.insert(performance);
        QueryBuilder qb = QueryBuilder.select(Employee.class)
                .join("performance")
                .join("performance.employee")
                .where(field("employee_id").eq(lit(105)));
        me.setPerformance(performance);
        Employee expected = session.executeSelect(qb, Employee.class).getFirst();
        assertThat(expected).usingRecursiveComparison().isEqualTo(me);
    }

    @Test
    void testInsertWithManyToManyRelations(Session session) throws SQLException {
        Employee Steven = new Employee(100, "Steven", "King", LocalDate.of(2003, 6, 17));
        Department Marketing = new Department(20, "Marketing");
        Project Hr = new Project(1, "HR Onboarding System");
        Project Payrol = new Project(2, "Internal Payroll Platform");
        Employee me = new Employee(105, "Salko", "Dinamitas", LocalDate.of(2002, 10, 10));
        me.setManager(Steven);
        me.setDepartment(Marketing);
        me.setProjects(List.of(Hr, Payrol));
        session.insert(me);
        QueryBuilder qb = QueryBuilder.select(Employee.class)
                .join("manager")
                .join("department")
                .join("projects")
                .where(field("employee_id").eq(lit(105)));
        List<Employee> employees = session.executeSelect(qb, Employee.class);
        assertThat(employees.getFirst()).usingRecursiveComparison().isEqualTo(me);
    }

    @Test
    void testInsertWithGeneratedPK(Session session) throws SQLException {
        Project myProject = new Project();
        myProject.setProjectName("myProject");
        Project p = session.insert(myProject);
        assertEquals(6, p.getProjectId());
    }

    @Test
    void testInsertWithGeneratedPKAndManyToManyRelation(Session session) throws SQLException {
        Project myProject = new Project();
        myProject.setProjectName("myProject");
        Employee Steven = new Employee(100, "Steven", "King", LocalDate.of(2003, 6, 17));
        myProject.setEmployees(List.of(Steven));
        myProject = session.insert(myProject);
        QueryBuilder qb = QueryBuilder.select(Project.class)
                .join("employees")
                .where(field("project_id").eq(lit(6)));
        List<Project> projects = session.executeSelect(qb, Project.class);
        assertThat(projects.getFirst()).usingRecursiveComparison().isEqualTo(myProject);
    }

    //update tests
    @Test
    void testUpdate(Session session) throws SQLException {
        Employee Steven = new Employee(100, "Salko", "Dinamitas", LocalDate.of(2005, 6, 27));
        session.update(Steven);
        QueryBuilder qb = QueryBuilder.select(Employee.class)
                .where(field("employee_id").eq(lit(100)));
        List<Employee> employees = session.executeSelect(qb, Employee.class);
        assertThat(employees.getFirst()).usingRecursiveComparison().isEqualTo(Steven);
    }

    @Test
    void testUpdateWithIgnoreNull(Session session) throws SQLException {
        Employee Steven = new Employee();
        Steven.setEmployeeId(100);
        Steven.setFirstName("Salko");
        session.update(Steven, Session.IGNORE_NULL);
        QueryBuilder qb = QueryBuilder.select(Employee.class)
                .where(field("employee_id").eq(lit(100)));
        List<Employee> employees = session.executeSelect(qb, Employee.class);
        Employee expected = new Employee(100, "Salko", "King", LocalDate.of(2003, 6, 17));
        assertThat(employees.getFirst()).usingRecursiveComparison().isEqualTo(expected);
    }

    //test delete objects and relations
    @Test
    void testDelete(Session session) throws SQLException {
        Employee me = new Employee(105, "Salko", "Dinamitas", LocalDate.of(2002, 10, 10));
        session.insert(me);
        QueryBuilder qb = QueryBuilder.select(Employee.class);
        List<Employee> employees = session.executeSelect(qb, Employee.class);
        Employee Steven = new Employee(100, "Steven", "King", LocalDate.of(2003, 6, 17));
        Employee Neena = new Employee(101, "Neena", "Kochhar", LocalDate.of(2005, 9, 21));
        Employee Lex = new Employee(102, "Lex", "De Haan", LocalDate.of(2001, 1, 13));
        Employee Alexander = new Employee(103, "Alexander", "Hunold", LocalDate.of(2006, 1, 3));
        Employee Bruce = new Employee(104, "Bruce", "Ernst", LocalDate.of(2007, 5, 21));
        //check insert success
        assertThat(employees).usingRecursiveComparison().isEqualTo(List.of(Steven, Neena, Lex, Alexander, Bruce, me));
        session.delete(me);
        employees = session.executeSelect(qb, Employee.class);
        //check delete success
        assertThat(employees).usingRecursiveComparison().isEqualTo(List.of(Steven, Neena, Lex, Alexander, Bruce));
    }

    @Test
    void testManyToManyDisconnect(Session session) throws SQLException {
        Employee Steven = new Employee(100, "Steven", "King", LocalDate.of(2003, 6, 17));
        Employee Neena = new Employee(101, "Neena", "Kochhar", LocalDate.of(2005, 9, 21));
        Employee Lex = new Employee(102, "Lex", "De Haan", LocalDate.of(2001, 1, 13));
        Employee Alexander = new Employee(103, "Alexander", "Hunold", LocalDate.of(2006, 1, 3));
        Employee Bruce = new Employee(104, "Bruce", "Ernst", LocalDate.of(2007, 5, 21));
        Project Payrol = new Project(2, "Internal Payroll Platform");
        Project Mobile = new Project(4, "Mobile Sales Dashboard");
        //clear constraining relations for Bruce
        session.disconnectRows(Bruce, Payrol, "projects");
        session.disconnectRows(Mobile, Bruce, "employees");
        session.delete(Bruce);
        QueryBuilder qb = QueryBuilder.select(Employee.class);
        List<Employee> employees = session.executeSelect(qb, Employee.class);
        assertThat(employees).usingRecursiveComparison().isEqualTo(List.of(Steven, Neena, Lex, Alexander));
    }

    @Test
    void testOneToOneDisconnect(Session session) throws SQLException{
        Employee Steven = new Employee(100, "Steven", "King", LocalDate.of(2003, 6, 17));
        Performance performance = new Performance();
        performance.setPerformanceId(1);
        session.disconnectRows(Steven, performance, "performance");
        QueryBuilder qb = QueryBuilder.select(Employee.class)
                .join("performance")
                .where(field("employee_id").eq(lit(100)));
        List<Employee> employees = session.executeSelect(qb, Employee.class);
        assertTrue(employees.isEmpty());

        Performance performance2 = new Performance();
        performance2.setPerformanceId(2);
        session.disconnectRow(performance2, "employee");
        qb = QueryBuilder.select(Employee.class)
                .join("performance")
                .where(field("employee_id").eq(lit(101)));
        employees = session.executeSelect(qb, Employee.class);
        assertTrue(employees.isEmpty());
    }

    @Test
    void testOtherRelationDisconnect(Session session) throws SQLException {
        Employee Neena = new Employee(101, "Neena", "Kochhar", LocalDate.of(2005, 9, 21));
        Employee Bruce = new Employee(104, "Bruce", "Ernst", LocalDate.of(2007, 5, 21));
        Department Marketing = new Department(20, "Marketing");
        session.disconnectRow(Neena, "department");
        session.disconnectRows(Marketing, Bruce, "employees");
        QueryBuilder qb = QueryBuilder.select(Department.class)
                .join("employees")
                .where(field("department_id").eq(lit(20)));
        List<Department> departments = session.executeSelect(qb, Department.class);
        assertTrue(departments.isEmpty());
    }

    //tests for connect
    @Test
    void testManyToManyConnect(Session session) throws SQLException {
        Employee Bruce = new Employee(104, "Bruce", "Ernst", LocalDate.of(2007, 5, 21));
        Project Hr = new Project(1, "HR Onboarding System");
        Project Payrol = new Project(2, "Internal Payroll Platform");
        Project Mobile = new Project(4, "Mobile Sales Dashboard");
        Bruce.setProjects(List.of(Hr, Payrol, Mobile));
        session.connectRows(Bruce, Hr, "projects");
        QueryBuilder qb = QueryBuilder.select(Employee.class)
                .join("projects")
                .where(field("employee_id").eq(lit(104)))
                .orderBy(asc(field("projects.project_id")));
        Employee expected = session.executeSelect(qb, Employee.class).getFirst();
        assertThat(expected).usingRecursiveComparison().isEqualTo(Bruce);
    }

    @Test
    void testOneToOneConnect(Session session) throws SQLException{
        Employee me = new Employee(105, "Salko", "Dinamitas", LocalDate.of(2002, 10, 10));
        session.insert(me);
        Performance performance = new Performance(6, 9.9);
        session.insert(performance);
        session.connectRows(me, performance, "performance");
        QueryBuilder qb = QueryBuilder.select(Employee.class)
                .join("performance")
                .where(field("employee_id").eq(lit(105)));
        Employee expected = session.executeSelect(qb, Employee.class).getFirst();
        me.setPerformance(performance);
        assertThat(expected).usingRecursiveComparison().isEqualTo(me);

        session.disconnectRow(performance, "employee");
        session.connectRows(performance, me, "employee");
        expected = session.executeSelect(qb, Employee.class).getFirst();
        assertThat(expected).usingRecursiveComparison().isEqualTo(me);
    }

    @Test
    void testOtherRelationsConnect(Session session) throws SQLException {
        Employee me = new Employee(105, "Salko", "Dinamitas", LocalDate.of(2002, 10, 10));
        session.insert(me);
        Employee Steven = new Employee(100, "Steven", "King", LocalDate.of(2003, 6, 17));
        Department Marketing = new Department(20, "Marketing");
        session.connectRows(me, Steven, "manager");
        session.connectRows(Marketing, me, "employees");
        me.setDepartment(Marketing);
        me.setManager(Steven);
        QueryBuilder qb = QueryBuilder.select(Employee.class)
                .join("department")
                .join("manager")
                .where(field("employee_id").eq(lit(105)));
        Employee expected = session.executeSelect(qb, Employee.class).getFirst();
        assertThat(expected).usingRecursiveComparison().isEqualTo(me);
    }

    @Test
    void testTransactionRollbackOnFailure(Session session) throws SQLException {
        Employee me = new Employee(105, "Salko", "Dinamitas", LocalDate.of(2002, 10, 10));
        assertThrows(SQLException.class, () -> session.transaction((_ -> {
                session.insert(me);
                Employee Bruce = new Employee(104, "Bruce", "Ernst", LocalDate.of(2007, 5, 21));
                session.delete(Bruce);
            })));
        QueryBuilder qb = QueryBuilder.select(Employee.class);
        List<Employee> employees = session.executeSelect(qb, Employee.class);
        Employee Steven = new Employee(100, "Steven", "King", LocalDate.of(2003, 6, 17));
        Employee Neena = new Employee(101, "Neena", "Kochhar", LocalDate.of(2005, 9, 21));
        Employee Lex = new Employee(102, "Lex", "De Haan", LocalDate.of(2001, 1, 13));
        Employee Alexander = new Employee(103, "Alexander", "Hunold", LocalDate.of(2006, 1, 3));
        Employee Bruce = new Employee(104, "Bruce", "Ernst", LocalDate.of(2007, 5, 21));
        assertThat(employees).usingRecursiveComparison().isEqualTo(List.of(Steven, Neena, Lex, Alexander, Bruce));
    }
}
