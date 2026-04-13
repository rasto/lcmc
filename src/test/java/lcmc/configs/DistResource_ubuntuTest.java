package lcmc.configs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive tests for Ubuntu family DistResource classes.
 */
class DistResource_ubuntuTest {

    // --- DistResource_ubuntu (base) ---

    private final DistResource_ubuntu ubuntu = new DistResource_ubuntu();

    @Test
    void ubuntuShouldHaveCorrectSupport() {
        assertEquals("ubuntu", ubuntu.getString("Support"));
    }

    @Test
    void ubuntuShouldHaveVersionMappings() {
        assertEquals("", ubuntu.getString("version:7.04"));
        assertEquals("VIVID", ubuntu.getString("version:jessie/sid/15.*"));
        assertEquals("LUCID", ubuntu.getString("version:squeeze/sid/10.04"));
        assertEquals("KARMIC", ubuntu.getString("version:squeeze/sid/9.10"));
        assertEquals("JAUNTY", ubuntu.getString("version:5.0/9.04"));
        assertEquals("INTREPID", ubuntu.getString("version:lenny/sid/8.10"));
        assertEquals("HARDY", ubuntu.getString("version:lenny/sid/8.04"));
        assertEquals("DAPPER", ubuntu.getString("version:testing/unstable/6.06"));
    }

    @Test
    void ubuntuShouldMapX86_64ToAmd64() {
        assertEquals("amd64", ubuntu.getString("arch:x86_64"));
    }

    @Test
    void ubuntuShouldHaveKerneldir() {
        String kerneldir = ubuntu.getString("kerneldir");
        assertNotNull(kerneldir);
        assertTrue(kerneldir.contains("\\d+"));
    }

    @Test
    void ubuntuShouldHaveDrbdInstallWithDpkg() {
        String cmd = ubuntu.getString("DrbdInst.install");
        assertTrue(cmd.contains("dpkg"), "Should use dpkg");
        assertTrue(cmd.contains("@DRBDPACKAGES@"), "Should have packages placeholder");
    }

    @Test
    void ubuntuShouldHaveHeartbeatPacemakerInstall() {
        assertEquals("apt-get install", ubuntu.getString("HbPmInst.install.text.1"));
        String cmd = ubuntu.getString("HbPmInst.install.1");
        assertTrue(cmd.contains("DEBIAN_FRONTEND=noninteractive"), "Should use non-interactive");
        assertTrue(cmd.contains("pacemaker"), "Should install pacemaker");
        assertTrue(cmd.contains("heartbeat"), "Should install heartbeat");
    }

    @Test
    void ubuntuShouldHaveCorosyncPacemakerInstall() {
        assertEquals("apt-get install", ubuntu.getString("PmInst.install.text.1"));
        String cmd = ubuntu.getString("PmInst.install.1");
        assertTrue(cmd.contains("DEBIAN_FRONTEND=noninteractive"), "Should use non-interactive");
        assertTrue(cmd.contains("corosync"), "Should install corosync");
        assertTrue(cmd.contains("pacemaker"), "Should install pacemaker");
    }

    @Test
    void ubuntuShouldHaveDrbdAptGetInstall() {
        assertEquals("apt-get install", ubuntu.getString("DrbdInst.install.text.2"));
        String cmd = ubuntu.getString("DrbdInst.install.2");
        assertTrue(cmd.contains("DEBIAN_FRONTEND=noninteractive"), "Should use non-interactive");
        assertTrue(cmd.contains("apt-get"), "Should use apt-get");
    }

    @Test
    void ubuntuShouldHaveDrbdSourceInstall() {
        assertEquals("from the source tarball", ubuntu.getString("DrbdInst.install.text.3"));
        assertEquals("source", ubuntu.getString("DrbdInst.install.method.3"));
        assertEquals("true", ubuntu.getString("DrbdInst.install.staging.3"));
        String cmd = ubuntu.getString("DrbdInst.install.3");
        assertTrue(cmd.contains("oss.linbit.com"), "Should download from linbit");
        assertTrue(cmd.contains("DEBIAN_FRONTEND=noninteractive"), "Should use non-interactive");
    }

