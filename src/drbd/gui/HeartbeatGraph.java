/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
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


package drbd.gui;

import drbd.utilities.Tools;
import drbd.utilities.MyMenuItem;
import drbd.gui.ClusterBrowser.ServiceInfo;
import drbd.gui.Browser.Info;
import drbd.gui.ClusterBrowser.ServicesInfo;
import drbd.gui.ClusterBrowser.GroupInfo;
import drbd.gui.ClusterBrowser.HostScoreInfo;
import drbd.gui.ClusterBrowser.HbConnectionInfo;
import drbd.gui.HostBrowser.HostInfo;

import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.impl.SparseVertex;
import edu.uci.ics.jung.graph.impl.DirectedSparseGraph;
import edu.uci.ics.jung.visualization.Coordinates;
import edu.uci.ics.jung.visualization.PickedState;
import edu.uci.ics.jung.visualization.VertexShapeFactory;

import edu.uci.ics.jung.utils.Pair;
import java.awt.Shape;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.RoundRectangle2D;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import javax.swing.JPopupMenu;
import javax.swing.JMenu;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import EDU.oswego.cs.dl.util.concurrent.Mutex;

/**
 * This class creates graph and provides methods to add new nodes, edges,
 * remove or modify them.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class HeartbeatGraph extends ResourceGraph {
    /** Services info object. */
    private ServicesInfo servicesInfo;
    /** List with edges that are order constraints. */
    private final List<Edge> edgeIsOrderList = new ArrayList<Edge>();
    /** List with edges that are colocation constraints. */
    private final List<Edge> edgeIsColocationList = new ArrayList<Edge>();
    /** List with vertices that are present. */
    private final List<Vertex> vertexIsPresentList = new ArrayList<Vertex>();
    /** Map from vertex to 'Add service' menu. */
    private final Map<Vertex, JMenu> vertexToAddServiceMap =
                                                new HashMap<Vertex, JMenu>();
    /** Map from vertex to 'Add existing service' menu. */
    private final Map<Vertex, JMenu> vertexToAddExistingServiceMap =
                                                new HashMap<Vertex, JMenu>();
    /** Map from edge to the hb connection info of this constraint. */
    private final Map<Edge, HbConnectionInfo>edgeToHbconnectionMap =
                                new LinkedHashMap<Edge, HbConnectionInfo>();
    /** Map from hb connection info to the edge. */
    private final Map<HbConnectionInfo, Edge>hbconnectionToEdgeMap =
                                new LinkedHashMap<HbConnectionInfo, Edge>();
    /** Map from the vertex to the host. */
    private final Map<Vertex, HostInfo>vertexToHostMap =
                                        new LinkedHashMap<Vertex, HostInfo>();
    /** Map from the host to the vertex. */
    private final Map<HostInfo, Vertex>hostToVertexMap =
                                        new LinkedHashMap<HostInfo, Vertex>();

    /** The first X position of the host. */
    private int hostDefaultXPos = 10;
    /** X position of a new block device. */
    private static final int BD_X_POS = 15;
    /** Y position of the host. */
    private static final int HOST_Y_POS = 40;
    /** Vertical step in pixels by which the hosts are drawn in the graph. */
    private static final int HOST_STEP_X = 230;
    /** Minimum horizontal position. */
    private static final int MIN_X_POS = 40;
    /** Minimum vertical position. */
    private static final int MIN_Y_POS = 20;
    /** Maximum horizontal position. */
    private static final int MAX_X_POS = 2600;
    /** Maximum vertical position. */
    private static final int MAX_Y_POS = 2600;
    /** Height of the vertices. */
    private static final int VERTEX_HEIGHT = 50;
    /** Host icon. */
    private static final ImageIcon HOST_ICON =
                Tools.createImageIcon(Tools.getDefault("DrbdGraph.HostIcon"));
    /** Icon that indicates a running service. */
    private static final ImageIcon SERVICE_RUNNING_ICON =
                                     Tools.createImageIcon(Tools.getDefault(
                                       "HeartbeatGraph.ServiceRunningIcon"));
    /** Icon that indicates a not running service. */
    private static final ImageIcon SERVICE_NOT_RUNNING_ICON =
                                Tools.createImageIcon(Tools.getDefault(
                                    "HeartbeatGraph.ServiceNotRunningIcon"));
    /** This mutex is for atomic adding and removing of edges. */
    private final Mutex mEdgeLock = new Mutex();

    /**
     * Prepares a new <code>HeartbeatGraph</code> object.
     */
    public HeartbeatGraph(final ClusterBrowser clusterBrowser) {
        super(clusterBrowser);
    }

    /**
     * Sets servicesInfo object.
     */
    public final void setServicesInfo(final ServicesInfo servicesInfo) {
        this.servicesInfo = servicesInfo;
    }

    /**
     * Gets services info object.
     */
    public final ServicesInfo getServicesInfo() {
        return servicesInfo;
    }

    /**
     * Inits the graph.
     */
    protected final void initGraph() {
        super.initGraph(new DirectedSparseGraph());
    }

    /**
     * Returns true if vertex v is one of the ancestors or the same as
     * vertex p.
     */
    private boolean isAncestor(final Vertex v, final Vertex p) {
        if (p.equals(v)) {
            return true;
        }
        for (final Object pre : p.getPredecessors()) {
            if (isAncestor(v, (Vertex) pre)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if service exists already in the parent ancestor line
     * or parent already has this service as a child.
     */
    public final boolean existsInThePath(final ServiceInfo si,
                                         final ServiceInfo parent) {
        if (parent == null) {
            return true;
        }
        if (si.equals(parent)) {
            return true;
        }
        final Vertex v = getVertex(si);
        if (v == null) {
            return false;
        }
        final Vertex pv = getVertex(parent);
        if (v.isSuccessorOf(pv) || isAncestor(v, pv)) {
            return true;
        }
        return false;
    }

    /**
     * Sets host scores in all ancestors as it is in the si node.
     */
    private void setHomeNodeInAncestors(
                         final ServiceInfo si,
                         final Map<HostInfo, HostScoreInfo> hostScoreInfos) {
        si.setSavedHostScoreInfos(hostScoreInfos);
        final Vertex v = getVertex(si);
        if (v != null) {
            for (final Object pV : v.getPredecessors()) {
                final ServiceInfo pre =
                                    (ServiceInfo) getInfo((Vertex) pV);
                pre.setSavedHostScoreInfos(hostScoreInfos);
                setHomeNodeInAncestors(pre, hostScoreInfos);
            }
        }
    }

    /**
     * Returns heartbeat ids of all services with colocation with this service.
     */
    public final String[] getColocationNeighbours(final ServiceInfo si) {
        final List<String> parentsHbIds = new ArrayList<String>();
        final Vertex v = getVertex(si);
        if (v != null) {
            for (Object pV : v.getPredecessors()) {
                final ServiceInfo psi = (ServiceInfo) getInfo((Vertex) pV);
                parentsHbIds.add(psi.getService().getHeartbeatId());
            }
        }

        return parentsHbIds.toArray(new String [parentsHbIds.size()]);
    }

    /**
     * Returns heartbeat ids from this service info's parents.
     */
    public final String[] getParents(final ServiceInfo si) {
        final List<String> parentsHbIds = new ArrayList<String>();
        final Vertex v = getVertex(si);
        if (v != null) {
            for (Object pV : v.getPredecessors()) {
                final ServiceInfo psi = (ServiceInfo) getInfo((Vertex) pV);
                parentsHbIds.add(psi.getService().getHeartbeatId());
            }
        }
        return parentsHbIds.toArray(new String [parentsHbIds.size()]);
    }

    /**
     * Returns children of the service.
     */
    public final String[] getChildren(final ServiceInfo si) {
        final List<String> childrenHbIds = new ArrayList<String>();
        final Vertex v = getVertex(si);
        if (v != null) {
            for (Object pV : v.getSuccessors()) {
                final ServiceInfo psi = (ServiceInfo) getInfo((Vertex) pV);
                childrenHbIds.add(psi.getService().getHeartbeatId());
            }
        }
        return childrenHbIds.toArray(new String [childrenHbIds.size()]);
    }

    /**
     * Returns all connections from this service.
     */
    public final HbConnectionInfo[] getHbConnections(final ServiceInfo si) {
        final List<HbConnectionInfo> infos = new ArrayList<HbConnectionInfo>();
        final Vertex v = getVertex(si);
        if (v != null) {
            for (Object pV : v.getPredecessors()) {
                MyEdge edge = (MyEdge) ((Vertex) pV).findEdge(v);

                if (edge == null) {
                    edge = (MyEdge) v.findEdge((Vertex) pV);
                }
                if (edge != null) {
                    infos.add(edgeToHbconnectionMap.get(edge));
                }
            }
            for (Object sV : v.getSuccessors()) {
                MyEdge edge = (MyEdge) v.findEdge((Vertex) sV);

                if (edge == null) {
                    edge = (MyEdge) ((Vertex) sV).findEdge(v);
                }
                if (edge != null) {
                    infos.add(edgeToHbconnectionMap.get(edge));
                }
            }
        }
        return infos.toArray(new HbConnectionInfo[infos.size()]);
    }

    /**
     * Sets host scores in all descendants as it is in the si node.
     */
    private void setHomeNodeInDescendants(
                          final ServiceInfo si,
                          final Map<HostInfo, HostScoreInfo> hostScoreInfos) {
        final Vertex v = getVertex(si);
        if (v != null) {
            for (Object pV : v.getSuccessors()) {
                final ServiceInfo pre = (ServiceInfo) getInfo((Vertex) pV);
                pre.setSavedHostScoreInfos(hostScoreInfos);
                setHomeNodeInDescendants(pre, hostScoreInfos);
            }
        }
    }

    /**
     * Sets host scores in all ancestors and descendants as in si node.
     * This is to ensure, that all connected nodes have the same host scores.
     */
    public final void setHomeNode(
                          final ServiceInfo si,
                          final Map<HostInfo, HostScoreInfo> hostScoreInfos) {
        si.setSavedHostScoreInfos(hostScoreInfos);
        setHomeNodeInDescendants(si, hostScoreInfos);
        setHomeNodeInAncestors(si, hostScoreInfos);
    }

    /**
     * Returns id that is used for saving of the vertex positions to a file.
     */
    protected final String getId(final Info i) {
        return "hb=" + i.getId();
    }

    /**
     * adds resource/service to the graph. Adds also edge from the parent.
     * If vertex exists add only the edge. If parent is null add only vertex
     * without edge. Return true if vertex existed in the graph before.
     */
    public final boolean addResource(final ServiceInfo serviceInfo,
                                     final ServiceInfo parent,
                                     final Point2D pos) {

        //vv.stop();
        boolean vertexExists = true;
        Vertex v = getVertex(serviceInfo);
        if (v == null) {
            final SparseVertex sv = new SparseVertex();
            if (pos == null) {
                final Point2D newPos = getSavedPosition(serviceInfo);
                if (newPos == null) {
                    final Point2D.Float max = getFilledGraphSize();
                    final float maxYPos = (float) max.getY();
                    getVertexLocations().setLocation(
                                            sv,
                                            new Point2D.Float(BD_X_POS,
                                                              maxYPos + 40));
                } else {
                    getVertexLocations().setLocation(sv, newPos);
                }
            } else {
                getVertexLocations().setLocation(sv, pos);
            }

            v = getGraph().addVertex(sv);
            putInfoToVertex(serviceInfo, v);
            putVertexToInfo(v, (Info) serviceInfo);
            vertexExists = false;
        }

        if (parent != null) {
            addColocation(parent, serviceInfo);
            addOrder(parent, serviceInfo);
            /* set host score as in parent */
            setHomeNode(parent, parent.getSavedHostScoreInfos());
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              scale();
            }
        });
        return vertexExists;
    }

    /**
     * Adds order constraint from parent to the service.
     */
    public final void addOrder(final ServiceInfo parent,
                               final ServiceInfo serviceInfo) {
        if (parent == null || serviceInfo == null) {
            return;
        }
        final Vertex vP = getVertex(parent);
        final Vertex v = getVertex(serviceInfo);
        if (v == null || vP == null) {
            return;
        }

        ServiceInfo colRsc = null;
        ServiceInfo colWithRsc = null;
        try {
            mEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        MyEdge edge = (MyEdge) vP.findEdge(v);

        if (edge == null) {
            edge = (MyEdge) v.findEdge(vP);
            if (edge != null) {
                /* reversed */
                final HbConnectionInfo hbci = edgeToHbconnectionMap.get(edge);
                if (edgeIsColocationList.contains(edge)) {
                    colRsc = hbci.getServiceInfoRsc();
                    colWithRsc = hbci.getServiceInfoWithRsc();
                    edgeIsColocationList.remove(edge);
                }
                edgeIsOrderList.remove(edge);
                hbconnectionToEdgeMap.remove(hbci);
                edgeToHbconnectionMap.remove(edge);
                getGraph().removeEdge(edge);
                edge = null;
            }
        }

        HbConnectionInfo hbci;
        if (edge == null) {
            hbci = getClusterBrowser().getNewHbConnectionInfo();
            edge = (MyEdge) getGraph().addEdge(new MyEdge(vP, v));
            if (colRsc != null) {
                edgeIsColocationList.add(edge);
                hbci.setServiceInfoRsc(colRsc);
                hbci.setServiceInfoWithRsc(colWithRsc);
            }
            edgeToHbconnectionMap.put(edge, hbci);
            hbconnectionToEdgeMap.put(hbci, edge);
        } else {
            hbci = edgeToHbconnectionMap.get(edge);
        }
        hbci.setServiceInfoParent(parent);
        hbci.setServiceInfoChild(serviceInfo);

        if (!edgeIsOrderList.contains(edge)) {
            edgeIsOrderList.add(edge);
        }
        mEdgeLock.release();
    }

    /**
     * Reverse the edge.
     */
    public final void reverse(final HbConnectionInfo hbConnectionInfo) {
        final Edge e = hbconnectionToEdgeMap.get(hbConnectionInfo);
        if (e != null && edgeIsColocationList.contains(e)) {
            ((MyEdge) e).reverse();
        }
    }

    /**
     * Adds colocation constraint from parent to the service.
     */
    public final void addColocation(final ServiceInfo rsc,
                                    final ServiceInfo withRsc) {
        if (rsc == null || withRsc == null) {
            return;
        }
        final Vertex vRsc = getVertex(rsc);
        final Vertex v = getVertex(withRsc);
        if (v == null || vRsc == null) {
            return;
        }
        try {
            mEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        MyEdge edge = (MyEdge) vRsc.findEdge(v);
        if (edge == null) {
            edge = (MyEdge) v.findEdge(vRsc);
        }
        HbConnectionInfo hbci;
        if (edge == null) {
            hbci = getClusterBrowser().getNewHbConnectionInfo();
            hbci.setServiceInfoRsc(rsc);
            hbci.setServiceInfoWithRsc(withRsc);

            edge = (MyEdge) getGraph().addEdge(new MyEdge(vRsc, v));
            edgeToHbconnectionMap.put(edge, hbci);
            hbconnectionToEdgeMap.put(hbci, edge);
        } else {
            hbci = edgeToHbconnectionMap.get(edge);
        }
        hbci.setServiceInfoRsc(rsc);
        hbci.setServiceInfoWithRsc(withRsc);
        if (!edgeIsColocationList.contains(edge)) {
            edgeIsColocationList.add(edge);
        }
        mEdgeLock.release();
    }

    /**
     * Removes items from order list.
     */
    public final void clearOrderList() {
        try {
            mEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        edgeIsOrderList.clear();
        mEdgeLock.release();
    }

    /**
     * Removes items from colocation list.
     */
    public final void clearColocationList() {
        try {
            mEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        edgeIsColocationList.clear();
        mEdgeLock.release();
    }

    /**
     * Removes all elements in vertexIsPresentList.
     */
    public final void clearVertexIsPresentList() {
        vertexIsPresentList.clear();
    }

    /**
     * Returns label for service vertex.
     */
    protected final String getMainText(final ArchetypeVertex v) {
        String str;
        if (vertexToHostMap.containsKey(v)) {
            str = vertexToHostMap.get(v).toString();
        } else {
            final ServiceInfo si = (ServiceInfo) getInfo((Vertex) v);
            if (si.getService().isRemoved()) {
                str = Tools.getString("HeartbeatGraph.Removing");
            } else if (vertexIsPresentList.contains(v)) {
                str = si.toString();
            } else {
                if (si.getService().isNew()) {
                    str = si.toString();
                } else {
                    str = Tools.getString("HeartbeatGraph.Unconfigured");
                }
            }
        }
        return str;
    }

    /**
     * Returns shape of the service vertex.
     */
    protected final Shape getVertexShape(final Vertex v,
                                   final VertexShapeFactory factory) {
        if (vertexToHostMap.containsKey(v)) {
            return factory.getRectangle(v);
        } else {
            final RoundRectangle2D r = factory.getRoundRectangle(v);

            return new RoundRectangle2D.Double(r.getX(),
                                        r.getY(),
                                        r.getWidth(),
                                        r.getHeight(),
                                        20,
                                        20);
        }
    }

    /**
     * Repaints the graph.
     */
    public final void repaint() {
        getVisualizationViewer().repaint();
    }

    /**
     * Picks info.
     */
    public final void pickInfo(final Info i) {
        final GroupInfo groupInfo = ((ServiceInfo) i).getGroupInfo();
        if (groupInfo == null) {
            super.pickInfo(i);
        } else {
            /* group is picked in the graph, if group service was selected. */
            final Vertex v = getVertex(groupInfo);
            final PickedState ps = getVisualizationViewer().getPickedState();
            ps.clearPickedEdges();
            ps.clearPickedVertices();
            ps.pick(v, true);
        }
    }

    /**
     * Handles right click on the service vertex and creates popup menu.
     */
    protected final JPopupMenu handlePopupVertex(final Vertex v,
                                                 final Point2D p) {
        if (vertexToHostMap.containsKey(v)) {
            final HostInfo hi = (HostInfo) getInfo(v);
            return hi.getPopup(p);
        } else {
            final ServiceInfo si = (ServiceInfo) getInfo(v);
            return si.getPopup(p);
        }
    }

    /**
     * Recreates the add existing service popup, since it can change in
     * very unlikely event.
     */
    private void reloadAddExistingServicePopup(final JMenu addServiceMenuItem,
                                               final Vertex v) {

        if (addServiceMenuItem == null) {
            return;
        }
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        addServiceMenuItem.removeAll();
        boolean separatorAdded = false;
        for (final ServiceInfo asi : si.getExistingServiceList(si)) {
            final MyMenuItem mmi = new MyMenuItem(asi.toString()) {
                private static final long serialVersionUID = 1L;
                public void action() {
                    si.addServicePanel(asi, null, true);
                    repaint();
                }
            };
            if (asi.getStringValue().equals("Filesystem")
                || asi.getStringValue().equals("IPaddr2")) {

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

    /**
     * Handles right click on the edge and creates popup menu.
     */
    protected final JPopupMenu handlePopupEdge(final Edge edge) {
        try {
            mEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final HbConnectionInfo hbci = edgeToHbconnectionMap.get(edge);
        mEdgeLock.release();
        return hbci.getPopup();
    }

    /**
     * Handles right click on the background and creates popup menu.
     */
    protected final JPopupMenu handlePopupBackground(final Point2D pos) {
        return servicesInfo.getPopup(pos);
    }

    /**
     * After service vertex v was released in position pos, set its location
     * there, so that it doesn't jump back. If position is outside of the view,
     * bring the vertex back in the view.
     */
    protected final void vertexReleased(final Vertex v, final Point2D pos) {
        double x = pos.getX();
        double y = pos.getY();
        final double minPos = (getVertexWidth(v)
                               - getDefaultVertexWidth(v)) / 2;
        x = x < minPos ? minPos : x;
        x = x > MAX_X_POS ? MAX_X_POS : x;
        y = y < MIN_Y_POS ? MIN_Y_POS : y;
        y = y > MAX_Y_POS ? MAX_Y_POS : y;
        final Coordinates c = getLayout().getCoordinates(v);
        c.setX(x);
        c.setY(y);
        pos.setLocation(x, y);
        getVertexLocations().setLocation(v, pos);
    }

    /**
     * Selects Info in the view if a service vertex was pressed. If more
     * vertices were selected, it does not do anything.
     */
    protected final void oneVertexPressed(final Vertex v) {
        if (vertexToHostMap.containsKey(v)) {
            final HostInfo hi = vertexToHostMap.get(v);
            hi.setGraph(this);
            getClusterBrowser().setRightComponentInView(hi);
            hi.setGraph(null);
        } else {
            final ServiceInfo si = (ServiceInfo) getInfo(v);
            si.selectMyself();
        }
    }

    /**
     * Is called, when background of the graph is clicked. It deselects
     * selected node.
     */
    protected final void backgroundClicked() {
        servicesInfo.selectMyself();
    }

    /**
     * Returns tool tip when mouse is over a service vertex.
     */
    public final String getVertexToolTip(final Vertex v) {

        if (vertexToHostMap.containsKey(v)) {
            return vertexToHostMap.get(v).getToolTipForGraph();
        }
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        return si.getToolTipText();
    }

    /**
     * Returns the tool tip for the edge.
     */
    public final String getEdgeToolTip(final Edge edge) {
        // TODO: move this to the clusterbrowser
        try {
            mEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final Pair p = edge.getEndpoints();
        final boolean edgeIsOrder = edgeIsOrderList.contains(edge);
        final boolean edgeIsColocation = edgeIsColocationList.contains(edge);
        mEdgeLock.release();

        final Vertex v = (Vertex) p.getSecond();
        final Vertex parent = (Vertex) p.getFirst();
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        final ServiceInfo siP = (ServiceInfo) getInfo(parent);
        final StringBuffer s = new StringBuffer(100);
        String state = "";


        if (edgeIsOrder && edgeIsColocation) {
            state = "colocated & ordered";
        } else if (edgeIsOrder) {
            state = "ordered";
        } else if (edgeIsColocation) {
            state = "colocated";
        }

        s.append(siP.toString());
        if (edgeIsOrder) {
            s.append(" is started before ");
        } else {
            s.append(" and ");
        }
        s.append(si.toString());
        if (!edgeIsOrder) {
            s.append(" are located");
        }
        if (edgeIsColocation) {
            s.append(" on the same host");
        } else {
            s.append(" not necessarily on the same host");
        }

        return "<b>" + state + "</b><br>" + s.toString();
    }

    /**
     * Returns color of the vertex, depending if the service is configured,
     * removed or color of the host.
     */
    protected final Color getVertexFillColor(final Vertex v) {
        if (vertexToHostMap.containsKey(v)) {
            final HostInfo hi = vertexToHostMap.get(v);
            if (!hi.getHost().isHbStatus() || !hi.getHost().isConnected()) {
                return Tools.getDefaultColor(
                                            "HeartbeatGraph.FillPaintUnknown");
            } else {
                return vertexToHostMap.get(v).getHost().getColor();
            }
        }
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        if (getClusterBrowser().allHostsDown()) {
            return Tools.getDefaultColor("HeartbeatGraph.FillPaintUnknown");
        } else if (si.isFailed()) {
            return Tools.getDefaultColor("HeartbeatGraph.FillPaintFailed");
        } else if (si.isStopped()) {
            return Tools.getDefaultColor("HeartbeatGraph.FillPaintStopped");
        } else if (getClusterBrowser().hbStatusFailed()) {
            return Tools.getDefaultColor("HeartbeatGraph.FillPaintUnknown");
        } else if (vertexIsPresentList.contains(v)) {
            return si.getHostColor();
        } else if (!si.getService().isNew()) {
            return Tools.getDefaultColor("HeartbeatGraph.FillPaintRemoved");
        } else {
            return Tools.getDefaultColor(
                                      "HeartbeatGraph.FillPaintUnconfigured");
        }
    }

    /**
     * Returns label that describes the edge.
     */
    protected final String getLabelForEdgeStringer(final ArchetypeEdge e) {
        try {
            mEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final boolean edgeIsOrder = edgeIsOrderList.contains((Edge) e);
        final boolean edgeIsColocation =
                                    edgeIsColocationList.contains((Edge) e);
        final Pair p = ((Edge) e).getEndpoints();
        mEdgeLock.release();
        final ServiceInfo s1 = (ServiceInfo) getInfo((Vertex) p.getSecond());
        final ServiceInfo s2 = (ServiceInfo) getInfo((Vertex) p.getFirst());
        String ret;
        if (edgeIsOrder && edgeIsColocation) {
            ret = Tools.getString("HeartbeatGraph.ColOrd");
        } else if (edgeIsOrder) {
            ret = Tools.getString("HeartbeatGraph.Order");
        } else if (edgeIsColocation) {
            ret = Tools.getString("HeartbeatGraph.Colocation");
        } else if (s1.getService().isNew() || s2.getService().isNew()) {
            ret = Tools.getString("HeartbeatGraph.Unconfigured");
        } else {
            ret = Tools.getString("HeartbeatGraph.Removing");
        }
        return ret;
    }

    /**
     * Returns paint of the edge.
     */
    protected final Paint getEdgeDrawPaint(final Edge e) {
        try {
            mEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        Paint p;
        if (edgeIsOrderList.contains(e) && edgeIsColocationList.contains(e)) {
            p = super.getEdgeDrawPaint(e);
        } else if (edgeIsOrderList.contains(e)
                   || edgeIsColocationList.contains(e)) {
            p = Tools.getDefaultColor(
                                      "ResourceGraph.EdgeDrawPaintBrighter");
        } else {
            p = Tools.getDefaultColor(
                                      "ResourceGraph.EdgeDrawPaintRemoved");
        }
        mEdgeLock.release();
        return p;
    }

    /**
     * Returns paint of the picked edge.
     */
    protected final Paint getEdgePickedPaint(final Edge e) {
        try {
            mEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        Paint p;
        if (edgeIsOrderList.contains(e) && edgeIsColocationList.contains(e)) {
            p = super.getEdgePickedPaint(e);
        } else if (edgeIsOrderList.contains(e)
                   || edgeIsColocationList.contains(e)) {
            p = Tools.getDefaultColor(
                                    "ResourceGraph.EdgePickedPaintBrighter");
        } else {
            p = Tools.getDefaultColor(
                                    "ResourceGraph.EdgePickedPaintRemoved");
        }
        mEdgeLock.release();
        return p;
    }

    /**
     * Remove edges that were marked as not present, meaning there are no
     * colocation nor order constraints.
     */
    public final void killRemovedEdges() {
        try {
            mEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final List<Edge> edges =
                            new ArrayList<Edge>(getGraph().getEdges().size());
        for (Object e : getGraph().getEdges()) {
            edges.add((Edge) e);
        }

        for (int i = 0; i < edges.size(); i++) {
            final Edge e = edges.get(i);
            if (!edgeIsOrderList.contains(e)) {
                final HbConnectionInfo hbci = edgeToHbconnectionMap.get(e);
                hbci.setServiceInfoParent(null);
                hbci.setServiceInfoChild(null);

            }

            if (!edgeIsColocationList.contains(e)) {
                final HbConnectionInfo hbci = edgeToHbconnectionMap.get(e);
                hbci.setServiceInfoRsc(null);
                hbci.setServiceInfoWithRsc(null);
            }

            if (!edgeIsOrderList.contains(e)
                && !edgeIsColocationList.contains(e)) {
                final Pair p = e.getEndpoints();
                final ServiceInfo s1 =
                                (ServiceInfo) getInfo((Vertex) p.getSecond());
                final ServiceInfo s2 =
                                (ServiceInfo) getInfo((Vertex) p.getFirst());
                if (!s1.getService().isNew() && !s2.getService().isNew()) {
                    getGraph().removeEdge(e);
                    final HbConnectionInfo hbci = edgeToHbconnectionMap.get(e);
                    edgeToHbconnectionMap.remove(e);
                    hbconnectionToEdgeMap.remove(hbci);
                }
            }
        }
        mEdgeLock.release();
    }

    /**
     * Remove vertices that were marked as not present.
     */
    public final void killRemovedVertices() {
        /* Make copy first. */
        final List<Vertex> vertices = new ArrayList<Vertex>();
        for (final Object vo : getGraph().getVertices()) {
            vertices.add((Vertex) vo);
        }
        for (final Vertex v : vertices) {
            if (vertexToHostMap.containsKey(v)) {
                continue;
            }
            if (!vertexIsPresentList.contains(v)) {
                final ServiceInfo si = (ServiceInfo) getInfo(v);
                if (!si.getService().isNew()
                    && !getClusterBrowser().hbStatusFailed()) {

                    si.setUpdated(false);
                    getVertexLocations().setLocation(v, null); //TODO: asdf
                    getGraph().removeVertex(v);
                    removeInfo(v);
                    removeVertex(si);
                    si.removeInfo();
                    //TODO: unregister removePopup(v);
                    getVertexToMenus().remove(v);
                    vertexToAddServiceMap.remove(v);
                    vertexToAddExistingServiceMap.remove(v);
                    //TODO: positions are still there
                }
            }
        }
    }

    /**
     * Mark vertex and its edges to be removed later.
     */
    protected final void removeInfo(final Info i) {
        final Vertex v = getVertex(i);
        /* remove edges */

        try {
            mEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final Set inEdges = v.getInEdges();
        final Set outEdges = v.getOutEdges();

        for (final Object e : inEdges) {
            edgeIsOrderList.remove((Edge) e);
            edgeIsColocationList.remove((Edge) e);
        }

        for (final Object e : outEdges) {
            edgeIsOrderList.remove((Edge) e);
            edgeIsColocationList.remove((Edge) e);
        }
        mEdgeLock.release();

        /* remove vertex */
        vertexIsPresentList.remove(v);
        ((ServiceInfo) i).getService().setNew(false);
        resetSavedPosition(i);
        getVertexLocations().reset();
    }

    /**
     * Set vertex as present.
     */
    public final void setVertexIsPresent(final ServiceInfo si) {
        final Vertex v = getVertex(si);
        if (v == null) {
            //Tools.Toolsrror("no vertex associated with service info");
            /* group vertices */
            return;
        }
        vertexIsPresentList.add(v);
    }

    /**
     * Returns an icon for the vertex.
     */
    protected final ImageIcon getIconForVertex(final ArchetypeVertex v) {
        if (vertexToHostMap.containsKey(v)) {
            return HOST_ICON;
        }

        final ServiceInfo si = (ServiceInfo) getInfo((Vertex) v);
        if (si == null) {
            return null;
        }
        if (si.isRunning() && !getClusterBrowser().allHostsDown()) {
            return SERVICE_RUNNING_ICON;
        }
        return SERVICE_NOT_RUNNING_ICON;
    }

    /**
     * Returns whether to show an edge arrow.
     */
    protected final boolean showEdgeArrow(final Edge e) {
        try {
            mEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final boolean show = edgeIsOrderList.contains(e);
        mEdgeLock.release();
        return show;
    }

    /**
     * Reloads popup menus for all services.
     */
    public final void reloadServiceMenus() {
        for (final Object v : getGraph().getVertices()) {
            final JMenu existingServiceMenuItem =
                                vertexToAddExistingServiceMap.get((Vertex) v);
            reloadAddExistingServicePopup(existingServiceMenuItem,
                                          (Vertex) v);
        }
    }

    /**
     * Is called after an edge was pressed.
     */
    protected final void oneEdgePressed(final Edge e) {
        try {
            mEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final HbConnectionInfo hbci = edgeToHbconnectionMap.get(e);
        mEdgeLock.release();
        getClusterBrowser().setRightComponentInView(hbci);
    }

    /**
     * Removes the connection, the order, the colocation or both.
     */
    public final void removeConnection(
                                    final HbConnectionInfo hbConnectionInfo) {
        try {
            mEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final ServiceInfo siP = hbConnectionInfo.getServiceInfoParent();
        final ServiceInfo siC = hbConnectionInfo.getServiceInfoChild();
        final Edge edge = hbconnectionToEdgeMap.get(hbConnectionInfo);
        if (edgeIsOrderList.contains(edge)) {
            mEdgeLock.release();
            siC.removeOrder(siP);
        } else {
            mEdgeLock.release();
        }
        try {
            mEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final ServiceInfo siRsc = hbConnectionInfo.getServiceInfoRsc();
        final ServiceInfo siWithRsc = hbConnectionInfo.getServiceInfoWithRsc();
        if (edgeIsColocationList.contains(edge)) {
            edgeIsOrderList.remove(edge);
            edgeIsColocationList.remove(edge);
            mEdgeLock.release();
            siRsc.removeColocation(siWithRsc);
        } else {
            edgeIsOrderList.remove(edge);
            edgeIsColocationList.remove(edge);
            mEdgeLock.release();
        }
    }

    /**
     * Removes order.
     */
    public final void removeOrder(final HbConnectionInfo hbConnectionInfo) {
        try {
            mEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final ServiceInfo siP = hbConnectionInfo.getServiceInfoParent();
        final ServiceInfo siC = hbConnectionInfo.getServiceInfoChild();
        final Edge edge = hbconnectionToEdgeMap.get(hbConnectionInfo);
        if (edgeIsOrderList.contains(edge)) {
            edgeIsOrderList.remove(edge);
            mEdgeLock.release();
            siC.removeOrder(siP);
        } else {
            mEdgeLock.release();
        }
    }

    /**
     * Adds order.
     */
    public final void addOrder(final HbConnectionInfo hbConnectionInfo) {
        try {
            mEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final ServiceInfo siP = hbConnectionInfo.getServiceInfoParent();
        final ServiceInfo siC = hbConnectionInfo.getServiceInfoChild();
        mEdgeLock.release();
        siC.addOrder(siP);
    }

    /**
     * Removes colocation.
     */
    public final void removeColocation(
                                    final HbConnectionInfo hbConnectionInfo) {
        try {
            mEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final ServiceInfo siRsc = hbConnectionInfo.getServiceInfoRsc();
        final ServiceInfo siWithRsc = hbConnectionInfo.getServiceInfoWithRsc();
        final Edge edge = hbconnectionToEdgeMap.get(hbConnectionInfo);
        if (edgeIsColocationList.contains(edge)) {
            edgeIsColocationList.remove(edge);
            mEdgeLock.release();
            siRsc.removeColocation(siWithRsc);
        } else {
            mEdgeLock.release();
        }
    }

    /**
     * Adds colocation.
     */
    public final void addColocation(final HbConnectionInfo hbConnectionInfo) {
        try {
            mEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final ServiceInfo siRsc = hbConnectionInfo.getServiceInfoRsc();
        final ServiceInfo siWithRsc = hbConnectionInfo.getServiceInfoWithRsc();
        mEdgeLock.release();
        siRsc.addColocation(siWithRsc);
    }

    /**
     * Returns whether this hb connection is order.
     */
    public final boolean isOrder(final HbConnectionInfo hbConnectionInfo) {
        try {
            mEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final Edge edge = hbconnectionToEdgeMap.get(hbConnectionInfo);
        final boolean order = edgeIsOrderList.contains(edge);
        mEdgeLock.release();
        return order;
    }

    /**
     * Returns whether this hb connection is colocation.
     */
    public final boolean isColocation(
                                    final HbConnectionInfo hbConnectionInfo) {
        try {
            mEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final Edge edge = hbconnectionToEdgeMap.get(hbConnectionInfo);
        final boolean colocation = edgeIsColocationList.contains(edge);
        mEdgeLock.release();
        return colocation;
    }

    /**
     * Adds host to the graph.
     */
    public final void addHost(final HostInfo hostInfo) {

        Vertex v = getVertex(hostInfo);
        if (v == null) {
            /* add host vertex */
            final SparseVertex sv = new SparseVertex();
            v = getGraph().addVertex(sv);
            putInfoToVertex(hostInfo, v);
            vertexToHostMap.put(v, hostInfo);
            hostToVertexMap.put(hostInfo, v);
            putVertexToInfo(v, (Info) hostInfo);
            Point2D hostPos = getSavedPosition(hostInfo);
            if (hostPos == null) {
                hostPos = new Point2D.Double(hostDefaultXPos, HOST_Y_POS);
                hostDefaultXPos += HOST_STEP_X;
            }
            getVertexLocations().setLocation(sv, hostPos);
            // TODO: vertexLocations needs locking
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                  scale();
                }
            });
        }
    }

    /**
     * Small text that appears above the icon.
     */
    protected final String getIconText(final Vertex v) {
        if (vertexToHostMap.containsKey(v)) {
            // TODO: running etc
            return null;
        }
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        if (si == null) {
            return null;
        }
        String targetRole = null;
        if (si.isStarted()) {
            if (si.isRunning()) {
                targetRole = null;
            } else if (si.isFailed()) {
                targetRole = "starting failed";
            } else {
                targetRole = "starting...";
            }
        } else if (si.isStopped()) {
            if (si.isRunning()) {
                targetRole = "stopping...";
            } else {
                targetRole = null;
            }
        }
        return targetRole;
    }

    /**
     * Small text that appears in the right corner.
     */
    protected final String getRightCornerText(final Vertex v) {
        if (vertexToHostMap.containsKey(v)) {
            final HostInfo hi = vertexToHostMap.get(v);
            if (hi.getHost().isHbStatus()) {
                return "online";
            } else if (hi.getHost().isConnected()) {
                return "offline";
            }
            return null;
        }
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        if (si != null && !si.isManaged()) {
            return "(unmanaged)";
        }
        return null;
    }

    /**
     * Small text that appears down.
     */
    protected final String[] getSubtexts(final Vertex v) {
        if (vertexToHostMap.containsKey(v)) {
            return vertexToHostMap.get(v).getSubtextsForGraph();
        }
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        if (si == null) {
            return null;
        }
        return si.getSubtextsForGraph();
    }

    /**
     * Returns how much of the disk is used. Probably useful only for disks.
     * -1 for not used or not applicable.
     */
    protected int getUsed(final Vertex v) {
        if (vertexToHostMap.containsKey(v)) {
            return ((HostInfo) getInfo(v)).getUsed();
        }
        return ((ServiceInfo) getInfo(v)).getUsed();
    }

    /**
     * This method draws how much of the vertex is used for something.
     */
    protected void drawInside(final Vertex v,
                              final Graphics2D g2d,
                              final double x,
                              final double y,
                              final Shape shape) {
        if (vertexToHostMap.containsKey(v)) {
            return;
        }
        final double used = getUsed(v);
        if (used > 0) {
            /** Show how much is used. */
            final double height = shape.getBounds().getHeight();
            final double width = shape.getBounds().getWidth();
            final double freeWidth = width * (100 - used) / 100;
            g2d.setColor(new Color(255, 255, 255, 220));
            g2d.fillRoundRect(
                          (int) (x + shape.getBounds().getWidth() - freeWidth),
                          (int) (y + 2),
                          (int) (freeWidth - 1),
                          (int) (height - 4),
                          20,
                          20);
        }
    }

    /**
     * Returns height of the vertex.
     */
    protected final int getDefaultVertexHeight(final Vertex v) {
        return VERTEX_HEIGHT;
    }
}
