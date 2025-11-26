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
import raf.thesis.metadata.annotations.ManyToOne;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(tableName = "flights")
public class Flight {
    @Id
    private String flightNumber;
    private String flightType;
    @ManyToOne
    private Crew crew;
    @ManyToMany(joinedTableName = "airplanes_flights")
    private List<Airplane> airplanes;

    public static EntityMetadata getMetadata() throws NoSuchFieldException {
        Map<String, ColumnMetadata> cols = new HashMap<>();
        cols.put("flightnumber", new ColumnMetadata("flightnumber", Flight.class.getDeclaredField("flightNumber")));
        cols.put("flighttype", new ColumnMetadata("flighttype", Flight.class.getDeclaredField("flightType")));
        List<RelationMetadata> rel = new ArrayList<>();
        rel.add(new RelationMetadata(Flight.class.getDeclaredField("crew"), "crew", RelationType.MANY_TO_ONE, Crew.class, List.of("crewid"), null, null, null));
        rel.add(new RelationMetadata(Flight.class.getDeclaredField("airplanes"), "airplanes", RelationType.MANY_TO_MANY, Airplane.class, List.of("id"), "airplanes_flights", List.of("flightnumber"), null));
        return new EntityMetadata("flights", Flight.class, List.of(Flight.class.getDeclaredField("flightNumber")), cols, rel);
    }
}
