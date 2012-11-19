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
package lcmc.gui.resources;

import lcmc.gui.Browser;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.SpringUtilities;
import lcmc.data.Host;
import lcmc.data.CRMXML;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.CRM;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class describes a connection between two heartbeat services.
 * It can be order, colocation or both.
 */
final class PcmkRscSetsInfo extends HbConnectionInfo {
    /** Placeholders. */
    private final Set<ConstraintPHInfo> constraintPHInfos =
                                          new LinkedHashSet<ConstraintPHInfo>();
    /** constraints lock. */
    private final Lock mConstraintPHLock = new ReentrantLock();

    /** Prepares a new <code>PcmkRscSetsInfo</code> object. */
    PcmkRscSetsInfo(final Browser browser) {
        super(browser);
    }

    /** Prepares a new <code>PcmkRscSetsInfo</code> object. */
    PcmkRscSetsInfo(final Browser browser, final ConstraintPHInfo cphi) {
        this(browser);
        mConstraintPHLock.lock();
        try {
            constraintPHInfos.add(cphi);
        } finally {
            mConstraintPHLock.unlock();
        }
    }

    /** Adds a new rsc set colocation. */
    void addColocation(final String colId,
                                 final ConstraintPHInfo cphi) {
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

    /** Returns info panel. */
    JComponent getInfoPanel(final ConstraintPHInfo constraintPHInfo) {
        return super.getInfoPanel();
    }

    /** Returns panal with user visible info. */
    @Override
    protected JPanel getLabels(final HbConstraintInterface c) {
        final JPanel panel = getParamPanel(c.getName());
        panel.setLayout(new SpringLayout());
        final int rows = 1;
        final int height = Tools.getDefaultSize("Browser.LabelFieldHeight");
        c.addLabelField(panel,
                        Tools.getString("ClusterBrowser.HeartbeatId"),
                        c.getService().getHeartbeatId(),
                        ClusterBrowser.SERVICE_LABEL_WIDTH,
                        ClusterBrowser.SERVICE_FIELD_WIDTH,
                        height);
        SpringUtilities.makeCompactGrid(panel, rows, 2, /* rows, cols */
                                        1, 1,        /* initX, initY */
                                        1, 1);       /* xPad, yPad */
        return panel;
    }

    /** Applies changes to the placeholders. Called from one connection to a
     * placeholder. */
    public Map<CRMXML.RscSet, Map<String, String>> getAllAttributes(
                                        final Host dcHost,
                                        final CRMXML.RscSet appliedRscSet,
                                        final Map<String, String> appliedAttrs,
                                        final boolean isColocation,
                                        final boolean testOnly) {
        final Map<CRMXML.RscSet, Map<String, String>> rscSetsAttrs =
                       new LinkedHashMap<CRMXML.RscSet, Map<String, String>>();
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
                    allCphis.get(i).getAttributes(isColocation,
                                                  first,
                                                  rscSetsAttrs);
                }
            }
        }
        rscSetsAttrs.put(appliedRscSet, appliedAttrs);
        return rscSetsAttrs;
    }

    /** Returns copy of all constraint placeholders. */
    private List<ConstraintPHInfo> getAllConstrainPHInfos() {
        final Map<String, ServiceInfo> idToInfoHash =
             getBrowser().getNameToServiceInfoHash(ConstraintPHInfo.NAME);
        final List<ConstraintPHInfo> allCphis =
                                            new ArrayList<ConstraintPHInfo>();
        if (idToInfoHash != null) {
            for (final String id : idToInfoHash.keySet()) {
                final ConstraintPHInfo cphi =
                                   (ConstraintPHInfo) idToInfoHash.get(id);
                allCphis.add(cphi);
            }
        }
        return allCphis;
    }

    /** Applies changes to the placeholders. */
    @Override
    void apply(final Host dcHost, final boolean testOnly) {
        super.apply(dcHost, testOnly);
        final Map<String, ServiceInfo> idToInfoHash =
             getBrowser().getNameToServiceInfoHash(ConstraintPHInfo.NAME);
        final List<ConstraintPHInfo> allCphis = getAllConstrainPHInfos();
        mConstraintPHLock.lock();
        final Map<ServiceInfo, ServiceInfo> parentToChild =
                            new HashMap<ServiceInfo, ServiceInfo>();
        for (final ConstraintPHInfo cphi : constraintPHInfos) {
            final Set<ServiceInfo> cphiParents =
                                    getBrowser().getCRMGraph().getParents(cphi);
            boolean startComparing = false;
            for (final ConstraintPHInfo withCphi : allCphis) {
                if (cphi == withCphi) {
                    startComparing = true;
                    continue;
                }
                if (!startComparing) {
                    continue;
                }
                final Set<ServiceInfo> withCphiChildren =
                             getBrowser().getCRMGraph().getChildren(withCphi);
                if (Tools.serviceInfoListEquals(cphiParents,
                                                withCphiChildren)) {
                    parentToChild.put(cphi, withCphi);
                    break;
                }
            }
        }
        final List<CRMXML.RscSet> rscSetsCol = new ArrayList<CRMXML.RscSet>();
        final List<CRMXML.RscSet> rscSetsOrd = new ArrayList<CRMXML.RscSet>();
        for (final ConstraintPHInfo cphi : constraintPHInfos) {
            if (cphi.getService().isNew()) {
                //cphi.apply(dcHost, testOnly);
                final List<CRMXML.RscSet> sets =
                 cphi.addConstraintWithPlaceholder(
                        getBrowser().getCRMGraph().getChildrenAndParents(cphi),
                        getBrowser().getCRMGraph().getParents(cphi),
                        true, /* colocation */
                        true, /* order */
                        dcHost,
                        false,
                        testOnly);
                rscSetsCol.add(sets.get(0)); /* col1 */
                rscSetsOrd.add(0, sets.get(3)); /* ord2 */
                ConstraintPHInfo parent = cphi;
                if (parentToChild.containsKey((ServiceInfo) parent)) {
                    List<CRMXML.RscSet> childSets = null;
                    while (parentToChild.containsKey((ServiceInfo) parent)) {
                        final ConstraintPHInfo child =
                            (ConstraintPHInfo) parentToChild.get((
                                                        ServiceInfo) parent);
                        if (child.getService().isNew()) {
                            //child.apply(dcHost, testOnly);
                            childSets =
                             child.addConstraintWithPlaceholder(
                                  getBrowser().getCRMGraph()
                                              .getChildrenAndParents(child),
                                  getBrowser().getCRMGraph().getParents(child),
                                  true, /* colocation */
                                  true, /* order */
                                  dcHost,
                                  false,
                                  testOnly);
                            rscSetsCol.add(childSets.get(0)); /* col1 */
                            rscSetsOrd.add(0, childSets.get(3)); /* ord2 */
                            //if (!testOnly) {
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
        final Map<String, String> attrs =
                                      new LinkedHashMap<String, String>();
        attrs.put(CRMXML.SCORE_STRING, CRMXML.INFINITY_STRING);
        String colId = null;
        String ordId = null;
        final Map<CRMXML.RscSet, Map<String, String>> rscSetsColAttrs =
                       new LinkedHashMap<CRMXML.RscSet, Map<String, String>>();
        for (final CRMXML.RscSet colSet : rscSetsCol) {
            if (colId == null && colSet != null) {
                colId = colSet.getId();
            }
            rscSetsColAttrs.put(colSet, null);
        }
        final Map<CRMXML.RscSet, Map<String, String>> rscSetsOrdAttrs =
                       new LinkedHashMap<CRMXML.RscSet, Map<String, String>>();
        for (final CRMXML.RscSet ordSet : rscSetsOrd) {
            if (ordId == null && ordSet != null) {
                ordId = ordSet.getId();
            }
            rscSetsOrdAttrs.put(ordSet, null);
        }
        final boolean createCol = true;
        final boolean createOrd = true;
        CRM.setRscSet(dcHost,
                      colId,
                      createCol,
                      ordId,
                      createOrd,
                      rscSetsColAttrs,
                      rscSetsOrdAttrs,
                      attrs,
                      testOnly);
    }

    /** Check order and colocation constraints. */
    @Override
    public boolean checkResourceFieldsChanged(final String param,
                                              final String[] params) {
        boolean oneIsNew = false;
        mConstraintPHLock.lock();
        try {
            for (final ConstraintPHInfo cphi : constraintPHInfos) {
                if (cphi.getService().isNew()
                    && !getBrowser().getCRMGraph().getChildrenAndParents(
                                                             cphi).isEmpty()) {
                    oneIsNew = true;
                }
            }
        } finally {
            mConstraintPHLock.unlock();
        }
        return super.checkResourceFieldsChanged(param, params) || oneIsNew;
    }

    /** Return list of popup items. */
    @Override
    public List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = super.createPopup();
        // TODO: make submenus for all cphis
        return items;
    }
}
