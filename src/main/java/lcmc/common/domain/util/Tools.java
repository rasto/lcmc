/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
 *
 * DRBD Management Console is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * DRBD Management Console is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package lcmc.common.domain.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.html.HTMLDocument;

import com.google.common.base.Optional;

import lcmc.Exceptions;
import lcmc.cluster.domain.Cluster;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.common.ui.main.MainPresenter;
import lcmc.crm.ui.resource.ServiceInfo;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lombok.SneakyThrows;

/**
 * This class provides tools, that are not classified.
 */
public final class Tools {
    private static final Logger LOG = LoggerFactory.getLogger(Tools.class);
    private static String release = null;
    private static final Map<String, ImageIcon> imageIcons = new HashMap<String, ImageIcon>();
    /** Resource bundle. */
    private static ResourceBundle resource = null;
    private static ResourceBundle resourceAppDefaults = null;

    /** Config data object. */
    private static final Pattern UNIT_PATTERN = Pattern.compile("(\\d*)(\\D*)");

    public static void init() {
        setDefaults();
    }

    /**
     * Returns an ImageIcon, or null if the path was invalid.
     */
    public static ImageIcon createImageIcon(final String imageFilename) {
        final ImageIcon imageIcon = imageIcons.get(imageFilename);
        if (imageIcon != null) {
            return imageIcon;
        }
        final java.net.URL imgURL = Tools.class.getResource("/images/" + imageFilename);
        if (imgURL == null) {
            LOG.appWarning("createImageIcon: couldn't find image: " + imageFilename);
            return null;
        } else {
            final ImageIcon newIcon = new ImageIcon(imgURL);
            imageIcons.put(imageFilename, newIcon);
            return newIcon;
        }
    }

    /** Returns the drbd gui release version. */
    public static String getRelease() {
        if (release != null) {
            return release;
        }
        final Properties p = new Properties();
        try {
            p.load(Tools.class.getResourceAsStream("/release.properties"));
            return p.getProperty("release");
        } catch (final IOException e) {
            LOG.appError("getRelease: cannot open release file", "", e);
            return "unknown";
        }
    }

    /** Sets defaults from AppDefaults bundle. */
    public static void setDefaults() {
        LoggerFactory.setDebugLevel(getDefaultInt("DebugLevel"));
        if (getDefault("AppWarning").equals("y")) {
            LoggerFactory.setShowAppWarning(true);
        }
        if (getDefault("AppError").equals("y")) {
            LoggerFactory.setShowAppError(true);
        }
    }

