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

package lcmc.cluster.service;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import static junitparams.JUnitParamsRunner.$;
import static org.junit.Assert.*;

@RunWith(JUnitParamsRunner.class)
public class BlockDeviceServiceTest {
    final BlockDeviceService blockDeviceService = new BlockDeviceService();

    private Object[] equalCollections() {
        return $(
                $(new ArrayList<String>(), new ArrayList<String>()),
                $(new ArrayList<String>(Arrays.asList("a", "b")), new ArrayList<String>(Arrays.asList("a", "b"))),
                $(new TreeSet<String>(), new TreeSet<String>()),
                $(new TreeSet<String>(Arrays.asList("a", "b")), new TreeSet<String>(Arrays.asList("a", "b"))),
                $(new TreeSet<String>(Arrays.asList("b", "a")), new TreeSet<String>(Arrays.asList("a", "b"))));
    }

    @Test
    @Parameters(method="equalCollections")
    public void collectionsShouldBeEqual(final Collection<String> collection1, Collection<String> collection2) {
        assertTrue(
                "" + collection1 + " != " + collection2,
                blockDeviceService.equalCollections(collection1, collection2));
    }

    private Object[] unequalCollections() {
        return $(
                $(new ArrayList<String>(), new ArrayList<String>(Arrays.asList("a"))),
                $(new ArrayList<String>(Arrays.asList("a")), new ArrayList<String>()),
                $(new ArrayList<String>(Arrays.asList("a")), new ArrayList<String>(Arrays.asList("a", "b"))),
                $(new ArrayList<String>(Arrays.asList("a", "b")), new ArrayList<String>(Arrays.asList("b"))),
                $(new ArrayList<String>(Arrays.asList("a", "a")), new ArrayList<String>(Arrays.asList("a", "b"))),
                $(new ArrayList<String>(Arrays.asList("b", "b")), new ArrayList<String>(Arrays.asList("a", "b"))),
                $(new TreeSet<String>(), new TreeSet<String>(Arrays.asList("a"))),
                $(new TreeSet<String>(Arrays.asList("a")), new TreeSet<String>()),
                $(new TreeSet<String>(Arrays.asList("a")), new TreeSet<String>(Arrays.asList("a", "b"))),
                $(new TreeSet<String>(Arrays.asList("a", "b")), new TreeSet<String>(Arrays.asList("b"))),
                $(new TreeSet<String>(Arrays.asList("a", "a")), new TreeSet<String>(Arrays.asList("a", "b"))),
                $(new TreeSet<String>(Arrays.asList("b", "b")), new TreeSet<String>(Arrays.asList("a", "b"))));
    }

    @Test
    @Parameters(method="unequalCollections")
    public void collectionsShouldNotBeEqual(final Collection<String> collection1, Collection<String> collection2) {
        assertTrue(
                "" + collection1 + " == " + collection2,
                !blockDeviceService.equalCollections(collection1, collection2));
    }
}