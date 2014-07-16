package lcmc.gui.resources.drbd;

import java.util.List;
import lcmc.data.Host;
import lcmc.data.resources.BlockDevice;
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
public class BlockDevMenuITest {
    static {
        Tools.init();
    }
    
    @Mock
    private Host hostStub;
    @Mock
    private BlockDevInfo blockDevInfoStub;
    @Mock
    private BlockDevice blockDeviceStub;
    @Mock
    private HostBrowser hostBrowserStub;
    @Mock
    private ClusterBrowser clusterBrowserStub;

    @Mock
    private BlockDevInfo blockDevInfoNoClusterStub;
    @Mock
    private HostBrowser hostBrowserNoClusterStub;

    private BlockDevMenu sut;

    @Before
    public void setUp() {
        when(blockDevInfoStub.getBrowser()).thenReturn(hostBrowserStub);
        when(blockDevInfoStub.getHost()).thenReturn(hostStub);
        when(blockDevInfoStub.getBlockDevice()).thenReturn(blockDeviceStub);
        when(hostBrowserStub.getClusterBrowser()).thenReturn(clusterBrowserStub);

        when(blockDevInfoNoClusterStub.getBrowser()).thenReturn(hostBrowserStub);
        when(blockDevInfoNoClusterStub.getHost()).thenReturn(hostStub);
        when(blockDevInfoNoClusterStub.getBlockDevice()).thenReturn(blockDeviceStub);

        sut = new BlockDevMenu(blockDevInfoStub);
    }

    @Test
    public void menuShouldHaveItems() {
        final List<UpdatableItem> items = sut.getPulldownMenu();

        assertEquals(20, items.size());
    }

    @Test
    public void menuWithOrWithoutClusterShoulBeTheSameSize() {
        final List<UpdatableItem> itemsWithCluster = sut.getPulldownMenu();
        when(blockDevInfoNoClusterStub.getBrowser())
                .thenReturn(hostBrowserNoClusterStub);
        final BlockDevMenu blockDevMenuNoCluster =
                                    new BlockDevMenu(blockDevInfoNoClusterStub);

        final List<UpdatableItem> itemsNoCluster =
                                        blockDevMenuNoCluster.getPulldownMenu();

        assertTrue(itemsNoCluster.size() == itemsWithCluster.size());
    }
}
