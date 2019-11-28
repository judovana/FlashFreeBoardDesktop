/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import org.fbb.board.Translator;
import org.fbb.board.desktop.Files;
import org.fbb.board.desktop.ScreenFinder;
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

    public StatsDialog(final String wall, final DB db, final GlobalSettings gs) {
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setModal(true);

        this.setSize(ScreenFinder.getCurrentScreenSizeWithoutBounds().getSize());
        this.setLocationRelativeTo(null);
        JButton filterButton = new JButton(Translator.R("SSfilter"));
        this.add(filterButton, BorderLayout.SOUTH);
        this.add(createStats(wall, new ListWithFilter(wall).getHistory()));
        filterButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                BoulderFiltering.BoulderListAndIndex listAndothers = new BoulderFiltering(db, gs).selectListBouder(wall);
                Component[] c = StatsDialog.this.getContentPane().getComponents();
                for (Component cc : c) {
                    if (cc instanceof JTabbedPane) {
                        StatsDialog.this.remove(cc);
                    }
                }
                if (listAndothers != null && listAndothers.list != null) {
                    if (listAndothers.list.size() > 0) {
                        StatsDialog.this.add(createStats(listAndothers.list.get(0).getWall(), listAndothers.list));
                    } else {
                        StatsDialog.this.add(createStats(wall, listAndothers.list));
                    }
                }
                StatsDialog.this.validate();

            }
        });
    }

    private JTabbedPane createStats(final String wall, List<Boulder> boulderList) {
        JTabbedPane jdb = new JTabbedPane();
        JPanel byDificulty = new JPanel(new BorderLayout());
        JPanel byAutor = new JPanel(new BorderLayout());
        JPanel byHolds = new JPanel(new BorderLayout());
        JPanel byHoldsCount = new JPanel(new BorderLayout());
        jdb.add(byDificulty);
        jdb.add(byAutor);
        jdb.add(byHolds);
        jdb.add(byHoldsCount);
        jdb.setTitleAt(0, Translator.R("byDiff"));
        jdb.setTitleAt(1, Translator.R("byAuthor"));
        jdb.setTitleAt(2, Translator.R("byHolds"));// charts (0 toppest)by row and stats per line; zoom all acrding to max!
        jdb.setTitleAt(3, Translator.R("byHoldsCount"));
        this.add(jdb);

        byDificulty.add(createDifficultyChartPannel(wall, boulderList));

        byAutor.add(createAuthorChartPannel(wall, boulderList));

        byHoldsCount.add(createLengthChartPannel(wall, boulderList));

        try {
            ChartPanel[] hCharts = createHoldsChartPannel(wall, boulderList);
            JPanel p = new JPanel(new GridLayout(hCharts.length + 1, 0));
            for (int i = 0; i < hCharts.length; i++) {
                ChartPanel hChart = hCharts[i];
                p.add(hChart);
            }
            JScrollPane scroll = new JScrollPane(p);
            byHolds.add(scroll);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, ex);
        }
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
                        if (allRows.get(i)[j]>max){
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
                cdata[i].addValue((Number) allRows.get(i)[j], "usge", j);
            }
        }
        ChartPanel[] charts = new ChartPanel[cdata.length + 1];
        DefaultCategoryDataset tops = new DefaultCategoryDataset();
        tops.addValue(noTops, "tops on edge", "tops on edge");
        charts[0] = createDefaultChartPannel(Translator.R("SStitle", wall, boulderList.size()), "tops on edge", "tops on edge", tops);

        for (int i = 0; i < cdata.length; i++) {
            charts[i + 1] = createDefaultChartPannel(Translator.R("SStitle", wall, boulderList.size()), "hold number from left to right - line " + i + " from top", "number of usages", cdata[i]);
            charts[i + 1].getChart().getCategoryPlot().getRangeAxis(0).setRange(0, max);
        }
        return charts;
    }

    private ChartPanel createLengthChartPannel(final String wall, List<Boulder> boulderList) {
        DefaultCategoryDataset cdataD = new DefaultCategoryDataset();
        for (Boulder b : boulderList) {
            Integer i = b.getPathLength();
            try {
                Number n = cdataD.getValue("lenghts", i);
                cdataD.setValue(n.intValue() + 1, "lenghts", i);
            } catch (org.jfree.data.UnknownKeyException e) {
                cdataD.setValue(1, "lenghts", i);
            }

        }

        return createDefaultChartPannel(Translator.R("SStitle", wall, boulderList.size()), Translator.R("ssLength"), Translator.R("SScount"), cdataD);
    }

    private ChartPanel createDifficultyChartPannel(final String wall, List<Boulder> boulderList) {
        DefaultCategoryDataset cdataD = new DefaultCategoryDataset();
        int i = -1;
        for (String s : Grade.currentGrades()) {
            i++;
            int c = 0;
            for (Boulder b : boulderList) {
                if (b.getGrade().equals(new Grade(i))) {
                    c++;
                }
            }
            if (c > 0) {
                cdataD.addValue((Number) c, "diff", new Grade.ToStringGradeWrapper(i));
            }
        }
        return createDefaultChartPannel(Translator.R("SStitle", wall, boulderList.size()), Translator.R("SSdifficulty"), Translator.R("SScount"), cdataD);
    }

    private ChartPanel createAuthorChartPannel(final String wall, List<Boulder> boulderList) {
        DefaultCategoryDataset cdataA = new DefaultCategoryDataset();
        Set<String> authors = new HashSet<>();
        for (Boulder boulder : boulderList) {
            authors.add(boulder.getAuthor().toLowerCase().trim());
        }
        List<AuthorAndCount> sortAuthors = new ArrayList<>();
        for (String author : authors) {
            //if (isNotContained()){
            //on jtextfield, skip...
            //}
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
        for (int j = 0; j < Math.min(sortAuthors.size(), 20); j++) {
            AuthorAndCount get = sortAuthors.get(j);
            cdataA.addValue(get.i, "authors", get.a);
        }
        return createDefaultChartPannel(Translator.R("SStitle", wall, boulderList.size()), Translator.R("SSauthor"), Translator.R("SScount"), cdataA);
    }

    private ChartPanel createDefaultChartPannel(String title, String xLabel, String yLabel, DefaultCategoryDataset data) {
        JFreeChart auhs = ChartFactory.createBarChart(title, xLabel, yLabel, data);
        auhs.getLegend(0).setVisible(false);
        ChartPanel chp2 = new ChartPanel(auhs);
        chp2.setMinimumDrawHeight(100);
        chp2.setMinimumDrawWidth(100);
        chp2.setMaximumDrawHeight(10000);
        chp2.setMaximumDrawWidth(10000);
        return chp2;
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

}
