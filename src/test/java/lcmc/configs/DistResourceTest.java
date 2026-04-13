package lcmc.configs;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive tests for the base DistResource class.
 */
class DistResourceTest {

    private final DistResource distResource = new DistResource();

    @Test
    void shouldExtendListResourceBundle() {
        assertTrue(distResource instanceof ListResourceBundle);
    }

    @Test
    void getContentsShouldReturnNonNullArray() {
        Object[][] contents = distResource.getContents();
        assertNotNull(contents);
        assertTrue(contents.length > 0, "Contents should not be empty");
    }

    @Test
    void getContentsShouldReturnDefensiveCopy() {
        Object[][] contents1 = distResource.getContents();
        Object[][] contents2 = distResource.getContents();
        assertFalse(contents1 == contents2, "getContents() should return a new array each time");
    }

    @Test
    void allEntriesShouldBeKeyValuePairs() {
        Object[][] contents = distResource.getContents();
        for (int i = 0; i < contents.length; i++) {
            assertNotNull(contents[i], "Entry " + i + " should not be null");
            assertEquals(2, contents[i].length, "Entry " + i + " should have exactly 2 elements");
        }
    }

    @Test
    void allKeysShouldBeNonNullStrings() {
        Object[][] contents = distResource.getContents();
        for (int i = 0; i < contents.length; i++) {
            assertNotNull(contents[i][0], "Key at index " + i + " should not be null");
            assertTrue(contents[i][0] instanceof String, "Key at index " + i + " should be a String");
            assertFalse(((String) contents[i][0]).isEmpty(), "Key at index " + i + " should not be empty");
        }
    }

    @Test
    void allValuesShouldBeNonNull() {
        Object[][] contents = distResource.getContents();
        for (int i = 0; i < contents.length; i++) {
            String key = (String) contents[i][0];
            assertNotNull(contents[i][1], "Value for key '" + key + "' should not be null");
        }
    }

    @Test
    void shouldNotHaveDuplicateKeys() {
        Object[][] contents = distResource.getContents();
        Set<String> keys = new HashSet<>();
        for (Object[] entry : contents) {
            String key = (String) entry[0];
            assertTrue(keys.add(key), "Duplicate key found: " + key);
        }
    }

    @Test
    void shouldHaveSupportKeySetToNo() {
        assertEquals("no", distResource.getString("Support"));
    }

    @Test
    void shouldHaveUndefinedDistribution() {
        assertEquals("undefined", distResource.getString("distribution"));
    }

    @Test
    void sudoConstantShouldBeDefined() {
        assertEquals("@DMCSUDO@", DistResource.SUDO);
    }

    // --- Architecture mappings ---

    @Test
    void shouldHaveI686ArchMapping() {
        assertEquals("i[3-6]86", distResource.getString("arch:i686"));
    }

    @Test
    void shouldHaveX86_64ArchMapping() {
        assertEquals("x86_64", distResource.getString("arch:x86_64"));
    }

    // --- Distribution mappings ---

    @Test
    void shouldHaveDistributionMappings() {
        assertEquals("suse", distResource.getString("dist:sles"));
        assertEquals("suse", distResource.getString("dist:suse"));
        assertEquals("suse", distResource.getString("dist:opensuse"));
        assertEquals("rhel", distResource.getString("dist:centos"));
        assertEquals("redhat", distResource.getString("dist:almalinux"));
        assertEquals("redhat", distResource.getString("dist:rocky"));
        assertEquals("redhat", distResource.getString("dist:rhas"));
        assertEquals("rhel", distResource.getString("dist:rhel"));
        assertEquals("fedora", distResource.getString("dist:fc"));
        assertEquals("debian", distResource.getString("dist:debian-etch"));
        assertEquals("ubuntu", distResource.getString("dist:ubuntu"));
        assertEquals("ubuntu", distResource.getString("dist:ubuntu-dapper-server"));
        assertEquals("ubuntu", distResource.getString("dist:ubuntu-hardy-server"));
        assertEquals("ubuntu", distResource.getString("dist:ubuntu-jaunty-server"));
        assertEquals("ubuntu", distResource.getString("dist:ubuntu-lucid"));
        assertEquals("ubuntu", distResource.getString("dist:ubuntu-maverick"));
    }

    // --- DRBD commands ---

