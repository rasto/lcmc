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

package lcmc.vm.domain.data;

import lcmc.common.domain.StringValue;

/** Class that holds data about virtual filesystems. */
public final class FilesystemData extends HardwareData {
    /** Type: mount. */
    private final String type;
    private final String sourceDir;
    private final String sourceName;
    private final String targetDir;
    public static final String TYPE = "type";
    public static final String SOURCE_DIR = "source_dir";
    public static final String SOURCE_NAME = "source_name";
    public static final String TARGET_DIR = "target_dir";
    public static final String SAVED_TARGET_DIR = "saved_target_dir";

    public FilesystemData(final String type,
                          final String sourceDir,
                          final String sourceName,
                          final String targetDir) {
        super();
        this.type = type;
        setValue(TYPE, new StringValue(type));
        this.sourceDir = sourceDir;
        setValue(SOURCE_DIR, new StringValue(sourceDir));
        this.sourceName = sourceName;
        setValue(SOURCE_NAME, new StringValue(sourceName));
        this.targetDir = targetDir;
        setValue(TARGET_DIR, new StringValue(targetDir));
    }

    public String getType() {
        return type;
    }

    public String getSourceDir() {
        return sourceDir;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getTargetDir() {
        return targetDir;
    }
}
