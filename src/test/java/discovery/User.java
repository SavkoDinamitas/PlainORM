package discovery;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import raf.thesis.metadata.annotations.Column;
import raf.thesis.metadata.annotations.Entity;
import raf.thesis.metadata.annotations.Id;

@Entity(tableName = "users")
@Getter@Setter@NoArgsConstructor
public class User {
    @Id
    @Column(columnName = "userId")
    private int user_id;
    @Column(columnName = "name")
    private String name;
    private String username;
}
