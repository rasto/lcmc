package lcmc.configs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parameterized tests that validate structural properties across all DistResource_ classes.
 */
class DistResourceAllDistributionsTest {

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

    static Stream<Arguments> allDistributions() {
        return Stream.of(
                Arguments.of(new DistResource_debian(), "debian"),
                Arguments.of(new DistResource_debian_3(), "debian-3"),
                Arguments.of(new DistResource_debian_ETCH(), "debian-4"),
                Arguments.of(new DistResource_debian_LENNY(), "debian-LENNY"),
                Arguments.of(new DistResource_debian_SQUEEZE(), "debian-SQUEEZE"),
                Arguments.of(new DistResource_debian_JESSIE(), "debian-JESSIE"),
                Arguments.of(new DistResource_redhat(), "redhat"),
                Arguments.of(new DistResource_redhat_5(), "redhat-5"),
                Arguments.of(new DistResource_redhat_6(), "redhat-6"),
                Arguments.of(new DistResource_redhat_7(), "redhat-7"),
                Arguments.of(new DistResource_redhat_8(), "redhat-8"),
                Arguments.of(new DistResource_redhat_9(), "redhat-9"),
                Arguments.of(new DistResource_redhatenterpriseas(), "redhatenterpriseas"),
                Arguments.of(new DistResource_redhatenterpriseas_4(), "redhatenterpriseas-4"),
                Arguments.of(new DistResource_redhatenterpriseas_4_0(), "redhatenterpriseas-4-0"),
                Arguments.of(new DistResource_redhatenterpriseserver(), "redhatenterpriseserver"),
                Arguments.of(new DistResource_redhatenterpriseserver_5(), "redhatenterpriseserver-5"),
                Arguments.of(new DistResource_redhatenterpriseserver_6(), "redhatenterpriseserver-6"),
                Arguments.of(new DistResource_redhatenterpriseserver_7(), "redhat-7"),
                Arguments.of(new DistResource_ubuntu(), "ubuntu"),
                Arguments.of(new DistResource_ubuntu_DAPPER(), "ubuntu-DAPPER"),
                Arguments.of(new DistResource_ubuntu_HARDY(), "ubuntu-HARDY"),
                Arguments.of(new DistResource_ubuntu_INTREPID(), "ubuntu-INTREPID"),
                Arguments.of(new DistResource_ubuntu_JAUNTY(), "ubuntu-JAUNTY"),
                Arguments.of(new DistResource_ubuntu_KARMIC(), "ubuntu-KARMIC"),
                Arguments.of(new DistResource_ubuntu_LUCID(), "ubuntu-LUCID"),
                Arguments.of(new DistResource_ubuntu_VIVID(), "ubuntu-VIVID"),
                Arguments.of(new DistResource_suse(), "suse"),
                Arguments.of(new DistResource_suse_SLES9(), "suse-SLES9"),
                Arguments.of(new DistResource_suse_SLES10(), "suse-SLES10"),
                Arguments.of(new DistResource_suse_SLES11(), "suse-SLES11"),
                Arguments.of(new DistResource_suse_OPENSUSE11_1(), "suse-OPENSUSE11_1"),
                Arguments.of(new DistResource_suse_OPENSUSE11_2(), "suse-OPENSUSE11_2"),
                Arguments.of(new DistResource_suse_OPENSUSE11_3(), "suse-OPENSUSE11_3"),
                Arguments.of(new DistResource_suse_OPENSUSE11_4(), "suse-OPENSUSE11_4"),
                Arguments.of(new DistResource_suse_OPENSUSE13_1(), "suse-OPENSUSE13_1"),
                Arguments.of(new DistResource_suse_OPENSUSE15(), "suse-OPENSUSE13_1"),
                Arguments.of(new DistResource_fedora(), "fedora"),
                Arguments.of(new DistResource_openfiler(), "openfiler"),
                Arguments.of(new DistResource_openfiler_2(), "openfiler-2")
        );
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("allDistributions")
    void shouldExtendListResourceBundle(ListResourceBundle bundle, String expectedSupport) {
        assertTrue(bundle instanceof ListResourceBundle);
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("allDistributions")
    void shouldHaveCorrectSupportValue(ListResourceBundle bundle, String expectedSupport) {
        assertEquals(expectedSupport, bundle.getString("Support"));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("allDistributions")
    void getContentsShouldReturnNonNullNonEmptyArray(ListResourceBundle bundle, String expectedSupport) {
        Object[][] contents = getContents(bundle);
        assertNotNull(contents, expectedSupport + ": contents should not be null");
        assertTrue(contents.length > 0, expectedSupport + ": contents should not be empty");
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("allDistributions")
    void getContentsShouldReturnDefensiveCopy(ListResourceBundle bundle, String expectedSupport) {
        Object[][] contents1 = getContents(bundle);
        Object[][] contents2 = getContents(bundle);
        assertFalse(contents1 == contents2,
                expectedSupport + ": getContents() should return a defensive copy");
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("allDistributions")
    void allEntriesShouldBeKeyValuePairs(ListResourceBundle bundle, String expectedSupport) {
        Object[][] contents = getContents(bundle);
        for (int i = 0; i < contents.length; i++) {
            assertNotNull(contents[i], expectedSupport + ": entry " + i + " should not be null");
            assertEquals(2, contents[i].length,
                    expectedSupport + ": entry " + i + " should have exactly 2 elements");
        }
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("allDistributions")
    void allKeysShouldBeNonNullNonEmptyStrings(ListResourceBundle bundle, String expectedSupport) {
        Object[][] contents = getContents(bundle);
        for (int i = 0; i < contents.length; i++) {
            assertNotNull(contents[i][0], expectedSupport + ": key at index " + i + " should not be null");
            assertTrue(contents[i][0] instanceof String,
                    expectedSupport + ": key at index " + i + " should be a String");
            assertFalse(((String) contents[i][0]).isEmpty(),
                    expectedSupport + ": key at index " + i + " should not be empty");
        }
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("allDistributions")
    void allValuesShouldBeNonNull(ListResourceBundle bundle, String expectedSupport) {
        Object[][] contents = getContents(bundle);
        for (int i = 0; i < contents.length; i++) {
            String key = (String) contents[i][0];
            assertNotNull(contents[i][1], expectedSupport + ": value for key '" + key + "' should not be null");
        }
    }

    /**
     * Known duplicate keys in source files (bugs in existing code).
     * debian-LENNY: PmInst.install.text.1 appears twice
     * redhat-6: Corosync.addToRc appears twice
     */
    private static final Set<String> DISTRIBUTIONS_WITH_KNOWN_DUPLICATE_KEYS = new HashSet<>(
            Arrays.asList("debian-LENNY", "redhat-6"));

    @ParameterizedTest(name = "{1}")
    @MethodSource("allDistributions")
    void shouldNotHaveDuplicateKeys(ListResourceBundle bundle, String expectedSupport) {
        if (DISTRIBUTIONS_WITH_KNOWN_DUPLICATE_KEYS.contains(expectedSupport)) {
            // These distributions have known duplicate keys in the source code
            return;
        }
        Object[][] contents = getContents(bundle);
        Set<String> keys = new HashSet<>();
        for (Object[] entry : contents) {
            String key = (String) entry[0];
            assertTrue(keys.add(key), expectedSupport + ": duplicate key found: " + key);
        }
    }

    @Test
    void debianLennyShouldHaveKnownDuplicateKey() {
        Object[][] contents = getContents(new DistResource_debian_LENNY());
        int count = 0;
        for (Object[] entry : contents) {
            if ("PmInst.install.text.1".equals(entry[0])) count++;
        }
        assertEquals(2, count, "debian-LENNY has a known duplicate key PmInst.install.text.1");
    }

    @Test
    void redhat6ShouldHaveKnownDuplicateKey() {
        Object[][] contents = getContents(new DistResource_redhat_6());
        int count = 0;
        for (Object[] entry : contents) {
            if ("Corosync.addToRc".equals(entry[0])) count++;
        }
        assertEquals(2, count, "redhat-6 has a known duplicate key Corosync.addToRc");
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("allDistributions")
    void stringValuesShouldNotBeNull(ListResourceBundle bundle, String expectedSupport) {
        Object[][] contents = getContents(bundle);
        for (Object[] entry : contents) {
            String key = (String) entry[0];
            Object value = entry[1];
            if (value instanceof String) {
                assertNotNull(value, expectedSupport + ": string value for '" + key + "' should not be null");
            }
        }
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("allDistributions")
    void listValuesShouldContainOnlyStrings(ListResourceBundle bundle, String expectedSupport) {
        Object[][] contents = getContents(bundle);
        for (Object[] entry : contents) {
            String key = (String) entry[0];
            Object value = entry[1];
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                for (Object item : list) {
                    assertTrue(item instanceof String,
                            expectedSupport + ": list items for '" + key + "' should be Strings");
                }
            }
        }
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("allDistributions")
    void valuesShouldBeStringOrList(ListResourceBundle bundle, String expectedSupport) {
        Object[][] contents = getContents(bundle);
        for (Object[] entry : contents) {
            String key = (String) entry[0];
            Object value = entry[1];
            assertTrue(value instanceof String || value instanceof List,
                    expectedSupport + ": value for '" + key + "' should be String or List, but was "
                            + value.getClass().getName());
        }
    }

    // --- Support value uniqueness ---

    @Test
    void allSupportValuesShouldMatchExpectedSet() {
        Set<String> expectedValues = new HashSet<>(Arrays.asList(
                "debian", "debian-3", "debian-4", "debian-LENNY", "debian-SQUEEZE", "debian-JESSIE",
                "redhat", "redhat-5", "redhat-6", "redhat-7", "redhat-8", "redhat-9",
                "redhatenterpriseas", "redhatenterpriseas-4", "redhatenterpriseas-4-0",
                "redhatenterpriseserver", "redhatenterpriseserver-5", "redhatenterpriseserver-6",
                "ubuntu", "ubuntu-DAPPER", "ubuntu-HARDY", "ubuntu-INTREPID",
                "ubuntu-JAUNTY", "ubuntu-KARMIC", "ubuntu-LUCID", "ubuntu-VIVID",
                "suse", "suse-SLES9", "suse-SLES10", "suse-SLES11",
                "suse-OPENSUSE11_1", "suse-OPENSUSE11_2", "suse-OPENSUSE11_3", "suse-OPENSUSE11_4",
                "suse-OPENSUSE13_1",
                "fedora",
                "openfiler", "openfiler-2"
        ));

        Set<String> actualValues = new HashSet<>();
        allDistributions().forEach(args -> {
            Object[] arr = args.get();
            ListResourceBundle bundle = (ListResourceBundle) arr[0];
            actualValues.add(bundle.getString("Support"));
        });

        assertEquals(expectedValues, actualValues,
                "Set of Support values should match expected distributions");
    }

    // --- Version pattern keys ---

    static Stream<Arguments> distributionsWithVersionKeys() {
        return Stream.of(
                Arguments.of(new DistResource_debian(), "debian"),
                Arguments.of(new DistResource_redhat(), "redhat"),
                Arguments.of(new DistResource_ubuntu(), "ubuntu"),
                Arguments.of(new DistResource_suse(), "suse"),
                Arguments.of(new DistResource_openfiler(), "openfiler")
        );
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("distributionsWithVersionKeys")
    void baseDistributionsShouldHaveVersionMappings(ListResourceBundle bundle, String name) {
        Object[][] contents = getContents(bundle);
        boolean hasVersionKey = false;
        for (Object[] entry : contents) {
            String key = (String) entry[0];
            if (key.startsWith("version:")) {
                hasVersionKey = true;
                assertTrue(entry[1] instanceof String,
                        name + ": version mapping value should be a String for key " + key);
                break;
            }
        }
        assertTrue(hasVersionKey, name + ": base distribution should have at least one version mapping");
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("distributionsWithVersionKeys")
    void versionMappingValuesShouldBeNonEmpty(ListResourceBundle bundle, String name) {
        Object[][] contents = getContents(bundle);
        for (Object[] entry : contents) {
            String key = (String) entry[0];
            if (key.startsWith("version:") && entry[1] instanceof String) {
                assertTrue(key.length() > "version:".length(),
                        name + ": version key pattern should not be empty: " + key);
            }
        }
    }
}
