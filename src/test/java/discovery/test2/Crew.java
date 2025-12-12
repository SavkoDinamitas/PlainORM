package discovery.test2;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import raf.thesis.metadata.ColumnMetadata;
import raf.thesis.metadata.EntityMetadata;
import raf.thesis.metadata.RelationMetadata;
import raf.thesis.metadata.RelationType;
import raf.thesis.metadata.annotations.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity(tableName = "crews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Crew {
    @Id
    private int crewID;
    @Column(columnName = "crewNumber")
    private int crewSize;
    @OneToOne(containsFk = false)
    private Pilot pilot;
    @OneToMany(relationName = "finished_flights", foreignKey = "fk_flights")
    private List<Flight> flights;

    public static EntityMetadata getMetadata() throws NoSuchFieldException {
        Map<String, ColumnMetadata> cols = new HashMap<>();
        cols.put("crewid", new ColumnMetadata("crewid", Crew.class.getDeclaredField("crewID")));
        cols.put("crewnumber", new ColumnMetadata("crewnumber", Crew.class.getDeclaredField("crewSize")));
        List<RelationMetadata> rel = new ArrayList<>();
        rel.add(new RelationMetadata(Crew.class.getDeclaredField("pilot"), "pilot", RelationType.ONE_TO_ONE, Pilot.class, List.of("crewid"), null, null, null, false));
        rel.add(new RelationMetadata(Crew.class.getDeclaredField("flights"), "finished_flights", RelationType.ONE_TO_MANY, Flight.class, List.of("fk_flights"), null, null, null));
        return new EntityMetadata("crews", Crew.class, List.of(Crew.class.getDeclaredField("crewID")), cols, rel, List.of(false));
    }
}
