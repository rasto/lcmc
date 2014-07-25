package lcmc.gui.resources.drbd;

import java.util.List;
import lcmc.model.Host;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.HostBrowser;
import lcmc.testutils.annotation.type.GuiTest;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.runners.MockitoJUnitRunner;

@Category(GuiTest.class)
@RunWith(MockitoJUnitRunner.class)
public class HostDrbdMenuITest {
    static {
        Tools.init();
    }

    private HostDrbdMenu hostDrbdMenu;

    @Mock
    private Host hostStub;
    @Mock
    private HostDrbdInfo hostDrbdInfoStub;
    @Mock
    private HostBrowser hostBrowserStub;
    @Mock
    private ClusterBrowser clusterBrowserStub;

    @Mock
    private Host hostNoClusterStub;
    @Mock
    private HostDrbdInfo hostDrbdInfoNoClusterStub;
    @Mock
    private HostBrowser hostBrowserNoClusterStub;

    @Before
    public void setUp() {
        when(hostDrbdInfoStub.getBrowser()).thenReturn(hostBrowserStub);
        when(hostBrowserStub.getClusterBrowser()).thenReturn(clusterBrowserStub);

        hostDrbdMenu = new HostDrbdMenu(hostStub, hostDrbdInfoStub);
    }

    @Test
    public void menuShouldHaveItems() {
        final List<UpdatableItem> items = hostDrbdMenu.getPulldownMenu();

        assertEquals(20, items.size());
    }

    @Test
    public void menuWithOrWithoutClusterShoulBeTheSameSize() {
        when(hostDrbdInfoNoClusterStub.getBrowser()).thenReturn(hostBrowserNoClusterStub);

        final HostDrbdMenu hostDrbdMenuNoCluster = new HostDrbdMenu(hostNoClusterStub, hostDrbdInfoNoClusterStub);
        final List<UpdatableItem> itemsWithCluster = hostDrbdMenu.getPulldownMenu();

        final List<UpdatableItem> itemsNoCluster = hostDrbdMenuNoCluster.getPulldownMenu();

        assertTrue(itemsNoCluster.size() == itemsWithCluster.size());
    }

}
