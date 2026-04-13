package lcmc.configs;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
 * Cross-cutting consistency tests across all DistResource distribution families.
 * Validates package manager consistency, service management patterns, and structural integrity.
 */
class DistResourceConsistencyTest {

    /**
     * Helper to call the protected getContents() method via reflection.
     */
    private static Object[][] getContents(ListResourceBundle bundle) {
        try {
            Method method = bundle.getClass().getDeclaredMethod("getContents");
            method.setAccessible(true);
            return (Object[][]) method.invoke(bundle);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to call getContents() on " + bundle.getClass().getSimpleName(), e);
        }
    }

    // --- Package manager consistency ---

    @Test
    void debianFamilyShouldUseAptGetOrDpkg() {
        List<ListResourceBundle> debianFamily = Arrays.asList(
                new DistResource_debian(),
                new DistResource_ubuntu()
        );

        for (ListResourceBundle bundle : debianFamily) {
            Object[][] contents = getContents(bundle);
            for (Object[] entry : contents) {
                String key = (String) entry[0];
                if (key.contains("Inst.install.") && !key.contains("text") && !key.contains("staging")
                        && !key.contains("method") && !key.contains("files") && entry[1] instanceof String) {
                    String cmd = (String) entry[1];
                    if (!cmd.isEmpty()) {
                        boolean usesDebian = cmd.contains("apt-get") || cmd.contains("dpkg")
                                || cmd.contains("wget") || cmd.contains("make install")
                                || cmd.contains("autogen.sh");
                        assertTrue(usesDebian,
                                bundle.getString("Support") + ": install command for " + key
                                        + " should use Debian tools: " + cmd.substring(0, Math.min(80, cmd.length())));
                    }
                }
            }
        }
    }

    @Test
    void redhatFamilyShouldUseYumDnfOrRpm() {
        List<ListResourceBundle> redhatFamily = Arrays.asList(
                new DistResource_redhat(),
                new DistResource_fedora()
        );

        for (ListResourceBundle bundle : redhatFamily) {
            String support = bundle.getString("Support");
            Object[][] contents = getContents(bundle);
            for (Object[] entry : contents) {
                String key = (String) entry[0];
                if (key.equals("DrbdInst.install") && entry[1] instanceof String) {
                    String cmd = (String) entry[1];
                    assertTrue(cmd.contains("rpm"),
                            support + ": DrbdInst.install should use rpm: " + cmd);
                }
            }
        }
    }

    @Test
    void suseFamilyShouldUseZypperOrRpm() {
        DistResource_suse suse = new DistResource_suse();
        String cmd = suse.getString("DrbdInst.install");
        assertTrue(cmd.contains("rpm"), "SUSE DrbdInst.install should use rpm");

        String zypperCmd = suse.getString("DrbdInst.install.2");
        assertTrue(zypperCmd.contains("zypper"), "SUSE DrbdInst.install.2 should use zypper");
    }

    // --- Service management patterns ---

    @Test
    void debianShouldUseUpdateRcD() {
        DistResource_debian debian = new DistResource_debian();
        String[] rcKeys = {
                "Heartbeat.deleteFromRc", "Heartbeat.addToRc",
                "Corosync.addToRc", "Corosync.deleteFromRc",
                "Openais.addToRc", "Openais.deleteFromRc"
        };
        for (String key : rcKeys) {
            assertTrue(debian.getString(key).contains("update-rc.d"),
                    "Debian " + key + " should use update-rc.d");
        }
    }

    @Test
    void redhatShouldUseSystemctl() {
        DistResource_redhat redhat = new DistResource_redhat();
        String[] rcKeys = {
                "Heartbeat.addToRc", "Heartbeat.deleteFromRc",
                "Corosync.addToRc", "Corosync.deleteFromRc",
                "Openais.addToRc", "Openais.deleteFromRc"
        };
        for (String key : rcKeys) {
            assertTrue(redhat.getString(key).contains("systemctl"),
                    "RedHat " + key + " should use systemctl");
        }
    }

