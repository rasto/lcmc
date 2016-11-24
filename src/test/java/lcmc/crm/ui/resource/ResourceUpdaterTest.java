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

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.domain.Application;
import lcmc.crm.domain.ClusterStatus;
import lcmc.crm.domain.CrmXml;
import lcmc.crm.domain.ResourceAgent;
import lcmc.crm.domain.RscSetConnectionData;
import lcmc.crm.ui.CrmGraph;
import lcmc.crm.ui.resource.update.ResourceUpdater;
import lcmc.host.domain.Host;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.tree.DefaultMutableTreeNode;

import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Named
public class ResourceUpdaterTest {
    private static final String GROUP_1 = "GROUP1";
    private static final String SERVICE_1 = "Service1";
    private static final Application.RunMode RUN_MODE = Application.RunMode.LIVE;
    private static final String CLONE_1 = "CLONE_1";
    @Mock
    private Application application;
    @Mock
    private ServicesInfo servicesInfo;
    @Mock
    private ClusterStatus clusterStatus;
    @Mock
    private ClusterBrowser clusterBrowser;
    @Mock
    private CrmGraph crmGraph;
    @Mock
    private CrmServiceFactory crmServiceFactory;

    private ConstraintPHInfo newConstraintPHInfo = null;
    @Spy
    private Provider<ConstraintPHInfo> constraintPHInfoProvider = new Provider<ConstraintPHInfo>() {
        @Override
        public ConstraintPHInfo get() {
            return newConstraintPHInfo;
        }
    };
    @Spy
    private Provider<PcmkRscSetsInfo> pcmkRscSetsInfoProvider = new Provider<PcmkRscSetsInfo>() {
        @Override
        public PcmkRscSetsInfo get() {
            return mock(PcmkRscSetsInfo.class);
        }
    };
    @InjectMocks
    private ResourceUpdater resourceUpdater = new ResourceUpdater();
    public static final Map<String, String> EXAMPLE_PARAMS = new HashMap<String, String>() {{
        put("param1", "value1");
    }};
    public static final Map<String, String> EXAMPLE_PARAMS_2 = new HashMap<String, String>() {{
        put("param2", "value2");
    }};

    @Before
    public void setUp() {
        when(clusterBrowser.getCrmGraph()).thenReturn(crmGraph);
        when(clusterBrowser.getClusterStatus()).thenReturn(clusterStatus);
        when(clusterBrowser.getClusterHosts()).thenReturn(new Host[]{});
    }

    @Test
    public void shouldNewServicePanel() {
        //given:
        val serviceInfo = mock(ServiceInfo.class);
        when(clusterStatus.getAllGroupsAndClones()).thenReturn(Sets.newHashSet("none"));
        when(clusterStatus.getGroupResources("none", RUN_MODE))
                .thenReturn(Optional.<List<String>>of(Lists.newArrayList(SERVICE_1)));
        when(crmServiceFactory.createServiceWithParameters(
                eq(SERVICE_1),
                (ResourceAgent) anyObject(),
                eq(EXAMPLE_PARAMS),
                eq(clusterBrowser))).thenReturn(serviceInfo);

        when(clusterStatus.getParamValuePairs(SERVICE_1)).thenReturn(EXAMPLE_PARAMS);

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        verify(servicesInfo).addServicePanel(serviceInfo, null, false, false, Application.RunMode.LIVE);
    }

    @Test
    public void shouldSetParametersOfExistingGroup() {
        //given:
        val groupInfo = mock(GroupInfo.class);
        when(clusterStatus.getAllGroupsAndClones()).thenReturn(Sets.newHashSet(GROUP_1));
        when(clusterBrowser.getServiceInfoFromCRMId(GROUP_1)).thenReturn(groupInfo);

        when(clusterStatus.getParamValuePairs(GROUP_1)).thenReturn(EXAMPLE_PARAMS);
        when(clusterStatus.getGroupResources(GROUP_1, Application.RunMode.LIVE)).thenReturn(Optional.of(Collections.<String>emptyList()));

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        verify(groupInfo).setParameters(EXAMPLE_PARAMS);
    }

