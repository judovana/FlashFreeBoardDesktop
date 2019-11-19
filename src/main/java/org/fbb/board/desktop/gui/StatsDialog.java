/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import org.fbb.board.Translator;
import org.fbb.board.internals.ListWithFilter;
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

    public StatsDialog(String wall) {
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setModal(true);
        this.setSize(500, 300);
        this.setLocationRelativeTo(null);
        this.add(new JButton("filter"), BorderLayout.SOUTH);
        JTabbedPane jdb = new JTabbedPane();
        JPanel byDificulty = new JPanel(new BorderLayout());
        JPanel byAutor = new JPanel(new BorderLayout());
        jdb.add(byDificulty);
        jdb.add(byAutor);
        jdb.setTitleAt(0, Translator.R("byDiff"));
        jdb.setTitleAt(1, Translator.R("byAuthor"));
        this.add(jdb);
        DefaultCategoryDataset cdataD = new DefaultCategoryDataset();
        int i = -1;
        for (String s : Grade.currentGrades()) {
            i++;
            ListWithFilter lv = new ListWithFilter(new Grade(i), new Grade(i), wall);
            if (lv.getSize() > 0) {
                cdataD.addValue((Number) lv.getSize(), "diff", new Grade.ToStringGradeWrapper(i));
            }
        }
        JFreeChart diffs = ChartFactory.createBarChart(wall, "difficulty", "count", cdataD);
        diffs.getLegend(0).setVisible(false);
        ChartPanel chp1 = new ChartPanel(diffs);
        chp1.setMinimumDrawHeight(100);
        chp1.setMinimumDrawWidth(100);
        chp1.setMaximumDrawHeight(10000);
        chp1.setMaximumDrawWidth(10000);
        byDificulty.add(chp1);

        DefaultCategoryDataset cdataA = new DefaultCategoryDataset();
        ListWithFilter lv = new ListWithFilter(wall);
        Set<String> authors = new HashSet<>();
        Vector<Boulder> v = lv.getHistory();
        for (Boulder boulder : v) {
            authors.add(boulder.getAuthor().toLowerCase().trim());
        }
        List<AuthorAndCount> sortAuthors = new ArrayList<>();
        for (String author : authors) {
            //if (isNotContained()){
            //on jtextfield, skip...
            //}
            i = 0;
            for (Boulder boulder : v) {
                if (boulder.getAuthor().toLowerCase().trim().equals(author)) {
                    i++;
                }
            }
            if (lv.getSize() > 0) {
                sortAuthors.add(new AuthorAndCount(author, i));
            }
        }
        Collections.sort(sortAuthors);
        for (int j = 0; j < Math.min(sortAuthors.size(), 20); j++) {
            AuthorAndCount get = sortAuthors.get(j);
            cdataA.addValue(get.i, "authors", get.a);
        }
        JFreeChart auhs = ChartFactory.createBarChart(wall, "author", "count", cdataA);
        auhs.getLegend(0).setVisible(false);
        ChartPanel chp2 = new ChartPanel(auhs);
        chp2.setMinimumDrawHeight(100);
        chp2.setMinimumDrawWidth(100);
        chp2.setMaximumDrawHeight(10000);
        chp2.setMaximumDrawWidth(10000);
        byAutor.add(chp2);
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
