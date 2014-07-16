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

package lcmc.gui.resources.drbd;

import java.util.List;
import lcmc.testutils.annotation.type.GuiTest;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@Category(GuiTest.class)
public class GlobalMenuITest {

    @Mock
    private GlobalInfo globalInfo;

    private GlobalMenu sut;

    @Before
    public void setUp() {
        Tools.init();

        sut = new GlobalMenu(globalInfo);
    }

    @Test
    public void menuShouldHaveItems() {
        final List<UpdatableItem> items = sut.getPulldownMenu();

        assertEquals(4, items.size());
    }
}
