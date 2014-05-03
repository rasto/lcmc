package lcmc.gui.resources.drbd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.JMenuItem;
import lcmc.data.AccessMode;
import lcmc.data.Application;
import lcmc.utilities.MyMenu;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

public class ResourceMenu {
    
    private final ResourceInfo resourceInfo;

    public ResourceMenu(final ResourceInfo resourceInfo) {
        this.resourceInfo = resourceInfo;
    }

    public List<UpdatableItem> getPulldownMenu() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        for (final VolumeInfo dvi : resourceInfo.getDrbdVolumes()) {
            final UpdatableItem volumesMenu = new MyMenu(
                            dvi.toString(),
                            new AccessMode(Application.AccessType.RO, false),
                            new AccessMode(Application.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void updateAndWait() {
                    Tools.isSwingThread();
                    removeAll();
                    final Collection<UpdatableItem> volumeMenus =
                                    new ArrayList<UpdatableItem>();
                    for (final UpdatableItem u : dvi.createPopup()) {
                        volumeMenus.add(u);
                    }
                    for (final UpdatableItem u : volumeMenus) {
                        add((JMenuItem) u);
                    }
                    super.updateAndWait();
                }
            };
            items.add(volumesMenu);
        }
        return items;
    }
}
