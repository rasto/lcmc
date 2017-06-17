/*
 * This file is part of Linux Cluster Management Console
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2012, Rastislav Levrinc
 *
 * LCMC is free software; you can redistribute it and/or
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
package lcmc.crm.ui;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.picking.PickedState;
import edu.uci.ics.jung.visualization.util.VertexShapeFactory;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.ColorText;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Info;
import lcmc.common.ui.MainPanel;
import lcmc.common.ui.ResourceGraph;
import lcmc.common.ui.utils.MenuAction;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyMenuItem;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.crm.ui.resource.ConstraintPHInfo;
import lcmc.crm.ui.resource.GroupInfo;
import lcmc.crm.ui.resource.HbConnectionInfo;
import lcmc.crm.ui.resource.HostInfo;
import lcmc.crm.ui.resource.PcmkMultiSelectionInfo;
import lcmc.crm.ui.resource.ServiceInfo;
import lcmc.host.domain.Host;
import lcmc.host.ui.HostBrowser;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class creates graph and provides methods to add new nodes, edges,
 * remove or modify them.
 */
@Named
public class CrmGraph extends ResourceGraph {
    /** X position of a new block device. */

    private static final int BD_X_POS = 15;
    /** Y position of the host. */
    private static final int HOST_Y_POS = 40;
    /** Vertical step in pixels by which the hosts are drawn in the graph. */
    private static final int HOST_STEP_X = 230;
    /** Minimum vertical position. */
    private static final int MIN_Y_POS = 20;
    /** Maximum horizontal position. */
    private static final int MAX_X_POS = 2600;
    /** Maximum vertical position. */
    private static final int MAX_Y_POS = 2600;

    private static final int VERTEX_HEIGHT = 50;
    private static final ImageIcon HOST_STANDBY_ICON =
                                            Tools.createImageIcon(Tools.getDefault("CRMGraph.HostStandbyIcon"));
    private static final ImageIcon SERVICE_RUNNING_ICON =
                                       Tools.createImageIcon(Tools.getDefault("CRMGraph.ServiceRunningIcon"));
    private static final ImageIcon SERVICE_RUNNING_FAILED_ICON =
                                        Tools.createImageIcon(Tools.getDefault("CRMGraph.ServiceRunningFailedIcon"));
    /** Icon that indicates a started service (but not running). */
    private static final ImageIcon SERVICE_STARTED_ICON = Tools.createImageIcon(
                                                                Tools.getDefault("CRMGraph.ServiceStartedIcon"));
    /** Icon that indicates a stopping service (but not stopped). */
    private static final ImageIcon SERVICE_STOPPING_ICON = Tools.createImageIcon(
                                                                Tools.getDefault("CRMGraph.ServiceStoppingIcon"));
    /** Icon that indicates a not running service. */
    private static final ImageIcon SERVICE_STOPPED_ICON = Tools.createImageIcon(
                                                                Tools.getDefault("CRMGraph.ServiceStoppedIcon"));
    /** Icon that indicates a not running service that failed. */
    private static final ImageIcon SERVICE_STOPPED_FAILED_ICON =
                                        Tools.createImageIcon(Tools.getDefault("CRMGraph.ServiceStoppedFailedIcon"));
    /** Icon that indicates an unmanaged service. */
    private static final ImageIcon SERVICE_UNMANAGED_ICON =
                                     Tools.createImageIcon(Tools.getDefault("CRMGraph.ServiceUnmanagedIcon"));
    /** Icon that indicates a migrated service. */
    private static final ImageIcon SERVICE_MIGRATED_ICON =
                                     Tools.createImageIcon(Tools.getDefault("CRMGraph.ServiceMigratedIcon"));
    /** List with edges that are order constraints. */
    private final Collection<Edge> edgeIsOrderList = new HashSet<Edge>();
    /** List with edges that should be kept as order constraints. */
    private final Collection<Edge> keepEdgeIsOrderList = new HashSet<Edge>();
    /** List with edges that are colocation constraints. */
    private final Collection<Edge> edgeIsColocationList = new HashSet<Edge>();
    /** List with edges that should be kept as colocation constraints. */
    private final Collection<Edge> keepEdgeIsColocationList = new HashSet<Edge>();
    private final Lock mVertexIsPresentListLock = new ReentrantLock();
    private Set<Vertex> vertexIsPresentList = new HashSet<Vertex>();
    /** Map from vertex to 'Add service' menu. */
    private final Map<Vertex, JMenu> vertexToAddServiceMap = new HashMap<Vertex, JMenu>();
    /** Map from vertex to 'Add existing service' menu. */
    private final Map<Vertex, JMenu> vertexToAddExistingServiceMap = new HashMap<Vertex, JMenu>();
    /** Map from edge to the hb connection info of this constraint. */
    private final Map<Edge, HbConnectionInfo> edgeToHbconnectionMap = new LinkedHashMap<Edge, HbConnectionInfo>();
    /** Map from hb connection info to the edge. */
    private final Map<HbConnectionInfo, Edge> hbconnectionToEdgeMap = new LinkedHashMap<HbConnectionInfo, Edge>();
    private final ReadWriteLock mHbConnectionLock = new ReentrantReadWriteLock();
    private final Lock mHbConnectionReadLock = mHbConnectionLock.readLock();
    private final Lock mHbConnectionWriteLock = mHbConnectionLock.writeLock();
    private final Map<Vertex, HostInfo> vertexToHostMap = new LinkedHashMap<Vertex, HostInfo>();
    private final Map<Info, Vertex> hostToVertexMap = new LinkedHashMap<Info, Vertex>();
    /** Map from the vertex to the constraint placeholder. */
    private final Map<Vertex, ConstraintPHInfo> vertexToConstraintPHMap = new HashMap<Vertex, ConstraintPHInfo>();
    /** Map from the host to the vertex. */
    private final Map<Info, Vertex> constraintPHToVertexMap = new HashMap<Info, Vertex>();

    private int hostDefaultXPos = 10;
    private PcmkMultiSelectionInfo multiSelectionInfo = null;
    @Inject
    private MainPanel mainPanel;
    @Inject
    private Provider<PcmkMultiSelectionInfo> pcmkMultiSelectionInfoProvider;
    @Inject
    private SwingUtils swingUtils;
    @Inject
    private MenuFactory menuFactory;

    @Override
    public void initGraph(final ClusterBrowser clusterBrowser) {
        super.initGraph(clusterBrowser);
        super.initGraph(new DirectedSparseGraph<Vertex, Edge>());
    }

