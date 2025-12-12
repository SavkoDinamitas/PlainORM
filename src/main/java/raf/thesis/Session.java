package raf.thesis;

import raf.thesis.mapper.DefaultMapperImplementation;
import raf.thesis.mapper.RowMapper;
import raf.thesis.metadata.EntityMetadata;
import raf.thesis.metadata.scan.MetadataScanner;
import raf.thesis.metadata.storage.MetadataStorage;
import raf.thesis.query.DBUpdateSolver;
import raf.thesis.query.PreparedStatementQuery;
import raf.thesis.query.QueryBuilder;
import raf.thesis.query.dialect.ANSISQLDialect;
import raf.thesis.query.dialect.Dialect;
import raf.thesis.query.exceptions.EntityObjectRequiredException;
import raf.thesis.query.transaction.SQLTransactionBody;
import raf.thesis.query.transaction.SQLValuedTransactionBody;
import raf.thesis.query.tree.Literal;

import java.sql.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("ClassEscapesDefinedScope")
public class Session {
    private final ConnectionSupplier connectionSupplier;
    private final RowMapper rowMapper = new DefaultMapperImplementation();
    private final Dialect dialect = new ANSISQLDialect();
    private final DBUpdateSolver DBUpdateSolver = new DBUpdateSolver(dialect);
    private static final MetadataScanner metadataScanner = new MetadataScanner();

    private final ThreadLocal<Connection> activeConnection = new ThreadLocal<>();

    public Session(ConnectionSupplier connectionSupplier, String... scanPackages) {
        this.connectionSupplier = connectionSupplier;
        metadataScanner.discoverMetadata(scanPackages);
    }

    private <T> T runBody(SQLValuedTransactionBody<T> body) throws SQLException {
        if (activeConnection.get() != null) return body.execute(activeConnection.get());
        else
            try (var conn = connectionSupplier.getConnection()) {
                return body.execute(conn);
            }
    }

    public <T> List<T> executeSelect(QueryBuilder queryBuilder, Class<T> resultClass) throws SQLException {
        String sql = queryBuilder.build(dialect);
        if (sql == null)
            return null;
        return executeSelect(sql, resultClass);
    }

