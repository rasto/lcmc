package lcmc.drbd.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DRBDTest {
    @ParameterizedTest
    @CsvSource({"8.3.11, 8.3.11, 8.4.1, 8.4.9, 8.3.11 8.3.13, 8.4.3.4, 8.4.1, 8.4.3, 8.4.1.4, 9.0.0, 9.0.5.4"})
    void versionsShouldBeCompatible(final String versionOne, final String versionTwo) {
        assertTrue(DRBD.compatibleVersions(versionOne, versionTwo));
        assertTrue(DRBD.compatibleVersions(versionTwo, versionOne));
    }

    private static Stream<Arguments> parametersForVersionsShouldNotBeCompatible() {
        return Stream.of(Arguments.of("8.4.2", "9.0.5"), Arguments.of("8.3.13", "8.4.3"), Arguments.of("8.3.11", "8.4.3"),
                Arguments.of("8.3.11", "8.4.3"), Arguments.of(null, "8.4.1"), Arguments.of(null, null), Arguments.of("1", "8.4.2"));
    }

    @ParameterizedTest
    @MethodSource("parametersForVersionsShouldNotBeCompatible")
    void versionsShouldNotBeCompatible(final String versionOne, final String versionTwo) {
        assertFalse(versionTwo + " not compatible with " + versionOne, DRBD.compatibleVersions(versionOne, versionTwo));
        assertFalse(versionOne + " not compatible with " + versionTwo, DRBD.compatibleVersions(versionTwo, versionOne));
    }
}
