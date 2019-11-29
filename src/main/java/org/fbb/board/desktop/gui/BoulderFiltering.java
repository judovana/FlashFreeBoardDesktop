/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui;

import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.components.DateTimePicker;
import com.github.lgooddatepicker.components.TimePickerSettings;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.fbb.board.Translator;
import org.fbb.board.desktop.Files;
import org.fbb.board.internals.Filter;
import org.fbb.board.internals.GlobalSettings;
import org.fbb.board.internals.GuiLogHelper;
import org.fbb.board.internals.ListWithFilter;
import org.fbb.board.internals.db.DB;
import org.fbb.board.internals.grades.Grade;
import org.fbb.board.internals.grid.Boulder;
import org.fbb.board.internals.grid.GridPane;

/**
 *
 * @author jvanek
 */
public class BoulderFiltering {

    private final DB db;
    private final GlobalSettings gs;
    private final long day = (long) (24 * 60 * 60 * 1000);
    private final long week = 7l * day;
    private final long month = 31l * day;
    private final long threeMmonth = 3l * month;

    public BoulderFiltering(DB db, GlobalSettings gs) {
        this.db = db;
        this.gs = gs;
    }

    public static class BoulderListAndIndex {

        private final int seelctedIndex;
        private final Boulder selctedValue;
        private final List<Boulder> list;

        public BoulderListAndIndex(int seelctedIndex, Boulder selctedValue, List<Boulder> list) {
            this.seelctedIndex = seelctedIndex;
            this.selctedValue = selctedValue;
            this.list = list;
        }

        public List<Boulder> getList() {
            return list;
        }

        public Boulder getSelctedValue() {
            return selctedValue;
        }

        public int getSeelctedIndex() {
            return seelctedIndex;
        }
        

    }

    public BoulderListAndIndex selectListBouder(String wallId) {
        try {
            return selectListBouderImpl(wallId, false);
        } catch (Exception ex) {
            GuiLogHelper.guiLogger.loge(ex);
            JOptionPane.showMessageDialog(null, ex);
            return null;
        }
    }

    BoulderListAndIndex selectListBouderAsAdmin(String wallId) {
        try {
            return selectListBouderImpl(wallId, true);
        } catch (Exception ex) {
            GuiLogHelper.guiLogger.loge(ex);
            JOptionPane.showMessageDialog(null, ex);
            return null;
        }
    }

