package discovery.test2;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import raf.thesis.metadata.ColumnMetadata;
import raf.thesis.metadata.EntityMetadata;
import raf.thesis.metadata.annotations.Entity;
import raf.thesis.metadata.annotations.Id;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity(tableName = "pilots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pilot {
    @Id
    private int pilotId;
    private String pilotName;

    public static EntityMetadata getMetadata() throws NoSuchFieldException {
        Map<String, ColumnMetadata> cols = new HashMap<>();
        cols.put("pilotid", new ColumnMetadata("pilotid", Pilot.class.getDeclaredField("pilotId")));
        cols.put("pilotname", new ColumnMetadata("pilotname", Pilot.class.getDeclaredField("pilotName")));
        return new EntityMetadata("pilots", Pilot.class, List.of(Pilot.class.getDeclaredField("pilotId")), cols, new ArrayList<>(), List.of(false));
    }
}
