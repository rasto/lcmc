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

package lcmc.crm.ui.resource;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.utils.ButtonCallback;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyMenuItem;
import lcmc.common.ui.utils.UpdatableItem;

@Named
public class HbConnectionMenu {

    private HbConnectionInfo hbConnectionInfo;
    private final MenuFactory menuFactory;

    public HbConnectionMenu(MenuFactory menuFactory) {
        this.menuFactory = menuFactory;
    }

    public List<UpdatableItem> getPulldownMenu(final HbConnectionInfo hbConnectionInfo) {
        this.hbConnectionInfo = hbConnectionInfo;
        final List<UpdatableItem> items = new ArrayList<>();

        final Application.RunMode runMode = Application.RunMode.LIVE;

        final MyMenuItem removeEdgeItem =
                menuFactory.createMenuItem(Tools.getString("ClusterBrowser.Hb.RemoveEdge"), ClusterBrowser.REMOVE_ICON,
                                   Tools.getString("ClusterBrowser.Hb.RemoveEdge.ToolTip"),
                                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL), new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .enablePredicate(() -> {
                            if (getBrowser().crmStatusFailed()) {
                                return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                            }
                            return null;
                        })
                        .addAction(text -> getBrowser().getCrmGraph()
                                .removeConnection(hbConnectionInfo, getBrowser().getDCHost(), runMode));
        final ButtonCallback removeEdgeCallback = getBrowser().new ClMenuItemCallback(null) {
            @Override
            public boolean isEnabled() {
                return super.isEnabled() && !hbConnectionInfo.isNew();
            }
        }.addAction(dcHost -> {
            if (!hbConnectionInfo.isNew()) {
                getBrowser().getCrmGraph().removeConnection(hbConnectionInfo, dcHost, Application.RunMode.TEST);
            }
        });
        hbConnectionInfo.addMouseOverListener(removeEdgeItem, removeEdgeCallback);
        items.add(removeEdgeItem);

        /* remove/add order */
        final MyMenuItem removeOrderItem =
                menuFactory.createMenuItem(Tools.getString("ClusterBrowser.Hb.RemoveOrder"), ClusterBrowser.REMOVE_ICON,
                                Tools.getString("ClusterBrowser.Hb.RemoveOrder.ToolTip"),

                                Tools.getString("ClusterBrowser.Hb.AddOrder"), null, Tools.getString("ClusterBrowser.Hb.AddOrder.ToolTip"),
                                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL), new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .predicate(() -> getBrowser().getCrmGraph().isOrder(hbConnectionInfo))
                        .enablePredicate(() -> {
                            if (getBrowser().crmStatusFailed()) {
                                return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                            }
                            return null;
                        });
        removeOrderItem.addAction(text -> {
            if (removeOrderItem.getText().equals(Tools.getString("ClusterBrowser.Hb.RemoveOrder"))) {
                getBrowser().getCrmGraph().removeOrder(hbConnectionInfo, getBrowser().getDCHost(), runMode);
            } else {
                /* there is colocation constraint so let's get the
                 * endpoints from it. */
                hbConnectionInfo.addOrder(null, hbConnectionInfo.getLastServiceInfoRsc(),
                        hbConnectionInfo.getLastServiceInfoWithRsc());
                getBrowser().getCrmGraph().addOrder(hbConnectionInfo, getBrowser().getDCHost(), runMode);
            }
        });

        final ButtonCallback removeOrderCallback = getBrowser().new ClMenuItemCallback(null) {
            @Override
            public boolean isEnabled() {
                return super.isEnabled() && !hbConnectionInfo.isNew();
            }
        }.addAction(dcHost -> {
            if (!hbConnectionInfo.isNew()) {
                if (getBrowser().getCrmGraph().isOrder(hbConnectionInfo)) {
                    getBrowser().getCrmGraph().removeOrder(hbConnectionInfo, dcHost, Application.RunMode.TEST);
                } else {
                    /* there is colocation constraint so let's get the
                     * endpoints from it. */
                    hbConnectionInfo.addOrder(null, hbConnectionInfo.getLastServiceInfoRsc(),
                            hbConnectionInfo.getLastServiceInfoWithRsc());
                    getBrowser().getCrmGraph().addOrder(hbConnectionInfo, dcHost, Application.RunMode.TEST);
                }
            }
        });
        hbConnectionInfo.addMouseOverListener(removeOrderItem, removeOrderCallback);
        items.add(removeOrderItem);

        /* remove/add colocation */
        final MyMenuItem removeColocationItem =
                menuFactory.createMenuItem(Tools.getString("ClusterBrowser.Hb.RemoveColocation"), ClusterBrowser.REMOVE_ICON,
                                Tools.getString("ClusterBrowser.Hb.RemoveColocation.ToolTip"),

                                Tools.getString("ClusterBrowser.Hb.AddColocation"), null,
                                Tools.getString("ClusterBrowser.Hb.AddColocation.ToolTip"),
                                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL), new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .predicate(() -> getBrowser().getCrmGraph().isColocation(hbConnectionInfo))
                        .enablePredicate(() -> {
                            if (getBrowser().crmStatusFailed()) {
                                return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                            }
                            return null;
                        });
        removeColocationItem.addAction(text -> {
            if (removeColocationItem.getText().equals(Tools.getString("ClusterBrowser.Hb.RemoveColocation"))) {
                getBrowser().getCrmGraph().removeColocation(hbConnectionInfo, getBrowser().getDCHost(), runMode);
            } else {
                /* add colocation */
                /* there is order constraint so let's get the endpoints
                 * from it. */
                hbConnectionInfo.addColocation(null, hbConnectionInfo.getLastServiceInfoParent(),
                        hbConnectionInfo.getLastServiceInfoChild());
                getBrowser().getCrmGraph().addColocation(hbConnectionInfo, getBrowser().getDCHost(), runMode);
            }
        });
        final ButtonCallback removeColocationCallback = getBrowser().new ClMenuItemCallback(null) {
            @Override
            public boolean isEnabled() {
                return super.isEnabled() && !hbConnectionInfo.isNew();
            }
        }.addAction(dcHost -> {
            if (!hbConnectionInfo.isNew()) {
                if (getBrowser().getCrmGraph().isColocation(hbConnectionInfo)) {
                    getBrowser().getCrmGraph().removeColocation(hbConnectionInfo, dcHost, Application.RunMode.TEST);
                } else {
                    /* add colocation */
                    /* there is order constraint so let's get the endpoints
                     * from it. */
                    hbConnectionInfo.addColocation(null, hbConnectionInfo.getLastServiceInfoParent(),
                            hbConnectionInfo.getLastServiceInfoChild());
                    getBrowser().getCrmGraph().addColocation(hbConnectionInfo, dcHost, Application.RunMode.TEST);
                }
            }
        });
        hbConnectionInfo.addMouseOverListener(removeColocationItem, removeColocationCallback);
        items.add(removeColocationItem);
        return items;
    }

    private ClusterBrowser getBrowser() {
        return hbConnectionInfo.getBrowser();
    }
}
