package lcmc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import lcmc.common.domain.Application;
import lcmc.common.ui.Access;
import lcmc.common.ui.main.MainData;

class ArgumentParserTest {
    @Test
    void shouldSetSkipNetInterfaceOption() {
        Application application = new Application();
        ArgumentParser argumentParser =
                new ArgumentParser(null, null, null, application, null, new Access(null), new MainData(null));

        argumentParser.parseOptionsAndReturnAutoArguments(
                new String[]{"--skip-net-interface=skipped", "--skip-net-interface=skipped2"});

        assertThat(application.isSkipNetInterface("skipped")).isTrue();
        assertThat(application.isSkipNetInterface("skipped2")).isTrue();
        assertThat(application.isSkipNetInterface("notskipped")).isFalse();
    }
}