    @Test
    void shouldHaveDrbdCheckVersion() {
        String cmd = distResource.getString("DrbdCheck.version");
        assertNotNull(cmd);
        assertTrue(cmd.contains("drbdadm"), "DrbdCheck.version should reference drbdadm");
    }

    @Test
    void shouldHaveDrbdCommands() {
        List<String> drbdKeys = Arrays.asList(
                "DRBD.get-gi", "DRBD.attach", "DRBD.detach",
                "DRBD.connect", "DRBD.disconnect",
                "DRBD.pauseSync", "DRBD.resumeSync",
                "DRBD.setPrimary", "DRBD.setSecondary",
                "DRBD.createMDDestroyData", "DRBD.createMD",
                "DRBD.forcePrimary", "DRBD.forcePrimary.8.3", "DRBD.forcePrimary.8.3.7",
                "DRBD.skipInitSync", "DRBD.skipInitSync.8.3", "DRBD.skipInitSync.8.3.2",
                "DRBD.invalidate", "DRBD.discardData",
                "DRBD.resize", "DRBD.verify",
                "DRBD.getDrbdStatus",
                "DRBD.proxyUp", "DRBD.proxyDown",
                "DRBD.startProxy", "DRBD.stopProxy",
                "DRBD.adjust", "DRBD.adjust.apply",
                "DRBD.down", "DRBD.up",
                "DRBD.makeFilesystem",
                "DRBD.delMinor", "DRBD.resDelConnection",
                "DRBD.getProcDrbd", "DRBD.getProcesses",
                "DRBD.showProxyInfo",
                "DRBD.start", "DRBD.load"
        );
        for (String key : drbdKeys) {
            String cmd = distResource.getString(key);
            assertNotNull(cmd, "DRBD command '" + key + "' should exist");
            assertFalse(cmd.isEmpty(), "DRBD command '" + key + "' should not be empty");
        }
    }

    @Test
    void drbdCommandsShouldUseSudoOrDrbdadm() {
        List<String> drbdOperationKeys = Arrays.asList(
                "DRBD.attach", "DRBD.detach", "DRBD.connect", "DRBD.disconnect",
                "DRBD.setPrimary", "DRBD.setSecondary"
        );
        for (String key : drbdOperationKeys) {
            String cmd = distResource.getString(key);
            assertTrue(cmd.contains(DistResource.SUDO) || cmd.contains("drbdadm"),
                    key + " should contain SUDO placeholder or drbdadm");
        }
    }

    @Test
    void drbdCommandsShouldContainPlaceholders() {
        String attach = distResource.getString("DRBD.attach");
        assertTrue(attach.contains("@DRYRUN@"), "DRBD.attach should contain @DRYRUN@ placeholder");
        assertTrue(attach.contains("@RES-VOL@"), "DRBD.attach should contain @RES-VOL@ placeholder");

        String setPrimary = distResource.getString("DRBD.setPrimary");
        assertTrue(setPrimary.contains("@RES@"), "DRBD.setPrimary should contain @RES@ placeholder");
    }

    // --- CRM commands ---

    @Test
    void shouldHaveCrmCommands() {
        List<String> crmKeys = Arrays.asList(
                "CRM.cleanupResource",
                "CRM.startResource", "CRM.stopResource",
                "CRM.isManagedOn", "CRM.isManagedOff",
                "CRM.migrateResource", "CRM.forceMigrateResource",
                "CRM.migrateFromResource", "CRM.unmigrateResource",
                "CRM.standByOn", "CRM.standByOff",
                "CRM.erase", "CRM.configureCommit"
        );
        for (String key : crmKeys) {
            String cmd = distResource.getString(key);
            assertNotNull(cmd, "CRM command '" + key + "' should exist");
            assertFalse(cmd.isEmpty(), "CRM command '" + key + "' should not be empty");
        }
    }

    @Test
    void shouldHaveLegacyCrmCommands() {
        List<String> legacyKeys = Arrays.asList(
                "CRM.2.1.4.startResource", "CRM.2.1.4.stopResource",
                "CRM.2.1.4.isManagedOn", "CRM.2.1.4.isManagedOff",
                "CRM.2.1.4.standByOn", "CRM.2.1.4.standByOff"
        );
        for (String key : legacyKeys) {
            String cmd = distResource.getString(key);
            assertNotNull(cmd, "Legacy CRM command '" + key + "' should exist");
        }
    }

