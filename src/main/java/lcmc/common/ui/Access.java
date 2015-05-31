/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2015, Rastislav Levrinc.
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

package lcmc.common.ui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lcmc.cluster.domain.Cluster;
import lcmc.cluster.domain.Clusters;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.domain.AccessMode;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.*;
import java.util.List;
import java.util.Map;

@Named
@Singleton
public class Access {
    @Inject
    private Clusters allClusters;

    private boolean advancedMode = false;
    private AccessMode.Type accessType = AccessMode.ADMIN;
    private AccessMode.Type maxAccessType = AccessMode.ADMIN;

    private final Map<JComponent, AccessMode> visibleInAccessType = Maps.newHashMap();
    /** Global elements like menus, that are enabled, disabled according to
     * their access type. */
    private final Map<JComponent, AccessMode> enabledInAccessType = Maps.newHashMap();

    /**
     * Add to the list of components that are visible only in specific access
     * mode.
     */
    void addToVisibleInAccessType(final JComponent c, final AccessMode accessMode) {
        c.setVisible(isAccessible(accessMode));
        visibleInAccessType.put(c, accessMode);
    }

    /**
     * Add to the list of components that are visible only in specific access
     * mode.
     */
    void addToEnabledInAccessType(final JComponent c, final AccessMode accessMode) {
        c.setEnabled(isAccessible(accessMode));
        enabledInAccessType.put(c, accessMode);
    }

    /** Updates access of the item according of their access type. */
    public void updateGlobalItems() {
        for (final Map.Entry<JComponent, AccessMode> accessEntry : visibleInAccessType.entrySet()) {
            accessEntry.getKey().setVisible(isAccessible(accessEntry.getValue()));
        }
        for (final Map.Entry<JComponent, AccessMode> enabledEntry : enabledInAccessType.entrySet()) {
            enabledEntry.getKey().setEnabled(isAccessible(enabledEntry.getValue()));
        }
    }

    public void setAccessible(final JComponent c, final AccessMode.Type required) {
        c.setEnabled(getAccessType().compareTo(required) >= 0);
    }

    public void setAdvancedMode(final boolean advancedMode) {
        this.advancedMode = advancedMode;
    }

    public boolean isAdvancedMode() {
        return advancedMode;
    }

    public void setAccessType(final AccessMode.Type accessType) {
        this.accessType = accessType;
    }

    public AccessMode.Type getAccessType() {
        return accessType;
    }

    AccessMode.Type getMaxAccessType() {
        return maxAccessType;
    }

    /**
     * Returns true if the access type is greater than the one that is
     * required and advanced mode is required and we are not in advanced mode.
     */
    public boolean isAccessible(final AccessMode required) {
        return getAccessType().compareTo(required.getType()) > 0
                || (getAccessType().compareTo(required.getType()) == 0
                && (advancedMode || !required.isAdvancedMode()));
    }

    public String[] getOperatingModes() {
        final List<String> modes = Lists.newArrayList();
        for (final AccessMode.Type at : AccessMode.OP_MODES_MAP.keySet()) {
            modes.add(AccessMode.OP_MODES_MAP.get(at));
            if (at.equals(maxAccessType)) {
                break;
            }
        }
        return modes.toArray(new String[modes.size()]);
    }

    public void setMaxAccessType(final AccessMode.Type maxAccessType) {
        this.maxAccessType = maxAccessType;
        setAccessType(maxAccessType);
        checkAccessOfEverything();
    }

    public void checkAccessOfEverything() {
        for (final Cluster c : allClusters.getClusterSet()) {
            final ClusterBrowser cb = c.getBrowser();
            if (cb != null) {
                cb.checkAccessOfEverything();
            }
        }
    }
}