    @Test
    public void shouldSetParametersOfNewGroup() {
        //given:
        val groupInfo = mock(GroupInfo.class);
        when(clusterStatus.getAllGroupsAndClones()).thenReturn(Sets.newHashSet(GROUP_1));
        when(servicesInfo.addServicePanel((ResourceAgent) anyObject(),
                (Point2D) anyObject(),
                anyBoolean(),
                anyString(),
                (CloneInfo) anyObject(),
                (Application.RunMode) anyObject())).thenReturn(groupInfo);

        when(clusterStatus.getParamValuePairs(GROUP_1)).thenReturn(EXAMPLE_PARAMS);
        when(clusterStatus.getGroupResources(GROUP_1, Application.RunMode.LIVE)).thenReturn(Optional.of(Collections.<String>emptyList()));

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        verify(groupInfo).setParameters(EXAMPLE_PARAMS);
    }

    @Test
    public void shouldSetParametersOfNewClonedGroup() {
        //given:
        val cloneInfo = mock(CloneInfo.class);
        val groupInfo = mock(GroupInfo.class);
        when(clusterBrowser.getServiceInfoFromCRMId(CLONE_1)).thenReturn(cloneInfo);
        when(clusterStatus.getAllGroupsAndClones()).thenReturn(Sets.newHashSet(GROUP_1, CLONE_1));
        when(clusterStatus.isClone(CLONE_1)).thenReturn(true);
        when(clusterStatus.getGroupResources(CLONE_1, RUN_MODE))
                .thenReturn(Optional.<List<String>>of(Lists.newArrayList(GROUP_1)));

        when(servicesInfo.addServicePanel((ResourceAgent) anyObject(),
                (Point2D) anyObject(),
                anyBoolean(),
                anyString(),
                (CloneInfo) anyObject(),
                (Application.RunMode) anyObject())).thenReturn(groupInfo);

        when(clusterStatus.getParamValuePairs(GROUP_1)).thenReturn(EXAMPLE_PARAMS);
        when(clusterStatus.getParamValuePairs(CLONE_1)).thenReturn(EXAMPLE_PARAMS_2);
        when(clusterStatus.getGroupResources(GROUP_1, Application.RunMode.LIVE)).thenReturn(Optional.of(Collections.<String>emptyList()));

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        verify(groupInfo, atLeastOnce()).setParameters(EXAMPLE_PARAMS); //TODO: times(1)
        verify(cloneInfo).setParameters(EXAMPLE_PARAMS_2);
    }

    @Test
    public void shouldSetParametersOfExistingGroupService() {
        //given:
        val groupInfo = mock(GroupInfo.class);
        when(clusterStatus.getAllGroupsAndClones()).thenReturn(Sets.newHashSet(GROUP_1));
        when(clusterBrowser.getServiceInfoFromCRMId(GROUP_1)).thenReturn(groupInfo);
        when(clusterStatus.getGroupResources(GROUP_1, RUN_MODE))
                .thenReturn(Optional.<List<String>>of(Lists.newArrayList(SERVICE_1)));
        val groupService = mock(ServiceInfo.class);
        when(clusterBrowser.getServiceInfoFromCRMId(SERVICE_1)).thenReturn(groupService);

        when(clusterStatus.getParamValuePairs(SERVICE_1)).thenReturn(EXAMPLE_PARAMS);

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        verify(groupService).setParameters(EXAMPLE_PARAMS);
    }

