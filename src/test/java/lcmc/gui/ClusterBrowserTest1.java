package lcmc.gui;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import lcmc.utilities.TestSuite1;
import lcmc.utilities.Tools;
import lcmc.utilities.CRM;
import lcmc.data.Host;

public final class ClusterBrowserTest1 extends TestCase {
    @Before
    protected void setUp() {
        TestSuite1.initTest();
    }

    @After
    protected void tearDown() {
        assertEquals("", TestSuite1.getStdout());
    }

    /* ---- tests ----- */

    @Test
    public void testProcessClusterOutput() {
        final CountDownLatch nolatch = new CountDownLatch(0);
        for (final Host host : TestSuite1.getHosts()) {
            final ClusterBrowser cb = host.getBrowser().getClusterBrowser();

            StringBuffer buffer = new StringBuffer("cd");
            cb.processClusterOutput("a---reset---\r\nb",
                                    buffer,
                                    host,
                                    nolatch,
                                    CRM.LIVE);
            assertEquals("cdab", buffer.toString());

            buffer = new StringBuffer("");
            cb.processClusterOutput("a---reset---\r\nb",
                                    buffer,
                                    host,
                                    nolatch,
                                    CRM.LIVE);
            assertEquals("ab", buffer.toString());

            buffer = new StringBuffer("cd");
            cb.processClusterOutput("a---reset---\r\nb",
                                    buffer,
                                    host,
                                    nolatch,
                                    CRM.LIVE);
            assertEquals("cdab", buffer.toString());

            buffer = new StringBuffer("cd");
            cb.processClusterOutput("a---reset---\r\nb---reset---\r\nc",
                                    buffer,
                                    host,
                                    nolatch,
                                    CRM.LIVE);
            assertEquals("cdabc", buffer.toString());
        }
                    
        
        if (TestSuite1.QUICK) {
            return;
        }
        final List<String> files = new ArrayList<String>();
        final String userHome = System.getProperty("user.home");
        files.add(userHome + "/testdir/empty.xml");
        final int repeat = TestSuite1.getFactor() * 2;
        for (final String dirName : new String[]{
                    /* userHome + "/testdir/pacemaker/shell/regression", */
                    userHome + "/testdir/pacemaker/pengine/test10"}) {
            final File dir = new File(dirName);
            assertFalse(dir == null);
            if (dir.listFiles() == null) {
                continue;
            }
            for (final File f : dir.listFiles()) {
                final String file = f.getAbsolutePath();
                if (file.length() > 3
                    && file.substring(file.length() - 4).equals(".xml")) {
                    for (int i = 0; i < repeat; i++) {
                        files.add(file);
                    }
                }
            }
        }
        String emptyCib = null;
        final StringBuilder nodes = new StringBuilder("<nodes>\n");
        for (final Host host : TestSuite1.getHosts()) {
            nodes.append("  <node id=\"");
            nodes.append(host.getName());
            nodes.append("\" uname=\"");
            nodes.append(host.getName());
            nodes.append("\" type=\"normal\"/>\n");
        }
        nodes.append("</nodes>\n");
        final StringBuilder status = new StringBuilder("<status>\n");
        for (final Host host : TestSuite1.getHosts()) {
            status.append("<node_state id=\"");
            status.append(host.getName());
            status.append("\" uname=\"");
            status.append(host.getName());
            status.append("\" ha=\"active\" in_ccm=\"true\" crmd=\"online\" "
                          + "join=\"member\" expected=\"member\" "
                          + "crm-debug-origin=\"do_update_resource\" "
                          + "shutdown=\"0\">");

            status.append("</node_state>\n");
        }
        status.append("</status>\n");
        int i = 0;

        Collections.sort(files);
        for (final String file : files) {
            System.out.println("file: " + file);
            i++;
            if (i > 58 * repeat + 1) {
                break;
            }
            Tools.startProgressIndicator(i + ": " + file);
            String xml = Tools.loadFile(file, true);
            xml = xml.replaceAll("<nodes/>", nodes.toString())
                     .replaceAll("<nodes>.*?</nodes>", nodes.toString())
                     .replaceAll("<status>.*?</status>", status.toString())
                     .replaceAll("<status/>", status.toString())
                     .replaceAll("<\\?.*?\\?>", "");


            final String cib = "---start---\n"
                             + "res_status\n"
                             + "ok\n"
                             + "\n"
                             + ">>>res_status\n"
                             + "cibadmin\n"
                             + "ok\n"
                             + "<pcmk>\n"
                             + xml
                             + "</pcmk>\n"
                             + ">>>cibadmin\n"
                             + "---done---\n";
            if (i == 1) {
                emptyCib = cib;
            }
            final CountDownLatch firstTime = new CountDownLatch(0);
            final boolean testOnly = false;
            for (final Host host : TestSuite1.getHosts()) {
                final ClusterBrowser cb = host.getBrowser().getClusterBrowser();
                cb.processClusterOutput(cib,
                                        new StringBuffer(""),
                                        host,
                                        firstTime,
                                        testOnly);
                Tools.waitForSwing();
                cb.getHeartbeatGraph().repaint();
            }
            Tools.sleep(100);
            Tools.stopProgressIndicator(i + ": " + file);
            for (final Host host : TestSuite1.getHosts()) {
                final ClusterBrowser cb = host.getBrowser().getClusterBrowser();
                Tools.waitForSwing();
                cb.processClusterOutput(emptyCib,
                                        new StringBuffer(""),
                                        host,
                                        firstTime,
                                        testOnly);
                Tools.waitForSwing();
            }
            Tools.sleep(250);
        }
        TestSuite1.clearStdout();
    }
}
