/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2016, Rastislav Levrinc.
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

package lcmc.crm.ui.resource.update;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Provider;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.domain.Application;
import lcmc.crm.domain.CrmXml;
import lcmc.crm.domain.RscSetConnectionData;
import lcmc.crm.ui.CrmGraph;
import lcmc.crm.ui.resource.ConstraintPHInfo;
import lcmc.crm.ui.resource.PcmkRscSetsInfo;
import lcmc.crm.ui.resource.ServiceInfo;
import lombok.Getter;

public class RscSetUpdater {
    private final Application.RunMode runMode;
    private final ClusterBrowser browser;
    private final Provider<ConstraintPHInfo> constraintPHInfoProvider;
    private final Provider<PcmkRscSetsInfo> pcmkRscSetsInfoProvider;
    private final CrmGraph crmGraph;
    @Getter
    private final List<ServiceInfo> serviceIsPresent = Lists.newArrayList();
    private final Map<RscSetConnectionData, ConstraintPHInfo> oldResourceSetToPlaceholder = Maps.newLinkedHashMap();
    private final List<ConstraintPHInfo> preNewCphis = Lists.newArrayList();
    private final Collection<ConstraintPHInfo> newCphis = Lists.newArrayList();

    public RscSetUpdater(final Application.RunMode runMode, final ClusterBrowser browser,
            final Provider<ConstraintPHInfo> constraintPHInfoProvider, final Provider<PcmkRscSetsInfo> pcmkRscSetsInfoProvider) {
        this.runMode = runMode;
        this.browser = browser;
        this.constraintPHInfoProvider = constraintPHInfoProvider;
        this.pcmkRscSetsInfoProvider = pcmkRscSetsInfoProvider;
        crmGraph = browser.getCrmGraph();
    }

    public void update(final List<RscSetConnectionData> newRscSetConnections) {
        findExistingPlaceholders();
        for (final RscSetConnectionData newRscSetConnectionData : newRscSetConnections) {
            updateRscConnection(newRscSetConnectionData);
        }

        addNewPlaceholders();
    }

    private void updateRscConnection(RscSetConnectionData newRscSetConnectionData) {
        Optional<ConstraintPHInfo> constraintPHInfo = updateExistingResourceSet(newRscSetConnectionData);

        if (constraintPHInfo.isEmpty()) {
            constraintPHInfo = addConstraintsToExistingPlaceholder(newRscSetConnectionData);
        }

        PcmkRscSetsInfo rscSetsInfo = null;

        if (constraintPHInfo.isPresent()) {
            rscSetsInfo = constraintPHInfo.get().getPcmkRscSetsInfo();
        }

        if (constraintPHInfo.isEmpty() && !preNewCphis.isEmpty()) {
            constraintPHInfo = Optional.of(setOldPlaceholder(newRscSetConnectionData));
        }

        if (constraintPHInfo.isEmpty()) {
            constraintPHInfo = Optional.of(createNewPlaceholder(newRscSetConnectionData, rscSetsInfo));
        }

        addPlaceholder(newRscSetConnectionData, constraintPHInfo.get());
    }

    private void addPlaceholder(final RscSetConnectionData newRscSetConnectionData,
                                final ConstraintPHInfo constraintPHInfo) {
        serviceIsPresent.add(constraintPHInfo);

        if (newRscSetConnectionData.isColocation()) {
            addColocation(newRscSetConnectionData, constraintPHInfo);
        } else {
            addOrder(newRscSetConnectionData, constraintPHInfo);
        }
        if (Application.isLive(runMode)) {
            constraintPHInfo.setUpdated(false);
            constraintPHInfo.setNew(false);
        }
    }

    private Optional<ConstraintPHInfo> updateExistingResourceSet(RscSetConnectionData newRscSetConnectionData) {
        for (final Map.Entry<RscSetConnectionData, ConstraintPHInfo> phEntry : oldResourceSetToPlaceholder.entrySet()) {
            final RscSetConnectionData oldRscSetConnectionData = phEntry.getKey();
            final ConstraintPHInfo placeholder = phEntry.getValue();

            if (oldRscSetConnectionData == newRscSetConnectionData) {
                continue;
            }
            if (newRscSetConnectionData.equals(oldRscSetConnectionData) || newRscSetConnectionData.equalsAlthoughReversed(oldRscSetConnectionData)) {
                placeholder.setRscSetConnectionData(newRscSetConnectionData);
                return Optional.of(placeholder);
            }
        }
        return Optional.empty();
    }

