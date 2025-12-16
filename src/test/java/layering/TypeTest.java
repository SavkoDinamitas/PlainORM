package layering;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import raf.thesis.metadata.annotations.Column;
import raf.thesis.metadata.annotations.Entity;
import raf.thesis.metadata.annotations.Id;

import java.time.LocalDateTime;
import java.time.LocalTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity(tableName = "enum_time_test")
public class TypeTest {
    @Id
    private int id;
    private Status status;
    @Column(columnName = "created_at")
    private LocalDateTime createdAt;
    @Column(columnName = "run_time")
    private LocalTime runTime;
}
