package drbd.utilities;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import drbd.TestSuite1;
import drbd.data.Host;

public final class SSHTest1 extends TestCase {
    @Before
    protected void setUp() {
        TestSuite1.initTest();
    }

    @After
    protected void tearDown() {
        assertEquals("", TestSuite1.getStdout());
    }

    /* ---- tests ----- */
    @Test
    public void testInstallTestFiles() {
        for (final Host host : TestSuite1.getHosts()) {
            host.getSSH().installTestFiles();
        }
    }
}
