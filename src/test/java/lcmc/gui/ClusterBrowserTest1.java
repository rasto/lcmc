package lcmc.gui;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.awt.Color;
import java.util.concurrent.CountDownLatch;

import lcmc.utilities.TestSuite1;
import lcmc.utilities.Tools;
import lcmc.utilities.SSH;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.SSH.ExecCommandThread;
import lcmc.gui.ClusterBrowser;
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
        final List<String> files = new ArrayList<String>();
        for (final String dirName : new String[]{
                    "/home/rasto/testdir/pacemaker-1.1.5/shell/regression",
                    "/home/rasto/testdir/pacemaker-1.1.5/pengine/test10"}) {
            final File dir = new File(dirName);
            for (final File f : dir.listFiles()) {
                final String file = f.getAbsolutePath();
                if (file.length() > 3) {
                    System.out.println("s: " + file.substring(file.length() - 4));
                }
                if (file.length() > 3
                    && file.substring(file.length() - 4).equals(".xml")) {
                    files.add(file);
                }
            }
        }
        int i = 0; 
        for (final String file : files) {
            i++;
            if (i % 4 != 0) {
                continue;
            }
            Tools.startProgressIndicator(i + ": " + file);
            final String cib = "---start---\n"
                             + "res_status\n"
                             + "ok\n"
                             + "\n"
                             + ">>>res_status\n"
                             + "cibadmin\n"
                             + "ok\n"
                             + "<pcmk>\n"
                             + Tools.loadFile( file, true)
                             + "</pcmk>\n"
                             + ">>>cibadmin\n"
                             + "---done---\n";
            for (final Host host : TestSuite1.getHosts()) {
                final ClusterBrowser cb = host.getBrowser().getClusterBrowser();
                final CountDownLatch firstTime = new CountDownLatch(0);
                final boolean testOnly = false;
                cb.processClusterOutput(cib,
                                        new StringBuilder(""),
                                        host,
                                        firstTime,
                                        testOnly);
            }
            //Tools.sleep(5000);
            Tools.stopProgressIndicator(i + ": " + file);
        }
    }
}
