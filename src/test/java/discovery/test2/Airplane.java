package discovery.test2;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import raf.thesis.metadata.ColumnMetadata;
import raf.thesis.metadata.EntityMetadata;
import raf.thesis.metadata.RelationMetadata;
import raf.thesis.metadata.RelationType;
import raf.thesis.metadata.annotations.Entity;
import raf.thesis.metadata.annotations.Id;
import raf.thesis.metadata.annotations.ManyToMany;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity(tableName = "airplanes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Airplane {
    @Id
    int id;
    String name;
    @ManyToMany(joinedTableName = "airplanes_flights")
    private List<Flight> flights;

    public static EntityMetadata getMetadata() throws NoSuchFieldException {
        Map<String, ColumnMetadata> cols = new HashMap<>();
        cols.put("id", new ColumnMetadata("id", Airplane.class.getDeclaredField("id")));
        cols.put("name", new ColumnMetadata("name", Airplane.class.getDeclaredField("name")));
        List<RelationMetadata> rel = new ArrayList<>();
        rel.add(new RelationMetadata(Airplane.class.getDeclaredField("flights"), "flights", RelationType.MANY_TO_MANY, Flight.class, List.of("flightnumber"), "airplanes_flights", List.of("id"), null));
        return new EntityMetadata("airplanes", Airplane.class, List.of(Airplane.class.getDeclaredField("id")), cols, rel, List.of(false));
    }
}
