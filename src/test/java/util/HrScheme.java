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

    //@Language(value = "SQL")
    public static String H2SCRIPT = """
            ------------------------------------------------------------
            -- DROP TABLES (to allow re-running)
            ------------------------------------------------------------
            DROP TABLE IF EXISTS performances;
            DROP TABLE IF EXISTS employee_projects;
            DROP TABLE IF EXISTS projects;
            DROP TABLE IF EXISTS job_history;
            DROP TABLE IF EXISTS employees;
            DROP TABLE IF EXISTS jobs;
            DROP TABLE IF EXISTS departments;
            DROP TABLE IF EXISTS locations;
            DROP TABLE IF EXISTS countries;
            DROP TABLE IF EXISTS regions;
            DROP TABLE IF EXISTS enum_time_test;
            
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
                project_id INT PRIMARY KEY AUTO_INCREMENT,
                project_name VARCHAR(100) NOT NULL
            );
            
            INSERT INTO projects (project_id, project_name) VALUES
                (1, 'HR Onboarding System'),
                (2, 'Internal Payroll Platform'),
                (3, 'Corporate Website Redesign'),
                (4, 'Mobile Sales Dashboard'),
                (5, 'Cloud Migration Initiative');
            
            ALTER TABLE projects ALTER COLUMN project_id RESTART WITH 6;
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
            -- ==============================
            --  PERFORMANCES (new table)
            -- ==============================
            CREATE TABLE performances (
                performance_id INT PRIMARY KEY,
                performance_score DOUBLE,
                employee_id INT UNIQUE,
                FOREIGN KEY (employee_id) REFERENCES employees(employee_id) ON DELETE CASCADE ON UPDATE CASCADE
            );
            INSERT INTO performances VALUES (1, 9.5, 100);
            INSERT INTO performances VALUES (2, 8.1, 101);
            INSERT INTO performances VALUES (3, 6, 102);
            INSERT INTO performances VALUES (4, 7.8, 103);
            INSERT INTO performances VALUES (5, 10, 104);
            
            CREATE TABLE enum_time_test (
                id INT PRIMARY KEY,
                status ENUM('NEW', 'RUNNING', 'DONE'),
                created_at TIMESTAMP,
                run_time TIME
            );
            
            INSERT INTO enum_time_test VALUES
            (1, 'NEW', TIMESTAMP '2024-01-10 12:34:56.123', TIME '12:34:56'),
            (2, 'DONE', TIMESTAMP '2024-06-01 08:00:00', TIME '08:00:00');
            """;
    //@Language("SQL")
    public static final String PSQLScript = """
            DROP TABLE IF EXISTS performances CASCADE;
            DROP TABLE IF EXISTS employee_projects CASCADE;
            DROP TABLE IF EXISTS projects CASCADE;
            DROP TABLE IF EXISTS job_history CASCADE;
            DROP TABLE IF EXISTS employees CASCADE;
            DROP TABLE IF EXISTS jobs CASCADE;
            DROP TABLE IF EXISTS departments CASCADE;
            DROP TABLE IF EXISTS locations CASCADE;
            DROP TABLE IF EXISTS countries CASCADE;
            DROP TABLE IF EXISTS regions CASCADE;
            DROP TABLE IF EXISTS enum_time_test;
            DROP TYPE IF EXISTS status_enum;
            
            CREATE TABLE regions (
                region_id INTEGER PRIMARY KEY,
                region_name VARCHAR(50)
            );
            
            INSERT INTO regions VALUES
                (1, 'Europe'),
                (2, 'Americas'),
                (3, 'Asia'),
                (4, 'Middle East and Africa');
            
            CREATE TABLE countries (
                country_id CHAR(2) PRIMARY KEY,
                country_name VARCHAR(50),
                region_id INTEGER REFERENCES regions(region_id)
            );
            
            INSERT INTO countries VALUES
                ('US', 'United States of America', 2),
                ('UK', 'United Kingdom', 1),
                ('CA', 'Canada', 2),
                ('JP', 'Japan', 3);
            
            CREATE TABLE locations (
                location_id INTEGER PRIMARY KEY,
                street_address VARCHAR(100),
                postal_code VARCHAR(12),
                city VARCHAR(30),
                state_province VARCHAR(25),
                country_id CHAR(2) REFERENCES countries(country_id)
            );
            
            INSERT INTO locations VALUES
                (1000, '200 Innovation Drive', '95054', 'San Jose', 'California', 'US'),
                (1100, '10 Oxford Street', 'OX1', 'Oxford', NULL, 'UK'),
                (1200, '77 Bay Street', 'M5J', 'Toronto', 'Ontario', 'CA'),
                (1300, '1 Chiyoda', '100-8111', 'Tokyo', NULL, 'JP');
            
            CREATE TABLE departments (
                department_id INTEGER PRIMARY KEY,
                department_name VARCHAR(30) NOT NULL,
                manager_id INTEGER,
                location_id INTEGER REFERENCES locations(location_id)
            );
            
            INSERT INTO departments VALUES
                (10, 'Administration', NULL, 1000),
                (20, 'Marketing', NULL, 1100),
                (30, 'Purchasing', NULL, 1200),
                (40, 'Human Resources', NULL, 1300);
            
            CREATE TABLE jobs (
                job_id VARCHAR(10) PRIMARY KEY,
                job_title VARCHAR(35) NOT NULL,
                min_salary INTEGER,
                max_salary INTEGER
            );
            
            INSERT INTO jobs VALUES
                ('AD_PRESS', 'President', 15000, 30000),
                ('AD_VP', 'Vice President', 12000, 25000),
                ('IT_PROG', 'Programmer', 4000, 10000),
                ('MK_REP', 'Marketing Representative', 3000, 8000);
            
            CREATE TABLE employees (
                employee_id INTEGER PRIMARY KEY,
                first_name VARCHAR(20),
                last_name VARCHAR(25) NOT NULL,
                email VARCHAR(50),
                phone_number VARCHAR(20),
                hire_date DATE NOT NULL,
                job_id VARCHAR(10) REFERENCES jobs(job_id),
                salary INTEGER,
                manager_id INTEGER,
                department_id INTEGER REFERENCES departments(department_id)
            );
            
            INSERT INTO employees VALUES
                (100, 'Steven', 'King', 'SKING', '515.123.4567', DATE '2003-06-17', 'AD_PRESS', 24000, NULL, 10),
                (101, 'Neena', 'Kochhar', 'NKOCHHAR', '515.123.4568', DATE '2005-09-21', 'AD_VP', 17000, 100, 20),
                (102, 'Lex', 'De Haan', 'LDEHAAN', '515.123.4569', DATE '2001-01-13', 'IT_PROG', 9000, 100, 30),
                (103, 'Alexander', 'Hunold', 'AHUNOLD', '590.423.4567', DATE '2006-01-03', 'IT_PROG', 6000, 102, 30),
                (104, 'Bruce', 'Ernst', 'BERNST', '590.423.5678', DATE '2007-05-21', 'MK_REP', 4000, 101, 20);
            
            CREATE TABLE job_history (
                employee_id INTEGER REFERENCES employees(employee_id),
                start_date DATE,
                end_date DATE,
                job_id VARCHAR(10) REFERENCES jobs(job_id),
                department_id INTEGER REFERENCES departments(department_id),
                PRIMARY KEY (employee_id, start_date)
            );
            
            INSERT INTO job_history VALUES
                (101, DATE '2004-01-01', DATE '2005-09-20', 'MK_REP', 20),
                (103, DATE '2005-03-01', DATE '2006-01-02', 'IT_PROG', 30);
            
            CREATE TABLE projects (
                project_id INTEGER PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
                project_name VARCHAR(100) NOT NULL
            );
            
            INSERT INTO projects (project_id, project_name) VALUES
                (1, 'HR Onboarding System'),
                (2, 'Internal Payroll Platform'),
                (3, 'Corporate Website Redesign'),
                (4, 'Mobile Sales Dashboard'),
                (5, 'Cloud Migration Initiative');
            
            ALTER SEQUENCE projects_project_id_seq RESTART WITH 6;
            
            CREATE TABLE employee_projects (
                employee_id INTEGER NOT NULL REFERENCES employees(employee_id),
                project_id INTEGER NOT NULL REFERENCES projects(project_id),
                PRIMARY KEY (employee_id, project_id)
            );
            
            INSERT INTO employee_projects VALUES
                (100, 1),
                (100, 2),
                (100, 5),
                (101, 1),
                (102, 3),
                (102, 5),
                (103, 4),
                (104, 2),
                (104, 4);
            
            CREATE TABLE performances (
                performance_id INTEGER PRIMARY KEY,
                performance_score DOUBLE PRECISION,
                employee_id INTEGER UNIQUE
                    REFERENCES employees(employee_id)
                    ON DELETE CASCADE
                    ON UPDATE CASCADE
            );
            
            INSERT INTO performances VALUES
                (1, 9.5, 100),
                (2, 8.1, 101),
                (3, 6.0, 102),
                (4, 7.8, 103),
                (5, 10.0, 104);
            
            
            CREATE TABLE enum_time_test (
                id INT PRIMARY KEY,
                status VARCHAR(100) CONSTRAINT valid_status CHECK (status IN ('NEW', 'RUNNING', 'DONE')),
                created_at TIMESTAMP,
                run_time TIME
            );
            
            INSERT INTO enum_time_test VALUES
            (1, 'NEW', TIMESTAMP '2024-01-10 12:34:56.123', TIME '12:34:56'),
            (2, 'DONE', TIMESTAMP '2024-06-01 08:00:00', TIME '08:00:00');
            """;
    //@Language("SQL")
    public static final String MARIADBSCRIPT = """
            DROP TABLE IF EXISTS performances;
            DROP TABLE IF EXISTS employee_projects;
            DROP TABLE IF EXISTS projects;
            DROP TABLE IF EXISTS job_history;
            DROP TABLE IF EXISTS employees;
            DROP TABLE IF EXISTS jobs;
            DROP TABLE IF EXISTS departments;
            DROP TABLE IF EXISTS locations;
            DROP TABLE IF EXISTS countries;
            DROP TABLE IF EXISTS regions;
            DROP TABLE IF EXISTS enum_time_test;
            
            CREATE TABLE regions (
                region_id INT PRIMARY KEY,
                region_name VARCHAR(50)
            ) ENGINE=InnoDB;
            
            INSERT INTO regions VALUES
                (1, 'Europe'),
                (2, 'Americas'),
                (3, 'Asia'),
                (4, 'Middle East and Africa');
            
            CREATE TABLE countries (
                country_id CHAR(2) PRIMARY KEY,
                country_name VARCHAR(50),
                region_id INT,
                FOREIGN KEY (region_id) REFERENCES regions(region_id)
            ) ENGINE=InnoDB;
            
            INSERT INTO countries VALUES
                ('US', 'United States of America', 2),
                ('UK', 'United Kingdom', 1),
                ('CA', 'Canada', 2),
                ('JP', 'Japan', 3);
            
            CREATE TABLE locations (
                location_id INT PRIMARY KEY,
                street_address VARCHAR(100),
                postal_code VARCHAR(12),
                city VARCHAR(30),
                state_province VARCHAR(25),
                country_id CHAR(2),
                FOREIGN KEY (country_id) REFERENCES countries(country_id)
            ) ENGINE=InnoDB;
            
            INSERT INTO locations VALUES
                (1000, '200 Innovation Drive', '95054', 'San Jose', 'California', 'US'),
                (1100, '10 Oxford Street', 'OX1', 'Oxford', NULL, 'UK'),
                (1200, '77 Bay Street', 'M5J', 'Toronto', 'Ontario', 'CA'),
                (1300, '1 Chiyoda', '100-8111', 'Tokyo', NULL, 'JP');
            
            CREATE TABLE departments (
                department_id INT PRIMARY KEY,
                department_name VARCHAR(30) NOT NULL,
                manager_id INT,
                location_id INT,
                FOREIGN KEY (location_id) REFERENCES locations(location_id)
            ) ENGINE=InnoDB;
            
            INSERT INTO departments VALUES
                (10, 'Administration', NULL, 1000),
                (20, 'Marketing', NULL, 1100),
                (30, 'Purchasing', NULL, 1200),
                (40, 'Human Resources', NULL, 1300);
            
            CREATE TABLE jobs (
                job_id VARCHAR(10) PRIMARY KEY,
                job_title VARCHAR(35) NOT NULL,
                min_salary INT,
                max_salary INT
            ) ENGINE=InnoDB;
            
            INSERT INTO jobs VALUES
                ('AD_PRESS', 'President', 15000, 30000),
                ('AD_VP', 'Vice President', 12000, 25000),
                ('IT_PROG', 'Programmer', 4000, 10000),
                ('MK_REP', 'Marketing Representative', 3000, 8000);
            
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
            ) ENGINE=InnoDB;
            
            INSERT INTO employees VALUES
                (100, 'Steven', 'King', 'SKING', '515.123.4567', '2003-06-17', 'AD_PRESS', 24000, NULL, 10),
                (101, 'Neena', 'Kochhar', 'NKOCHHAR', '515.123.4568', '2005-09-21', 'AD_VP', 17000, 100, 20),
                (102, 'Lex', 'De Haan', 'LDEHAAN', '515.123.4569', '2001-01-13', 'IT_PROG', 9000, 100, 30),
                (103, 'Alexander', 'Hunold', 'AHUNOLD', '590.423.4567', '2006-01-03', 'IT_PROG', 6000, 102, 30),
                (104, 'Bruce', 'Ernst', 'BERNST', '590.423.5678', '2007-05-21', 'MK_REP', 4000, 101, 20);
            
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
            ) ENGINE=InnoDB;
            
            INSERT INTO job_history VALUES
                (101, '2004-01-01', '2005-09-20', 'MK_REP', 20),
                (103, '2005-03-01', '2006-01-02', 'IT_PROG', 30);
            
            CREATE TABLE projects (
                project_id INT PRIMARY KEY AUTO_INCREMENT,
                project_name VARCHAR(100) NOT NULL
            ) ENGINE=InnoDB;
            
            INSERT INTO projects (project_id, project_name) VALUES
                (1, 'HR Onboarding System'),
                (2, 'Internal Payroll Platform'),
                (3, 'Corporate Website Redesign'),
                (4, 'Mobile Sales Dashboard'),
                (5, 'Cloud Migration Initiative');
            
            ALTER TABLE projects AUTO_INCREMENT = 6;
            
            CREATE TABLE employee_projects (
                employee_id INT NOT NULL,
                project_id INT NOT NULL,
                PRIMARY KEY (employee_id, project_id),
                FOREIGN KEY (employee_id) REFERENCES employees(employee_id),
                FOREIGN KEY (project_id) REFERENCES projects(project_id)
            ) ENGINE=InnoDB;
            
            INSERT INTO employee_projects VALUES
                (100, 1),
                (100, 2),
                (100, 5),
                (101, 1),
                (102, 3),
                (102, 5),
                (103, 4),
                (104, 2),
                (104, 4);
            
            CREATE TABLE performances (
                performance_id INT PRIMARY KEY,
                performance_score DOUBLE,
                employee_id INT UNIQUE,
                FOREIGN KEY (employee_id)
                    REFERENCES employees(employee_id)
                    ON DELETE CASCADE
                    ON UPDATE CASCADE
            ) ENGINE=InnoDB;
            
            INSERT INTO performances VALUES
                (1, 9.5, 100),
                (2, 8.1, 101),
                (3, 6.0, 102),
                (4, 7.8, 103),
                (5, 10.0, 104);
            
            CREATE TABLE enum_time_test (
                id INT PRIMARY KEY,
                status ENUM('NEW', 'RUNNING', 'DONE'),
                created_at TIMESTAMP(3),
                run_time TIME
            );
            
            INSERT INTO enum_time_test VALUES
            (1, 'NEW', '2024-01-10 12:34:56.123', '12:34:56'),
            (2, 'DONE', '2024-06-01 08:00:00', '08:00:00');
            """;
    //@Language("SQL")
    public static final String MSSQLSCRIPT = """
            ------------------------------------------------------------
            -- DROP TABLES (order matters)
            ------------------------------------------------------------
            IF OBJECT_ID('performances', 'U') IS NOT NULL DROP TABLE performances;
            IF OBJECT_ID('employee_projects', 'U') IS NOT NULL DROP TABLE employee_projects;
            IF OBJECT_ID('projects', 'U') IS NOT NULL DROP TABLE projects;
            IF OBJECT_ID('job_history', 'U') IS NOT NULL DROP TABLE job_history;
            IF OBJECT_ID('employees', 'U') IS NOT NULL DROP TABLE employees;
            IF OBJECT_ID('jobs', 'U') IS NOT NULL DROP TABLE jobs;
            IF OBJECT_ID('departments', 'U') IS NOT NULL DROP TABLE departments;
            IF OBJECT_ID('locations', 'U') IS NOT NULL DROP TABLE locations;
            IF OBJECT_ID('countries', 'U') IS NOT NULL DROP TABLE countries;
            IF OBJECT_ID('regions', 'U') IS NOT NULL DROP TABLE regions;
            IF OBJECT_ID('enum_time_test', 'U') IS NOT NULL DROP TABLE enum_time_test;
            
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
                CONSTRAINT fk_countries_regions
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
                CONSTRAINT fk_locations_countries
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
                CONSTRAINT fk_departments_locations
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
                CONSTRAINT fk_employees_jobs
                    FOREIGN KEY (job_id) REFERENCES jobs(job_id),
                CONSTRAINT fk_employees_departments
                    FOREIGN KEY (department_id) REFERENCES departments(department_id)
            );
            
            INSERT INTO employees VALUES
                (100, 'Steven', 'King', 'SKING', '515.123.4567', '2003-06-17', 'AD_PRESS', 24000, NULL, 10),
                (101, 'Neena', 'Kochhar', 'NKOCHHAR', '515.123.4568', '2005-09-21', 'AD_VP', 17000, 100, 20),
                (102, 'Lex', 'De Haan', 'LDEHAAN', '515.123.4569', '2001-01-13', 'IT_PROG', 9000, 100, 30),
                (103, 'Alexander', 'Hunold', 'AHUNOLD', '590.423.4567', '2006-01-03', 'IT_PROG', 6000, 102, 30),
                (104, 'Bruce', 'Ernst', 'BERNST', '590.423.5678', '2007-05-21', 'MK_REP', 4000, 101, 20);
            
            ------------------------------------------------------------
            -- JOB HISTORY
            ------------------------------------------------------------
            CREATE TABLE job_history (
                employee_id INT,
                start_date DATE,
                end_date DATE,
                job_id VARCHAR(10),
                department_id INT,
                CONSTRAINT pk_job_history
                    PRIMARY KEY (employee_id, start_date),
                CONSTRAINT fk_job_history_employees
                    FOREIGN KEY (employee_id) REFERENCES employees(employee_id),
                CONSTRAINT fk_job_history_jobs
                    FOREIGN KEY (job_id) REFERENCES jobs(job_id),
                CONSTRAINT fk_job_history_departments
                    FOREIGN KEY (department_id) REFERENCES departments(department_id)
            );
            
            INSERT INTO job_history VALUES
                (101, '2004-01-01', '2005-09-20', 'MK_REP', 20),
                (103, '2005-03-01', '2006-01-02', 'IT_PROG', 30);
            
            ------------------------------------------------------------
            -- PROJECTS
            ------------------------------------------------------------
            CREATE TABLE projects (
                project_id INT IDENTITY(1,1) PRIMARY KEY,
                project_name VARCHAR(100) NOT NULL
            );
            
            SET IDENTITY_INSERT projects ON;
            INSERT INTO projects (project_id, project_name) VALUES
                (1, 'HR Onboarding System'),
                (2, 'Internal Payroll Platform'),
                (3, 'Corporate Website Redesign'),
                (4, 'Mobile Sales Dashboard'),
                (5, 'Cloud Migration Initiative');
            SET IDENTITY_INSERT projects OFF;
            
            ------------------------------------------------------------
            -- EMPLOYEE_PROJECTS
            ------------------------------------------------------------
            CREATE TABLE employee_projects (
                employee_id INT NOT NULL,
                project_id INT NOT NULL,
                CONSTRAINT pk_employee_projects
                    PRIMARY KEY (employee_id, project_id),
                CONSTRAINT fk_employee_projects_employees
                    FOREIGN KEY (employee_id) REFERENCES employees(employee_id),
                CONSTRAINT fk_employee_projects_projects
                    FOREIGN KEY (project_id) REFERENCES projects(project_id)
            );
            
            INSERT INTO employee_projects VALUES (100, 1);
            INSERT INTO employee_projects VALUES (100, 2);
            INSERT INTO employee_projects VALUES (100, 5);
            INSERT INTO employee_projects VALUES (101, 1);
            INSERT INTO employee_projects VALUES (102, 3);
            INSERT INTO employee_projects VALUES (102, 5);
            INSERT INTO employee_projects VALUES (103, 4);
            INSERT INTO employee_projects VALUES (104, 2);
            INSERT INTO employee_projects VALUES (104, 4);
            
            ------------------------------------------------------------
            -- PERFORMANCES
            ------------------------------------------------------------
            CREATE TABLE performances (
                performance_id INT PRIMARY KEY,
                performance_score FLOAT,
                employee_id INT NULL,
                CONSTRAINT fk_performances_employees
                    FOREIGN KEY (employee_id)
                    REFERENCES employees(employee_id)
                    ON DELETE CASCADE
                    ON UPDATE CASCADE
            );
            
            CREATE UNIQUE INDEX ux_performances_employee_id
            ON performances(employee_id)
            WHERE employee_id IS NOT NULL;
            
            INSERT INTO performances VALUES
                (1, 9.5, 100),
                (2, 8.1, 101),
                (3, 6.0, 102),
                (4, 7.8, 103),
                (5, 10.0, 104);
            
            CREATE TABLE enum_time_test (
                id INT PRIMARY KEY,
                status VARCHAR(10) CHECK (status IN ('NEW', 'RUNNING', 'DONE')),
                created_at DATETIME2,
                run_time TIME
            );
            
            INSERT INTO enum_time_test VALUES
            (1, 'NEW', '2024-01-10T12:34:56.123', '12:34:56'),
            (2, 'DONE', '2024-06-01T08:00:00', '08:00:00');
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
