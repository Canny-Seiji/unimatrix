package com.unimatrix.view;
import com.unimatrix.dao.*;
import com.unimatrix.model.*;
import com.unimatrix.service.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/** Each row is an explicit exam requirement. All rows must fit before a draft is saved. */
final class AutoGeneratePanel extends JPanel {
    final JTextField name=new JTextField(18),year=new JTextField(10),semester=new JTextField(10);
    final JComboBox<String> period=new JComboBox<>(new String[]{"Prelims","Midterms","Finals","Other"}),start=PlannerControls.times(),end=PlannerControls.times();
    final DefaultTableModel dates=new DefaultTableModel(new String[]{"First exam date","Last exam date"},1);
    final DefaultTableModel requirements=new DefaultTableModel(new String[]{"Course","Block Section","Faculty","Exam length (minutes)"},2);
    final JTable grid=new JTable(requirements),dateGrid=new JTable(dates);final JLabel destination=new JLabel("Destination: workspace root");
    Integer folderId;ScheduleEditorService.Catalog catalog;final ScheduleEditorService service=new ScheduleEditorService();
    AutoGeneratePanel(Consumer<Integer> saved){super(new BorderLayout(0,8));
        JPanel top=new JPanel(new GridLayout(0,2,8,4));top.add(new JLabel("Name"));top.add(name);top.add(new JLabel("Examination period"));top.add(period);top.add(new JLabel("Academic year"));top.add(year);top.add(new JLabel("Semester"));top.add(semester);top.add(new JLabel("Daily start / end"));JPanel times=BackgroundTask.bar();times.add(start);times.add(end);top.add(times);start.setSelectedItem("7:00 AM");end.setSelectedItem("9:00 PM");
        PlannerControls.calendar(dateGrid,0);PlannerControls.calendar(dateGrid,1);dateGrid.setRowHeight(32);JScrollPane dateScroll=new JScrollPane(dateGrid);dateScroll.setColumnHeaderView(dateGrid.getTableHeader());dateScroll.setPreferredSize(new Dimension(500,58));top.add(new JLabel("Examination period dates"));top.add(dateScroll);top.add(destination);top.add(new JLabel("One row for each course/block exam; no rotation."));Component[] fields=top.getComponents();top.removeAll();top.setLayout(new GridBagLayout());
        for(int i=0;i<fields.length;i++){GridBagConstraints gc=new GridBagConstraints();gc.gridx=i%2;gc.gridy=i/2;gc.fill=GridBagConstraints.HORIZONTAL;gc.weightx=i%2==0?0.2:0.8;gc.insets=new Insets(3,5,3,5);top.add(fields[i],gc);}add(top,BorderLayout.NORTH);
        PlannerControls.stripes(grid);grid.putClientProperty("terminateEditOnFocusLost",true);JComboBox<Integer> lengths=new JComboBox<>();for(int n=30;n<=840;n+=30)lengths.addItem(n);grid.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(lengths));add(new JScrollPane(grid));
        JPanel bar=BackgroundTask.bar();JButton add=new JButton("+"),delete=new JButton("Delete Record"),reload=new JButton("Reload Choices"),generate=new JButton("Generate Draft");bar.add(add);bar.add(delete);bar.add(reload);bar.add(generate);add(bar,BorderLayout.SOUTH);
        add.addActionListener(e->{if(BackgroundTask.finishEdit(grid))requirements.addRow(new Object[4]);});delete.addActionListener(e->{if(!BackgroundTask.finishEdit(grid))return;int[] rows=grid.getSelectedRows();for(int i=rows.length-1;i>=0;i--)requirements.removeRow(rows[i]);});reload.addActionListener(e->reload());generate.addActionListener(e->generate(saved));SwingUtilities.invokeLater(this::reload);
    }
    void setDestination(Integer id,String label){folderId=id;destination.setText("Destination: "+label);reload();}
    void reload(){BackgroundTask.run(this,service::catalog,c->{catalog=c;JComboBox<Course> courses=new JComboBox<>();courses.addItem(null);c.courses().forEach(courses::addItem);JComboBox<BlockSection> blocks=new JComboBox<>();blocks.addItem(null);c.blocks().forEach(blocks::addItem);JComboBox<Faculty> faculty=new JComboBox<>();faculty.addItem(null);c.faculty().forEach(faculty::addItem);grid.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(courses));grid.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(blocks));grid.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(faculty));});}
    void generate(Consumer<Integer> saved){try{if(!BackgroundTask.finishEdit(grid)||!BackgroundTask.finishEdit(dateGrid))return;if(catalog==null)throw new IllegalArgumentException("Load choices first.");
        List<ExplicitPlanner.Requirement> rows=new ArrayList<>();for(int r=0;r<requirements.getRowCount();r++){boolean blank=true;for(int col=0;col<4;col++)blank&=requirements.getValueAt(r,col)==null;if(blank)continue;
            if(!(requirements.getValueAt(r,0) instanceof Course course)||!(requirements.getValueAt(r,1) instanceof BlockSection block)||!(requirements.getValueAt(r,2) instanceof Faculty faculty)||requirements.getValueAt(r,3)==null)throw new IllegalArgumentException("Complete requirement row "+(r+1));
            rows.add(new ExplicitPlanner.Requirement(course.getId(),block.getId(),faculty.getId(),Integer.parseInt(requirements.getValueAt(r,3).toString())));}
        LocalDate first=LocalDate.parse(Objects.toString(dates.getValueAt(0,0),"")),last=LocalDate.parse(Objects.toString(dates.getValueAt(0,1),""));LocalTime from=ExamTimes.parse((String)start.getSelectedItem()),to=ExamTimes.parse((String)end.getSelectedItem());String n=name.getText(),p=(String)period.getSelectedItem(),y=year.getText(),s=semester.getText();Integer target=folderId;var cat=catalog;
        BackgroundTask.run(this,()->{PlanningRules.Rules rules;try(Connection c=DatabaseConnection.getConnection()){rules=PlanningRules.load(c,0);}List<ExamSchedule> exams=ExplicitPlanner.generate(rows,cat.rooms(),cat.blocks(),first,last,from,to,rules);return service.save(new ScheduleDocument(0,target,n,p,y,s,0,exams),true);},d->{saved.accept(d.id());JOptionPane.showMessageDialog(this,"All "+d.exams().size()+" required exams saved as a draft. Review, then Publish.");});
    }catch(RuntimeException ex){BackgroundTask.error(this,ex);}}
}