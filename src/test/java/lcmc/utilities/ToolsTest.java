package lcmc.utilities;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.swing.JPanel;
import junitparams.JUnitParamsRunner;
import static junitparams.JUnitParamsRunner.$;
import junitparams.Parameters;
import lcmc.Exceptions;
import lcmc.gui.GUIData;
import lcmc.gui.TerminalPanel;
import lcmc.gui.resources.drbd.GlobalInfo;
import lcmc.host.domain.Host;
import lcmc.drbd.domain.DrbdHost;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnitParamsRunner.class)
public final class ToolsTest {
    @Mock
    private DrbdHost drbdHostStub;
    @Mock
    private TerminalPanel terminalPanelStub;

    private final GUIData guiData = new GUIData();


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Tools.init();
    }

    @Test
    public void testCreateImageIcon() {
        assertNull("not existing", Tools.createImageIcon("notexisting"));
        assertNotNull("existing", Tools.createImageIcon("startpage_head.jpg"));
    }

    @Test
    public void testSetDefaults() {
        Tools.setDefaults();
    }

    @Test
    @Parameters({"127.0.0.1",
                 "0.0.0.0",
                 "0.0.0.1",
                 "255.255.255.255",
                 "254.255.255.255"})
    public void testIsIp(final String ip) {
        assertTrue(ip, Tools.isIp(ip));
    }

    @Test
    @Parameters({"localhost",
                 "127-0-0-1",
                 "256.255.255.255",
                 "255.256.255.255",
                 "255.255.256.255",
                 "255.255.255.256",
                 "255.255.255.1000",
                 "255.255.255.-1",
                 "255.255.255",
                 "",
                 "255.255.255.255.255",
                 "127.0.0.false",
                 "127.0.false.1",
                 "127.false.0.1",
                 "false.0.0.1"})
    public void testIsNotIp(final String ip) {
        assertFalse(ip, Tools.isIp(ip));
    }

    @Test
    public void testPrintStackTrace() {
        assertFalse("".equals(Tools.getStackTrace()));
    }

    @SuppressWarnings("unused")
    private Object[] parametersForDefaultShouldBeReturned() {
        return $(
            $("", "SSH.PublicKey"),
            $("", "SSH.PublicKey"),
            $("22", "SSH.Port")
        );
    }

    @Test
    @Parameters(method="parametersForDefaultShouldBeReturned")
    public void defaultShouldBeReturned(final String default0, final String key) {
        assertEquals(default0, Tools.getDefault(key));
    }

    @Test
    public void testGetDefaultColor() {
        assertEquals(java.awt.Color.BLACK, Tools.getDefaultColor("TerminalPanel.Background"));
    }

    @Test
    public void testGetDefaultInt() {
        assertEquals(100000, Tools.getDefaultInt("Score.Infinity"));
    }

    @Test
    public void testGetString() {
        assertEquals("Linux Cluster Management Console", Tools.getString("DrbdMC.Title"));
    }

    @Test
    public void testGetErrorString() {
        final String errorString = "the same string";
        assertEquals(errorString, errorString);
    }

    @SuppressWarnings("unused")
    private Object[] parametersForDistStringShouldBeReturned() {
        return $(
            $(null, "none", "none", "none", "none"),
            $("no", "Support", "none", "none", "none"),
            $("no", "Support", null, null, null),
            $("debian", "Support", "debian", null, null),
            $("debian-SQUEEZE", "Support", "debian", "SQUEEZE", null),
            $("debian-SQUEEZE", "Support", "debian", "SQUEEZE", "a"),
            $("i586", "PmInst.install", "suse", null, "i386")
        );
    }

    @Test
    @Parameters(method="parametersForDistStringShouldBeReturned")
    public void distStringShouldBeReturned(final String distString,
                                           final String text,
                                           final String dist,
                                           final String version,
                                           final String arch) {
        assertEquals(distString, Tools.getDistString(text, dist, version, arch));
    }

    @Test
    public void commandShouldBeUndefined() {
        /*
         String text
         String dist
         String version
         String arch
         ConvertCmdCallback convertCmdCallback
         boolean inBash
        */
        assertEquals("undefined",
                     Tools.getDistCommand("undefined",
                                          "debian",
                                          "squeeze",
                                          "i386",
                                          null,
                                          false,
                                          false));
    }

    @Test
    public void twoCommandsShouldBeUndefined() {
        assertEquals("undefined2;;;undefined3",
                     Tools.getDistCommand("undefined2;;;undefined3",
                                          "debian",
                                          "squeeze",
                                          "i386",
                                          null,
                                          false,
                                          false));
    }

    private ConvertCmdCallback getConvertCallback() {
        return new lcmc.utilities.ConvertCmdCallback() {
            @Override
            public String convert(final String command) {
                return command.replaceAll(lcmc.configs.DistResource.SUDO, "sudo ");
            }
        };
    }

    @Test
    public void commandWithoutBash() {
        assertEquals("sudo /etc/init.d/corosync start",
                     Tools.getDistCommand("Corosync.startCorosync",
                                          "debian",
                                          "squeeze",
                                          "i386",
                                          getConvertCallback(),
                                          false,
                                          false));
    }

    @Test
    public void commandWithBash() {
        assertEquals("sudo bash -c \"sudo /etc/init.d/corosync start\"",
                     Tools.getDistCommand("Corosync.startCorosync",
                                          "debian",
                                          "squeeze",
                                          "i386",
                                          getConvertCallback(),
                                          true,
                                          true));
    }

    @Test
    public void multipleCommands() {
        assertEquals("sudo /etc/init.d/corosync start" + ";;;sudo /etc/init.d/corosync start",
                     Tools.getDistCommand("Corosync.startCorosync;;;"
                                          + "Corosync.startCorosync",
                                          "debian",
                                          "squeeze",
                                          "i386",
                                          getConvertCallback(),
                                          false,
                                          false));
    }

    @Test
    public void undefinedAndCommandShouldBeConverted() {
        assertEquals("undefined4" + ";;;sudo /etc/init.d/corosync start",
                     Tools.getDistCommand("undefined4;;;"
                                          + "Corosync.startCorosync",
                                          "debian",
                                          "squeeze",
                                          "i386",
                                          getConvertCallback(),
                                          false,
                                          false));
    }

    @Test
    public void nullCommandShouldReturnNull() {
        assertNull("null command",
                   Tools.getDistCommand(null, "debian", "squeeze", "i386", getConvertCallback(), false, false));
    }

    @Test
    public void nullCommandInBashShouldReturnNull() {
        assertNull(Tools.getDistCommand(null, "debian", "squeeze", "i386", getConvertCallback(), true, true));
    }

    @Test
    public void nullCommandWithNullDistShouldReturnNull() {
        assertNull(Tools.getDistCommand(null, null, null, null, null, true, true));
    }

    @SuppressWarnings("unused")
    private Object[] parametersForDownloadDirShouldBeReturned() {
        return $(
            $(null, null, null, null, null),
            $("2.6.32-28", "2.6.32-28-server", "ubuntu", "lucid", "x86_64"),
            $("2.6.32-28", "2.6.32-28-server", "ubuntu", "lucid", "i386"),
            $("2.6.24-28", "2.6.24-28-server", "ubuntu", "hardy", "x86_64"),
            $("2.6.32.27-0.2",
              "2.6.32.27-0.2-default",
              "suse",
              "SLES11",
              "x86_64"),
            $("2.6.16.60-0.60.1", 
              "2.6.16.60-0.60.1-default",
              "suse",
              "SLES10",
              "x86_64"),
            $("2.6.18-194.8.1.el5",
              "2.6.18-194.8.1.el5",
              "redhatenterpriseserver",
              "5",
              "x86_64"),
            $("2.6.32-71.18.1.el6.x86_64",
              "2.6.32-71.18.1.el6.x86_64",
              "redhatenterpriseserver",
              "6",
              "x86_64"),
            $("2.6.26-2", "2.6.26-2-amd64", "debian", "lenny", "x86_64"),
            $("2.6.32-5", "2.6.32-5-amd64", "debian", "squeeze", "x86_64"),
            $("2.6.32-5", "2.6.32-5-amd64", "debian", "unknown", "x86_64"),
            $("2.6.32-5-amd64",
              "2.6.32-5-amd64",
              "unknown",
              "unknown",
              "x86_64"),
            $(null, null, "unknown", "unknown", "x86_64"),
            $("2.6.32-5-amd64", "2.6.32-5-amd64", null, null, "x86_64")
        );
    }

    @Test
    @Parameters(method="parametersForDownloadDirShouldBeReturned")
    public void downloadDirShouldBeReturned(final String kernelDir,
                                            final String kernelVersion,
                                            final String dist,
                                            final String version,
                                            final String arch) {
         assertEquals(kernelDir, Tools.getKernelDownloadDir(kernelVersion, dist, version, arch));
    }

    @SuppressWarnings("unused")
    private Object[] parametersForDistVersionShouldBeReturned() {
        return $(
            $("LENNY", "debian", "5.0.8"),
            $("SQUEEZE", "debian", "6.0"),
            $("12", "fedora", "Fedora release 12 (Constantine)"),
            $("13", "fedora", "Fedora release 13 (Goddard)"),
            $("14", "fedora", "Fedora release 14 (Laughlin)"),
            $("5", "redhat", "CentOS release 5.5 (Final)"),
            $("5", "redhat", "CentOS release 5.5 (Final)"),
            $("6",
              "redhatenterpriseserver",
              "Red Hat Enterprise Linux Server release 6.0 (Santiago)"),
            $("5",
              "redhatenterpriseserver",
              "Red Hat Enterprise Linux Server release 5.5 (Tikanga)"),
            /* maverick */ 
            $("squeeze/sid/10.10", "ubuntu", "squeeze/sid/10.10"),
            $("KARMIC", "ubuntu", "squeeze/sid/9.10"),
            $("LUCID", "ubuntu", "squeeze/sid/10.04"),
            $("HARDY", "ubuntu", "lenny/sid/8.04"),
            $("SLES10", "suse", "SUSE Linux Enterprise Server 10 (x86_64)"),
            $("SLES11", "suse", "SUSE Linux Enterprise Server 11 (x86_64)"),
            $("OPENSUSE11_2", "suse", "openSUSE 11.2 (x86_64)"),
            $("OPENSUSE11_3", "suse", "openSUSE 11.3 (x86_64)"),
            $("OPENSUSE11_4", "suse", "openSUSE 11.4 (x86_64)"),
            $("2", "openfiler", "Openfiler NSA 2.3")
        );
    }

    @Test
    @Parameters(method="parametersForDistVersionShouldBeReturned")
    public void distVersionShouldBeReturned(final String distVersion, final String dist, final String version) {
        assertEquals(distVersion, Tools.getDistVersionString(dist, version));
    }

    @SuppressWarnings("unused")
    private Object[] parametersForTestJoin() {
        return $(
            $("a,b",   ",",  new String[]{"a", "b"}),
            $("a",     ",",  new String[]{"a"}),
            $("",      ",",  new String[]{}),
            $("",      ",",  (String[]) null),
            $("ab",    null, new String[]{"a", "b"}),
            $("a,b,c", ",",  new String[]{"a", "b" , "c"}),
            $("a",     ",",  new String[]{"a", null}),
            $("",      ",",  new String[]{null, null}),
            $("",      ",",  new String[]{null, null}),
            $("a",     ",",  new String[]{"a", null, null}),
            $("a",     ",",  new String[]{null, "a", null}),
            $("a",     ",",  new String[]{null, null, "a"})
        );
    }

    @Test
    @Parameters(method="parametersForTestJoin")
    public void testJoin(final String expected, final String delim, final String[] values) {
        assertEquals(expected, Tools.join(delim, values));
    }

    @SuppressWarnings("unused")
    private Object[] parametersForTestJoinWithLength() {
        return $(
            $("a,b",   ",", new String[]{"a", "b"},      2),
            $("a,b",   ",", new String[]{"a", "b"},      3),
            $("a",     ",", new String[]{"a", "b"},      1),
            $("",      ",", new String[]{"a", "b"},      0),
            $("",      ",", new String[]{"a", "b"},      -1),
            $("",      ",", null,                        1),
            $("a",     ",", new String[]{"a"},           1),
            $("",      ",", new String[]{},              2),
            $("",      ",", null,                        1),
            $("a,b,c", ",", new String[]{"a", "b", "c"}, 3)
        );
    }

    @Test
    @Parameters(method="parametersForTestJoinWithLength")
    public void joinWithLengthShouldWork(final String expected,
                                         final String delim,
                                         final String[] values,
                                         final int length) {
        assertEquals(expected, Tools.join(delim, values, length));
    }


    @Test
    public void joinCollectionShouldWork() {
        assertEquals("ab", Tools.join(null, Arrays.asList("a", "b")));
    }

    @Test
    public void joinBigArrayShouldWork() {
        final List<String> bigArray = new ArrayList<String>();
        for (int i = 0; i < 1000; i++) {
            bigArray.add("x");
        }
        assertTrue(Tools.join(",", bigArray).length() == 2000 - 1);
        assertTrue(Tools.join(",", bigArray.toArray(new String[bigArray.size()]), 500).length() == 1000 - 1);
    }

    @Test
    @Parameters({"Rasto, rasto",
                 "Rasto, Rasto",
                 "RASTO, RASTO"})
    public void testUCFirst(final String expected, final String anyString) {
        assertEquals(expected, Tools.ucfirst(anyString));
    }

    @Test
    public void ucFirstNullShouldBeNull() {
        assertNull(Tools.ucfirst(null));
    }

    @Test
    public void ucFirstEmptyStringShouldBeEmptyString() {
        assertEquals("", Tools.ucfirst(""));
    }

    @Test
    public void testEnumToStringArray() {
        assertNull(Tools.enumToStringArray(null));
        @SuppressWarnings("UseOfObsoleteCollectionType")
        final String[] testString = Tools.enumToStringArray(
                new Vector<String>(Arrays.asList("a", "b", "c")).elements());
        Assert.assertArrayEquals(new String[]{"a", "b", "c"}, testString);
    }

    @SuppressWarnings("unused")
    private Object[] parametersForTestIntersections() {
        return $( 
            $(new HashSet<String>(Arrays.asList("b")),
              new HashSet<String>(Arrays.asList("a", "b")),
              new HashSet<String>(Arrays.asList("b", "c"))),

            $(new HashSet<String>(Arrays.asList("a", "b")),
              new HashSet<String>(Arrays.asList("a", "b")),
              new HashSet<String>(Arrays.asList("a", "b"))),

            $(new HashSet<String>(Arrays.asList("a", "b")),
              new HashSet<String>(Arrays.asList("a", "b")),
              new HashSet<String>(Arrays.asList("b", "a"))),

            $(new HashSet<String>(),
              new HashSet<String>(Arrays.asList("a", "b")),
              new HashSet<String>(Arrays.asList("c", "d"))),

            $(new HashSet<String>(Arrays.asList("a")),
              new HashSet<String>(Arrays.asList("a", "a")),
              new HashSet<String>(Arrays.asList("a", "a"))),

            $(new HashSet<String>(Arrays.asList("a", "c")),
              new HashSet<String>(Arrays.asList("a", "b", "c")),
              new HashSet<String>(Arrays.asList("a", "d", "c"))),

            $(new HashSet<String>(Arrays.asList("a", "b", "c")),
              new HashSet<String>(Arrays.asList("a", "b", "c")),
              null),

            $(new HashSet<String>(Arrays.asList("a", "b", "c")),
              null,
              new HashSet<String>(Arrays.asList("a", "b", "c"))),

            $(null, null, null)
        );
    }


    @Test
    @Parameters(method="parametersForTestIntersections")
    public void testIntersections(final Set<String> expected, final Set<String> arrayOne, final Set<String> arrayTwo) {
        assertEquals(expected, Tools.getIntersection(arrayOne, arrayTwo));
    }

    @Test
    public void nullIntersectionShouldBeNull() {
        assertEquals(null, Tools.getIntersection(null, null));
    }

    @SuppressWarnings("unused")
    private Object[] parametersForHtmlShouldBeCreated() {
        return $( 
            $("<html><p>test\n</html>", "test"),
            $("<html><p>test<br>line2\n</html>", "test\nline2"),
            $("<html>\n</html>", null)
        );
    }

    @Test
    @Parameters(method="parametersForHtmlShouldBeCreated")
    public void htmlShouldBeCreated(final String html, final String text) {
        assertEquals(html, Tools.html(text));
    }


    @SuppressWarnings("unused")
    private Object[] parametersForShouldBeStringClass() {
        return $( 
            $("string"),
            $((String) null),
            $((Object) null)
        );
    }

    @Test
    @Parameters(method="parametersForShouldBeStringClass")
    public void shouldBeStringClass(final Object object) {
        assertTrue(Tools.isStringClass(object));
    }

    @SuppressWarnings("unused")
    private Object[] parametersForShouldNotBeStringClass() {
        return $( 
            $(new Object()),
            $(new StringBuilder())
        );
    }

    @Test
    @Parameters(method="parametersForShouldNotBeStringClass")
    public void shouldNotBeStringClass(final Object object) {
        assertFalse(Tools.isStringClass(object));
    }

    @SuppressWarnings("unused")
    private Object[] parametersForConfigShouldBeEscaped() {
        return $( 
            $(null,                  null), 
            $("",                    ""), 
            $("\"\\\"\"",            "\""), 
            $("text",                "text"), 
            $("\"text with space\"", "text with space"), 
            $("\"text with \\\"\"",  "text with \""), 
            $("\"just\\\"\"",        "just\"")
        );
    }

    @Test
    @Parameters(method="parametersForConfigShouldBeEscaped")
    public void configShouldBeEscaped(final String escaped, final String config) {
        assertEquals(escaped, Tools.escapeConfig(config));
    }

    @Test
    public void testSetSize() {
        final JPanel p = new JPanel();
        Tools.setSize(p, 20, 10);
        assertEquals(new Dimension(Short.MAX_VALUE, 10), p.getMaximumSize());
        assertEquals(new Dimension(20, 10), p.getMinimumSize());
        assertEquals(new Dimension(20, 10), p.getPreferredSize());
    }

    @SuppressWarnings("unused")
    private Object[] parametersForFirstVersionShouldBeSmaller() {
        return $( 
            $("2.1.3", "2.1.4"),
            $("2.1.3", "3.1.2"),
            $("2.1.3", "2.2.2"),
            $("2.1.3.1", "2.1.4"),

            $("8.3.9", "8.3.10rc1"),
            $("8.3.10rc1", "8.3.10rc2"),
            $("8.3.10rc2", "8.3.10"),
            $("8.3", "8.4"),
            $("8.3", "8.4.5"),
            $("8.3.5", "8.4"),
            $("8.3", "8.4rc3"),
            $("1.1.7-2.fc16", "1.1.8"),
            $("1.6.0_26", "1.7")
        );
    }

    @Test
    @Parameters(method="parametersForFirstVersionShouldBeSmaller")
    public void firstVersionShouldBeSmaller(final String versionOne, final String versionTwo)
    throws Exceptions.IllegalVersionException {
        assertEquals(-1, Tools.compareVersions(versionOne, versionTwo));
    }

    @SuppressWarnings("unused")
    private Object[] parametersForFirstVersionShouldBeGreater() {
        return $( 
            $("2.1.4", "2.1.3"),
            $("3.1.2", "2.1.3"),
            $("2.2.2", "2.1.3"),
            $("2.1.4", "2.1.3.1"),
            $("8.3.10rc1", "8.3.9"),
            $("8.3.10rc2", "8.3.10rc1"),
            $("8.3.10", "8.3.10rc2"),
            $("8.3.10", "8.3.10rc99999999"),
            $("8.4", "8.3"),
            $("8.4rc3", "8.3"),
            $("1.1.7-2.fc16", "1.1.6"),
            $("1.7", "1.6.0_26")
        );
    }

    @Test
    @Parameters(method="parametersForFirstVersionShouldBeGreater")
    public void firstVersionShouldBeGreater(final String versionOne, final String versionTwo)
    throws Exceptions.IllegalVersionException {
        assertEquals(1, Tools.compareVersions(versionOne, versionTwo));
    }

    @SuppressWarnings("unused")
    private Object[] parametersForVersionsShouldBeEqual() {
        return $( 
            $("2.1.3", "2.1.3.1"),
            $("2.1", "2.1.3"),
            $("2", "2.1.3"),
            $("2", "2.1"),

            $("2.1.3", "2.1.3"),
            $("2.1", "2.1"),
            $("2", "2"),
            $("2.1.3.1", "2.1.3"),
            $("2.1.3", "2.1"),
            $("2.1.3", "2"),
            $("2.1", "2"),
            $("8.3", "8.3.0"),

            $("8.3.10rc1", "8.3.10rc1"),
            $("8.3rc1", "8.3rc1"),
            $("8rc1", "8rc1"),
            $("8.3rc2", "8.3.0"),
            $("8.3", "8.3.2"),
            $("8.3.2", "8.3"),
            $("8.4", "8.4"),
            $("8.4", "8.4.0rc3"),
            $("8.4.0rc3", "8.4"),
            $("1.1.7-2.fc16", "1.1.7"),
            $("1.7.0_03", "1.7"),
            $("1.6.0_26", "1.6.0")
        );
    }

    @Test
    @Parameters(method="parametersForVersionsShouldBeEqual")
    public void versionsShouldBeEqual(final String versionOne, final String versionTwo)
    throws Exceptions.IllegalVersionException {
        assertEquals(0, Tools.compareVersions(versionOne, versionTwo));
    }

    @SuppressWarnings("unused")
    private Object[] parametersForCompareVersionsShouldThrowException() {
        return $( 
            $("", ""),
            $(null, null),
            $("", "2.1.3"),
            $("2.1.3", ""),
            $(null, "2.1.3"),
            $("2.1.3", null),
            $("2.1.3", "2.1.a"),
            $("a.1.3", "2.1.3"),
            $("rc1", "8rc1"),
            $("8rc1", "8rc"),
            $("8rc1", "8rc"),
            $("8rc", "8rc1"),
            $("8rc1", "rc"),
            $("rc", "8rc1"),
            $("8r1", "8.3.1rc1"),
            $("8.3.1", "8.3rc1.1"),
            $("8.3rc1.1", "8.3.1")
        );
    }

    @Test(expected=Exceptions.IllegalVersionException.class)
    @Parameters(method="parametersForCompareVersionsShouldThrowException")
    public void compareVersionsShouldThrowException(final String versionOne, final String versionTwo)
    throws Exceptions.IllegalVersionException {
        Tools.compareVersions(versionOne, versionTwo);
    }

    @SuppressWarnings("unused")
    private Object[] parametersForCharCountShouldBeReturned() {
        return $( 
            $(1, "abcd", 'b'),
            $(0, "abcd", 'e'),
            $(1, "abcd", 'd'),
            $(1, "abcd", 'a'),
            $(2, "abcdb", 'b'),
            $(5, "ccccc", 'c'),
            $(1, "a", 'a'),
            $(0, "a", 'b'),
            $(0, "", 'b')
        );
    }

    @Test
    @Parameters(method="parametersForCharCountShouldBeReturned")
    public void charCountShouldBeReturned(final int count, final String string, final char character) {
        assertEquals(count, Tools.charCount(string, character));
    }

    @Test
    public void charCountInNullShouldReturnZero() {
        assertEquals(0, Tools.charCount(null, 'b'));
    }

    @Test
    @Parameters({"1", "-1", "0", "-0", "1235", "100000000000000000", "-100000000000000000"})
    public void shouldBeNumber(final String number) {
        assertTrue(number, Tools.isNumber(number));
    }

    @Test
    @Parameters({"0.1", "1 1", "-", "", "a", ".5", "a1344", "1344a", "13x44"})
    public void shouldNotBeNumber(final String number) {
        assertFalse(number, Tools.isNumber(number));
    }

    @Test
    public void nullShouldNotBeNumber() {
        assertFalse("null", Tools.isNumber(null));
    }

    @SuppressWarnings("unused")
    private Object[] parametersForShellListShouldBeCreated() {
        return $( 
            $("{'a','b'}", new String[]{"a", "b"}),
            $("{'a','b','b'}", new String[]{"a", "b", "b"}),
            $("a", new String[]{"a"}),
            $(null, new String[]{}),
            $(null, null)
        );
    }

    @Test
    @Parameters(method="parametersForShellListShouldBeCreated")
    public void shellListShouldBeCreated(final String shellList, final String[] list) {
        assertEquals(shellList, Tools.shellList(list));
    }

    @SuppressWarnings("unused")
    private Object[] parametersForStringsShouldBeEqual() {
        return $( 
            $(null, null),
            $("", ""),
            $("x", "x")
        );
    }

    @Test
    @Parameters(method="parametersForStringsShouldBeEqual")
    public void stringsShouldBeEqual(final String stringOne, final String stringTwo) {
        assertEquals(stringOne, stringTwo);
    }

    @SuppressWarnings("unused")
    private Object[] parametersForStringsShouldNotBeEqual() {
        return $(
            $("x", "a"),
            $("x", ""),
            $("", "x"),
            $(null, "x"),
            $("x", null)
        );
    }

    @Test
    @Parameters(method="parametersForStringsShouldNotBeEqual")
    public void stringsShouldNotBeEqual(final String stringOne, final String stringTwo) {
        assertNotEquals(stringOne, stringTwo);
    }

    @SuppressWarnings("unused")
    private Object[] parametersForUnitShouldBeExtracted() {
        return $(
            $("10", "min", "10min"),
            $("0",  "s",   "0s"),
            $("0",  "",    "0"),
            $("5",  "",    "5"),
            $("",   "s",   "s"),
            $(null, null,  null)
        );
    }

    @Test
    @Parameters(method="parametersForUnitShouldBeExtracted")
    public void unitShouldBeExtracted(final String value, final String unit, final String valueWithUnit) {
        Assert.assertArrayEquals(new Object[]{value, unit}, Tools.extractUnit(valueWithUnit));
    }

    @Test
    public void testGetRandomSecret() {
        for (int i = 0; i < 100; i++) {
            final String s = Tools.getRandomSecret(2000);
            assertTrue(s.length() == 2000);
            final int count = Tools.charCount(s, 'a');
            assertTrue(count > 2 && count < 500);
        }
    }

    @Test
    @Parameters({"127.0.0.1", "127.0.1.1"})
    public void testIsLocalIp(final String ip) {
        assertTrue(Tools.isLocalIp(ip));
    }

    @Test
    @Parameters({"127.0.0", "127.0.0.1.1", "127.0.0.a", "a", "a"})
    public void testIsNotLocalIp(final String ip) {
        assertFalse(ip, Tools.isLocalIp(ip));
    }

    @Test
    public void textShouldBeTrimmed() {
        assertNull(Tools.trimText(null));
        assertEquals("x", Tools.trimText("x"));
        final String x20 = " xxxxxxxxxxxxxxxxxxx";
        assertEquals(x20 + x20 + x20 + x20, Tools.trimText(x20 + x20 + x20 + x20));
    }

    @Test
    public void textShouldNotBeTrimmed() {
        assertNull(Tools.trimText(null));
        assertEquals("x", Tools.trimText("x"));
        final String x20 = " xxxxxxxxxxxxxxxxxxx";
        assertEquals(x20 + x20 + x20 + x20 + "\n" + x20.trim(), Tools.trimText(x20 + x20 + x20 + x20 + x20));
    }

    @SuppressWarnings("unused")
    private Object[] parametersForDirectoryPartShouldBeExtracted() {
        return $(
            $("/usr/bin/", "/usr/bin/somefile"),
            $("/usr/bin/", "/usr/bin/"),
            $("somefile", "somefile"),
            $("", ""),
            $(null, null),
            $("/", "/")
        );
    }

    @Test
    @Parameters(method="parametersForDirectoryPartShouldBeExtracted")
    public void directoryPartShouldBeExtracted(final String extractedDir, final String file) {
        assertEquals(extractedDir, Tools.getDirectoryPart(file));
    }

    @SuppressWarnings("unused")
    private Object[] parametersForQuotesShouldBeEscaped() {
        return $(
            $("test", "test", 0),
            $("test", "test", -1),
            $(null, null, -1),
            $(null, null, 1),

            $("test", "test", 1),
            $("test", "test", 2),
            $("test", "test", 100),

            $("\\\"\\$\\`test\\\\", "\"$`test\\", 1),
            $("\\\\\\\"\\\\\\$\\\\\\`test\\\\\\\\", "\"$`test\\", 2)
        );
    }

    @Test
    @Parameters(method="parametersForQuotesShouldBeEscaped")
    public void quotesShouldBeEscaped(final String escaped, final String string, final int level) {
        assertEquals(escaped, Tools.escapeQuotes(string, level));
    }

    @SuppressWarnings("unused")
    private Object[] parametersForTestVersionBeforePacemaker() {
        return $(
            $(null, "2.1.4"),
            $(null, "2.1.3")
        );
    }

    @Test
    @Parameters(method="parametersForTestVersionBeforePacemaker")
    public void testVersionBeforePacemaker(final String pcmkVersion, final String hbVersion) {
        final GlobalInfo globalInfo = new GlobalInfo();
        final Host host = new Host();

        host.setPacemakerVersion(pcmkVersion);
        host.setHeartbeatVersion(hbVersion);
        assertTrue(Tools.versionBeforePacemaker(host));
    }

    @SuppressWarnings("unused")
    private Object[] parametersForTestVersionAfterPacemaker() {
        return $(
            $("1.1.5", null),
            $(null, null),
            $("1.0.9", "3.0.2"),
            $("1.0.9", "2.99.0"),
            $("1.0.9", null)
        );
    }

    @Test
    @Parameters(method="parametersForTestVersionAfterPacemaker")
    public void testVersionAfterPacemaker(final String pcmkVersion, final String hbVersion) {
        final Host host = new Host();

        host.setPacemakerVersion(pcmkVersion);
        host.setHeartbeatVersion(hbVersion);
        assertFalse(Tools.versionBeforePacemaker(host));
    }

    @SuppressWarnings("unused")
    private Object[] parametersForTwoNewLineShouldBeOne() {
        return $(
            $("",      ""),
            $("\n",    "\n\n\n"),
            $(" ",     " "),
            $("a",     "a"),
            $("a\nb",  "a\nb"),
            $(" a\n",  " a\n"),
            $(" a\n",  " a\n\n"),
            $(" a \n", " a \n")
        );
    }

    @Test
    @Parameters(method="parametersForTwoNewLineShouldBeOne")
    public void twoNewLineShouldBeOne(final String chomped, final String origString) {
        final StringBuffer sb = new StringBuffer(origString);
        Tools.chomp(sb);
        assertEquals(chomped, sb.toString());
    }

    @Test
    public void testGenerateVMMacAddress() {
       final String mac = Tools.generateVMMacAddress();
       assertEquals(mac.length(), 17);
    }

    @SuppressWarnings("unused")
    private Object[] parametersForNamesShouldBeTheSame() {
        return $(
            $("a", "a"),
            $("2a", "2a"),
            $("1a2b3c4", "1a2b3c4"),
            $(null, null)
        );
    }

    @Test
    @Parameters(method="parametersForNamesShouldBeTheSame")
    public void namesShouldBeTheSame(final String nameOne, final String nameTwo) {
        assertTrue(Tools.compareNames(nameOne, nameTwo) == 0);
    }

    @SuppressWarnings("unused")
    private Object[] parametersForNameOneShouldBeSmaller() {
        return $(
            $("a", "b"),
            $("1a", "2a"),
            $("2a", "2a1"),
            $("a2b", "a10b"),
            $("a2b3", "a10b"),
            $("a2b", "a10b3"),
            $("", "a"),
            $(null, "1"),
            $("1x", "Node001")
        );
    }

    @Test
    @Parameters(method="parametersForNameOneShouldBeSmaller")
    public void nameOneShouldBeSmaller(final String nameOne, final String nameTwo) {
        assertTrue(Tools.compareNames(nameOne, nameTwo) < 0);
    }

    @SuppressWarnings("unused")
    private Object[] parametersForNameOneShouldBeGreater() {
        return $(
            $("10a", "2a"),
            $("2a1", "2a"),
            $("a10", "a2"),
            $("a10b", "a2b"),
            $("a", ""),
            $("1", ""),
            $("1", null)
        );
    }

    @Test
    @Parameters(method="parametersForNameOneShouldBeGreater")
    public void nameOneShouldBeGreater(final String nameOne, final String nameTwo) {
        assertTrue(Tools.compareNames(nameOne, nameTwo) > 0);
    }
}
