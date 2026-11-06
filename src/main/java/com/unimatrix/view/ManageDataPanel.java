package com.unimatrix.view;

import com.unimatrix.dao.EntityDataDAO;
import com.unimatrix.dao.EntityDataDAO.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/** Existing and new records share the same grid. Each tab saves as one transaction. */
public class ManageDataPanel extends JPanel {
    public ManageDataPanel() {
        super(new BorderLayout());setOpaque(false);
        JTabbedPane tabs=new JTabbedPane();
        for(Kind kind:Kind.values()) tabs.addTab(kind.label,new EntityGrid(kind));
        add(tabs);
    }
    private static final class EntityGrid extends JPanel {
        final Kind kind; final EntityDataDAO dao=new EntityDataDAO(); final DefaultTableModel model;
        final JTable table; final Set<Integer> invalid=new HashSet<>();
        List<Row> original=List.of(); boolean dirty,loading,loaded;
        final JLabel status=new JLabel("Loading records...");
        EntityGrid(Kind kind) {
            super(new BorderLayout(0,8));this.kind=kind;setOpaque(false);
            String[] labels=new String[kind.labels.length+1];labels[0]="ID";System.arraycopy(kind.labels,0,labels,1,kind.labels.length);
            model=new DefaultTableModel(labels,0) { public boolean isCellEditable(int r,int c){return c>0;} };
            table=new JTable(model) {
                public Component prepareRenderer(TableCellRenderer r,int row,int column) {
                    Component c=super.prepareRenderer(r,row,column);
                    if(!isRowSelected(row)) c.setBackground(invalid.contains(convertRowIndexToModel(row))?new Color(255,225,225):Color.WHITE);
                    return c;
                }
            };
            table.putClientProperty("terminateEditOnFocusLost",true);table.setRowHeight(30);table.setAutoCreateRowSorter(true);
            model.addTableModelListener(e->{if(!loading){dirty=true;putClientProperty("dirty",true);invalid.clear();status.setText("Unsaved changes");}});
            JButton add=new JButton("+"),delete=new JButton("Delete Record"),save=new JButton("Save Changes"),reload=new JButton("Reload");
            add.setToolTipText("Add a blank record");
            add.addActionListener(e->{if(BackgroundTask.finishEdit(table)) model.addRow(new Object[model.getColumnCount()]);});
            delete.addActionListener(e->{
                if(!BackgroundTask.finishEdit(table))return;
                int[] rows=java.util.Arrays.stream(table.getSelectedRows()).map(table::convertRowIndexToModel).sorted().toArray();
                for(int i=rows.length-1;i>=0;i--)model.removeRow(rows[i]);
            });
            save.addActionListener(e->save());
            reload.addActionListener(e->{if(!dirty||JOptionPane.showConfirmDialog(this,"Discard unsaved changes and reload?","Reload",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)reload();});
            JPanel actions=BackgroundTask.bar();actions.add(add);actions.add(delete);actions.add(save);actions.add(reload);actions.add(status);
            add(new JScrollPane(table),BorderLayout.CENTER);add(actions,BorderLayout.SOUTH);
            SwingUtilities.invokeLater(this::reload);
        }
        void reload(){BackgroundTask.run(this,()->dao.load(kind),this::display);}
        void display(List<Row> rows) {
            loading=true;original=rows;model.setRowCount(0);
            for(Row r:rows){Object[] cells=new Object[model.getColumnCount()];cells[0]=r.id();for(int i=0;i<r.values().size();i++)cells[i+1]=r.values().get(i);model.addRow(cells);}
            for(int i=0;i<2;i++)model.addRow(new Object[model.getColumnCount()]);
            loading=false;dirty=false;putClientProperty("dirty",false);loaded=true;invalid.clear();status.setText(rows.size()+" records. Blank rows are ignored.");
        }
        void save() {
            if(!loaded){BackgroundTask.error(this,new IllegalArgumentException("Load records successfully before saving."));return;}
            if(!BackgroundTask.finishEdit(table))return;
            List<Row> rows=new ArrayList<>();List<String> errors=new ArrayList<>();invalid.clear();
            for(int r=0;r<model.getRowCount();r++) {
                List<String> values=new ArrayList<>();for(int c=1;c<model.getColumnCount();c++) values.add(java.util.Objects.toString(model.getValueAt(r,c),"").trim());
                Object id=model.getValueAt(r,0);
                if(id==null&&values.stream().allMatch(String::isBlank))continue;
                try{rows.add(EntityDataDAO.validated(kind,new Row(id==null?0:((Number)id).intValue(),values)));}
                catch(IllegalArgumentException ex){invalid.add(r);errors.add("Row "+(r+1)+": "+ex.getMessage());}
            }
            table.repaint();
            if(!errors.isEmpty()){BackgroundTask.error(this,new IllegalArgumentException(String.join("\n",errors)));return;}
            List<Row> before=List.copyOf(original);
            BackgroundTask.run(this,()->dao.save(kind,before,rows),saved->{display(saved);status.setText("All changes saved.");});
        }
    }
}
