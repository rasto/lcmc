/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009-2010, Rasto Levrinc
 * Copyright (C) 2009-2010, LINBIT HA-Solutions GmbH.
 *
 * DRBD Management Console is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * DRBD Management Console is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package lcmc.crm.ui.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import lcmc.common.domain.Application;
import lcmc.crm.domain.CrmXml;
import lcmc.host.domain.Host;
import lcmc.common.ui.Browser;
import lcmc.common.ui.SpringUtilities;
import lcmc.cluster.ui.widget.Check;
import lcmc.crm.service.CRM;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.utils.UpdatableItem;

/**
 * This class describes a connection between two heartbeat services.
 * It can be order, colocation or both.
 */
@Named
final class PcmkRscSetsInfo extends HbConnectionInfo {
    private final Collection<ConstraintPHInfo> constraintPHInfos = new LinkedHashSet<ConstraintPHInfo>();
    private final Lock mConstraintPHLock = new ReentrantLock();
    @Inject
    private Application application;

    void init(final Browser browser, final ConstraintPHInfo cphi) {
        super.init(browser);
        mConstraintPHLock.lock();
        try {
            constraintPHInfos.add(cphi);
        } finally {
            mConstraintPHLock.unlock();
        }
    }

    /** Adds a new rsc set colocation. */
    void addColocation(final String colId, final ConstraintPHInfo cphi) {
        mConstraintPHLock.lock();
        try {
            constraintPHInfos.add(cphi);
        } finally {
            mConstraintPHLock.unlock();
        }
        addColocation(colId, null, null);
    }

    /** Adds a new rsc set order. */
    void addOrder(final String ordId, final ConstraintPHInfo cphi) {
        mConstraintPHLock.lock();
        try {
            constraintPHInfos.add(cphi);
        } finally {
            mConstraintPHLock.unlock();
        }
        addOrder(ordId, null, null);
    }

    JComponent getInfoPanel(final ConstraintPHInfo constraintPHInfo) {
        return super.getInfoPanel();
    }

    /** Returns panel with user visible info. */
    @Override
    protected JPanel getLabels(final HbConstraintInterface c) {
        final JPanel panel = getParamPanel(c.getName());
        panel.setLayout(new SpringLayout());
        final int height = application.getDefaultSize("Browser.LabelFieldHeight");
        c.addLabelField(panel,
                        Tools.getString("ClusterBrowser.HeartbeatId"),
                        c.getService().getCrmId(),
                        application.getServiceLabelWidth(),
                        application.getServiceFieldWidth(),
                        height);
        final int rows = 1;
        SpringUtilities.makeCompactGrid(panel, rows, 2, /* rows, cols */
                                        1, 1,        /* initX, initY */
                                        1, 1);       /* xPad, yPad */
        return panel;
    }

    /** Applies changes to the placeholders. Called from one connection to a
     * placeholder. */
    public Map<CrmXml.RscSet, Map<String, String>> getAllAttributes(
                                        final Host dcHost,
                                        final CrmXml.RscSet appliedRscSet,
                                        final Map<String, String> appliedAttrs,
                                        final boolean isColocation,
                                        final Application.RunMode runMode) {
        final Map<CrmXml.RscSet, Map<String, String>> rscSetsAttrs =
                                                           new LinkedHashMap<CrmXml.RscSet, Map<String, String>>();
        final List<ConstraintPHInfo> allCphis = getAllConstrainPHInfos();
        if (isColocation) {
            for (final ConstraintPHInfo cphi : allCphis) {
                for (final Boolean first : new Boolean[]{false, true}) {
                    cphi.getAttributes(isColocation, first, rscSetsAttrs);
                }
            }
        } else {
            for (int i = allCphis.size() - 1; i >= 0; i--) {
                for (final Boolean first : new Boolean[]{true, false}) {
                    allCphis.get(i).getAttributes(isColocation, first, rscSetsAttrs);
                }
            }
        }
        rscSetsAttrs.put(appliedRscSet, appliedAttrs);
        return rscSetsAttrs;
    }

    private List<ConstraintPHInfo> getAllConstrainPHInfos() {
        final Map<String, ServiceInfo> idToInfoHash = getBrowser().getNameToServiceInfoHash(ConstraintPHInfo.NAME);
        final List<ConstraintPHInfo> allCphis = new ArrayList<ConstraintPHInfo>();
        if (idToInfoHash != null) {
            for (final Map.Entry<String, ServiceInfo> phEntry : idToInfoHash.entrySet()) {
                final ConstraintPHInfo cphi = (ConstraintPHInfo) phEntry.getValue();
                allCphis.add(cphi);
            }
        }
        return allCphis;
    }

