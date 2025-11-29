import discovery.test2.Airplane;
import discovery.test2.Crew;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import raf.thesis.metadata.scan.MetadataScanner;
import raf.thesis.query.QueryBuilder;

import static org.junit.jupiter.api.Assertions.*;

public class QueryBuilderTest {
    @BeforeAll
    public static void fillMetadata(){
        MetadataScanner ms = new MetadataScanner();
        ms.discoverMetadata("discovery.test2");
    }

    @Test
    public void testSingleJoinGeneration(){
        String check = QueryBuilder.select(Crew.class).join("pilot").generateJoinClauses();
        assertEquals("INNER JOIN pilots AS \"pilot\" ON ((\"pilot\".pilotid) = (\"%root\".pilotid))\n", check);
    }

    @Test
    public void testManyToManyJoinGeneration(){
        String check = QueryBuilder.select(Airplane.class).join("flights").generateJoinClauses();
        assertEquals("INNER JOIN airplanes_flights AS \"airplanes_flights\" ON ((\"airplanes_flights\".id) = (\"%root\".id))\n" +
                "INNER JOIN flights AS \"flights\" ON ((\"flights\".flightnumber) = (\"airplanes_flights\".flightnumber))\n", check);
    }

    @Test
    public void testInDepthJoinGeneration(){
        String check = QueryBuilder.select(Airplane.class).join("flights").join("flights.crew").join("flights.crew.pilot").generateJoinClauses();
        assertEquals("INNER JOIN airplanes_flights AS \"airplanes_flights\" ON ((\"airplanes_flights\".id) = (\"%root\".id))\n" +
                "INNER JOIN flights AS \"flights\" ON ((\"flights\".flightnumber) = (\"airplanes_flights\".flightnumber))\n" +
                "INNER JOIN crews AS \"flights.crew\" ON ((\"flights.crew\".crewid) = (\"flights\".crewid))\n" +
                "INNER JOIN pilots AS \"flights.crew.pilot\" ON ((\"flights.crew.pilot\".pilotid) = (\"flights.crew\".pilotid))\n", check);
    }
}
