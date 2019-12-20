/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.fbb.board.Translator;
import org.fbb.board.Utils;
import org.fbb.board.desktop.gui.dialogs.parts.DefaultSnoozeAction;
import org.fbb.board.desktop.gui.dialogs.parts.TimeAndSnooze;
import org.fbb.board.internals.GuiLogHelper;
import org.fbb.board.internals.grid.GridPane;

/**
 *
 * @author jvanek
 */
public class ClockWindow extends JDialog implements Runnable {

    private final GridPane gp;
    private final TimeAndSnooze tas;

    private JCheckBox clockwise;
    private final JComboBox<Integer> size_line;
    private final JComboBox<Integer> size_middle;
    private final JComboBox<Integer> axes;

    public ClockWindow(Window parent, GridPane gp) {
        super(parent);
        this.gp = gp;
        this.setTitle(Translator.R("clock"));
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setModalityType(ModalityType.DOCUMENT_MODAL);
        this.setSize(500, 400);
        this.setLocationRelativeTo(parent);
        this.add(new JTextField(Translator.R("clockHelp")), BorderLayout.SOUTH);
        JPanel panel = new JPanel();
        this.add(panel);
        panel.setLayout(new GridLayout(8, 2));
        panel.add(new JLabel(Translator.R("size_l")));
        size_line = new JComboBox<>(new Integer[]{1, 2, 3});
        size_line.setSelectedIndex(1);
        panel.add(size_line);

        panel.add(new JLabel(Translator.R("size_m")));
        size_middle = new JComboBox<>(new Integer[]{1, 2, 3});
        size_middle.setSelectedIndex(1);
        panel.add(size_middle);
        panel.add(new JLabel());
        clockwise = new JCheckBox(Translator.R("clockwise"), true);
        panel.add(clockwise);
        panel.add(new JLabel(Translator.R("axes")));
        axes = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6});
        axes.setSelectedIndex(0);
        panel.add(axes);
        tas = new TimeAndSnooze(panel, new DefaultSnoozeAction(gp), new Runnable() {
            @Override
            public void run() {
                ClockWindow.this.reset();
            }
        }, this);
        this.pack();
        this.setSize(this.getWidth(), 350);
        this.setLocationRelativeTo(parent);
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                tas.stop();
            }

        });
        tas.start();
    }
    private Random move = new Random();
    int degrees = 0;
    int jump = 15;

    @Override
    public void run() {
        try {
            gp.getGrid().clean();
            Point center = new Point(gp.getGrid().getWidth() / 2, gp.getGrid().getHeight() / 2);
            int r = Math.max(gp.getGrid().getWidth() / 2, gp.getGrid().getHeight() / 2);
            int astep = 360 / (axes.getSelectedIndex() + 1);
            BallWindow.drawBall(gp.getGrid(), size_middle.getSelectedIndex(), center, (byte) 3, (byte) 2, (byte) 1);
            for (int i = 0; i <= axes.getSelectedIndex(); i++) {
                Double a = Math.toRadians(degrees + (i * astep));
                int x = (int) ((double) r * Math.cos(a) - r * Math.sin(a));
                int y = (int) ((double) r * Math.sin(a) + r * Math.cos(a));
                Point dest = new Point(x + center.x, y + center.y);
                //by width, create three lines
                //moved in corect way to suggest direction
                int ymove = 0;
                int xmove = 0;
                if (x <= 0 && y <= 0) {
                    ymove = 1;
                }
                if (x >= 0 && y <= 0) {
                    xmove = -1;
                }
                if (x <= 0 && y >= 0) {
                    xmove = 1;
                }
                if (x >= 0 && y >= 0) {
                    ymove = -1;
                }
                if (!clockwise.isSelected()) {
                    xmove *= -1;
                    ymove *= -1;
                }
                List<int[]> l1 = Utils.bresenhamLine(center.x, center.y, dest.x, dest.y);
                //I keep drawing the line in two way, to get some more pixels in the most ignored ides.. not much sucess
                if (size_line.getSelectedIndex() > 1) {
                    List<int[]> l3 = Utils.bresenhamLine(center.x + 2 * xmove, center.y + 2 * ymove, dest.x + 2 * xmove, dest.y + 2 * ymove);
                    gp.getGrid().set(l3, (byte) 1);
                    l3 = new ArrayList<int[]>(l1.size());
                    for (int[] is : l1) {
                        l3.add(new int[]{is[0] + 2 * xmove, is[1] + 2 * ymove});
                    }
                    gp.getGrid().set(l3, (byte) 1);
                }
                if (size_line.getSelectedIndex() > 0) {
                    List<int[]> l2 = Utils.bresenhamLine(center.x + xmove, center.y + ymove, dest.x + xmove, dest.y + ymove);
                    gp.getGrid().set(l2, (byte) 2);
                    l2 = new ArrayList<int[]>(l1.size());
                    for (int[] is : l1) {
                        l2.add(new int[]{is[0] + xmove, is[1] + ymove});
                    }
                    gp.getGrid().set(l2, (byte) 2);
                }
                gp.getGrid().set(l1, (byte) 3);
                gp.repaintAndSendToKnown();
            }
            if (clockwise.isSelected()) {
                degrees = degrees + jump;
            } else {
                degrees = degrees - jump;
            }
        } catch (Exception ex) {
            GuiLogHelper.guiLogger.loge(ex);
            JOptionPane.showMessageDialog(ClockWindow.this, ex);
        }
    }

    private void reset() {
        degrees = 0;

    }

}
