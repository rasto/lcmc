/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 *
 * DRBD Management Console is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * DRBD Management Console is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package drbd.data;

import java.io.File;
import drbd.utilities.Tools;

/**
 * This class holds info about file in a linux file system. It should overwrite
 * everything that browse file system may ask about the file.
 */
public class LinuxFile extends File {
    /** Whether it is directory. */
    private boolean directory = false;
    private long lastModified;
    private long fileLength;
    public LinuxFile(final String name,
                     final String type,
                     final long lastModified,
                     final long fileLength) {
        super(name);
        if ("d".equals(type)) {
            directory = true;
        }
        this.lastModified = lastModified;
        this.fileLength = fileLength;
    }
    public final boolean isFile() {
        return true;
    }
    public final boolean exists() {
        return true;
    }
    public final boolean canRead() {
        return true;
    }
    public final boolean isDirectory() {
        return directory;
    }


    public final long lastModified() {
        System.out.println(toString() + " last modified1: " + lastModified);
        System.out.println(toString() + " last modified2: " + super.lastModified());
        return lastModified;
    }

    public final long length() {
        return fileLength;
    }
}
