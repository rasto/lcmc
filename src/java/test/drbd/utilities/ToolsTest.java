package drbd.utilities;

import junit.framework.TestCase;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;

final public class ToolsTest extends TestCase {
    /** Whether to test INTERACTIVE elements. */
    private static final boolean INTERACTIVE = false;

    private static PrintStream realOut = System.out;
    private static final String INFO_STRING = "INFO: ";
    private static final String DEBUG_STRING = "DEBUG: ";
    private static final String ERROR_STRING = "ERROR: ";
    private static final String APPWARNING_STRING = "APPWARNING: ";
    private static final String APPERROR_STRING = "APPERROR: ";

    private final StringBuilder stdout = new StringBuilder();

    private final OutputStream out = new OutputStream() {
         @Override public void write(int b) throws IOException {  
             stdout.append(String.valueOf((char) b));  
         }  
   
         @Override public void write(byte[] b, int off, int len)
                                                    throws IOException {  
             stdout.append(new String(b, off, len));  
         }  
   
         @Override public void write(byte[] b) throws IOException {  
             write(b, 0, b.length);  
         }
    };

    protected void setUp() {
        if (Tools.getGUIData() == null) {
            drbd.DrbdMC.main(null);
            Tools.sleep(5000);
        }
        Tools.waitForSwing();
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
        clearStdout();
    }

    protected void tearDown() {
        assertEquals("", stdout.toString());
    }

    private void clearStdout() {
        stdout.delete(0, stdout.length());
    }

    /* ---- tests ----- */
    public void testConvertKilobytes() {
        assertEquals("wrong", "aa", Tools.convertKilobytes("aa"));
        assertEquals("negative", "-1000K", Tools.convertKilobytes("-1000"));
        assertEquals("2G", Tools.convertKilobytes("2G"));
        assertEquals("0K", Tools.convertKilobytes("0"));
        assertEquals("1K", Tools.convertKilobytes("1"));

        assertEquals("1023K", Tools.convertKilobytes("1023"));
        assertEquals("1M", Tools.convertKilobytes("1024"));
        assertEquals("1025K", Tools.convertKilobytes("1025"));

        assertEquals("2047K", Tools.convertKilobytes("2047"));
        assertEquals("2M", Tools.convertKilobytes("2048"));
        assertEquals("2049K", Tools.convertKilobytes("2049"));

        assertEquals("1048575K", Tools.convertKilobytes("1048575"));
        assertEquals("1G", Tools.convertKilobytes("1048576"));
        assertEquals("1023M", Tools.convertKilobytes("1047552"));
        assertEquals("1048577K", Tools.convertKilobytes("1048577"));
        assertEquals("1025M", Tools.convertKilobytes("1049600"));

        assertEquals("1073741825K", Tools.convertKilobytes("1073741825"));
        assertEquals("1023G", Tools.convertKilobytes("1072693248"));
        assertEquals("1T", Tools.convertKilobytes("1073741824"));
        assertEquals("1025G", Tools.convertKilobytes("1074790400"));
        assertEquals("1050625M", Tools.convertKilobytes("1075840000"));
        assertEquals("1073741827K", Tools.convertKilobytes("1073741827"));



        assertEquals("1P", Tools.convertKilobytes("1099511627776"));
        assertEquals("1024P", Tools.convertKilobytes("1125899906842624"));
        assertEquals("10000P", Tools.convertKilobytes("10995116277760000"));
    }

    public void testCreateImageIcon() {
        assertNull("not existing", Tools.createImageIcon("notexisting"));
        assertFalse("".equals(stdout.toString()));
        clearStdout();
        assertNotNull("existing", Tools.createImageIcon("startpage_head.jpg"));
    }

    public void testGetRelease() {
        final String release = Tools.getRelease();
        assertNotNull("not null", release);
        assertFalse("not empty", "".equals(release));
        realOut.println("release: " + release);
    }

    public void testInfo() {
        Tools.info("info a");
        assertEquals(INFO_STRING + "info a\n", stdout.toString());
        clearStdout();
    }

    public void testSetDefaults() {
        Tools.setDefaults();
    }

