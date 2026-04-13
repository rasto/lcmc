package lcmc.configs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive tests for SUSE family DistResource classes.
 */
class DistResource_suseTest {

    // --- DistResource_suse (base) ---

    private final DistResource_suse suse = new DistResource_suse();

    @Test
    void suseShouldHaveCorrectSupport() {
        assertEquals("suse", suse.getString("Support"));
    }

    @Test
    void suseShouldHaveVersionMappings() {
        assertEquals("SLES9", suse.getString("version:SUSE LINUX Enterprise Server 9 (i586)"));
        assertEquals("SLES9", suse.getString("version:9*"));
        assertEquals("SLES10", suse.getString("version:SUSE Linux Enterprise Server 10*"));
        assertEquals("OPENSUSE11_1", suse.getString("version:openSUSE 11.1*"));
        assertEquals("OPENSUSE11_2", suse.getString("version:openSUSE 11.2*"));
        assertEquals("OPENSUSE11_3", suse.getString("version:openSUSE 11.3*"));
        assertEquals("OPENSUSE11_4", suse.getString("version:openSUSE 11.4*"));
        assertEquals("OPENSUSE13_1", suse.getString("version:openSUSE 13.*"));
        assertEquals("OPENSUSE13_1", suse.getString("version:openSUSE 42.*"));
        assertEquals("OPENSUSE15", suse.getString("version:openSUSE*"));
        assertEquals("SLES11", suse.getString("version:SUSE Linux Enterprise Server 11*"));
        assertEquals("SLES9", suse.getString("version:sles9"));
        assertEquals("SLES10", suse.getString("version:sles10"));
        assertEquals("SLES11", suse.getString("version:sles11"));
    }

    @Test
    void suseShouldHaveKerneldirRegex() {
        String kerneldir = suse.getString("kerneldir");
        assertNotNull(kerneldir);
        assertTrue(kerneldir.contains("\\d+"));
    }

    @Test
    void suseShouldUseDrbdInstallWithRpm() {
        String cmd = suse.getString("DrbdInst.install");
        assertTrue(cmd.contains("rpm"), "DRBD install should use rpm");
        assertTrue(cmd.contains("@DRBDPACKAGES@"), "Should have packages placeholder");
    }

    @Test
    void suseShouldHaveArchMappings() {
        assertEquals("i586", suse.getString("HbPmInst.install.i386"));
        assertEquals("i586", suse.getString("HbPmInst.install.i486"));
        assertEquals("i586", suse.getString("HbPmInst.install.i586"));
        assertEquals("i586", suse.getString("PmInst.install.i386"));
        assertEquals("i586", suse.getString("PmInst.install.i486"));
        assertEquals("i586", suse.getString("PmInst.install.i586"));
    }

    @Test
    void suseShouldUseChkconfigForServiceManagement() {
        assertTrue(suse.getString("Heartbeat.deleteFromRc").contains("chkconfig"));
        assertTrue(suse.getString("Heartbeat.addToRc").contains("chkconfig"));
        assertTrue(suse.getString("Corosync.addToRc").contains("chkconfig"));
        assertTrue(suse.getString("Corosync.deleteFromRc").contains("chkconfig"));
        assertTrue(suse.getString("Openais.addToRc").contains("chkconfig"));
        assertTrue(suse.getString("Openais.deleteFromRc").contains("chkconfig"));
    }

    @Test
    void suseShouldHaveHbCheckWithRpm() {
        String cmd = suse.getString("HbCheck.version");
        assertTrue(cmd.contains("rpm"), "HbCheck should use rpm");
        assertTrue(cmd.contains("@GUI-HELPER@"), "HbCheck should use GUI-HELPER");
    }

    @Test
    void suseShouldHaveDrbdLogWithMessages() {
        String cmd = suse.getString("DrbdLog.log");
        assertTrue(cmd.contains("/var/log/messages"), "Should grep from /var/log/messages");
    }

    @Test
    void suseShouldHaveKvmEmulator() {
        assertEquals("/usr/bin/qemu-kvm", suse.getString("KVM.emulator"));
    }

    @Test
    void suseShouldHaveZypperPacemakerInstall() {
        assertEquals("zypper install", suse.getString("PmInst.install.text.2"));
        String cmd = suse.getString("PmInst.install.2");
        assertTrue(cmd.contains("zypper"), "Should use zypper");
        assertTrue(cmd.contains("pacemaker"), "Should install pacemaker");
    }

