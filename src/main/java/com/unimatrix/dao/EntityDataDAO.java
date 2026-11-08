package com.unimatrix.dao;

import com.unimatrix.service.ScheduleEditorService;
import java.sql.*;
import java.util.*;

/** Identifiers come only from this enum; all user values use prepared parameters. */
public class EntityDataDAO {
    public enum Kind {
        ROOM("Rooms", "rooms", "room_id", new String[]{"room_name", "building", "capacity"},
                new String[]{"Room Name", "Building", "Capacity"}, new int[]{50,50,0}),
        COURSE("Courses", "courses", "course_id", new String[]{"course_code", "course_title"},
                new String[]{"Course Code", "Course Title"}, new int[]{20,100}),
        BLOCK("Block Sections", "block_sections", "block_section_id", new String[]{"section_name", "student_count"},
                new String[]{"Section Name", "Students"}, new int[]{50,0}),
        FACULTY("Faculty", "faculty", "faculty_id", new String[]{"faculty_name", "department"},
                new String[]{"Faculty Name", "Department"}, new int[]{100,50});
        public final String label, table, foreignKey;
        public final String[] columns, labels;
        public final int[] limits;
        Kind(String label, String table, String fk, String[] columns, String[] labels, int[] limits) {
            this.label=label; this.table=table; this.foreignKey=fk; this.columns=columns; this.labels=labels; this.limits=limits;
        }
    }
    public record Row(int id, List<String> values) {
        public Row { values = List.copyOf(values); }
    }
    public List<Row> load(Kind kind) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) { return load(c, kind); }
    }
    public List<Row> load(Connection c, Kind kind) throws SQLException {
        List<Row> rows = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement("SELECT id, " + String.join(",", kind.columns) + " FROM " + kind.table + " ORDER BY id");
             ResultSet r = p.executeQuery()) {
            while (r.next()) {
                List<String> values = new ArrayList<>();
                for (String column : kind.columns) values.add(r.getString(column));
                rows.add(new Row(r.getInt("id"), values));
            }
        }
        return rows;
    }
    public static Row validated(Kind kind, Row row) {
        if (row.values().size() != kind.columns.length) throw new IllegalArgumentException("Incomplete record.");
        List<String> values = new ArrayList<>();
        for (int i=0; i<kind.columns.length; i++) {
            String value = row.values().get(i).trim();
            if (value.isEmpty()) throw new IllegalArgumentException(kind.labels[i] + " is required.");
            if (kind.limits[i] > 0) {
                if (value.codePointCount(0,value.length()) > kind.limits[i])
                    throw new IllegalArgumentException(kind.labels[i] + " exceeds " + kind.limits[i] + " characters.");
            } else {
                try {
                    int number=Integer.parseInt(value); if (number<=0) throw new NumberFormatException(); value=Integer.toString(number);
                } catch (NumberFormatException ex) { throw new IllegalArgumentException(kind.labels[i] + " must be a positive number."); }
            }
            values.add(value);
        }
        return new Row(row.id(),values);
    }
    public List<Row> save(Kind kind, List<Row> before, List<Row> after) throws SQLException {
        List<Row> normalized = after.stream().map(r -> validated(kind,r)).toList();
        return Transactions.write(c -> {
            Map<Integer,Row> current = index(load(c,kind)), original = index(before), retained=index(normalized);
            Set<Integer> affectedSchedules = new TreeSet<>();
            for (Row old : before) {
                Row next=retained.get(old.id());
                if (Objects.equals(next,old)) continue;
                if (!Objects.equals(current.get(old.id()),old))
                    throw new SQLException("Record " + old.id() + " changed or was deleted elsewhere. Reload before saving.");
                List<String> refs = new ArrayList<>();
                try (PreparedStatement p = c.prepareStatement("SELECT DISTINCT e.schedule_id, s.name, s.deleted FROM exam_schedules e LEFT JOIN schedule_files s ON s.id=e.schedule_id WHERE e."+kind.foreignKey+"=?")) {
                    p.setInt(1,old.id());
                    try (ResultSet r=p.executeQuery()) { while(r.next()) {
                        int id=r.getInt(1); if(id>0 && !r.getBoolean(3)) affectedSchedules.add(id);
                        refs.add(r.getString(2)==null?"legacy exam rows":r.getString(2)+" (#"+id+")");
                    }}
                }
                if (next==null && !refs.isEmpty()) throw new IllegalArgumentException("Cannot delete " + kind.label + " record " + old.id() + ". Used by: " + String.join(", ",refs));
                if (next==null) try(PreparedStatement p=c.prepareStatement("DELETE FROM "+kind.table+" WHERE id=?")) {p.setInt(1,old.id());p.executeUpdate();}
            }
            for(Row row:normalized) {
                if(row.id()>0 && !original.containsKey(row.id())) throw new IllegalArgumentException("Unknown record identity.");
                if(row.equals(original.get(row.id()))) continue;
                String sql = row.id()==0 ? "INSERT INTO "+kind.table+" ("+String.join(",",kind.columns)+") VALUES ("+String.join(",",Collections.nCopies(kind.columns.length,"?"))+")"
                        : "UPDATE "+kind.table+" SET "+String.join(",",Arrays.stream(kind.columns).map(s->s+"=?").toList())+" WHERE id=?";
                try(PreparedStatement p=c.prepareStatement(sql)) {
                    for(int i=0;i<row.values().size();i++) {
                        if(kind.limits[i]>0) p.setString(i+1,row.values().get(i));
                        else p.setInt(i+1,Integer.parseInt(row.values().get(i)));
                    }
                    if(row.id()>0) p.setInt(row.values().size()+1,row.id());
                    if(p.executeUpdate()!=1) throw new SQLException("Record was not saved.");
                }
            }
            // Capacity/population edits must not invalidate active timetables. Trash is revalidated on save/publication.
            if(kind!=Kind.FACULTY) for(int id:affectedSchedules) {
                try { ScheduleEditorService.validateStored(c,id); }
                catch(IllegalArgumentException ex) { throw new IllegalArgumentException("Change would invalidate schedule #"+id+": "+ex.getMessage(),ex); }
            }
            return load(c,kind);
        });
    }
    private static Map<Integer,Row> index(List<Row> rows) {
        Map<Integer,Row> result=new HashMap<>();
        for(Row row:rows) if(row.id()>0 && result.put(row.id(),row)!=null) throw new IllegalArgumentException("Duplicate record identity.");
        return result;
    }
}