    @Test
    public void shouldAddServiceToGroup() {
        //given:
        val groupInfo = mock(GroupInfo.class);
        when(clusterStatus.getAllGroupsAndClones()).thenReturn(Sets.newHashSet(GROUP_1));
        when(clusterBrowser.getServiceInfoFromCRMId(GROUP_1)).thenReturn(groupInfo);
        when(clusterStatus.getGroupResources(GROUP_1, RUN_MODE))
                .thenReturn(Optional.<List<String>>of(Lists.newArrayList(SERVICE_1)));
        val groupService = mock(ServiceInfo.class);
        when(groupService.getNode()).thenReturn(new DefaultMutableTreeNode());
        when(crmServiceFactory.createServiceWithParameters(
                eq(SERVICE_1),
                (ResourceAgent) anyObject(),
                eq(EXAMPLE_PARAMS),
                eq(clusterBrowser))).thenReturn(groupService);

        when(clusterStatus.getParamValuePairs(SERVICE_1)).thenReturn(EXAMPLE_PARAMS);

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        verify(groupInfo).addGroupServicePanel(groupService, false);
    }

    @Test
    public void shouldAddServiceToClone() {
        //given:
        val cloneInfo = mock(CloneInfo.class);
        when(clusterStatus.getAllGroupsAndClones()).thenReturn(Sets.newHashSet(CLONE_1));
        when(clusterStatus.isClone(CLONE_1)).thenReturn(true);
        when(clusterBrowser.getServiceInfoFromCRMId(CLONE_1)).thenReturn(cloneInfo);
        when(clusterStatus.getGroupResources(CLONE_1, RUN_MODE))
                .thenReturn(Optional.<List<String>>of(Lists.newArrayList(SERVICE_1)));
        val cloneService = mock(ServiceInfo.class);
        when(cloneService.getNode()).thenReturn(new DefaultMutableTreeNode());
        when(crmServiceFactory.createServiceWithParameters(
                eq(SERVICE_1),
                (ResourceAgent) anyObject(),
                eq(EXAMPLE_PARAMS),
                eq(clusterBrowser))).thenReturn(cloneService);

        when(clusterStatus.getParamValuePairs(SERVICE_1)).thenReturn(EXAMPLE_PARAMS);

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        verify(cloneInfo).addCloneServicePanel(cloneService);
    }

    @Test
    public void orphanedResourceShouldBeSkipped() {
        //given:
        val cloneInfo = mock(CloneInfo.class);
        when(clusterStatus.getAllGroupsAndClones()).thenReturn(Sets.newHashSet(CLONE_1));
        when(clusterStatus.isClone(CLONE_1)).thenReturn(true);
        when(clusterBrowser.getServiceInfoFromCRMId(CLONE_1)).thenReturn(cloneInfo);
        when(clusterStatus.getGroupResources(CLONE_1, RUN_MODE))
                .thenReturn(Optional.<List<String>>of(Lists.newArrayList(SERVICE_1)));
        when(clusterStatus.isOrphaned(SERVICE_1)).thenReturn(true);
        when(application.isHideLRM()).thenReturn(true);
        val cloneService = mock(ServiceInfo.class);
        when(cloneService.getNode()).thenReturn(new DefaultMutableTreeNode());
        when(crmServiceFactory.createServiceWithParameters(
                eq(SERVICE_1),
                (ResourceAgent) anyObject(),
                eq(EXAMPLE_PARAMS),
                eq(clusterBrowser))).thenReturn(cloneService);

        when(clusterStatus.getParamValuePairs(SERVICE_1)).thenReturn(EXAMPLE_PARAMS);

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        verify(cloneInfo, never()).addCloneServicePanel(cloneService);
    }

    @Test
    public void shouldAddClonedGroupShouldKeptButNotUpdated() {
        //given:
        val cloneInfo = mock(CloneInfo.class);
        val groupInfo = mock(GroupInfo.class);
        when(groupInfo.getCloneInfo()).thenReturn(cloneInfo);
        when(clusterBrowser.getServiceInfoFromCRMId(GROUP_1)).thenReturn(groupInfo);
        when(clusterStatus.getAllGroupsAndClones()).thenReturn(Sets.newHashSet(GROUP_1));

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        verify(groupInfo, never()).setParameters((Map<String, String>) anyObject());
        servicesInfo.cleanupServiceMenu(Lists.<ServiceInfo>newArrayList(groupInfo));
    }