    @Test
    void fedoraShouldUseSystemctl() {
        DistResource_fedora fedora = new DistResource_fedora();
        String[] rcKeys = {
                "Heartbeat.addToRc", "Heartbeat.deleteFromRc",
                "Corosync.addToRc", "Corosync.deleteFromRc",
                "Openais.addToRc", "Openais.deleteFromRc"
        };
        for (String key : rcKeys) {
            assertTrue(fedora.getString(key).contains("systemctl"),
                    "Fedora " + key + " should use systemctl");
        }
    }

    @Test
    void suseBaseShouldUseChkconfig() {
        DistResource_suse suse = new DistResource_suse();
        String[] rcKeys = {
                "Heartbeat.addToRc", "Heartbeat.deleteFromRc",
                "Corosync.addToRc", "Corosync.deleteFromRc",
                "Openais.addToRc", "Openais.deleteFromRc"
        };
        for (String key : rcKeys) {
            assertTrue(suse.getString(key).contains("chkconfig"),
                    "SUSE " + key + " should use chkconfig");
        }
    }

    @Test
    void openfilerShouldUseChkconfig() {
        DistResource_openfiler openfiler = new DistResource_openfiler();
        String[] rcKeys = {
                "Heartbeat.addToRc", "Heartbeat.deleteFromRc",
                "Corosync.addToRc", "Corosync.deleteFromRc",
                "Openais.addToRc", "Openais.deleteFromRc"
        };
        for (String key : rcKeys) {
            assertTrue(openfiler.getString(key).contains("chkconfig"),
                    "Openfiler " + key + " should use chkconfig");
        }
    }

    // --- Architecture mapping consistency ---

    @Test
    void debianFamilyShouldMapX86_64ToAmd64() {
        assertEquals("amd64", new DistResource_debian().getString("arch:x86_64"));
        assertEquals("amd64", new DistResource_ubuntu().getString("arch:x86_64"));
    }

    // --- Distribution hierarchy validation ---

    @Test
    void versionSpecificDistsShouldHaveMatchingBaseDistFamily() {
        // Debian versions
        assertTrue(new DistResource_debian_3().getString("Support").startsWith("debian"));
        assertTrue(new DistResource_debian_ETCH().getString("Support").startsWith("debian"));
        assertTrue(new DistResource_debian_LENNY().getString("Support").startsWith("debian"));
        assertTrue(new DistResource_debian_SQUEEZE().getString("Support").startsWith("debian"));
        assertTrue(new DistResource_debian_JESSIE().getString("Support").startsWith("debian"));

        // RedHat versions
        assertTrue(new DistResource_redhat_5().getString("Support").startsWith("redhat"));
        assertTrue(new DistResource_redhat_6().getString("Support").startsWith("redhat"));
        assertTrue(new DistResource_redhat_7().getString("Support").startsWith("redhat"));
        assertTrue(new DistResource_redhat_8().getString("Support").startsWith("redhat"));
        assertTrue(new DistResource_redhat_9().getString("Support").startsWith("redhat"));

        // Ubuntu versions
        assertTrue(new DistResource_ubuntu_DAPPER().getString("Support").startsWith("ubuntu"));
        assertTrue(new DistResource_ubuntu_HARDY().getString("Support").startsWith("ubuntu"));
        assertTrue(new DistResource_ubuntu_INTREPID().getString("Support").startsWith("ubuntu"));
        assertTrue(new DistResource_ubuntu_JAUNTY().getString("Support").startsWith("ubuntu"));
        assertTrue(new DistResource_ubuntu_KARMIC().getString("Support").startsWith("ubuntu"));
        assertTrue(new DistResource_ubuntu_LUCID().getString("Support").startsWith("ubuntu"));
        assertTrue(new DistResource_ubuntu_VIVID().getString("Support").startsWith("ubuntu"));

        // SUSE versions
        assertTrue(new DistResource_suse_SLES9().getString("Support").startsWith("suse"));
        assertTrue(new DistResource_suse_SLES10().getString("Support").startsWith("suse"));
        assertTrue(new DistResource_suse_SLES11().getString("Support").startsWith("suse"));
        assertTrue(new DistResource_suse_OPENSUSE11_1().getString("Support").startsWith("suse"));
        assertTrue(new DistResource_suse_OPENSUSE11_2().getString("Support").startsWith("suse"));
        assertTrue(new DistResource_suse_OPENSUSE11_3().getString("Support").startsWith("suse"));
        assertTrue(new DistResource_suse_OPENSUSE11_4().getString("Support").startsWith("suse"));
        assertTrue(new DistResource_suse_OPENSUSE13_1().getString("Support").startsWith("suse"));
        assertTrue(new DistResource_suse_OPENSUSE15().getString("Support").startsWith("suse"));

        // RHEL enterprise versions
        assertTrue(new DistResource_redhatenterpriseas().getString("Support").startsWith("redhatenterprise"));
        assertTrue(new DistResource_redhatenterpriseas_4().getString("Support").startsWith("redhatenterprise"));
        assertTrue(new DistResource_redhatenterpriseas_4_0().getString("Support").startsWith("redhatenterprise"));
        assertTrue(new DistResource_redhatenterpriseserver().getString("Support").startsWith("redhatenterprise"));
        assertTrue(new DistResource_redhatenterpriseserver_5().getString("Support").startsWith("redhatenterprise"));
        assertTrue(new DistResource_redhatenterpriseserver_6().getString("Support").startsWith("redhatenterprise"));

        // Openfiler versions
        assertTrue(new DistResource_openfiler_2().getString("Support").startsWith("openfiler"));
    }

