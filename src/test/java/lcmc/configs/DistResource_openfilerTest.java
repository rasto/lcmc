package lcmc.configs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive tests for Openfiler DistResource classes.
 */
class DistResource_openfilerTest {

    // --- DistResource_openfiler ---

    private final DistResource_openfiler openfiler = new DistResource_openfiler();

    @Test
    void openfilerShouldHaveCorrectSupport() {
        assertEquals("openfiler", openfiler.getString("Support"));
    }

    @Test
    void openfilerShouldHaveVersionMapping() {
        assertEquals("2", openfiler.getString("version:Openfiler NSA 2.3"));
    }

    @Test
    void openfilerShouldUseChkconfigForServiceManagement() {
        assertTrue(openfiler.getString("Heartbeat.deleteFromRc").contains("chkconfig"));
        assertTrue(openfiler.getString("Heartbeat.addToRc").contains("chkconfig"));
        assertTrue(openfiler.getString("Corosync.addToRc").contains("chkconfig"));
        assertTrue(openfiler.getString("Corosync.deleteFromRc").contains("chkconfig"));
        assertTrue(openfiler.getString("Openais.addToRc").contains("chkconfig"));
        assertTrue(openfiler.getString("Openais.deleteFromRc").contains("chkconfig"));
    }

    @Test
    void openfilerShouldHaveRunlevelsInAddToRc() {
        assertTrue(openfiler.getString("Heartbeat.addToRc").contains("--level 2345"));
        assertTrue(openfiler.getString("Corosync.addToRc").contains("--level 2345"));
        assertTrue(openfiler.getString("Openais.addToRc").contains("--level 2345"));
    }

    @Test
    void openfilerShouldHaveDeleteServiceCommands() {
        assertTrue(openfiler.getString("Heartbeat.deleteFromRc").contains("--del heartbeat"));
        assertTrue(openfiler.getString("Corosync.deleteFromRc").contains("--del corosync"));
        assertTrue(openfiler.getString("Openais.deleteFromRc").contains("--del openais"));
    }

    @Test
    void openfilerShouldHaveCompactContents() {
        Object[][] contents = openfiler.getContents();
        assertEquals(8, contents.length,
                "Openfiler should have Support + version + 6 service management entries = 8");
    }

    // --- DistResource_openfiler_2 ---

    private final DistResource_openfiler_2 openfiler2 = new DistResource_openfiler_2();

    @Test
    void openfiler2ShouldHaveCorrectSupport() {
        assertEquals("openfiler-2", openfiler2.getString("Support"));
    }

    @Test
    void openfiler2ShouldHaveMinimalContents() {
        Object[][] contents = openfiler2.getContents();
        assertEquals(1, contents.length, "Openfiler 2 should only have Support entry");
    }
}
