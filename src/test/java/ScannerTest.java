import ch.qos.logback.classic.Logger;
import discovery.User;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.slf4j.LoggerFactory;
import raf.thesis.mapper.DefaultMapperImplementation;
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
    @Test
    void testSimpleEntityMetadataScan() throws NoSuchFieldException {
        MetadataScanner ms = new MetadataScanner();
        ms.discoverMetadata("discovery");
        assertFalse(MetadataStorage.getAllData().isEmpty());
        Map<String, ColumnMetadata> map = new HashMap<>();
        map.put("userid", new ColumnMetadata("userid", User.class.getDeclaredField("user_id")));
        map.put("name", new ColumnMetadata("name", User.class.getDeclaredField("name")));
        map.put("username", new ColumnMetadata("username", User.class.getDeclaredField("username")));
        EntityMetadata meta = new EntityMetadata("users", User.class, List.of(User.class.getDeclaredField("user_id")), map, new ArrayList<>());
        Map<Class<?>, EntityMetadata> expected = Map.of(User.class, meta);
        assertThat(MetadataStorage.getAllData()).usingRecursiveComparison().isEqualTo(expected);
    }
}