    @Override
    void apply(final Host dcHost, final Application.RunMode runMode) {
        super.apply(dcHost, runMode);
        final List<ConstraintPHInfo> allCphis = getAllConstrainPHInfos();
        mConstraintPHLock.lock();
        final Map<ServiceInfo, ServiceInfo> parentToChild = new HashMap<ServiceInfo, ServiceInfo>();
        for (final ConstraintPHInfo cphi : constraintPHInfos) {
            final Set<ServiceInfo> cphiParents = getBrowser().getCrmGraph().getParents(cphi);
            boolean startComparing = false;
            for (final ConstraintPHInfo withCphi : allCphis) {
                if (cphi == withCphi) {
                    startComparing = true;
                    continue;
                }
                if (!startComparing) {
                    continue;
                }
                final Set<ServiceInfo> withCphiChildren = getBrowser().getCrmGraph().getChildren(withCphi);
                if (Tools.serviceInfoListEquals(cphiParents, withCphiChildren)) {
                    parentToChild.put(cphi, withCphi);
                    break;
                }
            }
        }
        final Collection<CrmXml.RscSet> rscSetsCol = new ArrayList<CrmXml.RscSet>();
        final List<CrmXml.RscSet> rscSetsOrd = new ArrayList<CrmXml.RscSet>();
        for (final ConstraintPHInfo cphi : constraintPHInfos) {
            if (cphi.getService().isNew()) {
                //cphi.apply(dcHost, runMode);
                final List<CrmXml.RscSet> sets = cphi.addConstraintWithPlaceholder(
                                    getBrowser().getCrmGraph().getChildrenAndParents(cphi),
                                    getBrowser().getCrmGraph().getParents(cphi),
                                    true, /* colocation */
                                    true, /* order */
                                    dcHost,
                                    false,
                                    runMode);
                rscSetsCol.add(sets.get(0)); /* col1 */
                rscSetsOrd.add(0, sets.get(3)); /* ord2 */
                ConstraintPHInfo parent = cphi;
                if (parentToChild.containsKey(parent)) {
                    List<CrmXml.RscSet> childSets = null;
                    while (parentToChild.containsKey(parent)) {
                        final ConstraintPHInfo child = (ConstraintPHInfo) parentToChild.get(parent);
                        if (child.getService().isNew()) {
                            //child.apply(dcHost, runMode);
                            childSets =
                             child.addConstraintWithPlaceholder(
                                  getBrowser().getCrmGraph().getChildrenAndParents(child),
                                  getBrowser().getCrmGraph().getParents(child),
                                  true, /* colocation */
                                  true, /* order */
                                  dcHost,
                                  false,
                                  runMode);
                            rscSetsCol.add(childSets.get(0)); /* col1 */
                            rscSetsOrd.add(0, childSets.get(3)); /* ord2 */
                            //if (Application.isLive(runMode)) {
                            //    child.getService().setNew(false);
                            //}
                        }
                        parent = child;
                    }
                    if (childSets != null) {
                        rscSetsCol.add(childSets.get(1)); /* col2 */
                        rscSetsOrd.add(0, childSets.get(2)); /* ord1 */
                    }
                } else {
                    rscSetsCol.add(sets.get(1)); /* col2 */
                    rscSetsOrd.add(0, sets.get(2)); /* ord2 */
                }
            }
        }
        mConstraintPHLock.unlock();
        final Map<String, String> attrs = new LinkedHashMap<String, String>();
        attrs.put(CrmXml.SCORE_CONSTRAINT_PARAM, CrmXml.INFINITY_VALUE.getValueForConfig());
        String colId = null;
        final Map<CrmXml.RscSet, Map<String, String>> rscSetsColAttrs =
                                                         new LinkedHashMap<CrmXml.RscSet, Map<String, String>>();
        for (final CrmXml.RscSet colSet : rscSetsCol) {
            if (colId == null && colSet != null) {
                colId = colSet.getId();
            }
            rscSetsColAttrs.put(colSet, null);
        }
        final Map<CrmXml.RscSet, Map<String, String>> rscSetsOrdAttrs =
                                                         new LinkedHashMap<CrmXml.RscSet, Map<String, String>>();
        String ordId = null;
        for (final CrmXml.RscSet ordSet : rscSetsOrd) {
            if (ordId == null && ordSet != null) {
                ordId = ordSet.getId();
            }
            rscSetsOrdAttrs.put(ordSet, null);
        }
        final boolean createCol = true;
        final boolean createOrd = true;
        CRM.setRscSet(dcHost, colId, createCol, ordId, createOrd, rscSetsColAttrs, rscSetsOrdAttrs, attrs, runMode);
    }

    @Override
    public Check checkResourceFields(final String param, final String[] params) {
        final List<String> incorrect = new ArrayList<String>();
        final List<String> changed = new ArrayList<String>();
        mConstraintPHLock.lock();
        try {
            for (final ConstraintPHInfo cphi : constraintPHInfos) {
                if (cphi.getService().isNew()
                    && !getBrowser().getCrmGraph().getChildrenAndParents(cphi).isEmpty()) {
                    changed.add("new placeholder");
                }
            }
        } finally {
            mConstraintPHLock.unlock();
        }
        final Check check = new Check(incorrect, changed);
        check.addCheck(super.checkResourceFields(param, params));
        return check;
    }

    /** Return list of popup items. */
    @Override
    public List<UpdatableItem> createPopup() {
        // TODO: make submenus for all cphis
        return super.createPopup();
    }
}
