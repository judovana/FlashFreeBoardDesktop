package org.fbb.board;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.fbb.board.internals.GuiLogHelper;

/**
 * Utility class to provide simple methods to help localize messages
 */
public class Translator {

    public String getLocale() {
        String s = this.resources.getLocale().toString();
        //GuiLogHelper.guiLogger.logo(s);
        return s;
    }

    private void reload(String locale) {
        try {
            if (locale == null || locale.trim().isEmpty()) {
                resources = ResourceBundle.getBundle(defaultBundle);
            } else {
                resources = ResourceBundle.getBundle(defaultBundle, new Locale(locale));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("No bundles found for Locale: " + Locale.getDefault().toString()
                    + "and missing base resource bundle in jar:\\" + defaultBundle + "/Messages.properties");
        }
    }

    private static class TranslatorHolder {

        //https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
        //https://en.wikipedia.org/wiki/Initialization_on_demand_holder_idiom
        private static final Translator INSTANCE = new Translator();

        private static Translator getTransaltor() {
            return TranslatorHolder.INSTANCE;
        }
    }

    /**
     * the localized resource strings
     */
    private ResourceBundle resources;
    private static final String defaultBundle = "org.fbb.board.locales.bundles.Messages";

    Translator() {
        this(defaultBundle);
    }

    Translator(String s) {
        try {
            resources = ResourceBundle.getBundle(s);
            this.reload(getLocale());
        } catch (Exception ex) {
            GuiLogHelper.guiLogger.loge(ex);
            throw new IllegalStateException("No bundles found for Locale: " + Locale.getDefault().toString()
                    + "and missing base resource bundle in jar:\\" + s + "/Messages.properties");
        }
    }

    public static Translator getInstance() {
        return TranslatorHolder.getTransaltor();
    }

    public static void load(String locale) {
        TranslatorHolder.getTransaltor().reload(locale);
    }

    /**
     * Return a translated (localized) version of the message
     *
     * @param message the message to translate
     * @return a string representing the localized message
     */
    public static String R(String message) {
        return R(message, new Object[0]);
    }

    /**
     * @param message key to be found in properties
     * @param params params to be expanded to message
     * @return the localized string for the message
     */
    public static String R(String message, Object... params) {
        return getInstance().getMessage(message, params);
    }

    /**
     * @return the localized resource string using the specified arguments.
     * @param key key to be found in properties
     * @param args params to be expanded to message
     */
    protected String getMessage(String key, Object... args) {
        return MessageFormat.format(getMessage(key), args);
    }

    /**
     * Returns the localized resource string identified by the specified key. If
     * the message is empty, a null is returned.
     */
    private String getMessage(String key) {
        try {
            String result = resources.getString(key);
            if (result.length() == 0) {
                return "";
            } else {
                return result;
            }
        } catch (NullPointerException e) {
            return getMessage("RNoResource", new Object[]{key});
        } catch (MissingResourceException e) {
            return multiCatchTool(key);
        } catch (ClassCastException ee) {
            return multiCatchTool(key);
        }
    }

    private String multiCatchTool(String key) {
        if ("RNoResource".equals(key)) {
            return "No localized text found";
        } else {
            return getMessage("RNoResource", new Object[]{key});
        }
    }
}
