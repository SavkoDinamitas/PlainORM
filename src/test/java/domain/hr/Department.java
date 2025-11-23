package domain.hr;

import lombok.*;

import java.util.List;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class Department {
    private int department_id;
    private String department_name;
    private List<Employee> employees;

    public Department(int department_id, String department_name) {
        this.department_id = department_id;
        this.department_name = department_name;
    }
}