    public static String getStackTrace(final Throwable e) {
        final StringBuilder strace = new StringBuilder("");
        if (e != null) {
            strace.append('\n');
            strace.append(e.getMessage());
            final StackTraceElement[] st = e.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                strace.append('\n');
                strace.append(e.getStackTrace()[i].toString());
            }
            if (e.getCause() != null) {
                strace.append("\n\ncaused by:");
                strace.append(getStackTrace(e.getCause()));
            }
        }
        return strace.toString();
    }

    /**
     * Checks string if it is an ip.
     */
    public static boolean isIp(final CharSequence ipString) {
        boolean wasValid = true;
        // Inet4Address ip;
        if (ipString == null || "".equals(ipString)) {
            wasValid = false;
        } else {
            final Pattern pattern;
            try {
                final String ipPattern = "([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})";
                pattern = Pattern.compile(ipPattern);
            } catch (final PatternSyntaxException exception) {
                return true;
            }
            final Matcher myMatcher = pattern.matcher(ipString);
            if (myMatcher.matches())  {
                for (int i = 1; i < 5; i++) {
                    if (Integer.parseInt(myMatcher.group(i)) > 255) {
                        wasValid = false;
                        break;
                    }
                }
            } else {
                wasValid = false;
            }
        }
        return wasValid;
    }

    public static void printStackTrace(final String text) {
        System.out.println(text);
        printStackTrace();
    }

    public static void printStackTrace() {
        System.out.println(getStackTrace());
    }

    public static String getStackTrace() {
        final Throwable th = new Throwable();
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        th.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    /**
     * Loads the save file and returns its content as string. Return null, if
     * nothing was loaded.
     */
    public static String loadFile(MainPresenter mainPresenter, final String filename, final boolean showError) {
        BufferedReader in = null;
        final StringBuilder content = new StringBuilder("");
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename)), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                content.append(line);
            }
        } catch (final IOException ex) {
            if (showError) {
                mainPresenter.infoDialog("Load Error", "The file " + filename + " failed to load", ex.getMessage());
            }
            return null;
        } finally {
            if(in != null) {
                try {
                    in.close();
                } catch (final IOException ex) {
                    LOG.appError("loadFile: could not close: " + filename, ex);
                }
            }
        }
        return content.toString();
    }

    /** Stops the specified clusters in the gui. */
    public static void stopClusters(final Iterable<Cluster> selectedClusters) {
        for (final Cluster cluster : selectedClusters) {
            cluster.removeClusterAndDisconnect();
        }
    }

    /**
     * Returns default value for option from AppDefaults resource bundle.
     */
    public static String getDefault(final String option) {
        synchronized (Tools.class) {
            if (resourceAppDefaults == null) {
                resourceAppDefaults = ResourceBundle.getBundle("lcmc.configs.AppDefaults");
            }
        }
        try {
            return resourceAppDefaults.getString(option);
        } catch (final Exception e) {
            LOG.appError("getDefault: unresolved config resource", option, e);
            return option;
        }
    }

    /**
     * Returns default color for option from AppDefaults resource bundle.
     */
    public static Color getDefaultColor(final String option) {
        synchronized (Tools.class) {
            if (resourceAppDefaults == null) {
                resourceAppDefaults = ResourceBundle.getBundle("lcmc.configs.AppDefaults");
            }
        }
        try {
            return (Color) resourceAppDefaults.getObject(option);
        } catch (final Exception e) {
            LOG.appError("getDefaultColor: unresolved config resource", option, e);
            return Color.WHITE;
        }
    }

    /**
     * Returns default value for integer option from AppDefaults resource
     * bundle.
     */
    public static int getDefaultInt(final String option) {
        synchronized (Tools.class) {
            if (resourceAppDefaults == null) {
                resourceAppDefaults = ResourceBundle.getBundle("lcmc.configs.AppDefaults");
            }
        }
        try {
            return (Integer) resourceAppDefaults.getObject(option);
        } catch (final Exception e) {
            LOG.appError("getDefaultInt: exception", option + ": " + getDefault(option), e);
            return 0;
        }
    }

    /**
     * Returns localized string from TextResource resource bundle.
     * Replace {}s, with replace array in the order as they come.
     */
    public static String getString(final String text, final String[] replace) {
        String s = getString(text);
        if (s != null) {
            for (final String r : replace) {
                s = s.replaceFirst("\\{\\}", r);
            }
        }
        return s;
    }

    /**
     * Returns localized string from TextResource resource bundle.
     * Replace {}, with the replace string.
     */
    public static String getString(final String text, final String replace) {
        final String s = getString(text);
        if (s != null) {
            return s.replaceFirst("\\{\\}", replace);
        }
        return s;
    }

    /**
     * Returns localized string from TextResource resource bundle.
     */
    public static String getString(final String text) {
        synchronized (Tools.class) {
            if (resource == null) {
                /* set locale */
                final Locale currentLocale = Locale.getDefault();
                resource = ResourceBundle.getBundle("lcmc.configs.TextResource", currentLocale);
            }
        }
        try {
            return resource.getString(text);
        } catch (final Exception e) {
            LOG.appError("getString: unresolved resource: " + text);
            return text;
        }
    }

    /**
     * Returns service definiton from ServiceDefinitions resource bundle.
     */
    public static String[] getServiceDefinition(
        final String service) {
        final ResourceBundle resourceSD = ResourceBundle.getBundle("lcmc.configs.ServiceDefinitions");
        try {
            return resourceSD.getStringArray(service);
        } catch (final Exception e) {
            LOG.appWarning("getServiceDefinition: cannot get service definition for service: " + service, e);
            return new String[]{};
        }
    }

    /**
     * Joins String array into one string with specified delimiter.
     */
    public static String join(final String delim, final String[] strings) {
        if (strings == null || strings.length == 0) {
            return "";
        }
        if (strings.length == 1 && strings[0] == null) {
            return "";
        }
        final StringBuilder ret = new StringBuilder("");
        for (int i = 0; i < strings.length - 1; i++) {
            if (strings[i] != null) {
                ret.append(strings[i]);
                if (delim != null && strings[i + 1] != null) {
                    ret.append(delim);
                }
            }
        }
        if (strings[strings.length - 1] != null) {
            ret.append(strings[strings.length - 1]);
        }
        return ret.toString();
    }
    /** Joins String list into one string with specified delimiter. */
    public static String join(final String delim, final Collection<String> strings) {
        if (strings == null) {
            return "";
        }
        return join(delim, strings.toArray(new String[strings.size()]));
    }

    /**
     * Joins String array into one string with specified delimiter.
     */
    public static String join(final String delim, final String[] strings, final int length) {
        if (strings == null || strings.length == 0 || length <= 0) {
            return "";
        }
        final StringBuilder ret = new StringBuilder("");
        int i;
        for (i = 0; i < length - 1 && i < strings.length - 1; i++) {
            ret.append(strings[i]);
            if (delim != null) {
                ret.append(delim);
            }
        }
        i++;
        ret.append(strings[i - 1]);
        return ret.toString();
    }

    /** Uppercases the first character. */
    public static String ucfirst(final String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        final String f = s.substring(0, 1);
        return s.replaceFirst(".", f.toUpperCase(Locale.US));
    }

    /**
     * Returns intersection of two string lists as List of string.
     */
    public static Optional<Set<String>> getIntersection(
            final Optional<Set<String>> setA,
            final Optional<Set<String>> setB) {
        final Set<String> resultSet = new TreeSet<String>();
        if (!setB.isPresent()) {
            return setA;
        }
        if (!setA.isPresent()) {
            return setB;
        }
        for (final String item : setA.get()) {
            if (setB.get().contains(item)) {
                resultSet.add(item);
            }
        }
        return Optional.of(resultSet);
    }

    /**
     * Convert text to html.
     */
    public static String html(final String text) {
        if (text == null) {
            return "<html>\n</html>";
        }
        return "<html><p>" + text.replaceAll("\n", "<br>") + "\n</html>";
    }

    /**
     * Checks if object is of the string class. Returns true if object is null.
     */
    public static boolean isStringClass(final Object o) {
        return o == null || o instanceof String;
    }

    /** Escapes for config file. */
    public static String escapeConfig(final String value) {
        if (value == null) {
            return null;
        }
        if (!value.matches("[\\w-]*")) {
            return '"' + value.replaceAll("\"", "\\\\\"") + '"';
        }
        return value;
    }

    /** Sets fixed size for component. */
    public static void setSize(final Component c, final int width, final int height) {
        final Dimension d = new Dimension(width, height);
        c.setMaximumSize(new Dimension(Short.MAX_VALUE, height));
        c.setMinimumSize(d);
        c.setPreferredSize(d);
    }

    /**
     * Returns -1 if version1 is smaller that version2, 0 if version1 equals
     * version2 and 1 if version1 is bigger than version2.
     */
    public static int compareVersions(final String version1,
                                      final String version2) throws Exceptions.IllegalVersionException {
        if (version1 == null || version2 == null) {
            throw new Exceptions.IllegalVersionException(version1, version2);
        }
        final Pattern p = Pattern.compile("(.*\\d+)rc(\\d+)$");
        final Matcher m1 = p.matcher(version1);
        String version1a;
        int rc1 = Integer.MAX_VALUE;
        if (m1.matches()) {
            version1a = m1.group(1);
            try {
                rc1 = Integer.parseInt(m1.group(2));
            } catch (final java.lang.NumberFormatException e) {
                LOG.appWarning("cannot parse: " + m1.group(2));
            }
        } else {
            version1a = version1;
        }
        int index = version1a.indexOf('-');
        if (index < 0) {
            index = version1a.indexOf('_');
        }
        if (index >= 0) {
            version1a = version1a.substring(0, index);
        }

        final Matcher m2 = p.matcher(version2);
        final String version2a;
        int rc2 = Integer.MAX_VALUE;
        if (m2.matches()) {
            version2a = m2.group(1);
            try {
                rc2 = Integer.parseInt(m2.group(2));
            } catch (final java.lang.NumberFormatException e) {
                LOG.appWarning("cannot parse: " + m1.group(2));
            }
        } else {
            version2a = version2;
        }
        final String[] v1a = version1a.split("\\.");
        final String[] v2a = version2a.split("\\.");
        if (v1a.length < 1 || v2a.length < 1) {
            throw new Exceptions.IllegalVersionException(version1, version2);
        }
        int i = 0;
        while (true) {
            if (i >= v1a.length && i >= v2a.length) {
                break;
            } else if (i >= v1a.length || i >= v2a.length) {
                return 0;
            }
            int v1i = 0;
            if (i < v1a.length) {
                final String v1 = v1a[i];
                try {
                    v1i = Integer.parseInt(v1);
                } catch (final java.lang.NumberFormatException e) {
                    throw new Exceptions.IllegalVersionException(version1);
                }
            }

            int v2i = 0;
            if (i < v2a.length) {
                final String v2 = v2a[i];
                try {
                    v2i = Integer.parseInt(v2);
                } catch (final java.lang.NumberFormatException e) {
                    throw new Exceptions.IllegalVersionException(version2);
                }
            }

            if (v1i < v2i) {
                return -1;
            } else if (v1i > v2i) {
                return 1;
            }
            i++;
        }
        if (rc1 < rc2) {
            return -1;
        } else if (rc1 > rc2) {
            return 1;
        }
        return 0;
    }

    /** Returns number of characters 'c' in a string 's'. */
    public static int charCount(final CharSequence s, final char c) {
        if (s == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    /**
     * Creates config on all hosts with specified name in the specified
     * directory.
     */
    public static void createConfigOnAllHosts(final Host[] hosts,
                                              final String config,
                                              final String fileName,
                                              final String dir,
                                              final String mode,
                                              final boolean makeBackup) {
        for (final Host host : hosts) {
            host.getSSH().createConfig(config, fileName, dir, mode, makeBackup, null, null);
        }
    }

    /** Returns border with title. */
    public static TitledBorder getBorder(final String text) {
        final TitledBorder titledBorder = new TitledBorder(BorderFactory.createLineBorder(Color.BLACK, 1), text);
        titledBorder.setTitleJustification(TitledBorder.LEFT);
        return titledBorder;
    }

    /** Returns whether the computer, where this program is run, is Linux. */
    public static boolean isLinux() {
        return "Linux".equals(System.getProperty("os.name"));
    }

    /** Returns whether the computer, where this program is run, is Windows. */
    public static boolean isWindows() {
        return System.getProperty("os.name").indexOf("Windows") == 0;
    }

    /** Sets the html font of the editor pane to be the default font. */
    public static void setEditorFont(final JEditorPane ep) {
        final Font font = UIManager.getFont("Label.font");
        final String bodyRule = "body { font-family: " + font.getFamily()
                                + "; "
                                + "font-size: " + font.getSize() + "pt; }";
        ((HTMLDocument) ep.getDocument()).getStyleSheet().addRule(bodyRule);
    }

    /** Convenience sleep wrapper. */
    public static void sleep(final int ms) {
        try {
            Thread.sleep(ms);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /** Convenience sleep wrapper with float argument. */
    public static void sleep(final float ms) {
        sleep((int) ms);
    }

    /** Returns the latest version of this application. */
    public static String[] getLatestVersion() {
        String version = null;
        final Pattern vPattern = Pattern.compile(".*version:\\s+([0-9.]*)");
        final Pattern iPattern = Pattern.compile(".*info:\\s+(\\d+)\\s+(.*)");
        final int randomInfo = (int) (Math.random() * 100);
        String info = null;
        try {
            final String url = "http://lcmc.sourceforge.net/version.html?lcmc-check-" + getRelease();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream(), "UTF-8"));
            int rate = 0;
            do {
                final String line = reader.readLine();
                if (line == null) {
                    break;
                }
                final Matcher vm = vPattern.matcher(line);
                if (vm.matches()) {
                    final String v = vm.group(1);
                    try {
                        if (version == null || compareVersions(v, version) > 0) {
                            version = v;
                        }
                    } catch (final Exceptions.IllegalVersionException e) {
                        LOG.appWarning("getLatestVersion: " + e.getMessage(), e);
                    }
                } else if (info == null) {
                    final Matcher im = iPattern.matcher(line);
                    if (im.matches()) {
                        rate += Integer.parseInt(im.group(1));
                        if (rate > randomInfo) {
                            info = im.group(2);
                        }
                    }
                }
            } while (true);
        } catch (final MalformedURLException mue) {
            return new String[]{null, null};
        } catch (final IOException ioe) {
            return new String[]{version, info};
        }
        return new String[]{version, info};
    }

    /** Opens default browser. */
    public static void openBrowser(final String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                final Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                }
            }
        } catch (final IOException e) {
            LOG.error("openBrowser: can't open: " + url + "; " + e.getMessage());
        } catch (final URISyntaxException e) {
            LOG.error("openBrowser: can't open: " + url + "; " + e.getMessage());
        }
    }

    /** Check whether the string is number. */
    public static boolean isNumber(final String s) {
        try {
            Long.parseLong(s);
            return true;
        } catch (final NumberFormatException nfe) {
            return false;
        }
    }

    /** Returns list that is expandable by shell. {'a','b'...} */
    public static String shellList(final String[] items) {
        final StringBuilder list = new StringBuilder("");
        if (items == null || items.length == 0) {
            return null;
        } else if (items.length == 1) {
            list.append(items[0]);
        } else {
            list.append('{');
            for (int i = 0; i < items.length - 1; i++) {
                list.append('\'');
                list.append(items[i]);
                list.append("',");
            }
            if (items.length != 0) {
                list.append('\'');
                list.append(items[items.length - 1]);
                list.append('\'');
            }
            list.append('}');
        }
        return list.toString();
    }

    public static <T> boolean areEqual(final T v1, final T v2) {
        if (v1 == null) {
            return v2 == null;
        } else {
            return v1.equals(v2);
        }
    }

    public static boolean areEqual(final Value v1, final Value v2) {
        if (v1 != null) {
            if (v2 == null) {
                return v1.isNothingSelected();
            } else {
                return Tools.areEqual(v1.getValueForConfig(), v2.getValueForConfig());
            }
        } else if (v2 == null) {
            return true;
        } else {
            return v2.isNothingSelected();
        }
    }

    /** Returns random secret of the specified lenght. */
    public static String getRandomSecret(final int len) {
        final Random rand = new Random();
        final ArrayList<Character> charsL = new ArrayList<Character>();
        for (int a = 'a'; a <= 'z'; a++) {
            charsL.add((char) a);
            charsL.add(Character.toUpperCase((char) a));
        }
        for (int a = '0'; a <= '9'; a++) {
            charsL.add((char) a);
        }

        final Character[] chars = charsL.toArray(new Character[charsL.size()]);
        final StringBuilder s = new StringBuilder(len + 1);
        for (int i = 0; i < len; i++) {
            s.append(chars[rand.nextInt(chars.length)]);
        }
        return s.toString();
    }

    public static boolean isLocalIp(final String ip) {
        if (ip == null || "127.0.0.1".equals(ip) || "127.0.1.1".equals(ip)) {
            return true;
        }
        try {
            final String localIp = InetAddress.getLocalHost().getHostAddress();
            return ip.equals(localIp);
        } catch (final java.net.UnknownHostException e) {
            return false;
        }
    }

    /** Resize table. */
    public static void resizeTable(final JTable table, final Map<Integer, Integer> defaultWidths) {
        if (table == null) {
            return;
        }

        final int margin = 3;
        for (int i = 0; i < table.getColumnCount(); i++) {
            final int vColIndex = i;
            final TableColumnModel colModel = table.getColumnModel();
            final TableColumn col = colModel.getColumn(vColIndex);
            TableCellRenderer renderer = col.getHeaderRenderer();

            if (renderer == null) {
                renderer = table.getTableHeader().getDefaultRenderer();
            }
            if (renderer == null) {
                continue;
            }
            Component comp = renderer.getTableCellRendererComponent(table,
                                                                    col.getHeaderValue(),
                                                                    false,
                                                                    false,
                                                                    0,
                                                                    0);
            Integer dw = null;
            if (defaultWidths != null) {
                dw = defaultWidths.get(i);
            }
            int width;
            if (dw == null) {
                width = comp.getPreferredSize().width;
                for (int r = 0; r < table.getRowCount(); r++) {
                    renderer = table.getCellRenderer(r, vColIndex);
                    if (renderer == null) {
                        continue;
                    }
                    comp = renderer.getTableCellRendererComponent(table,
                                                                  table.getValueAt(r, vColIndex),
                                                                  false,
                                                                  false,
                                                                  r,
                                                                  vColIndex);
                    width = Math.max(width, comp.getPreferredSize().width);
                }
            } else {
                width = dw;
                col.setMaxWidth(width);
            }
            width += 2 * margin;
            col.setPreferredWidth(width);
        }
        ((JLabel) table.getTableHeader().getDefaultRenderer())
            .setHorizontalAlignment(SwingConstants.CENTER);
    }

    /** Sets the menu and all its parents visible, not visible. */
    public static void setMenuVisible(final JComponent menu, final boolean visible) {
        JComponent parent = (JComponent) menu.getParent();
        if (parent instanceof JViewport) {
            /* MyList */
            parent = (JComponent) parent.getParent();
            parent = (JComponent) parent.getParent();
        }
        if (parent instanceof JPopupMenu) {
            JComponent inv = (JComponent) ((JPopupMenu) parent).getInvoker();
            while (inv != null) {
                final JComponent invP = (JComponent) inv.getParent();
                if (!(invP instanceof JPopupMenu)) {
                    break;
                }
                invP.setVisible(visible);
                for (final Component c : invP.getComponents()) {
                    c.setVisible(visible);
                }
                final JComponent pp = (JComponent) invP.getParent();
                if (pp != null) {
                    pp.setVisible(visible);
                }
                inv = (JComponent) ((JPopupMenu) invP).getInvoker();
            }
            menu.setVisible(visible);
            parent.setVisible(visible);
            final JComponent pp = (JComponent) parent.getParent();
            if (pp != null) {
                pp.setVisible(visible);
            }
            for (final Component c : parent.getComponents()) {
                c.setVisible(visible);
            }
            parent.repaint();
        }
    }

    /** Sets background color to be opaque or semi-transparent. */
    private static void setBGColor(final JComponent c, final boolean opaque) {
        final Color oc = c.getBackground();
        c.setBackground(new Color(oc.getRed(), oc.getGreen(), oc.getBlue(), opaque ? 255 : 120));
    }

    /** Sets the menu and all its parents opaque, transparent. */
    public static void setMenuOpaque(final JComponent menu, final boolean opaque) {
        JComponent parent = (JComponent) menu.getParent();
        if (parent instanceof JViewport) {
            /* MyList */
            parent = (JComponent) parent.getParent();
            parent = (JComponent) parent.getParent();
        }
        if (parent instanceof JPopupMenu) {
            JComponent inv = (JComponent) ((JPopupMenu) parent).getInvoker();
            while (inv != null) {
                final JComponent invP = (JComponent) inv.getParent();
                if (!(invP instanceof JPopupMenu)) {
                    break;
                }
                setBGColor(invP, opaque);
                for (final Component c : invP.getComponents()) {
                    setBGColor((JComponent) c, opaque);
                }
                final JComponent pp = (JComponent) invP.getParent();
                if (pp != null) {
                    setBGColor(pp, opaque);
                }
                inv = (JComponent) ((JPopupMenu) invP).getInvoker();
            }
            setBGColor(menu, opaque);
            setBGColor(parent, opaque);
            final JComponent pp = (JComponent) parent.getParent();
            if (pp != null) {
                setBGColor(pp, opaque);
            }
            for (final Component c : parent.getComponents()) {
                setBGColor((JComponent) c, opaque);
            }
            parent.repaint();
        }
    }

    public static String getUnixPath(final String dir) {
        if (dir == null) {
            return null;
        }
        String unixPath;
        if (isWindows()) {
            unixPath = dir.replaceAll("\\\\", "/");
            if (unixPath.length() >= 2 && ":".equalsIgnoreCase(unixPath.substring(1, 2))) {
                unixPath = unixPath.substring(2);
            }
        } else {
            unixPath = dir;
        }
        return unixPath;
    }

    /** Returns bounds of the whole screen. */
    public static Rectangle getScreenBounds(final JComponent component) {
        final GraphicsConfiguration gc = component.getGraphicsConfiguration();
        final Rectangle sBounds = gc.getBounds();
        final Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        /* Take into account screen insets, decrease viewport */
        sBounds.x += screenInsets.left;
        sBounds.y += screenInsets.top;
        sBounds.width -= screenInsets.left + screenInsets.right;
        sBounds.height -= (screenInsets.top + screenInsets.bottom);
        return sBounds;
    }

    /** Compares two Lists with services if thery are equal. The order does not
     * matter. */
    public static boolean serviceInfoListEquals(final Collection<ServiceInfo> l1, final Collection<ServiceInfo> l2) {
        if (l1 == l2) {
            return true;
        }
        if (l1 == null || l2 == null) {
            return false;
        }
        if (l1.size() != l2.size()) {
            return false;
        }
        for (final ServiceInfo s1 : l1) {
            if (!l2.contains(s1)) {
                return false;
            }
        }
        return true;
    }

    /** Trims text to have displayable width. */
    public static String trimText(final String text) {
        final int width = 80;
        if (text == null || text.length() <= width) {
            return text;
        }
        final StringBuilder out = new StringBuilder(text.length() + 10);
        /* find next space */
        String t = text;
        while (true) {
            final int pos = t.indexOf(' ', width);
            if (pos > 0) {
                out.append(t.substring(0, pos));
                out.append('\n');
                t = t.substring(pos + 1);
            } else {
                break;
            }
        }
        out.append(t);
        return out.toString();
    }

    /** Return directory part (to the /) of the filename. */
    public static String getDirectoryPart(final String filename) {
        if (filename == null) {
            return null;
        }
        final int i = filename.lastIndexOf('/');
        if (i < 0) {
            return filename;
        }
        return filename.substring(0, i + 1);
    }

    /** Escapes the quotes for the stacked ssh commands. */
    public static String escapeQuotes(final String s, final int count) {
        if (s == null) {
            return null;
        }
        if (count <= 0) {
            return s;
        }
        final StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '"' || c == '$' || c == '`') {
                sb.append('\\');
                sb.append(c);
            } else if (c == '\n') {
                sb.append('\n');
            } else {
                sb.append(c);
            }
        }
        return escapeQuotes(sb.toString(), count - 1);
    }

    /** Escapes the single quotes. */
    public static String escapeSingleQuotes(final String s, final int count) {
        if (s == null) {
            return null;
        }
        if (count <= 0) {
            return s;
        }
        final StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == '\n') {
                sb.append('\n');
            } else if (c == '\'') {
                sb.append("'\\''");
            } else {
                sb.append(c);
            }
        }
        return escapeQuotes(sb.toString(), count - 1);
    }

    /** Returns array of host checkboxes in the specified cluster. */
    public static Map<Host, JCheckBox> getHostCheckBoxes(final Cluster cluster) {
        final Map<Host, JCheckBox> components = new LinkedHashMap<Host, JCheckBox>();
        for (final Host host : cluster.getHosts()) {
            final JCheckBox button = new JCheckBox(host.getName());
            button.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Light"));
            components.put(host, button);
        }
        return components;
    }

    /**
     * Returns true if the hb version on the host is smaller or equal
     * Heartbeat 2.1.4.
     */
    public static boolean versionBeforePacemaker(final Host host) {
        final String hbV = host.getHostParser().getHeartbeatVersion();
        final String pcmkV = host.getHostParser().getPacemakerVersion();
        try {
            return pcmkV == null && hbV != null && Tools.compareVersions(hbV, "2.99.0") < 0;
        } catch (final Exceptions.IllegalVersionException e) {
            LOG.appWarning("versionBeforePacemaker: " + e.getMessage(), e);
            return false;
        }
    }

    /** Trim the white space (' ', '\n') at the end of the string buffer.*/
    public static void chomp(final StringBuffer sb) {
        final int l = sb.length();
        int i = l;

        while (i > 0 && (sb.charAt(i - 1) == '\n')) {
            i--;
        }
        if (i >= 0 && i < l - 1) {
            sb.delete(i, l - 1);
        }
    }

    /** Return brigher version of the color (or darker). */
    public static Color brighterColor(final Color c, final double x) {
        double r = c.getRed() * x;
        if (r > 255) {
            r = 255;
        }
        double g = c.getGreen() * x;
        if (g > 255) {
            g = 255;
        }
        double b = c.getBlue() * x;
        if (b > 255) {
            b = 255;
        }
        return new Color((int) r, (int) g, (int) b);
    }

    public static String generateVMMacAddress() {
        final StringBuilder mac = new StringBuilder("52:54:00");
        for (int i = 0; i < 3; i++) {
            mac.append(':');
            mac.append(String.format("%02x", (int) (Math.random() * 256)));
        }
        return mac.toString();
    }

    private static List<String> getNameParts(final CharSequence name) {
        final List<String> parts = new ArrayList<String>();
        if (name == null) {
            return parts;
        }
        final Pattern p = Pattern.compile("(\\d+|\\D+)");
        final Matcher m = p.matcher(name);
        while (m.find()) {
            parts.add(m.group());
        }
        return parts;
    }

    /**
     * Compare two names, doing the right thing there are numbers in the
     * beginning or in the end of the string.
     */
    public static int compareNames(final CharSequence s1, final CharSequence s2) {
        final List<String> parts1 = getNameParts(s1);
        final List<String> parts2 = getNameParts(s2);
        int i = 0;
        for (final String p1 : parts1) {
            if (i >= parts2.size()) {
                return 1;
            }
            final String p2 = parts2.get(i);
            final int res;
            if (Character.isDigit(p1.charAt(0)) && Character.isDigit(p2.charAt(0))) {
                res = Integer.parseInt(p1) - Integer.parseInt(p2);
            } else {
                res = p1.compareToIgnoreCase(p2);
            }
            if (res != 0) {
                return res;
            }
            i++;
        }
        if (parts1.size() == parts2.size()) {
            return 0;
        }
        return -1;
    }

    public static boolean equalCollections(final Collection<?> collection1, final Collection<?> collection2) {
        if (collection1.size() != collection2.size()) {
            return false;
        }
        final Iterator<?> iterator1 = collection1.iterator();
        final Iterator<?> iterator2 = collection2.iterator();
        while (iterator1.hasNext()) {
            if (!iterator1.next().equals(iterator2.next())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns common file systems on all nodes as StringValue array.
     * The defaultValue is stored as the first item in the array.
     */
    public static Value[] getCommonFileSystemsWithDefault(final Set<String> commonFileSystems, final Value defaultValue) {
        final Value[] commonFileSystemItems =  new Value[commonFileSystems.size() + 1];
        commonFileSystemItems[0] = defaultValue;
        int i = 1;
        for (final String commonFileSystem : commonFileSystems) {
            commonFileSystemItems[i] = new StringValue(commonFileSystem);
            i++;
        }
        return commonFileSystemItems;
    }

    public static void writeImage(final String filename, final BufferedImage image, final String imageType) {
        File outputfile = new File(filename);
        try {
            ImageIO.write(image, imageType, outputfile);
        } catch (Exception e) {
            LOG.appError("writeImage: failed", e);
        }
    }

    @SneakyThrows
    public static String readFile(final String fileName) {
        return new String(Tools.class.getResourceAsStream(fileName).readAllBytes());
    }

    private Tools() {
        /* no instantiation possible. */
    }

}
