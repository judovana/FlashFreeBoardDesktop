/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import org.fbb.board.internals.db.DB;
import org.fbb.board.internals.GuiLogHelper;

/**
 *
 * @author jvanek
 */
public class LogView extends JDialog {

    private final DB db;

    private class Autorefrsh extends Thread {

        private boolean alive = true;
        private final JTextArea o;
        private final JTextArea e;
        private final JCheckBox r;

        private Autorefrsh(JTextArea o, JTextArea e, JCheckBox r) {
            this.o = o;
            this.e = e;
            this.r = r;
        }

        @Override
        public void run() {
            while (alive) {
                try {
                    if (r.isSelected()) {
                        e.setText(GuiLogHelper.guiLogger.getSerr());
                        e.setCaretPosition(e.getDocument().getLength());
                        o.setText(GuiLogHelper.guiLogger.getSout());
                        o.setCaretPosition(o.getDocument().getLength());
                    }
                    Thread.sleep(500);
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                }
            }
        }

    }

    public LogView(DB ddb) throws HeadlessException {
        this(ddb, false);
    }
    public LogView(DB ddb, boolean modal) throws HeadlessException {
        this.db = ddb;
        this.setModal(modal);
        this.setLayout(new BorderLayout());
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        JTextArea e = new JTextArea();
        e.setText(GuiLogHelper.guiLogger.getSerr());
        JTextArea o = new JTextArea();
        o.setText(GuiLogHelper.guiLogger.getSout());
        split.add(new JScrollPane(o));
        split.add(new JScrollPane(e));
        this.add(split);
        JPanel tools = new JPanel(new GridLayout(1, 3));
        JButton or = new JButton("refresh");
        or.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent x) {
                o.setText(GuiLogHelper.guiLogger.getSout());
            }
        });
        tools.add(or);
        final JCheckBox autor = new JCheckBox("autorefresh", true);
        tools.add(autor);
        JButton log = new JButton("db log");
        tools.add(log);
        log.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                autor.setSelected(false);
                o.setText(o.getText() + "\n" + db.logCatched());
            }
        });
        JButton er = new JButton("refresh");
        er.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent x) {
                e.setText(GuiLogHelper.guiLogger.getSerr());
            }
        });
        tools.add(er);
        this.add(tools, BorderLayout.SOUTH);
        this.pack();
        split.setDividerLocation(400);
        this.setSize(800, 800);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        final Autorefrsh autorefresh = new Autorefrsh(o, e, autor);
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                try {
                    super.windowClosed(e);
                } finally {
                    autorefresh.alive = false;
                }

            }

        });
        autorefresh.start();

    }

}
