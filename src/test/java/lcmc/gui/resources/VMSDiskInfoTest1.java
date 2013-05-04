package lcmc.gui.resources;

import junit.framework.TestCase;
import lcmc.utilities.TestSuite1;
import lcmc.data.VMSXML.DiskData;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import java.util.Map;
import java.util.HashMap;
import mockit.Deencapsulation;

public final class VMSDiskInfoTest1 extends TestCase {
    //final VMSDiskInfo vmsdi = new VMSDiskInfo("", null, null);
    @Before
    protected void setUp() {
        TestSuite1.initTest();
    }

    @After
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
}
