package lcmc.utilities;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public final class DRBDTest {

    private void cvAssertTrue(final String x, final String y) {
        assertTrue(DRBD.compatibleVersions(x, y));
        assertTrue(DRBD.compatibleVersions(y, x));
    }

    private void cvAssertFalse(final String x, final String y) {
        assertFalse(DRBD.compatibleVersions(x, y));
        assertFalse(DRBD.compatibleVersions(y, x));
    }

    @Test
    public void testCompareVersions() {
        cvAssertTrue("8.3.11", "8.3.11");
        cvAssertTrue("8.4.1", "8.4.9");
        cvAssertTrue("8.3.11", "8.3.13");
        cvAssertTrue("8.4.3.4", "8.4.1");
        cvAssertTrue("8.4.3", "8.4.1.4");
        cvAssertTrue("9.0.0", "9.0.5.4");

        cvAssertFalse("8.4.3", "9.0.5");
        cvAssertFalse("8.3.13", "8.4.3");
        cvAssertFalse("8.3.11", "8.4.3");
        cvAssertFalse("8.3.11", "8.4.3");
        cvAssertFalse(null, "8.4.1");
        cvAssertFalse(null, null);
        cvAssertFalse("1", "8.4.2");
    }
}
