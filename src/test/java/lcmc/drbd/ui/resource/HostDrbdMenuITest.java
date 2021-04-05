package lcmc.drbd.ui.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyMenu;
import lcmc.common.ui.utils.MyMenuItem;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.host.domain.Host;
import lcmc.host.ui.HostBrowser;

@ExtendWith(MockitoExtension.class)
class HostDrbdMenuITest {
    @Mock
    private Host hostStub;
    @Mock
    private HostDrbdInfo hostDrbdInfoStub;
    @Mock
    private HostBrowser hostBrowserStub;
    @Mock
    private Host hostNoClusterStub;
    @Mock
    private HostDrbdInfo hostDrbdInfoNoClusterStub;
    @Mock
    private HostBrowser hostBrowserNoClusterStub;
    @Mock
    private MainData mainData;
    @Mock
    private MyMenu menuStub;
    @Mock
    private MyMenuItem menuItemStub;
    @Mock
    private MenuFactory menuFactoryStub;
    @InjectMocks
    private HostDrbdMenu hostDrbdMenu;
    @InjectMocks
    private HostDrbdMenu hostDrbdMenuNoCluster;


    @BeforeEach
    void setUp() {
        when(hostDrbdInfoStub.getBrowser()).thenReturn(hostBrowserStub);
        when(menuFactoryStub.createMenuItem(any(), any(), any(), any(), any())).thenReturn(menuItemStub);
        when(menuFactoryStub.createMenuItem(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(menuItemStub);
        when(menuFactoryStub.createMenu(any(), any(), any())).thenReturn(menuStub);
        when(menuItemStub.enablePredicate(any())).thenReturn(menuItemStub);
        when(menuItemStub.visiblePredicate(any())).thenReturn(menuItemStub);
        when(menuItemStub.predicate(any())).thenReturn(menuItemStub);
        when(menuItemStub.addAction(any())).thenReturn(menuItemStub);
        when(menuStub.enablePredicate(any())).thenReturn(menuStub);

    }

    @Test
    void menuShouldHaveItems() {
        final List<UpdatableItem> items = hostDrbdMenu.getPulldownMenu(hostStub, hostDrbdInfoStub);

        verify(menuItemStub, times(1)).predicate(any());
        verify(menuItemStub, times(2)).visiblePredicate(any());
        verify(menuItemStub, times(11)).enablePredicate(any());
        verify(menuItemStub, times(17)).addAction(any());
        verify(menuStub, times(1)).enablePredicate(any());
        verify(menuStub, times(2)).onUpdate(any());
        assertThat(items).hasSize(19);
    }

    @Test
    void menuWithOrWithoutClusterShoulBeTheSameSize() {
        when(hostDrbdInfoNoClusterStub.getBrowser()).thenReturn(hostBrowserNoClusterStub);

        final List<UpdatableItem> itemsWithCluster = hostDrbdMenu.getPulldownMenu(hostStub, hostDrbdInfoStub);

        final List<UpdatableItem> itemsNoCluster =
                hostDrbdMenuNoCluster.getPulldownMenu(hostNoClusterStub, hostDrbdInfoNoClusterStub);

        assertThat(itemsNoCluster).hasSize(itemsWithCluster.size());
    }
}
