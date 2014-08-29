/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2014, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.gui.resources.crm;

import java.util.List;

import lcmc.model.AccessMode;
import lcmc.gui.ClusterBrowser;
import lcmc.utilities.*;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

import org.mockito.runners.MockitoJUnitRunner;

import javax.swing.*;

@RunWith(MockitoJUnitRunner.class)
public class ServicesMenuTest {
    @Mock
    private ServicesInfo servicesInfoStub;
    @Mock
    private ClusterBrowser clusterBrowserStub;
    @Mock
    private MyMenu menuStub;
    @Mock
    private MyMenuItem menuItemStub;
    @Mock
    private MenuFactory menuFactoryStub;
    @InjectMocks
    private ServicesMenu servicesMenu;

    @Before
    public void setUp() {
        when(servicesInfoStub.getBrowser()).thenReturn(clusterBrowserStub);
        when(menuFactoryStub.createMenuItem(
                anyString(),
                (ImageIcon) anyObject(),
                anyString(),
                (AccessMode) anyObject(),
                (AccessMode) anyObject())).thenReturn(menuItemStub);
        when(menuFactoryStub.createMenuItem(
                anyString(),
                (ImageIcon) anyObject(),
                (AccessMode) anyObject(),
                (AccessMode) anyObject())).thenReturn(menuItemStub);
        when(menuFactoryStub.createMenu(
                anyString(),
                (AccessMode) anyObject(),
                (AccessMode) anyObject())).thenReturn(menuStub);
        when(menuItemStub.enablePredicate((EnablePredicate) anyObject())).thenReturn(menuItemStub);
        when(menuItemStub.visiblePredicate((VisiblePredicate) anyObject())).thenReturn(menuItemStub);
        when(menuItemStub.addAction((MenuAction) anyObject())).thenReturn(menuItemStub);
        when(menuStub.enablePredicate((EnablePredicate) anyObject())).thenReturn(menuStub);

    }

    @Test
    public void menuShouldHaveItems() {
        final List<UpdatableItem> items = servicesMenu.getPulldownMenu(servicesInfoStub);

        verify(menuItemStub, never()).predicate((Predicate) anyObject());
        verify(menuItemStub).visiblePredicate((VisiblePredicate) anyObject());
        verify(menuItemStub, times(5)).enablePredicate((EnablePredicate) anyObject());
        verify(menuItemStub, times(8)).addAction((MenuAction) anyObject());
        verify(menuStub, times(1)).enablePredicate((EnablePredicate) anyObject());
        verify(menuStub, times(1)).onUpdate((Runnable) anyObject());
        assertEquals(9, items.size());
    }

}
