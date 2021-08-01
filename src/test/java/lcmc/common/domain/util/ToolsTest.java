package lcmc.common.domain.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;

import javax.inject.Provider;
import javax.swing.JPanel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import lcmc.Exceptions;
import lcmc.HwEventBus;
import lcmc.cluster.service.ssh.Ssh;
import lcmc.cluster.service.storage.BlockDeviceService;
import lcmc.common.domain.Application;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.main.ProgressIndicator;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.drbd.domain.DrbdXml;
import lcmc.host.domain.Host;
import lcmc.host.domain.HostFactory;
import lcmc.host.domain.Hosts;
import lcmc.host.ui.HostBrowser;
import lcmc.host.ui.TerminalPanel;
import lcmc.robotest.RoboTest;
import lcmc.vm.domain.VmsXml;

@ExtendWith(MockitoExtension.class)
final class ToolsTest {
    @InjectMocks
    private HostFactory hostFactory;
    @Mock
    private HwEventBus hwEventBus;
    @Mock
    private SwingUtils swingUtils;
    @Mock
    private Application application;
    @Mock
    private MainData mainData;
    @Mock
    private ProgressIndicator progressIndicator;
    @Mock
    private Hosts allHosts;
    @Mock
    private RoboTest roboTest;
    @Mock
    private BlockDeviceService blockDeviceService;