    @Test
    void crmCommandsShouldUseCrmTools() {
        String cleanup = distResource.getString("CRM.cleanupResource");
        assertTrue(cleanup.contains("crm_resource"), "CRM.cleanupResource should use crm_resource");

        String standByOn = distResource.getString("CRM.standByOn");
        assertTrue(standByOn.contains("crm_attribute"), "CRM.standByOn should use crm_attribute");
    }

    // --- Heartbeat commands ---

    @Test
    void shouldHaveHeartbeatCommands() {
        List<String> hbKeys = Arrays.asList(
                "Heartbeat.getOCFParametersQuick",
                "Heartbeat.getOCFParametersConfigured",
                "Heartbeat.getOCFParameters",
                "Heartbeat.getClusterMetadata",
                "Heartbeat.getClStatus",
                "Heartbeat.startHeartbeat", "Heartbeat.stopHeartbeat",
                "Heartbeat.reloadHeartbeat",
                "Heartbeat.getHbConfig",
                "Heartbeat.dopdWorkaround", "Heartbeat.enableDopd"
        );
        for (String key : hbKeys) {
            String cmd = distResource.getString(key);
            assertNotNull(cmd, "Heartbeat command '" + key + "' should exist");
            assertFalse(cmd.isEmpty(), "Heartbeat command '" + key + "' should not be empty");
        }
    }

    @Test
    void heartbeatCommandsShouldUseGuiHelper() {
        assertTrue(distResource.getString("Heartbeat.getOCFParameters").contains("@GUI-HELPER@"));
        assertTrue(distResource.getString("Heartbeat.getClusterMetadata").contains("@GUI-HELPER@"));
        assertTrue(distResource.getString("Heartbeat.getClStatus").contains("@GUI-HELPER@"));
    }

    // --- Corosync commands ---

    @Test
    void shouldHaveCorosyncCommands() {
        List<String> corosyncKeys = Arrays.asList(
                "Corosync.startCorosync", "Corosync.startPcmk",
                "Corosync.stopCorosync", "Corosync.stopCorosyncWithPcmk",
                "Corosync.startCorosyncWithPcmk", "Corosync.reloadCorosync"
        );
        for (String key : corosyncKeys) {
            String cmd = distResource.getString(key);
            assertNotNull(cmd, "Corosync command '" + key + "' should exist");
        }
    }

    // --- Openais commands ---

    @Test
    void shouldHaveOpenaisCommands() {
        List<String> openaisKeys = Arrays.asList(
                "Openais.startOpenais", "Openais.stopOpenais",
                "Openais.stopOpenaisWithPcmk", "Openais.reloadOpenais"
        );
        for (String key : openaisKeys) {
            String cmd = distResource.getString(key);
            assertNotNull(cmd, "OpenAIS command '" + key + "' should exist");
        }
    }

    // --- Virsh commands ---

    @Test
    void shouldHaveVirshCommands() {
        List<String> virshKeys = Arrays.asList(
                "VIRSH.Autostart", "VIRSH.Start", "VIRSH.Shutdown",
                "VIRSH.Destroy", "VIRSH.Reboot", "VIRSH.Suspend",
                "VIRSH.Resume", "VIRSH.Define", "VIRSH.Undefine"
        );
        for (String key : virshKeys) {
            String cmd = distResource.getString(key);
            assertNotNull(cmd, "VIRSH command '" + key + "' should exist");
            assertTrue(cmd.contains("virsh"), key + " should reference virsh");
        }
    }

    // --- LVM commands ---

    @Test
    void shouldHaveLvmCommands() {
        List<String> lvmKeys = Arrays.asList(
                "LVM.pvcreate", "LVM.pvremove",
                "LVM.vgcreate", "LVM.vgremove",
                "LVM.lvcreate", "LVM.lvremove",
                "LVM.resize", "LVM.lvsnapshot"
        );
        for (String key : lvmKeys) {
            String cmd = distResource.getString(key);
            assertNotNull(cmd, "LVM command '" + key + "' should exist");
            assertFalse(cmd.isEmpty(), "LVM command '" + key + "' should not be empty");
        }
    }

