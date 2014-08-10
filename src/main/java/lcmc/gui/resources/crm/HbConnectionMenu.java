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

import java.util.ArrayList;
import java.util.List;
import lcmc.model.AccessMode;
import lcmc.model.Application;
import lcmc.model.Host;
import lcmc.gui.ClusterBrowser;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class HbConnectionMenu {
    
    private final HbConnectionInfo hbConnectionInfo;

    public HbConnectionMenu(final HbConnectionInfo hbConnectionInfo) {
        this.hbConnectionInfo = hbConnectionInfo;
    }

    public List<UpdatableItem> getPulldownMenu() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();

        final Application.RunMode runMode = Application.RunMode.LIVE;

        final MyMenuItem removeEdgeItem = new MyMenuItem(
                     Tools.getString("ClusterBrowser.Hb.RemoveEdge"),
                     ClusterBrowser.REMOVE_ICON,
                     Tools.getString("ClusterBrowser.Hb.RemoveEdge.ToolTip"),
                     new AccessMode(Application.AccessType.ADMIN, false),
                     new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (getBrowser().crmStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                return null;
            }

            @Override
            public void action() {
                getBrowser().getCrmGraph().removeConnection(
                                                      hbConnectionInfo,
                                                      getBrowser().getDCHost(),
                                                      runMode);
            }
        };
        final ButtonCallback removeEdgeCallback =
                  getBrowser().new ClMenuItemCallback(null) {
            @Override
            public boolean isEnabled() {
                return super.isEnabled() && !hbConnectionInfo.isNew();
            }
            @Override
            public void action(final Host dcHost) {
                if (!hbConnectionInfo.isNew()) {
                    getBrowser().getCrmGraph().removeConnection(hbConnectionInfo,
                                                                dcHost,
                                                                Application.RunMode.TEST);
                }
            }
        };
        hbConnectionInfo.addMouseOverListener(removeEdgeItem, removeEdgeCallback);
        items.add(removeEdgeItem);

        /* remove/add order */
        final MyMenuItem removeOrderItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.RemoveOrder"),
                ClusterBrowser.REMOVE_ICON,
                Tools.getString("ClusterBrowser.Hb.RemoveOrder.ToolTip"),

                Tools.getString("ClusterBrowser.Hb.AddOrder"),
                null,
                Tools.getString("ClusterBrowser.Hb.AddOrder.ToolTip"),
                new AccessMode(Application.AccessType.ADMIN, false),
                new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean predicate() {
                return getBrowser().getCrmGraph().isOrder(hbConnectionInfo);
            }

            @Override
            public String enablePredicate() {
                if (getBrowser().crmStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                return null;
            }

            @Override
            public void action() {
                if (this.getText().equals(Tools.getString(
                                       "ClusterBrowser.Hb.RemoveOrder"))) {
                    getBrowser().getCrmGraph().removeOrder(
                                                     hbConnectionInfo,
                                                     getBrowser().getDCHost(),
                                                     runMode);
                } else {
                    /* there is colocation constraint so let's get the
                     * endpoints from it. */
                    hbConnectionInfo.addOrder(null,
                                              hbConnectionInfo.getLastServiceInfoRsc(),
                                              hbConnectionInfo.getLastServiceInfoWithRsc());
                    getBrowser().getCrmGraph().addOrder(
                                                      hbConnectionInfo,
                                                      getBrowser().getDCHost(),
                                                      runMode);
                }
            }
        };

        final ButtonCallback removeOrderCallback =
                 getBrowser().new ClMenuItemCallback(null) {
            @Override
            public boolean isEnabled() {
                return super.isEnabled() && !hbConnectionInfo.isNew();
            }
            @Override
            public void action(final Host dcHost) {
                if (!hbConnectionInfo.isNew()) {
                    if (getBrowser().getCrmGraph().isOrder(hbConnectionInfo)) {
                        getBrowser().getCrmGraph().removeOrder(hbConnectionInfo,
                                                               dcHost,
                                                               Application.RunMode.TEST);
                    } else {
                        /* there is colocation constraint so let's get the
                         * endpoints from it. */
                        hbConnectionInfo.addOrder(null,
                                                  hbConnectionInfo.getLastServiceInfoRsc(),
                                                  hbConnectionInfo.getLastServiceInfoWithRsc());
                        getBrowser().getCrmGraph().addOrder(hbConnectionInfo,
                                                            dcHost,
                                                            Application.RunMode.TEST);
                    }
                }
            }
        };
        hbConnectionInfo.addMouseOverListener(removeOrderItem, removeOrderCallback);
        items.add(removeOrderItem);

        /* remove/add colocation */
        final MyMenuItem removeColocationItem =
                new MyMenuItem(
                    Tools.getString("ClusterBrowser.Hb.RemoveColocation"),
                    ClusterBrowser.REMOVE_ICON,
                    Tools.getString(
                            "ClusterBrowser.Hb.RemoveColocation.ToolTip"),

                    Tools.getString("ClusterBrowser.Hb.AddColocation"),
                    null,
                    Tools.getString(
                            "ClusterBrowser.Hb.AddColocation.ToolTip"),
                    new AccessMode(Application.AccessType.ADMIN, false),
                    new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean predicate() {
                return getBrowser().getCrmGraph().isColocation(hbConnectionInfo);
            }

            @Override
            public String enablePredicate() {
                if (getBrowser().crmStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                return null;
            }

            @Override
            public void action() {
                if (this.getText().equals(Tools.getString(
                                  "ClusterBrowser.Hb.RemoveColocation"))) {
                    getBrowser().getCrmGraph().removeColocation(
                                                   hbConnectionInfo,
                                                   getBrowser().getDCHost(),
                                                   runMode);
                } else {
                    /* add colocation */
                    /* there is order constraint so let's get the endpoints
                     * from it. */
                    hbConnectionInfo.addColocation(null,
                                                   hbConnectionInfo.getLastServiceInfoParent(),
                                                   hbConnectionInfo.getLastServiceInfoChild());
                    getBrowser().getCrmGraph().addColocation(
                                                      hbConnectionInfo,
                                                      getBrowser().getDCHost(),
                                                      runMode);
                }
            }
        };

        final ButtonCallback removeColocationCallback =
                                   getBrowser().new ClMenuItemCallback(null) {

            @Override
            public boolean isEnabled() {
                return super.isEnabled() && !hbConnectionInfo.isNew();
            }
            @Override
            public void action(final Host dcHost) {
                if (!hbConnectionInfo.isNew()) {
                    if (getBrowser().getCrmGraph().isColocation(hbConnectionInfo)) {
                        getBrowser().getCrmGraph().removeColocation(hbConnectionInfo,
                                                                    dcHost,
                                                                    Application.RunMode.TEST);
                    } else {
                        /* add colocation */
                        /* there is order constraint so let's get the endpoints
                         * from it. */
                        hbConnectionInfo.addColocation(null,
                                                       hbConnectionInfo.getLastServiceInfoParent(),
                                                       hbConnectionInfo.getLastServiceInfoChild());
                        getBrowser().getCrmGraph().addColocation(hbConnectionInfo,
                                                                 dcHost,
                                                                 Application.RunMode.TEST);
                    }
                }
            }
        };
        hbConnectionInfo.addMouseOverListener(removeColocationItem, removeColocationCallback);
        items.add(removeColocationItem);
        return items;
    }

    private ClusterBrowser getBrowser() {
        return hbConnectionInfo.getBrowser();
    }
}
