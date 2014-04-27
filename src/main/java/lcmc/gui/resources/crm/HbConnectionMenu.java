package lcmc.gui.resources.crm;

import java.util.ArrayList;
import java.util.List;
import lcmc.data.AccessMode;
import lcmc.data.Application;
import lcmc.data.Host;
import lcmc.gui.ClusterBrowser;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

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
                if (getBrowser().clStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                return null;
            }

            @Override
            public void action() {
                getBrowser().getCRMGraph().removeConnection(
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
                    getBrowser().getCRMGraph().removeConnection(hbConnectionInfo,
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
                return getBrowser().getCRMGraph().isOrder(hbConnectionInfo);
            }

            @Override
            public String enablePredicate() {
                if (getBrowser().clStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                return null;
            }

            @Override
            public void action() {
                if (this.getText().equals(Tools.getString(
                                       "ClusterBrowser.Hb.RemoveOrder"))) {
                    getBrowser().getCRMGraph().removeOrder(
                                                     hbConnectionInfo,
                                                     getBrowser().getDCHost(),
                                                     runMode);
                } else {
                    /* there is colocation constraint so let's get the
                     * endpoints from it. */
                    hbConnectionInfo.addOrder(null,
                                              hbConnectionInfo.getLastServiceInfoRsc(),
                                              hbConnectionInfo.getLastServiceInfoWithRsc());
                    getBrowser().getCRMGraph().addOrder(
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
                    if (getBrowser().getCRMGraph().isOrder(hbConnectionInfo)) {
                        getBrowser().getCRMGraph().removeOrder(hbConnectionInfo,
                                                               dcHost,
                                                               Application.RunMode.TEST);
                    } else {
                        /* there is colocation constraint so let's get the
                         * endpoints from it. */
                        hbConnectionInfo.addOrder(null,
                                                  hbConnectionInfo.getLastServiceInfoRsc(),
                                                  hbConnectionInfo.getLastServiceInfoWithRsc());
                        getBrowser().getCRMGraph().addOrder(hbConnectionInfo,
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
                return getBrowser().getCRMGraph().isColocation(hbConnectionInfo);
            }

            @Override
            public String enablePredicate() {
                if (getBrowser().clStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                return null;
            }

            @Override
            public void action() {
                if (this.getText().equals(Tools.getString(
                                  "ClusterBrowser.Hb.RemoveColocation"))) {
                    getBrowser().getCRMGraph().removeColocation(
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
                    getBrowser().getCRMGraph().addColocation(
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
                    if (getBrowser().getCRMGraph().isColocation(hbConnectionInfo)) {
                        getBrowser().getCRMGraph().removeColocation(hbConnectionInfo,
                                                                    dcHost,
                                                                    Application.RunMode.TEST);
                    } else {
                        /* add colocation */
                        /* there is order constraint so let's get the endpoints
                         * from it. */
                        hbConnectionInfo.addColocation(null,
                                                       hbConnectionInfo.getLastServiceInfoParent(),
                                                       hbConnectionInfo.getLastServiceInfoChild());
                        getBrowser().getCRMGraph().addColocation(hbConnectionInfo,
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
