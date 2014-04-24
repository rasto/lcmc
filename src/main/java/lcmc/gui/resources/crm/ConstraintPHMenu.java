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

public class ConstraintPHMenu extends ServiceMenu {

    private final ConstraintPHInfo constraintPHInfo;
    
    public ConstraintPHMenu(final ConstraintPHInfo constraintPHInfo) {
        super(constraintPHInfo);
        this.constraintPHInfo = constraintPHInfo;
    }

    @Override
    public List<UpdatableItem> getPulldownMenu() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final Application.RunMode runMode = Application.RunMode.LIVE;
        addDependencyMenuItems(items, true, runMode);
        /* remove the placeholder and all constraints associated with it. */
        final MyMenuItem removeMenuItem = new MyMenuItem(
                    Tools.getString("ConstraintPHInfo.Remove"),
                    ClusterBrowser.REMOVE_ICON,
                    ClusterBrowser.STARTING_PTEST_TOOLTIP,
                    new AccessMode(Application.AccessType.ADMIN, false),
                    new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (getBrowser().clStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                } else if (constraintPHInfo.getService().isRemoved()) {
                    return ConstraintPHInfo.IS_BEING_REMOVED_STRING;
                }
                return null;
            }

            @Override
            public void action() {
                constraintPHInfo.hidePopup();
                constraintPHInfo.removeMyself(Application.RunMode.LIVE);
                getBrowser().getCRMGraph().repaint();
            }
        };
        final ButtonCallback removeItemCallback =
                                    getBrowser().new ClMenuItemCallback(null) {
            @Override
            public boolean isEnabled() {
                return super.isEnabled() && !getService().isNew();
            }
            @Override
            public void action(final Host dcHost) {
                constraintPHInfo.removeMyselfNoConfirm(dcHost, Application.RunMode.TEST);
            }
        };
        constraintPHInfo.addMouseOverListener(removeMenuItem, removeItemCallback);
        items.add(removeMenuItem);
        return items;
    }

}
