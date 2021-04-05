package lcmc.drbd.service;

import static junitparams.JUnitParamsRunner.$;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import junitparams.Parameters;

@ExtendWith(MockitoExtension.class)
final class DRBDTest {
    private Object[] parametersForVersionsShouldBeCompatible() {
        return $($("8.3.11", "8.3.11"), $("8.4.1", "8.4.9"), $("8.3.11", "8.3.13"), $("8.4.3.4", "8.4.1"), $("8.4.3", "8.4.1.4"),
                $("9.0.0", "9.0.5.4"));
    }

    @Test
    @Parameters(method = "parametersForVersionsShouldBeCompatible")
    void versionsShouldBeCompatible(final String versionOne, final String versionTwo) {
        assertTrue(DRBD.compatibleVersions(versionOne, versionTwo));
        assertTrue(DRBD.compatibleVersions(versionTwo, versionOne));
    }

    private Object[] parametersForVersionsShouldNotBeCompatible() {
        return $( 
            $("8.4.2", "9.0.5"), $("8.3.13", "8.4.3"), $("8.3.11", "8.4.3"), $("8.3.11", "8.4.3"), $(null, "8.4.1"), $(null, null),
                $("1", "8.4.2"));
    }

    @Test
    @Parameters(method = "parametersForVersionsShouldNotBeCompatible")
    void versionsShouldNotBeCompatible(final String versionOne, final String versionTwo) {
        assertFalse(versionTwo + " not compatible with " + versionOne, DRBD.compatibleVersions(versionOne, versionTwo));
        assertFalse(versionOne + " not compatible with " + versionTwo, DRBD.compatibleVersions(versionTwo, versionOne));
    }
}