    public <T> List<T> executeSelect(String query, Class<T> resultClass) throws SQLException {
        return runBody((conn) -> {
            List<T> result;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                result = rowMapper.mapWithRelations(rs, resultClass);
            }
            return result;
        });
    }

    public <T> List<T> executePDOSelect(QueryBuilder queryBuilder, Class<T> resultClass) throws SQLException {
        String sql = queryBuilder.build(dialect);
        if (sql == null)
            return null;
        return executePDOSelect(sql, resultClass);
    }

    public <T> List<T> executePDOSelect(String query, Class<T> resultClass) throws SQLException {
        return runBody((conn -> {
            List<T> result;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                result = rowMapper.mapList(rs, resultClass);
            }
            return result;
        }));
    }

    public <T> Optional<T> executeSingleRowPDOSelect(QueryBuilder queryBuilder, Class<T> resultClass) throws SQLException {
        String sql = queryBuilder.build(dialect);
        if (sql == null)
            return Optional.empty();
        return executeSingleRowPDOSelect(sql, resultClass);
    }

    public <T> Optional<T> executeSingleRowPDOSelect(String query, Class<T> resultClass) throws SQLException {
        return runBody((conn) -> {
            Optional<T> result;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                result = Optional.ofNullable(rowMapper.map(rs, resultClass));
            }
            return result;
        });
    }

    public <T> T insert(T obj) throws SQLException {
        return runBody((conn -> {
            PreparedStatementQuery mainInsert = DBUpdateSolver.generateInsert(obj);
            PreparedStatement preparedStatement = conn.prepareStatement(mainInsert.getQuery(), extractKeys(obj));
            for (int i = 1; i <= mainInsert.getArguments().size(); i++) {
                bindLiteral(preparedStatement, i, mainInsert.getArguments().get(i - 1));
            }
            preparedStatement.executeUpdate();
            //in case of generated keys, map the returned keys on instance
            ResultSet rs = preparedStatement.getGeneratedKeys();
            rs.next();
            T keysObject = rowMapper.map(rs, obj);

            //solve many-to-many relationships
            List<PreparedStatementQuery> queries = DBUpdateSolver.generateManyToManyInserts(keysObject);

            //go in reverse as last element in list is the main insert, others are many to many inserts
            for (int k = queries.size() - 1; k >= 0; k--) {
                PreparedStatementQuery pq = queries.get(k);
                PreparedStatement ps = conn.prepareStatement(pq.getQuery());
                for (int i = 1; i <= pq.getArguments().size(); i++) {
                    bindLiteral(ps, i, pq.getArguments().get(i - 1));
                }
                ps.executeUpdate();
            }
            return keysObject;
        }));
    }

    public void update(Object obj) throws SQLException {
        PreparedStatementQuery update = DBUpdateSolver.updateObject(obj, false);
        executeUpdateStatement(update);
    }

    public void update(Object obj, IgnoreNull ignoreNull) throws SQLException {
        PreparedStatementQuery update = DBUpdateSolver.updateObject(obj, true);
        executeUpdateStatement(update);
    }

    public void delete(Object obj) throws SQLException {
        PreparedStatementQuery delete = DBUpdateSolver.deleteObject(obj);
        executeUpdateStatement(delete);
    }

    public void connectRows(Object obj1, Object obj2, String relationName) throws SQLException {
        PreparedStatementQuery connect = DBUpdateSolver.connect(obj1, obj2, relationName);
        executeUpdateStatement(connect);
    }

    //only for MANY-TO-ONE and ONE-TO-ONE with containsFK = true relations
    public void disconnectRow(Object obj1, String relationName) throws SQLException {
        PreparedStatementQuery disconnect = DBUpdateSolver.disconnect(obj1, null, relationName);
        executeUpdateStatement(disconnect);
    }

    //only for MANY-TO-MANY, ONE-TO-MANY and ONE-TO-ONE with containsFK = false relations
    public void disconnectRows(Object obj1, Object obj2, String relationName) throws SQLException {
        PreparedStatementQuery disconnect = DBUpdateSolver.disconnect(obj1, obj2, relationName);
        executeUpdateStatement(disconnect);
    }

    private void executeUpdateStatement(PreparedStatementQuery update) throws SQLException {
        runBody(conn -> {
            PreparedStatement preparedStatement = conn.prepareStatement(update.getQuery());
            for (int i = 1; i <= update.getArguments().size(); i++) {
                bindLiteral(preparedStatement, i, update.getArguments().get(i - 1));
            }
            preparedStatement.executeUpdate();
            return null;
        });
    }

    private String[] extractKeys(Object obj) {
        EntityMetadata metadata = MetadataStorage.get(obj.getClass());
        if (metadata == null)
            throw new EntityObjectRequiredException("Given object: " + obj.getClass().getName() + " is not an entity");

        String[] keys = new String[metadata.getIdFields().size()];
        int i = 0;
        for (var column : metadata.getColumns().values()) {
            if (metadata.getIdFields().contains(column.getField())) {
                keys[i++] = column.getColumnName();
            }
        }
        return keys;
    }

    private void bindLiteral(PreparedStatement ps, int idx, Literal lit) throws SQLException {
        switch (lit) {
            case Literal.DoubleCnst d -> ps.setDouble(idx, d.x());
            case Literal.LongCnst l -> ps.setLong(idx, l.x());
            case Literal.StringCnst s -> ps.setString(idx, s.x());
            case Literal.BoolCnst b -> ps.setBoolean(idx, b.x());
            case Literal.DateCnst d -> ps.setDate(idx, java.sql.Date.valueOf(d.x()));
            case Literal.DateTimeCnst dt -> ps.setTimestamp(idx, java.sql.Timestamp.valueOf(dt.x()));
            case Literal.TimeCnst t -> ps.setTime(idx, java.sql.Time.valueOf(t.x()));
            case Literal.NullCnst _ -> ps.setNull(idx, java.sql.Types.NULL);

            default -> throw new IllegalArgumentException("Unsupported literal: " + lit.getClass());
        }
    }

    private static class IgnoreNull {
        public String toString() {
            return "IGNORE NULL";
        }
    }

    public static final IgnoreNull IGNORE_NULL = new IgnoreNull();


    //transaction management
    public <T> T transaction(SQLValuedTransactionBody<T> body) throws SQLException {
        return withConnection(connection -> {
            connection.setAutoCommit(false);
            var committed = false;
            try {
                var ret = body.execute(connection);
                committed = true;
                connection.commit();
                return ret;
            } finally {
                if (!committed) connection.rollback();
            }
        });
    }

    public void transaction(SQLTransactionBody body) throws SQLException {
        transaction(conn -> {
            body.execute(conn);
            return null;
        });
    }

    public <T> T withConnection(Connection connection, SQLValuedTransactionBody<T> body) throws SQLException {
        Objects.requireNonNull(connection);
        if (activeConnection.get() != null)
            throw new IllegalStateException("Cannot nest withConnection or transaction constructs");
        try {
            activeConnection.set(connection);
            return body.execute(connection);
        } finally {
            activeConnection.remove();
        }
    }

    public void withConnection(Connection connection, SQLTransactionBody body) throws SQLException {
        withConnection(connection, conn -> {
            body.execute(conn);
            return null;
        });
    }

    public <T> T withConnection(SQLValuedTransactionBody<T> body) throws SQLException {
        try (var connection = connectionSupplier.getConnection()) {
            return withConnection(connection, body);
        }
    }

    public void withConnection(SQLTransactionBody body) throws SQLException {
        withConnection(conn -> {
            body.execute(conn);
            return null;
        });
    }
}
