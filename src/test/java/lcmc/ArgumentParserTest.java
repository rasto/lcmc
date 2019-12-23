package lcmc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import lcmc.common.domain.Application;
import lcmc.common.ui.Access;
import lcmc.common.ui.main.MainData;

public class ArgumentParserTest {
    @Test
    public void shouldSetSkipNetInterfaceOption() {
        Application application = new Application();
        ArgumentParser argumentParser = new ArgumentParser(null, null, null, application, null, new Access(),
                new MainData());

        argumentParser.parseOptionsAndReturnAutoArguments(new String[]{"--skip-net-interface=skipped", "--skip-net-interface=skipped2"});

        assertTrue(application.isSkipNetInterface("skipped"));
        assertTrue(application.isSkipNetInterface("skipped2"));
        assertFalse(application.isSkipNetInterface("notskipped"));
    }
}