package layering;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import raf.thesis.metadata.annotations.PDO;

@NoArgsConstructor@AllArgsConstructor
@Getter@Setter
@PDO
public class DepartmentsWithMaxEmployeeIdPDO {
    int department_id;
    int maxEmployeeId;
}
