package discovery.test8;

import raf.thesis.metadata.ColumnMetadata;
import raf.thesis.metadata.EntityMetadata;
import raf.thesis.metadata.RelationMetadata;
import raf.thesis.metadata.RelationType;
import raf.thesis.metadata.annotations.Entity;
import raf.thesis.metadata.annotations.Id;
import raf.thesis.metadata.annotations.OneToMany;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity(tableName = "clubs")
public class Club {
    @Id
    private int id;
    private String name;
    @OneToMany
    private List<Player> players;

    public static EntityMetadata getMetadata() throws NoSuchFieldException {
        Map<String, ColumnMetadata> cols = new HashMap<>();
        cols.put("id", new ColumnMetadata("id", Club.class.getDeclaredField("id")));
        cols.put("name", new ColumnMetadata("name", Club.class.getDeclaredField("name")));
        List<RelationMetadata> rels = new ArrayList<>();
        rels.add(new RelationMetadata(Club.class.getDeclaredField("players"), "players", RelationType.ONE_TO_MANY, Player.class, List.of("id"), null, null, null));
        return new EntityMetadata("clubs", Club.class, List.of(Club.class.getDeclaredField("id")), cols, rels, List.of(false));
    }
}
