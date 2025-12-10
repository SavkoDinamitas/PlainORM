import discovery.test1.User;
import discovery.test2.Airplane;
import discovery.test2.Crew;
import discovery.test2.Flight;
import discovery.test2.Pilot;
import discovery.test8.Club;
import discovery.test8.Player;
import discovery.test9.PDOTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raf.thesis.metadata.ColumnMetadata;
import raf.thesis.metadata.EntityMetadata;
import raf.thesis.metadata.exception.DuplicateRelationNamesException;
import raf.thesis.metadata.exception.ListFieldRequiredException;
import raf.thesis.metadata.exception.RequiredFieldException;
import raf.thesis.metadata.exception.UnsupportedRelationException;
import raf.thesis.metadata.scan.MetadataScanner;
import raf.thesis.metadata.storage.MetadataStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class ScannerTest {
    //TODO: maybe no entries found support
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
        EntityMetadata meta = new EntityMetadata("users", User.class, List.of(User.class.getDeclaredField("user_id")), map, new ArrayList<>(), List.of(false));
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

    @Test
    void testCompositeKeyMetadataScan() throws NoSuchFieldException {
        MetadataScanner ms = new MetadataScanner();
        ms.discoverMetadata("discovery.test8");
        assertFalse(MetadataStorage.getAllData().isEmpty());
        Map<Class<?>, EntityMetadata> expected = new HashMap<>();
        expected.put(Club.class, Club.getMetadata());
        expected.put(Player.class, Player.getMetadata());
        assertThat(MetadataStorage.getAllData()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void testPDOsMetadataScan() throws NoSuchFieldException {
        MetadataScanner ms = new MetadataScanner();
        ms.discoverMetadata("discovery.test9");
        assertFalse(MetadataStorage.getAllData().isEmpty());
        Map<Class<?>, EntityMetadata> expected = new HashMap<>();
        expected.put(PDOTest.class, PDOTest.getMetadata());
        assertThat(MetadataStorage.getAllData()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void testIdMissingDetection(){
        MetadataScanner ms = new MetadataScanner();
        RequiredFieldException e = assertThrows(RequiredFieldException.class, () -> ms.discoverMetadata("discovery.test3"));
        assertTrue(e.getMessage().contains("requires id fields"));
    }

    @Test
    void testTableNameMissingDetection(){
        MetadataScanner ms = new MetadataScanner();
        RequiredFieldException e = assertThrows(RequiredFieldException.class, () -> ms.discoverMetadata("discovery.test4"));
        assertTrue(e.getMessage().contains("requires table name"));
    }

    @Test
    void testRelationWithNonEntityTypesDetection(){
        MetadataScanner ms = new MetadataScanner();
        UnsupportedRelationException e = assertThrows(UnsupportedRelationException.class, () -> ms.discoverMetadata("discovery.test5"));
        assertEquals("Class java.lang.Integer inside relation 'seats' is not an Entity", e.getMessage());
    }

    @Test
    void testListFieldRequiredForManyRelations(){
        MetadataScanner ms = new MetadataScanner();
        ListFieldRequiredException e = assertThrows(ListFieldRequiredException.class, () -> ms.discoverMetadata("discovery.test6"));
        assertEquals("Field subject of class Professor must be a list for ONE_TO_MANY relation", e.getMessage());
    }

    @Test
    void testSameRelationNamesInClassDetection(){
        MetadataScanner ms = new MetadataScanner();
        DuplicateRelationNamesException e = assertThrows(DuplicateRelationNamesException.class, () -> ms.discoverMetadata("discovery.test7"));
        assertTrue(e.getMessage().contains("Relation names must be unique inside class!"));
    }
}