    @Test
    public void shouldSetParametersOfExistingClone() {
        //given:
        val cloneInfo = mock(CloneInfo.class);
        when(clusterStatus.getAllGroupsAndClones()).thenReturn(Sets.newHashSet(CLONE_1));
        when(clusterStatus.isClone(CLONE_1)).thenReturn(true);
        when(clusterBrowser.getServiceInfoFromCRMId(CLONE_1)).thenReturn(cloneInfo);

        when(clusterStatus.getParamValuePairs(CLONE_1)).thenReturn(EXAMPLE_PARAMS);
        when(clusterStatus.getGroupResources(CLONE_1, Application.RunMode.LIVE)).thenReturn(Optional.of(Collections.<String>emptyList()));

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        verify(cloneInfo).setParameters(EXAMPLE_PARAMS);
    }

    @Test
    public void shouldSetParametersOfNewClone() {
        //given:
        val cloneInfo = mock(CloneInfo.class);
        when(clusterStatus.getAllGroupsAndClones()).thenReturn(Sets.newHashSet(CLONE_1));
        when(clusterStatus.getGroupResources(CLONE_1, Application.RunMode.LIVE)).thenReturn(Optional.of(Collections.<String>emptyList()));
        when(clusterStatus.isClone(CLONE_1)).thenReturn(true);
        when(servicesInfo.addServicePanel(
                (ResourceAgent) anyObject(),
                (Point2D) anyObject(),
                anyBoolean(),
                eq(CLONE_1),
                (CloneInfo) anyObject(),
                (Application.RunMode) anyObject())).thenReturn(cloneInfo);

        when(clusterStatus.getParamValuePairs(CLONE_1)).thenReturn(EXAMPLE_PARAMS);

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        verify(cloneInfo).setParameters(EXAMPLE_PARAMS);
    }

    @Test
    public void shouldAddResourceSetOrdersToExistingPlaceholder() {
        //given:
        final ConstraintPHInfo constraintPlaceHolder = mock(ConstraintPHInfo.class);
        final Map<String, ServiceInfo> placeholderMap = new HashMap<String, ServiceInfo>() {{
            put("Placeholder", constraintPlaceHolder);
        }};
        when(clusterBrowser.getNameToServiceInfoHash(ConstraintPHInfo.NAME))
                .thenReturn(placeholderMap);
        boolean isColocation = false;
        final RscSetConnectionData rscSetConnectionData = createRscSetConnectionData(isColocation);
        when(clusterStatus.getRscSetConnections())
                .thenReturn(Lists.newArrayList(rscSetConnectionData));
        when(constraintPlaceHolder.getRscSetConnectionDataOrder())
                .thenReturn(rscSetConnectionData);
        final ServiceInfo service1 = mock(ServiceInfo.class);
        final ServiceInfo service2 = mock(ServiceInfo.class);
        when(clusterBrowser.getServiceInfoFromCRMId("rsc1")).thenReturn(service1);
        when(clusterBrowser.getServiceInfoFromCRMId("rsc2")).thenReturn(service2);

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        verify(crmGraph).addOrder("constraintId1", service1, constraintPlaceHolder);
        verify(crmGraph).addOrder("constraintId1", constraintPlaceHolder, service2);

    }

    @Test
    public void shouldAddResourceSetColocationsToExistingPlaceholder() {
        //given:
        final ConstraintPHInfo constraintPlaceHolder = mock(ConstraintPHInfo.class);
        final Map<String, ServiceInfo> placeholderMap = new HashMap<String, ServiceInfo>() {{
            put("Placeholder", constraintPlaceHolder);
        }};
        when(clusterBrowser.getNameToServiceInfoHash(ConstraintPHInfo.NAME))
                .thenReturn(placeholderMap);
        boolean isColocation = true;
        final RscSetConnectionData rscSetConnectionData = createRscSetConnectionData(isColocation);
        when(clusterStatus.getRscSetConnections())
                .thenReturn(Lists.newArrayList(rscSetConnectionData));
        when(constraintPlaceHolder.getRscSetConnectionDataColocation())
                .thenReturn(rscSetConnectionData);
        final ServiceInfo service1 = mock(ServiceInfo.class);
        final ServiceInfo service2 = mock(ServiceInfo.class);
        when(clusterBrowser.getServiceInfoFromCRMId("rsc1")).thenReturn(service1);
        when(clusterBrowser.getServiceInfoFromCRMId("rsc2")).thenReturn(service2);

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        verify(crmGraph).addColocation("constraintId1", constraintPlaceHolder, service1);
        verify(crmGraph).addColocation("constraintId1", service2, constraintPlaceHolder);

    }

