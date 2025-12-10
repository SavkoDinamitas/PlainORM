package discovery.test8;

import raf.thesis.metadata.ColumnMetadata;
import raf.thesis.metadata.EntityMetadata;
import raf.thesis.metadata.annotations.Entity;
import raf.thesis.metadata.annotations.Id;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity(tableName = "players")
public class Player {
    @Id
    private int number;
    @Id
    private String clas;
    private String name;

    public static EntityMetadata getMetadata() throws NoSuchFieldException {
        Map<String, ColumnMetadata> cols = new HashMap<>();
        cols.put("number", new ColumnMetadata("number", Player.class.getDeclaredField("number")));
        cols.put("clas", new ColumnMetadata("clas", Player.class.getDeclaredField("clas")));
        cols.put("name", new ColumnMetadata("name", Player.class.getDeclaredField("name")));
        return new EntityMetadata("players", Player.class, List.of(Player.class.getDeclaredField("number"), Player.class.getDeclaredField("clas")), cols, new ArrayList<>(), List.of(false, false));
    }
}
