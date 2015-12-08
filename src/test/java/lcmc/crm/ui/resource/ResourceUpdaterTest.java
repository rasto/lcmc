/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2015, Rastislav Levrinc.
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

package lcmc.crm.ui.resource;

import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.domain.Application;
import lcmc.common.ui.treemenu.TreeMenuController;
import lcmc.crm.domain.ClusterStatus;
import lcmc.crm.ui.CrmGraph;
import lcmc.testutils.annotation.type.IntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.inject.Named;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Category(IntegrationTest.class)
@Named
public class ResourceUpdaterTest {
    @Mock
    private ServicesInfo servicesInfo;
    @Mock
    private ClusterStatus clusterStatus;
    @Mock
    private ClusterBrowser clusterBrowser;
    @Mock
    private CrmGraph crmGraph;
    @Mock
    private TreeMenuController treeMenuController;
    @InjectMocks
    private ResourceUpdater resourceUpdater = new ResourceUpdater();

    @Before
    public void setUp() {
        when(clusterBrowser.getCrmGraph()).thenReturn(crmGraph);
    }

    @Test
    public void shouldSetResources() {
        resourceUpdater.setAllResources(servicesInfo, clusterBrowser, clusterStatus, Application.RunMode.LIVE);
    }
}