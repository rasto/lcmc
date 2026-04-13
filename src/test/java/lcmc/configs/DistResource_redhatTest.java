package lcmc.configs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive tests for Red Hat family DistResource classes.
 */
class DistResource_redhatTest {

    // --- DistResource_redhat (base) ---

    private final DistResource_redhat redhat = new DistResource_redhat();

    @Test
    void redhatShouldHaveCorrectSupport() {
        assertEquals("redhat", redhat.getString("Support"));
    }

    @Test
    void redhatShouldHaveCorrectDistribution() {
        assertEquals("redhat", redhat.getString("distribution"));
    }

    @Test
    void redhatShouldHaveVersionMappings() {
        assertEquals("4_Nahant_2", redhat.getString(
                "version:Red Hat Enterprise Linux ES release 4 (Nahant Update 2)"));
        assertEquals("5", redhat.getString("version:CentOS release 5*"));
        assertEquals("6", redhat.getString("version:CentOS Linux release 6*"));
        assertEquals("6", redhat.getString("version:CentOS release 6*"));
        assertEquals("6", redhat.getString("version:Red Hat Enterprise Linux Server release 6*"));
        assertEquals("7", redhat.getString("version:CentOS Linux release 7*"));
        assertEquals("8", redhat.getString("version:CentOS Linux release 8*"));
        assertEquals("8", redhat.getString("version:AlmaLinux release 8*"));
        assertEquals("8", redhat.getString("version:Rocky Linux release 8*"));
        assertEquals("9", redhat.getString("version:CentOS Linux release 9*"));
        assertEquals("9", redhat.getString("version:AlmaLinux release 9*"));
        assertEquals("9", redhat.getString("version:Rocky Linux release 9*"));
    }

    @Test
    void redhatShouldHaveKerneldirRegex() {
        String kerneldir = redhat.getString("kerneldir");
        assertTrue(kerneldir.contains("el\\d+"), "Kerneldir should match Enterprise Linux pattern");
    }

    @Test
    void redhatShouldUseDrbdInstallWithRpm() {
        String cmd = redhat.getString("DrbdInst.install");
        assertTrue(cmd.contains("rpm"), "DRBD install should use rpm");
        assertTrue(cmd.contains("@DRBDPACKAGES@"), "DRBD install should reference packages placeholder");
    }

    @Test
    void redhatShouldHaveDrbdSourceInstall() {
        assertEquals("from the source tarball", redhat.getString("DrbdInst.install.text.2"));
        assertEquals("source", redhat.getString("DrbdInst.install.method.2"));
        assertEquals("true", redhat.getString("DrbdInst.install.staging.2"));
        String cmd = redhat.getString("DrbdInst.install.2");
        assertTrue(cmd.contains("yum"), "Source install should use yum for dependencies");
        assertTrue(cmd.contains("oss.linbit.com"), "Should download from linbit");
    }

    @Test
    void redhatShouldHaveDrbdDnfInstall() {
        assertEquals("dnf install: 9.x.x", redhat.getString("DrbdInst.install.text.3"));
        String cmd = redhat.getString("DrbdInst.install.3");
        assertTrue(cmd.contains("dnf"), "DrbdInst.3 should use dnf");
        assertTrue(cmd.contains("kmod-drbd"), "Should install kmod-drbd");
    }

    @Test
    void redhatShouldHavePmInstallViaDnf() {
        assertEquals("dnf install", redhat.getString("PmInst.install.text.2"));
        String cmd = redhat.getString("PmInst.install.2");
        assertTrue(cmd.contains("dnf"), "PmInst should use dnf");
        assertTrue(cmd.contains("pacemaker"), "PmInst should install pacemaker");
        assertTrue(cmd.contains("HighAvailability"), "PmInst should enable HA repo");
    }

    @Test
    void redhatShouldHavePmInstallFromSource() {
        assertEquals("from source: latest/1.1.x", redhat.getString("PmInst.install.text.9"));
        assertEquals("true", redhat.getString("PmInst.install.staging.9"));
        String cmd = redhat.getString("PmInst.install.9");
        assertTrue(cmd.contains("yum"), "Source install should use yum");
        assertTrue(cmd.contains("autogen.sh"), "Should run autogen");
    }

    @Test
    void redhatShouldHaveHbCheckWithRpm() {
        String cmd = redhat.getString("HbCheck.version");
        assertTrue(cmd.contains("rpm"), "HbCheck should use rpm");
        assertTrue(cmd.contains("@GUI-HELPER@"), "HbCheck should use GUI-HELPER");
    }

