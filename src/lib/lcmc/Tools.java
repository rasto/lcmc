/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2013, Rastislav Levrinc.
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

package lcmc;



import java.awt.Component;
import java.awt.Cursor;
import java.awt.image.MemoryImageSource;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;



/**
 * Tools for outside libraries.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class Tools {
    /** Singleton. */
    private static Tools instance = null;
    /** Private constructor. */
    private Tools() {
        /* no instantiation possible. */
    }

    /** This is to make this class a singleton. */
    public static Tools getInstance() {
        synchronized (Tools.class) {
            if (instance == null) {
                instance = new Tools();
            }
        }
        return instance;
    }

    /** Hides mouse pointer. */
    public static void hideMousePointer(final Component c) {
        final int[] pixels = new int[16 * 16];
        final Image image = Toolkit.getDefaultToolkit().createImage(
                                 new MemoryImageSource(16, 16, pixels, 0, 16));
        final Cursor transparentCursor =
             Toolkit.getDefaultToolkit().createCustomCursor(image,
                                                            new Point(0, 0),
                                                            "invisibleCursor");
        c.setCursor(transparentCursor);
    }
}
