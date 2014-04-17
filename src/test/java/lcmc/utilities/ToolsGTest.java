package lcmc.utilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import lcmc.testutils.TestSuite1;
import lcmc.testutils.annotation.type.GuiTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(GuiTest.class)
public final class ToolsGTest {
    private final TestSuite1 testSuite = new TestSuite1();
    @Before
    public void setUp() {
        testSuite.initMain();
    }

    @Test
    public void testLoadFile() {
        assertNull(Tools.loadFile("JUNIT_TEST_FILE_CLICK_OK", false));
        final String testFile = "/tmp/lcmc-test-file";
        Tools.save(testFile, false);
        final String file = Tools.loadFile(testFile, false);
        assertNotNull(file);
        testSuite.clearStdout();
        assertFalse("".equals(file));
    }

    @Test
    public void testStartProgressIndicator() {
        for (int i = 0; i < 10; i++) {
            Tools.startProgressIndicator(null);
            Tools.startProgressIndicator("test");
            Tools.startProgressIndicator("test2");
            Tools.startProgressIndicator("test3");
            Tools.startProgressIndicator(null, "test4");
            Tools.startProgressIndicator("name", "test4");
            Tools.startProgressIndicator("name2", "test4");
            Tools.startProgressIndicator("name2", null);
            Tools.startProgressIndicator(null, null);
            Tools.stopProgressIndicator(null, null);
            Tools.stopProgressIndicator("name2", null);
            Tools.stopProgressIndicator("name2", "test4");
            Tools.stopProgressIndicator("name", "test4");
            Tools.stopProgressIndicator(null, "test4");
            Tools.stopProgressIndicator("test3");
            Tools.stopProgressIndicator("test2");
            Tools.stopProgressIndicator("test");
            Tools.stopProgressIndicator(null);
        }
    }

    @Test
    public void testProgressIndicatorFailed() {
        Tools.progressIndicatorFailed(null, "fail3");
        Tools.progressIndicatorFailed("name", "fail2");
        Tools.progressIndicatorFailed("name", null);
        Tools.progressIndicatorFailed("fail1");
        Tools.progressIndicatorFailed(null);

        Tools.progressIndicatorFailed("fail two seconds", 2);
        testSuite.clearStdout();
    }

    @Test
    public void testIsLinux() {
        if (System.getProperty("os.name").indexOf("Windows") >= 0
            || System.getProperty("os.name").indexOf("windows") >= 0) {
            assertFalse(Tools.isLinux());
        }
        if (System.getProperty("os.name").indexOf("Linux") >= 0
            || System.getProperty("os.name").indexOf("linux") >= 0) {
            assertTrue(Tools.isLinux());
        }
    }

    @Test
    public void testIsWindows() {
        if (System.getProperty("os.name").indexOf("Windows") >= 0
            || System.getProperty("os.name").indexOf("windows") >= 0) {
            assertTrue(Tools.isWindows());
        }
        if (System.getProperty("os.name").indexOf("Linux") >= 0
            || System.getProperty("os.name").indexOf("linux") >= 0) {
            assertFalse(Tools.isWindows());
        }
    }

    @Test
    public void testGetUnixPath() {
        assertEquals("/bin", Tools.getUnixPath("/bin"));
        if (Tools.isWindows()) {
            assertEquals("/bin/dir/file",
                         Tools.getUnixPath("d:\\bin\\dir\\file"));
        }
    }
}
