package raf.thesis.query;

import raf.thesis.query.dialect.Dialect;

public interface ToSql {
    String toSql(Dialect dialect);
}