    @Test
    void lvmCommandsShouldUseSudo() {
        List<String> lvmKeys = Arrays.asList(
                "LVM.pvcreate", "LVM.pvremove",
                "LVM.vgcreate", "LVM.vgremove",
                "LVM.lvcreate", "LVM.lvremove"
        );
        for (String key : lvmKeys) {
            String cmd = distResource.getString(key);
            assertTrue(cmd.contains(DistResource.SUDO), key + " should use SUDO");
        }
    }

    // --- Host info commands ---

    @Test
    void shouldHaveHostInfoCommands() {
        assertNotNull(distResource.getString("GetHostAllInfo"));
        assertNotNull(distResource.getString("HostHWInfoDaemon"));
        assertNotNull(distResource.getString("GetHostHWInfo"));
        assertNotNull(distResource.getString("GetHostHWInfoLVM"));
        assertNotNull(distResource.getString("GetHostHWInfoLazy"));
        assertNotNull(distResource.getString("GetNetInfo"));
    }

    @Test
    void shouldHaveTestCommand() {
        assertEquals("uptime", distResource.getString("TestCommand"));
    }

    @Test
    void shouldHavePingCommand() {
        String cmd = distResource.getString("PingCommand");
        assertTrue(cmd.contains("sleep"), "PingCommand should contain sleep");
    }

    // --- Config file commands ---

    @Test
    void shouldHaveConfigCommands() {
        assertNotNull(distResource.getString("OpenAIS.getAisConfig"));
        assertNotNull(distResource.getString("Corosync.getAisConfig"));
        assertNotNull(distResource.getString("Heartbeat.getHbConfig"));
    }

    // --- OCF resource parameters ---

    @Test
    void shouldHaveOcfParameters() {
        List<String> ocfKeys = Arrays.asList(
                "ocf:heartbeat:apache.params",
                "ocf:heartbeat:Dummy.params",
                "ocf:heartbeat:CTDB.params",
                "ocf:heartbeat:drbd.params",
                "ocf:linbit:drbd.params",
                "ocf:heartbeat:eDir88.params",
                "ocf:heartbeat:Raid1.params",
                "ocf:heartbeat:Squid.params",
                "ocf:heartbeat:vmware.params",
                "ocf:heartbeat:Xen.params",
                "ocf:heartbeat:mysql-proxy.params"
        );
        for (String key : ocfKeys) {
            Object val = distResource.getObject(key);
            assertNotNull(val, "OCF parameter '" + key + "' should exist");
            assertTrue(val instanceof List, "OCF parameter '" + key + "' should be a List");
            assertFalse(((List<?>) val).isEmpty(), "OCF parameter '" + key + "' should not be empty");
        }
    }

    @Test
    void shouldHaveOcfFiles() {
        List<String> ocfFileKeys = Arrays.asList(
                "ocf:heartbeat:LVM.files",
                "ocf:heartbeat:CTDB.files",
                "ocf:heartbeat:exportfs.files",
                "ocf:heartbeat:Filesystem.files",
                "ocf:heartbeat:iSCSITarget.files",
                "ocf:heartbeat:ManageRAID.files",
                "ocf:heartbeat:nfsserver.files",
                "ocf:lsb:nfs-common.files",
                "ocf:lsb:nfs-kernel-server.files",
                "ocf:heartbeat:oracle.files",
                "ocf:heartbeat:oralsnr.files",
                "ocf:heartbeat:portblock.files",
                "ocf:heartbeat:postfix.files",
                "ocf:heartbeat:SAPDatabase.files",
                "ocf:heartbeat:SAPInstance.files"
        );
        for (String key : ocfFileKeys) {
            Object val = distResource.getObject(key);
            assertNotNull(val, "OCF file list '" + key + "' should exist");
            assertTrue(val instanceof List, "OCF file list '" + key + "' should be a List");
            List<?> fileList = (List<?>) val;
            assertFalse(fileList.isEmpty(), "OCF file list '" + key + "' should not be empty");
            for (Object item : fileList) {
                assertTrue(item instanceof String, "OCF file items should be Strings");
                assertTrue(((String) item).startsWith("/"), "OCF file path should be absolute: " + item);
            }
        }
    }

    @Test
    void ocfDrbdParamsShouldContainDrbdconf() {
        @SuppressWarnings("unchecked")
        List<String> params = (List<String>) distResource.getObject("ocf:heartbeat:drbd.params");
        assertTrue(params.contains("drbdconf"));
    }

    // --- Logging ---