    /**
     * Returns true if vertex v is one of the ancestors or the same as
     * vertex p. The vertex and edge lists must be locked when called.
     */
    private boolean isAncestor(final Vertex v, final Vertex p, final List<Vertex> list) {
        if (p.equals(v)) {
            return true;
        }
        for (final Vertex pre : getGraph().getPredecessors(p)) {
            if (list.contains(pre)) {
                return false;
            }
            list.add(pre);
            if (isAncestor(v, pre, list)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if service exists already in the parent ancestor line
     * or parent already has this service as a child.
     */
    public boolean existsInThePath(final ServiceInfo si, final ServiceInfo parent) {
        if (parent == null) {
            return true;
        }
        if (si.equals(parent)) {
            return true;
        }
        lockGraph();
        final Vertex v = getVertex(si);
        if (v == null) {
            unlockGraph();
            return false;
        }
        final Vertex pv = getVertex(parent);
        if (pv == null) {
            unlockGraph();
            return false;
        }
        if (getGraph().isSuccessor(pv, v) || isAncestor(v, pv, new ArrayList<Vertex>())) {
            unlockGraph();
            return true;
        }
        unlockGraph();
        return false;
    }

    /** Returns heartbeat ids from this service info's parents. */
    public Set<ServiceInfo> getParents(final ServiceInfo si) {
        final Set<ServiceInfo> parents = new TreeSet<ServiceInfo>();
        final Vertex v = getVertex(si);
        if (v != null) {
            lockGraph();
            for (final Vertex pV : getGraph().getPredecessors(v)) {
                final ServiceInfo psi = (ServiceInfo) getInfo(pV);
                if (psi != null) {
                    parents.add(psi);
                }
            }
            unlockGraph();
        }
        return parents;
    }

    /** Returns children of the service. */
    public Set<ServiceInfo> getChildren(final ServiceInfo si) {
        final Set<ServiceInfo> children = new TreeSet<ServiceInfo>();
        final Vertex v = getVertex(si);
        if (v != null) {
            lockGraph();
            for (final Vertex pV : getGraph().getSuccessors(v)) {
                final ServiceInfo psi = (ServiceInfo) getInfo(pV);
                if (psi != null) {
                    children.add(psi);
                }
            }
            unlockGraph();
        }
        return children;
    }

    /** Returns children and parents of the service. */
    public Collection<ServiceInfo> getChildrenAndParents(final ServiceInfo si) {
        final Collection<ServiceInfo> chAndP = new TreeSet<ServiceInfo>();
        chAndP.addAll(getChildren(si));
        chAndP.addAll(getParents(si));
        return chAndP;
    }

    /** Returns all connections from this service. */
    public HbConnectionInfo[] getHbConnections(final ServiceInfo si) {
        final List<HbConnectionInfo> infos = new ArrayList<HbConnectionInfo>();
        final Vertex v = getVertex(si);
        if (v != null) {
            mHbConnectionReadLock.lock();
            try {
                lockGraph();
                final Collection<Vertex> predecessors = getGraph().getPredecessors(v);
                if (predecessors != null) {
                    for (final Vertex pV : predecessors) {
                        Edge edge = getGraph().findEdge(pV, v);
                        if (edge == null) {
                            edge = getGraph().findEdge(v, pV);
                        }
                        if (edge != null) {
                            final HbConnectionInfo hbci = edgeToHbconnectionMap.get(edge);
                            if (hbci != null) {
                                infos.add(hbci);
                            }
                        }
                    }
                }
                final Collection<Vertex> successors = getGraph().getPredecessors(v);
                if (successors != null) {
                    for (final Vertex sV : successors) {
                        Edge edge = getGraph().findEdge(v, sV);
                        if (edge == null) {
                            edge = getGraph().findEdge(sV, v);
                        }
                        if (edge != null) {
                            final HbConnectionInfo hbci = edgeToHbconnectionMap.get(edge);
                            if (hbci != null) {
                                infos.add(hbci);
                            }
                        }
                    }
                }
                unlockGraph();
            } finally {
                mHbConnectionReadLock.unlock();
            }
        }
        return infos.toArray(new HbConnectionInfo[infos.size()]);
    }

    /** Returns id that is used for saving of the vertex positions to a file. */
    @Override
    protected String getId(final Info i) {
        final String id = i.getId();
        if (id == null) {
            return null;
        }
        return "hb=" + i.getId();
    }

    /**
     * Exchanges object in vertex, e.g., wenn ra changes to/from m/s resource.
     */
    public void exchangeObjectInTheVertex(final ServiceInfo newSI, final ServiceInfo oldSI) {
        final Vertex v = getVertex(oldSI);
        removeVertex(oldSI);
        putInfoToVertex(newSI, v);
        putVertexToInfo(v, newSI);
        somethingChanged();
    }

    /**
     * adds resource/service to the graph. Adds also edge from the parent.
     * If vertex exists add only the edge. If parent is null add only vertex
     * without edge. Return true if vertex existed in the graph before.
     */
    public boolean addResource(final ServiceInfo serviceInfo,
                               final ServiceInfo parent,
                               final Point2D pos,
                               final boolean colocation,
                               final boolean order,
                               final Application.RunMode runMode) {
        boolean vertexExists = true;
        Vertex v = getVertex(serviceInfo);
        if (v == null) {
            v = new Vertex();
            if (pos == null) {
                final Point2D newPos = getSavedPosition(serviceInfo);
                if (newPos == null) {
                    final Point2D max = getLastPosition();
                    final float maxYPos = (float) max.getY();
                    getVertexLocations().put(v, new Point2D.Float(BD_X_POS, maxYPos + 40));
                    putVertexLocations();
                } else {
                    getVertexLocations().put(v, newPos);
                    putVertexLocations();
                }
            } else {
                final Point2D p = getVisualizationViewer().getRenderContext().getMultiLayerTransformer()
                                                                             .inverseTransform(Layer.VIEW, pos);
                getVertexLocations().put(v, posWithScrollbar(p));
                putVertexLocations();
            }

            lockGraph();
            getGraph().addVertex(v);
            unlockGraph();
            somethingChanged();
            putInfoToVertex(serviceInfo, v);
            putVertexToInfo(v, serviceInfo);
            vertexExists = false;
        } else if (Application.isTest(runMode)) {
            addTestEdge(getVertex(parent), getVertex(serviceInfo));
        }

        if (parent != null && Application.isLive(runMode)) {
            if (colocation) {
                addColocation(null, serviceInfo, parent);
            }
            if (order) {
                addOrder(null, parent, serviceInfo);
            }
        }
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                repaint();
            }
        });
        return vertexExists;
    }

