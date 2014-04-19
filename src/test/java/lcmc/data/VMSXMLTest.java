/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2014, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.data;

import static org.junit.Assert.assertEquals;
import org.junit.Test;


public final class VMSXMLTest {
    @Test
    public void testConvertKilobytes() {
        assertEquals("wrong",
                     new StringValue("aa", VMSXML.getUnitKiBytes()),
                     VMSXML.convertKilobytes("aa"));
        assertEquals("negative",
                     new StringValue("-1000", VMSXML.getUnitKiBytes()),
                     VMSXML.convertKilobytes("-1000"));
        assertEquals(new StringValue("2G", VMSXML.getUnitKiBytes()),
                     VMSXML.convertKilobytes("2G"));
        assertEquals(new StringValue("0", VMSXML.getUnitKiBytes()),
                     VMSXML.convertKilobytes("0"));
        assertEquals(new StringValue("1", VMSXML.getUnitKiBytes()),
                     VMSXML.convertKilobytes("1"));

        assertEquals(new StringValue("1023", VMSXML.getUnitKiBytes()),
                     VMSXML.convertKilobytes("1023"));

        assertEquals(new StringValue("1", VMSXML.getUnitMiBytes()),
                     VMSXML.convertKilobytes("1024"));

        assertEquals(new StringValue("1025", VMSXML.getUnitKiBytes()),
                     VMSXML.convertKilobytes("1025"));

        assertEquals(new StringValue("2047", VMSXML.getUnitKiBytes()),
                     VMSXML.convertKilobytes("2047"));
        assertEquals(new StringValue("2", VMSXML.getUnitMiBytes()),
                     VMSXML.convertKilobytes("2048"));
        assertEquals(new StringValue("2049", VMSXML.getUnitKiBytes()),
                     VMSXML.convertKilobytes("2049"));
        assertEquals(new StringValue("1048575", VMSXML.getUnitKiBytes()),
                     VMSXML.convertKilobytes("1048575"));
        assertEquals(new StringValue("1", VMSXML.getUnitGiBytes()),
                     VMSXML.convertKilobytes("1048576"));
        assertEquals(new StringValue("1023", VMSXML.getUnitMiBytes()),
                     VMSXML.convertKilobytes("1047552"));
        assertEquals(new StringValue("1048577", VMSXML.getUnitKiBytes()),
                     VMSXML.convertKilobytes("1048577"));
        assertEquals(new StringValue("1025", VMSXML.getUnitMiBytes()),
                     VMSXML.convertKilobytes("1049600"));

        assertEquals(new StringValue("1073741825", VMSXML.getUnitKiBytes()),
                     VMSXML.convertKilobytes("1073741825"));
        assertEquals(new StringValue("1023", VMSXML.getUnitGiBytes()),
                     VMSXML.convertKilobytes("1072693248"));
        assertEquals(new StringValue("1", VMSXML.getUnitTiBytes()),
                     VMSXML.convertKilobytes("1073741824"));
        assertEquals(new StringValue("1025", VMSXML.getUnitGiBytes()),
                     VMSXML.convertKilobytes("1074790400"));
        assertEquals(new StringValue("1050625", VMSXML.getUnitMiBytes()),
                     VMSXML.convertKilobytes("1075840000"));
        assertEquals(new StringValue("1073741827", VMSXML.getUnitKiBytes()),
                     VMSXML.convertKilobytes("1073741827"));

        assertEquals(new StringValue("1", VMSXML.getUnitPiBytes()),
                     VMSXML.convertKilobytes("1099511627776"));
        assertEquals(new StringValue("1024", VMSXML.getUnitPiBytes()),
                     VMSXML.convertKilobytes("1125899906842624"));
        assertEquals(new StringValue("10000", VMSXML.getUnitPiBytes()),
                     VMSXML.convertKilobytes("10995116277760000"));
    }

    @Test
    public void testConvertToKilobytes() {
        assertEquals(10, VMSXML.convertToKilobytes(
                              new StringValue("10", VMSXML.getUnitKiBytes())));

        assertEquals(6144, VMSXML.convertToKilobytes(
                              new StringValue("6", VMSXML.getUnitMiBytes())));

        assertEquals(8388608, VMSXML.convertToKilobytes(
                              new StringValue("8", VMSXML.getUnitGiBytes())));

        assertEquals(10737418240L, VMSXML.convertToKilobytes(
                              new StringValue("10", VMSXML.getUnitTiBytes())));

        assertEquals(13194139533312L, VMSXML.convertToKilobytes(
                              new StringValue("12", VMSXML.getUnitPiBytes())));
        assertEquals(1099511627776000000L,
                     VMSXML.convertToKilobytes(
                         new StringValue("1000000", VMSXML.getUnitPiBytes())));
        //TODO:
        //assertEquals(10995116277760000000L,
        //             VMSXML.convertToKilobytes("10000000"));

        assertEquals(-1, VMSXML.convertToKilobytes(new StringValue("7")));

        assertEquals(-1, VMSXML.convertToKilobytes(new StringValue()));
        assertEquals(-1, VMSXML.convertToKilobytes(null));
        assertEquals(-1, VMSXML.convertToKilobytes(new StringValue("P")));
        assertEquals(-1, VMSXML.convertToKilobytes(new StringValue("-3")));
    }
}