    @Test
    void shouldHaveLogCommands() {
        assertNotNull(distResource.getString("Logs.hbLog"));
        assertNotNull(distResource.getString("DrbdLog.log"));
        assertTrue(distResource.getString("Logs.hbLog").contains("@GREPPATTERN@"));
        assertTrue(distResource.getString("DrbdLog.log").contains("@GREPPATTERN@"));
    }

    // --- Libvirt paths ---

    @Test
    void shouldHaveLibvirtPaths() {
        assertEquals("/usr/lib/libvirt", distResource.getString("libvirt.lxc.libpath"));
        assertEquals("/usr/lib/xen-default", distResource.getString("libvirt.xen.libpath"));
    }

    // --- Kernel panic commands ---

    @Test
    void shouldHaveKernelPanicCommands() {
        String panic = distResource.getString("MakeKernelPanic");
        assertTrue(panic.contains("sysrq-trigger"), "MakeKernelPanic should use sysrq-trigger");

        String reboot = distResource.getString("MakeKernelReboot");
        assertTrue(reboot.contains("sysrq-trigger"), "MakeKernelReboot should use sysrq-trigger");
    }

    // --- Pacemaker service version ---

    @Test
    void shouldHavePacemakerServiceVersion() {
        assertEquals("0", distResource.getString("Pacemaker.Service.Ver"));
    }

    // --- Host browser commands ---

    @Test
    void shouldHaveHostBrowserCommands() {
        String hostInfo = distResource.getString("HostBrowser.getHostInfo");
        assertNotNull(hostInfo);
        assertTrue(hostInfo.contains("crm_mon"), "Should contain crm_mon");
        assertTrue(hostInfo.contains("crm_verify"), "Should contain crm_verify");

        String hostInfoHb = distResource.getString("HostBrowser.getHostInfoHeartbeat");
        assertNotNull(hostInfoHb);
        assertTrue(hostInfoHb.contains("cl_status"), "Should contain cl_status");
    }

    // --- CmdLog commands ---

    @Test
    void shouldHaveCmdLogCommands() {
        assertNotNull(distResource.getString("CmdLog.Raw"));
        assertNotNull(distResource.getString("CmdLog.Processed"));
        assertNotNull(distResource.getString("CmdLog.Clear"));
    }

    // --- DRBD parameters/config ---

    @Test
    void shouldHaveDrbdParametersAndConfig() {
        assertTrue(distResource.getString("Drbd.getParameters").contains("@GUI-HELPER@"));
        assertTrue(distResource.getString("Drbd.getConfig").contains("@GUI-HELPER@"));
    }

    // --- Kerneldir ---

    @Test
    void shouldHaveKerneldirRegex() {
        String kerneldir = distResource.getString("kerneldir");
        assertNotNull(kerneldir);
        assertEquals("(.*)", kerneldir);
    }

    // --- Available versions ---

    @Test
    void shouldHaveDrbdAvailableVersionsSources() {
        String source = distResource.getString("DrbdAvailVersionsSource");
        assertNotNull(source);
        assertTrue(source.contains("oss.linbit.com"), "Should reference linbit download");

        String utilSource = distResource.getString("DrbdUtilAvailVersionsSource");
        assertNotNull(utilSource);
        assertTrue(utilSource.contains("drbd-utils"), "Should reference drbd-utils");
    }

    // --- HbCheck version ---

    @Test
    void shouldHaveHbCheckVersion() {
        String cmd = distResource.getString("HbCheck.version");
        assertTrue(cmd.contains("@GUI-HELPER@"));
    }

    // --- Proxy check ---

    @Test
    void shouldHaveProxyCheckVersion() {
        String cmd = distResource.getString("ProxyCheck.version");
        assertTrue(cmd.contains("drbd-proxy-ctl"));
    }

    // --- Installation info ---

    @Test
    void shouldHaveClusterInstallationInfo() {
        String cmd = distResource.getString("Cluster.Init.getInstallationInfo");
        assertTrue(cmd.contains("installation-info"));
    }

    // --- VMSXML ---

    @Test
    void shouldHaveVmsXmlGetData() {
        String cmd = distResource.getString("VMSXML.GetData");
        assertTrue(cmd.contains("get-vm-info"));
    }

    // --- Host connection status ---

    @Test
    void shouldHaveHostConnectionStatus() {
        assertEquals("true", distResource.getString("Host.getConnectionStatus"));
    }
}
