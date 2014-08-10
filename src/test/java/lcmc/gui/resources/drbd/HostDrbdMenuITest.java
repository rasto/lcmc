package lcmc.gui.resources.drbd;

import java.util.List;

import lcmc.gui.GUIData;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.runners.MockitoJUnitRunner;

@Category(GuiTest.class)
@RunWith(MockitoJUnitRunner.class)
public class HostDrbdMenuITest {
    static {
        Tools.init();
    }

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
    @Mock
    private GUIData guiData;
    @InjectMocks
    private HostDrbdMenu hostDrbdMenu;
    @InjectMocks
    private HostDrbdMenu hostDrbdMenuNoCluster;


    @Before
    public void setUp() {
        when(hostDrbdInfoStub.getBrowser()).thenReturn(hostBrowserStub);
        when(hostBrowserStub.getClusterBrowser()).thenReturn(clusterBrowserStub);

    }

    @Test
    public void menuShouldHaveItems() {
        final List<UpdatableItem> items = hostDrbdMenu.getPulldownMenu(hostStub, hostDrbdInfoStub);

        assertEquals(20, items.size());
    }

    @Test
    public void menuWithOrWithoutClusterShoulBeTheSameSize() {
        when(hostDrbdInfoNoClusterStub.getBrowser()).thenReturn(hostBrowserNoClusterStub);

        final List<UpdatableItem> itemsWithCluster = hostDrbdMenu.getPulldownMenu(hostStub, hostDrbdInfoStub);

        final List<UpdatableItem> itemsNoCluster =
                                hostDrbdMenuNoCluster.getPulldownMenu(hostNoClusterStub, hostDrbdInfoNoClusterStub);

        assertTrue(itemsNoCluster.size() == itemsWithCluster.size());
    }

}
