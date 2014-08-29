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

package lcmc.gui.dialog.drbd;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lcmc.model.Host;
import lcmc.gui.dialog.HostLogs;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * An implementation of an dialog with log files.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public final class DrbdLog extends HostLogs {
    private String drbdDeviceName;

    public void init(final Host host, final String device) {
        super.init(host);
        drbdDeviceName = device.substring(device.lastIndexOf('/') + 1);
    }

    /**
     * Returns a command name from the DistResource that gets the drbd log file.
     * "DrbdLog.log"
     */
    @Override
    protected String logFileCommand() {
        return "DrbdLog.log";
    }

    /** Returns which pattern names are selected by default. */
    @Override
    protected Set<String> getSelectedSet() {
        final Set<String> selected = new HashSet<String>();
        selected.add(drbdDeviceName);
        return selected;
    }

    @Override
    protected Map<String, String> getPatternMap() {
        final Map<String, String> patternMap = new LinkedHashMap<String, String>();
        patternMap.put(drbdDeviceName, wordBoundary(drbdDeviceName));
        return patternMap;
    }
}