    @Test
    public void shouldAddNewPlaceholderWithColocation() {
        //given:
        final ConstraintPHInfo constraintPlaceHolder = mock(ConstraintPHInfo.class);
        newConstraintPHInfo = constraintPlaceHolder;
        boolean isColocation = true;

        final RscSetConnectionData oldRscSetConnectionData = createRscSetConnectionData("oldrsc1", "oldrsc2", isColocation);
        final Map<String, ServiceInfo> placeholderMap = new HashMap<String, ServiceInfo>() {{
            put("Placeholder", constraintPlaceHolder);
        }};
        when(clusterBrowser.getNameToServiceInfoHash(ConstraintPHInfo.NAME))
                .thenReturn(placeholderMap);

        final RscSetConnectionData newRscSetConnectionData = createRscSetConnectionData(isColocation);
        when(clusterStatus.getRscSetConnections())
                .thenReturn(Lists.newArrayList(oldRscSetConnectionData));
        when(constraintPlaceHolder.getRscSetConnectionDataColocation())
                .thenReturn(newRscSetConnectionData);
        final ServiceInfo service1 = mock(ServiceInfo.class);
        final ServiceInfo service2 = mock(ServiceInfo.class);
        when(clusterBrowser.getServiceInfoFromCRMId("rsc1")).thenReturn(service1);
        when(clusterBrowser.getServiceInfoFromCRMId("rsc2")).thenReturn(service2);

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        verify(crmGraph).addConstraintPlaceholder(constraintPlaceHolder, null, Application.RunMode.LIVE);
    }

    @Test
    public void shouldAddNewPlaceholderWithOrder() {
        //given:
        final ConstraintPHInfo constraintPlaceHolder = mock(ConstraintPHInfo.class);
        newConstraintPHInfo = constraintPlaceHolder;
        boolean isColocation = false;
        final RscSetConnectionData rscSetConnectionData = createRscSetConnectionData(isColocation);
        when(clusterStatus.getRscSetConnections())
                .thenReturn(Lists.newArrayList(rscSetConnectionData));
        when(constraintPlaceHolder.getRscSetConnectionDataOrder())
                .thenReturn(rscSetConnectionData);
        final ServiceInfo service1 = mock(ServiceInfo.class);
        final ServiceInfo service2 = mock(ServiceInfo.class);
        when(clusterBrowser.getServiceInfoFromCRMId("rsc1")).thenReturn(service1);
        when(clusterBrowser.getServiceInfoFromCRMId("rsc2")).thenReturn(service2);

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        verify(crmGraph).addConstraintPlaceholder(constraintPlaceHolder, null, Application.RunMode.LIVE);
    }

    @Test
    public void shouldUpdatePlaceholderWithColocation() {
        //given:
        final ConstraintPHInfo constraintPlaceHolder = mock(ConstraintPHInfo.class);
        final Map<String, ServiceInfo> placeholderMap = new HashMap<String, ServiceInfo>() {{
            put("Placeholder", constraintPlaceHolder);
        }};
        when(clusterBrowser.getNameToServiceInfoHash(ConstraintPHInfo.NAME))
                .thenReturn(placeholderMap);
        boolean isColocation = true;
        final RscSetConnectionData oldRscSetConnectionData = createRscSetConnectionData(isColocation);
        final RscSetConnectionData newRscSetConnectionData = createRscSetConnectionData(isColocation);
        when(clusterStatus.getRscSetConnections())
                .thenReturn(Lists.newArrayList(newRscSetConnectionData));
        when(constraintPlaceHolder.getRscSetConnectionDataColocation())
                .thenReturn(oldRscSetConnectionData);
        final ServiceInfo service1 = mock(ServiceInfo.class);
        final ServiceInfo service2 = mock(ServiceInfo.class);
        when(clusterBrowser.getServiceInfoFromCRMId("rsc1")).thenReturn(service1);
        when(clusterBrowser.getServiceInfoFromCRMId("rsc2")).thenReturn(service2);

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        verify(crmGraph).addColocation("constraintId1", constraintPlaceHolder, service1);
        verify(crmGraph).addColocation("constraintId1", service2, constraintPlaceHolder);
    }