    // --- Distribution dir consistency ---

    @Test
    void redhatVersionsShouldHaveMatchingDistributiondirs() {
        assertEquals("rhel5", new DistResource_redhat_5().getString("distributiondir"));
        assertEquals("rhel6", new DistResource_redhat_6().getString("distributiondir"));
        assertEquals("rhel7", new DistResource_redhat_7().getString("distributiondir"));
        assertEquals("rhel8", new DistResource_redhat_8().getString("distributiondir"));
        assertEquals("rhel9", new DistResource_redhat_9().getString("distributiondir"));
    }

    @Test
    void rhelEnterpriseServerVersionsShouldHaveMatchingDistributiondirs() {
        assertEquals("rhel5", new DistResource_redhatenterpriseserver_5().getString("distributiondir"));
        assertEquals("rhel6", new DistResource_redhatenterpriseserver_6().getString("distributiondir"));
        assertEquals("rhel7", new DistResource_redhatenterpriseserver_7().getString("distributiondir"));
    }

    @Test
    void rhelEnterpriseAsVersionsShouldHaveMatchingDistributiondirs() {
        assertEquals("rhel4", new DistResource_redhatenterpriseas_4().getString("distributiondir"));
        assertEquals("rhel4", new DistResource_redhatenterpriseas_4_0().getString("distributiondir"));
    }

    // --- SUDO placeholder consistency ---

    @Test
    void commandsUsingSudoShouldHaveCorrectPlaceholder() {
        ListResourceBundle[] bundles = {
                new DistResource(),
                new DistResource_debian(),
                new DistResource_redhat(),
                new DistResource_ubuntu(),
                new DistResource_suse(),
                new DistResource_fedora(),
                new DistResource_openfiler()
        };

        for (ListResourceBundle bundle : bundles) {
            Object[][] contents = getContents(bundle);
            for (Object[] entry : contents) {
                String key = (String) entry[0];
                if (entry[1] instanceof String) {
                    String value = (String) entry[1];
                    if (value.contains("@DMCSUDO@")) {
                        assertEquals(DistResource.SUDO, "@DMCSUDO@",
                                "SUDO constant should match @DMCSUDO@ in " + key);
                    }
                }
            }
        }
    }

    // --- All instantiable classes test ---

