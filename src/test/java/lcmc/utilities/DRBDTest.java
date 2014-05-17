package lcmc.utilities;

import junitparams.JUnitParamsRunner;
import static junitparams.JUnitParamsRunner.$;
import junitparams.Parameters;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public final class DRBDTest {
    @SuppressWarnings("unused")
    private Object[] parametersForVersionsShouldBeCompatible() {
        return $( 
            $("8.3.11", "8.3.11"),
            $("8.4.1", "8.4.9"),
            $("8.3.11", "8.3.13"),
            $("8.4.3.4", "8.4.1"),
            $("8.4.3", "8.4.1.4"),
            $("9.0.0", "9.0.5.4")
        );
    }

    @Test
    @Parameters(method="parametersForVersionsShouldBeCompatible")
    public void versionsShouldBeCompatible(final String versionOne,
                                           final String versionTwo) {
        assertTrue(DRBD.compatibleVersions(versionOne, versionTwo));
        assertTrue(DRBD.compatibleVersions(versionTwo, versionOne));
    }

    @SuppressWarnings("unused")
    private Object[] parametersForVersionsShouldNotBeCompatible() {
        return $( 
            $("8.4.3", "9.0.5"),
            $("8.3.13", "8.4.3"),
            $("8.3.11", "8.4.3"),
            $("8.3.11", "8.4.3"),
            $(null, "8.4.1"),
            $(null, null),
            $("1", "8.4.2")
        );
    }
    @Test
    @Parameters(method="parametersForVersionsShouldNotBeCompatible")
    public void versionsShouldNotBeCompatible(final String versionOne,
                                              final String versionTwo) {
        assertFalse(DRBD.compatibleVersions(versionOne, versionTwo));
        assertFalse(DRBD.compatibleVersions(versionTwo, versionOne));
    }
}
