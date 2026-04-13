package lcmc.configs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive tests for Debian family DistResource classes.
 */
class DistResource_debianTest {

    // --- DistResource_debian (base) ---

    private final DistResource_debian debian = new DistResource_debian();

    @Test
    void debianShouldHaveCorrectSupport() {
        assertEquals("debian", debian.getString("Support"));
    }

    @Test
    void debianShouldHaveVersionMappings() {
        assertEquals("3", debian.getString("version:3*"));
        assertEquals("LENNY", debian.getString("version:5*"));
        assertEquals("SQUEEZE", debian.getString("version:6*"));
        assertEquals("JESSIE", debian.getString("version:8*"));
        assertEquals("ETCH", debian.getString("version:debian-etch"));
        assertEquals("ETCH", debian.getString("version:4*"));
    }

    @Test
    void debianShouldMapX86_64ToAmd64() {
        assertEquals("amd64", debian.getString("arch:x86_64"));
    }

    @Test
    void debianShouldHaveKerneldir() {
        String kerneldir = debian.getString("kerneldir");
        assertNotNull(kerneldir);
        assertTrue(kerneldir.contains("\\d+"), "Kerneldir should be a regex pattern");
    }

    @Test
    void debianShouldHaveHeartbeatPacemakerInstall() {
        assertEquals("apt-get install", debian.getString("HbPmInst.install.text.1"));
        String cmd = debian.getString("HbPmInst.install.1");
        assertTrue(cmd.contains("apt-get"), "HbPmInst should use apt-get");
        assertTrue(cmd.contains("heartbeat"), "HbPmInst should install heartbeat");
        assertTrue(cmd.contains("pacemaker"), "HbPmInst should install pacemaker");
    }

    @Test
    void debianShouldHaveCorosyncPacemakerInstall() {
        assertEquals("apt-get install", debian.getString("PmInst.install.text.1"));
        String cmd = debian.getString("PmInst.install.1");
        assertTrue(cmd.contains("apt-get"), "PmInst should use apt-get");
        assertTrue(cmd.contains("corosync"), "PmInst should install corosync");
        assertTrue(cmd.contains("pacemaker"), "PmInst should install pacemaker");
    }

    @Test
    void debianShouldHaveDrbdInstallFromPackage() {
        String cmd = debian.getString("DrbdInst.install");
        assertTrue(cmd.contains("dpkg"), "DrbdInst should use dpkg");
        assertTrue(cmd.contains("@DRBDPACKAGES@"), "DrbdInst should have packages placeholder");
    }

    @Test
    void debianShouldHaveDrbdInstallFromSource() {
        assertEquals("from the source tarball", debian.getString("DrbdInst.install.text.2"));
        assertEquals("true", debian.getString("DrbdInst.install.staging.2"));
        assertEquals("source", debian.getString("DrbdInst.install.method.2"));
        String cmd = debian.getString("DrbdInst.install.2");
        assertTrue(cmd.contains("oss.linbit.com"), "Source install should download from linbit");
        assertTrue(cmd.contains("make install"), "Source install should compile and install");
    }

    @Test
    void debianShouldHaveDrbdInstallViaAptGet() {
        assertEquals("apt-get install", debian.getString("DrbdInst.install.text.3"));
        String cmd = debian.getString("DrbdInst.install.3");
        assertTrue(cmd.contains("apt-get"), "DrbdInst.3 should use apt-get");
    }

    @Test
    void debianShouldUseUpdateRcDForServiceManagement() {
        assertTrue(debian.getString("Heartbeat.deleteFromRc").contains("update-rc.d"));
        assertTrue(debian.getString("Heartbeat.addToRc").contains("update-rc.d"));
        assertTrue(debian.getString("Corosync.addToRc").contains("update-rc.d"));
        assertTrue(debian.getString("Corosync.deleteFromRc").contains("update-rc.d"));
        assertTrue(debian.getString("Openais.addToRc").contains("update-rc.d"));
        assertTrue(debian.getString("Openais.deleteFromRc").contains("update-rc.d"));
    }

    @Test
    void debianShouldHaveHbCheckVersionWithDpkgQuery() {
        String cmd = debian.getString("HbCheck.version");
        assertTrue(cmd.contains("dpkg-query"), "HbCheck should use dpkg-query");
        assertTrue(cmd.contains("@GUI-HELPER@"), "HbCheck should use GUI-HELPER");
    }

