/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipInputStream;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.fbb.board.Translator;
import org.fbb.board.desktop.Files;
import org.fbb.board.desktop.ScreenFinder;
import org.fbb.board.desktop.gui.BoulderFiltering;
import org.fbb.board.internals.GlobalSettings;
import org.fbb.board.internals.ListWithFilter;
import org.fbb.board.internals.db.DB;
import org.fbb.board.internals.grades.Grade;
import org.fbb.board.internals.grid.Boulder;
import org.fbb.board.internals.grid.GridPane;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 *
 * @author jvanek
 */
public class StatsDialog extends JDialog {

    private final JSpinner paging;
    private List<Boulder> lastList;

    public StatsDialog(final String wall, final DB db, final GlobalSettings gs) {
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setModalityType(ModalityType.DOCUMENT_MODAL);
        this.setSize(ScreenFinder.getCurrentScreenSizeWithoutBounds().getSize());
        this.setLocationRelativeTo(null);
        paging = new JSpinner(new SpinnerNumberModel(10, 1, 40, 1));
        paging.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Component[] c = StatsDialog.this.getContentPane().getComponents();
                int selected = 0;
                for (Component cc : c) {
                    if (cc instanceof JTabbedPane) {
                        StatsDialog.this.remove(cc);
                        selected = ((JTabbedPane) cc).getSelectedIndex();
                    }
                }
                createStats(wall, lastList, selected);
                StatsDialog.this.validate();
            }
        });
        this.add(paging, BorderLayout.NORTH);
        JButton filterButton = new JButton(Translator.R("SSfilter"));
        this.add(filterButton, BorderLayout.SOUTH);
        this.add(createStats(wall, new ListWithFilter(wall).getHistory(), 0));
        filterButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                BoulderFiltering.BoulderListAndIndex listAndothers = new BoulderFiltering(db, gs).selectListBouder(wall, StatsDialog.this);
                Component[] c = StatsDialog.this.getContentPane().getComponents();
                int selected = 0;
                for (Component cc : c) {
                    if (cc instanceof JTabbedPane) {
                        StatsDialog.this.remove(cc);
                        selected = ((JTabbedPane) cc).getSelectedIndex();
                    }
                }
                if (listAndothers != null && listAndothers.getList() != null) {
                    if (listAndothers.getList().size() > 0) {
                        StatsDialog.this.add(createStats(listAndothers.getList().get(0).getWall(), listAndothers.getList(), selected));
                    } else {
                        StatsDialog.this.add(createStats(wall, listAndothers.getList(), selected));
                    }
                }
                StatsDialog.this.validate();

            }
        });
    }

    private JScrollPane wrapPanelsToScroll(List<? extends Component> hCharts) {
        if (hCharts.isEmpty()) {
            return new JScrollPane();
        }
        JPanel p = new JPanel(new GridLayout(hCharts.size(), 0));
        for (int i = 0; i < hCharts.size(); i++) {
            Component hChart = hCharts.get(i);
            p.add(hChart);
        }
        JScrollPane scroll = new JScrollPane(p);
        return scroll;

    }

    private JTabbedPane createStats(final String wall, List<Boulder> boulderList, int selected) {
        this.lastList = boulderList;
        JTabbedPane jdb = new JTabbedPane();
        JPanel byDificulty = new JPanel(new BorderLayout());
        JPanel byAutor = new JPanel(new BorderLayout());
        JPanel byHolds = new JPanel(new BorderLayout());
        JPanel byHoldsCount = new JPanel(new BorderLayout());
        JPanel byAuthorAndDificulty = new JPanel(new BorderLayout());
        jdb.add(byDificulty);
        jdb.add(byAutor);
        jdb.add(byHolds);
        jdb.add(byHoldsCount);
        jdb.add(byAuthorAndDificulty);
        jdb.setTitleAt(0, Translator.R("byDiff"));
        jdb.setTitleAt(1, Translator.R("byAuthor"));
        jdb.setTitleAt(2, Translator.R("byHolds"));// charts (0 toppest)by row and stats per line; zoom all acrding to max!
        jdb.setTitleAt(3, Translator.R("byHoldsCount"));
        jdb.setTitleAt(4, Translator.R("byAuthorAndDificulty"));
        this.add(jdb);

        byDificulty.add(wrapPanelsToScroll(createDifficultyChartPannel(wall, boulderList)));

        byAutor.add(wrapPanelsToScroll(createAuthorChartPannel(wall, boulderList)));

        byHoldsCount.add(wrapPanelsToScroll(createLengthChartPannel(wall, boulderList)));

        try {
            ChartPanel[] hCharts = createHoldsChartPannel(wall, boulderList);
            byHolds.add(wrapPanelsToScroll(Arrays.asList(hCharts)));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, ex);
        }

        byAuthorAndDificulty.add(wrapPanelsToScroll(createAuthorsAndDiffs(wall, boulderList)));
        jdb.setSelectedIndex(selected);
        return jdb;
    }

    private ChartPanel[] createHoldsChartPannel(final String wall, List<Boulder> boulderList) throws IOException {
        File f = Files.getWallFile(wall);
        GridPane.Preload preloaded = GridPane.preload(new ZipInputStream(new FileInputStream(f)), f.getName());
        //BufferedImage bi = ImageIO.read(new ByteArrayInputStream(preloaded.img));
        BufferedImage bi = null; //should be ok for headless processing, but maybe needed if pcture of hold is necessary
        GridPane gp = new GridPane(bi, preloaded.props, null);

        int w = gp.getGrid().getWidth();
        int h = gp.getGrid().getHeight();
        //cumulative array for full stats
        List<int[]> allRows = new ArrayList<>(h);
        for (int i = 0; i < h; i++) {
            int[] row = new int[w];
            for (int j = 0; j < row.length; j++) {
                row[j] = 0;
            }
            allRows.add(row);
        }
        int noTops = 0;
        int max = 0;
        for (Boulder b : boulderList) {
            gp.getGrid().setBouler(b);
            //single boulder matrix
            boolean topOnEdge = true;
            List<int[]> rows = gp.getGrid().get();
            for (int i = 0; i < rows.size(); i++) {
                int[] get = rows.get(i);
                for (int j = 0; j < get.length; j++) {
                    if (get[j] > 0) {
                        allRows.get(i)[j]++;
                        if (allRows.get(i)[j] > max) {
                            max = allRows.get(i)[j];
                        }
                        if (get[j] == 3) {
                            topOnEdge = false;
                        }
                    }
                }
            }
            if (topOnEdge) {
                noTops++;
            }
        }
        DefaultCategoryDataset[] cdata = new DefaultCategoryDataset[allRows.size()];
        for (int i = 0; i < allRows.size(); i++) {
            cdata[i] = new DefaultCategoryDataset();
            int[] get = allRows.get(i);
            for (int j = 0; j < get.length; j++) {
                cdata[i].addValue((Number) allRows.get(i)[j], "usage", j);
            }
        }
        ChartPanel[] charts = new ChartPanel[cdata.length + 1];
        DefaultCategoryDataset tops = new DefaultCategoryDataset();
        tops.addValue(noTops, Translator.R("toe"), Translator.R("toe"));
        charts[0] = createDefaultChartPannel(Translator.R("SStitle", wall, boulderList.size()), Translator.R("toe"), Translator.R("toe"), tops);

        for (int i = 0; i < cdata.length; i++) {
            charts[i + 1] = createDefaultChartPannel(Translator.R("SStitle", wall, boulderList.size()), Translator.R("holdId", i), Translator.R("holdUsages"), cdata[i]);
            charts[i + 1].getChart().getCategoryPlot().getRangeAxis(0).setRange(0, max);
        }
        return charts;
    }

    private List<ChartPanel> createLengthChartPannel(final String wall, List<Boulder> boulderList) {
        Map<Integer, LengthAndCount> data = new TreeMap<>();
        int max = 0;
        for (Boulder b : boulderList) {
            Integer l = b.getPathLength();
            LengthAndCount v = data.get(l);
            if (v == null) {
                data.put(l, new LengthAndCount(l, 1));
                if (1 > max) {
                    max = 1;
                }
            } else {
                v.i++;
                if (v.i > max) {
                    max = v.i;
                }
            }
        }
        List<ChartPanel> r = new ArrayList<>();
        List<LengthAndCount> vals = new ArrayList<>(data.values());
        for (int a = 0; a < vals.size(); a += getPaging()) {
            DefaultCategoryDataset cdataA = new DefaultCategoryDataset();
            for (int j = a; j < Math.min(vals.size(), a + getPaging()); j++) {
                LengthAndCount get = vals.get(j);
                cdataA.addValue((Number) get.i, "lenghts", get.l);
            }
            r.add(createDefaultChartPannel(Translator.R("SStitle", wall, boulderList.size()), Translator.R("ssLength"), Translator.R("SScount"), cdataA));
            r.get(r.size() - 1).getChart().getCategoryPlot().getRangeAxis(0).setRange(0, max);
        }

        return r;
    }

    private List<ChartPanel> createDifficultyChartPannel(final String wall, List<Boulder> boulderList) {
        List<ChartPanel> r = new ArrayList<>();
        DefaultCategoryDataset cdataD = new DefaultCategoryDataset();
        int i = -1;
        int one = 0;
        int max = 0;
        for (String s : Grade.currentGrades()) {
            i++;
            int c = 0;
            for (Boulder b : boulderList) {
                if (b.getGrade().equals(new Grade(i))) {
                    c++;
                }
            }
            if (c > max) {
                max = c;
            }
            if (c > 0) {
                one++;
                cdataD.addValue((Number) c, "diff", new Grade.ToStringGradeWrapper(i));
                if (one % getPaging() == 0 || i == Grade.currentGrades().size() - 1) {
                    one = 0;
                    r.add(createDefaultChartPannel(Translator.R("SStitle", wall, boulderList.size()), Translator.R("SSdifficulty"), Translator.R("SScount"), cdataD));
                    cdataD = new DefaultCategoryDataset();
                }
            } else {
                if (i == Grade.currentGrades().size() - 1) {
                    one = 0;
                    r.add(createDefaultChartPannel(Translator.R("SStitle", wall, boulderList.size()), Translator.R("SSdifficulty"), Translator.R("SScount"), cdataD));
                }
            }
        }

        for (ChartPanel chartPanel : r) {
            chartPanel.getChart().getCategoryPlot().getRangeAxis(0).setRange(0, max);
        }
        return r;
    }

    private List<ChartPanel> createAuthorChartPannel(final String wall, List<Boulder> boulderList) {
        Set<String> authors = new HashSet<>();
        for (Boulder boulder : boulderList) {
            authors.add(boulder.getAuthor().toLowerCase().trim());
        }
        List<AuthorAndCount> sortAuthors = new ArrayList<>();
        for (String author : authors) {
            int i = 0;
            for (Boulder boulder : boulderList) {
                if (boulder.getAuthor().toLowerCase().trim().equals(author)) {
                    i++;
                }
            }
            if (boulderList.size() > 0) {
                sortAuthors.add(new AuthorAndCount(author, i));
            }
        }
        Collections.sort(sortAuthors);
        List<ChartPanel> r = new ArrayList<>();
        for (int a = 0; a < sortAuthors.size(); a += getPaging()) {
            DefaultCategoryDataset cdataA = new DefaultCategoryDataset();
            for (int j = a; j < Math.min(sortAuthors.size(), a + getPaging()); j++) {
                AuthorAndCount get = sortAuthors.get(j);
                cdataA.addValue(get.i, "authors", get.a);
            }
            r.add(createDefaultChartPannel(Translator.R("SStitle", wall, boulderList.size()), Translator.R("SSauthor"), Translator.R("SScount"), cdataA));
            r.get(r.size() - 1).getChart().getCategoryPlot().getRangeAxis(0).setRange(0, sortAuthors.get(0).i);
        }
        return r;
    }

    private ChartPanel createDefaultChartPannel(String title, String xLabel, String yLabel, DefaultCategoryDataset data) {
        return createDefaultChartPannel(title, xLabel, yLabel, data, 0);
    }

    private SortableChartPanel createDefaultChartPannel(String title, String xLabel, String yLabel, DefaultCategoryDataset data, int key) {
        JFreeChart auhs = ChartFactory.createBarChart(title, xLabel, yLabel, data);
        auhs.getLegend(0).setVisible(false);
        SortableChartPanel chp2 = new SortableChartPanel(auhs, key);
        chp2.setMinimumDrawHeight(100);
        chp2.setMinimumDrawWidth(100);
        chp2.setMaximumDrawHeight(10000);
        chp2.setMaximumDrawWidth(10000);
        return chp2;
    }

    private List<SortableChartPanel> createAuthorsAndDiffs(String wall, List<Boulder> boulderList) {
        Set<String> authors = new HashSet<>();
        for (Boulder boulder : boulderList) {
            authors.add(boulder.getAuthor().toLowerCase().trim());
        }
        List<SortableChartPanel> r = new ArrayList<>();

        int max = 0;
        for (String author : authors) {
            DefaultCategoryDataset cdataD = new DefaultCategoryDataset();
            int i = -1;
            int cum = 0;
            for (String s : Grade.currentGrades()) {
                i++;
                int c = 0;
                for (Boulder b : boulderList) {
                    if (b.getGrade().equals(new Grade(i)) && b.getAuthor().equalsIgnoreCase(author)) {
                        c++;
                        cum++;
                    }
                }
                if (c > max) {
                    max = c;
                }
                if (c > 0) {
                    cdataD.addValue((Number) c, "diff", new Grade.ToStringGradeWrapper(i));
                } else {
                    //we want empty places and no garabage output
                    cdataD.addValue((Number) c, "diff", new SilentGrade(i));
                }
            }
            r.add(createDefaultChartPannel(author + " - " + cum, Translator.R("SSdifficulty"), Translator.R("SScount"), cdataD, cum));
        }

        for (ChartPanel chartPanel : r) {
            chartPanel.getChart().getCategoryPlot().getRangeAxis(0).setRange(0, max);
        }
        Collections.sort(r);
        return r;
    }

    private static class AuthorAndCount implements Comparable<AuthorAndCount> {

        private final String a;
        private final int i;

        public AuthorAndCount(String a, int i) {
            this.a = a;
            this.i = i;
        }

        @Override
        public int compareTo(AuthorAndCount t) {
            return t.i - this.i;
        }
    }

    private static class LengthAndCount implements Comparable<LengthAndCount> {

        private final int l;
        private int i;

        public LengthAndCount(int l, int i) {
            this.l = l;
            this.i = i;
        }

        @Override
        public int compareTo(LengthAndCount t) {
            return t.l - this.l;
        }
    }

    private static class SilentGrade extends Grade {

        public SilentGrade(int i) {
            super(i);
        }

        @Override
        public String toString() {
            return "";
        }
    }

    private class SortableChartPanel extends ChartPanel implements Comparable<SortableChartPanel> {

        private final int key;

        public SortableChartPanel(JFreeChart chart, int key) {
            super(chart);
            this.key = key;
        }

        @Override
        public int compareTo(SortableChartPanel o) {
            return o.key - key;
        }

    }

    private int getPaging() {
        return (int) paging.getValue();
    }

}