    public void testDebug() {
        Tools.setDebugLevel(1);
        Tools.debug(null, "test a");
        assertEquals(DEBUG_STRING + "test a\n", stdout.toString());
        clearStdout();
        Tools.setDebugLevel(0);
        Tools.decrementDebugLevel(); /* -1 */
        clearStdout();
        Tools.debug(null, "test b");
        assertEquals("", stdout.toString());
        Tools.incrementDebugLevel(); /* 0 */
        clearStdout();
        Tools.debug(null, "test c");
        assertEquals(DEBUG_STRING + "test c\n", stdout.toString());
        clearStdout();
        Tools.setDebugLevel(1); /* 1 */
        clearStdout();
        Tools.debug(new Object(), "test d2", 2);
        Tools.debug(new Object(), "test d1", 1);
        Tools.debug(new Object(), "test d0", 0);
        assertEquals(DEBUG_STRING + "(1) test d1 (java.lang.Object)\n"
                     + DEBUG_STRING + "(0) test d0 (java.lang.Object)\n",
                     stdout.toString());
        clearStdout();
    }

    public void testError() {
        if (INTERACTIVE) {
            Tools.error("test error a / just click ok");
            assertEquals(ERROR_STRING + "test error a / just click ok\n",
                         stdout.toString());
            clearStdout();
        }
    }

    public void testSSHError() {
        Tools.sshError(null, "cmd a", "ans a", "stack trace a", 2);
        assertEquals(APPWARNING_STRING + "Command: 'cmd a'\n"
                     + "returned exit code 2\n"
                     + "ans a\n"
                     + "stack trace a\n",
                     stdout.toString());
        clearStdout();
    }

    public void testConfirmDialog() {
        if (INTERACTIVE) {
            assertTrue(
                Tools.confirmDialog("tile a", "click yes", "yes (click)", "no"));
            assertFalse(
                Tools.confirmDialog("tile a", "click no", "yes", "no (click)"));
        }
    }

    public void testExecCommandProgressIndicator() {
        final drbd.data.Host localhost = new drbd.data.Host();
        localhost.setHostname("localhost");
        Tools.execCommandProgressIndicator(localhost, /* host */
                                           "command h",
                                           null, /* ExecCallback */
                                           true, /* outputVisible */
                                           "text h",
                                           1000); /* command timeout */
    }

    public void testAppWarning() {
        Tools.appWarning("warning a");
        if (Tools.getDefault("AppWarning").equals("y")) {
            assertEquals(APPWARNING_STRING + "warning a\n", stdout.toString());
        }
        clearStdout();
    }

    public void testInfoDialog() {
        if (INTERACTIVE) {
            Tools.infoDialog("info a", "info1 a", "info2 a / CLICK OK");
        }
    }

    public void testIsIp() {
        assertTrue(Tools.isIp("127.0.0.1"));
        assertTrue(Tools.isIp("0.0.0.0"));
        assertTrue(Tools.isIp("0.0.0.1"));
        assertTrue(Tools.isIp("255.255.255.255"));
        assertTrue(Tools.isIp("254.255.255.255"));

        assertFalse(Tools.isIp("localhost"));
        assertFalse(Tools.isIp("127-0-0-1"));
        assertFalse(Tools.isIp("256.255.255.255"));
        assertFalse(Tools.isIp("255.256.255.255"));
        assertFalse(Tools.isIp("255.255.256.255"));
        assertFalse(Tools.isIp("255.255.255.256"));

        assertFalse(Tools.isIp("255.255.255.1000"));
        assertFalse(Tools.isIp("255.255.255.-1"));
        assertFalse(Tools.isIp("255.255.255"));
        assertFalse(Tools.isIp(""));
        assertFalse(Tools.isIp("255.255.255.255.255"));

        assertFalse(Tools.isIp("127.0.0.false"));
        assertFalse(Tools.isIp("127.0.false.1"));
        assertFalse(Tools.isIp("127.false.0.1"));
        assertFalse(Tools.isIp("false.0.0.1"));
    }

    public void testPrintStackTrace() {
        Tools.printStackTrace();
        assertFalse("".equals(stdout.toString()));
        clearStdout();
        Tools.printStackTrace("stack trace test");
        assertTrue(stdout.toString().startsWith("stack trace test"));
        clearStdout();
        assertFalse("".equals(Tools.getStackTrace()));
    }

