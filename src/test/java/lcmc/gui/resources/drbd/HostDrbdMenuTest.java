package lcmc.gui.resources.drbd;

import java.util.List;
import lcmc.data.Host;
import lcmc.data.drbd.DrbdInstallation;
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

@RunWith(MockitoJUnitRunner.class)
public class HostDrbdMenuTest {
    static {
        Tools.init();
    }

    private HostDrbdMenu sut;

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

        sut = new HostDrbdMenu(hostStub, hostDrbdInfoStub);
    }

    @Test
    @Category(GuiTest.class)
    public void menuShouldHaveItems() {
        final List<UpdatableItem> items = sut.getPulldownMenu();

        assertEquals(20, items.size());
    }

    @Test
    @Category(GuiTest.class)
    public void menuWithOrWithoutClusterShoulBeTheSameSize() {
        when(hostDrbdInfoNoClusterStub.getBrowser())
                                         .thenReturn(hostBrowserNoClusterStub);

        final HostDrbdMenu hostDrbdMenuNoCluster =
                                    new HostDrbdMenu(hostNoClusterStub,
                                                     hostDrbdInfoNoClusterStub);
        final List<UpdatableItem> itemsWithCluster = sut.getPulldownMenu();

        final List<UpdatableItem> itemsNoCluster =
                                        hostDrbdMenuNoCluster.getPulldownMenu();

        assertTrue(itemsNoCluster.size() == itemsWithCluster.size());
    }

}
