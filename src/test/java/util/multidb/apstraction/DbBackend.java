package util.multidb.apstraction;

public interface DbBackend {
    String name();
    void start();
    String jdbcUrl();
    String jdbcUser();
    String jdbcPass();
    String initScript();
    void stop();
}
