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

import lcmc.data.vm.VmsXml;
import org.junit.Test;


public final class VmsXmlTest {
    @Test
    public void testConvertKilobytes() {
        assertEquals("wrong",
                     new StringValue("aa", VmsXml.getUnitKiBytes()),
                     VmsXml.convertKilobytes("aa"));
        assertEquals("negative",
                     new StringValue("-1000", VmsXml.getUnitKiBytes()),
                     VmsXml.convertKilobytes("-1000"));
        assertEquals(new StringValue("2G", VmsXml.getUnitKiBytes()),
                     VmsXml.convertKilobytes("2G"));
        assertEquals(new StringValue("0", VmsXml.getUnitKiBytes()),
                     VmsXml.convertKilobytes("0"));
        assertEquals(new StringValue("1", VmsXml.getUnitKiBytes()),
                     VmsXml.convertKilobytes("1"));

        assertEquals(new StringValue("1023", VmsXml.getUnitKiBytes()),
                     VmsXml.convertKilobytes("1023"));

        assertEquals(new StringValue("1", VmsXml.getUnitMiBytes()),
                     VmsXml.convertKilobytes("1024"));

        assertEquals(new StringValue("1025", VmsXml.getUnitKiBytes()),
                     VmsXml.convertKilobytes("1025"));

        assertEquals(new StringValue("2047", VmsXml.getUnitKiBytes()),
                     VmsXml.convertKilobytes("2047"));
        assertEquals(new StringValue("2", VmsXml.getUnitMiBytes()),
                     VmsXml.convertKilobytes("2048"));
        assertEquals(new StringValue("2049", VmsXml.getUnitKiBytes()),
                     VmsXml.convertKilobytes("2049"));
        assertEquals(new StringValue("1048575", VmsXml.getUnitKiBytes()),
                     VmsXml.convertKilobytes("1048575"));
        assertEquals(new StringValue("1", VmsXml.getUnitGiBytes()),
                     VmsXml.convertKilobytes("1048576"));
        assertEquals(new StringValue("1023", VmsXml.getUnitMiBytes()),
                     VmsXml.convertKilobytes("1047552"));
        assertEquals(new StringValue("1048577", VmsXml.getUnitKiBytes()),
                     VmsXml.convertKilobytes("1048577"));
        assertEquals(new StringValue("1025", VmsXml.getUnitMiBytes()),
                     VmsXml.convertKilobytes("1049600"));

        assertEquals(new StringValue("1073741825", VmsXml.getUnitKiBytes()),
                     VmsXml.convertKilobytes("1073741825"));
        assertEquals(new StringValue("1023", VmsXml.getUnitGiBytes()),
                     VmsXml.convertKilobytes("1072693248"));
        assertEquals(new StringValue("1", VmsXml.getUnitTiBytes()),
                     VmsXml.convertKilobytes("1073741824"));
        assertEquals(new StringValue("1025", VmsXml.getUnitGiBytes()),
                     VmsXml.convertKilobytes("1074790400"));
        assertEquals(new StringValue("1050625", VmsXml.getUnitMiBytes()),
                     VmsXml.convertKilobytes("1075840000"));
        assertEquals(new StringValue("1073741827", VmsXml.getUnitKiBytes()),
                     VmsXml.convertKilobytes("1073741827"));

        assertEquals(new StringValue("1", VmsXml.getUnitPiBytes()),
                     VmsXml.convertKilobytes("1099511627776"));
        assertEquals(new StringValue("1024", VmsXml.getUnitPiBytes()),
                     VmsXml.convertKilobytes("1125899906842624"));
        assertEquals(new StringValue("10000", VmsXml.getUnitPiBytes()),
                     VmsXml.convertKilobytes("10995116277760000"));
    }

    @Test
    public void testConvertToKilobytes() {
        assertEquals(10, VmsXml.convertToKilobytes(
                new StringValue("10", VmsXml.getUnitKiBytes())));

        assertEquals(6144, VmsXml.convertToKilobytes(
                new StringValue("6", VmsXml.getUnitMiBytes())));

        assertEquals(8388608, VmsXml.convertToKilobytes(
                new StringValue("8", VmsXml.getUnitGiBytes())));

        assertEquals(10737418240L, VmsXml.convertToKilobytes(
                new StringValue("10", VmsXml.getUnitTiBytes())));

        assertEquals(13194139533312L, VmsXml.convertToKilobytes(
                new StringValue("12", VmsXml.getUnitPiBytes())));
        assertEquals(1099511627776000000L,
                     VmsXml.convertToKilobytes(
                             new StringValue("1000000", VmsXml.getUnitPiBytes())));
        //TODO:
        //assertEquals(10995116277760000000L,
        //             VMSXML.convertToKilobytes("10000000"));

        assertEquals(-1, VmsXml.convertToKilobytes(new StringValue("7")));

        assertEquals(-1, VmsXml.convertToKilobytes(new StringValue()));
        assertEquals(-1, VmsXml.convertToKilobytes(null));
        assertEquals(-1, VmsXml.convertToKilobytes(new StringValue("P")));
        assertEquals(-1, VmsXml.convertToKilobytes(new StringValue("-3")));
    }
}
