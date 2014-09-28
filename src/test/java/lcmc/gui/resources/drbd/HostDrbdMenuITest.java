package lcmc.gui.resources.drbd;

import java.util.List;

import lcmc.drbd.ui.resource.HostDrbdInfo;
import lcmc.drbd.ui.resource.HostDrbdMenu;
import lcmc.common.ui.GUIData;
import lcmc.common.domain.AccessMode;
import lcmc.host.domain.Host;
import lcmc.host.ui.HostBrowser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import lcmc.common.domain.EnablePredicate;
import lcmc.common.ui.utils.MenuAction;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyMenu;
import lcmc.common.ui.utils.MyMenuItem;
import lcmc.common.domain.Predicate;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.common.domain.VisiblePredicate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.runners.MockitoJUnitRunner;

import javax.swing.ImageIcon;

@RunWith(MockitoJUnitRunner.class)
public class HostDrbdMenuITest {
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
    private GUIData guiData;
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


    @Before
    public void setUp() {
        when(hostDrbdInfoStub.getBrowser()).thenReturn(hostBrowserStub);
        when(menuFactoryStub.createMenuItem(
                anyString(),
                (ImageIcon) anyObject(),
                anyString(),
                (AccessMode) anyObject(),
                (AccessMode) anyObject())).thenReturn(menuItemStub);
        when(menuFactoryStub.createMenuItem(anyString(),
                (ImageIcon) anyObject(),
                anyString(),

                anyString(),
                (ImageIcon) anyObject(),
                anyString(),

                (AccessMode) anyObject(),
                (AccessMode) anyObject())).thenReturn(menuItemStub);
        when(menuFactoryStub.createMenu(
                anyString(),
                (AccessMode) anyObject(),
                (AccessMode) anyObject())).thenReturn(menuStub);
        when(menuItemStub.enablePredicate((EnablePredicate) anyObject())).thenReturn(menuItemStub);
        when(menuItemStub.visiblePredicate((VisiblePredicate) anyObject())).thenReturn(menuItemStub);
        when(menuItemStub.predicate((Predicate) anyObject())).thenReturn(menuItemStub);
        when(menuItemStub.addAction((MenuAction) anyObject())).thenReturn(menuItemStub);
        when(menuStub.enablePredicate((EnablePredicate) anyObject())).thenReturn(menuStub);

    }

    @Test
    public void menuShouldHaveItems() {
        final List<UpdatableItem> items = hostDrbdMenu.getPulldownMenu(hostStub, hostDrbdInfoStub);

        verify(menuItemStub, times(1)).predicate((Predicate) anyObject());
        verify(menuItemStub, times(2)).visiblePredicate((VisiblePredicate) anyObject());
        verify(menuItemStub, times(11)).enablePredicate((EnablePredicate) anyObject());
        verify(menuItemStub, times(17)).addAction((MenuAction) anyObject());
        verify(menuStub, times(1)).enablePredicate((EnablePredicate) anyObject());
        verify(menuStub, times(2)).onUpdate((Runnable) anyObject());
        assertEquals(19, items.size());
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
