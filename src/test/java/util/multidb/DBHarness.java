package util.multidb;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import util.HrScheme;
import util.multidb.apstraction.DbBackend;
import util.multidb.apstraction.SimpleBackend;
import util.multidb.apstraction.TestContainerDb;

import java.sql.DriverManager;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class DBHarness implements AutoCloseable {
    private final PostgreSQLContainer psqlContainer = new PostgreSQLContainer("postgres:18");
    private final MariaDBContainer mariaDBContainer = new MariaDBContainer("mariadb:12");
    private final MSSQLServerContainer mssqlServerContainer = new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();
    @Getter
    private final List<DbUnderTest> dbs;
    private final List<DbBackend> dbBackends;

    public DBHarness() {
        //list of all dbc for testing
        dbBackends = List.of(
                new SimpleBackend("H2",
                        "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
                        "sa",
                        "",
                        HrScheme.H2SCRIPT),
                new TestContainerDb(psqlContainer, "Postgres", HrScheme.PSQLScript),
                new TestContainerDb(mariaDBContainer, "MariaDB", HrScheme.MARIADBSCRIPT, "?allowMultiQueries=true"),
                new TestContainerDb(mssqlServerContainer, "MSSQL Server", HrScheme.MSSQLSCRIPT)
        );
        //start all dbs
        dbBackends.forEach(backend -> { try { backend.start();} catch (Exception e) { log.error(e.getMessage(), e); } });

        //make DBUnderTest instances for each db with jdbc standard connection and hikaricp pool
        dbs = dbBackends.stream().flatMap(backend -> Stream.of(
                hikari(backend.name(), backend.jdbcUrl(), backend.jdbcUser(), backend.jdbcPass(), backend.initScript()),
                new DbUnderTest(backend.name(), () -> DriverManager.getConnection(backend.jdbcUrl(), backend.jdbcUser(), backend.jdbcPass()), () -> {
                }, backend.initScript())
        )).toList();
    }

    @Override
    public void close() {
        for(DbUnderTest db : dbs){
            db.closeFunction().run();
        }
        dbBackends.forEach(backend -> { try { backend.stop();} catch (Exception e) { log.error(e.getMessage(), e); } });
    }

    private DbUnderTest hikari(
            String dbName, String url, String user, String pass, String initScript
    ) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(4);
        var dataSource = new HikariDataSource(cfg);
        return new DbUnderTest(dbName + " with hikaricp", dataSource::getConnection, dataSource::close, initScript);
    }

}
