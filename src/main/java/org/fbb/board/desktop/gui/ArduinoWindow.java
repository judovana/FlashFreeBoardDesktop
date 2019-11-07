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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import org.fbb.board.desktop.ScreenFinder;
import org.fbb.board.internals.ContentReaderListener;
import org.fbb.board.internals.GuiLogHelper;
import org.fbb.board.internals.ProcessResult;
import org.fbb.board.internals.ProcessWrapper;
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

    public static void main(String... a) throws IOException {
        new ArduinoWindow(File.createTempFile("aaa", "bbb"), "aaa", "zz").setVisible(true);
    }

    private final File file;
    private final Object lock = new Object();

    public ArduinoWindow(final File f, String content, String currentPort) {
        this.file = f;
        this.setModal(true);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        Dimension s = ScreenFinder.getCurrentScreenSizeWithoutBounds().getSize();
        s.width = s.width / 6 * 5;
        s.height = s.height / 6 * 5;
        this.setSize(s);
        final RSyntaxTextArea readOnlySourceCode = new RSyntaxTextArea(content);
        readOnlySourceCode.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
        readOnlySourceCode.setEditable(false);

        final RSyntaxTextArea readOnlyLog = new RSyntaxTextArea("");
        readOnlyLog.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
        readOnlyLog.setEditable(false);

        final JTabbedPane pane = new JTabbedPane();

        pane.add(new RTextScrollPane(readOnlySourceCode));
        pane.add(new RTextScrollPane(readOnlyLog));
        pane.setTitleAt(0, "code");
        pane.setTitleAt(1, "log");
        this.add(pane);
        JPanel tools = new JPanel(new GridLayout(1, 3));
        ConnectionID[] ports = new PortWork().listDevices();
        if (ports.length == 0) {
            JOptionPane.showMessageDialog(null, "No ports! Maybe you are on bluetooh only? Do NOT  execute run!");
        }
        final JComboBox<ConnectionID> port = new JComboBox(ports);
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
        final JTextField tf = new JTextField("arduino --upload  {1} -v --port {2} #--board package:arch:board[:parameters]]");
        tf.setEditable(false);
        tools.add(tf);
        JButton run = new JButton("run");
        run.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                pane.setSelectedIndex(1);
                new ProcessRun().work(readOnlyLog, createCommand(tf.getText(), f, (ConnectionID) port.getSelectedItem()));
            }
        });
        tools.add(run);
        this.add(tools, BorderLayout.SOUTH);
    }

    private String createCommand(String template, File f, ConnectionID port) {
        String s = template.replaceAll("#.*", "");
        if (file == null) {
            s = s.replace("--upload", "").replace("{1}", "");
        } else {
            s = s.replace("{1}", f.getAbsolutePath());
        }
        if (port == null) {
            s = s.replace("--port", "").replace("{2}", "");
        } else {
            s = s.replace("{2}", port.getId());
        }
        return s;
    }

    private class ProcessRun {

        public void work(final RSyntaxTextArea t, String cmd) {
            t.setText("Executing: " + cmd + "\n");
            try {
                ProcessWrapper pw = new ProcessWrapper(cmd.split("\\s+"));
                pw.addStdErrListener(new ContentReaderListener() {
                    @Override
                    public void charReaded(char ch) {

                    }

                    @Override
                    public void lineReaded(String s) {
                        synchronized (lock) {
                            t.setText(t.getText() + "# " + s);
                        }
                    }
                });
                pw.addStdOutListener(new ContentReaderListener() {
                    @Override
                    public void charReaded(char ch) {

                    }

                    @Override
                    public void lineReaded(String s) {
                        synchronized (lock) {
                            t.setText(t.getText() + "$ " + s);
                        }
                    }
                });
                ProcessResult pr = pw.execute();
                if (pr.deadlyException != null) {
                    t.setText(t.getText() + etoText(pr.deadlyException));
                }
                t.setText(t.getText() + "\n EXIT: " + pr.returnValue);
            } catch (Exception e) {
                GuiLogHelper.guiLogger.loge(e);
                String s = etoText(e);
                t.setText(t.getText() + "\n" + e.getMessage() + "\n" + s);
            }
        }

        private String etoText(Throwable e) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(out));
            String str = new String(out.toByteArray());
            return str;
        }
    }
}
