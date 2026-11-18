package com.unimatrix.view;

import com.unimatrix.view.components.UnderlineTabs;
import com.unimatrix.view.components.Card;
import com.unimatrix.view.theme.UniMatrixTheme;
import com.unimatrix.dao.EntityDataDAO;
import com.unimatrix.dao.DatabaseConnection;
import com.unimatrix.dao.TrackTermDAO;
import com.unimatrix.model.TrackTerm;
import com.unimatrix.dao.EntityDataDAO.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayDeque;
import java.util.Deque;

/** Existing and new records share the same grid. Each tab saves as one transaction. */
public class ManageDataPanel extends JPanel {
    public ManageDataPanel() { this(true); }
    ManageDataPanel(boolean loadData) {
        super(new BorderLayout());setOpaque(false);ToolTipManager.sharedInstance().setInitialDelay(700);ToolTipManager.sharedInstance().setDismissDelay(5000);
        UnderlineTabs tabs=new UnderlineTabs();
        for(Kind kind:new Kind[]{Kind.ROOM,Kind.BLOCK,Kind.COURSE,Kind.FACULTY}) {
            Card card=new Card(new BorderLayout());
            card.add(new EntityGrid(kind,loadData));
            tabs.addTab(kind.label,card);
        }
        add(tabs);
    }
    private static final class EntityGrid extends JPanel {
        final Kind kind; final EntityDataDAO dao=new EntityDataDAO(); final DefaultTableModel model;
        final JTable table; final Set<Integer> invalid=new HashSet<>();
        List<Row> original=List.of(),lastSnapshot=List.of(); final Deque<List<Row>> undoHistory=new ArrayDeque<>(); boolean dirty,loading,loaded,restoring;
        final JLabel status=new JLabel("Loading records..."); final JButton undo=new JButton("Undo Changes");
        EntityGrid(Kind kind, boolean loadData) {
            super(new BorderLayout(0,8));this.kind=kind;setOpaque(false);
            String[] labels=new String[kind.labels.length+1];labels[0]="ID";System.arraycopy(kind.labels,0,labels,1,kind.labels.length);
            model=new DefaultTableModel(labels,0) { public boolean isCellEditable(int r,int c){return c>0;} };
            table=new JTable(model) {
                public Component prepareRenderer(TableCellRenderer r,int row,int column) {
                    Component c=super.prepareRenderer(r,row,column);
                    if(!isRowSelected(row)) c.setBackground(invalid.contains(convertRowIndexToModel(row))?UniMatrixTheme.DANGER_SOFT:PlannerControls.stripe(row));
                    return c;
                }
            };
            table.putClientProperty("terminateEditOnFocusLost",true);table.setRowHeight(32);
            table.setFont(UniMatrixTheme.font(Font.PLAIN,13f));
            table.getTableHeader().setFont(UniMatrixTheme.font(Font.BOLD,13f));
            table.getTableHeader().setBackground(UniMatrixTheme.TABLE_HEADER);
            table.setGridColor(UniMatrixTheme.BORDER);table.setShowGrid(true);
            TableRowSorter<DefaultTableModel> sorter=new TableRowSorter<>(model);
            sorter.setComparator(0,(left,right)->{
                if(left==null||right==null)return left==null?(right==null?0:1):-1;
                return Integer.compare(((Number)left).intValue(),((Number)right).intValue());
            });
            table.setRowSorter(sorter);
            JButton add=new JButton("+"),delete=new JButton("Delete Record"),save=UniMatrixTheme.primaryButton("Save Changes"),reload=new JButton("Reload");
        UniMatrixTheme.style(delete,"foreground:"+UniMatrixTheme.hex(UniMatrixTheme.DANGER)+"; hoverBackground:"+UniMatrixTheme.hex(UniMatrixTheme.DANGER_SOFT));save.setToolTipText("Save Changes");reload.setToolTipText("Reload");
            add.setToolTipText("Add");delete.setToolTipText("Delete Record");undo.setToolTipText("Undo unsaved changes in this tab");undo.setEnabled(false);
            model.addTableModelListener(e->{if(!loading&&!restoring){List<Row> now=currentRows();if(!now.equals(lastSnapshot)&&!lastSnapshot.isEmpty())undoHistory.push(lastSnapshot);lastSnapshot=now;dirty=true;putClientProperty("dirty",true);undo.setEnabled(!undoHistory.isEmpty());invalid.clear();status.setText("Unsaved changes");}});
            add.addActionListener(e->{if(BackgroundTask.finishEdit(table)) model.addRow(new Object[model.getColumnCount()]);});
            delete.addActionListener(e->{
                if(!BackgroundTask.finishEdit(table))return;
                int[] rows=java.util.Arrays.stream(table.getSelectedRows()).map(table::convertRowIndexToModel).sorted().toArray();
                for(int i=rows.length-1;i>=0;i--)model.removeRow(rows[i]);
            });
            save.addActionListener(e->save());
            reload.addActionListener(e->{if(!dirty||JOptionPane.showConfirmDialog(this,"Refresh database data while keeping local deletions?","Reload",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)reload();});undo.addActionListener(e->{if(!undoHistory.isEmpty()){restoring=true;displayRows(undoHistory.pop(),original);restoring=false;undo.setEnabled(!undoHistory.isEmpty());}});
            JPanel actions=BackgroundTask.bar();actions.add(add);actions.add(delete);actions.add(save);actions.add(reload);actions.add(undo);actions.add(status);
            JPanel searchBar=BackgroundTask.bar();JTextField search=new JTextField(22);JButton find=new JButton("Search"),clear=new JButton("Clear Search");find.setToolTipText("Search");clear.setToolTipText("Clear Search");searchBar.add(search);searchBar.add(find);searchBar.add(clear);
            Runnable filter=()->{if(BackgroundTask.finishEdit(table))((TableRowSorter<?>)table.getRowSorter()).setRowFilter(search.getText().isBlank()?null:RowFilter.orFilter(java.util.List.of(RowFilter.regexFilter("(?i)"+java.util.regex.Pattern.quote(search.getText().trim())),new RowFilter<Object,Integer>(){public boolean include(Entry<? extends Object,? extends Integer> entry){for(int c=0;c<entry.getValueCount();c++)if(!entry.getStringValue(c).isBlank())return false;return true;}})));};find.addActionListener(e->filter.run());search.addActionListener(e->filter.run());clear.addActionListener(e->{search.setText("");filter.run();});add(searchBar,BorderLayout.NORTH);
            add(new JScrollPane(table),BorderLayout.CENTER);add(actions,BorderLayout.SOUTH);
            if (loadData) SwingUtilities.invokeLater(this::reload);
        }
        private record GridData(List<Row> rows, List<TrackTerm> tracks) { }
        void reload() {
            if (!BackgroundTask.finishEdit(table)) return;
            List<Row> current=currentRows();
            Set<Integer> locallyDeleted=new HashSet<>(original.stream().map(Row::id).toList());
            locallyDeleted.removeAll(current.stream().map(Row::id).toList());
            BackgroundTask.run(this,()->{
                try (var c=DatabaseConnection.getConnection()) {
                    return new GridData(dao.load(c,kind),
                            kind==Kind.BLOCK||kind==Kind.COURSE ? new TrackTermDAO().load(c) : List.of());
                }
            },data->{
                installTrackChoices(data.tracks());
                List<Row> visible=data.rows().stream().filter(r->!locallyDeleted.contains(r.id())).toList();
                displayRows(visible,data.rows());
            });
        }
        void installTrackChoices(List<TrackTerm> tracks) {
            if (kind!=Kind.BLOCK && kind!=Kind.COURSE) return;
            JComboBox<String> choices=new JComboBox<>();
            choices.addItem(""); // Keep unused rows empty until the user chooses an option.
            tracks.forEach(track->choices.addItem(track.label()));
            choices.setEditable(false);
            choices.setMaximumRowCount(14);
            choices.setToolTipText("Choose a track and term shared by courses and block sections.");
            DefaultCellEditor editor=new DefaultCellEditor(choices);
            editor.setClickCountToStart(1);
            table.getColumnModel().getColumn(3).setCellEditor(editor);
            table.getColumnModel().getColumn(3).setPreferredWidth(200);
        }
        void display(List<Row> rows) { displayRows(rows, rows); }
        void displayRows(List<Row> rows, List<Row> baseline) {
            loading=true;original=List.copyOf(baseline);model.setRowCount(0);
            for(Row r:rows){Object[] cells=new Object[model.getColumnCount()];cells[0]=r.id();for(int i=0;i<r.values().size();i++)cells[i+1]=r.values().get(i);model.addRow(cells);}
            for(int i=0;i<2;i++)model.addRow(new Object[model.getColumnCount()]);
            loading=false;dirty=false;putClientProperty("dirty",false);lastSnapshot=currentRows();undo.setEnabled(!undoHistory.isEmpty());loaded=true;invalid.clear();status.setText(rows.size()+" records. Blank rows are ignored.");
        }
        List<Row> currentRows() {
            List<Row> rows=new ArrayList<>();
            for(int r=0;r<model.getRowCount();r++){List<String> values=new ArrayList<>();boolean blank=true;for(int c=1;c<model.getColumnCount();c++){String value=java.util.Objects.toString(model.getValueAt(r,c),"").trim();values.add(value);if(!value.isBlank())blank=false;}if(blank)continue;Object id=model.getValueAt(r,0);rows.add(new Row(id instanceof Number?((Number)id).intValue():0,values));}
            return rows;
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
            BackgroundTask.run(this,()->dao.save(kind,before,rows),saved->{display(saved);status.setText("All changes saved. Undo is available.");});
        }
    }
}