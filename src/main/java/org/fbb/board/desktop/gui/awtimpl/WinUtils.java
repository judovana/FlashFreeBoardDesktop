package org.fbb.board.desktop.gui.awtimpl;

import org.fbb.board.desktop.gui.*;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.net.URL;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.fbb.board.Translator;
import org.fbb.board.desktop.ScreenFinder;
import org.fbb.board.internals.GlobalSettings;
import org.fbb.board.internals.GuiLogHelper;

/**
 *
 * @author jvanek
 */
public class WinUtils {

    public static double getIdealWindowSizw(BufferedImage bis) {
        Rectangle size = ScreenFinder.getCurrentScreenSizeWithoutBounds();
        double dw = (double) size.width / (double) bis.getWidth();
        double dh = (double) size.height / (double) bis.getHeight();
        double ratio = Math.min(dw, dh);
        ratio = ratio * MainWindow.gs.getRatio();
        return ratio;
    }

    public static void setIdealWindowLocation(Window w) {
        GlobalSettings gs = MainWindow.gs;
        int he = gs.getHardcodedEdge();
        if (gs.getLocation().equalsIgnoreCase("TR")) {
            w.setLocation(
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() - w.getWidth() - he),
                    (int) (he));
        } else if (gs.getLocation().equalsIgnoreCase("TL")) {
            w.setLocation(
                    (int) (he),
                    (int) (he));
        } else if (gs.getLocation().equalsIgnoreCase("BR")) {
            w.setLocation(
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() - w.getWidth() - he),
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() - w.getHeight()) - he);
        } else if (gs.getLocation().equalsIgnoreCase("BL")) {
            w.setLocation(
                    (int) (he),
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() - w.getHeight()) - he);

        } else if (gs.getLocation().equalsIgnoreCase("T")) {
            w.setLocation(
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() - w.getWidth()) / 2,
                    (int) (he));
        } else if (gs.getLocation().equalsIgnoreCase("B")) {
            w.setLocation(
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() - w.getWidth()) / 2,
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() - w.getHeight()) - he);
        } else if (gs.getLocation().equalsIgnoreCase("R")) {
            w.setLocation(
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() - w.getWidth() - he),
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() - w.getHeight()) / 2);
        } else if (gs.getLocation().equalsIgnoreCase("L")) {
            w.setLocation(
                    (int) (he),
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() - w.getHeight()) / 2);
        } else {
            w.setLocation(
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() - w.getWidth()) / 2,
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() - w.getHeight()) / 2);
        }
    }

    public static String addCtrLine(String string) {
        return "<html>" + string + "<br>" + Translator.R("tryCtrl");
    }

    public static class ShowWebHelp implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent ae) {
            try {
                JDialog jd = new JDialog();
                jd.setModal(true);
                jd.setSize(ScreenFinder.getCurrentScreenSizeWithoutBounds().getSize());
                JEditorPane text = new JEditorPane(new URL("https://raw.githubusercontent.com/judovana/FlashFreeBoardDesktop/master/README.md"));
                jd.add(text);
                jd.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                jd.setVisible(true);
            } catch (Exception ex) {
                GuiLogHelper.guiLogger.loge(ex);
                JOptionPane.showMessageDialog(null, ex);
            }
        }
    }

}