    @Test
    void ubuntuShouldHaveHbCheckWithDpkgQuery() {
        String cmd = ubuntu.getString("HbCheck.version");
        assertTrue(cmd.contains("dpkg-query"), "HbCheck should use dpkg-query");
        assertTrue(cmd.contains("@GUI-HELPER@"), "HbCheck should use GUI-HELPER");
    }

    @Test
    void ubuntuShouldUseUpdateRcDForServiceManagement() {
        assertTrue(ubuntu.getString("Heartbeat.deleteFromRc").contains("update-rc.d"));
        assertTrue(ubuntu.getString("Heartbeat.addToRc").contains("update-rc.d"));
        assertTrue(ubuntu.getString("Corosync.addToRc").contains("update-rc.d"));
        assertTrue(ubuntu.getString("Corosync.deleteFromRc").contains("update-rc.d"));
        assertTrue(ubuntu.getString("Openais.addToRc").contains("update-rc.d"));
        assertTrue(ubuntu.getString("Openais.deleteFromRc").contains("update-rc.d"));
    }

    @Test
    void ubuntuShouldHavePacemakerFromSourceInstall() {
        assertEquals("from source: latest/1.1.x", ubuntu.getString("PmInst.install.text.2"));
        assertEquals("true", ubuntu.getString("PmInst.install.staging.2"));
        String cmd = ubuntu.getString("PmInst.install.2");
        assertTrue(cmd.contains("DEBIAN_FRONTEND=noninteractive"), "Should use non-interactive");
        assertTrue(cmd.contains("pacemaker"), "Should reference pacemaker");
    }

    @Test
    void ubuntuShouldHaveProxyInstall() {
        assertEquals("apt-get install", ubuntu.getString("ProxyInst.install.text.1"));
        String cmd = ubuntu.getString("ProxyInst.install.1");
        assertTrue(cmd.contains("DEBIAN_FRONTEND=noninteractive"), "Should use non-interactive");
        assertTrue(cmd.contains("drbd-proxy"), "Should install drbd-proxy");
    }

    @Test
    void ubuntuShouldHaveProxyCheck() {
        String cmd = ubuntu.getString("ProxyCheck.version");
        assertTrue(cmd.contains("dpkg-query"), "ProxyCheck should use dpkg-query");
    }

    @Test
    void ubuntuShouldHaveInstallFiles() {
        String files = ubuntu.getString("PmInst.install.files.2");
        assertTrue(files.contains("corosync"), "Install files should reference corosync");
    }

    // --- DistResource_ubuntu_DAPPER ---

    private final DistResource_ubuntu_DAPPER dapper = new DistResource_ubuntu_DAPPER();

    @Test
    void dapperShouldHaveCorrectSupport() {
        assertEquals("ubuntu-DAPPER", dapper.getString("Support"));
    }

    @Test
    void dapperShouldHaveDistributionDir() {
        assertEquals("ubuntu-dapper-server", dapper.getString("distributiondir"));
    }

    // --- DistResource_ubuntu_HARDY ---

    private final DistResource_ubuntu_HARDY hardy = new DistResource_ubuntu_HARDY();

    @Test
    void hardyShouldHaveCorrectSupport() {
        assertEquals("ubuntu-HARDY", hardy.getString("Support"));
    }

    @Test
    void hardyShouldHaveDistributionDir() {
        assertEquals("ubuntu-hardy-server", hardy.getString("distributiondir"));
    }

    @Test
    void hardyShouldHaveDrbdInstallFromSource() {
        String cmd = hardy.getString("DrbdInst.install.2");
        assertNotNull(cmd);
        assertTrue(cmd.contains("oss.linbit.com"), "Should download from linbit");
    }

