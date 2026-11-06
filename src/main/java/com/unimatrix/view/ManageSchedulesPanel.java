package com.unimatrix.view;

import com.unimatrix.dao.*;
import com.unimatrix.model.*;
import com.unimatrix.service.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/** Explorer owns schedule selection; the manual tab owns its independent editable draft. */
public class ManageSchedulesPanel extends JPanel {
    private final ScheduleFolderDAO folders=new ScheduleFolderDAO();
    private final ExamScheduleDAO exams=new ExamScheduleDAO();
    private final ScheduleEditorService service=new ScheduleEditorService();
    private final JTabbedPane tabs=new JTabbedPane();
    private final JPanel explorer=new JPanel(new BorderLayout(0,8));
    private final DefaultMutableTreeNode root=new DefaultMutableTreeNode("Exam Workspaces");
    private final DefaultTreeModel treeModel=new DefaultTreeModel(root);
    private final JTree tree=new JTree(treeModel);
    private final DefaultTableModel tableModel=new DefaultTableModel(new String[]{"Schedule","Date","Time","Course","Room","Block Section","Faculty"},0){public boolean isCellEditable(int r,int c){return false;}};
    private final JTable table=new JTable(tableModel);
    private final JButton edit=new JButton("Open / Edit"),export=new JButton("Export PDF"),delete=new JButton("Delete");
    private final JLabel scope=new JLabel("Select a schedule to edit or export.");
    private final ManualSchedulePanel manual;
    private int loadSequence;
    public ManageSchedulesPanel() {
        super(new BorderLayout());setOpaque(false);explorer.setOpaque(false);
        manual=new ManualSchedulePanel(this::saved);
        JPanel actions=BackgroundTask.bar();JButton addFolder=new JButton("New Folder"),newSchedule=new JButton("New Schedule"),refresh=new JButton("Refresh");
        actions.add(addFolder);actions.add(newSchedule);actions.add(edit);actions.add(export);actions.add(delete);actions.add(refresh);
        addFolder.addActionListener(e->createFolder());newSchedule.addActionListener(e->{manual.newSchedule(selectedFolder(),selectedFolderLabel());tabs.setSelectedIndex(2);});
        refresh.addActionListener(e->refresh());edit.addActionListener(e->openSelected());export.addActionListener(e->exportSelected());delete.addActionListener(e->deleteSelected());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setRowHeight(26);tree.setShowsRootHandles(true);tree.addTreeSelectionListener(e->{updateButtons();loadRows();});
        tree.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){if(e.getClickCount()==2)openSelected();}});
        table.setRowHeight(30);table.setAutoCreateRowSorter(true);table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for(int i=0;i<table.getColumnCount();i++)table.getColumnModel().getColumn(i).setPreferredWidth(i==0?170:i==3?160:140);
        JPanel left=new JPanel(new BorderLayout());left.add(new JScrollPane(tree));left.setMinimumSize(new Dimension(180,100));
        JPanel right=new JPanel(new BorderLayout(0,8));right.add(scope,BorderLayout.NORTH);right.add(new JScrollPane(table));
        JPanel searchBar=BackgroundTask.bar();JTextField search=new JTextField(20);JButton searchBtn=new JButton("Search"),clear=new JButton("Clear");searchBar.add(new JLabel("Search displayed schedules:"));searchBar.add(search);searchBar.add(searchBtn);searchBar.add(clear);
        Runnable filter=()->((TableRowSorter<?>)table.getRowSorter()).setRowFilter(search.getText().isBlank()?null:RowFilter.regexFilter("(?i)"+java.util.regex.Pattern.quote(search.getText().trim())));
        searchBtn.addActionListener(e->filter.run());search.addActionListener(e->filter.run());clear.addActionListener(e->{search.setText("");filter.run();});right.add(searchBar,BorderLayout.SOUTH);
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,left,right);split.setDividerLocation(240);split.setResizeWeight(.2);
        explorer.add(actions,BorderLayout.NORTH);explorer.add(split,BorderLayout.CENTER);
        tabs.addTab("Schedules",explorer);tabs.addTab("Auto-Generate Config",new AutoGeneratePanel(this::saved));tabs.addTab("Manual Encoding",manual);add(tabs);updateButtons();SwingUtilities.invokeLater(this::refresh);
    }
    private record TreeData(List<ScheduleFolder> folders,List<ScheduleFile> files) { }
    public void refresh(){refresh(null);}
    private void refresh(Integer selectId) {
        Object selected=selectedObject();
        BackgroundTask.run(explorer,()->new TreeData(folders.getAllFolders(),folders.getAllFiles()),data->{
            DefaultMutableTreeNode replacement=new DefaultMutableTreeNode("Exam Workspaces");Map<Integer,DefaultMutableTreeNode> nodes=new HashMap<>();
            Map<Integer,Integer> parents=new HashMap<>();for(ScheduleFolder f:data.folders()){nodes.put(f.getId(),new DefaultMutableTreeNode(f));parents.put(f.getId(),f.getParentId());}
            for(ScheduleFolder f:data.folders()) {
                Set<Integer> visited=new HashSet<>();Integer id=f.getId();while(id!=null){if(!visited.add(id))throw new IllegalArgumentException("Folder hierarchy contains a cycle.");if(!parents.containsKey(id))throw new IllegalArgumentException("Folder hierarchy has a missing parent.");id=parents.get(id);}
                (f.getParentId()==null?replacement:nodes.get(f.getParentId())).add(nodes.get(f.getId()));
            }
            for(ScheduleFile f:data.files()) {
                DefaultMutableTreeNode parent=f.getFolderId()==null?replacement:nodes.get(f.getFolderId());if(parent==null)throw new IllegalArgumentException("Schedule has a missing folder.");parent.add(new DefaultMutableTreeNode(f,false));
            }
            root.removeAllChildren();while(replacement.getChildCount()>0)root.add((DefaultMutableTreeNode)replacement.getChildAt(0));treeModel.reload();
            DefaultMutableTreeNode target=root;
            for(var it=root.depthFirstEnumeration();it.hasMoreElements();) {
                DefaultMutableTreeNode node=(DefaultMutableTreeNode)it.nextElement();Object obj=node.getUserObject();
                if(selectId!=null&&obj instanceof ScheduleFile f&&f.getId()==selectId)target=node;
                else if(selectId==null&&sameIdentity(selected,obj))target=node;
            }
            tree.setSelectionPath(new TreePath(target.getPath()));tree.scrollPathToVisible(new TreePath(target.getPath()));tree.expandPath(new TreePath(root.getPath()));updateButtons();loadRows();
        });
    }
    private static boolean sameIdentity(Object a,Object b){return a instanceof ScheduleFile x&&b instanceof ScheduleFile y&&x.getId()==y.getId()||a instanceof ScheduleFolder folderA&&b instanceof ScheduleFolder folderB&&folderA.getId()==folderB.getId();}
    private Object selectedObject(){DefaultMutableTreeNode n=(DefaultMutableTreeNode)tree.getLastSelectedPathComponent();return n==null?null:n.getUserObject();}
    private Integer selectedFolder(){Object o=selectedObject();return o instanceof ScheduleFolder f?f.getId():o instanceof ScheduleFile f?f.getFolderId():null;}
    private String selectedFolderLabel(){Object o=selectedObject();return o instanceof ScheduleFolder f?f.getName():o instanceof ScheduleFile f?(f.getFolderId()==null?"Root workspace":"Folder #"+f.getFolderId()):"Root workspace";}
    private void updateButtons(){boolean file=selectedObject() instanceof ScheduleFile;edit.setEnabled(file);export.setEnabled(file);delete.setEnabled(selectedObject() instanceof ScheduleFolder||file);}
    private void saved(int id){refresh(id);tabs.setSelectedIndex(0);}
    private void openSelected(){if(selectedObject() instanceof ScheduleFile f){manual.open(f.getId());tabs.setSelectedIndex(2);}}
    private void loadRows() {
        int token=++loadSequence;DefaultMutableTreeNode node=(DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
        List<Integer> ids=new ArrayList<>();Map<Integer,String> names=new HashMap<>();DefaultMutableTreeNode source=node==null?root:node;
        for(var it=source.depthFirstEnumeration();it.hasMoreElements();) {Object obj=((DefaultMutableTreeNode)it.nextElement()).getUserObject();if(obj instanceof ScheduleFile f){ids.add(f.getId());names.put(f.getId(),f.getName());}}
        scope.setText("Loading...");
        new SwingWorker<List<ExamSchedule>,Void>() {
            protected List<ExamSchedule> doInBackground(){return exams.getSchedulesByFiles(ids);}
            protected void done(){if(token!=loadSequence)return;try{List<ExamSchedule> rows=get();tableModel.setRowCount(0);for(ExamSchedule e:rows)tableModel.addRow(new Object[]{names.get(e.getScheduleId()),e.getExamDate(),e.getTimeSlot(),e.getCourseCode(),e.getRoomName(),e.getBlockSection(),e.getFacultyName()});scope.setText(rows.size()+" exams. Select one schedule for PDF export.");}catch(Exception ex){scope.setText("Could not load schedules. Previous rows retained; click Refresh.");}}
        }.execute();
    }
    private void createFolder() {
        String name=JOptionPane.showInputDialog(this,"Folder name:");if(name==null)return;String trimmed=name.trim();
        if(trimmed.isEmpty()||trimmed.length()>100){BackgroundTask.error(this,new IllegalArgumentException("Folder name must contain 1-100 characters."));return;}
        Integer parent=selectedFolder();BackgroundTask.run(explorer,()->folders.createFolder(parent,trimmed),created->refresh());
    }
    private void deleteSelected() {
        Object obj=selectedObject();if(!(obj instanceof ScheduleFile)&&!(obj instanceof ScheduleFolder))return;
        if(JOptionPane.showConfirmDialog(this,"Permanently delete '"+obj+"' and its contents?","Delete",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION)return;
        BackgroundTask.run(explorer,()->obj instanceof ScheduleFile f?folders.deleteFile(f.getId()):folders.deleteFolder(((ScheduleFolder)obj).getId()),ok->{if(!ok)throw new IllegalArgumentException("Item no longer exists.");refresh();});
    }
    private void exportSelected() {
        if(!(selectedObject() instanceof ScheduleFile file))return;
        JFileChooser chooser=new JFileChooser();chooser.setFileFilter(new FileNameExtensionFilter("PDF document","pdf"));chooser.setSelectedFile(new java.io.File(file.getName().replaceAll("[\\\\/:*?\"<>|]","_")+".pdf"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        Path path=chooser.getSelectedFile().toPath();if(!path.toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))path=Path.of(path+".pdf");
        if(Files.exists(path)&&JOptionPane.showConfirmDialog(this,"Replace the existing PDF?","Export PDF",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        Path target=path;
        BackgroundTask.run(explorer,()->{ScheduleDocument doc=service.load(file.getId());SchedulePdfExporter.export(doc,target);return target;},saved->JOptionPane.showMessageDialog(this,"PDF exported:\n"+saved));
    }
}
