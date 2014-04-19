package lcmc.gui.resources.vms;

import java.util.HashMap;
import java.util.Map;
import lcmc.data.VMSXML.DiskData;
import lcmc.utilities.Tools;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public final class DiskInfoTest {
    private DiskInfo vmsdi;
    @Before
    public void setUp() {
        Tools.init();
        vmsdi = new DiskInfo("", null, null);
    }

    private String invokeFixSourceHostParams(final String names,
                                             final  String ports) {
        final Map<String, String> params = new HashMap<String, String>();
        params.put(DiskData.SOURCE_HOST_NAME, names);
        params.put(DiskData.SOURCE_HOST_PORT, ports);
        vmsdi.fixSourceHostParams(params);
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
