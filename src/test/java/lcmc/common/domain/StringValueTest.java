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

package lcmc.common.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class StringValueTest {
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
