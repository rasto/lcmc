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
import java.io.IOException;

import drbd.utilities.Tools;
import drbd.utilities.SSH;
import drbd.gui.resources.VMSDiskInfo;

/**
 * This class holds info about file in a linux file system. It should overwrite
 * everything that browse file system may ask about the file.
 */
public class LinuxFile extends File {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    public static final char separatorChar = '/';
    public static final String separator = "" + separatorChar;
    public static final char pathSeparatorChar = '/';
    public static final String pathSeparator = "" + pathSeparatorChar;
    /** Host on which is this file. */
    private final Host host;
    /** Whether it is directory. */
    private boolean directory = false;
    /** Last modified. */
    private long lastModified;
    /** File length. */
    private long fileLength;
    /** Is true if this file exists, false it doesn't, null - it is not known.*/
    private Boolean existCache = null;
    /** VMs disk info object. */
    private final VMSDiskInfo vmsDiskInfo;

    /** Creates new LinuxFile object. */
    public LinuxFile(final VMSDiskInfo vmsDiskInfo,
                     final Host host,
                     final String name,
                     final String type,
                     final long lastModified,
                     final long fileLength) {
        super(Tools.getUnixPath(name));
        this.vmsDiskInfo = vmsDiskInfo;
        this.host = host;
        if ("d".equals(type)) {
            directory = true;
        }
        this.lastModified = lastModified;
        this.fileLength = fileLength;
    }

    /** Updates this file with possible new info. */
    public final void update(final String type,
                             final long lastModified,
                             final long fileLength) {
        if ("d".equals(type)) {
            directory = true;
        }
        this.lastModified = lastModified;
        this.fileLength = fileLength;
    }

    /** Returns whether it is a file. */
    public final boolean isFile() {
        return true;
    }

    /** Returns whether it exists. */
    public final boolean exists() {
        if (existCache != null) {
            return existCache;
        }
        final SSH.SSHOutput out =
                Tools.execCommandProgressIndicator(
                              host,
                              "stat "
                              + Tools.getUnixPath(toString())
                              + " 2>/dev/null",
                              null,
                              false,
                              "executing...",
                              SSH.DEFAULT_COMMAND_TIMEOUT);
        existCache = out.getExitCode() == 0;
        return existCache;
    }

    /** Returns whether it readable. */
    public final boolean canRead() {
        return true;
    }

    /** Returns whether it executable. */
    public final boolean canExecute() {
        return true;
    }

    /** Returns whether it is directory. */
    public final boolean isDirectory() {
        return directory;
    }

    /** Returns last modified time. */
    public final long lastModified() {
        return lastModified;
    }

    /** Returns length of the file. */
    public final long length() {
        return fileLength;
    }

    /** Returns file with absolute path. */
    public final File getAbsoluteFile() {
        final String absPath = getAbsolutePath();
        return vmsDiskInfo.getLinuxDir(absPath, host);
    }

    /** Returns cannonical file name. */
    public final File getCanonicalFile() throws IOException {
        final String canonPath = getCanonicalPath();
        return vmsDiskInfo.getLinuxDir(canonPath, host);
    }

    /** Returns parent dir. */
    public final File getParentFile() {
        final String p = getParent();
        if (p == null) {
            return null;
        }
        return vmsDiskInfo.getLinuxDir(p, host);
    }
}
