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

package lcmc.vm.domain;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import lcmc.common.domain.StringValue;


class VmsXmlTest {
    @Test
    void testConvertKilobytes() {
        assertThat(VmsXml.convertKilobytes("aa")).describedAs("wrong").isEqualTo(new StringValue("aa", VmsXml.getUnitKiBytes()));
        assertThat(VmsXml.convertKilobytes("-1000")).describedAs("negative")
                .isEqualTo(new StringValue("-1000", VmsXml.getUnitKiBytes()));
        assertThat(VmsXml.convertKilobytes("2G")).isEqualTo(new StringValue("2G", VmsXml.getUnitKiBytes()));
        assertThat(VmsXml.convertKilobytes("0")).isEqualTo(new StringValue("0", VmsXml.getUnitKiBytes()));
        assertThat(VmsXml.convertKilobytes("1")).isEqualTo(new StringValue("1", VmsXml.getUnitKiBytes()));

        assertThat(VmsXml.convertKilobytes("1023")).isEqualTo(new StringValue("1023", VmsXml.getUnitKiBytes()));

        assertThat(VmsXml.convertKilobytes("1024")).isEqualTo(new StringValue("1", VmsXml.getUnitMiBytes()));

        assertThat(VmsXml.convertKilobytes("1025")).isEqualTo(new StringValue("1025", VmsXml.getUnitKiBytes()));

        assertThat(VmsXml.convertKilobytes("2047")).isEqualTo(new StringValue("2047", VmsXml.getUnitKiBytes()));
        assertThat(VmsXml.convertKilobytes("2048")).isEqualTo(new StringValue("2", VmsXml.getUnitMiBytes()));
        assertThat(VmsXml.convertKilobytes("2049")).isEqualTo(new StringValue("2049", VmsXml.getUnitKiBytes()));
        assertThat(VmsXml.convertKilobytes("1048575")).isEqualTo(new StringValue("1048575", VmsXml.getUnitKiBytes()));
        assertThat(VmsXml.convertKilobytes("1048576")).isEqualTo(new StringValue("1", VmsXml.getUnitGiBytes()));
        assertThat(VmsXml.convertKilobytes("1047552")).isEqualTo(new StringValue("1023", VmsXml.getUnitMiBytes()));
        assertThat(VmsXml.convertKilobytes("1048577")).isEqualTo(new StringValue("1048577", VmsXml.getUnitKiBytes()));
        assertThat(VmsXml.convertKilobytes("1049600")).isEqualTo(new StringValue("1025", VmsXml.getUnitMiBytes()));

        assertThat(VmsXml.convertKilobytes("1073741825")).isEqualTo(new StringValue("1073741825", VmsXml.getUnitKiBytes()));
        assertThat(VmsXml.convertKilobytes("1072693248")).isEqualTo(new StringValue("1023", VmsXml.getUnitGiBytes()));
        assertThat(VmsXml.convertKilobytes("1073741824")).isEqualTo(new StringValue("1", VmsXml.getUnitTiBytes()));
        assertThat(VmsXml.convertKilobytes("1074790400")).isEqualTo(new StringValue("1025", VmsXml.getUnitGiBytes()));
        assertThat(VmsXml.convertKilobytes("1075840000")).isEqualTo(new StringValue("1050625", VmsXml.getUnitMiBytes()));
        assertThat(VmsXml.convertKilobytes("1073741827")).isEqualTo(new StringValue("1073741827", VmsXml.getUnitKiBytes()));

        assertThat(VmsXml.convertKilobytes("1099511627776")).isEqualTo(new StringValue("1", VmsXml.getUnitPiBytes()));
        assertThat(VmsXml.convertKilobytes("1125899906842624")).isEqualTo(new StringValue("1024", VmsXml.getUnitPiBytes()));
        assertThat(VmsXml.convertKilobytes("10995116277760000")).isEqualTo(new StringValue("10000", VmsXml.getUnitPiBytes()));
    }

    @Test
    public void testConvertToKilobytes() {
        assertThat(VmsXml.convertToKilobytes(new StringValue("10", VmsXml.getUnitKiBytes()))).isEqualTo(10);
        assertThat(VmsXml.convertToKilobytes(new StringValue("6", VmsXml.getUnitMiBytes()))).isEqualTo(6144);
        assertThat(VmsXml.convertToKilobytes(new StringValue("8", VmsXml.getUnitGiBytes()))).isEqualTo(8388608);
        assertThat(VmsXml.convertToKilobytes(new StringValue("10", VmsXml.getUnitTiBytes()))).isEqualTo(10737418240L);
        assertThat(VmsXml.convertToKilobytes(new StringValue("12", VmsXml.getUnitPiBytes()))).isEqualTo(13194139533312L);
        assertThat(VmsXml.convertToKilobytes(new StringValue("1000000", VmsXml.getUnitPiBytes()))).isEqualTo(1099511627776000000L);

        assertThat(VmsXml.convertToKilobytes(new StringValue("7"))).isEqualTo(-1);
        assertThat(VmsXml.convertToKilobytes(new StringValue())).isEqualTo(-1);
        assertThat(VmsXml.convertToKilobytes(null)).isEqualTo(-1);
        assertThat(VmsXml.convertToKilobytes(new StringValue("P"))).isEqualTo(-1);
        assertThat(VmsXml.convertToKilobytes(new StringValue("-3"))).isEqualTo(-1);
    }
}
