package com.unimatrix.view;

import com.unimatrix.model.*;
import com.unimatrix.service.*;
import javax.swing.*;
import java.awt.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/** Retains entity selection and automatic generation, with user-defined daily bounds. */
final class AutoGeneratePanel extends JPanel {
    private final ScheduleEditorService service=new ScheduleEditorService();
    private final JTextField name=new JTextField(18),year=new JTextField(10),semester=new JTextField(10),date=new JTextField(LocalDate.now().plusDays(7).toString(),10),start=new JTextField("08:00",6),end=new JTextField("18:30",6);
    private final JComboBox<String> period=new JComboBox<>(new String[]{"Prelims","Midterms","Finals","Other"});
    private final JList<Course> courses=new JList<>();private final JList<Room> rooms=new JList<>();private final JList<BlockSection> blocks=new JList<>();private final JList<Faculty> faculty=new JList<>();
    private final List<JCheckBox> days=new ArrayList<>();private final JLabel status=new JLabel("Select entries; Ctrl/Shift selects multiple.");
    AutoGeneratePanel(Consumer<Integer> onSaved) {
        super(new BorderLayout(0,8));setOpaque(false);
        JPanel top=new JPanel();top.setLayout(new BoxLayout(top,BoxLayout.Y_AXIS));top.setOpaque(false);
        JPanel metadata=new JPanel(new GridLayout(2,2,12,6));metadata.add(labeled("Name",name));metadata.add(labeled("Period",period));metadata.add(labeled("Academic year",year));metadata.add(labeled("Semester",semester));top.add(metadata);
        JPanel times=new JPanel(new GridLayout(1,3,8,0));times.add(labeled("Start date",date));times.add(labeled("Daily start",start));times.add(labeled("Daily end",end));top.add(times);JPanel timing=BackgroundTask.bar();
        for(int i=1;i<=6;i++){JCheckBox box=new JCheckBox(DayOfWeek.of(i).toString().substring(0,3),true);days.add(box);timing.add(box);}top.add(timing);
        top.add(new JLabel("Assignments rotate through selected blocks/faculty. Review in Manual Encoding."));
        JTabbedPane entities=new JTabbedPane();entities.addTab("Courses",new JScrollPane(courses));entities.addTab("Rooms",new JScrollPane(rooms));entities.addTab("Block Sections",new JScrollPane(blocks));entities.addTab("Faculty",new JScrollPane(faculty));
        JPanel actions=BackgroundTask.bar();JButton reload=new JButton("Reload Choices"),generate=new JButton("Generate & Save");actions.add(reload);actions.add(generate);actions.add(status);
        reload.addActionListener(e->reload());
        generate.addActionListener(e->{try{
            Set<DayOfWeek> allowed=EnumSet.noneOf(DayOfWeek.class);for(int i=0;i<6;i++)if(days.get(i).isSelected())allowed.add(DayOfWeek.of(i+1));
            ScheduleRequest request=new ScheduleRequest(0,LocalDate.parse(date.getText().trim()),allowed,rooms.getSelectedValuesList(),courses.getSelectedValuesList(),blocks.getSelectedValuesList(),faculty.getSelectedValuesList());
            LocalTime dailyStart=ExamTimes.parse(start.getText()),dailyEnd=ExamTimes.parse(end.getText());
            String n=name.getText(),p=(String)period.getSelectedItem(),y=year.getText(),s=semester.getText();
            BackgroundTask.run(this,()->{
                SchedulingEngine.Plan plan=new SchedulingEngine().plan(request,dailyStart,dailyEnd);
                if(plan.exams().isEmpty())throw new IllegalArgumentException("No exams fit. "+String.join("\n",plan.skipped()));
                ScheduleDocument doc=service.save(new ScheduleDocument(0,null,n,p,y,s,0,plan.exams()),true);
                return new Object[]{doc,plan};
            },result->{ScheduleDocument doc=(ScheduleDocument)result[0];SchedulingEngine.Plan plan=(SchedulingEngine.Plan)result[1];status.setText(plan.exams().size()+" of "+plan.requested()+" exams saved.");onSaved.accept(doc.id());if(!plan.skipped().isEmpty())JOptionPane.showMessageDialog(this,String.join("\n",plan.skipped()),"Unscheduled exams",JOptionPane.WARNING_MESSAGE);});
        }catch(RuntimeException ex){BackgroundTask.error(this,ex);}});
        for(Component child:top.getComponents())if(child instanceof JComponent component)component.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(top,BorderLayout.NORTH);add(entities,BorderLayout.CENTER);add(actions,BorderLayout.SOUTH);SwingUtilities.invokeLater(this::reload);
    }
    private static JPanel labeled(String text,JComponent input){JPanel p=new JPanel(new BorderLayout(4,4));p.add(new JLabel(text+":"),BorderLayout.NORTH);p.add(input);return p;}
    private static void field(JPanel panel,String text,JComponent component){panel.add(new JLabel(text+":"));panel.add(component);}
    private void reload(){BackgroundTask.run(this,service::catalog,c->{populate(courses,c.courses());populate(rooms,c.rooms());populate(blocks,c.blocks());populate(faculty,c.faculty());});}
    private static <T> void populate(JList<T> list,List<T> values){DefaultListModel<T> model=new DefaultListModel<>();values.forEach(model::addElement);list.setModel(model);if(!values.isEmpty())list.setSelectionInterval(0,values.size()-1);}
}
