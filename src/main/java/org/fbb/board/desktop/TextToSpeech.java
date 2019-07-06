/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop;

import java.io.IOException;
import org.fbb.board.Translator;
import org.fbb.board.internals.GuiLogHelper;

/**
 *
 * @author jvanek
 */
public class TextToSpeech {

    public static class TextId {

        final private String name;
        final private String code;

        public TextId(String name, String code) {
            this.name = name;
            this.code = code;
        }

        @Override
        public String toString() {
            return name;
        }

    }
    private static final TextId[] avail = new TextId[]{
        new TextId(Translator.R("silent"), null),
        new TextId(("čeština"), "cs"),
        new TextId(("english"), "en")

    };

    public static void change(TextId id) {
        try {
            if (id != null && id.code != null) {
                if (id.code.equalsIgnoreCase("cs")) {
                    tellImpl("Změna v tréniku", id);
                }
                if (id.code.equalsIgnoreCase("en")) {
                    tellImpl("Change in training", id);
                }
            }
        } catch (Exception ex) {
            GuiLogHelper.guiLogger.loge(ex);
        }
    }

    public static void pause(TextId id) {
        try {
            if (id.code.equalsIgnoreCase("cs")) {
                tellImpl("Pauza", id);
            }
            if (id.code.equalsIgnoreCase("en")) {
                tellImpl("Break", id);
            }
        } catch (Exception ex) {
            GuiLogHelper.guiLogger.loge(ex);
        }
    }

    public void tell(String s, TextId id) {
        try {
            tellImpl(s, id);
        } catch (Exception ex) {
            GuiLogHelper.guiLogger.loge(ex);
        }
    }

    public static void tellImpl(String s, TextId id) throws IOException {
        if (id.code != null) {
            Process p = Runtime.getRuntime().exec(new String[]{"espeak", "-v", id.code, s});
        }
    }

    public static boolean isAvail() throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec("espeak --version");
        p.waitFor();
        return (p.exitValue() == 0);
    }

    public static TextId[] getLangs() {
        try {
            if (isAvail()) {
                return avail;
            } else {
                return new TextId[]{
                    new TextId(Translator.R("noEspeak"), null)
                };
            }
        } catch (Exception ex) {
            GuiLogHelper.guiLogger.loge(ex);
            return new TextId[]{
                new TextId(Translator.R("noEspeak"), null)
            };

        }
    }

}
