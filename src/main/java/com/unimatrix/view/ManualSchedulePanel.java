package com.unimatrix.view;

import com.unimatrix.dao.ScheduleFolderDAO;
import com.unimatrix.model.*;
import com.unimatrix.service.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/** Draft state is independent of the table's current sort/filter view. */
public class ManualSchedulePanel extends JPanel {
    record Choice(int id,String label) { public String toString(){return label;} }
    private final ScheduleEditorService service=new ScheduleEditorService();
    private final Consumer<Integer> onSaved;
    private final ScheduleFolderDAO folderDAO=new ScheduleFolderDAO();
    private ScheduleEditorService.Catalog catalog;
    private final JTextField name=new JTextField(18),year=new JTextField(10),semester=new JTextField(10),search=new JTextField(16);
    private final JComboBox<String> period=new JComboBox<>(new String[]{"Prelims","Midterms","Finals","Other"});
    private final List<JComboBox<Choice>> filters=new ArrayList<>();
    private final Set<Integer> invalid=new HashSet<>();
    private final JLabel status=new JLabel("New schedule"),location=new JLabel("Root workspace");
    private int scheduleId; private Integer folderId; private long revision; private boolean dirty,loading;
    private List<Integer> lastSourceRows=List.of();
    private final JButton save=new JButton("Save Schedule"),saveAs=new JButton("Save As New");
    private final DefaultTableModel model=new DefaultTableModel(new String[]{"Course","Room","Block Section","Faculty","Exam Date","Start Time","End Time"},0) {
        public boolean isCellEditable(int r,int c){return true;}
    };
    private final JTable table=new JTable(model) {
        public Component prepareRenderer(TableCellRenderer r,int row,int col) {
            Component c=super.prepareRenderer(r,row,col);
            if(!isRowSelected(row))c.setBackground(invalid.contains(convertRowIndexToModel(row))?new Color(255,225,225):PlannerControls.stripe(row));
            return c;
        }
    };
    private final TableRowSorter<DefaultTableModel> sorter=new TableRowSorter<>(model);
    public ManualSchedulePanel(Consumer<Integer> onSaved) { this(onSaved, true); }
    ManualSchedulePanel(Consumer<Integer> onSaved, boolean loadData) {
        super(new BorderLayout(0,8));this.onSaved=onSaved;setOpaque(false);
        JPanel top=new JPanel();top.setLayout(new BoxLayout(top,BoxLayout.Y_AXIS));top.setOpaque(false);
        JPanel meta=new JPanel(new GridLayout(2,2,12,6));meta.setOpaque(false);
        meta.add(labeled("Name",name));meta.add(labeled("Period",period));meta.add(labeled("Academic year",year));meta.add(labeled("Semester",semester));top.add(meta);
        JPanel destinationBar=BackgroundTask.bar();destinationBar.add(location);JButton changeDestination=new JButton("Change Destination");changeDestination.addActionListener(e->chooseDestination());destinationBar.add(changeDestination);top.add(destinationBar);
        JPanel filterBar=new JPanel(new GridLayout(1,4,8,0));filterBar.setOpaque(false);
        for(String label:List.of("Course","Room","Block","Faculty")) {JComboBox<Choice> box=new JComboBox<>();box.setPreferredSize(new Dimension(135,28));filters.add(box);filterBar.add(labeled(label,box));}
        JButton filter=new JButton("Filter"),find=new JButton("Search"),clear=new JButton("Clear Filters");
        filter.addActionListener(e->applyFilter());find.addActionListener(e->applyFilter());search.addActionListener(e->applyFilter());
        clear.addActionListener(e->clearFilters());top.add(filterBar);
        JPanel searchBar=BackgroundTask.bar();field(searchBar,"Search all columns",search);searchBar.add(filter);searchBar.add(find);searchBar.add(clear);top.add(searchBar);
        top.add(new JLabel("Date: YYYY-MM-DD. Start: HH:mm (24-hour) or h:mm AM/PM."));
        top.add(new JLabel("Choose start and end times. Monday-Saturday; 7 AM-9 PM."));
        PlannerControls.calendar(table,4);
        table.getColumnModel().getColumn(5).setCellEditor(PlannerControls.timeEditor());
        table.getColumnModel().getColumn(6).setCellEditor(PlannerControls.timeEditor());
        table.setRowHeight(32);table.setRowSorter(sorter);table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);table.putClientProperty("terminateEditOnFocusLost",true);
        int[] widths={240,190,160,200,130,120,120};for(int i=0;i<widths.length;i++)table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        JPanel bottom=BackgroundTask.bar();JButton add=new JButton("+"),delete=new JButton("Delete Record"),reload=new JButton("Refresh Choices");
        add.setToolTipText("Add a blank exam row");
        add.addActionListener(e->{if(BackgroundTask.finishEdit(table)){model.addRow(new Object[7]);int r=table.convertRowIndexToView(model.getRowCount()-1);if(r>=0)table.scrollRectToVisible(table.getCellRect(r,0,true));}});
        delete.addActionListener(e->{if(!BackgroundTask.finishEdit(table))return;int[] rows=Arrays.stream(table.getSelectedRows()).map(table::convertRowIndexToModel).sorted().toArray();for(int i=rows.length-1;i>=0;i--)model.removeRow(rows[i]);});
        reload.addActionListener(e->refreshChoices());save.addActionListener(e->save(false));saveAs.addActionListener(e->save(true));
        bottom.add(add);bottom.add(delete);bottom.add(reload);bottom.add(save);bottom.add(saveAs);bottom.add(status);
        for(Component child:top.getComponents())if(child instanceof JComponent component)component.setAlignmentX(Component.LEFT_ALIGNMENT);
        JScrollPane scroll=new JScrollPane(table);scroll.setColumnHeaderView(table.getTableHeader());
        add(top,BorderLayout.NORTH);add(scroll,BorderLayout.CENTER);add(bottom,BorderLayout.SOUTH);
        model.addTableModelListener(e->{if(!loading){dirty=true;putClientProperty("dirty",true);if(e.getFirstRow()>=0)for(int row=e.getFirstRow();row<=e.getLastRow();row++)invalid.remove(row);status.setText("Unsaved changes");table.repaint();}});
        DocumentListener listener=new DocumentListener(){public void insertUpdate(DocumentEvent e){changed();}public void removeUpdate(DocumentEvent e){changed();}public void changedUpdate(DocumentEvent e){changed();}};
        name.getDocument().addDocumentListener(listener);year.getDocument().addDocumentListener(listener);semester.getDocument().addDocumentListener(listener);
        period.addActionListener(e->changed());reset(null,"Root workspace");if (loadData) SwingUtilities.invokeLater(this::refreshChoices);
    }
    private void changed(){if(!loading){dirty=true;putClientProperty("dirty",true);status.setText("Unsaved changes");}}
    private void chooseDestination(){
        BackgroundTask.run(this,folderDAO::getAllFolders,folders->{
            List<Object> options=new ArrayList<>();options.add(new Destination(null,"Root workspace"));options.addAll(folders.stream().map(f->new Destination(f.getId(),f.getName())).toList());
            Destination selected=(Destination)JOptionPane.showInputDialog(this,"Save this schedule in:","Change Destination",JOptionPane.PLAIN_MESSAGE,null,options.toArray(),options.get(0));
            if(selected!=null){folderId=selected.id();location.setText("Destination: "+selected.label());changed();}
        });
    }
    private record Destination(Integer id,String label){public String toString(){return label;}}
    private static JPanel labeled(String label,JComponent input) {
        JPanel p=new JPanel(new BorderLayout(4,4));p.setOpaque(false);p.add(new JLabel(label+":"),BorderLayout.NORTH);p.add(input);return p;
    }
    private static void field(JPanel p,String label,JComponent input){p.add(new JLabel(label+":"));p.add(input);}
    public boolean mayReplaceDraft() {
        if(Boolean.TRUE.equals(getClientProperty("busy")))return false;
        return !dirty||JOptionPane.showConfirmDialog(this,"Discard unsaved schedule changes?","Open schedule",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;
    }
    public void newSchedule(Integer folder,String label) {if(mayReplaceDraft()){reset(folder,label);refreshChoices();}}
    private void reset(Integer folder,String label) {
        loading=true;scheduleId=0;revision=0;folderId=folder;name.setText("");year.setText("");semester.setText("");period.setSelectedItem("Midterms");location.setText(label);
        model.setRowCount(0);model.addRow(new Object[7]);model.addRow(new Object[7]);clearFilters();loading=false;dirty=false;putClientProperty("dirty",false);save.setText("Save Schedule");saveAs.setEnabled(false);status.setText("Two blank rows ready.");
    }
    public void open(int id) {
        if(!mayReplaceDraft())return;
        BackgroundTask.run(this,()->new Object[]{service.catalog(),service.load(id)},result->{setCatalog((ScheduleEditorService.Catalog)result[0]);display((ScheduleDocument)result[1]);});
    }
    private void refreshChoices(){BackgroundTask.run(this,service::catalog,this::setCatalog);}
    private void setCatalog(ScheduleEditorService.Catalog loaded) {
        catalog=loaded;
        List<List<Choice>> choices=List.of(loaded.courses().stream().map(x->new Choice(x.getId(),x.toString())).toList(),loaded.rooms().stream().map(x->new Choice(x.getId(),x.toString())).toList(),loaded.blocks().stream().map(x->new Choice(x.getId(),x.toString())).toList(),loaded.faculty().stream().map(x->new Choice(x.getId(),x.toString())).toList());
        loading=true;
        for(int col=0;col<4;col++) {
            if(col<1 || col==2) table.getColumnModel().getColumn(col).setCellEditor(new TrackEditor(col==0)); else {JComboBox<Choice> edit=new JComboBox<>();edit.addItem(null);choices.get(col).forEach(edit::addItem);table.getColumnModel().getColumn(col).setCellEditor(new DefaultCellEditor(edit));}
            Choice selected=(Choice)filters.get(col).getSelectedItem();filters.get(col).removeAllItems();filters.get(col).addItem(new Choice(0,"All"));choices.get(col).forEach(filters.get(col)::addItem);
            if(selected!=null)for(Choice choice:choices.get(col))if(choice.id()==selected.id())filters.get(col).setSelectedItem(choice);
            for(int r=0;r<model.getRowCount();r++)if(model.getValueAt(r,col) instanceof Choice old) {
                Choice replacement=choices.get(col).stream().filter(x->x.id()==old.id()).findFirst().orElse(new Choice(old.id(),"[Deleted] "+old.label()));
                model.setValueAt(replacement,r,col);
            }
        }
        loading=false;table.repaint();
    }
    private final class TrackEditor extends AbstractCellEditor implements TableCellEditor { final boolean course; final JComboBox<Choice> box=new JComboBox<>(); TrackEditor(boolean course){this.course=course;} public Object getCellEditorValue(){return box.getSelectedItem();} public Component getTableCellEditorComponent(JTable t,Object v,boolean s,int row,int col){box.removeAllItems();box.addItem(null);int r=t.convertRowIndexToModel(row);Course c=null;BlockSection b=null;if(model.getValueAt(r,0) instanceof Choice x)c=catalog.courses().stream().filter(y->y.getId()==x.id()).findFirst().orElse(null);if(model.getValueAt(r,2) instanceof Choice x)b=catalog.blocks().stream().filter(y->y.getId()==x.id()).findFirst().orElse(null);final Course selectedCourse=c;final BlockSection selectedBlock=b;if(course)catalog.courses().stream().filter(x->TrackChoices.compatible(x,selectedBlock)).map(x->new Choice(x.getId(),x.toString())).forEach(box::addItem);else catalog.blocks().stream().filter(x->TrackChoices.compatible(selectedCourse,x)).map(x->new Choice(x.getId(),x.toString())).forEach(box::addItem);box.setSelectedItem(v);return box;}}
    private Choice choice(int column,int id,String fallback) {
        JComboBox<Choice> box=filters.get(column);for(int i=1;i<box.getItemCount();i++)if(box.getItemAt(i).id()==id)return box.getItemAt(i);
        return id==0?null:new Choice(id,fallback==null?"[Missing #"+id+"]":fallback);
    }
    private void display(ScheduleDocument d) {
        loading=true;scheduleId=d.id();folderId=d.folderId();revision=d.revision();name.setText(d.name());period.setSelectedItem(d.period());year.setText(d.academicYear());semester.setText(d.semester());
        location.setText("Schedule #"+d.id());model.setRowCount(0);invalid.clear();
        for(ExamSchedule e:d.exams()) {
            String start="",end="";try{var times=ExamTimes.interval(e.getTimeSlot());start=times[0].format(java.time.format.DateTimeFormatter.ofPattern("h:mm a",java.util.Locale.ENGLISH));end=times[1].format(java.time.format.DateTimeFormatter.ofPattern("h:mm a",java.util.Locale.ENGLISH));}catch(RuntimeException ignored){ }
            int row=model.getRowCount();model.addRow(new Object[]{choice(0,e.getCourseId(),e.getCourseCode()),choice(1,e.getRoomId(),e.getRoomName()),choice(2,e.getBlockSectionId(),e.getBlockSection()),choice(3,e.getFacultyId(),e.getFacultyName()),e.getExamDate()==null?"":e.getExamDate().toString(),start,end});
            if(e.getRoomId()==0||e.getExamDate()==null||start.isBlank()||end.isBlank())invalid.add(row);
        }
        model.addRow(new Object[7]);model.addRow(new Object[7]);clearFilters();loading=false;dirty=false;putClientProperty("dirty",false);save.setText("Save Changes");saveAs.setEnabled(true);status.setText(d.exams().size()+" exams loaded.");
    }
    private boolean blank(int r) {for(int c=0;c<7;c++){Object v=model.getValueAt(r,c);if(v!=null&&!v.toString().isBlank())return false;}return true;}
    private void clearFilters(){search.setText("");for(var f:filters)if(f.getItemCount()>0)f.setSelectedIndex(0);sorter.setRowFilter(null);}
    private void applyFilter() {
        if(!BackgroundTask.finishEdit(table))return;
        String term=search.getText().trim().toLowerCase(Locale.ROOT);int[] ids=filters.stream().mapToInt(f->f.getSelectedItem() instanceof Choice ch?ch.id():0).toArray();
        sorter.setRowFilter(new RowFilter<>(){public boolean include(Entry<? extends DefaultTableModel,? extends Integer> e){
            int r=e.getIdentifier();if(blank(r))return true;
            for(int c=0;c<4;c++)if(ids[c]!=0&&(!(model.getValueAt(r,c) instanceof Choice ch)||ch.id()!=ids[c]))return false;
            for(int c=0;c<7;c++)if(e.getStringValue(c).toLowerCase(Locale.ROOT).contains(term))return true;
            return false;
        }});
    }
    ScheduleDocument draft() {
        if(catalog==null)throw new IllegalArgumentException("Load entity choices before saving.");
        if(!BackgroundTask.finishEdit(table))throw new IllegalArgumentException("Finish editing the current cell.");
        List<ExamSchedule> exams=new ArrayList<>();List<Integer> sourceRows=new ArrayList<>();List<String> errors=new ArrayList<>();invalid.clear();
        for(int r=0;r<model.getRowCount();r++) {
            if(blank(r))continue;
            try {
                Choice[] choices=new Choice[4];for(int c=0;c<4;c++){choices[c]=(Choice)model.getValueAt(r,c);if(choices[c]==null)throw new IllegalArgumentException("Select course, room, block and faculty.");}
                LocalDate date=LocalDate.parse(Objects.toString(model.getValueAt(r,4),"").trim());
                LocalTime start=ExamTimes.parse(Objects.toString(model.getValueAt(r,5),""));
                int courseId=choices[0].id();catalog.courses().stream().filter(x->x.getId()==courseId).findFirst().orElseThrow(()->new IllegalArgumentException("Course no longer exists."));
                exams.add(new ExamSchedule(0,courseId,choices[1].id(),choices[2].id(),choices[3].id(),date,ExamTimes.slot(start,ExamTimes.parse(Objects.toString(model.getValueAt(r,6),"")))));sourceRows.add(r);
            }catch(RuntimeException ex){invalid.add(r);errors.add("Row "+(r+1)+": "+ex.getMessage());}
        }
        if(errors.isEmpty())try{ScheduleEditorService.validate(exams,catalog);}catch(IllegalArgumentException ex){
            String message=ex.getMessage(); java.util.regex.Matcher match=java.util.regex.Pattern.compile("[Rr]ow (\\d+)").matcher(message); StringBuffer fixed=new StringBuffer();
            while(match.find()){int i=Integer.parseInt(match.group(1))-1;if(i>=0&&i<sourceRows.size()){invalid.add(sourceRows.get(i));match.appendReplacement(fixed,"Row "+(sourceRows.get(i)+1));}}
            match.appendTail(fixed);errors.add(fixed.length()==0?message:fixed.toString());
        }
        if(!errors.isEmpty()){sorter.setRowFilter(null);table.repaint();throw new IllegalArgumentException(String.join("\n",errors));}
        if(exams.isEmpty())throw new IllegalArgumentException("Add at least one completed exam row.");
        lastSourceRows=List.copyOf(sourceRows);
        return new ScheduleDocument(scheduleId,folderId,name.getText(),(String)period.getSelectedItem(),year.getText(),semester.getText(),revision,exams);
    }
    private String visibleRows(String message) {
        if(message==null||lastSourceRows.isEmpty()) return message;
        var match=java.util.regex.Pattern.compile("[Rr]ow (\\d+)").matcher(message); StringBuffer fixed=new StringBuffer();
        while(match.find()) { int i=Integer.parseInt(match.group(1))-1; if(i>=0&&i<lastSourceRows.size()) match.appendReplacement(fixed,"Row "+(lastSourceRows.get(i)+1)); }
        match.appendTail(fixed); return fixed.toString();
    }
    private void save(boolean asNew) {
        final ScheduleDocument d;
        try { d=draft(); } catch(RuntimeException ex) { BackgroundTask.error(this,ex);return; }
        BackgroundTask.run(this,()->service.save(d,asNew),saved->{display(saved);status.setText("Saved. Export PDF from the selected schedule in Explorer.");onSaved.accept(saved.id());},ex->{Throwable cause=ex;while(cause.getCause()!=null)cause=cause.getCause();JOptionPane.showMessageDialog(this,visibleRows(cause.getMessage()),"Operation failed",JOptionPane.ERROR_MESSAGE);});
    }
}
