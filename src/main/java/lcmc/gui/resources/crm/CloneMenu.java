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

package lcmc.gui.resources.crm;

import java.util.List;
import java.util.Locale;
import javax.swing.JMenuItem;
import lcmc.model.AccessMode;
import lcmc.model.Application;
import lcmc.model.Host;
import lcmc.gui.ClusterBrowser;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.MyMenu;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

public class CloneMenu extends ServiceMenu {

    private final CloneInfo cloneInfo;
    
    public CloneMenu(final CloneInfo cloneInfo) {
        super(cloneInfo);
        this.cloneInfo = cloneInfo;
    }

    @Override
    public List<UpdatableItem> getPulldownMenu() {
        final List<UpdatableItem> items = super.getPulldownMenu();
        final ServiceInfo cs = cloneInfo.getContainedService();
        if (cs == null) {
            return items;
        }
        final UpdatableItem csMenu = new MyMenu(
                                     cs.toString(),
                                     new AccessMode(Application.AccessType.RO,
                                                    false),
                                     new AccessMode(Application.AccessType.RO,
                                                    false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void updateAndWait() {
                Tools.isSwingThread();
                removeAll();
                final ServiceInfo cs0 = cloneInfo.getContainedService();
                if (cs0 != null) {
                    for (final UpdatableItem u : cs0.createPopup()) {
                        add((JMenuItem) u);
                        u.updateAndWait();
                    }
                }
                super.updateAndWait();
            }
        };
        items.add(csMenu);
        return items;
    }

    /** Adds migrate and unmigrate menu items. */
    @Override
    protected void addMigrateMenuItems(final List<UpdatableItem> items) {
        super.addMigrateMenuItems(items);
        if (!cloneInfo.getService().isMaster()) {
            return;
        }
        final Application.RunMode runMode = Application.RunMode.LIVE;
        for (final Host host : getBrowser().getClusterHosts()) {
            final String hostName = host.getName();
            final MyMenuItem migrateFromMenuItem =
               new MyMenuItem(Tools.getString(
                                   "ClusterBrowser.Hb.MigrateFromResource")
                                   + ' ' + hostName + " (stop)",
                              ServiceInfo.MIGRATE_ICON,
                              ClusterBrowser.STARTING_PTEST_TOOLTIP,

                              Tools.getString(
                                   "ClusterBrowser.Hb.MigrateFromResource")
                                   + ' ' + hostName + " (stop) (offline)",
                              ServiceInfo.MIGRATE_ICON,
                              ClusterBrowser.STARTING_PTEST_TOOLTIP,
                              new AccessMode(Application.AccessType.OP, false),
                              new AccessMode(Application.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean predicate() {
                        return host.isCrmStatusOk();
                    }

                    @Override
                    public boolean visiblePredicate() {
                        return !host.isCrmStatusOk()
                               || enablePredicate() == null;
                    }

                    @Override
                    public String enablePredicate() {
                        final List<String> runningOnNodes =
                                            cloneInfo.getRunningOnNodes(runMode);
                        if (runningOnNodes == null
                            || runningOnNodes.size() < 1) {
                            return "must run";
                        }
                        boolean runningOnNode = false;
                        for (final String ron : runningOnNodes) {
                            if (hostName.toLowerCase(Locale.US).equals(
                                               ron.toLowerCase(Locale.US))) {
                                runningOnNode = true;
                                break;
                            }
                        }
                        if (!getBrowser().crmStatusFailed()
                               && cloneInfo.getService().isAvailable()
                               && runningOnNode
                               && host.isCrmStatusOk()) {
                            return null;
                        } else {
                            return ""; /* is not visible anyway */
                        }
                    }

                    @Override
                    public void action() {
                        cloneInfo.hidePopup();
                        if (cloneInfo.getService().isMaster()) {
                            /* without role=master */
                            cloneInfo.superMigrateFromResource(getBrowser().getDCHost(),
                                                      hostName,
                                                      runMode);
                        } else {
                            cloneInfo.migrateFromResource(getBrowser().getDCHost(),
                                                hostName,
                                                runMode);
                        }
                    }
                };
            final ButtonCallback migrateItemCallback =
               getBrowser().new ClMenuItemCallback(null) {
                @Override
                public void action(final Host dcHost) {
                    if (cloneInfo.getService().isMaster()) {
                        /* without role=master */
                        cloneInfo.superMigrateFromResource(dcHost,
                                                 hostName,
                                                 Application.RunMode.TEST);
                    } else {
                        cloneInfo.migrateFromResource(dcHost,
                                            hostName,
                                            Application.RunMode.TEST);
                    }
                }
            };
            cloneInfo.addMouseOverListener(migrateFromMenuItem, migrateItemCallback);
            items.add(migrateFromMenuItem);
        }
    }

    /** Adds "migrate from" and "force migrate" menuitems to the submenu. */
    @Override
    protected void addMoreMigrateMenuItems(final MyMenu submenu) {
        /* no migrate / unmigrate menu advanced items for clones. */
    }
}
