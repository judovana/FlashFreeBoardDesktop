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
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import org.fbb.board.Updater;
import org.fbb.board.desktop.Files;
import org.fbb.board.desktop.ScreenFinder;
import org.fbb.board.desktop.gui.awtimpl.WinUtils;
import org.fbb.board.desktop.gui.dialogs.LogView;
import org.fbb.board.internals.FUtils;
import org.fbb.board.internals.GlobalSettings;
import org.fbb.board.internals.GuiLogHelper;
import org.fbb.board.internals.comm.ConnectionID;
import org.fbb.board.internals.db.DB;
import org.fbb.board.internals.db.GuiExceptionHandler;
import org.fbb.board.internals.db.Puller;
import org.fbb.board.internals.grid.GridPane;
import org.jasypt.util.text.StrongTextEncryptor;

/**
 *
 * @author jvanek
 */
public class SettingsListener implements ActionListener {

    public static final StrongTextEncryptor textEncryptor = new StrongTextEncryptor();

    static {
        textEncryptor.setPassword("IwasForcedToDoSo");
    }

    private final String wall;
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
                    String myEncryptedText = textEncryptor.encrypt(p);
                    java.nio.file.Files.write(Files.remotePass.toPath(), myEncryptedText.getBytes(Charset.forName("utf-8")));
                    JOptionPane.showMessageDialog(null, Translator.R("cReated", Files.remotePass.getAbsolutePath()));
                    Set<PosixFilePermission> perms = new HashSet<>();
                    perms.add(PosixFilePermission.OWNER_READ);
                    perms.add(PosixFilePermission.OWNER_WRITE);
                    java.nio.file.Files.setPosixFilePermissions(Files.remotePass.toPath(), perms);
                    JOptionPane.showMessageDialog(null, Translator.R("cSecured", Files.remotePass.getAbsolutePath()));
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                    JOptionPane.showMessageDialog(null, ex);
                }
            }
            setRemoteSecuirty();
        }
    };

    public SettingsListener(GridPane gp, Authenticator auth, GlobalSettings gs, Puller puller, DB db, int selectedTab, String wall) {
        this.gp = gp;
        this.auth = auth;
        this.gs = gs;
        this.puller = puller;
        this.db = db;
        this.selectedTab = selectedTab;
        this.wall = wall;
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
        int genRows = 8;
        int conRows = 2;
        int colRows = 13;
        int remRows = 10;
        int ampRows = 6;
        int upRows = 6;
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
        JPanel update = new JPanel(new GridLayout(maxRows, 2));
        update.setName(Translator.R("UpdateTab"));
        settingsTabs.add(general);
        settingsTabs.add(connection);
        settingsTabs.add(colors);
        settingsTabs.add(remote);
        settingsTabs.add(amps);
        settingsTabs.add(update);
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
        JButton deleteBouldrs = new JButton(Translator.R("SelectListBoulders"));
        JButton logs = new JButton("logs");
        if (wall == null) {
            deleteBouldrs.setEnabled(false);
        }
        general.add(deleteBouldrs);
        general.add(logs);
        logs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new LogView(db, true).setVisible(true);
            }
        });
        deleteBouldrs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                new BoulderFiltering(db, gs).selectListBouderAsAdmin(wall, allSettingsWindow);
            }
        });
        JCheckBox resizeable = new JCheckBox(Translator.R("resizeable"), gs.isResizeAble());
        general.add(resizeable);
        resizeable.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gs.setResizeAble(resizeable.isSelected(), true);
            }
        });
        JCheckBox popupping = new JCheckBox(Translator.R("popupping"), gs.isPopUpping());
        general.add(popupping);
        popupping.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gs.setPopupping(popupping.isSelected(), true);
            }
        });
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
        remote.add(new JLabel(Translator.R("tokenInfo")));
        remote.add(new JLabel("https://github.com/name/repo.git or https://token@github.com/name/repo.git"));
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
        final JTextField updateStatus0 = new JTextField();
        final JTextField updateStatus1 = new JTextField();
        final JTextField updateStatus2 = new JTextField();
        final JButton updateButton = new JButton(Translator.R("checkUpdate"));
        final JButton doUpdate1 = new JButton(Translator.R("OnlyDownload"));
        final JButton doUpdate2 = new JButton(Translator.R("DownloadAndRemove"));
        doUpdate1.setEnabled(false);
        doUpdate2.setEnabled(false);
        final JCheckBox allowDowngrade = new JCheckBox(Translator.R("AllowDown"), false);
        final JCheckBox allowReplace = new JCheckBox(Translator.R("AllowReplace"), false);
        allowReplace.setEnabled(false);
        final JTextField arduino = new JTextField();
        final JButton arduinoWork = new JButton("arduino");
        arduino.setEnabled(false);
        arduinoWork.setEnabled(false);
        final JButton web = new JButton("web");
        web.addActionListener(new WinUtils.ShowWebHelp());
        final JButton otherAssets = new JButton(Translator.R("OAses"));
        otherAssets.setEnabled(false);

        allowDowngrade.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isWindows()) {
                    allowReplace.setEnabled(allowDowngrade.isSelected());
                    if (!allowDowngrade.isSelected()) {
                        allowReplace.setSelected(false);
                    }
                }
            }
        });
        updateButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final Updater.Update update = Updater.getUpdatePossibility(allowReplace.isSelected(), allowDowngrade.isSelected());
                clean(doUpdate1);
                clean(doUpdate2);
                clean(arduinoWork);
                clean(otherAssets);
                if (update == null) {
                    doUpdate1.setEnabled(false);
                    doUpdate2.setEnabled(false);
                    arduinoWork.setEnabled(false);
                    otherAssets.setEnabled(false);
                    updateStatus1.setText(Translator.R("UpdateImpossible"));
                    arduino.setText(Translator.R("UpdateImpossible"));
                    updateStatus2.setText(Translator.R("SeeLaogs"));
                } else {
                    if (!update.getOtherAssets().isEmpty()) {
                        otherAssets.setEnabled(true);
                        otherAssets.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                new OtherAssetsDialog(update.getOtherAssets(), OtherAssetsDialog.defaultpaths).setVisible(true);
                            }
                        });
                    }
                    arduinoWork.setEnabled(true);
                    arduinoWork.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            try {
                                File f = update.downloadArduino();
                                System.out.println(f.getAbsolutePath());
                                List<String> ard = java.nio.file.Files.readAllLines(f.toPath());
                                StringBuilder sb = new StringBuilder();
                                for (String string : ard) {
                                    System.out.println(string);
                                    sb.append(string).append("\n");
                                }
                                new ArduinoWindow(f, sb.toString(), gs.getPortId()).setVisible(true);
                            } catch (Exception ex) {
                                GuiLogHelper.guiLogger.loge(ex);
                                JOptionPane.showMessageDialog(null, ex);
                            }
                        }
                    });
                    arduino.setText(update.getRemoteArduino().toExternalForm());
                    doUpdate1.setEnabled(true);
                    doUpdate1.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                update.downloadJar();
                                JOptionPane.showMessageDialog(null, Translator.R("Downloaded", " \n " + update.getDwnldTarget().getAbsolutePath()));
                            } catch (Exception ex) {
                                GuiLogHelper.guiLogger.loge(ex);
                                JOptionPane.showMessageDialog(null, ex);
                            }
                        }
                    });
                    if (!isWindows()) {
                        doUpdate2.setEnabled(true);
                        doUpdate2.addActionListener(new ActionListener() {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    update.downloadJar();
                                    JOptionPane.showMessageDialog(null, Translator.R("Downloaded", " \n " + update.getDwnldTarget().getAbsolutePath()));
                                    if (update.getDwnldTarget().getAbsoluteFile().equals(update.getLocal().getAbsoluteFile())) {
                                        JOptionPane.showMessageDialog(null, Translator.R("Restart"));
                                        return;
                                    }
                                    boolean deleted = update.getLocal().delete();
                                    if (deleted) {
                                        JOptionPane.showMessageDialog(null, Translator.R("Erased", " \n " + update.getLocal().getAbsolutePath()));
                                        JOptionPane.showMessageDialog(null, Translator.R("Restart"));
                                    } else {
                                        JOptionPane.showMessageDialog(null, Translator.R("EraseFailed", " \n " + update.getLocal().getAbsolutePath()));
                                        JOptionPane.showMessageDialog(null, Translator.R("RestartFail"));
                                    }
                                } catch (Exception ex) {
                                    GuiLogHelper.guiLogger.loge(ex);
                                    JOptionPane.showMessageDialog(null, ex);
                                }
                            }
                        });
                    }
                    updateStatus1.setText(Translator.R("CurrentVersion", update.getLocalVersion(), update.getLocal().getAbsolutePath()));
                    updateStatus2.setText(Translator.R("RemoteVersion", update.getRemoteVersion(), update.getRemoteJar().toExternalForm()));
                    updateStatus1.setCaretPosition(0);
                    updateStatus2.setCaretPosition(0);
                }

            }

            private void clean(JButton b) {
                ActionListener[] ls = b.getActionListeners();
                for (ActionListener l : ls) {
                    b.removeActionListener(l);
                }
            }
        });
        update.add(updateStatus0);
        update.add(updateButton);
        update.add(updateStatus1);
        update.add(updateStatus2);
        update.add(doUpdate1);
        update.add(doUpdate2);
        update.add(allowDowngrade);
        update.add(allowReplace);
        update.add(arduino);
        update.add(arduinoWork);
        update.add(web);
        update.add(otherAssets);
        FUtils.align(genRows, maxRows, general);
        FUtils.align(conRows, maxRows, connection);
        FUtils.align(colRows, maxRows, colors);
        FUtils.align(remRows, maxRows, remote);
        FUtils.align(ampRows, maxRows, amps);
        FUtils.align(upRows, maxRows, update);
        allSettingsWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        allSettingsWindow.pack();
        //intentioanlly after pack
        Updater.Update cur = Updater.getCurrentVersionInfo();
        if (cur != null) {
            updateStatus0.setText(cur.getLocalVersion() + " " + cur.getLocalFileName() + " " + cur.getLocal().getAbsolutePath());
            updateStatus0.setCaretPosition(0);
        } else {
            updateStatus0.setText(Translator.R("UnknownVersion"));
        }

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

    private static final String OS = System.getProperty("os.name").toLowerCase();

    public static boolean isWindows() {
        return OS.contains("win");
    }
}