    @Test
    void suseShouldHaveDrbdZypperInstall() {
        assertEquals("zypper install", suse.getString("DrbdInst.install.text.2"));
        String cmd = suse.getString("DrbdInst.install.2");
        assertTrue(cmd.contains("zypper"), "Should use zypper");
    }

    @Test
    void suseShouldHaveDrbdSourceInstall() {
        assertEquals("from the source tarball", suse.getString("DrbdInst.install.text.3"));
        assertEquals("source", suse.getString("DrbdInst.install.method.3"));
        assertEquals("true", suse.getString("DrbdInst.install.staging.3"));
        String cmd = suse.getString("DrbdInst.install.3");
        assertTrue(cmd.contains("oss.linbit.com"), "Should download from linbit");
        assertTrue(cmd.contains("zypper"), "Should use zypper for dependencies");
        assertTrue(cmd.contains("kernel-source"), "Should install kernel-source");
    }

    @Test
    void suseShouldHaveLibvirtPaths() {
        assertEquals("/usr/lib64/libvirt", suse.getString("libvirt.lxc.libpath.x86_64"));
        assertEquals("/usr/lib/xen", suse.getString("libvirt.xen.libpath"));
    }

    @Test
    void suseShouldHaveProxyInstall() {
        assertEquals("zypper install", suse.getString("ProxyInst.install.text.1"));
        assertTrue(suse.getString("ProxyInst.install.1").contains("drbd-proxy"));
    }

    @Test
    void suseShouldHaveProxyCheck() {
        String cmd = suse.getString("ProxyCheck.version");
        assertTrue(cmd.contains("rpm"), "ProxyCheck should use rpm");
    }

    // --- DistResource_suse_SLES9 ---

    private final DistResource_suse_SLES9 sles9 = new DistResource_suse_SLES9();

    @Test
    void sles9ShouldHaveCorrectSupport() {
        assertEquals("suse-SLES9", sles9.getString("Support"));
    }

    @Test
    void sles9ShouldHaveDistributionDir() {
        assertEquals("sles9", sles9.getString("distributiondir"));
    }

    // --- DistResource_suse_SLES10 ---

    private final DistResource_suse_SLES10 sles10 = new DistResource_suse_SLES10();

    @Test
    void sles10ShouldHaveCorrectSupport() {
        assertEquals("suse-SLES10", sles10.getString("Support"));
    }

    @Test
    void sles10ShouldHaveDistributionDir() {
        assertEquals("sles10", sles10.getString("distributiondir"));
    }

    @Test
    void sles10ShouldHaveHeartbeatInstall() {
        String cmd = sles10.getString("HbPmInst.install.1");
        assertTrue(cmd.contains("zypper"), "Should use zypper");
        assertTrue(cmd.contains("heartbeat"), "Should install heartbeat");
    }

    @Test
    void sles10ShouldHaveDrbdSourceInstall() {
        String cmd = sles10.getString("DrbdInst.install.2");
        assertTrue(cmd.contains("oss.linbit.com"), "Should download from linbit");
        assertTrue(cmd.contains("zypper"), "Should use zypper for dependencies");
    }

    // --- DistResource_suse_SLES11 ---

    private final DistResource_suse_SLES11 sles11 = new DistResource_suse_SLES11();

    @Test
    void sles11ShouldHaveCorrectSupport() {
        assertEquals("suse-SLES11", sles11.getString("Support"));
    }

    @Test
    void sles11ShouldHaveDistributionDir() {
        assertEquals("sles11", sles11.getString("distributiondir"));
    }

    @Test
    void sles11ShouldHaveDrbdLoad() {
        String cmd = sles11.getString("DRBD.load");
        assertNotNull(cmd);
        assertTrue(cmd.contains("modprobe"), "DRBD.load should use modprobe");
    }

    // --- DistResource_suse_OPENSUSE11_1 ---

    private final DistResource_suse_OPENSUSE11_1 opensuse111 = new DistResource_suse_OPENSUSE11_1();

    @Test
    void opensuse111ShouldHaveCorrectSupport() {
        assertEquals("suse-OPENSUSE11_1", opensuse111.getString("Support"));
    }