    @Test
    public void shouldAddColocationToExistingOrder() {

        //given:
        final ConstraintPHInfo constraintPlaceHolder = mock(ConstraintPHInfo.class);
        final Map<String, ServiceInfo> placeholderMap = new HashMap<String, ServiceInfo>() {{
            put("Placeholder", constraintPlaceHolder);
        }};
        when(clusterBrowser.getNameToServiceInfoHash(ConstraintPHInfo.NAME))
                .thenReturn(placeholderMap);
        boolean isColocation = true;
        final RscSetConnectionData oldRscSetConnectionData = createRscSetConnectionData("rsc1", "rsc2", !isColocation);
        final RscSetConnectionData newRscSetConnectionData = createRscSetConnectionData("rsc1", "rsc2", isColocation);
        when(clusterStatus.getRscSetConnections())
                .thenReturn(Lists.newArrayList(newRscSetConnectionData));
        when(constraintPlaceHolder.getRscSetConnectionDataColocation())
                .thenReturn(oldRscSetConnectionData);
        final ServiceInfo service1 = mock(ServiceInfo.class);
        final ServiceInfo service2 = mock(ServiceInfo.class);
        when(clusterBrowser.getServiceInfoFromCRMId("rsc1")).thenReturn(service1);
        when(clusterBrowser.getServiceInfoFromCRMId("rsc2")).thenReturn(service2);
        when(constraintPlaceHolder.sameConstraintId(newRscSetConnectionData)).thenReturn(true);
        final PcmkRscSetsInfo pcmkRscSetsInfo = mock(PcmkRscSetsInfo.class);
        when(constraintPlaceHolder.getPcmkRscSetsInfo()).thenReturn(pcmkRscSetsInfo);

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        pcmkRscSetsInfo.addColocation("constraintId1", constraintPlaceHolder);
    }

    @Test
    public void shouldAddOrderExistingColocation() {

        //given:
        final ConstraintPHInfo constraintPlaceHolder = mock(ConstraintPHInfo.class);
        final Map<String, ServiceInfo> placeholderMap = new HashMap<String, ServiceInfo>() {{
            put("Placeholder", constraintPlaceHolder);
        }};
        when(clusterBrowser.getNameToServiceInfoHash(ConstraintPHInfo.NAME))
                .thenReturn(placeholderMap);
        boolean isColocation = true;
        final RscSetConnectionData oldRscSetConnectionData = createRscSetConnectionData("rsc1", "rsc2", isColocation);
        final RscSetConnectionData newRscSetConnectionData = createRscSetConnectionData("rsc1", "rsc2", !isColocation);
        when(clusterStatus.getRscSetConnections())
                .thenReturn(Lists.newArrayList(newRscSetConnectionData));
        when(constraintPlaceHolder.getRscSetConnectionDataColocation())
                .thenReturn(oldRscSetConnectionData);
        final ServiceInfo service1 = mock(ServiceInfo.class);
        final ServiceInfo service2 = mock(ServiceInfo.class);
        when(clusterBrowser.getServiceInfoFromCRMId("rsc1")).thenReturn(service1);
        when(clusterBrowser.getServiceInfoFromCRMId("rsc2")).thenReturn(service2);
        when(constraintPlaceHolder.sameConstraintId(newRscSetConnectionData)).thenReturn(true);
        final PcmkRscSetsInfo pcmkRscSetsInfo = mock(PcmkRscSetsInfo.class);
        when(constraintPlaceHolder.getPcmkRscSetsInfo()).thenReturn(pcmkRscSetsInfo);

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        pcmkRscSetsInfo.addOrder("constraintId1", constraintPlaceHolder);
    }

