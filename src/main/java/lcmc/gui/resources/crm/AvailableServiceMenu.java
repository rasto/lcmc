package lcmc.gui.resources.crm;

import java.util.ArrayList;
import java.util.List;
import lcmc.data.AccessMode;
import lcmc.data.Application;
import lcmc.gui.ClusterBrowser;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

public class AvailableServiceMenu {
    private final AvailableServiceInfo availableServiceInfo;
    
    public AvailableServiceMenu(final AvailableServiceInfo availableServiceInfo) {
        this.availableServiceInfo = availableServiceInfo;
    }

    public List<UpdatableItem> getPulldownMenu() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final UpdatableItem addServiceMenu = new MyMenuItem(
                        Tools.getString("ClusterBrowser.AddServiceToCluster"),
                        null,
                        null,
                        new AccessMode(Application.AccessType.ADMIN, false),
                        new AccessMode(Application.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (availableServiceInfo.getBrowser().clStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                return null;
            }

            @Override
            public void action() {
                availableServiceInfo.hidePopup();
                final ServicesInfo si = availableServiceInfo.getBrowser().getServicesInfo();
                si.addServicePanel(availableServiceInfo.getResourceAgent(),
                                   null, /* pos */
                                   true,
                                   null,
                                   null,
                                   Application.RunMode.LIVE);
            }
        };
        items.add(addServiceMenu);
        return items;
    }
}
