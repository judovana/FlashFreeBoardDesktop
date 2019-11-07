/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.fbb.board.desktop.ScreenFinder;
import org.fbb.board.internals.GuiLogHelper;
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

    public static void main(String... a) {
        new ArduinoWindow(null, "aaa", "").setVisible(true);
    }

    private final File file;

    public ArduinoWindow(File f, String content, String currentPort) {
        this.file = f;
        this.setModal(true);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        Dimension s = ScreenFinder.getCurrentScreenSizeWithoutBounds().getSize();
        s.width = s.width / 6 * 5;
        s.height = s.height / 6 * 5;
        this.setSize(s);
        final RSyntaxTextArea rs = new RSyntaxTextArea(content);
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
        run.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                new ProcessRun().work(rs);
            }
        });
        tools.add(run);
        this.add(tools, BorderLayout.SOUTH);
    }

    private class ProcessRun {

        public void work(final RSyntaxTextArea t) {
            String cmd = "cat /etc/fstab";
            t.setText("Executing: " + cmd);
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd.split("\\s+"));
                Process process = pb.start();
                Thread outThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String s = reader.readLine();
                        if (s != null) {
                            t.setText(t.getText() + "\n" + s);
                        }
                    } catch (Exception e) {
                        GuiLogHelper.guiLogger.loge(e);
                        String s = etoText(e);
                        t.setText(t.getText() + "\n" + e.getMessage());
                        t.setText(t.getText() + "\n" + s);
                    }
                });

                Thread errThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                        String s = reader.readLine();
                        if (s != null) {
                            t.setText(t.getText() + "\n" + s);
                        }
                    } catch (Exception e) {
                        GuiLogHelper.guiLogger.loge(e);
                        String s = etoText(e);
                        t.setText(t.getText() + "\n" + s);
                    }
                });

                outThread.start();
                errThread.start();

                new Thread(() -> {
                    int exitCode = -1;
                    try {
                        Thread.sleep(1000);
                        exitCode = process.waitFor();
                        t.setText(t.getText() + "\n EXIT: " + exitCode);
                        outThread.join();
                        errThread.join();
                    } catch (Exception e) {
                        GuiLogHelper.guiLogger.loge(e);
                        String s = etoText(e);
                        t.setText(t.getText() + "\n" + s);
                    }

                    // Process completed and read all stdout and stderr here
                }).start();
            } catch (Exception e) {
                GuiLogHelper.guiLogger.loge(e);
                String s = etoText(e);
                t.setText(t.getText() + "\n" + s);
            }
        }

        private String etoText(Exception e) {
            return Arrays.stream(e.getStackTrace())
                    .map(s -> s.toString())
                    .collect(Collectors.joining("\n"));
        }
    }
}
