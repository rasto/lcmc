package lcmc.cluster.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import lcmc.LCMC;
import lcmc.common.domain.Application;
import lcmc.common.ui.main.MainPresenter;
import lcmc.common.ui.main.ProgressIndicator;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Host;
import lcmc.testutils.IntegrationTestLauncher;
import lcmc.testutils.annotation.type.IntegrationTest;
import lcmc.common.domain.util.Tools;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public final class ClusterBrowserITest {
    private IntegrationTestLauncher integrationTestLauncher;
    private MainPresenter mainPresenter;
    private SwingUtils swingUtils;
    private ProgressIndicator progressIndicator;

    @Before
    public void setUp() {
        integrationTestLauncher = LCMC.getInstance(IntegrationTestLauncher.class);
        integrationTestLauncher.initTestCluster();
        mainPresenter = LCMC.getInstance(MainPresenter.class);
        swingUtils = LCMC.getInstance(SwingUtils.class);
        progressIndicator = LCMC.getInstance(ProgressIndicator.class);
    }

    @Test
    public void testProcessClusterOutput() {
        final CountDownLatch nolatch = new CountDownLatch(0);
        for (final Host host : integrationTestLauncher.getHosts()) {
            final ClusterBrowser cb = host.getBrowser().getClusterBrowser();
            assertNotNull("cb is null", cb);

            StringBuffer buffer = new StringBuffer("cd");
            cb.parseClusterOutput("a---reset---\r\nb", buffer, host, nolatch, Application.RunMode.LIVE);
            assertEquals("cdab", buffer.toString());

            buffer = new StringBuffer("");
            cb.parseClusterOutput("a---reset---\r\nb", buffer, host, nolatch, Application.RunMode.LIVE);
            assertEquals("ab", buffer.toString());

            buffer = new StringBuffer("cd");
            cb.parseClusterOutput("a---reset---\r\nb", buffer, host, nolatch, Application.RunMode.LIVE);
            assertEquals("cdab", buffer.toString());

            buffer = new StringBuffer("cd");
            cb.parseClusterOutput("a---reset---\r\nb---reset---\r\nc", buffer, host, nolatch, Application.RunMode.LIVE);
            assertEquals("cdabc", buffer.toString());
        }

        final List<String> files = new ArrayList<String>();
        final String userHome = System.getProperty("user.home");
        files.add(userHome + "/testdir/empty.xml");
        for (final String dirName : new String[]{
            /* userHome + "/testdir/pacemaker/shell/regression", */
            userHome + "/testdir/pacemaker/pengine/test10"}) {
            final File dir = new File(dirName);
            if (dir.listFiles() == null) {
                continue;
            }
            for (final File f : dir.listFiles()) {
                final String file = f.getAbsolutePath();
                if (file.length() > 3
                    && file.substring(file.length() - 4).equals(".xml")) {
                    files.add(file);
                }
            }
        }
        String emptyCib = null;
        final StringBuilder nodes = new StringBuilder("<nodes>\n");
        for (final Host host : integrationTestLauncher.getHosts()) {
            nodes.append("  <node id=\"");
            nodes.append(host.getName());
            nodes.append("\" uname=\"");
            nodes.append(host.getName());
            nodes.append("\" type=\"normal\"/>\n");
        }
        nodes.append("</nodes>\n");
        final StringBuilder status = new StringBuilder("<status>\n");
        for (final Host host : integrationTestLauncher.getHosts()) {
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
        for (String file : files) {
            i++;
            progressIndicator.startProgressIndicator(i + ": " + file);
            String xml = Tools.loadFile(mainPresenter, file, true);
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
            final Application.RunMode runMode = Application.RunMode.LIVE;
            for (final Host host : integrationTestLauncher.getHosts()) {
                final ClusterBrowser cb = host.getBrowser().getClusterBrowser();
                cb.getClusterViewPanel().setDisabledDuringLoad(true);
                cb.parseClusterOutput(cib, new StringBuffer(""), host, firstTime, runMode);
                swingUtils.waitForSwing();
                cb.getClusterViewPanel().setDisabledDuringLoad(false);
                cb.getCrmGraph().repaint();
            }
            progressIndicator.stopProgressIndicator(i + ": " + file);
            for (final Host host : integrationTestLauncher.getHosts()) {
                final ClusterBrowser cb = host.getBrowser().getClusterBrowser();
                swingUtils.waitForSwing();
                cb.parseClusterOutput(emptyCib, new StringBuffer(""), host, firstTime, runMode);
                swingUtils.waitForSwing();
            }
        }
    }
}