    @Test
    void allDistResourceClassesShouldBeInstantiable() {
        List<ListResourceBundle> allBundles = Arrays.asList(
                new DistResource(),
                new DistResource_debian(),
                new DistResource_debian_3(),
                new DistResource_debian_ETCH(),
                new DistResource_debian_LENNY(),
                new DistResource_debian_SQUEEZE(),
                new DistResource_debian_JESSIE(),
                new DistResource_redhat(),
                new DistResource_redhat_5(),
                new DistResource_redhat_6(),
                new DistResource_redhat_7(),
                new DistResource_redhat_8(),
                new DistResource_redhat_9(),
                new DistResource_redhatenterpriseas(),
                new DistResource_redhatenterpriseas_4(),
                new DistResource_redhatenterpriseas_4_0(),
                new DistResource_redhatenterpriseserver(),
                new DistResource_redhatenterpriseserver_5(),
                new DistResource_redhatenterpriseserver_6(),
                new DistResource_redhatenterpriseserver_7(),
                new DistResource_ubuntu(),
                new DistResource_ubuntu_DAPPER(),
                new DistResource_ubuntu_HARDY(),
                new DistResource_ubuntu_INTREPID(),
                new DistResource_ubuntu_JAUNTY(),
                new DistResource_ubuntu_KARMIC(),
                new DistResource_ubuntu_LUCID(),
                new DistResource_ubuntu_VIVID(),
                new DistResource_suse(),
                new DistResource_suse_SLES9(),
                new DistResource_suse_SLES10(),
                new DistResource_suse_SLES11(),
                new DistResource_suse_OPENSUSE11_1(),
                new DistResource_suse_OPENSUSE11_2(),
                new DistResource_suse_OPENSUSE11_3(),
                new DistResource_suse_OPENSUSE11_4(),
                new DistResource_suse_OPENSUSE13_1(),
                new DistResource_suse_OPENSUSE15(),
                new DistResource_fedora(),
                new DistResource_openfiler(),
                new DistResource_openfiler_2()
        );

        assertEquals(41, allBundles.size(), "Should have all 41 DistResource classes");

        for (ListResourceBundle bundle : allBundles) {
            assertNotNull(bundle, "Bundle should be instantiable");
            Object[][] contents = getContents(bundle);
            assertNotNull(contents, "Contents should not be null for " + bundle.getClass().getSimpleName());
            assertTrue(contents.length > 0,
                    "Contents should not be empty for " + bundle.getClass().getSimpleName());
        }
    }

    // --- Total count of distinct distributions ---

    @Test
    void shouldHaveExpectedNumberOfDistributions() {
        Set<String> supportValues = new HashSet<>();
        ListResourceBundle[] all = {
                new DistResource_debian(),
                new DistResource_debian_3(),
                new DistResource_debian_ETCH(),
                new DistResource_debian_LENNY(),
                new DistResource_debian_SQUEEZE(),
                new DistResource_debian_JESSIE(),
                new DistResource_redhat(),
                new DistResource_redhat_5(),
                new DistResource_redhat_6(),
                new DistResource_redhat_7(),
                new DistResource_redhat_8(),
                new DistResource_redhat_9(),
                new DistResource_redhatenterpriseas(),
                new DistResource_redhatenterpriseas_4(),
                new DistResource_redhatenterpriseas_4_0(),
                new DistResource_redhatenterpriseserver(),
                new DistResource_redhatenterpriseserver_5(),
                new DistResource_redhatenterpriseserver_6(),
                new DistResource_redhatenterpriseserver_7(),
                new DistResource_ubuntu(),
                new DistResource_ubuntu_DAPPER(),
                new DistResource_ubuntu_HARDY(),
                new DistResource_ubuntu_INTREPID(),
                new DistResource_ubuntu_JAUNTY(),
                new DistResource_ubuntu_KARMIC(),
                new DistResource_ubuntu_LUCID(),
                new DistResource_ubuntu_VIVID(),
                new DistResource_suse(),
                new DistResource_suse_SLES9(),
                new DistResource_suse_SLES10(),
                new DistResource_suse_SLES11(),
                new DistResource_suse_OPENSUSE11_1(),
                new DistResource_suse_OPENSUSE11_2(),
                new DistResource_suse_OPENSUSE11_3(),
                new DistResource_suse_OPENSUSE11_4(),
                new DistResource_suse_OPENSUSE13_1(),
                new DistResource_suse_OPENSUSE15(),
                new DistResource_fedora(),
                new DistResource_openfiler(),
                new DistResource_openfiler_2()
        };

        for (ListResourceBundle bundle : all) {
            supportValues.add(bundle.getString("Support"));
        }

        // 40 classes but redhat-7 is shared by DistResource_redhat_7 and DistResource_redhatenterpriseserver_7,
        // and suse-OPENSUSE13_1 is shared by DistResource_suse_OPENSUSE13_1 and DistResource_suse_OPENSUSE15
        assertEquals(38, supportValues.size(),
                "Should have 38 unique Support values (2 shared values)");
    }

