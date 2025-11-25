package domain.hr;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import raf.thesis.metadata.annotations.Entity;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
//check if scanner goes beyond his package
@Entity(tableName = "xd")
public class Project {
    private int project_id;
    private String project_name;
    private List<Employee> employees;

    public Project(int project_id, String project_name) {
        this.project_id = project_id;
        this.project_name = project_name;
    }
}
