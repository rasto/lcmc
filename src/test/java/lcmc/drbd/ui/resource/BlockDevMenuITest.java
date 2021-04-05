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

import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyMenu;
import lcmc.common.ui.utils.MyMenuItem;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.drbd.domain.BlockDevice;
import lcmc.host.ui.HostBrowser;

@ExtendWith(MockitoExtension.class)
class BlockDevMenuITest {
   @Mock
   private BlockDevInfo blockDevInfoStub;
   @Mock
   private BlockDevice blockDeviceStub;
   @Mock
   private HostBrowser hostBrowserStub;
   @Mock
   private BlockDevInfo blockDevInfoNoClusterStub;
   @Mock
   private HostBrowser hostBrowserNoClusterStub;
   @Mock
   private MyMenu menuStub;
   @Mock
   private MyMenuItem menuItemStub;
   @Mock
   private MenuFactory menuFactoryStub;
   @InjectMocks
   private BlockDevMenu blockDevMenu;
   @InjectMocks
   private BlockDevMenu blockDevMenuNoCluster;

   @BeforeEach
   void setUp() {
      when(blockDevInfoStub.getBrowser()).thenReturn(hostBrowserStub);
      when(blockDevInfoStub.getBlockDevice()).thenReturn(blockDeviceStub);

      when(menuFactoryStub.createMenu(any(), any(), any())).thenReturn(menuStub);
      when(menuFactoryStub.createMenuItem(any(), any(), any(), any(), any())).thenReturn(menuItemStub);
      when(menuFactoryStub.createMenuItem(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(menuItemStub);
      when(menuStub.enablePredicate(any())).thenReturn(menuStub);
      when(menuItemStub.enablePredicate(any())).thenReturn(menuItemStub);
      when(menuItemStub.predicate(any())).thenReturn(menuItemStub);
      when(menuItemStub.visiblePredicate(any())).thenReturn(menuItemStub);
      when(menuItemStub.addAction(any())).thenReturn(menuItemStub);
   }

   @Test
   void menuShouldHaveItems() {
      final List<UpdatableItem> items = blockDevMenu.getPulldownMenu(blockDevInfoStub);

      verify(menuItemStub, times(6)).predicate(any());
      verify(menuItemStub, times(19)).visiblePredicate(any());
      verify(menuItemStub, times(19)).enablePredicate(any());
      verify(menuItemStub, times(19)).addAction(any());
      verify(menuStub).enablePredicate(any());
      verify(menuStub).onUpdate(any());
      assertThat((Object) items.size()).isEqualTo(20);
   }

   @Test
   void menuWithOrWithoutClusterShoulBeTheSameSize() {
      when(blockDevInfoNoClusterStub.getBlockDevice()).thenReturn(blockDeviceStub);
      final List<UpdatableItem> itemsWithCluster = blockDevMenu.getPulldownMenu(blockDevInfoStub);
      when(blockDevInfoNoClusterStub.getBrowser()).thenReturn(hostBrowserNoClusterStub);

      final List<UpdatableItem> itemsNoCluster = blockDevMenuNoCluster.getPulldownMenu(blockDevInfoNoClusterStub);

      assertThat(itemsNoCluster.size()).isEqualTo(itemsWithCluster.size());
   }
}
