package lcmc.gui.resources.drbd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import lcmc.data.Cluster;
import lcmc.data.Host;
import lcmc.gui.HostBrowser;
import lcmc.testutils.annotation.type.GuiTest;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HostDrbdMenuTest {
    
    private HostDrbdMenu hostDrbdMenu;

    @Before
    public void setUp() {
        Tools.init();

        final Cluster cluster = new Cluster();
        final Host host = new Host();
        host.setCluster(cluster);
        cluster.createClusterBrowser();
        final HostDrbdInfo hostDrbdInfo =
                                     new HostDrbdInfo(host, host.getBrowser());
        hostDrbdMenu = new HostDrbdMenu(host, hostDrbdInfo);
    }

    @Test
    @Category(GuiTest.class)
    public void menuShouldHaveAtLeastTwoItems() {
        final List<UpdatableItem> items = hostDrbdMenu.getPulldownMenu();

        assertTrue(items.size() > 1);
    }

    @Test
    @Category(GuiTest.class)
    public void menuWithOrWithoutClusterShoulBeTheSameSize() {
        final Host host = new Host();
        final HostDrbdInfo hostDrbdInfo =
                                      new HostDrbdInfo(host, host.getBrowser());
        final HostDrbdMenu hostDrbdMenuNoCluster =
                                           new HostDrbdMenu(host, hostDrbdInfo);
        final List<UpdatableItem> itemsWithCluster =
                                                 hostDrbdMenu.getPulldownMenu();

        final List<UpdatableItem> itemsNoCluster =
                                        hostDrbdMenuNoCluster.getPulldownMenu();

        assertTrue(itemsNoCluster.size() == itemsWithCluster.size());
    }

}
