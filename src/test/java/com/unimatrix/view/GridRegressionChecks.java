package com.unimatrix.view;

import com.unimatrix.model.*;
import com.unimatrix.service.*;
import com.unimatrix.service.ScheduleEditorService.Catalog;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.lang.reflect.*;
import java.util.List;

public final class GridRegressionChecks {
    public static void main(String[] args)throws Exception {
        SwingUtilities.invokeAndWait(()->{try{run();}catch(Exception ex){throw new RuntimeException(ex);}});
        System.out.println("Grid regression checks passed: 10");
    }
    private static void run()throws Exception {
        ManualSchedulePanel panel=new ManualSchedulePanel(id->{},false);
        DefaultTableModel model=(DefaultTableModel)field(panel,"model");JTable table=(JTable)field(panel,"table");
        check(model.getRowCount()==2,"two initial blank rows");
        var catalog=new ScheduleEditorService.Catalog(List.of(new Course(1,"CS1","OOP",1.5f),new Course(2,"CS2","SQL",1.5f)),List.of(new Room(1,"R1","A",50)),List.of(new BlockSection(1,"BSCS 1A",40)),List.of(new Faculty(1,"Faculty","Dept")));
        Method setCatalog=ManualSchedulePanel.class.getDeclaredMethod("setCatalog",ScheduleEditorService.Catalog.class);setCatalog.setAccessible(true);setCatalog.invoke(panel,catalog);
        row(model,0,1,"08:00");row(model,1,2,"09:30");model.addRow(new Object[7]);
        check(model.getValueAt(0,6).equals("09:30"),"derived end displayed");check(!model.isCellEditable(0,6),"end read-only");
        check(panel.draft().exams().size()==2,"blank row ignored");
        ((JTextField)field(panel,"search")).setText("CS1");Method apply=ManualSchedulePanel.class.getDeclaredMethod("applyFilter");apply.setAccessible(true);apply.invoke(panel);
        check(table.getRowCount()==2,"search shows matching row plus blank");check(panel.draft().exams().size()==2,"save includes hidden row");
        table.setRowSelectionInterval(0,0);button(panel,"Delete Record").doClick();
        check(model.getRowCount()==2&&panel.draft().exams().get(0).getCourseId()==2,"filtered deletion targets correct model row");
        button(panel,"+").doClick();check(model.getRowCount()==3,"plus adds row");
        model.setValueAt("2026-11-07",1,4);
        try{panel.draft();throw new AssertionError("partial row accepted");}catch(IllegalArgumentException expected){ }
        check(table.getRowCount()==3,"invalid hidden rows revealed");
        model.setValueAt(null,1,4);((TableRowSorter<?>)table.getRowSorter()).toggleSortOrder(0);table.setRowSelectionInterval(0,1);button(panel,"Delete Record").doClick();check(model.getRowCount()==1,"multiple sorted rows deleted");
    }
    private static void row(DefaultTableModel model,int row,int course,String start){Object[] values={new ManualSchedulePanel.Choice(course,"CS"+course),new ManualSchedulePanel.Choice(1,"R1"),new ManualSchedulePanel.Choice(1,"BSCS 1A"),new ManualSchedulePanel.Choice(1,"Faculty"),"2026-11-07",start};for(int i=0;i<6;i++)model.setValueAt(values[i],row,i);}
    private static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    private static JButton button(Container p,String text){for(Component c:p.getComponents()){if(c instanceof JButton b&&b.getText().equals(text))return b;if(c instanceof Container nested){JButton b=button(nested,text);if(b!=null)return b;}}return null;}
    private static void check(boolean result,String name){if(!result)throw new AssertionError(name);}
}