    @Test
    void hardyShouldHaveOpenaisForceReload() {
        String cmd = hardy.getString("Openais.reloadOpenais");
        assertTrue(cmd.contains("force-reload"), "Should use force-reload for OpenAIS");
    }

    // --- DistResource_ubuntu_INTREPID ---

    private final DistResource_ubuntu_INTREPID intrepid = new DistResource_ubuntu_INTREPID();

    @Test
    void intrepidShouldHaveCorrectSupport() {
        assertEquals("ubuntu-INTREPID", intrepid.getString("Support"));
    }

    @Test
    void intrepidShouldHaveDistributionDir() {
        assertEquals("ubuntu-inrepid-server", intrepid.getString("distributiondir"));
    }

    // --- DistResource_ubuntu_JAUNTY ---

    private final DistResource_ubuntu_JAUNTY jaunty = new DistResource_ubuntu_JAUNTY();

    @Test
    void jauntyShouldHaveCorrectSupport() {
        assertEquals("ubuntu-JAUNTY", jaunty.getString("Support"));
    }

    @Test
    void jauntyShouldHaveDistributionDir() {
        assertEquals("ubuntu-jaunty-server", jaunty.getString("distributiondir"));
    }

    // --- DistResource_ubuntu_KARMIC ---

    private final DistResource_ubuntu_KARMIC karmic = new DistResource_ubuntu_KARMIC();

    @Test
    void karmicShouldHaveCorrectSupport() {
        assertEquals("ubuntu-KARMIC", karmic.getString("Support"));
    }

    @Test
    void karmicShouldHaveDistributionDir() {
        assertEquals("ubuntu-karmic-server", karmic.getString("distributiondir"));
    }

    // --- DistResource_ubuntu_LUCID ---

    private final DistResource_ubuntu_LUCID lucid = new DistResource_ubuntu_LUCID();

    @Test
    void lucidShouldHaveCorrectSupport() {
        assertEquals("ubuntu-LUCID", lucid.getString("Support"));
    }

    @Test
    void lucidShouldHaveDistributionDir() {
        assertEquals("ubuntu-lucid", lucid.getString("distributiondir"));
    }

    @Test
    void lucidShouldHaveMinimalContents() {
        Object[][] contents = lucid.getContents();
        assertEquals(2, contents.length, "LUCID should only have Support and distributiondir");
    }

    // --- DistResource_ubuntu_VIVID ---

    private final DistResource_ubuntu_VIVID vivid = new DistResource_ubuntu_VIVID();

    @Test
    void vividShouldHaveCorrectSupport() {
        assertEquals("ubuntu-VIVID", vivid.getString("Support"));
    }

    @Test
    void vividShouldUseSystemctlForServiceManagement() {
        assertTrue(vivid.getString("Heartbeat.addToRc").contains("systemctl"));
        assertTrue(vivid.getString("Heartbeat.deleteFromRc").contains("systemctl"));
        assertTrue(vivid.getString("Corosync.addToRc").contains("systemctl"));
        assertTrue(vivid.getString("Corosync.deleteFromRc").contains("systemctl"));
    }

    @Test
    void vividShouldHaveCorosyncPacemakerInstall() {
        String cmd = vivid.getString("PmInst.install.1");
        assertTrue(cmd.contains("apt-get"), "Should use apt-get");
        assertTrue(cmd.contains("corosync"), "Should install corosync");
        assertTrue(cmd.contains("pacemaker"), "Should install pacemaker");
        assertTrue(cmd.contains("mkdir") && cmd.contains("/var/log/cluster"), "Should create cluster log dir");
    }

    @Test
    void vividShouldHaveHbCheckWithGuiHelper() {
        String cmd = vivid.getString("HbCheck.version");
        assertTrue(cmd.contains("@GUI-HELPER@"), "HbCheck should use GUI-HELPER");
    }
}