    @Test
    public void shouldAddResourceSetOrdersToNewPlaceholder() {
        //given:
        final ConstraintPHInfo constraintPlaceHolder = mock(ConstraintPHInfo.class);
        when(constraintPlaceHolder.isNew()).thenReturn(true);
        final Map<String, ServiceInfo> placeholderMap = new HashMap<String, ServiceInfo>() {{
            put("Placeholder", constraintPlaceHolder);
        }};
        when(clusterBrowser.getNameToServiceInfoHash(ConstraintPHInfo.NAME))
                .thenReturn(placeholderMap);
        boolean isColocation = false;
        final RscSetConnectionData newRscSetConnectionData = createRscSetConnectionData("rsc1", "rsc2", isColocation);
        when(clusterStatus.getRscSetConnections())
                .thenReturn(Lists.newArrayList(newRscSetConnectionData));
        final ServiceInfo service1 = mock(ServiceInfo.class);
        final ServiceInfo service2 = mock(ServiceInfo.class);
        when(clusterBrowser.getServiceInfoFromCRMId("rsc1")).thenReturn(service1);
        when(clusterBrowser.getServiceInfoFromCRMId("rsc2")).thenReturn(service2);

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        verify(crmGraph).addOrder("constraintId1", service1, constraintPlaceHolder);
        verify(crmGraph).addOrder("constraintId1", constraintPlaceHolder, service2);
    }

    @Test
    public void colocationShouldBeAdded() {
        //given:
        final CrmXml.ColocationData colocationData = new CrmXml.ColocationData(
                "colId1", "service", "withService", "rscRole", "withRscRole", "score");
        final Map<String, List<CrmXml.ColocationData>> colocationRscMap = new HashMap<String, List<CrmXml.ColocationData>>() {{
            put("service", Lists.newArrayList(colocationData));

        }};
        when(clusterStatus.getColocationRscMap()).thenReturn(colocationRscMap);

        final ServiceInfo service = mock(ServiceInfo.class);
        final ServiceInfo withService = mock(ServiceInfo.class);
        when(clusterBrowser.getServiceInfoFromCRMId("service")).thenReturn(service);
        when(clusterBrowser.getServiceInfoFromCRMId("withService")).thenReturn(withService);

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        verify(crmGraph).addColocation("colId1", service, withService);
    }

    @Test
    public void orderShouldBeAdded() {
        //given:
        final CrmXml.OrderData orderData = new CrmXml.OrderData(
                "ordId1", "rscFirst", "rscThen", "score", "symmetrical", "firstAction", "thenAction");
        final Map<String, List<CrmXml.OrderData>> orderRscMap = new HashMap<String, List<CrmXml.OrderData>>() {{
            put("rscFirst", Lists.newArrayList(orderData));

        }};
        when(clusterStatus.getOrderRscMap()).thenReturn(orderRscMap);

        final ServiceInfo rscFirst = mock(ServiceInfo.class);
        final ServiceInfo rscThen = mock(ServiceInfo.class);
        when(rscFirst.getResourceAgent()).thenReturn(mock(ResourceAgent.class));

        when(clusterBrowser.getServiceInfoFromCRMId("rscFirst")).thenReturn(rscFirst);
        when(clusterBrowser.getServiceInfoFromCRMId("rscThen")).thenReturn(rscThen);

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        verify(crmGraph).addOrder("ordId1", rscFirst, rscThen);
    }

