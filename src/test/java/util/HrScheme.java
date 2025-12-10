package util;

import domain.hr.Department;
import domain.hr.Employee;
import domain.hr.Project;
import org.intellij.lang.annotations.Language;
import raf.thesis.metadata.ColumnMetadata;
import raf.thesis.metadata.EntityMetadata;
import raf.thesis.metadata.RelationMetadata;
import raf.thesis.metadata.RelationType;
import raf.thesis.metadata.storage.MetadataStorage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HrScheme {
    @Language("SQL")
    public static String TESTJOIN1 = """
            SELECT
                d.department_id AS "%root.department_id",
                d.department_name AS "%root.department_name",
                e.employee_id AS "%root.employees.employee_id",
                e.first_name AS "%root.employees.first_name",
                e.last_name AS "%root.employees.last_name",
                e.hire_date AS "%root.employees.hire_date"
            FROM departments d
            JOIN employees e
                ON d.department_id = e.department_id
            WHERE d.department_id = 30;
            """;
    @Language("SQL")
    public static String OUTERJOIN = """
            SELECT
                d.department_id AS "%root.department_id",
                d.department_name AS "%root.department_name",
                e.employee_id AS "%root.employees.employee_id",
                e.first_name AS "%root.employees.first_name",
                e.last_name AS "%root.employees.last_name",
                e.hire_date AS "%root.employees.hire_date"
            FROM departments d
            LEFT OUTER JOIN employees e
                ON d.department_id = e.department_id;
            """;
    @Language("SQL")
    public static String MANYTOMANYJOIN = """
            SELECT
                p.project_id AS "%root.project_id",
                p.project_name AS "%root.project_name",
                e.employee_id AS "%root.employees.employee_id",
                e.first_name AS "%root.employees.first_name",
                e.last_name AS "%root.employees.last_name",
                e.hire_date AS "%root.employees.hire_date",
                r.project_id AS "%root.employees.projects.project_id",
                r.project_name AS "%root.employees.projects.project_name"
            FROM projects p
            JOIN employee_projects ep
                ON p.project_id = ep.project_id
            JOIN employees e
                ON ep.employee_id = e.employee_id
            JOIN projects r
                ON ep.project_id = r.project_id
            ORDER BY p.project_id;
            """;
    @Language("SQL")
    public static String RECURSIVEMULTIJOIN = """
            SELECT
                e.employee_id AS "%root.employee_id",
                e.first_name AS "%root.first_name",
                e.last_name AS "%root.last_name",
                e.hire_date AS "%root.hire_date",
                m.employee_id AS "%root.manager.employee_id",
                m.first_name AS "%root.manager.first_name",
                m.last_name AS "%root.manager.last_name",
                m.hire_date AS "%root.manager.hire_date",
                d.department_id AS "%root.department.department_id",
                d.department_name AS "%root.department.department_name",
                s.employee_id AS "%root.department.employees.employee_id",
                s.first_name AS "%root.department.employees.first_name",
                s.last_name AS "%root.department.employees.last_name",
                s.hire_date AS "%root.department.employees.hire_date"
            FROM employees e
            JOIN employees m
                ON e.manager_id = m.employee_id
            JOIN departments d
                ON e.department_id = d.department_id
            JOIN employees s ON d.department_id = s.department_id;
            """;

    @Language(value = "SQL")
    public static String SCRIPT = """
            ------------------------------------------------------------
            -- DROP TABLES (to allow re-running)
            ------------------------------------------------------------
            DROP TABLE IF EXISTS employee_projects;
            DROP TABLE IF EXISTS projects;
            DROP TABLE IF EXISTS job_history;
            DROP TABLE IF EXISTS employees;
            DROP TABLE IF EXISTS jobs;
            DROP TABLE IF EXISTS departments;
            DROP TABLE IF EXISTS locations;
            DROP TABLE IF EXISTS countries;
            DROP TABLE IF EXISTS regions;
            
            ------------------------------------------------------------
            -- REGIONS
            ------------------------------------------------------------
            CREATE TABLE regions (
                region_id INT PRIMARY KEY,
                region_name VARCHAR(50)
            );
            
            INSERT INTO regions VALUES
                (1, 'Europe'),
                (2, 'Americas'),
                (3, 'Asia'),
                (4, 'Middle East and Africa');
            
            ------------------------------------------------------------
            -- COUNTRIES
            ------------------------------------------------------------
            CREATE TABLE countries (
                country_id CHAR(2) PRIMARY KEY,
                country_name VARCHAR(50),
                region_id INT,
                FOREIGN KEY (region_id) REFERENCES regions(region_id)
            );
            
            INSERT INTO countries VALUES
                ('US', 'United States of America', 2),
                ('UK', 'United Kingdom', 1),
                ('CA', 'Canada', 2),
                ('JP', 'Japan', 3);
            
            ------------------------------------------------------------
            -- LOCATIONS
            ------------------------------------------------------------
            CREATE TABLE locations (
                location_id INT PRIMARY KEY,
                street_address VARCHAR(100),
                postal_code VARCHAR(12),
                city VARCHAR(30),
                state_province VARCHAR(25),
                country_id CHAR(2),
                FOREIGN KEY (country_id) REFERENCES countries(country_id)
            );
            
            INSERT INTO locations VALUES
                (1000, '200 Innovation Drive', '95054', 'San Jose', 'California', 'US'),
                (1100, '10 Oxford Street', 'OX1', 'Oxford', NULL, 'UK'),
                (1200, '77 Bay Street', 'M5J', 'Toronto', 'Ontario', 'CA'),
                (1300, '1 Chiyoda', '100-8111', 'Tokyo', NULL, 'JP');
            
            ------------------------------------------------------------
            -- DEPARTMENTS
            ------------------------------------------------------------
            CREATE TABLE departments (
                department_id INT PRIMARY KEY,
                department_name VARCHAR(30) NOT NULL,
                manager_id INT,
                location_id INT,
                FOREIGN KEY (location_id) REFERENCES locations(location_id)
            );
            
            INSERT INTO departments VALUES
                (10, 'Administration', NULL, 1000),
                (20, 'Marketing', NULL, 1100),
                (30, 'Purchasing', NULL, 1200),
                (40, 'Human Resources', NULL, 1300);
            
            ------------------------------------------------------------
            -- JOBS
            ------------------------------------------------------------
            CREATE TABLE jobs (
                job_id VARCHAR(10) PRIMARY KEY,
                job_title VARCHAR(35) NOT NULL,
                min_salary INT,
                max_salary INT
            );
            
            INSERT INTO jobs VALUES
                ('AD_PRESS', 'President', 15000, 30000),
                ('AD_VP', 'Vice President', 12000, 25000),
                ('IT_PROG', 'Programmer', 4000, 10000),
                ('MK_REP', 'Marketing Representative', 3000, 8000);
            
            ------------------------------------------------------------
            -- EMPLOYEES
            ------------------------------------------------------------
            CREATE TABLE employees (
                employee_id INT PRIMARY KEY,
                first_name VARCHAR(20),
                last_name VARCHAR(25) NOT NULL,
                email VARCHAR(50),
                phone_number VARCHAR(20),
                hire_date DATE NOT NULL,
                job_id VARCHAR(10),
                salary INT,
                manager_id INT,
                department_id INT,
                FOREIGN KEY (job_id) REFERENCES jobs(job_id),
                FOREIGN KEY (department_id) REFERENCES departments(department_id)
            );
            
            INSERT INTO employees VALUES
                (100, 'Steven', 'King', 'SKING', '515.123.4567', DATE '2003-06-17', 'AD_PRESS', 24000, NULL, 10),
                (101, 'Neena', 'Kochhar', 'NKOCHHAR', '515.123.4568', DATE '2005-09-21', 'AD_VP', 17000, 100, 20),
                (102, 'Lex', 'De Haan', 'LDEHAAN', '515.123.4569', DATE '2001-01-13', 'IT_PROG', 9000, 100, 30),
                (103, 'Alexander', 'Hunold', 'AHUNOLD', '590.423.4567', DATE '2006-01-03', 'IT_PROG', 6000, 102, 30),
                (104, 'Bruce', 'Ernst', 'BERNST', '590.423.5678', DATE '2007-05-21', 'MK_REP', 4000, 101, 20);
            
            ------------------------------------------------------------
            -- JOB HISTORY
            ------------------------------------------------------------
            CREATE TABLE job_history (
                employee_id INT,
                start_date DATE,
                end_date DATE,
                job_id VARCHAR(10),
                department_id INT,
                PRIMARY KEY (employee_id, start_date),
                FOREIGN KEY (employee_id) REFERENCES employees(employee_id),
                FOREIGN KEY (job_id) REFERENCES jobs(job_id),
                FOREIGN KEY (department_id) REFERENCES departments(department_id)
            );
            
            INSERT INTO job_history VALUES
                (101, DATE '2004-01-01', DATE '2005-09-20', 'MK_REP', 20),
                (103, DATE '2005-03-01', DATE '2006-01-02', 'IT_PROG', 30);
            
            -- ==============================
            --  PROJECTS (new table)
            -- ==============================
            CREATE TABLE projects (
                project_id INT PRIMARY KEY,
                project_name VARCHAR(100) NOT NULL
            );
            
            INSERT INTO projects (project_id, project_name) VALUES
                (1, 'HR Onboarding System'),
                (2, 'Internal Payroll Platform'),
                (3, 'Corporate Website Redesign'),
                (4, 'Mobile Sales Dashboard'),
                (5, 'Cloud Migration Initiative');

            -- ==============================
            --  EMPLOYEE_PROJECTS (junction table, N:M)
            -- ==============================
            CREATE TABLE employee_projects (
                employee_id INT NOT NULL,
                project_id INT NOT NULL,
                PRIMARY KEY (employee_id, project_id),
                FOREIGN KEY (employee_id) REFERENCES employees(employee_id),
                FOREIGN KEY (project_id)  REFERENCES projects(project_id)
            );
            -- Steven King – lead on internal systems
            INSERT INTO employee_projects VALUES (100, 1);
            INSERT INTO employee_projects VALUES (100, 2);
            INSERT INTO employee_projects VALUES (100, 5);
            
            -- Neena Kochhar – HR and onboarding
            INSERT INTO employee_projects VALUES (101, 1);
            
            -- Lex De Haan – website redesign & cloud
            INSERT INTO employee_projects VALUES (102, 3);
            INSERT INTO employee_projects VALUES (102, 5);
            
            -- Alexander Hunold – mobile dashboard
            INSERT INTO employee_projects VALUES (103, 4);
            
            -- Bruce Ernst – works on payroll & mobile
            INSERT INTO employee_projects VALUES (104, 2);
            INSERT INTO employee_projects VALUES (104, 4);
            """;

    public static void fillMetadataManually() throws NoSuchFieldException {
        //Departments
        Map<String, ColumnMetadata> map = new HashMap<>();
        map.put("department_id", new ColumnMetadata("department_id", Department.class.getDeclaredField("department_id")));
        map.put("department_name", new ColumnMetadata("department_name", Department.class.getDeclaredField("department_name")));
        MetadataStorage.register(new EntityMetadata(
                "departments",
                Department.class,
                List.of(Department.class.getDeclaredField("department_id")),
                map,
                List.of(new RelationMetadata(
                        Department.class.getDeclaredField("employees"),
                        "employees",
                        RelationType.ONE_TO_MANY,
                        Employee.class)), List.of(false)));
        //Employees
        Map<String, ColumnMetadata> emap = new HashMap<>();
        emap.put("employee_id", new ColumnMetadata("employee_id", Employee.class.getDeclaredField("employee_id")));
        emap.put("first_name", new ColumnMetadata("first_name", Employee.class.getDeclaredField("first_name")));
        emap.put("last_name", new ColumnMetadata("last_name", Employee.class.getDeclaredField("last_name")));
        emap.put("hire_date", new ColumnMetadata("hire_date", Employee.class.getDeclaredField("hire_date")));
        MetadataStorage.register(new EntityMetadata(
                        "employees",
                        Employee.class,
                        List.of(Employee.class.getDeclaredField("employee_id")),
                        emap,
                        List.of(new RelationMetadata(
                                Employee.class.getDeclaredField("department"),
                                "department",
                                RelationType.MANY_TO_ONE,
                                Department.class
                                ),
                                new RelationMetadata(
                                        Employee.class.getDeclaredField("manager"),
                                        "manager",
                                        RelationType.MANY_TO_ONE,
                                        Employee.class
                                        ),
                                new RelationMetadata(
                                        Employee.class.getDeclaredField("projects"),
                                        "projects",
                                        RelationType.MANY_TO_MANY,
                                        Project.class,
                                        "employees"
                                )
                        ), List.of(false)
                )
        );
        //Projects
        //Departments
        Map<String, ColumnMetadata> pmap = new HashMap<>();
        pmap.put("project_id", new ColumnMetadata("project_id", Project.class.getDeclaredField("project_id")));
        pmap.put("project_name", new ColumnMetadata("project_name", Project.class.getDeclaredField("project_name")));
        MetadataStorage.register(new EntityMetadata(
                "projects",
                Project.class,
                List.of(Project.class.getDeclaredField("project_id")),
                pmap,
                List.of(new RelationMetadata(
                        Project.class.getDeclaredField("employees"),
                        "employees",
                        RelationType.MANY_TO_MANY,
                        Employee.class,
                        "projects")), List.of(false)));
    }
}