    private ConstraintPHInfo setOldPlaceholder(RscSetConnectionData newRscSetConnectionData) {
        ConstraintPHInfo constraintPHInfo;
        constraintPHInfo = preNewCphis.remove(0);
        oldResourceSetToPlaceholder.put(newRscSetConnectionData, constraintPHInfo);
        constraintPHInfo.setRscSetConnectionData(newRscSetConnectionData);
        return constraintPHInfo;
    }

    private ConstraintPHInfo createNewPlaceholder(RscSetConnectionData newRscSetConnectionData, PcmkRscSetsInfo rscSetsInfo) {
        ConstraintPHInfo constraintPHInfo;
        constraintPHInfo = constraintPHInfoProvider.get();
        constraintPHInfo.init(browser, newRscSetConnectionData, ConstraintPHInfo.Preference.AND);
        if (rscSetsInfo == null) {
            rscSetsInfo = pcmkRscSetsInfoProvider.get();
            rscSetsInfo.init(browser);
        }
        if (newRscSetConnectionData.isColocation()) {
            rscSetsInfo.addColocation(newRscSetConnectionData.getConstraintId(), constraintPHInfo);
        } else {
            rscSetsInfo.addOrder(newRscSetConnectionData.getConstraintId(), constraintPHInfo);
        }
        constraintPHInfo.setPcmkRscSetsInfo(rscSetsInfo);
        browser.addNameToServiceInfoHash(constraintPHInfo);
        newCphis.add(constraintPHInfo); /* have to add it later,
                           so that ids are correct. */
        oldResourceSetToPlaceholder.put(newRscSetConnectionData, constraintPHInfo);
        return constraintPHInfo;
    }


    private Optional<ConstraintPHInfo> addConstraintsToExistingPlaceholder(RscSetConnectionData newRscSetConnectionData) {
        for (final Map.Entry<RscSetConnectionData, ConstraintPHInfo> phEntry : oldResourceSetToPlaceholder.entrySet()) {
            final RscSetConnectionData oldRrcSetConnectionData = phEntry.getKey();
            final ConstraintPHInfo placeholder = phEntry.getValue();
            if (oldRrcSetConnectionData == newRscSetConnectionData) {
                return Optional.of(placeholder);
            }
            if (canReusePlaceholder(newRscSetConnectionData, oldRrcSetConnectionData, placeholder)) {
                return Optional.of(addContraintsToPlaceholder(newRscSetConnectionData, placeholder));
            }
        }
        return Optional.empty();
    }

    private ConstraintPHInfo addContraintsToPlaceholder(RscSetConnectionData newRscSetConnectionData, ConstraintPHInfo placeholder) {
        ConstraintPHInfo constraintPHInfo;
        constraintPHInfo = placeholder;
        constraintPHInfo.setRscSetConnectionData(newRscSetConnectionData);
        final PcmkRscSetsInfo rscSetsInfo = constraintPHInfo.getPcmkRscSetsInfo();
        if (rscSetsInfo != null) {
            if (newRscSetConnectionData.isColocation()) {
                rscSetsInfo.addColocation(newRscSetConnectionData.getConstraintId(), constraintPHInfo);
            } else {
                rscSetsInfo.addOrder(newRscSetConnectionData.getConstraintId(), constraintPHInfo);
            }
        }
        return constraintPHInfo;
    }

    private boolean canReusePlaceholder(final RscSetConnectionData newRscSetConnectionData,
                                        final RscSetConnectionData oldRrcSetConnectionData,
                                        final ConstraintPHInfo placeholder) {
        return placeholder.isNew()
                || (newRscSetConnectionData.canUseSamePlaceholder(oldRrcSetConnectionData)
                && placeholder.sameConstraintId(newRscSetConnectionData));
    }

