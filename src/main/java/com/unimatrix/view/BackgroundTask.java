package com.unimatrix.view;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** All I/O off the EDT; controls restored before reporting success or failure. */
final class BackgroundTask {
    private BackgroundTask() { }
    static <T> void run(JComponent owner, Callable<T> work, Consumer<T> success) {
        run(owner, work, success, ex -> { Throwable cause=ex; while(cause.getCause()!=null) cause=cause.getCause(); JOptionPane.showMessageDialog(owner,cause.getMessage(),"Operation failed",JOptionPane.ERROR_MESSAGE); });
    }
    static <T> void run(JComponent owner, Callable<T> work, Consumer<T> success, Consumer<Exception> failure) {
        if(Boolean.TRUE.equals(owner.getClientProperty("busy"))) return;
        owner.putClientProperty("busy",true);
        Map<Component,Boolean> enabled=new IdentityHashMap<>(); freeze(owner,enabled);
        owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<T,Void>() {
            protected T doInBackground() throws Exception { return work.call(); }
            protected void done() {
                enabled.forEach(Component::setEnabled); owner.putClientProperty("busy",false); owner.setCursor(Cursor.getDefaultCursor());
                try { success.accept(get()); }
                catch(Exception ex) { failure.accept(ex); }
            }
        }.execute();
    }
    private static void freeze(Component c,Map<Component,Boolean> enabled) {
        enabled.put(c,c.isEnabled());c.setEnabled(false);
        if(c instanceof Container container) for(Component child:container.getComponents()) freeze(child,enabled);
    }
    static boolean finishEdit(JTable table) { return !table.isEditing() || table.getCellEditor().stopCellEditing(); }
    static JPanel bar() { JPanel p=new JPanel(new com.unimatrix.view.components.WrapLayout(FlowLayout.LEFT,8,6));p.setOpaque(false);return p; }
    static void error(Component parent,Exception ex) { JOptionPane.showMessageDialog(parent,ex.getMessage(),"Check entries",JOptionPane.WARNING_MESSAGE); }
}