    private BoulderListAndIndex selectListBouderImpl(String wallID, final boolean allowDelete) throws IOException {
        final int[] result = new int[]{0};
        final int ALL = 1;
        final int SEL = 2;
        final Map<String, GridPane.Preload> wallCache = new HashMap();
        JDialog d = new JDialog((JDialog) null, Translator.R("selectBoulderCaption"), true);
        d.setSize(750, 600);
        d.setLocation(
                (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() - d.getWidth()) / 2,
                (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() - d.getHeight()) / 2);
        d.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        final JList<Boulder> boulders = new JList();
        final JPanel boulderPreview = new JPanel(new BorderLayout());
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(boulders), boulderPreview);
        d.add(sp);
        boulders.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                try {
                    boulderPreview.removeAll();
                    Boulder b = boulders.getSelectedValue();
                    if (b != null) {
                        //disable repainting of boulder on real world?
                        GridPane.Preload prelaod = wallCache.get(b.getWall());
                        if (prelaod == null) {
                            prelaod = GridPane.preload(new ZipInputStream(new FileInputStream(Files.getWallFile(b.getWall()))), b.getWall());
                            wallCache.put(b.getWall(), prelaod);
                        }
                        GridPane gdp = new GridPane(ImageIO.read(new ByteArrayInputStream(prelaod.img)), prelaod.props, gs);
                        gdp.disableClicking();
                        gdp.getGrid().setBouler(b);
                        boulderPreview.add(gdp);
                        boulderPreview.validate();
                    }
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                    JOptionPane.showMessageDialog(boulders, ex);
                }
            }
        });
        //delete boulders? made walls editabel to allow old boulders cleanup? delete walls?
        JPanel resultsPanel1 = new JPanel(new GridLayout(1, 2));
        JPanel resultsPanel2 = new JPanel(new GridLayout(1, 2));
        JButton addAll = new JButton(Translator.R("BAddAll"));
        addAll.setFont(addAll.getFont().deriveFont(Font.BOLD | Font.ITALIC));
        resultsPanel1.add(addAll);
        addAll.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Files.lastUsedToLastApplied();
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                    JOptionPane.showMessageDialog(d, ex);
                }
                result[0] = ALL;
                d.setVisible(false);
            }
        });
        JButton addSeelcted = new JButton(Translator.R("BAddSel"));
        addSeelcted.setFont(addSeelcted.getFont().deriveFont(Font.BOLD));
        resultsPanel1.add(addSeelcted);
        addSeelcted.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Files.lastUsedToLastApplied();
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                    JOptionPane.showMessageDialog(d, ex);
                }
                result[0] = SEL;
                d.setVisible(false);
            }
        });
        JButton deleteAll = new JButton(Translator.R("delAll"));
        deleteAll.setFont(deleteAll.getFont().deriveFont(Font.PLAIN));
        JButton delteSelected = new JButton(Translator.R("delSel"));
        delteSelected.setFont(delteSelected.getFont().deriveFont(Font.PLAIN));
        resultsPanel2.add(deleteAll);
        resultsPanel2.add(delteSelected);
        JPanel tools0 = new JPanel(new BorderLayout());
        JPanel tools1 = new JPanel(new GridLayout(1, 3));
        JPanel tools2 = new JPanel(new GridLayout(1, 3));
        JPanel tools3 = new JPanel(new BorderLayout());
        JPanel tools4 = new JPanel(new GridLayout(1, 3));
        JPanel tools4dates = new JPanel(new GridLayout(1, 4));
        JPanel tools5 = new JPanel(new BorderLayout());
        JPanel tools6 = new JPanel(new GridLayout(1, 3));
        JButton lastApplied = new JButton(Translator.R("BLastAppliedFilter"));
        tools6.add(lastApplied);
        JButton lastUsed = new JButton(Translator.R("BLastUsedFilter"));
        tools6.add(lastUsed);
        JButton wallDefault = new JButton(Translator.R("BWallDefault"));
        tools6.add(wallDefault);
        tools0.add(new JLabel(Translator.R("Wall")));
        final JComboBox<String> walls = new JComboBox(Files.wallsDir.list());
        tools0.add(walls);
        final JCheckBox random = new JCheckBox(Translator.R("random"), true);
        tools0.add(random, BorderLayout.EAST);
        if (!Authenticator.auth.isPernament()) {
            random.setEnabled(false);
            walls.setEnabled(false);
        }
        final JLabel dificultyLabel = new JLabel(Translator.R("DificultyInterval"));
        tools1.add(dificultyLabel);
        dificultyLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(d, dificultyLabel.getToolTipText());
            }
        });
        final JComboBox<String> gradesFrom = new JComboBox(Grade.currentGrades());
        final JComboBox<String> gradesTo = new JComboBox(Grade.currentGrades());
        gradesFrom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dificultyLabel.setToolTipText("<html>"
                        + Grade.getStandardTooltip(gradesFrom.getSelectedIndex())
                        + "-----<br>"
                        + Grade.getStandardTooltip(gradesTo.getSelectedIndex())
                );
            }
        });
        gradesTo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dificultyLabel.setToolTipText("<html>"
                        + Grade.getStandardTooltip(gradesFrom.getSelectedIndex())
                        + "-----<br>"
                        + Grade.getStandardTooltip(gradesTo.getSelectedIndex())
                );
            }
        });
        tools1.add(gradesFrom);
        tools1.add(gradesTo);
        tools2.add(new JLabel(Translator.R("NumberOfHolds") + " " + Translator.R("FromTo")));
        final JSpinner holdsMin = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
        tools2.add(holdsMin);
        final JSpinner holdsMax = new JSpinner(new SpinnerNumberModel(100, 0, 1000, 1));
        tools2.add(holdsMax);
        JLabel nameLabel = new JLabel(Translator.R("NameFilter"));
        tools3.add(nameLabel, BorderLayout.WEST);
        final JTextField nameFilter = new JTextField();
        tools3.add(nameFilter);
        final JLabel authorLabel = new JLabel(Translator.R("AuthorFilter"));
        authorLabel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(d, authorLabel.getToolTipText());
            }

        });
        tools5.add(authorLabel, BorderLayout.WEST);
        final JTextField authorsFilter = new JTextField();
        tools5.add(authorsFilter);
        tools4.add(new JLabel(Translator.R("Date") + " [dd/MM/YYYY HH:mm] " + Translator.R("FromTo")));
        TimePickerSettings timeSettings1 = new TimePickerSettings();
        timeSettings1.setFormatForDisplayTime(Filter.hours);
        timeSettings1.setFormatForMenuTimes(Filter.hours);
        DatePickerSettings dateSettings1 = new DatePickerSettings();
        dateSettings1.setFormatForDatesCommonEra(Filter.days);
        dateSettings1.setFormatsForParsing(Filter.dtdtgs());
        TimePickerSettings timeSettings2 = new TimePickerSettings();
        timeSettings2.setFormatForDisplayTime(Filter.hours);
        timeSettings2.setFormatForMenuTimes(Filter.hours);
        DatePickerSettings dateSettings2 = new DatePickerSettings();
        dateSettings2.setFormatForDatesCommonEra(Filter.days);
        dateSettings2.setFormatsForParsing(Filter.dtdtgs());
        final DateTimePicker dateFrom = new DateTimePicker(dateSettings1, timeSettings1);
        tools4.add(dateFrom);
        final DateTimePicker dateTo = new DateTimePicker(dateSettings2, timeSettings2);
        tools4.add(dateTo);
        JButton lastDay = new JButton(Translator.R("LastDay"));
        JButton lastWeek = new JButton(Translator.R("LastWeek"));
        JButton lastMonth = new JButton(Translator.R("LastMonth"));
        JButton lastTreeMonths = new JButton(Translator.R("LastThreeMonths"));
        tools4dates.add(lastDay);
        tools4dates.add(lastWeek);
        tools4dates.add(lastMonth);
        tools4dates.add(lastTreeMonths);

        lastDay.addActionListener(new QuickPAstSelectListener(day, dateFrom, dateTo));
        lastWeek.addActionListener(new QuickPAstSelectListener(week, dateFrom, dateTo));
        lastMonth.addActionListener(new QuickPAstSelectListener(month, dateFrom, dateTo));
        lastTreeMonths.addActionListener(new QuickPAstSelectListener(threeMmonth, dateFrom, dateTo));
        wallDefault.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                resetDefaults(wallID, walls, boulders, holdsMin, holdsMax, authorLabel, dateFrom, dateTo, gradesFrom, gradesTo, authorsFilter, nameFilter, random);
            }
        });
        resetDefaults(wallID, walls, boulders, holdsMin, holdsMax, authorLabel, dateFrom, dateTo, gradesFrom, gradesTo, authorsFilter, nameFilter, random);

        JPanel tools = new JPanel(new GridLayout(9, 1));
        tools.add(tools0);
        tools.add(tools1);
        tools.add(tools2);
        tools.add(tools4);
        tools.add(tools4dates);
        tools.add(tools3);
        tools.add(tools5);
        tools.add(tools6);
        final JButton apply = new JButton("********* " + Translator.R("Apply").toUpperCase() + " *********");
        tools.add(apply);
        d.add(tools, BorderLayout.NORTH);
        JPanel resultsPanel = new JPanel(new GridLayout(2, 1));
        resultsPanel.add(resultsPanel1);
        resultsPanel.add(resultsPanel2);
        d.add(resultsPanel, BorderLayout.SOUTH);
        if (!Authenticator.auth.isPernament()) {
            resultsPanel2.setVisible(false);
            walls.setVisible(false);
            random.setVisible(false);
        }
        if (allowDelete) {
            resultsPanel2.setVisible(true);
            walls.setVisible(true);
            random.setVisible(true);
        }
        boulders.setCellRenderer(new BoulderListRenderer());
        lastApplied.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (Files.getLastAppliedFilterFile().exists()) {
                        Filter f = Filter.load(Files.getLastAppliedFilterFile());
                        if (f != null) {
                            applyFilter(f, wallID, walls, holdsMin, holdsMax, dateFrom, dateTo, gradesFrom, gradesTo, authorsFilter, nameFilter, random);
                        }
                    }
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                    JOptionPane.showMessageDialog(boulders, ex);
                }
            }
        });
        lastUsed.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (Files.getLastUsedFilterFile().exists()) {
                        Filter f = Filter.load(Files.getLastUsedFilterFile());
                        if (f != null) {
                            applyFilter(f, wallID, walls, holdsMin, holdsMax, dateFrom, dateTo, gradesFrom, gradesTo, authorsFilter, nameFilter, random);
                        }
                    }
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                    JOptionPane.showMessageDialog(boulders, ex);
                }
            }
        });
        apply.addActionListener(new ApplyFilterListener(walls, gradesFrom, gradesTo, holdsMin, holdsMax, authorsFilter, nameFilter, dateFrom, dateTo, boulders, random));
        sp.setDividerLocation((int) ((double) (d.getWidth() * 2) / 3));
        wallDefault.setFont(addAll.getFont().deriveFont(Font.PLAIN));
        deleteAll.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (boulders.getModel() == null || boulders.getModel().getSize() == 0) {
                    return;
                }
                if (Authenticator.auth.isPernament() || allowDelete) {
                    int y = JOptionPane.showConfirmDialog(d, Translator.R("delConf", boulders.getModel().getSize()));
                    if (y != JOptionPane.YES_OPTION) {
                        return;
                    }
                } else {
                    try {
                        Authenticator.auth.authenticate(Translator.R("delConf", boulders.getModel().getSize()));
                    } catch (Authenticator.AuthoriseException ex) {
                        JOptionPane.showMessageDialog(d, ex);
                        return;
                    }

                }
                try {
                    File[] toDelete = new File[boulders.getModel().getSize()];
                    for (int x = boulders.getModel().getSize() - 1; x >= 0; x--) {
                        Boulder i = boulders.getModel().getElementAt(x);
                        toDelete[x] = i.getFile();
                    }
                    String appendix = "";
                    if (toDelete.length == 1) {
                        appendix = toDelete[0].getName();
                    }
                    db.delte(appendix, toDelete);
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                    JOptionPane.showMessageDialog(d, ex);
                } finally {
                    apply.getActionListeners()[0].actionPerformed(null);
                }

            }
        });
        delteSelected.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (boulders.getSelectedValuesList() == null || boulders.getSelectedValuesList().isEmpty()) {
                    return;
                }
                if (Authenticator.auth.isPernament() || allowDelete) {
                    int y = JOptionPane.showConfirmDialog(d, Translator.R("delConf", boulders.getSelectedValuesList().size()));
                    if (y != JOptionPane.YES_OPTION) {
                        return;
                    }
                } else {
                    try {
                        Authenticator.auth.authenticate(Translator.R("delConf", boulders.getSelectedValuesList().size()));
                    } catch (Authenticator.AuthoriseException ex) {
                        JOptionPane.showMessageDialog(d, ex);
                        return;
                    }

                }
                try {
                    File[] toDelete = new File[boulders.getSelectedValuesList().size()];
                    for (int i = 0; i < boulders.getSelectedValuesList().size(); i++) {
                        Boulder get = boulders.getSelectedValuesList().get(i);
                        toDelete[i] = get.getFile();
                    }
                    String appendix = "";
                    if (toDelete.length == 1) {
                        appendix = toDelete[0].getName();
                    }
                    db.delte(appendix, toDelete);
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                    JOptionPane.showMessageDialog(d, ex);
                } finally {
                    apply.getActionListeners()[0].actionPerformed(null);
                }

            }
        });
        d.setVisible(true);
        if (boulders.getModel().getSize() == 0) {
            d.dispose();
            return null;
        }
        switch (result[0]) {
            case SEL: {
                BoulderListAndIndex r = new BoulderListAndIndex(-1/*misleading*/, null/*misleading, first clicked, not last*/, boulders.getSelectedValuesList());
                d.dispose();
                return r;
            }
            case ALL: {
                BoulderListAndIndex r = new BoulderListAndIndex(boulders.getSelectedIndex() >= 0 ? boulders.getSelectedIndex() : boulders.getModel().getSize() - 1, boulders.getSelectedValue(), getAll(boulders.getModel()));
                d.dispose();
                return r;
            }
            default:
                d.dispose();
                return null;
        }
    }

    public static ListWithFilter resetDefaults(String wallID, final JComboBox<String> walls, final JList<Boulder> boulders, final JSpinner holdsMin, final JSpinner holdsMax, final JLabel authorLabel, final DateTimePicker dateFrom, final DateTimePicker dateTo, final JComboBox<String> gradesFrom, final JComboBox<String> gradesTo, JTextField author, JTextField name, JCheckBox random) {
        ListWithFilter currentList = new ListWithFilter(wallID);
        walls.setSelectedItem(wallID);
        boulders.setModel(new DefaultComboBoxModel<>(currentList.getHistory()));
        if (boulders.getModel().getSize() > 0) {
            boulders.ensureIndexIsVisible(boulders.getModel().getSize() - 1);
        }
        holdsMin.setValue(currentList.getShortest());
        holdsMax.setValue(currentList.getLongest());
        authorLabel.setToolTipText(currentList.getAuthors());
        LocalDateTime ldtFrom = Instant.ofEpochMilli(currentList.getOldest().getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        LocalDateTime ldtTo = Instant.ofEpochMilli(currentList.getYoungest().getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        dateFrom.setDateTimeStrict(ldtFrom);
        dateTo.setDateTimeStrict(ldtTo);
        gradesFrom.setSelectedItem(currentList.getEasiest().toString());
        gradesTo.setSelectedItem(currentList.getHardest().toString());
        author.setText("");
        name.setText("");
        random.setSelected(true);
        return currentList;
    }

    public static void applyFilter(Filter f, String wallID, final JComboBox<String> walls, final JSpinner holdsMin, final JSpinner holdsMax, final DateTimePicker dateFrom, final DateTimePicker dateTo, final JComboBox<String> gradesFrom, final JComboBox<String> gradesTo, JTextField author, JTextField name, JCheckBox random) {
        walls.setSelectedItem(f.wall);
        holdsMin.setValue(f.pathMin);
        holdsMax.setValue(f.pathTo);
        LocalDateTime ldtFrom = Instant.ofEpochMilli(f.ageFrom)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        LocalDateTime ldtTo = Instant.ofEpochMilli(f.ageTo)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        dateFrom.setDateTimeStrict(ldtFrom);
        dateTo.setDateTimeStrict(ldtTo);
        gradesFrom.setSelectedItem(new Grade(f.gradeFrom).toString());
        gradesTo.setSelectedItem(new Grade(f.gradeTo).toString());
        author.setText(f.getAuthorsString());
        name.setText(f.getNamesString());
        random.setSelected(f.random);

    }

    private static class BoulderListRenderer extends JLabel implements ListCellRenderer<Boulder> {

        public BoulderListRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Boulder> list, Boulder b, int index,
                boolean isSelected, boolean cellHasFocus) {
            this.setFont(this.getFont().deriveFont(Font.PLAIN, new JLabel().getFont().getSize() + 2));
            String grade = b.getGrade().toString();
            setText("<html><big><b>" + grade + "</b>:  <u>" + b.getName() + "</u>| <i>" + b.getAuthor() + "</i> (" + Filter.dtf.format(b.getDate()) + ")[" + b.getWall() + "]");

            if (isSelected) {
//                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
                setBackground(new Color(225, 0, 0));

            } else {
                int inter = 255 / Grade.currentGrades().size();
                if (b.getGrade().toNumber() < 0) {
                    setBackground(new Color(0, 250, 0));
                } else {
                    setBackground(new Color(b.getGrade().toNumber() * inter, 255, 255));
                }
                setForeground(list.getForeground());
            }

            return this;
        }
    }

    private static class ApplyFilterListener implements ActionListener {

        private final JComboBox<String> walls;
        private final JComboBox<String> gradesFrom;
        private final JComboBox<String> gradesTo;
        private final JSpinner holdsMin;
        private final JSpinner holdsMax;
        private final JTextField authorsFilter;
        private final JTextField nameFilter;
        private final DateTimePicker dateFrom;
        private final DateTimePicker dateTo;
        private final JList<Boulder> boulders;
        private ListWithFilter lastList;
        private final JCheckBox random;

        public ListWithFilter getLastList() {
            return lastList;
        }

        public ApplyFilterListener(JComboBox<String> walls, JComboBox<String> gradesFrom, JComboBox<String> gradesTo, JSpinner holdsMin, JSpinner holdsMax, JTextField authorsFilter, JTextField nameFilter, DateTimePicker dateFrom, DateTimePicker dateTo, JList<Boulder> boulders, JCheckBox random) {
            this.walls = walls;
            this.gradesFrom = gradesFrom;
            this.gradesTo = gradesTo;
            this.holdsMin = holdsMin;
            this.holdsMax = holdsMax;
            this.authorsFilter = authorsFilter;
            this.nameFilter = nameFilter;
            this.dateFrom = dateFrom;
            this.dateTo = dateTo;
            this.boulders = boulders;
            this.random = random;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                lastList = new ListWithFilter(
                        new Filter(
                                (String) walls.getSelectedItem(),
                                gradesFrom.getSelectedIndex(),
                                gradesTo.getSelectedIndex(),
                                (Integer) (holdsMin.getValue()),
                                (Integer) (holdsMax.getValue()),
                                authorsFilter.getText(),
                                nameFilter.getText(),
                                Date.from(dateFrom.getDateTimeStrict().atZone(ZoneId.systemDefault()).toInstant()),
                                Date.from(dateTo.getDateTimeStrict().atZone(ZoneId.systemDefault()).toInstant()),
                                random.isSelected())
                );
                lastList.getLastFilter().save(Files.getLastAppliedFilterFile());
                boulders.setModel(new DefaultComboBoxModel<>(lastList.getHistory()));
                if (boulders.getModel().getSize() > 0) {
                    boulders.ensureIndexIsVisible(boulders.getModel().getSize() - 1);
                }
            } catch (Exception ex) {
                GuiLogHelper.guiLogger.loge(ex);
                JOptionPane.showMessageDialog(dateFrom, ex);
            }
        }
    }

    private List<Boulder> getAll(ListModel<Boulder> model) {
        ArrayList<Boulder> r = new ArrayList(model.getSize());
        for (int i = 0; i < model.getSize(); i++) {
            r.add(model.getElementAt(i));
        }
        return r;
    }

    private static class QuickPAstSelectListener implements ActionListener {

        private final long past;
        private final DateTimePicker df;
        private final DateTimePicker dt;

        public QuickPAstSelectListener(long past, DateTimePicker df, DateTimePicker dt) {
            this.past = past;
            this.df = df;
            this.dt = dt;
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            df.setDateTimeStrict(
                    Instant.ofEpochMilli(new Date().getTime() - past).atZone(ZoneId.systemDefault()).toLocalDateTime()
            );
            dt.setDateTimeStrict(
                    Instant.ofEpochMilli(new Date().getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime()
            );
        }
    }

}
