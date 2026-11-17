package com.unimatrix.dao;

import com.unimatrix.model.TrackTerm;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Both Manage Data tabs use these same foreign-key targets. */
public final class TrackTermDAO {
    public List<TrackTerm> load() throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) { return load(c); }
    }

    public List<TrackTerm> load(Connection c) throws SQLException {
        List<TrackTerm> options = new ArrayList<>();
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(
                "SELECT id,label,track_name,term_number FROM track_terms "
                + "ORDER BY CASE WHEN id=1 THEN 0 ELSE 1 END,track_name,term_number,label")) {
            while (r.next()) options.add(new TrackTerm(r.getInt("id"), r.getString("label"),
                    r.getString("track_name"), r.getInt("term_number")));
        }
        return List.copyOf(options);
    }

    public static int requireId(Connection c, String label) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT id FROM track_terms WHERE label=?")) {
            p.setString(1, label);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) return r.getInt(1);
            }
        }
        throw new IllegalArgumentException("Unknown track '" + label + "'. Reload and select a track from the dropdown.");
    }
}