    public void testLoadFile() {
        assertNull(Tools.loadFile("JUNIT_TEST_FILE_CLICK_OK", INTERACTIVE));
        final String testFile = "/tmp/drbd-mc-test-file";
        Tools.save(testFile);
        assertTrue(stdout.toString().indexOf("saved:") >= 0);
        clearStdout();
        final String file = Tools.loadFile(testFile, INTERACTIVE);
        assertNotNull(file);
        assertFalse("".equals(file));
    }

    public void testRemoveEverything() {
        //TODO:
        //Tools.removeEverything();
    }

    public void testGetDefault() {
        if (INTERACTIVE) {
            assertNull(Tools.getDefault("JUNIT TEST JUNIT TEST, click ok"));
            assertTrue(stdout.toString().indexOf("unresolved") >= 0);
            clearStdout();
        }
        assertEquals("", Tools.getDefault("SSH.PublicKey"));
        assertEquals("", Tools.getDefault("SSH.PublicKey"));
        assertEquals("22", Tools.getDefault("SSH.Port"));
    }

    public void testGetDefaultColor() {
        if (INTERACTIVE) {
            assertEquals(
                      java.awt.Color.WHITE,
                      Tools.getDefault("JUNIT TEST unknown color, click ok"));
            clearStdout();
        }
        assertEquals(java.awt.Color.BLACK,
                     Tools.getDefaultColor("TerminalPanel.Background"));
    }

    public void testGetDefaultInt() {
        if (INTERACTIVE) {
            assertEquals(
                      0, 
                      Tools.getDefaultInt("JUNIT TEST unknown int, click ok"));
            clearStdout();
        }
        assertEquals(100000,
                     Tools.getDefaultInt("Score.Infinity"));
    }

    public void testGetString() {
        final String testString = "JUNIT TEST unknown string, click ok";
        if (INTERACTIVE) {
            assertEquals(testString, Tools.getString(testString));
            clearStdout();
        }
        assertEquals("DRBD Management Console", 
                     Tools.getString("DrbdMC.Title"));
    }

    public void testGetErrorString() {
        final String errorString = "the same string";
        assertEquals(errorString, errorString);
    }

    public void testGetDistString() {
        /* text, dist, version, arch */
        assertNull(Tools.getDistString("none",
                                       "none",
                                       "none",
                                       "none"));
        assertEquals("no", Tools.getDistString("Support",
                                               "none",
                                               "none",
                                               "none"));
        assertEquals("no", Tools.getDistString("Support",
                                               null,
                                               null,
                                               null));
        assertEquals("debian", Tools.getDistString("Support",
                                                   "debian",
                                                   null,
                                                   null));
        assertEquals("debian-SQUEEZE", Tools.getDistString("Support",
                                                           "debian",
                                                           "SQUEEZE",
                                                           null));
        assertEquals("debian-SQUEEZE", Tools.getDistString("Support",
                                                           "debian",
                                                           "SQUEEZE",
                                                           "a"));
        assertEquals("i586", Tools.getDistString("PmInst.install",
                                                 "suse",
                                                 null,
                                                 "i386"));
    }

