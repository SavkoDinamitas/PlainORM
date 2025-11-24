package domain.hr;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor @NoArgsConstructor
public class Employee {
    private int employee_id;
    private String first_name;
    private String last_name;
    private LocalDate hire_date;
    private Department department;
    private Employee manager;
    private List<Project> projects;

    public Employee(int employee_id, String first_name, String last_name, LocalDate hire_date) {
        this.employee_id = employee_id;
        this.first_name = first_name;
        this.last_name = last_name;
        this.hire_date = hire_date;
    }
}
