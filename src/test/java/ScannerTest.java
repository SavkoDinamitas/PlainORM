import discovery.test1.User;
import discovery.test2.Airplane;
import discovery.test2.Crew;
import discovery.test2.Flight;
import discovery.test2.Pilot;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raf.thesis.metadata.ColumnMetadata;
import raf.thesis.metadata.EntityMetadata;
import raf.thesis.metadata.scan.MetadataScanner;
import raf.thesis.metadata.storage.MetadataStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class ScannerTest {
    //TODO: multiple relations with same name, all lowercase, all exceptions, complex keys
    @BeforeEach
    void clearMetadataStorage(){
        MetadataStorage.removeAll();
    }

    @Test
    void testSimpleEntityMetadataScan() throws NoSuchFieldException {
        MetadataScanner ms = new MetadataScanner();
        ms.discoverMetadata("discovery.test1");
        assertFalse(MetadataStorage.getAllData().isEmpty());
        Map<String, ColumnMetadata> map = new HashMap<>();
        map.put("userid", new ColumnMetadata("userid", User.class.getDeclaredField("user_id")));
        map.put("name", new ColumnMetadata("name", User.class.getDeclaredField("name")));
        map.put("username", new ColumnMetadata("username", User.class.getDeclaredField("username")));
        EntityMetadata meta = new EntityMetadata("users", User.class, List.of(User.class.getDeclaredField("user_id")), map, new ArrayList<>());
        Map<Class<?>, EntityMetadata> expected = Map.of(User.class, meta);
        assertThat(MetadataStorage.getAllData()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void testMultipleRelationsMetadataScan() throws NoSuchFieldException {
        MetadataScanner ms = new MetadataScanner();
        ms.discoverMetadata("discovery.test2");
        assertFalse(MetadataStorage.getAllData().isEmpty());
        Map<Class<?>, EntityMetadata> expected = new HashMap<>();
        expected.put(Airplane.class, Airplane.getMetadata());
        expected.put(Crew.class, Crew.getMetadata());
        expected.put(Flight.class, Flight.getMetadata());
        expected.put(Pilot.class, Pilot.getMetadata());
        assertThat(MetadataStorage.getAllData()).usingRecursiveComparison().isEqualTo(expected);
    }
}
