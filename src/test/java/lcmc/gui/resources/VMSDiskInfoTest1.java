package lcmc.gui.resources;

import junit.framework.TestCase;
import lcmc.utilities.TestSuite1;
import lcmc.data.VMSXML.DiskData;
import lcmc.gui.Browser;
import lcmc.gui.ClusterBrowser;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import java.util.Map;
import java.util.HashMap;
import mockit.Deencapsulation;

public final class VMSDiskInfoTest1 extends TestCase {
    //final VMSDiskInfo vmsdi = new VMSDiskInfo("", null, null);
    @Before
    @Override
    protected void setUp() {
        TestSuite1.initTest();
    }

    @After
    @Override
    protected void tearDown() {
        assertEquals("", TestSuite1.getStdout());
    }

    /* ---- tests ----- */

    private String invokeFixSourceHostParams(final String names,
                                             final  String ports) {
        final Map<String, String> params = new HashMap<String, String>();
        params.put(DiskData.SOURCE_HOST_NAME, names);
        params.put(DiskData.SOURCE_HOST_PORT, ports);
        Deencapsulation.invoke(VMSDiskInfo.class,
                               "fixSourceHostParams",
                               params);
        return params.get(DiskData.SOURCE_HOST_NAME)
               + ":" + params.get(DiskData.SOURCE_HOST_PORT);
    }

    @Test
    public void testFixSourceHostParams() {
        assertEquals("a, b:1, 2", invokeFixSourceHostParams("a,b", "1,2"));
        assertEquals("a, b:1, 1", invokeFixSourceHostParams("a,b", "1"));
        assertEquals("a, b:1, 2", invokeFixSourceHostParams("a,b", "1,2,3"));
        assertEquals(":", invokeFixSourceHostParams("", "1,2,3"));
        assertEquals("null:null", invokeFixSourceHostParams(null, "1,2,3"));
        assertEquals("a:6789", invokeFixSourceHostParams("a", null));
        assertEquals("a:6789", invokeFixSourceHostParams("a", ""));
        assertEquals("a, b, c:6789, 6789, 6789",
                     invokeFixSourceHostParams("a,b,c", ""));
        assertEquals("a:1", invokeFixSourceHostParams("a,", "1,2"));
        assertEquals(", a:1, 2", invokeFixSourceHostParams(",a", "1,2"));
        assertEquals("a, b, c:1, 2, 2",
                     invokeFixSourceHostParams(" a , b , c", " 1 , 2 "));
        assertEquals("a, b:1, 2",
                     invokeFixSourceHostParams(" a , b ", " 1 , 2 , 3 "));
    }

    @Test
    public void testNotEqualNamesAndDomains() {

        final ClusterBrowser b =
                TestSuite1.getHosts().get(0).getBrowser().getClusterBrowser();
        final VMSVirtualDomainInfo domain1 =
                                      new VMSVirtualDomainInfo("dname1", b);
        final VMSVirtualDomainInfo domain2 =
                                      new VMSVirtualDomainInfo("dname2", b);
        final VMSDiskInfo disk1 = new VMSDiskInfo("name1", b, domain1);
        final VMSDiskInfo disk2 = new VMSDiskInfo("name2", b, domain2);
        assertFalse("not equal names and domains", disk1.equals(disk2));
    }

    @Test
    public void testEqualNamesAndDomains() {

        final ClusterBrowser b =
                TestSuite1.getHosts().get(0).getBrowser().getClusterBrowser();
        final VMSVirtualDomainInfo domain1 =
                                      new VMSVirtualDomainInfo("dname", b);
        final VMSVirtualDomainInfo domain2 =
                                      new VMSVirtualDomainInfo("dname", b);
        final VMSDiskInfo disk1 = new VMSDiskInfo("name", b, domain1);
        final VMSDiskInfo disk2 = new VMSDiskInfo("name", b, domain2);
        assertTrue("equal names and domains", disk1.equals(disk2));
    }

    @Test
    public void testNotEqualNamesEqualDomains() {

        final ClusterBrowser b =
                TestSuite1.getHosts().get(0).getBrowser().getClusterBrowser();
        final VMSVirtualDomainInfo domain1 =
                                      new VMSVirtualDomainInfo("dname", b);
        final VMSVirtualDomainInfo domain2 =
                                      new VMSVirtualDomainInfo("dname", b);
        final VMSDiskInfo disk1 = new VMSDiskInfo("name1", b, domain1);
        final VMSDiskInfo disk2 = new VMSDiskInfo("name2", b, domain2);
        assertFalse("not equal names, equal domains", disk1.equals(disk2));
    }

    @Test
    public void testEqualNamesNotEqualDomains() {

        final ClusterBrowser b =
                TestSuite1.getHosts().get(0).getBrowser().getClusterBrowser();
        final VMSVirtualDomainInfo domain1 =
                                      new VMSVirtualDomainInfo("dname1", b);
        final VMSVirtualDomainInfo domain2 =
                                      new VMSVirtualDomainInfo("dname2", b);
        final VMSDiskInfo disk1 = new VMSDiskInfo("name", b, domain1);
        final VMSDiskInfo disk2 = new VMSDiskInfo("name", b, domain2);
        assertFalse("equal names, not equal domains", disk1.equals(disk2));
    }

    @Test
    public void testEqualNull() {

        final ClusterBrowser b =
                TestSuite1.getHosts().get(0).getBrowser().getClusterBrowser();
        final VMSVirtualDomainInfo domain1 =
                                      new VMSVirtualDomainInfo("dname1", b);
        final VMSDiskInfo disk1 = new VMSDiskInfo("name", b, domain1);
        assertFalse("equal null", disk1.equals(null));
    }
}
