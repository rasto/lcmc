package drbd.utilities;

import junit.framework.TestCase;

public class ToolsTest extends TestCase {
    public void testConvertKilobytes() {
        assertEquals("aa", Tools.convertKilobytes("aa"));
        //assertEquals("0K", Tools.convertKilobytes("0")); //TODO
        assertEquals("1K", Tools.convertKilobytes("1"));
        assertEquals("1023K", Tools.convertKilobytes("1023"));
        assertEquals("1M", Tools.convertKilobytes("1024"));
        assertEquals("1025", Tools.convertKilobytes("1025"));
        assertEquals("2047", Tools.convertKilobytes("2047"));
        assertEquals("2K", Tools.convertKilobytes("2048"));
        assertEquals("2049", Tools.convertKilobytes("2049"));
    }
}
