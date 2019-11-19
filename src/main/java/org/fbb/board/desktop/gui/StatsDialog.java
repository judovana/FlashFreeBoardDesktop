/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui;

import java.awt.BorderLayout;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
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
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.title.LegendTitle;
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
        JTabbedPane jdb = new JTabbedPane();
        JPanel byDificulty = new JPanel(new BorderLayout());
        JPanel byAutor = new JPanel(new BorderLayout());
        byAutor.add(new JTextField(""));
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
        byDificulty.add(chp1);

        DefaultCategoryDataset cdataA = new DefaultCategoryDataset();

        ListWithFilter lv = new ListWithFilter();
        Set<String> authors = new HashSet<>();
        Vector<Boulder> v = lv.getHistory();
        for (Boulder boulder : v) {
            authors.add(boulder.getAuthor().toLowerCase().trim());
        }
        for (String author : authors) {
            i = 0;
            for (Boulder boulder : v) {
                if (boulder.getName().toLowerCase().trim().equals(author)) {
                    i++;
                }
            }
            if (lv.getSize() > 0) {
                cdataA.addValue(i, "authors", author);
            }
        }
        JFreeChart auhs = ChartFactory.createBarChart(wall, "author", "count", cdataA);
        diffs.getLegend(0).setVisible(false);
        ChartPanel chp2 = new ChartPanel(auhs);
        byAutor.add(chp2);
    }

}
