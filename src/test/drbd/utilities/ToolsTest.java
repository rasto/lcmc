package drbd.utilities;

import junit.framework.TestCase;

public class ToolsTest extends TestCase {
    public void testConvertKilobytes() {
        assertEquals("aa", Tools.convertKilobytes("aa"));
        assertEquals("-1000K", Tools.convertKilobytes("-1000"));
        assertEquals("2G", Tools.convertKilobytes("2G"));
        assertEquals("0K", Tools.convertKilobytes("0"));
        assertEquals("1K", Tools.convertKilobytes("1"));

        assertEquals("1023K", Tools.convertKilobytes("1023"));
        assertEquals("1M", Tools.convertKilobytes("1024"));
        assertEquals("1025K", Tools.convertKilobytes("1025"));

        assertEquals("2047K", Tools.convertKilobytes("2047"));
        assertEquals("2M", Tools.convertKilobytes("2048"));
        assertEquals("2049K", Tools.convertKilobytes("2049"));

        assertEquals("1048575K", Tools.convertKilobytes("1048575"));
        assertEquals("1G", Tools.convertKilobytes("1048576"));
        assertEquals("1023M", Tools.convertKilobytes("1047552"));
        assertEquals("1048577K", Tools.convertKilobytes("1048577"));
        assertEquals("1025M", Tools.convertKilobytes("1049600"));

        assertEquals("1073741825K", Tools.convertKilobytes("1073741825"));
        assertEquals("1023G", Tools.convertKilobytes("1072693248"));
        assertEquals("1T", Tools.convertKilobytes("1073741824"));
        assertEquals("1025G", Tools.convertKilobytes("1074790400"));
        assertEquals("1050625M", Tools.convertKilobytes("1075840000"));
        assertEquals("1073741827K", Tools.convertKilobytes("1073741827"));



        assertEquals("1P", Tools.convertKilobytes("1099511627776"));
        assertEquals("1024P", Tools.convertKilobytes("1125899906842624"));
        assertEquals("10000P", Tools.convertKilobytes("10995116277760000"));
    }
}