    @Test
    public void orderBetweenLinbitDrbdAndFilesystemShouldBeAdded() {
        //given:
        final CrmXml.OrderData orderData = new CrmXml.OrderData(
                "ordId1", "rscFirst", "rscThen", "score", "symmetrical", "firstAction", "thenAction");
        final CrmXml.ColocationData colocationData = new CrmXml.ColocationData(
                "colId1", "rscFirst", "rscThen", "rscRole", "withRscRole", "score");
        final Map<String, List<CrmXml.OrderData>> orderRscMap = new HashMap<String, List<CrmXml.OrderData>>() {{
            put("rscFirst", Lists.newArrayList(orderData));

        }};
        final LinbitDrbdInfo rscFirst = mock(LinbitDrbdInfo.class);
        final FilesystemRaInfo rscThen = mock(FilesystemRaInfo.class);

        when(clusterStatus.getOrderRscMap()).thenReturn(orderRscMap);
        when(clusterStatus.getColocationDatas("rscFirst")).thenReturn(Lists.newArrayList(colocationData));

        final ResourceAgent resourceAgent = mock(ResourceAgent.class);
        when(resourceAgent.isLinbitDrbd()).thenReturn(true);
        when(rscFirst.getResourceAgent()).thenReturn(resourceAgent);
        when(rscThen.getName()).thenReturn("Filesystem");

        when(clusterBrowser.getServiceInfoFromCRMId("rscFirst")).thenReturn(rscFirst);
        when(clusterBrowser.getServiceInfoFromCRMId("rscThen")).thenReturn(rscThen);

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        verify(crmGraph).addOrder("ordId1", rscFirst, rscThen);
        verify(rscThen).setLinbitDrbdInfo(rscFirst);
    }

    @Test
    public void orderBetweenDrbddiskAndFilesystemShouldBeAdded() {
        //given:
        final CrmXml.OrderData orderData = new CrmXml.OrderData(
                "ordId1", "rscFirst", "rscThen", "score", "symmetrical", "firstAction", "thenAction");
        final CrmXml.ColocationData colocationData = new CrmXml.ColocationData(
                "colId1", "rscFirst", "rscThen", "rscRole", "withRscRole", "score");
        final Map<String, List<CrmXml.OrderData>> orderRscMap = new HashMap<String, List<CrmXml.OrderData>>() {{
            put("rscFirst", Lists.newArrayList(orderData));

        }};
        final DrbddiskInfo rscFirst = mock(DrbddiskInfo.class);
        final FilesystemRaInfo rscThen = mock(FilesystemRaInfo.class);

        when(clusterStatus.getOrderRscMap()).thenReturn(orderRscMap);
        when(clusterStatus.getColocationDatas("rscFirst")).thenReturn(Lists.newArrayList(colocationData));

        final ResourceAgent resourceAgent = mock(ResourceAgent.class);
        when(resourceAgent.isDrbddisk()).thenReturn(true);
        when(rscFirst.getResourceAgent()).thenReturn(resourceAgent);
        when(rscThen.getName()).thenReturn("Filesystem");

        when(clusterBrowser.getServiceInfoFromCRMId("rscFirst")).thenReturn(rscFirst);
        when(clusterBrowser.getServiceInfoFromCRMId("rscThen")).thenReturn(rscThen);

        //when:
        resourceUpdater.updateAllResources(servicesInfo, clusterBrowser, clusterStatus, RUN_MODE);

        //then:
        verify(crmGraph).addOrder("ordId1", rscFirst, rscThen);
        verify(rscThen).setDrbddiskInfo(rscFirst);
    }

    private RscSetConnectionData createRscSetConnectionData(final boolean isColocation) {
        return createRscSetConnectionData("rsc1", "rsc2", isColocation);
    }
    private RscSetConnectionData createRscSetConnectionData(final String rsc1,
                                                            final String rsc2,
                                                            final boolean isColocation) {
        final CrmXml.RscSet rscSet1 = new CrmXml.RscSet(
                "id1",
                Lists.newArrayList(rsc1),
                null, null, null, null);
        final CrmXml.RscSet rscSet2 = new CrmXml.RscSet(
                "id2",
                Lists.newArrayList(rsc2),
                null, null, null, null);
        final RscSetConnectionData rscSetConnectionData = new RscSetConnectionData(
                rscSet1,
                rscSet2,
                "constraintId1",
                0,
                isColocation);
        return rscSetConnectionData;
    }
}