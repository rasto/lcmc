package lcmc.drbd.service;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(DRBD.compatibleVersions(versionOne, versionTwo)).isTrue();
        assertThat(DRBD.compatibleVersions(versionTwo, versionOne)).isTrue();
    }

    private static Stream<Arguments> parametersForVersionsShouldNotBeCompatible() {
        return Stream.of(Arguments.of("8.4.2", "9.0.5"), Arguments.of("8.3.13", "8.4.3"), Arguments.of("8.3.11", "8.4.3"),
                Arguments.of("8.3.11", "8.4.3"), Arguments.of(null, "8.4.1"), Arguments.of(null, null), Arguments.of("1", "8.4.2"));
    }

    @ParameterizedTest
    @MethodSource("parametersForVersionsShouldNotBeCompatible")
    void versionsShouldNotBeCompatible(final String versionOne, final String versionTwo) {
        assertThat(DRBD.compatibleVersions(versionOne, versionTwo)).describedAs(versionTwo + " not compatible with " + versionOne)
                .isFalse();
        assertThat(DRBD.compatibleVersions(versionTwo, versionOne)).describedAs(versionOne + " not compatible with " + versionTwo)
                .isFalse();
    }
}
