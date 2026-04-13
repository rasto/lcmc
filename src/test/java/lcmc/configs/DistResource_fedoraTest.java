package lcmc.configs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive tests for Fedora DistResource class.
 */
class DistResource_fedoraTest {

    private final DistResource_fedora fedora = new DistResource_fedora();

    @Test
    void fedoraShouldHaveCorrectSupport() {
        assertEquals("fedora", fedora.getString("Support"));
    }

    @Test
    void fedoraShouldHaveRedhatDistribution() {
        assertEquals("redhat", fedora.getString("distribution"));
    }

    @Test
    void fedoraShouldHaveKerneldirRegexWithFc() {
        String kerneldir = fedora.getString("kerneldir");
        assertNotNull(kerneldir);
        assertTrue(kerneldir.contains("fc\\d+"), "Kerneldir should match Fedora Core pattern");
    }

    @Test
    void fedoraShouldUseDrbdInstallWithRpm() {
        String cmd = fedora.getString("DrbdInst.install");
        assertTrue(cmd.contains("rpm"), "DRBD install should use rpm");
        assertTrue(cmd.contains("@DRBDPACKAGES@"), "Should have packages placeholder");
    }

    @Test
    void fedoraShouldHaveDrbdDnfInstall() {
        assertEquals("dnf install", fedora.getString("DrbdInst.install.text.1"));
        String cmd = fedora.getString("DrbdInst.install.1");
        assertTrue(cmd.contains("dnf"), "Should use dnf");
        assertTrue(cmd.contains("drbd-utils"), "Should install drbd-utils");
    }

    @Test
    void fedoraShouldHaveEmptyHeartbeatPacemakerInstall() {
        assertEquals("", fedora.getString("HbPmInst.install.text.1"));
        assertEquals("", fedora.getString("HbPmInst.install.1"));
    }

    @Test
    void fedoraShouldHavePacemakerDnfInstall() {
        assertEquals("dnf install: 1.1.x/1.4.x", fedora.getString("PmInst.install.text.1"));
        String cmd = fedora.getString("PmInst.install.1");
        assertTrue(cmd.contains("dnf"), "Should use dnf");
        assertTrue(cmd.contains("pacemaker"), "Should install pacemaker");
        assertTrue(cmd.contains("corosync"), "Should install corosync");
        assertTrue(cmd.contains("pcs"), "Should install pcs");
        assertTrue(cmd.contains("fence-agents"), "Should install fence-agents");
    }

    @Test
    void fedoraShouldHaveHbCheckWithRpmAndCorosync() {
        String cmd = fedora.getString("HbCheck.version");
        assertTrue(cmd.contains("rpm"), "HbCheck should use rpm");
        assertTrue(cmd.contains("@GUI-HELPER@"), "HbCheck should use GUI-HELPER");
        assertTrue(cmd.contains("corosync"), "HbCheck should check corosync version too");
    }

    @Test
    void fedoraShouldUseSystemctlForServiceManagement() {
        assertTrue(fedora.getString("Heartbeat.addToRc").contains("systemctl enable"));
        assertTrue(fedora.getString("Heartbeat.deleteFromRc").contains("systemctl disable"));
        assertTrue(fedora.getString("Corosync.addToRc").contains("systemctl enable"));
        assertTrue(fedora.getString("Corosync.deleteFromRc").contains("systemctl disable"));
        assertTrue(fedora.getString("Openais.addToRc").contains("systemctl enable"));
        assertTrue(fedora.getString("Openais.deleteFromRc").contains("systemctl disable"));
    }

    @Test
    void fedoraShouldHaveKvmEmulator() {
        assertEquals("/usr/bin/qemu-kvm", fedora.getString("KVM.emulator"));
    }

    @Test
    void fedoraShouldHaveLibvirtPaths() {
        assertEquals("/usr/libexec", fedora.getString("libvirt.lxc.libpath"));
        assertEquals("/usr/lib/xen", fedora.getString("libvirt.xen.libpath"));
    }

    @Test
    void fedoraShouldHaveCorosyncServiceCommands() {
        assertTrue(fedora.getString("Corosync.startCorosync").contains("service corosync start"));
        assertTrue(fedora.getString("Corosync.startPcmk").contains("service pacemaker start"));
        assertTrue(fedora.getString("Corosync.stopCorosync").contains("service corosync stop"));
        assertTrue(fedora.getString("Corosync.stopCorosyncWithPcmk").contains("pacemaker stop"));
        assertTrue(fedora.getString("Corosync.startCorosyncWithPcmk").contains("corosync start"));
    }

    @Test
    void fedoraShouldHaveProxyInstall() {
        assertEquals("dnf install", fedora.getString("ProxyInst.install.text.1"));
        assertTrue(fedora.getString("ProxyInst.install.1").contains("drbd-proxy"));
    }

    @Test
    void fedoraShouldHaveProxyCheck() {
        String cmd = fedora.getString("ProxyCheck.version");
        assertTrue(cmd.contains("rpm"), "ProxyCheck should use rpm");
    }
}