    @Test
    void redhatShouldUseSystemctlForServiceManagement() {
        assertTrue(redhat.getString("Heartbeat.addToRc").contains("systemctl enable"));
        assertTrue(redhat.getString("Heartbeat.deleteFromRc").contains("systemctl disable"));
        assertTrue(redhat.getString("Corosync.addToRc").contains("systemctl enable"));
        assertTrue(redhat.getString("Corosync.deleteFromRc").contains("systemctl disable"));
        assertTrue(redhat.getString("Openais.addToRc").contains("systemctl enable"));
        assertTrue(redhat.getString("Openais.deleteFromRc").contains("systemctl disable"));
    }

    @Test
    void redhatShouldHaveCorosyncServiceCommands() {
        assertTrue(redhat.getString("Corosync.startCorosync").contains("service corosync start"));
        assertTrue(redhat.getString("Corosync.startPcmk").contains("service pacemaker start"));
        assertTrue(redhat.getString("Corosync.stopCorosync").contains("service corosync stop"));
        assertTrue(redhat.getString("Corosync.stopCorosyncWithPcmk").contains("pacemaker stop"));
        assertTrue(redhat.getString("Corosync.startCorosyncWithPcmk").contains("corosync start"));
    }

    @Test
    void redhatShouldHaveKvmEmulator() {
        assertEquals("/usr/libexec/qemu-kvm", redhat.getString("KVM.emulator"));
    }

    @Test
    void redhatShouldHaveLibvirtPaths() {
        assertEquals("/usr/libexec", redhat.getString("libvirt.lxc.libpath"));
        assertEquals("/usr/lib/xen", redhat.getString("libvirt.xen.libpath"));
    }

    @Test
    void redhatShouldHaveProxyInstall() {
        assertEquals("yum install", redhat.getString("ProxyInst.install.text.1"));
        assertTrue(redhat.getString("ProxyInst.install.1").contains("drbd-proxy"));
    }

    @Test
    void redhatShouldHaveProxyCheck() {
        String cmd = redhat.getString("ProxyCheck.version");
        assertTrue(cmd.contains("rpm"), "ProxyCheck should use rpm");
    }

    // --- DistResource_redhat_5 ---

    private final DistResource_redhat_5 redhat5 = new DistResource_redhat_5();

    @Test
    void redhat5ShouldHaveCorrectSupport() {
        assertEquals("redhat-5", redhat5.getString("Support"));
    }

    @Test
    void redhat5ShouldHaveDistributionDir() {
        assertEquals("rhel5", redhat5.getString("distributiondir"));
    }

    @Test
    void redhat5ShouldHaveArchMapping() {
        assertEquals("i[3-6]86", redhat5.getString("arch:i686"));
    }

    // --- DistResource_redhat_6 ---

    private final DistResource_redhat_6 redhat6 = new DistResource_redhat_6();

    @Test
    void redhat6ShouldHaveCorrectSupport() {
        assertEquals("redhat-6", redhat6.getString("Support"));
    }

    @Test
    void redhat6ShouldHaveDistributionDir() {
        assertEquals("rhel6", redhat6.getString("distributiondir"));
    }

    // --- DistResource_redhat_7 ---

    private final DistResource_redhat_7 redhat7 = new DistResource_redhat_7();

    @Test
    void redhat7ShouldHaveCorrectSupport() {
        assertEquals("redhat-7", redhat7.getString("Support"));
    }

    @Test
    void redhat7ShouldHaveDistributionDir() {
        assertEquals("rhel7", redhat7.getString("distributiondir"));
    }

    // --- DistResource_redhat_8 ---

    private final DistResource_redhat_8 redhat8 = new DistResource_redhat_8();

    @Test
    void redhat8ShouldHaveCorrectSupport() {
        assertEquals("redhat-8", redhat8.getString("Support"));
    }

    @Test
    void redhat8ShouldHaveDistributionDir() {
        assertEquals("rhel8", redhat8.getString("distributiondir"));
    }

    @Test
    void redhat8ShouldUseDnf() {
        String cmd = redhat8.getString("DrbdInst.install.3");
        assertTrue(cmd.contains("dnf"), "RHEL8 should use dnf");
    }

    // --- DistResource_redhat_9 ---

    private final DistResource_redhat_9 redhat9 = new DistResource_redhat_9();