    public void testGetDistCommand() {
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
                                          false));
        assertEquals(APPWARNING_STRING + "unknown command: undefined\n",
                     stdout.toString());
        clearStdout();

        assertEquals("undefined2;;;undefined3",
                     Tools.getDistCommand("undefined2;;;undefined3",
                                          "debian",
                                          "squeeze",
                                          "i386",
                                          null,
                                          false));
        assertEquals(APPWARNING_STRING + "unknown command: undefined2\n"
                     + APPWARNING_STRING + "unknown command: undefined3\n",
                     stdout.toString());
        clearStdout();
        final drbd.utilities.ConvertCmdCallback ccc =
                                    new drbd.utilities.ConvertCmdCallback() {
            @Override public String convert(final String command) {
                return command.replaceAll(drbd.configs.DistResource.SUDO,
                                          "sudo ");
            }
        };
        assertEquals("sudo /etc/init.d/corosync start",
                     Tools.getDistCommand("Corosync.startCorosync",
                                          "debian",
                                          "squeeze",
                                          "i386",
                                          ccc,
                                          false));
        assertEquals("sudo bash -c \"sudo /etc/init.d/corosync start\"",
                     Tools.getDistCommand("Corosync.startCorosync",
                                          "debian",
                                          "squeeze",
                                          "i386",
                                          ccc,
                                          true));
        assertEquals("sudo /etc/init.d/corosync start"
                     + ";;;sudo /etc/init.d/corosync start",
                     Tools.getDistCommand("Corosync.startCorosync;;;"
                                          + "Corosync.startCorosync",
                                          "debian",
                                          "squeeze",
                                          "i386",
                                          ccc,
                                          false));
        assertEquals("undefined4"
                     + ";;;sudo /etc/init.d/corosync start",
                     Tools.getDistCommand("undefined4;;;"
                                          + "Corosync.startCorosync",
                                          "debian",
                                          "squeeze",
                                          "i386",
                                          ccc,
                                          false));
        assertEquals(APPWARNING_STRING + "unknown command: undefined4\n",
                     stdout.toString());
        clearStdout();
        assertNull(Tools.getDistCommand(null,
                                        "debian",
                                        "squeeze",
                                        "i386",
                                        ccc,
                                        false));
        assertNull(Tools.getDistCommand(null,
                                        "debian",
                                        "squeeze",
                                        "i386",
                                        ccc,
                                        true));
        assertNull(Tools.getDistCommand(null, null, null, null, null, true));
    }

    public void testGetKernelDownloadDir() {
        /* String kernelVersion
           String dist
           String version
           String arch
         */
         assertNull(Tools.getKernelDownloadDir(null, null, null, null));
         assertEquals("2.6.32-28",
                      Tools.getKernelDownloadDir("2.6.32-28-server",
                                                 "ubuntu",
                                                 "lucid",
                                                 "x86_64"));
         assertEquals("2.6.32-28",
                      Tools.getKernelDownloadDir("2.6.32-28-server",
                                                 "ubuntu",
                                                 "lucid",
                                                 "i386"));
         assertEquals("2.6.24-28",
                      Tools.getKernelDownloadDir("2.6.24-28-server",
                                                 "ubuntu",
                                                 "hardy",
                                                 "x86_64"));
         assertEquals("2.6.32.27-0.2",
                      Tools.getKernelDownloadDir("2.6.32.27-0.2-default",
                                                 "suse",
                                                 "SLES11",
                                                 "x86_64"));
         assertEquals("2.6.16.60-0.60.1",
                      Tools.getKernelDownloadDir("2.6.16.60-0.60.1-default",
                                                 "suse",
                                                 "SLES10",
                                                 "x86_64"));
         assertEquals("2.6.18-194.8.1.el5",
                      Tools.getKernelDownloadDir("2.6.18-194.8.1.el5",
                                                 "redhatenterpriseserver",
                                                 "5",
                                                 "x86_64"));
         assertEquals("2.6.32-71.18.1.el6.x86_64",
                      Tools.getKernelDownloadDir("2.6.32-71.18.1.el6.x86_64",
                                                 "redhatenterpriseserver",
                                                 "6",
                                                 "x86_64"));
         assertEquals("2.6.26-2",
                      Tools.getKernelDownloadDir("2.6.26-2-amd64",
                                                 "debian",
                                                 "lenny",
                                                 "x86_64"));
         assertEquals("2.6.32-5",
                      Tools.getKernelDownloadDir("2.6.32-5-amd64",
                                                 "debian",
                                                 "squeeze",
                                                 "x86_64"));
         assertEquals("2.6.32-5",
                      Tools.getKernelDownloadDir("2.6.32-5-amd64",
                                                 "debian",
                                                 "unknown",
                                                 "x86_64"));
         assertEquals("2.6.32-5-amd64",
                      Tools.getKernelDownloadDir("2.6.32-5-amd64",
                                                 "unknown",
                                                 "unknown",
                                                 "x86_64"));
         assertNull(Tools.getKernelDownloadDir(null,
                                               "unknown",
                                               "unknown",
                                               "x86_64"));
         assertEquals("2.6.32-5-amd64",
                      Tools.getKernelDownloadDir("2.6.32-5-amd64",
                                                 null,
                                                 null,
                                                 "x86_64"));
    }

    public void testLast() {
        //Tools.confirmDialog("all tests finished", "", "done", "done");
    }
}