    @Mock
    private Provider<VmsXml> vmsXmlProvider;
    @Mock
    private VmsXml vmsXml;
    @Mock
    private Provider<DrbdXml> drbdXmlProvider;
    @Mock
    private DrbdXml drbdXml;
    @Mock
    private Provider<TerminalPanel> terminalPanelProvider;
    @Mock
    private TerminalPanel terminalPanel;
    @Mock
    private Provider<Ssh> sshProvider;
    @Mock
    private Ssh ssh;
    @Mock
    private Provider<HostBrowser> hostBrowserProvider;
    @Mock
    private HostBrowser hostBrowser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        Tools.init();
    }

    @Test
    void testCreateImageIcon() {
        assertThat(Tools.createImageIcon("notexisting")).describedAs("not existing").isNull();
        assertThat(Tools.createImageIcon("startpage_head.jpg")).describedAs("existing").isNotNull();
    }

    @Test
    void testSetDefaults() {
        Tools.setDefaults();
    }

    @ParameterizedTest
    @CsvSource({"127.0.0.1, 0.0.0.0, 0.0.0.1, 255.255.255.255, 254.255.255.255"})
    void testIsIp(final String ip) {
        assertThat(Tools.isIp(ip)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"localhost, 127-0-0-1, 256.255.255.255, 255.256.255.255, 255.255.256.255, 255.255.255.256, 255.255.255.1000,"
                + " 255.255.255.-1, 255.255.255, , 255.255.255.255.255, 127.0.0.false, 127.0.false.1, 127.false.0.1, false.0.0.1"})
    void testIsNotIp(final String ip) {
        assertThat(Tools.isIp(ip)).isFalse();
    }

    @Test
    void testPrintStackTrace() {
        assertThat("".equals(Tools.getStackTrace())).isFalse();
    }

    private static Stream<Arguments> parametersForDefaultShouldBeReturned() {
        return Stream.of(Arguments.of("", "SSH.PublicKey"), Arguments.of("", "SSH.PublicKey"), Arguments.of("22", "SSH.Port"));
    }

    @ParameterizedTest
    @MethodSource("parametersForDefaultShouldBeReturned")
    void defaultShouldBeReturned(final String default0, final String key) {
        assertThat(Tools.getDefault(key)).isEqualTo(default0);
    }

    @Test
    void testGetDefaultColor() {
        assertThat(Tools.getDefaultColor("TerminalPanel.Background")).isEqualTo(java.awt.Color.BLACK);
    }

    @Test
    void testGetDefaultInt() {
        assertThat(Tools.getDefaultInt("Score.Infinity")).isEqualTo(100000);
    }

    @Test
    void testGetString() {
        assertThat(Tools.getString("DrbdMC.Title")).isEqualTo("Linux Cluster Management Console");
    }

    @Test
    void testGetErrorString() {
        final String errorString = "the same string";
        assertThat(errorString).isEqualTo(errorString);
    }

    private static Stream<Arguments> parametersForTestJoin() {
        return Stream.of(Arguments.of("a,b", ",", new String[]{"a", "b"}), Arguments.of("a", ",", new String[]{"a"}),
                Arguments.of("", ",", new String[]{}), Arguments.of("", ",", null),
                Arguments.of("ab", null, new String[]{"a", "b"}), Arguments.of("a,b,c", ",", new String[]{"a", "b", "c"}),
                Arguments.of("a", ",", new String[]{"a", null}), Arguments.of("", ",", new String[]{null, null}),
                Arguments.of("", ",", new String[]{null, null}), Arguments.of("a", ",", new String[]{"a", null, null}),
                Arguments.of("a", ",", new String[]{null, "a", null}), Arguments.of("a", ",", new String[]{null, null, "a"}));
    }

    @ParameterizedTest
    @MethodSource("parametersForTestJoin")
    void testJoin(final String expected, final String delim, final String[] values) {
        assertThat(Tools.join(delim, values)).isEqualTo(expected);
    }

    private static Stream<Arguments> parametersForTestJoinWithLength() {
        return Stream.of(Arguments.of("a,b", ",", new String[]{"a", "b"}, 2), Arguments.of("a,b", ",", new String[]{"a", "b"}, 3),
                Arguments.of("a", ",", new String[]{"a", "b"}, 1), Arguments.of("", ",", new String[]{"a", "b"}, 0),
                Arguments.of("", ",", new String[]{"a", "b"}, -1), Arguments.of("", ",", null, 1),
                Arguments.of("a", ",", new String[]{"a"}, 1), Arguments.of("", ",", new String[]{}, 2),
                Arguments.of("", ",", null, 1), Arguments.of("a,b,c", ",", new String[]{"a", "b", "c"}, 3));
    }

    @ParameterizedTest
    @MethodSource("parametersForTestJoinWithLength")
    void joinWithLengthShouldWork(final String expected, final String delim, final String[] values, final int length) {
        assertThat(Tools.join(delim, values, length)).isEqualTo(expected);
    }


    @Test
    void joinCollectionShouldWork() {
        assertThat(Tools.join(null, Arrays.asList("a", "b"))).isEqualTo("ab");
    }

    @Test
    void joinBigArrayShouldWork() {
        final List<String> bigArray = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            bigArray.add("x");
        }
        assertThat(Tools.join(",", bigArray)).hasSize(2000 - 1);
        assertThat(Tools.join(",", bigArray.toArray(new String[bigArray.size()]), 500)).hasSize(1000 - 1);
    }

    @ParameterizedTest
    @CsvSource({"Rasto, rasto, Rasto, Rasto, RASTO, RASTO"})
    void testUCFirst(final String expected, final String anyString) {
        assertThat(Tools.ucfirst(anyString)).isEqualTo(expected);
    }

    @Test
    void ucFirstNullShouldBeNull() {
        assertThat(Tools.ucfirst(null)).isNull();
    }

    @Test
    void ucFirstEmptyStringShouldBeEmptyString() {
        assertThat(Tools.ucfirst("")).isEqualTo("");
    }


    private static Stream<Arguments> parametersForHtmlShouldBeCreated() {
        return Stream.of(Arguments.of("<html><p>test\n</html>", "test"),
                Arguments.of("<html><p>test<br>line2\n</html>", "test\nline2"), Arguments.of("<html>\n</html>", null));
    }

    @ParameterizedTest
    @MethodSource("parametersForHtmlShouldBeCreated")
    void htmlShouldBeCreated(final String html, final String text) {
        assertThat(Tools.html(text)).isEqualTo(html);
    }


    private static Stream<Arguments> parametersForShouldBeStringClass() {
        return Stream.of(Arguments.of("string"), Arguments.of((String) null), Arguments.of((Object) null));
    }

    @ParameterizedTest
    @MethodSource("parametersForShouldBeStringClass")
    void shouldBeStringClass(final Object object) {
        assertThat(Tools.isStringClass(object)).isTrue();
    }

    private static Stream<Arguments> parametersForShouldNotBeStringClass() {
        return Stream.of(Arguments.of(new Object()), Arguments.of(new StringBuilder()));
    }

    @ParameterizedTest
    @MethodSource("parametersForShouldNotBeStringClass")
    void shouldNotBeStringClass(final Object object) {
        assertThat(Tools.isStringClass(object)).isFalse();
    }

    private static Stream<Arguments> parametersForConfigShouldBeEscaped() {
        return Stream.of(Arguments.of(null, null), Arguments.of("", ""), Arguments.of("\"\\\"\"", "\""),
                Arguments.of("text", "text"), Arguments.of("\"text with space\"", "text with space"),
                Arguments.of("\"text with \\\"\"", "text with \""), Arguments.of("\"just\\\"\"", "just\""));
    }

    @ParameterizedTest
    @MethodSource("parametersForConfigShouldBeEscaped")
    void configShouldBeEscaped(final String escaped, final String config) {
        assertThat(Tools.escapeConfig(config)).isEqualTo(escaped);
    }

    @Test
    void testSetSize() {
        final JPanel p = new JPanel();
        Tools.setSize(p, 20, 10);
        assertThat(p.getMaximumSize()).isEqualTo(new Dimension(Short.MAX_VALUE, 10));
        assertThat(p.getMinimumSize()).isEqualTo(new Dimension(20, 10));
        assertThat(p.getPreferredSize()).isEqualTo(new Dimension(20, 10));
    }

    private static Stream<Arguments> parametersForFirstVersionShouldBeSmaller() {
        return Stream.of(Arguments.of("2.1.3", "2.1.4"), Arguments.of("2.1.3", "3.1.2"), Arguments.of("2.1.3", "2.2.2"),
                Arguments.of("2.1.3.1", "2.1.4"),

                Arguments.of("8.3.9", "8.3.10rc1"), Arguments.of("8.3.10rc1", "8.3.10rc2"), Arguments.of("8.3.10rc2", "8.3.10"),
                Arguments.of("8.3", "8.4"), Arguments.of("8.3", "8.4.5"), Arguments.of("8.3.5", "8.4"),
                Arguments.of("8.3", "8.4rc3"), Arguments.of("1.1.7-2.fc16", "1.1.8"), Arguments.of("1.6.0_26", "1.7"));
    }

    @ParameterizedTest
    @MethodSource("parametersForFirstVersionShouldBeSmaller")
    void firstVersionShouldBeSmaller(final String versionOne, final String versionTwo) throws Exceptions.IllegalVersionException {
        assertThat(Tools.compareVersions(versionOne, versionTwo)).isEqualTo(-1);
    }

    private static Stream<Arguments> parametersForFirstVersionShouldBeGreater() {
        return Stream.of(Arguments.of("2.1.4", "2.1.3"), Arguments.of("3.1.2", "2.1.3"), Arguments.of("2.2.2", "2.1.3"),
                Arguments.of("2.1.4", "2.1.3.1"), Arguments.of("8.3.10rc1", "8.3.9"), Arguments.of("8.3.10rc2", "8.3.10rc1"),
                Arguments.of("8.3.10", "8.3.10rc2"), Arguments.of("8.3.10", "8.3.10rc99999999"), Arguments.of("8.4", "8.3"),
                Arguments.of("8.4rc3", "8.3"), Arguments.of("1.1.7-2.fc16", "1.1.6"), Arguments.of("1.7", "1.6.0_26"));
    }

    @ParameterizedTest
    @MethodSource("parametersForFirstVersionShouldBeGreater")
    void firstVersionShouldBeGreater(final String versionOne, final String versionTwo) throws Exceptions.IllegalVersionException {
        assertThat(Tools.compareVersions(versionOne, versionTwo)).isEqualTo(1);
    }

    private static Stream<Arguments> parametersForVersionsShouldBeEqual() {
        return Stream.of(Arguments.of("2.1.3", "2.1.3.1"), Arguments.of("2.1", "2.1.3"), Arguments.of("2", "2.1.3"),
                Arguments.of("2", "2.1"),

                Arguments.of("2.1.3", "2.1.3"), Arguments.of("2.1", "2.1"), Arguments.of("2", "2"),
                Arguments.of("2.1.3.1", "2.1.3"), Arguments.of("2.1.3", "2.1"), Arguments.of("2.1.3", "2"),
                Arguments.of("2.1", "2"), Arguments.of("8.3", "8.3.0"),

                Arguments.of("8.3.10rc1", "8.3.10rc1"), Arguments.of("8.3rc1", "8.3rc1"), Arguments.of("8rc1", "8rc1"),
                Arguments.of("8.3rc2", "8.3.0"), Arguments.of("8.3", "8.3.2"), Arguments.of("8.3.2", "8.3"),
                Arguments.of("8.4", "8.4"), Arguments.of("8.4", "8.4.0rc3"), Arguments.of("8.4.0rc3", "8.4"),
                Arguments.of("1.1.7-2.fc16", "1.1.7"), Arguments.of("1.7.0_03", "1.7"), Arguments.of("1.6.0_26", "1.6.0"));
    }

    @ParameterizedTest
    @MethodSource("parametersForVersionsShouldBeEqual")
    void versionsShouldBeEqual(final String versionOne, final String versionTwo) throws Exceptions.IllegalVersionException {
        assertThat(Tools.compareVersions(versionOne, versionTwo)).isEqualTo(0);
    }

    private static Stream<Arguments> parametersForCompareVersionsShouldThrowException() {
        return Stream.of(Arguments.of("", ""), Arguments.of(null, null), Arguments.of("", "2.1.3"), Arguments.of("2.1.3", ""),
                Arguments.of(null, "2.1.3"), Arguments.of("2.1.3", null), Arguments.of("2.1.3", "2.1.a"),
                Arguments.of("a.1.3", "2.1.3"), Arguments.of("rc1", "8rc1"), Arguments.of("8rc1", "8rc"),
                Arguments.of("8rc1", "8rc"), Arguments.of("8rc", "8rc1"), Arguments.of("8rc1", "rc"), Arguments.of("rc", "8rc1"),
                Arguments.of("8r1", "8.3.1rc1"), Arguments.of("8.3.1", "8.3rc1.1"), Arguments.of("8.3rc1.1", "8.3.1"));
    }

    @ParameterizedTest
    @MethodSource("parametersForCompareVersionsShouldThrowException")
    void compareVersionsShouldThrowException(final String versionOne, final String versionTwo) {
        assertThatThrownBy(() -> Tools.compareVersions(versionOne, versionTwo)).isInstanceOf(
                Exceptions.IllegalVersionException.class);
    }

    private static Stream<Arguments> parametersForCharCountShouldBeReturned() {
        return Stream.of(Arguments.of(1, "abcd", 'b'), Arguments.of(0, "abcd", 'e'), Arguments.of(1, "abcd", 'd'),
                Arguments.of(1, "abcd", 'a'), Arguments.of(2, "abcdb", 'b'), Arguments.of(5, "ccccc", 'c'),
                Arguments.of(1, "a", 'a'), Arguments.of(0, "a", 'b'), Arguments.of(0, "", 'b'));
    }

    @ParameterizedTest
    @MethodSource("parametersForCharCountShouldBeReturned")
    void charCountShouldBeReturned(final int count, final String string, final char character) {
        assertThat(Tools.charCount(string, character)).isEqualTo(count);
    }

    @Test
    void charCountInNullShouldReturnZero() {
        assertThat(Tools.charCount(null, 'b')).isEqualTo(0);
    }

    @ParameterizedTest
    @CsvSource({"1, -1, 0, -0, 1235, 100000000000000000, -100000000000000000"})
    void shouldBeNumber(final String number) {
        assertThat(Tools.isNumber(number)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"0.1, 1 1, -, '', a, .5, a1344, 1344a, 13x44"})
    void shouldNotBeNumber(final String number) {
        assertThat(Tools.isNumber(number)).isFalse();
    }

    @Test
    void nullShouldNotBeNumber() {
        assertThat(Tools.isNumber(null)).isFalse();
    }

    private static Stream<Arguments> parametersForShellListShouldBeCreated() {
        return Stream.of(Arguments.of("{'a','b'}", new String[]{"a", "b"}),
                Arguments.of("{'a','b','b'}", new String[]{"a", "b", "b"}), Arguments.of("a", new String[]{"a"}),
                Arguments.of(null, new String[]{}), Arguments.of(null, null));
    }

    @ParameterizedTest
    @MethodSource("parametersForShellListShouldBeCreated")
    void shellListShouldBeCreated(final String shellList, final String[] list) {
        assertThat(Tools.shellList(list)).isEqualTo(shellList);
    }

    private static Stream<Arguments> parametersForStringsShouldBeEqual() {
        return Stream.of(Arguments.of(null, null), Arguments.of("", ""), Arguments.of("x", "x"));
    }

    @ParameterizedTest
    @MethodSource("parametersForStringsShouldBeEqual")
    void stringsShouldBeEqual(final String stringOne, final String stringTwo) {
        assertThat(stringTwo).isEqualTo(stringOne);
    }

    private static Stream<Arguments> parametersForStringsShouldNotBeEqual() {
        return Stream.of(Arguments.of("x", "a"), Arguments.of("x", ""), Arguments.of("", "x"), Arguments.of(null, "x"),
                Arguments.of("x", null));
    }

    @ParameterizedTest
    @MethodSource("parametersForStringsShouldNotBeEqual")
    void stringsShouldNotBeEqual(final String stringOne, final String stringTwo) {
        assertThat(stringOne).isNotEqualTo(stringTwo);
    }

    private Stream<Arguments> parametersForUnitShouldBeExtracted() {
        return Stream.of(Arguments.of("10", "min", "10min"), Arguments.of("0", "s", "0s"), Arguments.of("0", "", "0"),
                Arguments.of("5", "", "5"), Arguments.of("", "s", "s"), Arguments.of(null, null, null));
    }

    @Test
    void testGetRandomSecret() {
        for (int i = 0; i < 100; i++) {
            final String s = Tools.getRandomSecret(2000);
            assertThat(s).hasSize(2000);
            final int count = Tools.charCount(s, 'a');
            assertThat(count > 2 && count < 500).isTrue();
        }
    }

    @ParameterizedTest
    @CsvSource({"127.0.0.1, 127.0.1.1"})
    void testIsLocalIp(final String ip) {
        assertThat(Tools.isLocalIp(ip)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"127.0.0, 127.0.0.1.1, 127.0.0.a, a, a"})
    void testIsNotLocalIp(final String ip) {
        assertThat(Tools.isLocalIp(ip)).isFalse();
    }

    @Test
    void textShouldBeTrimmed() {
        assertThat(Tools.trimText(null)).isNull();
        assertThat(Tools.trimText("x")).isEqualTo("x");
        final String x20 = " xxxxxxxxxxxxxxxxxxx";
        assertThat(Tools.trimText(x20 + x20 + x20 + x20)).isEqualTo(x20 + x20 + x20 + x20);
    }

    @Test
    void textShouldNotBeTrimmed() {
        assertThat(Tools.trimText(null)).isNull();
        assertThat(Tools.trimText("x")).isEqualTo("x");
        final String x20 = " xxxxxxxxxxxxxxxxxxx";
        assertThat(Tools.trimText(x20 + x20 + x20 + x20 + x20)).isEqualTo(x20 + x20 + x20 + x20 + "\n" + x20.trim());
    }

    private static Stream<Arguments> parametersForDirectoryPartShouldBeExtracted() {
        return Stream.of(Arguments.of("/usr/bin/", "/usr/bin/somefile"), Arguments.of("/usr/bin/", "/usr/bin/"),
                Arguments.of("somefile", "somefile"), Arguments.of("", ""), Arguments.of(null, null), Arguments.of("/", "/"));
    }

    @ParameterizedTest
    @MethodSource("parametersForDirectoryPartShouldBeExtracted")
    void directoryPartShouldBeExtracted(final String extractedDir, final String file) {
        assertThat(Tools.getDirectoryPart(file)).isEqualTo(extractedDir);
    }

    private static Stream<Arguments> parametersForQuotesShouldBeEscaped() {
        return Stream.of(Arguments.of("test", "test", 0), Arguments.of("test", "test", -1), Arguments.of(null, null, -1),
                Arguments.of(null, null, 1), Arguments.of("test", "test", 1), Arguments.of("test", "test", 2),
                Arguments.of("test", "test", 100), Arguments.of("\\\"\\$\\`test\\\\", "\"$`test\\", 1),
                Arguments.of("\\\\\\\"\\\\\\$\\\\\\`test\\\\\\\\", "\"$`test\\", 2));
    }

    @ParameterizedTest
    @MethodSource("parametersForQuotesShouldBeEscaped")
    void quotesShouldBeEscaped(final String escaped, final String string, final int level) {
        assertThat(Tools.escapeQuotes(string, level)).isEqualTo(escaped);
    }

    private static Stream<Arguments> parametersForTestVersionBeforePacemaker() {
        return Stream.of(Arguments.of(null, "2.1.4"), Arguments.of(null, "2.1.3"));
    }

    @ParameterizedTest
    @MethodSource("parametersForTestVersionBeforePacemaker")
    void testVersionBeforePacemaker(final String pcmkVersion, final String hbVersion) {
        when(terminalPanelProvider.get()).thenReturn(terminalPanel);
        when(sshProvider.get()).thenReturn(ssh);
        when(hostBrowserProvider.get()).thenReturn(hostBrowser);
        final Host host = hostFactory.createInstance();

        host.getHostParser().setPacemakerVersion(pcmkVersion);
        host.getHostParser().setHeartbeatVersion(hbVersion);
        assertThat(Tools.versionBeforePacemaker(host)).isTrue();
    }

    private static Stream<Arguments> parametersForTestVersionAfterPacemaker() {
        return Stream.of(Arguments.of("1.1.5", null), Arguments.of(null, null), Arguments.of("1.0.9", "3.0.2"),
                Arguments.of("1.0.9", "2.99.0"), Arguments.of("1.0.9", null));
    }

    @ParameterizedTest
    @MethodSource("parametersForTestVersionAfterPacemaker")
    void testVersionAfterPacemaker(final String pcmkVersion, final String hbVersion) {
        when(terminalPanelProvider.get()).thenReturn(terminalPanel);
        when(sshProvider.get()).thenReturn(ssh);
        when(hostBrowserProvider.get()).thenReturn(hostBrowser);

        final Host host = hostFactory.createInstance();

        host.getHostParser().setPacemakerVersion(pcmkVersion);
        host.getHostParser().setHeartbeatVersion(hbVersion);
        assertThat(Tools.versionBeforePacemaker(host)).isFalse();
    }

    private static Stream<Arguments> parametersForTwoNewLineShouldBeOne() {
        return Stream.of(Arguments.of("", ""), Arguments.of("\n", "\n\n\n"), Arguments.of(" ", " "), Arguments.of("a", "a"),
                Arguments.of("a\nb", "a\nb"), Arguments.of(" a\n", " a\n"), Arguments.of(" a\n", " a\n\n"),
                Arguments.of(" a \n", " a \n"));
    }

    @ParameterizedTest
    @MethodSource("parametersForTwoNewLineShouldBeOne")
    void twoNewLineShouldBeOne(final String chomped, final String origString) {
        final StringBuffer sb = new StringBuffer(origString);
        Tools.chomp(sb);
        assertThat(sb.toString()).isEqualTo(chomped);
    }

    @Test
    void testGenerateVMMacAddress() {
        final String mac = Tools.generateVMMacAddress();
        assertThat(17).isEqualTo(mac.length());
    }

    private static Stream<Arguments> parametersForNamesShouldBeTheSame() {
        return Stream.of(Arguments.of("a", "a"), Arguments.of("2a", "2a"), Arguments.of("1a2b3c4", "1a2b3c4"),
                Arguments.of(null, null));
    }

    @ParameterizedTest
    @MethodSource("parametersForNamesShouldBeTheSame")
    void namesShouldBeTheSame(final String nameOne, final String nameTwo) {
        assertThat(Tools.compareNames(nameOne, nameTwo) == 0).isTrue();
    }

    private static Stream<Arguments> parametersForNameOneShouldBeSmaller() {
        return Stream.of(Arguments.of("a", "b"), Arguments.of("1a", "2a"), Arguments.of("2a", "2a1"), Arguments.of("a2b", "a10b"),
                Arguments.of("a2b3", "a10b"), Arguments.of("a2b", "a10b3"), Arguments.of("", "a"), Arguments.of(null, "1"),
                Arguments.of("1x", "Node001"));
    }

    @ParameterizedTest
    @MethodSource("parametersForNameOneShouldBeSmaller")
    void nameOneShouldBeSmaller(final String nameOne, final String nameTwo) {
        assertThat(Tools.compareNames(nameOne, nameTwo) < 0).isTrue();
    }

    private static Stream<Arguments> parametersForNameOneShouldBeGreater() {
        return Stream.of(Arguments.of("10a", "2a"), Arguments.of("2a1", "2a"), Arguments.of("a10", "a2"),
                Arguments.of("a10b", "a2b"), Arguments.of("a", ""), Arguments.of("1", ""), Arguments.of("1", null));
    }

    @ParameterizedTest
    @MethodSource("parametersForNameOneShouldBeGreater")
    void nameOneShouldBeGreater(final String nameOne, final String nameTwo) {
        assertThat(Tools.compareNames(nameOne, nameTwo) > 0).isTrue();
    }

    private static Stream<Arguments> equalCollections() {
        return Stream.of(Arguments.of(new ArrayList<String>(), new ArrayList<String>()),
                Arguments.of(new ArrayList<>(Arrays.asList("a", "b")), new ArrayList<>(Arrays.asList("a", "b"))),
                Arguments.of(new TreeSet<String>(), new TreeSet<String>()),
                Arguments.of(new TreeSet<>(Arrays.asList("a", "b")), new TreeSet<>(Arrays.asList("a", "b"))),
                Arguments.of(new TreeSet<>(Arrays.asList("b", "a")), new TreeSet<>(Arrays.asList("a", "b"))));
    }

    @ParameterizedTest
    @MethodSource("equalCollections")
    void collectionsShouldBeEqual(final Collection<String> collection1, Collection<String> collection2) {
        assertThat(Tools.equalCollections(collection1, collection2)).isTrue();
    }

    private static Stream<Arguments> unequalCollections() {
        return Stream.of(Arguments.of(new ArrayList<String>(), new ArrayList<>(List.of("a"))),
                Arguments.of(new ArrayList<>(List.of("a")), new ArrayList<String>()),
                Arguments.of(new ArrayList<>(List.of("a")), new ArrayList<>(Arrays.asList("a", "b"))),
                Arguments.of(new ArrayList<>(Arrays.asList("a", "b")), new ArrayList<>(List.of("b"))),
                Arguments.of(new ArrayList<>(Arrays.asList("a", "a")), new ArrayList<>(Arrays.asList("a", "b"))),
                Arguments.of(new ArrayList<>(Arrays.asList("b", "b")), new ArrayList<>(Arrays.asList("a", "b"))),
                Arguments.of(new TreeSet<String>(), new TreeSet<>(List.of("a"))),
                Arguments.of(new TreeSet<>(List.of("a")), new TreeSet<String>()),
                Arguments.of(new TreeSet<>(List.of("a")), new TreeSet<>(Arrays.asList("a", "b"))),
                Arguments.of(new TreeSet<>(Arrays.asList("a", "b")), new TreeSet<>(List.of("b"))),
                Arguments.of(new TreeSet<>(Arrays.asList("a", "a")), new TreeSet<>(Arrays.asList("a", "b"))),
                Arguments.of(new TreeSet<>(Arrays.asList("b", "b")), new TreeSet<>(Arrays.asList("a", "b"))));
    }

    @ParameterizedTest
    @MethodSource("unequalCollections")
    void collectionsShouldNotBeEqual(final Collection<String> collection1, Collection<String> collection2) {
        assertThat(Tools.equalCollections(collection1, collection2)).isFalse();
    }
}