    private void addColocation(final RscSetConnectionData newRscSetConnectionData,
                               final ConstraintPHInfo constraintPHInfo) {
        final CrmXml.RscSet rscSet1 = newRscSetConnectionData.getRscSet1();
        final CrmXml.RscSet rscSet2 = newRscSetConnectionData.getRscSet2();
        if (rscSet1 != null) {
            addColocationsFromPlaceholder(newRscSetConnectionData, constraintPHInfo, rscSet1);
        }
        if (rscSet2 != null) {
            addColocationsToPlaceholder(newRscSetConnectionData, constraintPHInfo, rscSet2);
        }
    }

    private void addColocationsToPlaceholder(final RscSetConnectionData newRscSetConnectionData,
                                             final ConstraintPHInfo constraintPHInfo,
                                             final CrmXml.RscSet rscSet2) {
        for (final String rscId : rscSet2.getRscIds()) {
            final ServiceInfo si = browser.getServiceInfoFromCRMId(rscId);
            crmGraph.addColocation(newRscSetConnectionData.getConstraintId(), si, constraintPHInfo);
        }
    }

    private void addColocationsFromPlaceholder(final RscSetConnectionData newRscSetConnectionData,
                                               final ConstraintPHInfo constraintPHInfo,
                                               final CrmXml.RscSet rscSet1) {
        for (final String rscId : rscSet1.getRscIds()) {
            final ServiceInfo si =
                    browser.getServiceInfoFromCRMId(rscId);
            crmGraph.addColocation(newRscSetConnectionData.getConstraintId(), constraintPHInfo, si);
        }
    }

    private void addOrder(RscSetConnectionData newRscSetConnectionData, ConstraintPHInfo constraintPHInfo) {
        final CrmXml.RscSet rscSet1 = newRscSetConnectionData.getRscSet1();
        final CrmXml.RscSet rscSet2 = newRscSetConnectionData.getRscSet2();
        if (rscSet1 != null) {
            addRscSetOrdersToPlaceholder(newRscSetConnectionData, rscSet1, constraintPHInfo);
        }
        if (rscSet2 != null) {
            addRscSetOrdersFromPlaceholder(newRscSetConnectionData, constraintPHInfo, rscSet2);
        }
    }

    private void addRscSetOrdersToPlaceholder(RscSetConnectionData newRscSetConnectionData, CrmXml.RscSet rscSet1, ConstraintPHInfo constraintPHInfo) {
        for (final String rscId : rscSet1.getRscIds()) {
            final ServiceInfo si = browser.getServiceInfoFromCRMId(rscId);
            crmGraph.addOrder(newRscSetConnectionData.getConstraintId(), si, constraintPHInfo);
        }
    }

    private void addRscSetOrdersFromPlaceholder(RscSetConnectionData newRscSetConnectionData, ConstraintPHInfo constraintPHInfo, CrmXml.RscSet rscSet2) {
        for (final String rscId : rscSet2.getRscIds()) {
            final ServiceInfo si = browser.getServiceInfoFromCRMId(rscId);
            crmGraph.addOrder(newRscSetConnectionData.getConstraintId(), constraintPHInfo, si);
        }
    }


    private void findExistingPlaceholders() {
        browser.lockNameToServiceInfo();
        final Map<String, ServiceInfo> oldIdToInfoHash = browser.getNameToServiceInfoHash(ConstraintPHInfo.NAME);
        if (oldIdToInfoHash != null) {
            for (final Map.Entry<String, ServiceInfo> infoEntry : oldIdToInfoHash.entrySet()) {
                final ConstraintPHInfo cphi = (ConstraintPHInfo) infoEntry.getValue();
                final RscSetConnectionData rdataOrd = cphi.getRscSetConnectionDataOrder();
                final RscSetConnectionData rdataCol = cphi.getRscSetConnectionDataColocation();
                if (cphi.isNew()) {
                    preNewCphis.add(cphi);
                }
                if (rdataOrd != null && !rdataOrd.isEmpty()) {
                    oldResourceSetToPlaceholder.put(rdataOrd, cphi);
                }
                if (rdataCol != null && !rdataCol.isEmpty()) {
                    oldResourceSetToPlaceholder.put(rdataCol, cphi);
                }
            }
        }
        browser.unlockNameToServiceInfo();
    }

    private void addNewPlaceholders() {
        for (final ConstraintPHInfo cphi : newCphis) {
            crmGraph.addConstraintPlaceholder(cphi, null /* pos */, Application.RunMode.LIVE);
        }
    }
}