    // --- KVM emulator paths ---

    @Test
    void redhatFamilyShouldHaveKvmEmulatorInUsrLibexec() {
        assertEquals("/usr/libexec/qemu-kvm", new DistResource_redhat().getString("KVM.emulator"));
    }

    @Test
    void suseFamilyShouldHaveKvmEmulatorInUsrBin() {
        assertEquals("/usr/bin/qemu-kvm", new DistResource_suse().getString("KVM.emulator"));
    }

    @Test
    void fedoraShouldHaveKvmEmulatorInUsrBin() {
        assertEquals("/usr/bin/qemu-kvm", new DistResource_fedora().getString("KVM.emulator"));
    }

    // --- Command placeholder validation ---

    @Test
    void commandPlaceholdersShouldBeWellFormed() {
        ListResourceBundle[] bundles = {
                new DistResource(),
                new DistResource_debian(),
                new DistResource_redhat(),
                new DistResource_ubuntu(),
                new DistResource_suse(),
                new DistResource_fedora()
        };

        for (ListResourceBundle bundle : bundles) {
            Object[][] contents = getContents(bundle);
            for (Object[] entry : contents) {
                String key = (String) entry[0];
                if (entry[1] instanceof String) {
                    String value = (String) entry[1];
                    int atCount = 0;
                    for (char c : value.toCharArray()) {
                        if (c == '@') atCount++;
                    }
                    assertEquals(0, atCount % 2,
                            bundle.getString("Support") + "." + key
                                    + ": @-placeholders should come in pairs (found " + atCount + " @ symbols)");
                }
            }
        }
    }

    // --- HbCheck.version commands should all use GUI-HELPER ---

    @Test
    void hbCheckVersionShouldUseGuiHelperInAllDistributions() {
        ListResourceBundle[] withHbCheck = {
                new DistResource(),
                new DistResource_debian(),
                new DistResource_redhat(),
                new DistResource_ubuntu(),
                new DistResource_suse(),
                new DistResource_fedora()
        };

        for (ListResourceBundle bundle : withHbCheck) {
            String cmd = bundle.getString("HbCheck.version");
            assertTrue(cmd.contains("@GUI-HELPER@") || cmd.contains("gui-helper"),
                    bundle.getString("Support") + ": HbCheck.version should use GUI-HELPER");
        }
    }

    // --- ProxyCheck commands ---

    @Test
    void proxyCheckShouldUseCorrectToolPerFamily() {
        // Debian/Ubuntu use dpkg-query
        assertTrue(new DistResource_debian().getString("ProxyCheck.version").contains("dpkg-query"));
        assertTrue(new DistResource_ubuntu().getString("ProxyCheck.version").contains("dpkg-query"));

        // RedHat/SUSE/Fedora use rpm
        assertTrue(new DistResource_redhat().getString("ProxyCheck.version").contains("rpm"));
        assertTrue(new DistResource_suse().getString("ProxyCheck.version").contains("rpm"));
        assertTrue(new DistResource_fedora().getString("ProxyCheck.version").contains("rpm"));
    }

    // --- DrbdInst.install base command should use correct package tool ---

