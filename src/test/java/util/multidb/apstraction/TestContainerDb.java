package util.multidb.apstraction;

import lombok.AllArgsConstructor;
import org.testcontainers.containers.JdbcDatabaseContainer;
@AllArgsConstructor
public class TestContainerDb implements DbBackend{
    private final JdbcDatabaseContainer<?> tc;
    private final String name;
    private final String initScript;
    private String connectionArgs = "";

    public TestContainerDb(JdbcDatabaseContainer<?> tc, String name, String initScript) {
        this.tc = tc;
        this.name = name;
        this.initScript = initScript;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void start() {
        tc.start();
    }

    @Override
    public String jdbcUrl() {
        return tc.getJdbcUrl() + connectionArgs;
    }

    @Override
    public String jdbcUser() {
        return tc.getUsername();
    }

    @Override
    public String jdbcPass() {
        return tc.getPassword();
    }

    @Override
    public String initScript() {
        return initScript;
    }

    @Override
    public void stop() {
        tc.stop();
    }
}
