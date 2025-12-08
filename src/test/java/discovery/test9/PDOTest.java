package discovery.test9;

import raf.thesis.metadata.ColumnMetadata;
import raf.thesis.metadata.EntityMetadata;
import raf.thesis.metadata.annotations.Column;
import raf.thesis.metadata.annotations.PDO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@PDO
public class PDOTest {
    @Column(columnName = "department_id")
    private int departmentId;
    private int maxSalary;

    public static EntityMetadata getMetadata() throws NoSuchFieldException {
        Map<String, ColumnMetadata> columns = new HashMap<>();
        columns.put("department_id", new ColumnMetadata("department_id", PDOTest.class.getDeclaredField("departmentId")));
        columns.put("maxsalary", new ColumnMetadata("maxsalary", PDOTest.class.getDeclaredField("maxSalary")));
        return new EntityMetadata(null, PDOTest.class, new ArrayList<>(), columns, new ArrayList<>());
    }
}