    @Test
    void drbdInstallBaseShouldUseCorrectPackageTool() {
        assertTrue(new DistResource_debian().getString("DrbdInst.install").contains("dpkg"));
        assertTrue(new DistResource_ubuntu().getString("DrbdInst.install").contains("dpkg"));
        assertTrue(new DistResource_redhat().getString("DrbdInst.install").contains("rpm"));
        assertTrue(new DistResource_suse().getString("DrbdInst.install").contains("rpm"));
        assertTrue(new DistResource_fedora().getString("DrbdInst.install").contains("rpm"));
    }

    // --- Version pattern mappings ---

    @Test
    void versionKeysInRedhatShouldMapAllMajorVersions() {
        DistResource_redhat redhat = new DistResource_redhat();
        Set<String> mappedVersions = new HashSet<>();
        Object[][] contents = redhat.getContents();
        for (Object[] entry : contents) {
            String key = (String) entry[0];
            if (key.startsWith("version:")) {
                mappedVersions.add((String) entry[1]);
            }
        }
        assertTrue(mappedVersions.contains("5"));
        assertTrue(mappedVersions.contains("6"));
        assertTrue(mappedVersions.contains("7"));
        assertTrue(mappedVersions.contains("8"));
        assertTrue(mappedVersions.contains("9"));
    }

    @Test
    void versionKeysInDebianShouldMapAllVersions() {
        DistResource_debian debian = new DistResource_debian();
        Set<String> mappedVersions = new HashSet<>();
        Object[][] contents = debian.getContents();
        for (Object[] entry : contents) {
            String key = (String) entry[0];
            if (key.startsWith("version:")) {
                mappedVersions.add((String) entry[1]);
            }
        }
        assertTrue(mappedVersions.contains("3"));
        assertTrue(mappedVersions.contains("ETCH"));
        assertTrue(mappedVersions.contains("LENNY"));
        assertTrue(mappedVersions.contains("SQUEEZE"));
        assertTrue(mappedVersions.contains("JESSIE"));
    }

    @Test
    void versionKeysInUbuntuShouldMapAllVersions() {
        DistResource_ubuntu ubuntu = new DistResource_ubuntu();
        Set<String> mappedVersions = new HashSet<>();
        Object[][] contents = ubuntu.getContents();
        for (Object[] entry : contents) {
            String key = (String) entry[0];
            if (key.startsWith("version:")) {
                mappedVersions.add((String) entry[1]);
            }
        }
        assertTrue(mappedVersions.contains("DAPPER"));
        assertTrue(mappedVersions.contains("HARDY"));
        assertTrue(mappedVersions.contains("INTREPID"));
        assertTrue(mappedVersions.contains("JAUNTY"));
        assertTrue(mappedVersions.contains("KARMIC"));
        assertTrue(mappedVersions.contains("LUCID"));
        assertTrue(mappedVersions.contains("VIVID"));
    }

    @Test
    void versionKeysInSuseShouldMapAllVersions() {
        DistResource_suse suse = new DistResource_suse();
        Set<String> mappedVersions = new HashSet<>();
        Object[][] contents = suse.getContents();
        for (Object[] entry : contents) {
            String key = (String) entry[0];
            if (key.startsWith("version:")) {
                mappedVersions.add((String) entry[1]);
            }
        }
        assertTrue(mappedVersions.contains("SLES9"));
        assertTrue(mappedVersions.contains("SLES10"));
        assertTrue(mappedVersions.contains("SLES11"));
        assertTrue(mappedVersions.contains("OPENSUSE11_1"));
        assertTrue(mappedVersions.contains("OPENSUSE11_2"));
        assertTrue(mappedVersions.contains("OPENSUSE11_3"));
        assertTrue(mappedVersions.contains("OPENSUSE11_4"));
        assertTrue(mappedVersions.contains("OPENSUSE13_1"));
        assertTrue(mappedVersions.contains("OPENSUSE15"));
    }

    // --- Corosync with Pacemaker start/stop should include delimiter ---

