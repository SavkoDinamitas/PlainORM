package util.multidb.apstraction;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SimpleBackend implements DbBackend{
    private final String name;
    private final String jdbcUrl;
    private final String jdbcUser;
    private final String jdbcPass;
    private final String initScript;
    @Override
    public String name() {
        return name;
    }

    @Override
    public void start() {

    }

    @Override
    public String jdbcUrl() {
        return jdbcUrl;
    }

    @Override
    public String jdbcUser() {
        return jdbcUser;
    }

    @Override
    public String jdbcPass() {
        return jdbcPass;
    }

    @Override
    public String initScript() {
        return initScript;
    }

    @Override
    public void stop() {

    }
}
