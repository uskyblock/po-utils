package dk.lockfuglsang.minecraft.po;

import java.io.*;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Convenience util for supporting static imports.
 */
public enum I18nUtil {
    ;
    private static final Logger log = Logger.getLogger(I18nUtil.class.getName());
    private static I18n i18n;
    private static Locale locale;
    private static File dataFolder = new File(".");

    public static String tr(String s) {
        return getI18n().tr(s);
    }

    public static String tr(String s, Object... args) {
        return getI18n().tr(s, args);
    }

    /**
     * Just used for marking translations (dynamic ones) for .po files.
     */
    public static String marktr(String key) {
        return key;
    }

    public static String pre(String s, Object... args) {
        if (s != null && !s.isEmpty()) {
            return new MessageFormat(s, i18n.getLocale()).format(s, args);
        }
        return "";
    }

    public static I18n getI18n() {
        if (i18n == null) {
            i18n = new I18n(getLocale());
        }
        return i18n;
    }

    public static Locale getLocale() {
        return locale != null ? locale : Locale.ENGLISH;
    }

    public static void setLocale(Locale locale) {
        I18nUtil.locale = locale;
        clearCache();
    }

    public static void setDataFolder(File folder) {
        dataFolder = folder;
        clearCache();
    }

    public static void clearCache() {
        i18n = null;
    }

    public static Locale getLocale(String lang) {
        if (lang != null) {
            // Why is this not just standard Java Locale??
            String[] parts = lang.split("[_\\-]");
            if (parts.length >= 3) {
                return new Locale(parts[0], parts[1], parts[2]);
            } else if (parts.length == 2) {
                return new Locale(parts[0], parts[1]);
            } else {
                return new Locale(parts[0]);
            }
        }
        return null;
    }

    /**
     * Proxy between uSkyBlock and org.xnap.commons.i18n.I18n
     */
    public static class I18n {
        private final Locale locale;
        private List<Properties> props;

        I18n(Locale locale) {
            this.locale = locale;
            props = new ArrayList<>();
            addPropsFromPluginFolder();
            addPropsFromJar();
            addPropsFromZipInJar();
        }

        private void addPropsFromJar() {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("po/" + locale + ".po")) {
                if (in == null) {
                    return;
                }
                Properties i18nProps = POParser.asProperties(in);
                if (i18nProps != null && !i18nProps.isEmpty()) {
                    props.add(i18nProps);
                }
            } catch (IOException e) {
                log.info("Unable to read translations from po/" + locale + ".po: " + e);
            }
        }

        private void addPropsFromZipInJar() {
            // We zip the .po files, since they are currently half the footprint of the jar.
            try (
                    InputStream in = getClass().getClassLoader().getResourceAsStream("i18n.zip");
                    ZipInputStream zin = in != null ? new ZipInputStream(in, Charset.forName("UTF-8")) : null
            ) {
                ZipEntry nextEntry = null;
                do {
                    nextEntry = zin != null ? zin.getNextEntry() : null;
                    if (nextEntry != null && nextEntry.getName().equalsIgnoreCase(locale + ".po")) {
                        Properties i18nProps = POParser.asProperties(zin);
                        if (i18nProps != null && !i18nProps.isEmpty()) {
                            props.add(i18nProps);
                        }
                    }
                } while (nextEntry != null);
            } catch (IOException e) {
                log.info("Unable to load translations from i18n.zip!" + locale + ".po: " + e);
            }
        }

        private void addPropsFromPluginFolder() {
            File poFile = new File(dataFolder, "i18n/" + locale + ".po");
            if (poFile.exists()) {
                try (InputStream in = new FileInputStream(poFile)) {
                    Properties i18nProps = POParser.asProperties(in);
                    if (i18nProps != null && !i18nProps.isEmpty()) {
                        props.add(i18nProps);
                    }
                } catch (IOException e) {
                    log.info("Unable to load translations from i18n/" + locale + ".po: " + e);
                }
            }
        }

        public String tr(String key, Object... args) {
            if (key == null || key.trim().isEmpty()) {
                return "";
            }
            for (Properties prop : props) {
                String propKey = prop.getProperty(key);
                if (prop != null && prop.containsKey(key) && !propKey.trim().isEmpty()) {
                    return format(propKey, args);
                }
            }
            return format(key, args);
        }

        private String format(String propKey, Object[] args) {
            try {
                return new MessageFormat(propKey, getLocale()).format(args);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Problem with: '" + propKey + "'", e);
            }
        }

        public Locale getLocale() {
            return locale;
        }
    }
}
