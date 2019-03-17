/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import org.fbb.board.internals.GuiLogHelper;

/**
 *
 * @author jvanek
 */
public class LogView extends JFrame {

    public LogView() throws HeadlessException {
        this.setLayout(new BorderLayout());
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        JTextArea e = new JTextArea();
        e.setText(GuiLogHelper.guiLogger.getSerr());
        JTextArea o = new JTextArea();
        o.setText(GuiLogHelper.guiLogger.getSout());
        split.add(new JScrollPane(o));
        split.add(new JScrollPane(e));
        this.add(split);
        JPanel tools = new JPanel(new GridLayout(1, 2));
        JButton or = new JButton("refresh");
        or.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent x) {
                o.setText(GuiLogHelper.guiLogger.getSout());
            }
        });
        tools.add(or);
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

    }

}
