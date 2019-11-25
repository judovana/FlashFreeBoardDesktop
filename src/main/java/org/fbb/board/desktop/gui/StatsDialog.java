/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import org.fbb.board.Translator;
import org.fbb.board.desktop.ScreenFinder;
import org.fbb.board.internals.GlobalSettings;
import org.fbb.board.internals.ListWithFilter;
import org.fbb.board.internals.db.DB;
import org.fbb.board.internals.grades.Grade;
import org.fbb.board.internals.grid.Boulder;
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
        return jdb;
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
