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

package lcmc.model;

import java.io.File;
import java.io.IOException;

import lcmc.configs.DistResource;
import lcmc.gui.resources.vms.HardwareInfo;
import lcmc.utilities.Tools;
import lcmc.utilities.ssh.ExecCommandConfig;
import lcmc.utilities.ssh.SshOutput;

/**
 * This class holds info about file in a linux file system. It should overwrite
 * everything that browse file system may ask about the file.
 */
public final class LinuxFile extends File {
    static final char separatorChar = '/';
    static final String separator = "" + separatorChar;
    static final char pathSeparatorChar = '/';
    static final String pathSeparator = "" + pathSeparatorChar;
    private final Host host;
    private boolean directory = false;
    private long lastModified;
    private long fileLength;
    /** Is true if this file exists, false it doesn't, null - it is not known.*/
    private Boolean existCache = null;
    private final HardwareInfo vmsHardwareInfo;

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

    public void update(final String type, final long lastModified, final long fileLength) {
        if ("d".equals(type)) {
            directory = true;
        }
        this.lastModified = lastModified;
        this.fileLength = fileLength;
    }

    @Override
    public boolean isFile() {
        return true;
    }

    @Override
    public boolean exists() {
        if (existCache != null) {
            return existCache;
        }
        final SshOutput out = host.captureCommandProgressIndicator(
                                            "executing...",
                                            new ExecCommandConfig().command(DistResource.SUDO
                                                                            + "stat "
                                                                            + Tools.getUnixPath(toString())
                                                                            + " 2>/dev/null")
                                                                    .silentOutput());
        return out.getExitCode() == 0;
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    public boolean canExecute() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        return directory;
    }

    @Override
    public long lastModified() {
        return lastModified;
    }

    @Override
    public long length() {
        return fileLength;
    }

    @Override
    public File getAbsoluteFile() {
        final String absPath = getAbsolutePath();
        return vmsHardwareInfo.getLinuxDir(absPath, host);
    }

    @Override
    public File getCanonicalFile() throws IOException {
        final String canonPath = getCanonicalPath();
        return vmsHardwareInfo.getLinuxDir(canonPath, host);
    }

    @Override
    public File getParentFile() {
        final String p = getParent();
        if (p == null) {
            return null;
        }
        return vmsHardwareInfo.getLinuxDir(p, host);
    }
}
