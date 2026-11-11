package com.unimatrix.view;

import com.unimatrix.view.components.UnderlineTabs;
import com.unimatrix.view.components.Card;
import com.unimatrix.view.theme.UniMatrixTheme;
import com.unimatrix.dao.DatabaseConnection;
import com.unimatrix.dao.ExamScheduleDAO;
import com.unimatrix.dao.ScheduleFolderDAO;
import com.unimatrix.model.*;
import com.unimatrix.service.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/** Explorer owns schedule selection; the manual tab owns its independent editable draft. */
public class ManageSchedulesPanel extends JPanel {
    private final ScheduleFolderDAO folders=new ScheduleFolderDAO();
    private final ExamScheduleDAO exams=new ExamScheduleDAO();
    private final ScheduleEditorService service=new ScheduleEditorService();
    private final UnderlineTabs tabs=new UnderlineTabs();
    private final JPanel explorer=new JPanel(new BorderLayout(0,8));
    private final DefaultMutableTreeNode root=new DefaultMutableTreeNode("Exam Workspaces");
    private final DefaultTreeModel treeModel=new DefaultTreeModel(root);
    private final JTree tree=new JTree(treeModel);
    private final DefaultTableModel tableModel=new DefaultTableModel(new String[]{"Schedule","Date","Time","Course","Room","Block Section","Faculty"},0){public boolean isCellEditable(int r,int c){return false;}};
    private final JTable table=new JTable(tableModel);
    private final JButton edit=new JButton("Open / Edit"),export=new JButton("Export PDF"),delete=new JButton("Delete");
    private final JLabel scope=new JLabel("Select a schedule to edit or export.");
    private final Set<Integer> conflictScheduleIds=new HashSet<>(),conflictFolderIds=new HashSet<>();
    private final ManualSchedulePanel manual;
    private final AutoGeneratePanel automatic;
    private int loadSequence;
    public ManageSchedulesPanel() {
        super(new BorderLayout());setOpaque(false);explorer.setOpaque(false);
        manual=new ManualSchedulePanel(this::saved);
        automatic=new AutoGeneratePanel(this::saved);
        JPanel actions=BackgroundTask.bar();JButton addFolder=new JButton("New Folder"),newSchedule=new JButton("New Schedule"),refresh=new JButton("Refresh");
        actions.add(addFolder);actions.add(newSchedule);actions.add(edit);actions.add(export);actions.add(delete);actions.add(refresh);
        JButton rename=new JButton("Rename / Move"),history=new JButton("History / Undo"),trash=new JButton("Trash / Restore"),newManual=new JButton("New Manual");
        actions.add(rename);actions.add(history);actions.add(trash);actions.add(newManual);
        rename.addActionListener(e->renameMove());history.addActionListener(e->history());trash.addActionListener(e->restoreTrash());newManual.addActionListener(e->{manual.newSchedule(selectedFolder(),selectedFolderLabel());tabs.setSelectedIndex(2);});
        addFolder.addActionListener(e->createFolder());newSchedule.addActionListener(e->{automatic.setDestination(selectedFolder(),selectedFolderLabel());tabs.setSelectedIndex(1);});
        refresh.addActionListener(e->refresh());edit.addActionListener(e->openSelected());export.addActionListener(e->exportSelected());delete.addActionListener(e->deleteSelected());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new DefaultTreeCellRenderer(){
            public Component getTreeCellRendererComponent(JTree t,Object value,boolean selected,boolean expanded,boolean leaf,int row,boolean hasFocus){
                Component c=super.getTreeCellRendererComponent(t,value,selected,expanded,leaf,row,hasFocus);
                Object object=((DefaultMutableTreeNode)value).getUserObject();
                boolean conflict=object instanceof ScheduleFile fileNode&&conflictScheduleIds.contains(fileNode.getId())||object instanceof ScheduleFolder folderNode&&conflictFolderIds.contains(folderNode.getId());
                if(conflict&&!selected){c.setForeground(UniMatrixTheme.DANGER);c.setBackground(new Color(255,225,225));}
                return c;
            }
        });
        tree.setRowHeight(26);tree.setShowsRootHandles(true);tree.addTreeSelectionListener(e->{updateButtons();loadRows();});
        tree.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){if(e.getClickCount()==2)openSelected();}});
        table.setFont(UniMatrixTheme.font(Font.PLAIN,12f));
        table.getTableHeader().setFont(UniMatrixTheme.font(Font.BOLD,12f));
        table.getTableHeader().setBackground(new Color(0xF8FAFC));
        table.setGridColor(UniMatrixTheme.BORDER);
        PlannerControls.stripes(table);table.setRowHeight(32);table.setAutoCreateRowSorter(true);table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for(int i=0;i<table.getColumnCount();i++)table.getColumnModel().getColumn(i).setPreferredWidth(i==0?170:i==3?160:140);
        JPanel left=new Card(new BorderLayout());left.add(new JScrollPane(tree));left.setMinimumSize(new Dimension(180,100));
        JPanel right=new Card(new BorderLayout(0,8));right.add(scope,BorderLayout.NORTH);right.add(new JScrollPane(table));
        JPanel searchBar=BackgroundTask.bar();JTextField search=new JTextField(20);JButton searchBtn=new JButton("Search"),clear=new JButton("Clear");searchBar.add(new JLabel("Search displayed schedules:"));searchBar.add(search);searchBar.add(searchBtn);searchBar.add(clear);
        Runnable filter=()->((TableRowSorter<?>)table.getRowSorter()).setRowFilter(search.getText().isBlank()?null:RowFilter.regexFilter("(?i)"+java.util.regex.Pattern.quote(search.getText().trim())));
        searchBtn.addActionListener(e->filter.run());search.addActionListener(e->filter.run());clear.addActionListener(e->{search.setText("");filter.run();});right.add(searchBar,BorderLayout.SOUTH);
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,left,right);split.setDividerLocation(240);split.setResizeWeight(.2);split.setBorder(null);split.setOpaque(false);split.setDividerSize(8);
        explorer.add(actions,BorderLayout.NORTH);explorer.add(split,BorderLayout.CENTER);
        tabs.addTab("Schedules",explorer);tabs.addTab("Auto-Generate Config",automatic);tabs.addTab("Manual Encoding",manual);add(tabs);updateButtons();SwingUtilities.invokeLater(this::refresh);
    }
    private record TreeData(List<ScheduleFolder> folders,List<ScheduleFile> files,Set<Integer> conflicts) { }
    private Set<Integer> unresolvedScheduleIds() throws SQLException {Set<Integer> ids=new HashSet<>();try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT DISTINCT e.schedule_id FROM exam_schedules e JOIN schedule_files f ON f.id=e.schedule_id WHERE f.deleted=FALSE AND (e.exam_date IS NULL OR e.time_slot IS NULL OR e.room_id IS NULL)")){try(ResultSet r=p.executeQuery()){while(r.next())ids.add(r.getInt(1));}}return ids;}
    public void refresh(){refresh(null);}
    private void refresh(Integer selectId) {
        Object selected=selectedObject();
        BackgroundTask.run(explorer,()->new TreeData(folders.getAllFolders(),folders.getAllFiles(),unresolvedScheduleIds()),data->{
            conflictScheduleIds.clear();conflictScheduleIds.addAll(data.conflicts());conflictFolderIds.clear();Map<Integer,Integer> folderParents=new HashMap<>();for(ScheduleFolder f:data.folders())folderParents.put(f.getId(),f.getParentId());for(ScheduleFile f:data.files())if(conflictScheduleIds.contains(f.getId())){Integer parent=f.getFolderId();while(parent!=null&&conflictFolderIds.add(parent))parent=folderParents.get(parent);}
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
    private Integer selectedFolder(){Object o=selectedObject(); if(o instanceof ScheduleFolder f)return Integer.valueOf(f.getId()); if(o instanceof ScheduleFile f)return f.getFolderId(); return null;}
    private String selectedFolderLabel(){Object o=selectedObject();return o instanceof ScheduleFolder f?f.getName():o instanceof ScheduleFile f?(f.getFolderId()==null?"Root workspace":"Folder #"+f.getFolderId()):"Root workspace";}
    private void updateButtons(){boolean file=selectedObject() instanceof ScheduleFile;edit.setEnabled(file);export.setEnabled(file);delete.setEnabled(selectedObject() instanceof ScheduleFolder||file);}
    private void saved(int id){conflictScheduleIds.remove(id);refresh(id);tabs.setSelectedIndex(0);}
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
        Integer parent=selectedFolder();BackgroundTask.run(explorer,()->folders.createFolder(parent,trimmed),created->{
            if(created==null)throw new IllegalArgumentException("Database returned no folder ID.");
            // Insert the returned object directly; do not discard a completed create behind a busy refresh.
            DefaultMutableTreeNode parentNode=root;
            for(var it=root.depthFirstEnumeration();it.hasMoreElements();){DefaultMutableTreeNode n=(DefaultMutableTreeNode)it.nextElement();if(n.getUserObject() instanceof ScheduleFolder f&&Objects.equals(f.getId(),created.getParentId()))parentNode=n;}
            DefaultMutableTreeNode child=new DefaultMutableTreeNode(created,true);treeModel.insertNodeInto(child,parentNode,parentNode.getChildCount());TreePath path=new TreePath(child.getPath());tree.expandPath(new TreePath(parentNode.getPath()));tree.setSelectionPath(path);tree.scrollPathToVisible(path);
        });
    }
    private void deleteSelected() {
        Object obj=selectedObject();if(!(obj instanceof ScheduleFile)&&!(obj instanceof ScheduleFolder))return;
        if(JOptionPane.showConfirmDialog(this,"Move '"+obj+"' and its contents to Trash?","Delete",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION)return;
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
    private WorkspaceService.Item selectedItem(){Object o=selectedObject();if(o instanceof ScheduleFolder f)return new WorkspaceService.Item(f.getId(),true,f.getParentId(),f.getName());if(o instanceof ScheduleFile f)return new WorkspaceService.Item(f.getId(),false,f.getFolderId(),f.getName());return null;}
    private void renameMove(){WorkspaceService.Item selected=selectedItem();if(selected==null)return;BackgroundTask.run(explorer,()->WorkspaceService.items(false),items->{
        JTextField name=new JTextField(selected.name(),22);JComboBox<WorkspaceService.Item> destinations=new JComboBox<>();destinations.addItem(new WorkspaceService.Item(0,true,null,"Workspace root"));for(var item:items)if(item.folder())destinations.addItem(item);for(int i=0;i<destinations.getItemCount();i++)if(Objects.equals(selected.parent(),destinations.getItemAt(i).id()))destinations.setSelectedIndex(i);
        if(JOptionPane.showConfirmDialog(this,new Object[]{"Name",name,"Destination",destinations},"Rename / Move",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;var dest=(WorkspaceService.Item)destinations.getSelectedItem();String text=name.getText();BackgroundTask.run(explorer,()->{WorkspaceService.renameMove(selected,text,dest.id()==0?null:dest.id());return true;},ok->refresh());});}
    private void restoreTrash(){BackgroundTask.run(explorer,()->WorkspaceService.items(true),items->{if(items.isEmpty()){JOptionPane.showMessageDialog(this,"Trash is empty.");return;}JComboBox<WorkspaceService.Item> choices=new JComboBox<>(items.toArray(WorkspaceService.Item[]::new));JButton recover=new JButton("Recover"),permanent=new JButton("Permanently Delete");JPanel panel=new JPanel(new BorderLayout(8,8));panel.add(new JLabel("Deleted schedules and folders:"),BorderLayout.NORTH);panel.add(choices,BorderLayout.CENTER);JPanel buttons=new JPanel();buttons.add(recover);buttons.add(permanent);panel.add(buttons,BorderLayout.SOUTH);JDialog dialog=new JOptionPane(panel,JOptionPane.PLAIN_MESSAGE,JOptionPane.DEFAULT_OPTION).createDialog(this,"Trash");recover.addActionListener(e->{var selected=(WorkspaceService.Item)choices.getSelectedItem();dialog.dispose();if(selected!=null)BackgroundTask.run(explorer,()->WorkspaceService.recover(selected),cleared->{refresh();if(cleared>0)JOptionPane.showMessageDialog(this,"The restored schedule has a conflicting error. Conflicting exam entries were cleared and highlighted in red.","Schedule Conflict",JOptionPane.WARNING_MESSAGE);});});permanent.addActionListener(e->{var selected=(WorkspaceService.Item)choices.getSelectedItem();if(selected==null)return;if(JOptionPane.showConfirmDialog(dialog,"Permanently delete "+selected+" and any trashed contents? This cannot be undone.","Confirm Permanent Deletion",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION)return;dialog.dispose();BackgroundTask.run(explorer,()->{WorkspaceService.permanentlyDelete(selected);return true;},ok->refresh());});dialog.setVisible(true);});}
    private void history(){if(!(selectedObject() instanceof ScheduleFile file))return;BackgroundTask.run(explorer,()->new Object[]{service.load(file.getId()),ScheduleHistory.list(file.getId())},data->{
        var current=(ScheduleDocument)data[0];@SuppressWarnings("unchecked") List<ScheduleHistory.Entry> entries=(List<ScheduleHistory.Entry>)data[1];if(entries.isEmpty()){JOptionPane.showMessageDialog(this,"No earlier saved revisions.");return;}var selected=(ScheduleHistory.Entry)JOptionPane.showInputDialog(this,"Restore which saved revision? Current data will be recorded before restoration.","Schedule history",JOptionPane.PLAIN_MESSAGE,null,entries.toArray(),entries.get(0));if(selected!=null)BackgroundTask.run(explorer,()->{ScheduleHistory.restore(file.getId(),selected.id(),current.revision());return true;},ok->refresh(file.getId()));});}
}
