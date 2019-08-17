/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.charset.Charset;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.fbb.board.Translator;
import org.fbb.board.desktop.Files;
import org.fbb.board.desktop.ScreenFinder;
import org.fbb.board.internals.FUtils;
import org.fbb.board.internals.GlobalSettings;
import org.fbb.board.internals.GuiLogHelper;
import org.fbb.board.internals.comm.ConnectionID;
import org.fbb.board.internals.db.DB;
import org.fbb.board.internals.db.GuiExceptionHandler;
import org.fbb.board.internals.db.Puller;
import org.fbb.board.internals.grid.GridPane;

/**
 *
 * @author jvanek
 */
class SettingsListener implements ActionListener {

    private final GridPane gp;
    private final DB db;
    private final GlobalSettings gs;
    private final Authenticator auth;
    private final Puller puller;
    private final int selectedTab;
    private final JLabel remoteSecurityStatus = new JLabel();
    private final JButton remoteSecurityButton = new JButton();
    private final ActionListener deleteCokie = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean a = Files.remotePass.delete();
            if (a) {
                JOptionPane.showMessageDialog(null, Translator.R("cDeleted", Files.remotePass.getAbsolutePath()));
            } else {
                JOptionPane.showMessageDialog(null, Translator.R("cNotDeleted", Files.remotePass.getAbsolutePath()));
            }
            setRemoteSecuirty();
        }
    };

    private final ActionListener createCookie = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            String p = JOptionPane.showInputDialog(Translator.R("cPass"));
            if (p != null) {
                try {
                    java.nio.file.Files.write(Files.remotePass.toPath(), p.getBytes(Charset.forName("utf-8")));
                    JOptionPane.showMessageDialog(null, Translator.R("cReated", Files.remotePass.getAbsolutePath()));
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                    JOptionPane.showMessageDialog(null, ex);
                }
            }
            setRemoteSecuirty();
        }
    };

    public SettingsListener(GridPane gp, Authenticator auth, GlobalSettings gs, Puller puller, DB db, int selectedTab) {
        this.gp = gp;
        this.auth = auth;
        this.gs = gs;
        this.puller = puller;
        this.db = db;
        this.selectedTab = selectedTab;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            auth.authenticate(Translator.R("settingsAuth"));
        } catch (Authenticator.AuthoriseException a) {
            GuiLogHelper.guiLogger.loge(a);
            JOptionPane.showMessageDialog(null, a);
            return;
        }
        JDialog allSettingsWindow = new JDialog((JFrame) null, "FFB settings", true);
        JTabbedPane settingsTabs = new JTabbedPane();
        int genRows = 6;
        int conRows = 2;
        int colRows = 13;
        int remRows = 9;
        int ampRows = 6;
        int maxRows = colRows;
        JPanel general = new JPanel(new GridLayout(maxRows, 2));
        general.setName(Translator.R("generalTab"));
        JPanel connection = new JPanel(new GridLayout(maxRows, 2));
        connection.setName(Translator.R("connectionTab"));
        JPanel colors = new JPanel(new GridLayout(maxRows, 2));
        colors.setName(Translator.R("colorsTab"));
        JPanel remote = new JPanel(new GridLayout(maxRows, 2));
        remote.setName(Translator.R("remoteTab"));
        JPanel amps = new JPanel(new GridLayout(maxRows, 2));
        amps.setName(Translator.R("ampsTab"));
        settingsTabs.add(general);
        settingsTabs.add(connection);
        settingsTabs.add(colors);
        settingsTabs.add(remote);
        settingsTabs.add(amps);
        settingsTabs.setSelectedIndex(selectedTab);
        allSettingsWindow.add(settingsTabs);
        general.add(new JLabel(Translator.R("brightenes")));
        JSpinner brigthtnessSpinner = new JSpinner(new SpinnerNumberModel(gs.getBrightness(), 1, 254, 1));
        brigthtnessSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                gs.setBrightness((Integer) brigthtnessSpinner.getValue());
                if (gp != null) {
                    gp.repaintAndSend(gs);
                }
            }
        });
        general.add(brigthtnessSpinner);
        JLabel securityLabel = new JLabel(Translator.R("security"));
        general.add(securityLabel);
        JTextField securityStatus1 = new JTextField();
        JTextField securityStatus2 = new JTextField();
        JTextField securityStatus3 = new JTextField();
        securityStatus1.setEditable(false);
        securityStatus2.setEditable(false);
        securityStatus3.setEditable(false);
        securityStatus1.setText(auth.getStatus()[0]);
        securityStatus2.setText(auth.getStatus()[1]);
        securityStatus3.setText(auth.getStatus()[2]);
        general.add(securityStatus1);
        general.add(securityStatus2);
        general.add(securityStatus3);
        general.add(new JLabel(Translator.R("HOLD_OPACITY")));
        JSpinner holdOpacity = new JSpinner(new SpinnerNumberModel((double) gs.getHoldMarkerOapcity(), 0, 1, 0.05));
        holdOpacity.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                gs.setHoldMarkerOapcity(((Double) holdOpacity.getValue()).floatValue());
                if (gp != null) {
                    gp.repaint();
                }
            }
        });
        general.add(holdOpacity);
        general.add(new JLabel(Translator.R("STYLE")));
        JComboBox<String> holdStyle = new JComboBox<>(new String[]{Translator.R("FILL_0"), Translator.R("C_BIG_1"), Translator.R("C_SMALL_2"), Translator.R("C_BOTH_3"), Translator.R("E_BIG_4"), Translator.R("E_SMALL_5"), Translator.R("E_BOTH_6"), Translator.R("RECT_7")});
        holdStyle.setSelectedIndex(gs.getDefaultStyle());
        holdStyle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gs.setDefaultStyle(holdStyle.getSelectedIndex());
                if (gp != null) {
                    gp.repaint();
                }
            }
        });
        general.add(holdStyle);
        setRemoteSecuirty();
        general.add(remoteSecurityStatus);
        general.add(remoteSecurityButton);
        colors.add(new JLabel(Translator.R("testdelay")));
        JSpinner testDelay = new JSpinner(new SpinnerNumberModel(50, 1, 10000, 50));
        colors.add(testDelay);
        final JButton re = new JButton(Translator.R("testred"));
        re.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gp != null) {
                    gp.getGrid().testRed((Integer) testDelay.getValue());
                }
            }
        });
        final JButton gr = new JButton(Translator.R("testgreen"));
        gr.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gp != null) {
                    gp.getGrid().testGreen((Integer) testDelay.getValue());
                }
            }
        });
        final JButton bl = new JButton(Translator.R("testblue"));
        bl.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gp != null) {
                    gp.getGrid().testBlue((Integer) testDelay.getValue());
                }
            }
        });
        colors.add(gr);
        colors.add(bl);
        colors.add(re);
        JButton snake = new JButton("snake game");
        snake.setEnabled(false);
        colors.add(snake);
        JComboBox<String> portType = new JComboBox<>(new String[]{"port", "bluetooth", "nothing"});
        portType.setSelectedIndex(gs.getPortTypeIndex());
        portType.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gs.setPortType(portType.getSelectedIndex());
            }
        });
        connection.add(portType);
        JTextField portName = new JTextField(gs.getPortId());
        connection.add(portName);
        portName.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                gs.setDeviceId(portName.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                gs.setDeviceId(portName.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                gs.setDeviceId(portName.getText());
            }
        });
        connection.add(new JLabel());
        JButton selectPort = new JButton(Translator.R("Bselect"));
        connection.add(selectPort);
        selectPort.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    JDialog selectPortDialog = new JDialog();
                    selectPortDialog.setModal(true);
                    selectPortDialog.setLayout(new BorderLayout());
                    JLabel title = new JLabel(Translator.R("selectTitle"));
                    selectPortDialog.add(title, BorderLayout.NORTH);
                    JLabel waiting = new JLabel("<html><div style='text-align: center;'>" + Translator.R("scanning") + "</div></html>");
                    selectPortDialog.add(waiting);
                    JLabel message = new JLabel(Translator.R("scanHelp"));
                    selectPortDialog.add(message, BorderLayout.SOUTH);
                    selectPortDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    selectPortDialog.setSize(300, 400);
                    selectPortDialog.setLocationRelativeTo(allSettingsWindow);
                    SwingWorker<JList<ConnectionID>, JList<ConnectionID>> sw = new SwingWorker() {
                        @Override
                        protected JList<ConnectionID> doInBackground() throws Exception {
                            return new JList<>(gs.list());
                        }

                        @Override
                        public void done() {
                            try {
                                selectPortDialog.remove(waiting);
                                JList<ConnectionID> item = (JList) this.get();
                                if (item.getModel().getSize() == 0) {
                                    JLabel iitem = new JLabel("<html><div style='text-align: center;'>" + Translator.R("noDeviceFoound") + "</div></html>");
                                    selectPortDialog.add(iitem);
                                } else {
                                    selectPortDialog.add(item);
                                    item.addListSelectionListener(new ListSelectionListener() {
                                        @Override
                                        public void valueChanged(ListSelectionEvent e) {
                                            portName.setText(item.getSelectedValue().getId());
                                        }
                                    });
                                }
                                item.addMouseListener(new MouseAdapter() {
                                    @Override
                                    public void mouseClicked(MouseEvent e) {
                                        if (e.getClickCount() > 1) {
                                            selectPortDialog.dispose();
                                        }
                                    }
                                });
                                selectPortDialog.pack();
                            } catch (Exception ex) {
                                GuiLogHelper.guiLogger.loge(ex);
                                JOptionPane.showMessageDialog(selectPortDialog, ex);
                            }
                        }
                    };
                    sw.execute();
                    selectPortDialog.setVisible(true);
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                    JOptionPane.showMessageDialog(allSettingsWindow, ex);
                }
            }
        });
        JLabel greenRedTitle = new JLabel(Translator.R("StartCompozition", Translator.R("red")));
        JSpinner greenRed = new JSpinner(new SpinnerNumberModel(gs.getPart(0), 0d, 1d, 0.1));
        JLabel greenGreenTitle = new JLabel(Translator.R("StartCompozition", Translator.R("green")));
        JSpinner greenGreen = new JSpinner(new SpinnerNumberModel(gs.getPart(1), 0d, 1d, 0.1));
        JLabel greenBlueTitle = new JLabel(Translator.R("StartCompozition", Translator.R("blue")));
        JSpinner greenBlue = new JSpinner(new SpinnerNumberModel(gs.getPart(2), 0d, 1d, 0.1));
        JLabel blueRedTitle = new JLabel(Translator.R("PathCompozition", Translator.R("red")));
        JSpinner blueRed = new JSpinner(new SpinnerNumberModel(gs.getPart(3), 0d, 1d, 0.1));
        JLabel blueGreenTitle = new JLabel(Translator.R("PathCompozition", Translator.R("green")));
        JSpinner blueGreen = new JSpinner(new SpinnerNumberModel(gs.getPart(4), 0d, 1d, 0.1));
        JLabel blueBlueTitle = new JLabel(Translator.R("PathCompozition", Translator.R("blue")));
        JSpinner blueBlue = new JSpinner(new SpinnerNumberModel(gs.getPart(5), 0d, 1d, 0.1));
        JLabel redRedTitle = new JLabel(Translator.R("TopCompozition", Translator.R("red")));
        JSpinner redRed = new JSpinner(new SpinnerNumberModel(gs.getPart(6), 0d, 1d, 0.1));
        JLabel redGreenTitle = new JLabel(Translator.R("TopCompozition", Translator.R("green")));
        JSpinner redGreen = new JSpinner(new SpinnerNumberModel(gs.getPart(7), 0d, 1d, 0.1));
        JLabel redBlueTitle = new JLabel(Translator.R("TopCompozition", Translator.R("blue")));
        JSpinner redBlue = new JSpinner(new SpinnerNumberModel(gs.getPart(8), 0d, 1d, 0.1));
        colors.add(greenRedTitle);
        colors.add(greenRed);
        colors.add(greenGreenTitle);
        colors.add(greenGreen);
        colors.add(greenBlueTitle);
        colors.add(greenBlue);
        colors.add(blueRedTitle);
        colors.add(blueRed);
        colors.add(blueGreenTitle);
        colors.add(blueGreen);
        colors.add(blueBlueTitle);
        colors.add(blueBlue);
        colors.add(redRedTitle);
        colors.add(redRed);
        colors.add(redGreenTitle);
        colors.add(redGreen);
        colors.add(redBlueTitle);
        colors.add(redBlue);
        if (gp != null) {
            greenRed.addChangeListener(new PathColorCompozitorListener(0, gp, brigthtnessSpinner, gr));
            greenGreen.addChangeListener(new PathColorCompozitorListener(1, gp, brigthtnessSpinner, gr));
            greenBlue.addChangeListener(new PathColorCompozitorListener(2, gp, brigthtnessSpinner, gr));
            blueRed.addChangeListener(new PathColorCompozitorListener(3, gp, brigthtnessSpinner, bl));
            blueGreen.addChangeListener(new PathColorCompozitorListener(4, gp, brigthtnessSpinner, bl));
            blueBlue.addChangeListener(new PathColorCompozitorListener(5, gp, brigthtnessSpinner, bl));
            redRed.addChangeListener(new PathColorCompozitorListener(6, gp, brigthtnessSpinner, re));
            redGreen.addChangeListener(new PathColorCompozitorListener(7, gp, brigthtnessSpinner, re));
            redBlue.addChangeListener(new PathColorCompozitorListener(8, gp, brigthtnessSpinner, re));
        } else {
            greenRed.setEnabled(false);
            greenGreen.setEnabled(false);
            greenBlue.setEnabled(false);
            blueRed.setEnabled(false);
            blueGreen.setEnabled(false);
            blueBlue.setEnabled(false);
            redRed.setEnabled(false);
            redGreen.setEnabled(false);
            redBlue.setEnabled(false);
            brigthtnessSpinner.setEnabled(false);
            holdOpacity.setEnabled(false);
            holdStyle.setEnabled(false);
            re.setEnabled(false);
            gr.setEnabled(false);
            bl.setEnabled(false);
        }
        remote.add(new JLabel(Translator.R("remoteUrl")));
        JTextField remoteUrl = new JTextField(gs.getUrl());
        remote.add(remoteUrl);
        remoteUrl.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                gs.setUrl(remoteUrl.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                gs.setUrl(remoteUrl.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                gs.setUrl(remoteUrl.getText());
            }
        });
        remote.add(new JLabel(Translator.R("remoteBranch")));
        JTextField remoteBranch = new JTextField(gs.getBranch());
        remote.add(remoteBranch);
        remoteBranch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                gs.setBranch(remoteBranch.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                gs.setBranch(remoteBranch.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                gs.setBranch(remoteBranch.getText());
            }
        });
        remote.add(new JLabel(Translator.R("remoteUser")));
        JTextField remoteUser = new JTextField(gs.getRuser());
        remote.add(remoteUser);
        remoteUser.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                gs.setRuser(remoteUser.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                gs.setRuser(remoteUser.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                gs.setRuser(remoteUser.getText());
            }
        });
        final JLabel autoPullLabel = new JLabel(Translator.R("AutoPull"));
        final JSpinner autoPull = new JSpinner(new SpinnerNumberModel(gs.getPullerDelay(), 0, 60, 1));
        remote.add(autoPullLabel);
        remote.add(autoPull);
        autoPull.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                gs.setPullerDelay((Integer) autoPull.getValue());
                puller.setDelay(gs.getPullerDelay() * 60);
            }
        });
        JButton reInit = new JButton(Translator.R("reInit"));
        remote.add(reInit);
        reInit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    boolean exists = db.init(false);
                    if (exists) {
                        int force = JOptionPane.showConfirmDialog(null, Translator.R("reInitConfirm"));
                        if (force == JOptionPane.YES_OPTION) {
                            db.init(true);
                        }
                    }
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                    JOptionPane.showMessageDialog(null, ex);
                }
            }
        });
        JButton rdelete = new JButton(Translator.R("rdelete"));
        remote.add(rdelete);
        rdelete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    boolean exists = db.unregisterRm(false);
                    if (exists) {
                        int force = JOptionPane.showConfirmDialog(null, Translator.R("rdeleteConfirm"));
                        if (force == JOptionPane.YES_OPTION) {
                            db.unregisterRm(true);
                        }
                    }
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                    JOptionPane.showMessageDialog(null, ex);
                }
            }
        });
        JButton rSyncDown = new JButton(Translator.R("rSyncDown"));
        rSyncDown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                db.pullCatched(new GuiExceptionHandler());
            }
        });
        JButton rSyncUp = new JButton(Translator.R("rSyncUp"));
        rSyncUp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                db.pushCatched(new GuiExceptionHandler());
            }
        });
        JButton rResetHard = new JButton(Translator.R("rResetHard"));
        JButton rAddAll = new JButton(Translator.R("rAddAll"));
        remote.add(rSyncDown);
        remote.add(rSyncUp);
        remote.add(new JLabel(Translator.R("danger")));
        remote.add(rResetHard);
        remote.add(new JLabel(Translator.R("danger")));
        remote.add(rAddAll);
        rAddAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    db.addAll();
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                    JOptionPane.showMessageDialog(null, ex);
                }
            }
        });
        rResetHard.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    db.hardReset();
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                    JOptionPane.showMessageDialog(null, ex);
                }
            }
        });
        JButton voltageHelp = new JButton("? ->");
        voltageHelp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(allSettingsWindow, Translator.R("underAmpersDescrioption"));
            }
        });
        JLabel voltageTitle = new JLabel(Translator.R("voltageTitle"));
        JLabel singleLedAmpersLabel = new JLabel(Translator.R("singleLedAmpers"));
        final JSpinner singleLedAmpers = new JSpinner(new SpinnerNumberModel(gs.getSingleRgbLedAmpers(), 0.d, 10d, 0.01));
        JLabel singleSourceAmpersLabel = new JLabel(Translator.R("singleSourceAmpers"));
        final JSpinner singleSourceAmpers = new JSpinner(new SpinnerNumberModel(gs.getSingleSourceAmpers(), 0d, 150d, 0.1));
        JLabel numberOfSourcesLabel = new JLabel(Translator.R("numberOfSources"));
        final JSpinner numberOfSources = new JSpinner(new SpinnerNumberModel(gs.getNumberOfSources(), 0, 100, 1));
        amps.add(voltageHelp);
        amps.add(voltageTitle);
        amps.add(singleLedAmpersLabel);
        amps.add(singleLedAmpers);
        amps.add(singleSourceAmpersLabel);
        amps.add(singleSourceAmpers);
        amps.add(numberOfSourcesLabel);
        amps.add(numberOfSources);
        final JLabel ampersResult1 = new JLabel();
        final JLabel ampersResult2 = new JLabel();
        adjustAmperLabels(ampersResult1, ampersResult2, singleLedAmpers, singleSourceAmpers, numberOfSources);
        amps.add(ampersResult1);
        amps.add(ampersResult2);
        singleLedAmpers.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                gs.setSingleRgbLedAmpers((Double) (singleLedAmpers.getValue()));
                adjustAmperLabels(ampersResult1, ampersResult2, singleLedAmpers, singleSourceAmpers, numberOfSources);
            }

        });
        singleSourceAmpers.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                gs.setSingleSourceAmpers((Double) (singleSourceAmpers.getValue()));
                adjustAmperLabels(ampersResult1, ampersResult2, singleLedAmpers, singleSourceAmpers, numberOfSources);
            }
        });
        numberOfSources.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                gs.setNumberOfSources((Integer) (numberOfSources.getValue()));
                adjustAmperLabels(ampersResult1, ampersResult2, singleLedAmpers, singleSourceAmpers, numberOfSources);
            }
        });
        FUtils.align(genRows, maxRows, general);
        FUtils.align(conRows, maxRows, connection);
        FUtils.align(colRows, maxRows, colors);
        FUtils.align(remRows, maxRows, remote);
        FUtils.align(ampRows, maxRows, amps);
        allSettingsWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        allSettingsWindow.pack();
        allSettingsWindow.setLocationRelativeTo(null);
        if (allSettingsWindow.getWidth() > ScreenFinder.getCurrentScreenSizeWithoutBounds().width) {
            allSettingsWindow.setSize(ScreenFinder.getCurrentScreenSizeWithoutBounds().width - 5, allSettingsWindow.getHeight());
        }
        if (allSettingsWindow.getHeight() > ScreenFinder.getCurrentScreenSizeWithoutBounds().height) {
            allSettingsWindow.setSize(allSettingsWindow.getWidth(), ScreenFinder.getCurrentScreenSizeWithoutBounds().height - 5);
        }
        allSettingsWindow.setVisible(true);
    }

    public void adjustAmperLabels(JLabel ampersResult1, JLabel ampersResult2, JSpinner singleLedAmpers, JSpinner singleSourceAmpers, JSpinner numberOfSources) {
        if (gp != null) {
            ampersResult1.setText(gp.getGrid().getWallAmpersSentence((double) (singleLedAmpers.getValue())));
            ampersResult2.setText(gp.getGrid().getSingleSourceRowAmpersSentence((double) (singleSourceAmpers.getValue()), (double) (singleLedAmpers.getValue()), (Integer) (numberOfSources.getValue())));
        } else {
            ampersResult1.setText(Translator.R("BnoWall"));
            ampersResult2.setText(Translator.R("BnoWall"));
        }
    }

    private void setRemoteSecuirty() {
        setRemoteSecuirty(remoteSecurityButton, remoteSecurityStatus);
    }

    private void setRemoteSecuirty(JButton remoteSecurityButton, JLabel remoteSecurityStatus) {
        if (Files.remotePass.exists()) {
            remoteSecurityStatus.setText(Translator.R("cInsecure"));
            remoteSecurityButton.setText(Translator.R("cDelete"));
            ActionListener[] q = remoteSecurityButton.getActionListeners();
            for (ActionListener q1 : q) {
                remoteSecurityButton.removeActionListener(q1);
            }
            remoteSecurityButton.addActionListener(deleteCokie);
        } else {
            remoteSecurityStatus.setText(Translator.R("cSecure"));
            remoteSecurityButton.setText(Translator.R("cUnlock"));
            ActionListener[] q = remoteSecurityButton.getActionListeners();
            for (ActionListener q1 : q) {
                remoteSecurityButton.removeActionListener(q1);
            }
            remoteSecurityButton.addActionListener(createCookie);
        }
    }

    private class PathColorCompozitorListener implements ChangeListener {

        private final JSpinner sss;
        private final int i;
        private final GridPane gp;
        private final Component prev;

        private PathColorCompozitorListener(int i, GridPane gp, JSpinner sss, Component prev) {
            this.sss = sss;
            this.i = i;
            this.gp = gp;
            this.prev = prev;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            JSpinner s = (JSpinner) e.getSource();
            gs.setPart(((Double) (s.getValue())), i);
            Color c = Color.WHITE;
            if (i >= 0 && i < 3) {
                c = gs.getStartColor();
            } else if (i >= 3 && i < 6) {
                c = gs.getPathColor();
            } else if (i >= 6 && i < 9) {
                c = gs.getTopColor();
            }
            prev.setBackground(c);
            prev.repaint();
            gs.setBrightness(((Integer) sss.getValue()));
            gp.repaintAndSend(gs);
        }
    }
}
