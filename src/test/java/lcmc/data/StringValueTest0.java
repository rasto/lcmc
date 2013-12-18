package lcmc.data;

import junit.framework.TestCase;
import org.junit.Test;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

public final class StringValueTest0 extends TestCase {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(StringValueTest0.class);


    /* ---- tests ----- */

    @Test
    public void testCompareToFunction() {
        final StringValue sv = new StringValue("bb");
        assertEquals(sv.compareTo(null), 2);
        assertEquals(sv.compareTo(new StringValue(null)), 2);
        assertEquals(sv.compareTo(new StringValue("")), 2);
        assertEquals(sv.compareTo(new StringValue("bb")), 0);
        assertEquals(sv.compareTo(new StringValue("ba")), 1);
        assertEquals(sv.compareTo(new StringValue("bc")), -1);
        assertEquals(sv.compareTo(new StringValue("bd")), -2);
    }

    @Test
    public void testCompareToFunctionWithNull() {
        final StringValue sv = new StringValue(null);
        assertEquals(sv.compareTo(null), 0);
        assertEquals(sv.compareTo(new StringValue(null)), 0);
        assertEquals(sv.compareTo(new StringValue("")), 0);
        assertEquals(sv.compareTo(new StringValue("bb")), -2);
    }
}
