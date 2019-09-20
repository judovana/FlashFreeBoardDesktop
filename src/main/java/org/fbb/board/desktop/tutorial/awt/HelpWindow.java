/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.tutorial.awt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.fbb.board.Translator;
import org.fbb.board.desktop.tutorial.ImgProvider;
import org.fbb.board.desktop.tutorial.StoryPart;
import org.fbb.board.desktop.tutorial.StoryProvider;
import org.fbb.board.internals.GuiLogHelper;

/**
 *
 * @author jvanek
 */
public class HelpWindow extends JDialog {

    private final ImgProvider ip;
    private final JLabel welcome;
    private boolean running = true;
    private AtomicInteger index = new AtomicInteger();
    private final StoryProvider sp;
    private ImageWithTitles iwt;
    private boolean isAlive = true;

    public HelpWindow() throws IOException {
        this.setTitle(Translator.R("HELP"));
        this.setModal(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setSize(650, 800);
        sp = new StoryProvider();
        ip = new ImgProvider();
        welcome = new JLabel(Translator.R("nwBoulderWelcome"));
        welcome.setOpaque(true);
        this.add(welcome, BorderLayout.NORTH);
        final Color wc = welcome.getBackground();
        setImg();
        JPanel tools = new JPanel(new GridLayout(1, 3));
        final JButton backb = new JButton("<<");
        tools.add(backb);
        final JButton pauseb = new JButton("II");
        tools.add(pauseb);
        final JButton fwdb = new JButton(">>");
        tools.add(fwdb);
        this.add(tools, BorderLayout.SOUTH);
        pauseb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                running = !running;
                if (running) {
                    pauseb.setText("II");
                } else {
                    pauseb.setText(">");
                }
            }
        });
        fwdb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {

                try {
                    index.addAndGet(1);
                    if (index.get() >= sp.getStory().size()) {
                        index.set(sp.getStory().size() - 1);
                    }
                    setImg();
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                }
            }
        });
        backb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
                    index.addAndGet(-1);
                    if (index.get() < 0) {
                        index.set(0);
                    }
                    setImg();
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                }
            }
        });
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                    for (int i = 0; i < 6; i++) {
                        Thread.sleep(200);
                        if (i % 2 == 0) {
                            welcome.setBackground(Color.yellow);
                        } else {
                            welcome.setBackground(wc);
                        }
                        welcome.repaint();
                    }
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                }
            }
        });
        t1.setDaemon(true);
        t1.start();
        Thread t2 = new Thread(new Runner());
        t2.setDaemon(true);
        t2.start();

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent we) {
                isAlive = false;
            }

        });
    }

    private void setImg() throws IOException {
        if (index.get() < 0 || index.get() >= sp.getStory().size()) {
            return;
        }
        StoryPart start = sp.getStory().get(index.get());
        if (iwt != null) {
            this.remove(iwt);
        }
        iwt = new ImageWithTitles(start.getLines(), ip.get(start.getImg()));
        this.add(iwt);
        this.validate();
    }

    private class Runner implements Runnable {

        @Override
        public void run() {
            try {
                Thread.sleep(1000);
            } catch (Exception ex) {
                GuiLogHelper.guiLogger.loge(ex);
            }
            while (isAlive) {
                try {
                    Thread.sleep(3000);
                    if (running) {
                        index.addAndGet(1);
                        if (index.get() >= sp.getStory().size()) {
                            index.set(0);
                        }
                        setImg();
                    }
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                }
            }
        }
    }

    public static void show(final Component c) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    HelpWindow h = new HelpWindow();
                    h.setLocationRelativeTo(c);
                    h.setVisible(true);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

    }
    
    public static void main(String... args) {
        show(null);
    }

}