    @Test
    void redhat9ShouldHaveCorrectSupport() {
        assertEquals("redhat-9", redhat9.getString("Support"));
    }

    @Test
    void redhat9ShouldHaveDistributionDir() {
        assertEquals("rhel9", redhat9.getString("distributiondir"));
    }

    @Test
    void redhat9ShouldUseDnf() {
        String cmd = redhat9.getString("DrbdInst.install.3");
        assertTrue(cmd.contains("dnf"), "RHEL9 should use dnf");
    }

    // --- DistResource_redhatenterpriseas ---

    private final DistResource_redhatenterpriseas rhelas = new DistResource_redhatenterpriseas();

    @Test
    void rhelasShouldHaveCorrectSupport() {
        assertEquals("redhatenterpriseas", rhelas.getString("Support"));
    }

    @Test
    void rhelasShouldHaveCorrectDistribution() {
        assertEquals("redhatenterpriseas", rhelas.getString("distribution"));
    }

    @Test
    void rhelasShouldHaveVersionMappings() {
        assertNotNull(rhelas.getString(
                "version:Red Hat Enterprise Linux AS release 4 (Nahant*"));
        assertNotNull(rhelas.getString(
                "version:Red Hat Enterprise Linux AS release 4 (Nahant)/4"));
    }

    // --- DistResource_redhatenterpriseas_4 ---

    private final DistResource_redhatenterpriseas_4 rhelas4 = new DistResource_redhatenterpriseas_4();

    @Test
    void rhelas4ShouldHaveCorrectSupport() {
        assertEquals("redhatenterpriseas-4", rhelas4.getString("Support"));
    }

    @Test
    void rhelas4ShouldHaveDistributionDir() {
        assertEquals("rhel4", rhelas4.getString("distributiondir"));
    }

    // --- DistResource_redhatenterpriseas_4_0 ---

    private final DistResource_redhatenterpriseas_4_0 rhelas40 = new DistResource_redhatenterpriseas_4_0();

    @Test
    void rhelas40ShouldHaveCorrectSupport() {
        assertEquals("redhatenterpriseas-4-0", rhelas40.getString("Support"));
    }

    @Test
    void rhelas40ShouldHaveDistributionDir() {
        assertEquals("rhel4", rhelas40.getString("distributiondir"));
    }

    // --- DistResource_redhatenterpriseserver ---

    private final DistResource_redhatenterpriseserver rhels = new DistResource_redhatenterpriseserver();

    @Test
    void rhelsShouldHaveCorrectSupport() {
        assertEquals("redhatenterpriseserver", rhels.getString("Support"));
    }

    @Test
    void rhelsShouldHaveDistributionRhel() {
        assertEquals("rhel", rhels.getString("distribution"));
    }

    // --- DistResource_redhatenterpriseserver_5 ---

    private final DistResource_redhatenterpriseserver_5 rhels5 = new DistResource_redhatenterpriseserver_5();

    @Test
    void rhels5ShouldHaveCorrectSupport() {
        assertEquals("redhatenterpriseserver-5", rhels5.getString("Support"));
    }

    @Test
    void rhels5ShouldHaveDistributionDir() {
        assertEquals("rhel5", rhels5.getString("distributiondir"));
    }

    // --- DistResource_redhatenterpriseserver_6 ---

    private final DistResource_redhatenterpriseserver_6 rhels6 = new DistResource_redhatenterpriseserver_6();

    @Test
    void rhels6ShouldHaveCorrectSupport() {
        assertEquals("redhatenterpriseserver-6", rhels6.getString("Support"));
    }

    @Test
    void rhels6ShouldHaveDistributionDir() {
        assertEquals("rhel6", rhels6.getString("distributiondir"));
    }

    // --- DistResource_redhatenterpriseserver_7 ---

    private final DistResource_redhatenterpriseserver_7 rhels7 = new DistResource_redhatenterpriseserver_7();

    @Test
    void rhels7ShouldHaveCorrectSupport() {
        assertEquals("redhat-7", rhels7.getString("Support"));
    }

    @Test
    void rhels7ShouldHaveDistributionDir() {
        assertEquals("rhel7", rhels7.getString("distributiondir"));
    }

    @Test
    void rhels7ShouldHaveSystemctlCorosyncCommands() {
        String start = rhels7.getString("Corosync.startCorosync");
        assertTrue(start.contains("systemctl") || start.contains("service"),
                "RHEL7 should use systemctl or service for Corosync");
    }
}
