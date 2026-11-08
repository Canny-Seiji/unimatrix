package com.unimatrix.view;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.time.*;
import java.time.format.*;
import java.util.Locale;

final class PlannerControls {
    private PlannerControls() { }
    static JComboBox<String> times(){JComboBox<String> box=new JComboBox<>();box.addItem("");for(int minute=420;minute<=1260;minute+=30)box.addItem(LocalTime.of(minute/60,minute%60).format(DateTimeFormatter.ofPattern("h:mm a",Locale.ENGLISH)));return box;}
    static DefaultCellEditor timeEditor(){
        JComboBox<String> box=times();
        return new DefaultCellEditor(box){
            @Override public Component getTableCellEditorComponent(JTable table,Object value,boolean selected,int row,int column){
                if(value!=null&&!value.toString().isBlank()){
                    boolean found=false;for(int i=0;i<box.getItemCount();i++)found|=box.getItemAt(i).equals(value.toString());
                    if(!found)box.addItem(value.toString()); // Keep previously saved off-grid times editable without erasing them.
                }
                return super.getTableCellEditorComponent(table,value,selected,row,column);
            }
        };
    }
    static Color stripe(int row){return row%2==0?new Color(248,248,248):new Color(229,231,234);}
    static void stripes(JTable table){table.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){public Component getTableCellRendererComponent(JTable t,Object v,boolean selected,boolean focus,int row,int col){Component c=super.getTableCellRendererComponent(t,v,selected,focus,row,col);if(!selected)c.setBackground(stripe(row));return c;}});table.setRowHeight(32);table.setShowHorizontalLines(true);table.setGridColor(new Color(205,208,213));}
    static void calendar(JTable table,int column){table.getColumnModel().getColumn(column).setCellEditor(new CalendarEditor());}
    static final class CalendarEditor extends AbstractCellEditor implements TableCellEditor {
        final JButton button=new JButton("Choose date");String value="";
        CalendarEditor(){button.addActionListener(e->popup());}
        public Object getCellEditorValue(){return value;}
        public Component getTableCellEditorComponent(JTable t,Object v,boolean selected,int row,int col){value=v==null?"":v.toString();button.setText(value.isEmpty()?"Choose date":value);SwingUtilities.invokeLater(this::popup);return button;}
        void popup(){if(!button.isShowing())return;LocalDate date;try{date=LocalDate.parse(value);}catch(Exception ex){date=LocalDate.now();}JPopupMenu popup=new JPopupMenu();JPanel panel=new JPanel(new BorderLayout());YearMonth[] month={YearMonth.from(date)};JPanel days=new JPanel(new GridLayout(0,7,2,2));JLabel title=new JLabel("",SwingConstants.CENTER);JPanel nav=new JPanel(new BorderLayout());JButton prev=new JButton("<"),next=new JButton(">");nav.add(prev,BorderLayout.WEST);nav.add(title);nav.add(next,BorderLayout.EAST);panel.add(nav,BorderLayout.NORTH);panel.add(days);panel.setPreferredSize(new Dimension(350,260));popup.add(panel);
            Runnable render=()->{days.removeAll();title.setText(month[0].format(DateTimeFormatter.ofPattern("MMMM uuuu",Locale.ENGLISH)));for(String label:new String[]{"Mon","Tue","Wed","Thu","Fri","Sat","Sun"})days.add(new JLabel(label,SwingConstants.CENTER));for(int i=1;i<month[0].atDay(1).getDayOfWeek().getValue();i++)days.add(new JLabel());for(int d=1;d<=month[0].lengthOfMonth();d++){LocalDate chosen=month[0].atDay(d);JButton pick=new JButton(Integer.toString(d));pick.setMargin(new Insets(2,1,2,1));pick.setHorizontalAlignment(SwingConstants.CENTER);pick.setEnabled(chosen.getDayOfWeek()!=DayOfWeek.SUNDAY);pick.addActionListener(e->{value=chosen.toString();popup.setVisible(false);fireEditingStopped();});days.add(pick);}days.revalidate();days.repaint();};prev.addActionListener(e->{month[0]=month[0].minusMonths(1);render.run();});next.addActionListener(e->{month[0]=month[0].plusMonths(1);render.run();});render.run();popup.show(button,0,button.getHeight());
        }
    }
}
