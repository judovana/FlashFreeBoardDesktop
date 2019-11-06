/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.fbb.board.desktop.ScreenFinder;
import org.fbb.board.internals.comm.ConnectionID;
import org.fbb.board.internals.comm.wired.PortWork;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 *
 * @author jvanek
 */
public class ArduinoWindow extends JDialog {

    private final File file;

    public ArduinoWindow(File f, String content, String currentPort) {
        this.file = f;
        this.setModal(true);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        Dimension s = ScreenFinder.getCurrentScreenSizeWithoutBounds().getSize();
        s.width = s.width / 6 * 5;
        s.height = s.height / 6 * 5;
        this.setSize(s);
        RSyntaxTextArea rs = new RSyntaxTextArea(content);
        rs.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
        rs.setEditable(false);
        this.add(new RTextScrollPane(rs));
        JPanel tools = new JPanel(new GridLayout(1, 3));
        ConnectionID[] ports = new PortWork().listDevices();
        if (ports.length == 0) {
            JOptionPane.showMessageDialog(null, "No ports! Maybe you are on bluetooh only? Do NOT  execute run!");
        }
        JComboBox port = new JComboBox(ports);
        boolean found = false;
        for (int i = 0; i < ports.length; i++) {
            ConnectionID p = ports[i];
            if (p.getId().equals(currentPort)) {
                port.setSelectedIndex(i);
                found = true;
                break;
            }
        }
        if (!found) {
            JOptionPane.showMessageDialog(null, "Warning, set port of `" + currentPort + "` not found in currently seen ports: `" + Arrays.toString(ports) + "`");
            if (ports.length > 0) {
                JOptionPane.showMessageDialog(null, "maybe you are on bluetooth? Do NOT  execute run!");
            }
        }
        tools.add(port);
        JTextField tf = new JTextField("arduino --upload -v --port $$ #--board package:arch:board[:parameters]]");
        tools.add(tf);
        JButton run = new JButton("run");
        tools.add(run);
        this.add(tools, BorderLayout.SOUTH);
    }

}