    @Test
    void debianShouldHavePacemakerFromSourceInstall() {
        assertEquals("from source: latest/1.1.x", debian.getString("PmInst.install.text.3"));
        assertEquals("true", debian.getString("PmInst.install.staging.3"));
        String cmd = debian.getString("PmInst.install.3");
        assertTrue(cmd.contains("apt-get"), "Source install should use apt-get for deps");
        assertTrue(cmd.contains("autogen.sh"), "Source install should run autogen");
        assertTrue(cmd.contains("pacemaker"), "Source install should reference pacemaker");
    }

    @Test
    void debianShouldHaveHeartbeatFromSourceInstall() {
        assertEquals("from source: latest/3.0.x", debian.getString("HbPmInst.install.text.3"));
        assertEquals("true", debian.getString("HbPmInst.install.staging.3"));
        String cmd = debian.getString("HbPmInst.install.3");
        assertTrue(cmd.contains("Heartbeat"), "Source install should reference Heartbeat");
    }

    @Test
    void debianShouldHaveProxyInstall() {
        assertEquals("apt-get install", debian.getString("ProxyInst.install.text.1"));
        assertTrue(debian.getString("ProxyInst.install.1").contains("drbd-proxy"));
    }

    @Test
    void debianShouldHaveProxyCheck() {
        String cmd = debian.getString("ProxyCheck.version");
        assertTrue(cmd.contains("dpkg-query"), "ProxyCheck should use dpkg-query");
    }

    @Test
    void debianShouldHaveInstallFiles() {
        String files = debian.getString("PmInst.install.files.2");
        assertTrue(files.contains("corosync"), "Install files should reference corosync");
    }

    // --- DistResource_debian_3 ---

    @Test
    void debian3ShouldHaveCorrectSupport() {
        DistResource_debian_3 debian3 = new DistResource_debian_3();
        assertEquals("debian-3", debian3.getString("Support"));
    }

    @Test
    void debian3ShouldHaveMinimalContents() {
        DistResource_debian_3 debian3 = new DistResource_debian_3();
        Object[][] contents = debian3.getContents();
        assertEquals(1, contents.length, "debian-3 should only have Support key");
    }

    // --- DistResource_debian_ETCH ---

    private final DistResource_debian_ETCH debianEtch = new DistResource_debian_ETCH();

    @Test
    void debianEtchShouldHaveCorrectSupport() {
        assertEquals("debian-4", debianEtch.getString("Support"));
    }

    @Test
    void debianEtchShouldHaveDistributionDir() {
        assertEquals("debian-etch", debianEtch.getString("distributiondir"));
    }

    @Test
    void debianEtchShouldHaveDrbdSourceInstall() {
        String cmd = debianEtch.getString("DrbdInst.install.2");
        assertTrue(cmd.contains("oss.linbit.com"), "DRBD source install should download from linbit");
    }

    @Test
    void debianEtchShouldHaveHeartbeatInstall() {
        String text = debianEtch.getString("HbPmInst.install.text.1");
        assertNotNull(text);
    }

    // --- DistResource_debian_LENNY ---

    private final DistResource_debian_LENNY debianLenny = new DistResource_debian_LENNY();

    @Test
    void debianLennyShouldHaveCorrectSupport() {
        assertEquals("debian-LENNY", debianLenny.getString("Support"));
    }

    @Test
    void debianLennyShouldHaveDistributionDir() {
        assertEquals("debian-lenny", debianLenny.getString("distributiondir"));
    }

    @Test
    void debianLennyShouldHaveDrbdInstall() {
        String cmd = debianLenny.getString("DrbdInst.install.3");
        assertTrue(cmd.contains("apt-get"), "DRBD install should use apt-get");
        assertTrue(cmd.contains("drbd8"), "DRBD install should reference drbd8");
    }

    // --- DistResource_debian_SQUEEZE ---

    private final DistResource_debian_SQUEEZE debianSqueeze = new DistResource_debian_SQUEEZE();

    @Test
    void debianSqueezeShouldHaveCorrectSupport() {
        assertEquals("debian-SQUEEZE", debianSqueeze.getString("Support"));
    }

    @Test
    void debianSqueezeShouldHaveDistributionDir() {
        assertEquals("debian-squeeze", debianSqueeze.getString("distributiondir"));
    }

    // --- DistResource_debian_JESSIE ---

    private final DistResource_debian_JESSIE debianJessie = new DistResource_debian_JESSIE();

    @Test
    void debianJessieShouldHaveCorrectSupport() {
        assertEquals("debian-JESSIE", debianJessie.getString("Support"));
    }

    @Test
    void debianJessieShouldHaveDistributionDir() {
        assertEquals("debian-jessie", debianJessie.getString("distributiondir"));
    }
}