    @Test
    void opensuse111ShouldHaveDistributionDir() {
        assertEquals("sles11", opensuse111.getString("distributiondir"));
    }

    @Test
    void opensuse111ShouldHaveDrbdLoad() {
        String cmd = opensuse111.getString("DRBD.load");
        assertNotNull(cmd);
        assertTrue(cmd.contains("modprobe"), "DRBD.load should use modprobe");
    }

    // --- DistResource_suse_OPENSUSE11_2 ---

    private final DistResource_suse_OPENSUSE11_2 opensuse112 = new DistResource_suse_OPENSUSE11_2();

    @Test
    void opensuse112ShouldHaveCorrectSupport() {
        assertEquals("suse-OPENSUSE11_2", opensuse112.getString("Support"));
    }

    @Test
    void opensuse112ShouldHaveDistributionDir() {
        assertEquals("sles11", opensuse112.getString("distributiondir"));
    }

    @Test
    void opensuse112ShouldHaveDrbdLoad() {
        String cmd = opensuse112.getString("DRBD.load");
        assertNotNull(cmd);
    }

    // --- DistResource_suse_OPENSUSE11_3 ---

    private final DistResource_suse_OPENSUSE11_3 opensuse113 = new DistResource_suse_OPENSUSE11_3();

    @Test
    void opensuse113ShouldHaveCorrectSupport() {
        assertEquals("suse-OPENSUSE11_3", opensuse113.getString("Support"));
    }

    @Test
    void opensuse113ShouldHaveDistributionDir() {
        assertEquals("sles11", opensuse113.getString("distributiondir"));
    }

    @Test
    void opensuse113ShouldHaveDrbdLoad() {
        String cmd = opensuse113.getString("DRBD.load");
        assertNotNull(cmd);
    }

    // --- DistResource_suse_OPENSUSE11_4 ---

    private final DistResource_suse_OPENSUSE11_4 opensuse114 = new DistResource_suse_OPENSUSE11_4();

    @Test
    void opensuse114ShouldHaveCorrectSupport() {
        assertEquals("suse-OPENSUSE11_4", opensuse114.getString("Support"));
    }

    @Test
    void opensuse114ShouldHaveDistributionDir() {
        assertEquals("sles11", opensuse114.getString("distributiondir"));
    }

    // --- DistResource_suse_OPENSUSE13_1 ---

    private final DistResource_suse_OPENSUSE13_1 opensuse131 = new DistResource_suse_OPENSUSE13_1();

    @Test
    void opensuse131ShouldHaveCorrectSupport() {
        assertEquals("suse-OPENSUSE13_1", opensuse131.getString("Support"));
    }

    @Test
    void opensuse131ShouldHaveDistributionDir() {
        assertEquals("sles11", opensuse131.getString("distributiondir"));
    }

    @Test
    void opensuse131ShouldUseSystemctlForCorosync() {
        String cmd = opensuse131.getString("Corosync.startCorosync");
        assertTrue(cmd.contains("systemctl") || cmd.contains("service"),
                "OpenSUSE 13 should use systemctl or service");
    }

    // --- DistResource_suse_OPENSUSE15 ---

    private final DistResource_suse_OPENSUSE15 opensuse15 = new DistResource_suse_OPENSUSE15();

    @Test
    void opensuse15ShouldHaveCorrectSupport() {
        // NOTE: this is the actual value in the source code
        assertEquals("suse-OPENSUSE13_1", opensuse15.getString("Support"));
    }

    @Test
    void opensuse15ShouldHaveDistributionDir() {
        assertEquals("sles11", opensuse15.getString("distributiondir"));
    }

    // --- Cross-cutting SUSE tests ---

    @Test
    void allSuseVersionsShouldUseDistributiondirSles11() {
        // All SUSE version-specific configs share the same distributiondir
        assertEquals("sles11", opensuse111.getString("distributiondir"));
        assertEquals("sles11", opensuse112.getString("distributiondir"));
        assertEquals("sles11", opensuse113.getString("distributiondir"));
        assertEquals("sles11", opensuse114.getString("distributiondir"));
        assertEquals("sles11", opensuse131.getString("distributiondir"));
        assertEquals("sles11", opensuse15.getString("distributiondir"));
        assertEquals("sles11", sles11.getString("distributiondir"));
    }
}