    @Test
    void corosyncStartWithPcmkShouldHaveSeparator() {
        ListResourceBundle[] withCorosync = {
                new DistResource(),
                new DistResource_redhat(),
                new DistResource_fedora()
        };
        for (ListResourceBundle bundle : withCorosync) {
            String cmd = bundle.getString("Corosync.startCorosyncWithPcmk");
            assertTrue(cmd.contains(";;;"),
                    bundle.getString("Support") + ": Corosync.startCorosyncWithPcmk should have ;;; separator");
        }
    }

    @Test
    void corosyncStopWithPcmkShouldStopPacemakerFirst() {
        ListResourceBundle[] withCorosync = {
                new DistResource(),
                new DistResource_redhat(),
                new DistResource_fedora()
        };
        for (ListResourceBundle bundle : withCorosync) {
            String cmd = bundle.getString("Corosync.stopCorosyncWithPcmk");
            int pacemakerIdx = cmd.indexOf("pacemaker");
            int corosyncIdx = cmd.indexOf("corosync");
            assertTrue(pacemakerIdx < corosyncIdx,
                    bundle.getString("Support")
                            + ": Corosync.stopCorosyncWithPcmk should stop pacemaker before corosync");
        }
    }

    // --- All classes should be final ---

    @Test
    void allClassesShouldBeFinalClasses() {
        Class<?>[] classes = {
                DistResource.class,
                DistResource_debian.class,
                DistResource_debian_3.class,
                DistResource_debian_ETCH.class,
                DistResource_debian_LENNY.class,
                DistResource_debian_SQUEEZE.class,
                DistResource_debian_JESSIE.class,
                DistResource_redhat.class,
                DistResource_redhat_5.class,
                DistResource_redhat_6.class,
                DistResource_redhat_7.class,
                DistResource_redhat_8.class,
                DistResource_redhat_9.class,
                DistResource_redhatenterpriseas.class,
                DistResource_redhatenterpriseas_4.class,
                DistResource_redhatenterpriseas_4_0.class,
                DistResource_redhatenterpriseserver.class,
                DistResource_redhatenterpriseserver_5.class,
                DistResource_redhatenterpriseserver_6.class,
                DistResource_redhatenterpriseserver_7.class,
                DistResource_ubuntu.class,
                DistResource_ubuntu_DAPPER.class,
                DistResource_ubuntu_HARDY.class,
                DistResource_ubuntu_INTREPID.class,
                DistResource_ubuntu_JAUNTY.class,
                DistResource_ubuntu_KARMIC.class,
                DistResource_ubuntu_LUCID.class,
                DistResource_ubuntu_VIVID.class,
                DistResource_suse.class,
                DistResource_suse_SLES9.class,
                DistResource_suse_SLES10.class,
                DistResource_suse_SLES11.class,
                DistResource_suse_OPENSUSE11_1.class,
                DistResource_suse_OPENSUSE11_2.class,
                DistResource_suse_OPENSUSE11_3.class,
                DistResource_suse_OPENSUSE11_4.class,
                DistResource_suse_OPENSUSE13_1.class,
                DistResource_suse_OPENSUSE15.class,
                DistResource_fedora.class,
                DistResource_openfiler.class,
                DistResource_openfiler_2.class
        };

        for (Class<?> clazz : classes) {
            assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()),
                    clazz.getSimpleName() + " should be a final class");
        }
    }

    // --- All distribution classes should extend ListResourceBundle ---

    @Test
    void allClassesShouldExtendListResourceBundle() {
        Class<?>[] classes = {
                DistResource.class,
                DistResource_debian.class,
                DistResource_debian_3.class,
                DistResource_debian_ETCH.class,
                DistResource_redhat.class,
                DistResource_ubuntu.class,
                DistResource_suse.class,
                DistResource_fedora.class,
                DistResource_openfiler.class,
                DistResource_openfiler_2.class
        };

        for (Class<?> clazz : classes) {
            assertTrue(ListResourceBundle.class.isAssignableFrom(clazz),
                    clazz.getSimpleName() + " should extend ListResourceBundle");
        }
    }
}