    /** Adds order constraint from parent to the service. */
    public void addOrder(final String ordId, final ServiceInfo parent, final ServiceInfo serviceInfo) {
        if (parent == null || serviceInfo == null) {
            return;
        }
        Vertex vP = getVertex(parent);
        Vertex v = getVertex(serviceInfo);
        if (vP == v) {
            return;
        }
        if (parent.isConstraintPlaceholder() || serviceInfo.isConstraintPlaceholder()) {
            /* if it is sequential rsc set change it to show to the resource
             * instead on placeholder. */

            final ConstraintPHInfo cphi;
            if (parent.isConstraintPlaceholder()) {
                cphi = (ConstraintPHInfo) parent;
                final ServiceInfo si = cphi.prevInSequence(serviceInfo, false);
                if (si != null) {
                    vP = getVertex(si);
                }
            } else {
                cphi = (ConstraintPHInfo) serviceInfo;
                final ServiceInfo si = cphi.nextInSequence(parent, false);
                if (si != null) {
                    v = getVertex(si);
                }
            }
        }
        if (v == null || vP == null) {
            return;
        }
        final Vertex vP0 = vP;
        final Vertex v0 = v;

        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                mHbConnectionWriteLock.lock();
                Edge edge = null;
                try {
                    lockGraph();
                    edge = getGraph().findEdge(vP0, v0);
                    if (edge == null) {
                        edge = getGraph().findEdge(v0, vP0);
                        unlockGraph();
                        if (edge != null) {
                            edge.reverse();
                            if (edgeIsOrderList.contains(edge)) {
                                edge.setWrongColocation(true);
                            } else {
                                edge.setWrongColocation(false);
                            }
                        }
                    } else {
                        if (!edgeIsOrderList.contains(edge)) {
                            edge.setWrongColocation(false);
                        }
                        unlockGraph();
                    }
                } catch (final RuntimeException e) {
                    unlockGraph();
                }
                final HbConnectionInfo hbci;
                if (edge == null) {
                    hbci = getClusterBrowser().getNewHbConnectionInfo();
                    edge = new Edge(vP0, v0);
                    lockGraph();
                    getGraph().addEdge(edge, vP0, v0);
                    unlockGraph();
                    edgeToHbconnectionMap.put(edge, hbci);
                    hbconnectionToEdgeMap.put(hbci, edge);
                } else {
                    hbci = edgeToHbconnectionMap.get(edge);
                }
                mHbConnectionWriteLock.unlock();
                if (hbci != null) {
                    hbci.addOrder(ordId, parent, serviceInfo);
                    edgeIsOrderList.add(edge);
                    keepEdgeIsOrderList.add(edge);
                }
            }
        });
    }

    /** Returns whether these two services are colocated. */
    public boolean isColocation(final ServiceInfo parent, final ServiceInfo serviceInfo) {
        final Vertex vP = getVertex(parent);
        final Vertex v = getVertex(serviceInfo);
        if (vP == null || v == null) {
            return false;
        }

        lockGraph();
        Edge edge = getGraph().findEdge(vP, v);
        if (edge == null) {
            edge = getGraph().findEdge(v, vP);
        }
        unlockGraph();
        return edge != null && edgeIsColocationList.contains(edge);
    }

    /** Returns whether these two services are ordered. */
    public boolean isOrder(final ServiceInfo parent, final ServiceInfo serviceInfo) {
        final Vertex vP = getVertex(parent);
        final Vertex v = getVertex(serviceInfo);
        if (vP == null || v == null) {
            return false;
        }

        lockGraph();
        Edge edge = getGraph().findEdge(vP, v);
        if (edge == null) {
            edge = getGraph().findEdge(v, vP);
        }
        unlockGraph();
        return edge != null && edgeIsOrderList.contains(edge);
    }

    /** Adds colocation constraint from parent to the service. */
    public void addColocation(final String colId, final ServiceInfo rsc, final ServiceInfo withRsc) {
        if (rsc == null || withRsc == null) {
            return;
        }
        Vertex vRsc = getVertex(rsc);
        Vertex vWithRsc = getVertex(withRsc);
        if (vRsc == vWithRsc) {
            return;
        }
        if (rsc.isConstraintPlaceholder() || withRsc.isConstraintPlaceholder()) {
            /* if it is sequential rsc set change it to show to the resource
             * instead on placeholder. */

            final ConstraintPHInfo cphi;
            if (rsc.isConstraintPlaceholder()) {
                cphi = (ConstraintPHInfo) rsc;
                final ServiceInfo si = cphi.nextInSequence(withRsc, true);
                if (si != null) {
                    vRsc = getVertex(si);
                }
            } else {
                cphi = (ConstraintPHInfo) withRsc;
                final ServiceInfo si = cphi.prevInSequence(rsc, true);
                if (si != null) {
                    vWithRsc = getVertex(si);
                }
            }
        }
        if (vWithRsc == null || vRsc == null) {
            return;
        }
        final Vertex vWithRsc0 = vWithRsc;
        final Vertex vRsc0 = vRsc;
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                mHbConnectionWriteLock.lock();
                Edge edge = null;
                try {
                    lockGraph();
                    edge = getGraph().findEdge(vWithRsc0, vRsc0);
                    if (edge == null) {
                        edge = getGraph().findEdge(vRsc0, vWithRsc0);
                        if (edge != null) {
                            edge.setWrongColocation(true);
                        }
                    } else {
                        edge.setWrongColocation(false);
                    }
                    unlockGraph();
                } catch (final RuntimeException e) {
                    /* ignore */
                }
                final HbConnectionInfo hbci;
                if (edge == null) {
                    hbci = getClusterBrowser().getNewHbConnectionInfo();
                    edge = new Edge(vWithRsc0, vRsc0);
                    lockGraph();
                    getGraph().addEdge(edge, vWithRsc0, vRsc0);
                    unlockGraph();
                    edgeToHbconnectionMap.put(edge, hbci);
                    hbconnectionToEdgeMap.put(hbci, edge);
                } else {
                    hbci = edgeToHbconnectionMap.get(edge);
                }
                mHbConnectionWriteLock.unlock();
                if (hbci != null) {
                    hbci.addColocation(colId, rsc, withRsc);
                    edgeIsColocationList.add(edge);
                    keepEdgeIsColocationList.add(edge);
                }
            }
        });
    }

    /** Removes items from order list. */
    public void clearKeepOrderList() {
        mHbConnectionReadLock.lock();
        try {
            lockGraph();
            for (final Edge edge : getGraph().getEdges()) {
                final HbConnectionInfo hbci = edgeToHbconnectionMap.get(edge);
                if (hbci != null && !hbci.isNew()) {
                    /* don't remove the new ones. */
                    keepEdgeIsOrderList.remove(edge);
                }
            }
            unlockGraph();
        } finally {
            mHbConnectionReadLock.unlock();
        }
    }

    /** Removes items from colocation list. */
    public void clearKeepColocationList() {
        mHbConnectionReadLock.lock();
        try {
            lockGraph();
            for (final Edge edge : getGraph().getEdges()) {
                final HbConnectionInfo hbci = edgeToHbconnectionMap.get(edge);
                if (hbci != null && !hbci.isNew()) {
                    /* don't remove the new ones. */
                    keepEdgeIsColocationList.remove(edge);
                }
            }
            unlockGraph();
        } finally {
            mHbConnectionReadLock.unlock();
        }
    }

    /** Returns label for service vertex. */
    @Override
    protected String getMainText(final Vertex v, final Application.RunMode runMode) {
        final String str;
        if (vertexToHostMap.containsKey(v)) {
            str = vertexToHostMap.get(v).toString();
        } else if (vertexToConstraintPHMap.containsKey(v)) {
            str = vertexToConstraintPHMap.get(v).getMainTextForGraph();
        } else {
            final ServiceInfo si = (ServiceInfo) getInfo(v);
            if (si == null) {
                return "";
            }
            final Set<Vertex> vipl = getVertexIsPresentList();
            putVertexIsPresentList();
            if (si.getService().isRemoved()) {
                str = Tools.getString("CRMGraph.Removing");
            } else if (vipl.contains(v)) {
                str = si.getMainTextForGraph();
            } else {
                if (si.getService().isNew()) {
                    str = si.getMainTextForGraph();
                } else {
                    str = Tools.getString("CRMGraph.Unconfigured");
                }
            }
        }
        return str;
    }

    /** Returns shape of the service vertex. */
    @Override
    protected Shape getVertexShape(final Vertex v, final VertexShapeFactory<Vertex> factory) {
        if (vertexToHostMap.containsKey(v)) {
            return factory.getRectangle(v);
        } else if (vertexToConstraintPHMap.containsKey(v)) {
            return factory.getEllipse(v);
        } else {
            final RoundRectangle2D r = factory.getRoundRectangle(v);

            return new RoundRectangle2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight(), 20, 20);
        }
    }

    @Override
    public void pickInfo(final Info i) {
        final GroupInfo groupInfo = ((ServiceInfo) i).getGroupInfo();
        if (groupInfo == null) {
            super.pickInfo(i);
        } else {
            /* group is picked in the graph, if group service was selected. */
            final Vertex v = getVertex(groupInfo);
            final PickedState<Edge> psEdge = getVisualizationViewer().getRenderContext().getPickedEdgeState();
            final PickedState<Vertex> psVertex = getVisualizationViewer().getRenderContext().getPickedVertexState();
            psEdge.clear();
            psVertex.clear();
            psVertex.pick(v, true);
        }
    }

    /** Handles right click on the service vertex and creates popup menu. */
    @Override
    protected void handlePopupVertex(final Vertex v, final List<Vertex> pickedV, final Point2D pos) {
        final Info info;
        if (pickedV.size() > 1) {
            info = multiSelectionInfo;
        } else if (vertexToHostMap.containsKey(v)) {
            info = getInfo(v);
        } else if (vertexToConstraintPHMap.containsKey(v)) {
            info = vertexToConstraintPHMap.get(v);
        } else {
            info = getInfo(v);
        }
        if (info != null) {
            final JPopupMenu p = info.getPopup();
            info.updateMenus(pos);
            showPopup(p, pos);
        }
    }

    /**
     * Recreates the add existing service popup, since it can change in
     * very unlikely event.
     */
    private void reloadAddExistingServicePopup(final JMenu addServiceMenuItem, final Vertex v) {
        if (addServiceMenuItem == null) {
            return;
        }
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        addServiceMenuItem.removeAll();
        if (si == null) {
            return;
        }
        boolean separatorAdded = false;
        final Application.RunMode runMode = getRunMode();
        for (final ServiceInfo asi : getClusterBrowser().getExistingServiceList(si)) {
            final MyMenuItem mmi = menuFactory.createMenuItem(asi.toString(),
                                                  null,
                                                  null,
                                                  new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                                                  new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        si.addServicePanel(asi,
                                null,
                                false, /* TODO: colocation only */
                                false, /* order only */
                                true,
                                getClusterBrowser().getDCHost(),
                                runMode);
                        repaint();
                    }
                });
            if ("Filesystem".equals(asi.getInternalValue()) || "IPaddr2".equals(asi.getInternalValue())) {

                mmi.setSpecialFont();
                addServiceMenuItem.add(mmi);
            } else {
                if (!separatorAdded) {
                    addServiceMenuItem.addSeparator();
                    separatorAdded = true;
                }
                addServiceMenuItem.add(mmi);
            }
        }
    }

    /** Handles right click on the edge and creates popup menu. */
    @Override
    protected void handlePopupEdge(final Edge edge, final Point2D pos) {
        mHbConnectionReadLock.lock();
        final HbConnectionInfo info;
        try {
            info = edgeToHbconnectionMap.get(edge);
        } finally {
            mHbConnectionReadLock.unlock();
        }
        if (info != null) {
            final JPopupMenu p = info.getPopup();
            info.updateMenus(pos);
            showPopup(p, pos);
        }
    }

    /** Handles right click on the background and creates popup menu. */
    @Override
    protected void handlePopupBackground(final Point2D pos) {
        final Info info = getClusterBrowser().getServicesInfo();
        final JPopupMenu p = info.getPopup();
        info.updateMenus(pos);
        showPopup(p, pos);
    }

    /**
     * After service vertex v was released in position pos, set its location
     * there, so that it doesn't jump back. If position is outside of the view,
     * bring the vertex back in the view.
     */
    @Override
    protected void vertexReleased(final Vertex v, final Point2D pos) {
        double x = pos.getX();
        double y = pos.getY();
        final double minPos = (getVertexWidth(v) - getDefaultVertexWidth(v)) / 2;
        x = x < minPos ? minPos : x;
        x = x > MAX_X_POS ? MAX_X_POS : x;
        y = y < MIN_Y_POS ? MIN_Y_POS : y;
        y = y > MAX_Y_POS ? MAX_Y_POS : y;
        pos.setLocation(x, y);
        getVertexLocations().put(v, pos);
        putVertexLocations();
        getLayout().setLocation(v, pos);
    }

    /**
     * Selects Info in the view if a service vertex was pressed. If more
     * vertices were selected, it does not do anything.
     */
    @Override
    protected void oneVertexPressed(final Vertex v) {
        pickVertex(v);
        if (vertexToHostMap.containsKey(v)) {
            final HostInfo hi = vertexToHostMap.get(v);
            if (hi != null) {
                mainPanel.setTerminalPanel(hi.getHost().getTerminalPanel());
                getClusterBrowser().setRightComponentInView(hi);
            }
        } else if (vertexToConstraintPHMap.containsKey(v)) {
            final ConstraintPHInfo cphi = vertexToConstraintPHMap.get(v);
            if (cphi != null) {
                getClusterBrowser().setRightComponentInView(cphi);
            }
        } else {
            final ServiceInfo si = (ServiceInfo) getInfo(v);
            if (si != null) {
                si.selectMyself();
            }
        }
    }

    /**
     * Is called, when background of the graph is clicked. It deselects
     * selected node.
     */
    @Override
    protected void backgroundClicked() {
        getClusterBrowser().getServicesInfo().selectMyself();
    }

    /** Returns tool tip when mouse is over a service vertex. */
    @Override
    public String getVertexToolTip(final Vertex v) {
        final Application.RunMode runMode = getRunMode();

        if (vertexToHostMap.containsKey(v)) {
            return vertexToHostMap.get(v).getToolTipForGraph(runMode);
        } else if (vertexToConstraintPHMap.containsKey(v)) {
            return vertexToConstraintPHMap.get(v).getToolTipForGraph(runMode);
        }
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        if (si == null) {
            return null;
        }
        return si.getToolTipText(runMode);
    }

    /** Returns the tool tip for the edge. */
    @Override
    public String getEdgeToolTip(final Edge edge) {
        // TODO: move this to the clusterbrowser
        final Pair<Vertex> p = getGraph().getEndpoints(edge);
        final boolean edgeIsOrder = edgeIsOrderList.contains(edge);
        final boolean edgeIsColocation = edgeIsColocationList.contains(edge);

        final Vertex v = p.getSecond();
        final Vertex parent = p.getFirst();
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        if (si == null) {
            return null;
        }
        final ServiceInfo siP = (ServiceInfo) getInfo(parent);
        if (siP == null) {
            return null;
        }
        final StringBuilder s = new StringBuilder(100);
        s.append(siP);
        mHbConnectionReadLock.lock();
        final HbConnectionInfo hbci;
        try {
            hbci = edgeToHbconnectionMap.get(edge);
        } finally {
            mHbConnectionReadLock.unlock();
        }
        if (edgeIsOrder && hbci != null && !hbci.isOrdScoreNull(null, null)) {
            s.append(" is started before ");
        } else {
            s.append(" and ");
        }
        s.append(si);
        if (!edgeIsOrder) {
            s.append(" are located");
        }
        if (edgeIsColocation) {
            final HbConnectionInfo.ColScoreType colSType = hbci.getColocationScoreType(null, null);
            if (colSType == HbConnectionInfo.ColScoreType.NEGATIVE
                || colSType == HbConnectionInfo.ColScoreType.MINUS_INFINITY) {
                s.append(" not on the same host");
            } else {
                s.append(" on the same host");
            }
        } else {
            s.append(" not necessarily on the same host");
        }

        return s.toString();
    }

    /**
     * Returns color of the vertex, depending if the service is configured,
     * removed or color of the host.
     */
    @Override
    protected Color getVertexFillColor(final Vertex v) {
        final Application.RunMode runMode = getRunMode();
        if (vertexToHostMap.containsKey(v)) {
            return vertexToHostMap.get(v).getHost().getPmColors()[0];
        } else if (vertexToConstraintPHMap.containsKey(v)) {
            if (vertexToConstraintPHMap.get(v).getService().isNew()) {
                return Tools.getDefaultColor("CRMGraph.FillPaintUnconfigured");
            } else {
                // TODO fillpaint.placeholder
                return Tools.getDefaultColor("CRMGraph.FillPaintPlaceHolder");
            }
        }
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        if (si == null) {
            return null;
        }
        final Set<Vertex> vipl = getVertexIsPresentList();
        putVertexIsPresentList();
        if (getClusterBrowser().allHostsWithoutClusterStatus()) {
            return Tools.getDefaultColor("CRMGraph.FillPaintUnknown");
        } else if (si.getService().isOrphaned()) {
            return Tools.getDefaultColor("CRMGraph.FillPaintUnknown");
        } else if (si.isFailed(runMode)) {
            return Tools.getDefaultColor("CRMGraph.FillPaintFailed");
        } else if (!si.isRunning(runMode)) {
            return getClusterBrowser().SERVICE_STOPPED_FILL_PAINT;
        } else if (getClusterBrowser().crmStatusFailed()) {
            return Tools.getDefaultColor("CRMGraph.FillPaintUnknown");
        } else if (vipl.contains(v) || Application.isTest(runMode)) {
            final List<Color> colors = si.getHostColors(runMode);
            if (!colors.isEmpty()) {
                return colors.get(0);
            } else {
                return Color.WHITE; /* more colors */
            }
        } else if (!si.getService().isNew()) {
            return Tools.getDefaultColor("CRMGraph.FillPaintRemoved");
        } else {
            return Tools.getDefaultColor("CRMGraph.FillPaintUnconfigured");
        }
    }

    /** Returns label that describes the edge. */
    @Override
    protected String getLabelForEdgeStringer(final Edge e) {
        if (isTestEdge(e)) {
            return Tools.getString("CRMGraph.Simulate");
        }
        final boolean edgeIsOrder = edgeIsOrderList.contains(e);
        final boolean edgeIsColocation = edgeIsColocationList.contains(e);
        final Pair<Vertex> p = getGraph().getEndpoints(e);
        if (p == null) {
            return "";
        }
        final ServiceInfo s1 = (ServiceInfo) getInfo(p.getSecond());
        if (s1 == null) {
            return "";
        }
        final ServiceInfo s2 = (ServiceInfo) getInfo(p.getFirst());
        if (s2 == null) {
            return "";
        }
        mHbConnectionReadLock.lock();
        final HbConnectionInfo hbci;
        try {
            hbci = edgeToHbconnectionMap.get(e);
        } finally {
            mHbConnectionReadLock.unlock();
        }
        if (hbci == null) {
            return "";
        }
        String leftArrow = "\u2190 "; /* <- */
        String rightArrow = " \u2192"; /* -> */
        final HbConnectionInfo.ColScoreType colSType = hbci.getColocationScoreType(null, null);
        if (edgeIsColocation) {
            if (hbci.isColocationTwoDirections()) {
                leftArrow = "\u21AE "; /* </> */
                rightArrow = "\u21AE "; /* </> */
            } else if (colSType == HbConnectionInfo.ColScoreType.NEGATIVE
                || colSType == HbConnectionInfo.ColScoreType.MINUS_INFINITY) {
            } else if (colSType == HbConnectionInfo.ColScoreType.IS_NULL) {
                leftArrow = "\u21E0 "; /* < - - */
                rightArrow = " \u21E2"; /* - - > */
            } else if (colSType == HbConnectionInfo.ColScoreType.MIXED) {
                leftArrow = "\u219A "; /* </- */
                rightArrow = " \u219B"; /* -/> */
            }
        }
        String colScore = "";
        if (edgeIsColocation
            && colSType == HbConnectionInfo.ColScoreType.IS_NULL) {
            colScore = "0";
        }
        String ordScore = "";
        if (edgeIsOrder && hbci.isOrdScoreNull(null, null)) {
            ordScore = "0";
        }

        String colDesc = null;
        String ordDesc = null;
        String desc = null;
        if (edgeIsOrder && edgeIsColocation) {
            if (colSType == HbConnectionInfo.ColScoreType.NEGATIVE
                || colSType == HbConnectionInfo.ColScoreType.MINUS_INFINITY) {
                colDesc = "repelled";
            } else {
                colDesc = "col";
            }
            ordDesc = "ord";
        } else if (edgeIsOrder) {
            ordDesc = "ordered";
        } else if (edgeIsColocation) {
            if (colSType == HbConnectionInfo.ColScoreType.NEGATIVE
                || colSType == HbConnectionInfo.ColScoreType.MINUS_INFINITY) {
                colDesc = "repelled";
            } else {
                colDesc = "colocated";
            }
        } else if (s1.getService().isNew() || s2.getService().isNew()) {
            desc = Tools.getString("CRMGraph.Unconfigured");
        } else {
            desc = Tools.getString("CRMGraph.Removing");
        }
        if (desc != null) {
            return desc;
        }
        final Vertex v1 = p.getSecond();
        final Vertex v2 = p.getFirst();
        double s1X = 0;
        final Point2D loc1 = getLayout().transform(v1);
        if (loc1 != null) {
            s1X = loc1.getX();
        }
        final Point2D loc2 = getLayout().transform(v2);
        double s2X = 0;
        if (loc2 != null) {
            s2X = loc2.getX();
        }
        final StringBuilder sb = new StringBuilder(15);
        final boolean upsideDown = s1X < s2X;
        final boolean left = hbci.isWithRsc(s2) && !e.isWrongColocation();
        if (edgeIsColocation) {
            if ((left && !upsideDown) || (!left && upsideDown)) {
                sb.append(leftArrow); /* <- */
                sb.append(colScore);
                sb.append(' ');
                sb.append(colDesc);
                if (edgeIsOrder) {
                    sb.append('/');
                    sb.append(ordDesc);
                    sb.append(' ');
                    sb.append(ordScore);
                }
            } else if ((!left && !upsideDown) || (left && upsideDown)) {
                if (edgeIsOrder) {
                    sb.append(ordScore);
                    sb.append(' ');
                    sb.append(ordDesc);
                    sb.append('/');
                }
                sb.append(colDesc);
                sb.append(' ');
                sb.append(colScore);
                sb.append(rightArrow); /* -> */
            }
        } else if (edgeIsOrder) {
            if (left && !upsideDown) {
                sb.append(ordDesc);
                sb.append(' ');
                sb.append(ordScore);
            } else {
                sb.append(ordScore);
                sb.append(' ');
                sb.append(ordDesc);
            }
        }
        return sb.toString();
    }

    /** Returns paint of the edge. */
    @Override
    protected Paint getEdgeDrawPaint(final Edge e) {
        mHbConnectionReadLock.lock();
        final HbConnectionInfo hbci;
        try {
            hbci = edgeToHbconnectionMap.get(e);
        } finally {
            mHbConnectionReadLock.unlock();
        }
        if (hbci != null && hbci.isNew()) {
            return Tools.getDefaultColor("ResourceGraph.EdgeDrawPaintNew");
        } else if (edgeIsOrderList.contains(e) && edgeIsColocationList.contains(e)) {
            return super.getEdgeDrawPaint(e);
        } else if (edgeIsOrderList.contains(e) || edgeIsColocationList.contains(e)) {
            return Tools.getDefaultColor("ResourceGraph.EdgeDrawPaintBrighter");
        } else {
            return Tools.getDefaultColor("ResourceGraph.EdgeDrawPaintRemoved");
        }
    }

    /** Returns paint of the picked edge. */
    @Override
    protected Paint getEdgePickedPaint(final Edge e) {
        mHbConnectionReadLock.lock();
        final HbConnectionInfo hbci;
        try {
            hbci = edgeToHbconnectionMap.get(e);
        } finally {
            mHbConnectionReadLock.unlock();
        }
        if (hbci != null && hbci.isNew()) {
            return Tools.getDefaultColor("ResourceGraph.EdgePickedPaintNew");
        } else if (edgeIsOrderList.contains(e) && edgeIsColocationList.contains(e)) {
            return super.getEdgePickedPaint(e);
        } else if (edgeIsOrderList.contains(e) || edgeIsColocationList.contains(e)) {
            return Tools.getDefaultColor("ResourceGraph.EdgePickedPaintBrighter");
        } else {
            return Tools.getDefaultColor("ResourceGraph.EdgePickedPaintRemoved");
        }
    }

    /**
     * Remove edges that were marked as not present, meaning there are no
     * colocation nor order constraints.
     */
    public void killRemovedEdges() {
        lockGraph();
        final Collection<Edge> edges = new ArrayList<Edge>(getGraph().getEdges().size());
        for (final Edge e : getGraph().getEdges()) {
            edges.add(e);
        }
        unlockGraph();

        for (final Edge e : edges) {
            final Pair<Vertex> p = getGraph().getEndpoints(e);
            final ServiceInfo s1 = (ServiceInfo) getInfo(p.getSecond());
            if (s1 == null) {
                continue;
            }
            final ServiceInfo s2 = (ServiceInfo) getInfo(p.getFirst());
            if (s2 == null) {
                continue;
            }
            if (!keepEdgeIsOrderList.contains(e)) {
                edgeIsOrderList.remove(e);
            }
            if (!keepEdgeIsColocationList.contains(e)) {
                edgeIsColocationList.remove(e);
            }
            if (s1.getService().isNew() && !s1.getService().isRemoved()
                || (s2.getService().isNew() && !s2.getService().isRemoved())) {
                continue;
            }
            if (!keepEdgeIsOrderList.contains(e)) {
                mHbConnectionReadLock.lock();
                final HbConnectionInfo hbci;
                try {
                    hbci = edgeToHbconnectionMap.get(e);
                } finally {
                    mHbConnectionReadLock.unlock();
                }
                if (hbci != null) {
                    hbci.removeOrders();
                }
            }

            if (!keepEdgeIsColocationList.contains(e)) {
                mHbConnectionReadLock.lock();
                final HbConnectionInfo hbci;
                try {
                    hbci = edgeToHbconnectionMap.get(e);
                } finally {
                    mHbConnectionReadLock.unlock();
                }
                if (hbci != null) {
                    hbci.removeColocations();
                }
            }
            if (!keepEdgeIsOrderList.contains(e) && !keepEdgeIsColocationList.contains(e)) {
                removeEdge(e, Application.RunMode.LIVE);
            }
        }
    }

    /** Removes edge if it is not in the list of constraints. */
    private void removeEdge(final Edge e, final Application.RunMode runMode) {
        if (e == null) {
            return;
        }
        if (!edgeIsOrderList.contains(e)
            && !edgeIsColocationList.contains(e)) {
            e.reset();
            lockGraph();
            getGraph().removeEdge(e);
            unlockGraph();
            mHbConnectionWriteLock.lock();
            try {
                final HbConnectionInfo hbci = edgeToHbconnectionMap.get(e);
                edgeToHbconnectionMap.remove(e);
                if (hbci != null) {
                    hbconnectionToEdgeMap.remove(hbci);
                    hbci.removeMyself(runMode);
                }
            } finally {
                mHbConnectionWriteLock.unlock();
            }
        }
    }

    /** Remove vertices that were marked as not present. */
    public void killRemovedVertices() {
        /* Make copy first. */
        final Collection<Vertex> vertices = new ArrayList<Vertex>();
        lockGraph();
        for (final Vertex vo : getGraph().getVertices()) {
            vertices.add(vo);
        }
        unlockGraph();
        for (final Vertex v : vertices) {
            if (vertexToHostMap.containsKey(v)) {
                continue;
            }
            lockGraph();
            if (!getGraph().getInEdges(v).isEmpty() || !getGraph().getOutEdges(v).isEmpty()) {
                unlockGraph();
                continue;
            }
            unlockGraph();
            final Set<Vertex> vipl = getVertexIsPresentList();
            putVertexIsPresentList();
            if (vertexToConstraintPHMap.containsKey(v)) {
                final ConstraintPHInfo cphi = (ConstraintPHInfo) getInfo(v);
                if (cphi == null) {
                    continue;
                }
                if (!vipl.contains(v)) {
                    cphi.setUpdated(false);
                    if (!getClusterBrowser().crmStatusFailed()
                        && cphi.getService().isRemoved()) {
                        getVertexLocations().put(v, null);
                        putVertexLocations();
                        lockGraph();
                        getGraph().removeVertex(v);
                        removeInfo(v);
                        removeVertex(cphi);
                        unlockGraph();
                        cphi.removeInfo();
                        getVertexToMenus().remove(v);
                        vertexToConstraintPHMap.remove(v);
                        constraintPHToVertexMap.remove(cphi);
                        somethingChanged();
                    } else {
                        cphi.getService().setNew(true);
                    }
                }
                cphi.resetRscSetConnectionData();
            } else {
                if (!vipl.contains(v)) {
                    final ServiceInfo si = (ServiceInfo) getInfo(v);
                    if (si != null && !si.getService().isNew() && !getClusterBrowser().crmStatusFailed()) {
                        si.setUpdated(false);
                        getVertexLocations().put(v, null);
                        putVertexLocations();
                        lockGraph();
                        getGraph().removeVertex(v);
                        removeInfo(v);
                        removeVertex(si);
                        unlockGraph();
                        si.removeInfo();
                        //TODO: unregister removePopup(v);
                        getVertexToMenus().remove(v);
                        vertexToAddServiceMap.remove(v);
                        vertexToAddExistingServiceMap.remove(v);
                        //TODO: positions are still there
                        somethingChanged();
                    }
                }
            }
        }
    }

    /** Mark vertex and its edges to be removed later. */
    @Override
    protected void removeInfo(final Info i) {
        final Vertex v = getVertex(i);
        /* remove edges */

        lockGraph();
        for (final Edge e : getGraph().getInEdges(v)) {
            edgeIsOrderList.remove(e);
            edgeIsColocationList.remove(e);
            keepEdgeIsOrderList.remove(e);
            keepEdgeIsColocationList.remove(e);
        }

        for (final Edge e : getGraph().getOutEdges(v)) {
            edgeIsOrderList.remove(e);
            edgeIsColocationList.remove(e);
            keepEdgeIsOrderList.remove(e);
            keepEdgeIsColocationList.remove(e);
        }
        unlockGraph();

        /* remove vertex */
        getVertexIsPresentList().remove(v);
        putVertexIsPresentList();
        ((ServiceInfo) i).getService().setNew(false);
        resetSavedPosition(i);
    }

    /**
     * Returns vertex location list, it acquires mVertexIsPresentListLock and
     * must be followed by putVertexIsPresentList.
     */
    Set<Vertex> getVertexIsPresentList() {
        mVertexIsPresentListLock.lock();
        return vertexIsPresentList;
    }

    void putVertexIsPresentList() {
        mVertexIsPresentListLock.unlock();
    }

    /** Set vertex-is-present list. */
    public void setServiceIsPresentList(final Iterable<ServiceInfo> sis) {
        final Set<Vertex> vipl = new HashSet<Vertex>();
        for (final ServiceInfo si : sis) {
            final Vertex v = getVertex(si);
            if (v == null) {
                /* e.g. group vertices */
                continue;
            }
            vipl.add(v);
        }
        mVertexIsPresentListLock.lock();
        try {
            vertexIsPresentList = vipl;
        } finally {
            mVertexIsPresentListLock.unlock();
        }
    }

    /** Returns an icon for the vertex. */
    @Override
    protected List<ImageIcon> getIconsForVertex(final Vertex v, final Application.RunMode runMode) {
        final List<ImageIcon> icons = new ArrayList<ImageIcon>();
        final HostInfo hi = vertexToHostMap.get(v);
        if (hi != null) {
            if (hi.getHost().isCrmStatusOk()) {
                icons.add(HostBrowser.HOST_ON_ICON_LARGE);
            } else {
                icons.add(HostBrowser.HOST_ICON_LARGE);
            }
            if (hi.isStandby(runMode)) {
                icons.add(HOST_STANDBY_ICON);
            }
            return icons;
        } else if (vertexToConstraintPHMap.containsKey(v)) {
            return null;
        }

        final ServiceInfo si = (ServiceInfo) getInfo(v);
        if (si == null) {
            return null;
        }
        if (si.isFailed(runMode)) {
            if (si.isRunning(runMode)) {
                icons.add(SERVICE_RUNNING_FAILED_ICON);
            } else {
                icons.add(SERVICE_STOPPED_FAILED_ICON);
            }
        } else if (si.isStopped(runMode) || getClusterBrowser().allHostsWithoutClusterStatus()) {
            if (si.isRunning(runMode)) {
                icons.add(SERVICE_STOPPING_ICON);
            } else {
                icons.add(SERVICE_STOPPED_ICON);
            }
        } else {
            if (si.isRunning(runMode)) {
                icons.add(SERVICE_RUNNING_ICON);
            } else {
                icons.add(SERVICE_STARTED_ICON);
            }
        }
        if (!si.isManaged(runMode) || si.getService().isOrphaned()) {
            icons.add(SERVICE_UNMANAGED_ICON);
        }
        if (si.getMigratedTo(runMode) != null || si.getMigratedFrom(runMode) != null) {
            icons.add(SERVICE_MIGRATED_ICON);
        }
        return icons;
    }

    /** Returns whether to show an edge arrow. */
    @Override
    protected boolean showEdgeArrow(final Edge e) {
        if (edgeIsOrderList.contains(e)) {
            mHbConnectionReadLock.lock();
            final HbConnectionInfo hbci;
            try {
                hbci = edgeToHbconnectionMap.get(e);
            } finally {
                mHbConnectionReadLock.unlock();
            }
            return hbci != null && !hbci.isOrderTwoDirections();
        }
        return false;
    }

    /** Returns whether to show a hollow arrow. */
    @Override
    protected boolean showHollowArrow(final Edge e) {
        if (edgeIsOrderList.contains(e)) {
            mHbConnectionReadLock.lock();
            final HbConnectionInfo hbci;
            try {
                hbci = edgeToHbconnectionMap.get(e);
            } finally {
                mHbConnectionReadLock.unlock();
            }
            return hbci != null && hbci.isOrdScoreNull(null, null);
        }
        return false;
    }

    /** Reloads popup menus for all services. */
    public void reloadServiceMenus() {
        lockGraph();
        for (final Vertex v : getGraph().getVertices()) {
            final JMenu existingServiceMenuItem = vertexToAddExistingServiceMap.get(v);
            reloadAddExistingServicePopup(existingServiceMenuItem, v);
        }
        unlockGraph();
    }

    /** Is called after an edge was pressed. */
    @Override
    protected void oneEdgePressed(final Edge e) {
        mHbConnectionReadLock.lock();
        final HbConnectionInfo hbci;
        try {
            hbci = edgeToHbconnectionMap.get(e);
        } finally {
            mHbConnectionReadLock.unlock();
        }
        if (hbci != null) {
            getClusterBrowser().setRightComponentInView(hbci);
        }
    }

    /** Removes the connection, the order, the colocation or both. */
    public void removeConnection(final HbConnectionInfo hbci, final Host dcHost, final Application.RunMode runMode) {
        final ServiceInfo siP = hbci.getLastServiceInfoParent();
        final ServiceInfo siC = hbci.getLastServiceInfoChild();
        mHbConnectionReadLock.lock();
        final Edge edge;
        try {
            edge = hbconnectionToEdgeMap.get(hbci);
        } finally {
            mHbConnectionReadLock.unlock();
        }
        if (edgeIsOrderList.contains(edge)) {
            siC.removeOrder(siP, dcHost, runMode);
        }
        final ServiceInfo siRsc = hbci.getLastServiceInfoRsc();
        final ServiceInfo siWithRsc = hbci.getLastServiceInfoWithRsc();
        if (edgeIsColocationList.contains(edge)) {
            if (Application.isLive(runMode)) {
                edgeIsOrderList.remove(edge);
                edgeIsColocationList.remove(edge);
                keepEdgeIsOrderList.remove(edge);
                keepEdgeIsColocationList.remove(edge);
            }
            siRsc.removeColocation(siWithRsc, dcHost, runMode);
        } else {
            if (Application.isLive(runMode)) {
                edgeIsOrderList.remove(edge);
                edgeIsColocationList.remove(edge);
                keepEdgeIsOrderList.remove(edge);
                keepEdgeIsColocationList.remove(edge);
            }
        }
        if (Application.isTest(runMode)) {
            addExistingTestEdge(edge);
        } else {
            if (hbci.isNew()) {
                swingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        removeEdge(edge, runMode);
                    }
                });
            }
        }
    }

    public void removeOrder(final HbConnectionInfo hbci, final Host dcHost, final Application.RunMode runMode) {
        if (hbci == null) {
            return;
        }
        final ServiceInfo siP = hbci.getLastServiceInfoParent();
        final ServiceInfo siC = hbci.getLastServiceInfoChild();
        mHbConnectionReadLock.lock();
        final Edge edge;
        try {
            edge = hbconnectionToEdgeMap.get(hbci);
        } finally {
            mHbConnectionReadLock.unlock();
        }
        if (edgeIsOrderList.contains(edge)) {
            if (Application.isLive(runMode)) {
                edgeIsOrderList.remove(edge);
                keepEdgeIsOrderList.remove(edge);
            }
            siC.removeOrder(siP, dcHost, runMode);
        }
        if (Application.isTest(runMode)) {
            addExistingTestEdge(edge);
        } else {
            if (hbci.isNew()) {
                swingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        removeEdge(edge, runMode);
                    }
                });
            }
        }
    }

    public void addOrder(final HbConnectionInfo hbConnectionInfo,
                         final Host dcHost,
                         final Application.RunMode runMode) {
        final ServiceInfo siRsc = hbConnectionInfo.getLastServiceInfoRsc();
        final ServiceInfo siWithRsc = hbConnectionInfo.getLastServiceInfoWithRsc();
        if ((siWithRsc != null && siWithRsc.getService() != null && siWithRsc.getService().isNew())
            || (siRsc != null && siRsc.getService() != null && siRsc.getService().isNew())) {
            addOrder(null, siRsc, siWithRsc);
        } else {
            siWithRsc.addOrder(siRsc, dcHost, runMode);
        }
        if (Application.isTest(runMode)) {
            mHbConnectionReadLock.lock();
            final Edge edge;
            try {
                edge = hbconnectionToEdgeMap.get(hbConnectionInfo);
            } finally {
                mHbConnectionReadLock.unlock();
            }
            addExistingTestEdge(edge);
        }
    }

    public void removeColocation(final HbConnectionInfo hbci, final Host dcHost, final Application.RunMode runMode) {
        final ServiceInfo siRsc = hbci.getLastServiceInfoRsc();
        final ServiceInfo siWithRsc = hbci.getLastServiceInfoWithRsc();
        mHbConnectionReadLock.lock();
        final Edge edge;
        try {
            edge = hbconnectionToEdgeMap.get(hbci);
            if (edge == null) {
                return;
            }
        } finally {
            mHbConnectionReadLock.unlock();
        }
        if (edgeIsColocationList.contains(edge)) {
            if (Application.isLive(runMode)) {
                edgeIsColocationList.remove(edge);
                keepEdgeIsColocationList.remove(edge);
            }
            siRsc.removeColocation(siWithRsc, dcHost, runMode);
        }
        if (Application.isTest(runMode)) {
            addExistingTestEdge(edge);
        } else {
            if (hbci.isNew()) {
                swingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        removeEdge(edge, runMode);
                    }
                });
            }
        }
    }

    /** Adds colocation. */
    public void addColocation(final HbConnectionInfo hbConnectionInfo,
                              final Host dcHost,
                              final Application.RunMode runMode) {
        final ServiceInfo siP = hbConnectionInfo.getLastServiceInfoParent();
        final ServiceInfo siC = hbConnectionInfo.getLastServiceInfoChild();
        if (siC != null
            && siC.getService() != null
            && siC.getService().isNew()) {
            addColocation(null, siP, siC);
        } else {
            siP.addColocation(siC, dcHost, runMode);
        }
        if (Application.isTest(runMode)) {
            mHbConnectionReadLock.lock();
            final Edge edge;
            try {
                edge = hbconnectionToEdgeMap.get(hbConnectionInfo);
            } finally {
                mHbConnectionReadLock.unlock();
            }
            addExistingTestEdge(edge);
        }
    }

    /** Returns whether this hb connection is order. */
    public boolean isOrder(final HbConnectionInfo hbConnectionInfo) {
        mHbConnectionReadLock.lock();
        final Edge edge;
        try {
            edge = hbconnectionToEdgeMap.get(hbConnectionInfo);
        } finally {
            mHbConnectionReadLock.unlock();
        }
        return edgeIsOrderList.contains(edge);
    }

    /** Returns whether this hb connection is colocation. */
    public boolean isColocation(final HbConnectionInfo hbConnectionInfo) {
        mHbConnectionReadLock.lock();
        final Edge edge;
        try {
            edge = hbconnectionToEdgeMap.get(hbConnectionInfo);
        } finally {
            mHbConnectionReadLock.unlock();
        }
        return edgeIsColocationList.contains(edge);
    }

    /** Adds host to the graph. */
    public void addHost(final HostInfo hostInfo) {
        Vertex v = super.getVertex(hostInfo);
        if (v == null) {
            /* add host vertex */
            v = new Vertex();
            lockGraph();
            getGraph().addVertex(v);
            unlockGraph();
            somethingChanged();
            putInfoToVertex(hostInfo, v);
            vertexToHostMap.put(v, hostInfo);
            hostToVertexMap.put(hostInfo, v);
            putVertexToInfo(v, hostInfo);
            Point2D hostPos = getSavedPosition(hostInfo);
            if (hostPos == null) {
                hostPos = new Point2D.Double(hostDefaultXPos, HOST_Y_POS);
                hostDefaultXPos += HOST_STEP_X;
            }
            getVertexLocations().put(v, hostPos);
            putVertexLocations();
        }
    }

    /** Small text that appears above the icon. */
    @Override
    protected String getIconText(final Vertex v, final Application.RunMode runMode) {
        if (isRunModeTestAnimation()) {
            return Tools.getString("CRMGraph.Simulate");
        }
        if (vertexToHostMap.containsKey(v)) {
            return vertexToHostMap.get(v).getIconTextForGraph(runMode);
        } else if (vertexToConstraintPHMap.containsKey(v)) {
            return vertexToConstraintPHMap.get(v).getIconTextForGraph(runMode);
        }
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        if (si == null) {
            return null;
        }
        return si.getIconTextForGraph(runMode);
    }

    /** Small text that appears in the right corner. */
    @Override
    protected ColorText getRightCornerText(final Vertex v, final Application.RunMode runMode) {
        if (vertexToHostMap.containsKey(v)) {
            final HostInfo hi = vertexToHostMap.get(v);
            return hi.getRightCornerTextForGraph(runMode);
        } else if (vertexToConstraintPHMap.containsKey(v)) {
            return null;
        }
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        if (si != null) {
            return si.getRightCornerTextForGraph(runMode);
        }
        return null;
    }

    /** Small text that appears down. */
    @Override
    protected ColorText[] getSubtexts(final Vertex v, final Application.RunMode runMode) {
        if (vertexToHostMap.containsKey(v)) {
            return vertexToHostMap.get(v).getSubtextsForGraph(runMode);
        } else if (vertexToConstraintPHMap.containsKey(v)) {
            return vertexToConstraintPHMap.get(v).getSubtextsForGraph(runMode);
        }
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        if (si == null) {
            return null;
        }
        return si.getSubtextsForGraph(runMode);
    }

    /**
     * Returns how much of the disk is used. Probably useful only for disks.
     * -1 for not used or not applicable.
     */
    protected int getUsed(final Vertex v) {
        if (vertexToHostMap.containsKey(v)) {
            final HostInfo hi = (HostInfo) getInfo(v);
            if (hi == null) {
                return 0;
            }
            return hi.getUsed();
        }
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        if (si == null) {
            return 0;
        }
        return si.getUsed();
    }

    /**
     * This method draws how much of the vertex is used for something.
     * It draws more colors for verteces that have more background colors.
     */
    @Override
    protected void drawInside(final Vertex v,
                              final Graphics2D g2d,
                              final double x,
                              final double y,
                              final Shape shape) {
        final Application.RunMode runMode = getRunMode();
        final float height = (float) shape.getBounds().getHeight();
        final float width = (float) shape.getBounds().getWidth();
        if (vertexToHostMap.containsKey(v)) {
            final HostInfo hi = (HostInfo) getInfo(v);
            if (hi != null) {
                drawInsideVertex(g2d,
                                 v,
                                 hi.getHost().getPmColors(),
                                 x,
                                 y,
                                 height,
                                 width);
            }
        } else if (vertexToConstraintPHMap.containsKey(v)) {
        } else {
            final ServiceInfo si = (ServiceInfo) getInfo(v);
            if (si == null) {
                return;
            }
            final List<Color> colors = si.getHostColors(runMode);
            final int number = colors.size();
            if (number > 1) {
                for (int i = 1; i < number; i++) {
                    final Paint p = new GradientPaint((float) x + width / number,
                                                      (float) y,
                                                      getVertexFillSecondaryColor(v),
                                                      (float) x + width / number,
                                                      (float) y + height,
                                                      colors.get(i),
                                                      false);
                    g2d.setPaint(p);
                    final Shape s = new RoundRectangle2D.Double(x + (width / number) * i - 5,
                                                                y,
                                                                width / number + (i < number - 1 ? 20 : 5),
                                                                height,
                                                                20,
                                                                20);
                    g2d.fill(s);
                }
            }
            final double used = getUsed(v);
            if (used > 0) {
                /** Show how much is used. */
                final double freeWidth = width * (100 - used) / 100;
                final Shape freeShape =
                   new RoundRectangle2D.Double(x + width - freeWidth, y, freeWidth, height, 20, 20);
                g2d.setColor(new Color(255, 255, 255, 180));
                g2d.fill(freeShape);
            }
        }
        if (isPicked(v)) {
            if (Application.isTest(runMode)) {
                g2d.setColor(Color.RED);
            } else {
                g2d.setColor(Color.BLACK);
            }
        } else {
            g2d.setColor(Color.WHITE);
        }
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(shape);
    }

    /** Returns all crm connections. */
    public Iterable<HbConnectionInfo> getAllHbConnections() {
        final Collection<HbConnectionInfo> allConnections = new ArrayList<HbConnectionInfo>();
        mHbConnectionReadLock.lock();
        try {
            for (final HbConnectionInfo hbci : hbconnectionToEdgeMap.keySet()) {
                allConnections.add(hbci);
            }
        } finally {
            mHbConnectionReadLock.unlock();
        }
        return allConnections;
    }

    /**
     * Returns the vertex that represents the specified resource or its group.
     */
    @Override
    public Vertex getVertex(final Info i) {
        if (i == null) {
            return null;
        }
        if (hostToVertexMap.containsKey(i)) {
            return super.getVertex(i);
        }
        final GroupInfo gi = ((ServiceInfo) i).getGroupInfo();
        if (gi == null) {
            return super.getVertex(i);
        } else {
            return super.getVertex(gi);
        }
    }

    /** Adds placeholder that is used to create resource sets. */
    public void addConstraintPlaceholder(final ConstraintPHInfo rsoi,
                                         final Point2D pos,
                                         final Application.RunMode runMode) {
        if (Application.isTest(runMode)) {
            return;
        }
        final Vertex v = new Vertex();
        if (pos == null) {
            final Point2D newPos = getSavedPosition(rsoi);
            if (newPos == null) {
                final Point2D max = getLastPosition();
                final float maxYPos = (float) max.getY();
                getVertexLocations().put(v, new Point2D.Float(BD_X_POS, maxYPos + 40));
                putVertexLocations();
            } else {
                getVertexLocations().put(v, newPos);
                putVertexLocations();
            }
        } else {
            getVertexLocations().put(v, pos);
            putVertexLocations();
        }

        lockGraph();
        getGraph().addVertex(v);
        unlockGraph();
        putInfoToVertex(rsoi, v);
        putVertexToInfo(v, rsoi);
        constraintPHToVertexMap.put(rsoi, v);
        vertexToConstraintPHMap.put(v, rsoi);
        somethingChanged();
    }

    @Override
    protected int getDefaultVertexHeight(final Vertex v) {
        if (vertexToConstraintPHMap.containsKey(v)) {
            return 50;
        } else {
            return VERTEX_HEIGHT;
        }
    }


    @Override
    protected int getDefaultVertexWidth(final Vertex v) {
        if (vertexToConstraintPHMap.containsKey(v)) {
            return 50;
        } else {
            return super.getDefaultVertexWidth(v);
        }
    }

    @Override
    protected void setVertexWidth(final Vertex v, final int size) {
        super.setVertexWidth(v, size);
    }

    @Override
    protected void setVertexHeight(final Vertex v, final int size) {
        super.setVertexHeight(v, size);
    }

    /** Select multiple services. */
    @Override
    protected void multiSelection() {
        final List<Info> selectedInfos = new ArrayList<Info>();
        final PickedState<Vertex> ps = getVisualizationViewer().getRenderContext().getPickedVertexState();
        for (final Vertex v : getPickedVertices()) {
            final Info i = getInfo(v);
            if (i != null) {
                selectedInfos.add(i);
            }
        }
        multiSelectionInfo = pcmkMultiSelectionInfoProvider.get();
        multiSelectionInfo.init(selectedInfos, getClusterBrowser());
        getClusterBrowser().setRightComponentInView(multiSelectionInfo);
    }

    public void updateRemovedElements(List<ServiceInfo> serviceIsPresent) {
        setServiceIsPresentList(serviceIsPresent);
        swingUtils.invokeInEdt(new Runnable() {
            @Override
            public void run() {
                killRemovedEdges();
                final Map<String, ServiceInfo> idToInfoHash =
                        getClusterBrowser().getNameToServiceInfoHash(ConstraintPHInfo.NAME);
                if (idToInfoHash != null) {
                    for (final Map.Entry<String, ServiceInfo> serviceEntry : idToInfoHash.entrySet()) {
                        final ConstraintPHInfo cphi = (ConstraintPHInfo) serviceEntry.getValue();
                        if (!cphi.getService().isNew() && cphi.isEmpty()) {
                            cphi.getService().setNew(true);
                        }
                    }
                }
                killRemovedVertices();
                scale();
            }
        });
    }
}
