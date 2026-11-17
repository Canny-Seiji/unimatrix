package com.unimatrix.view;

import com.unimatrix.dao.EntityDataDAO.Row;
import com.unimatrix.model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Exercises real Swing cell editors on the event dispatch thread, without database I/O. */
public final class ManageDataTrackChecks {
    private static int checks;
    public static void main(String[] args) throws Exception {
        checks=0;
        SwingUtilities.invokeAndWait(()->{
            try { run(); } catch (Exception ex) { throw new RuntimeException(ex); }
        });
        System.out.println("Manage Data track checks passed: "+checks);
    }

    private static void run() throws Exception {
        List<JTable> tables=new ArrayList<>();
        collect(new ManageDataPanel(false),tables);
        List<TrackTerm> choices=new ArrayList<>();
        choices.add(new TrackTerm(1,"General","General",0));
        int id=2;
        for (String name:List.of("CPE","CSSE","ECE")) for (AcademicTerm term:AcademicTerm.values())
            choices.add(new TrackTerm(id++,name+", "+term,name,term.number()));
        List<String> previous=null;
        int trackTables=0;
        for (JTable table:tables) {
            if (table.getColumnCount()!=4 || !table.getColumnName(3).equals("Track")) continue;
            trackTables++;
            Container grid=SwingUtilities.getAncestorOfClass(JPanel.class,table);
            // The first JPanel ancestor is the enclosing EntityGrid (JViewport is not a JPanel).
            Method install=grid.getClass().getDeclaredMethod("installTrackChoices",List.class);
            install.setAccessible(true);install.invoke(grid,choices);
            Method display=grid.getClass().getDeclaredMethod("display",List.class);
            display.setAccessible(true);
            boolean course=table.getColumnName(1).equals("Course Code");
            display.invoke(grid,List.of(new Row(2,List.of(course?"C2":"B2",course?"Title B":"30","ECE, 1st term")),
                    new Row(1,List.of(course?"C1":"B1",course?"Title A":"25","CSSE, 1st term"))));
            check(table.getModel().getValueAt(2,3)==null,"blank rows keep empty tracks");
            DefaultCellEditor editor=(DefaultCellEditor)table.getColumnModel().getColumn(3).getCellEditor();
            JComboBox<?> box=(JComboBox<?>)editor.getComponent();
            check(!box.isEditable(),"track selection cannot accept arbitrary typing");
            List<String> labels=new ArrayList<>();
            for(int i=0;i<box.getItemCount();i++) labels.add(box.getItemAt(i).toString());
            check(labels.contains("CPE, 3rd term")&&labels.contains("CSSE, 1st term")
                    &&labels.contains("ECE, 2nd term"),"seed tracks and terms offered");
            if (previous!=null) check(previous.equals(labels),"both tabs have identical choices");
            previous=labels;
            table.getRowSorter().toggleSortOrder(0);
            check(table.editCellAt(table.convertRowIndexToView(1),3),"sorted row enters combo editor");
            box.setSelectedItem("CSSE, 2nd term");
            BackgroundTask.finishEdit(table);
            DefaultTableModel model=(DefaultTableModel)table.getModel();
            check(model.getValueAt(1,3).equals("CSSE, 2nd term"),"selection commits to correct sorted model row");
            check(model.getValueAt(0,3).equals("ECE, 1st term"),"other row unchanged");
        }
        check(trackTables==2,"only courses and sections use the track editor");
        Course c=new Course(1,"C","Title",1,"CSSE, 1st term");
        check(!TrackChoices.compatible(c,new BlockSection(1,"B",20,"CSSE, 2nd term")),"scheduler choices reject other terms");
        check(!TrackChoices.compatible(c,new BlockSection(1,"B",20,"General")),"scheduler choices reject wildcard sections");
        check(TrackChoices.compatible(c,new BlockSection(1,"B",20,"CSSE, 1st term")),"scheduler choices accept same track and term");
    }

    private static void collect(Container parent,List<JTable> tables) {
        for(Component child:parent.getComponents()) {
            if(child instanceof JTable table) tables.add(table);
            else if(child instanceof Container nested) collect(nested,tables);
        }
    }
    private static void check(boolean ok,String message) {
        if(!ok) throw new AssertionError(message);
        checks++;
    }
}
