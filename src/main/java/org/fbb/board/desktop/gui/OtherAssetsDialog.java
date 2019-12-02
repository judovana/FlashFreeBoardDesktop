/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.gui;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.fbb.board.Updater;
import org.fbb.board.desktop.ScreenFinder;
import org.fbb.board.internals.GuiLogHelper;

/**
 *
 * @author jvanek
 */
public class OtherAssetsDialog extends JDialog {

    public static final File[] defaultpaths = {
        new File(System.getProperty("user.home"))
    };

    private AssetWithPath findLocal(URL u) {
        String file = new File(u.getFile()).getName();
        String fileMatcher = file.replace(".", "\\.").replaceAll("\\d+", "\\\\d+");
        for (File path : paths) {
            File[] fs = path.listFiles();
            for (File f : fs) {
                if (f.getName().matches(fileMatcher)) {
                    System.out.println(f.getAbsolutePath());
                    return new AssetWithPath(file, f, u);
                }
            }
        }
        return null;
    }
    private final File[] paths;

    private static class MericelessComboBasedListener implements ActionListener {

        private final URL from;
        private final String as;
        private final JComboBox<File> path;

        public MericelessComboBasedListener(URL from, String as, JComboBox<File> path) {
            this.from = from;
            this.as = as;
            this.path = path;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                Updater.downloadUsingNIO(from, new File((File) path.getSelectedItem(), as));
            } catch (IOException ex) {
                GuiLogHelper.guiLogger.loge(ex);
                JOptionPane.showMessageDialog(null, ex);
            }
        }

    }

    private static class MericelessListener implements ActionListener {

        private final URL from;
        private final File to;
        private final File relict;

        public MericelessListener(URL from, File to) {
            this(from, to, null);
        }

        public MericelessListener(URL from, File to, File relict) {
            this.from = from;
            this.to = to;
            this.relict = relict;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                Updater.downloadUsingNIO(from, to);
                if (relict != null && relict.exists()) {
                    boolean q = relict.delete();
                    if (q == false) {
                        throw new IOException("Failed to delete " + relict);
                    } else {
                        GuiLogHelper.guiLogger.logo("Deleted " + relict);
                    }
                }
            } catch (IOException ex) {
                GuiLogHelper.guiLogger.loge(ex);
                JOptionPane.showMessageDialog(null, ex);
            }
        }

    }

    private JPanel createNewAsetPanel(URL u) {
        JPanel r = new JPanel(new GridLayout(4, 3));
        JTextField t = new JTextField(u.toExternalForm());
        t.setEnabled(false);
        r.add(t);
        JButton b = new JButton("Save to");
        r.add(b);
        JComboBox c = new JComboBox(paths);
        b.addActionListener(new MericelessComboBasedListener(u, new File(u.getFile()).getName(), c));
        r.add(c);
        //adding 3 rows of nothing to make it nice
        for (int i = 0; i < 6; i++) {
            r.add(new JLabel());
        }
        return r;
    }

    private JPanel createUpdateAssetPanel(AssetWithPath found) {
        JPanel r = new JPanel(new GridLayout(5, 2));
        JTextField t1 = new JTextField(found.remote.toExternalForm());
        t1.setEnabled(false);
        r.add(t1);
        JTextField t2 = new JTextField(found.existingFile.getAbsolutePath());
        t2.setEnabled(false);
        r.add(t2);
        int ii = 0;
        if (found.futureFile.equals(found.existingFile)) {
            JButton b1 = new JButton("replace");
            b1.addActionListener(new MericelessListener(found.remote, found.existingFile));
            r.add(b1);
            r.add(new JLabel());
            ii = 4;
        } else {
            JButton b1 = new JButton("replace under new name");
            b1.addActionListener(new MericelessListener(found.remote, found.futureFile, found.existingFile));
            JButton b2 = new JButton("replace under old name");
            b2.addActionListener(new MericelessListener(found.remote, found.existingFile, found.futureFile));
            JButton b3 = new JButton("place alongside (not recomended)");
            b3.addActionListener(new MericelessListener(found.remote, found.futureFile));
            r.add(b1);
            r.add(b2);
            r.add(b3);
            r.add(new JLabel());
            ii = 2;
        }
        JButton b = new JButton("Save to");
        r.add(b);
        JComboBox c = new JComboBox(paths);
        b.addActionListener(new MericelessComboBasedListener(found.remote, found.newName, c));
        r.add(c);
        //adding  rows of nothing to make it nice
        for (int i = 0; i < ii; i++) {
            r.add(new JLabel());

        }
        return r;
    }

    private static class AssetWithPath {

        private final String newName;
        private final File existingFile;
        private final URL remote;
        private final File futureFile;

        public AssetWithPath(String newName, File existingFile, URL remote) {
            this.newName = newName;
            this.existingFile = existingFile;
            this.remote = remote;
            this.futureFile = new File(existingFile.getParentFile(), newName);
        }

    }

    public OtherAssetsDialog(List<URL> otherAssets, File[] paths) {
        this.paths = paths;
        this.setLayout(new GridLayout(otherAssets.size(), 1));
        this.setModal(true);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        Dimension s = ScreenFinder.getCurrentScreenSizeWithoutBounds().getSize();
        s.width = s.width / 6 * 5;
        s.height = s.height / 6 * 5;
        this.setSize(s);
        this.setLocationRelativeTo(null);
        for (URL u : otherAssets) {
            AssetWithPath found = findLocal(u);
            JPanel control = (found == null) ? createNewAsetPanel(u) : createUpdateAssetPanel(found);
            this.add(control);
        }
        this.pack();
        this.setLocationRelativeTo(null);
    }

    public static void main(String... args) throws MalformedURLException {
        //Updater.Update u = Updater.getUpdatePossibility(true, true, "/home/jvanek/git/FlashFreeBoardDesktop/target/FlashFreeBoardDesktop-1.4-SNAPSHOT-jar-with-dependencies.jar");
        List<URL> l = Arrays.asList(
                new URL("https://github.com/judovana/FlashBalknaSwing/releases/download/FlashBalknaSwing-2.4/FlashBalknaSwing_2.4.jar"),
                new URL("https://github.com/judovana/FlashBalknaSwing/releases/download/FlashBalknaSwing-2.3/FlashBalknaSwing_2.3.jar"),
                new URL("https://github.com/judovana/FlashBalknaSwing/archive/FlashBalknaSwing-2.4.zip"));
        Collections.shuffle(l);
        File[] paths = {
            new File(System.getProperty("user.home")),
            new File("/home/jvanek/git/FlashBalknaSwing/assembly") /*testing only!*/};
        new OtherAssetsDialog(l, paths).setVisible(true);

    }
}
