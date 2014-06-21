/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
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

package lcmc.data;

import java.io.File;
import java.io.IOException;

import lcmc.configs.DistResource;
import lcmc.gui.resources.vms.HardwareInfo;
import lcmc.utilities.Tools;
import lcmc.utilities.ssh.ExecCommandConfig;
import lcmc.utilities.ssh.Ssh;
import lcmc.utilities.ssh.SshOutput;

/**
 * This class holds info about file in a linux file system. It should overwrite
 * everything that browse file system may ask about the file.
 */
public final class LinuxFile extends File {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    static final char separatorChar = '/';
    static final String separator = "" + separatorChar;
    static final char pathSeparatorChar = '/';
    static final String pathSeparator = "" + pathSeparatorChar;
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
    private final HardwareInfo vmsHardwareInfo;

    /** Creates new LinuxFile object. */
    public LinuxFile(final HardwareInfo vmsHardwareInfo,
                     final Host host,
                     final String name,
                     final String type,
                     final long lastModified,
                     final long fileLength) {
        super(Tools.getUnixPath(name));
        this.vmsHardwareInfo = vmsHardwareInfo;
        this.host = host;
        if ("d".equals(type)) {
            directory = true;
        }
        this.lastModified = lastModified;
        this.fileLength = fileLength;
    }

    /** Updates this file with possible new info. */
    public void update(final String type,
                       final long lastModified,
                       final long fileLength) {
        if ("d".equals(type)) {
            directory = true;
        }
        this.lastModified = lastModified;
        this.fileLength = fileLength;
    }

    /** Returns whether it is a file. */
    @Override
    public boolean isFile() {
        return true;
    }

    /** Returns whether it exists. */
    @Override
    public boolean exists() {
        if (existCache != null) {
            return existCache;
        }
        final SshOutput out = host.captureCommandProgressIndicator("executing...",
                                                                   new ExecCommandConfig().command(DistResource.SUDO
                                                                                                   + "stat "
                                                                                                   + Tools.getUnixPath(toString())
                                                                                                   + " 2>/dev/null")
                                                                       .silentOutput());
        return out.getExitCode() == 0;
    }

    /** Returns whether it readable. */
    @Override
    public boolean canRead() {
        return true;
    }

    /** Returns whether it executable. */
    @Override
    public boolean canExecute() {
        return true;
    }

    /** Returns whether it is directory. */
    @Override
    public boolean isDirectory() {
        return directory;
    }

    /** Returns last modified time. */
    @Override
    public long lastModified() {
        return lastModified;
    }

    /** Returns length of the file. */
    @Override
    public long length() {
        return fileLength;
    }

    /** Returns file with absolute path. */
    @Override
    public File getAbsoluteFile() {
        final String absPath = getAbsolutePath();
        return vmsHardwareInfo.getLinuxDir(absPath, host);
    }

    /** Returns cannonical file name. */
    @Override
    public File getCanonicalFile() throws IOException {
        final String canonPath = getCanonicalPath();
        return vmsHardwareInfo.getLinuxDir(canonPath, host);
    }

    /** Returns parent dir. */
    @Override
    public File getParentFile() {
        final String p = getParent();
        if (p == null) {
            return null;
        }
        return vmsHardwareInfo.getLinuxDir(p, host);
    }
}
