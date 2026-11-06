package com.unimatrix.dao;

import java.sql.*;

public final class Transactions {
    private Transactions() { }
    @FunctionalInterface public interface Work<T> { T run(Connection connection) throws SQLException; }
    public static <T> T write(Work<T> work) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                // One lock order across entity edits and every schedule-writing path.
                try (PreparedStatement p = c.prepareStatement("SELECT id FROM scheduler_write_lock WHERE id = 1 FOR UPDATE");
                     ResultSet r = p.executeQuery()) {
                    if (!r.next()) throw new SQLException("Run database/002_schedule_editor.sql before saving.");
                }
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException ex) {
                try { c.rollback(); } catch (SQLException rollback) { ex.addSuppressed(rollback); }
                throw ex;
            }
        }
    }
}
