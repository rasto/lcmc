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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.awt.geom.Point2D;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.awt.Color;
import java.awt.Paint;

import javax.swing.JPopupMenu;
import javax.swing.JMenu;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

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
    /** Map from vertex to its size. */
    private final Map<Vertex, Integer>vertexSize =
                                               new HashMap<Vertex, Integer>();
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
    private int hostDefaultXPos = 80;
    /** X position of a new block device. */
    private static final int BD_X_POS = 100;
    /** Minimum width of the block device vertex. */
    private static final int MIN_BD_WIDTH = 160;
    /** Y position of the host. */
    private static final int HOST_Y_POS = 40;
    /** Vertical step in pixels by which the hosts are drawn in the graph. */
    private static final int HOST_STEP_X = 330;
    /** Minimum horizontal position. */
    private static final int MIN_X_POS = 40;
    /** Minimum vertical position. */
    private static final int MIN_Y_POS = 20;
    /** Maximum horizontal position. */
    private static final int MAX_X_POS = 2600;
    /** Maximum vertical position. */
    private static final int MAX_Y_POS = 2600;
    /** Height of the vertices. */
    private static final float VERTEX_HEIGHT = 50.0f;
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
        List<String> parentsHbIds = new ArrayList<String>();
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
        List<String> parentsHbIds = new ArrayList<String>();
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
        List<String> childrenHbIds = new ArrayList<String>();
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
        boolean setLocationLater = false;
        if (v == null) {
            final SparseVertex sv = new SparseVertex();
            if (pos == null) {
                final Point2D newPos = getSavedPosition(serviceInfo);
                if (newPos == null) {
                    setLocationLater = true;
                    //final Point2D.Float max = getFilledGraphSize();
                    //final float maxYPos = (float) max.getY();
                    //getVertexLocations().setLocation(
                    //                        sv,
                    //                        new Point2D.Float(BD_X_POS,
                    //                                          maxYPos + 20));
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
            if (setLocationLater) {
                final Point2D.Float max = getFilledGraphSize();
                final float maxYPos = (float) max.getY();
                getLabelForVertexStringer(v);
                getVertexLocations().setLocation(
                    sv,
                    new Point2D.Float(BD_X_POS +
                                      (getVertexSize(v) - MIN_BD_WIDTH) / 2,
                                      maxYPos + 40));
            }
        }

        if (parent != null) {
            addColocation(parent, serviceInfo);
            addOrder(parent, serviceInfo);
            parent.setUpdated(true);
            serviceInfo.setUpdated(true);
            /* set host score as in parent */
            setHomeNode(parent, parent.getSavedHostScoreInfos());
        }
        //SwingUtilities.invokeLater(new Runnable() {
        //    public void run() {
                scale();
        //    }
        //});
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
        MyEdge edge = (MyEdge) vP.findEdge(v);
        //TODO: bug is somewhere here
        if (edge == null) {
            edge = (MyEdge) v.findEdge(vP);
            if (edge != null  && edgeIsColocationList.contains(edge)) {
                /* reversed */
                getGraph().removeEdge(edge);
                addColocation(parent, serviceInfo);
                edge = (MyEdge) vP.findEdge(v);
            }
        }
        if (edge == null) {
            final HbConnectionInfo hbci =
                    getClusterBrowser().getNewHbConnectionInfo(parent,
                                                               serviceInfo);

            edge = (MyEdge) getGraph().addEdge(new MyEdge(vP, v));
            edgeToHbconnectionMap.put(edge, hbci);
            hbconnectionToEdgeMap.put(hbci, edge);
        }
        if (!edgeIsOrderList.contains(edge)) {
            edgeIsOrderList.add(edge);
        }
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
    public final void addColocation(final ServiceInfo parent,
                              final ServiceInfo serviceInfo) {
        if (parent == null || serviceInfo == null) {
            return;
        }
        final Vertex vP = getVertex(parent);
        final Vertex v = getVertex(serviceInfo);
        if (v == null || vP == null) {
            return;
        }
        MyEdge edge = (MyEdge) vP.findEdge(v);
        if (edge == null) {
            edge = (MyEdge) v.findEdge(vP);
            if (edge != null) {
                /* reversed */
                getGraph().removeEdge(edge);
                edgeIsColocationList.remove(edge);
                final HbConnectionInfo hbci = edgeToHbconnectionMap.get(edge);

                edgeToHbconnectionMap.remove(edge);
                hbconnectionToEdgeMap.remove(hbci);
                edge = null;
            }
        }
        if (edge == null) {
            final HbConnectionInfo hbci =
                       getClusterBrowser().getNewHbConnectionInfo(parent,
                                                                  serviceInfo);

            edge = (MyEdge) getGraph().addEdge(new MyEdge(vP, v));
            edgeToHbconnectionMap.put(edge, hbci);
            hbconnectionToEdgeMap.put(hbci, edge);
        }
        edgeIsColocationList.add(edge);
    }

    /**
     * Removes items from order list.
     */
    public final void clearOrderList() {
        edgeIsOrderList.clear();
    }

    /**
     * Removes items from colocation list.
     */
    public final void clearColocationList() {
        edgeIsColocationList.clear();
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
    protected final String getLabelForVertexStringer(final ArchetypeVertex v) {
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
        int size = str.length() * 7 + 50;
        if (size < MIN_BD_WIDTH) {
            size = MIN_BD_WIDTH;
        //} else if (!vertexSize.containsKey((Vertex) v)) {
        //    final Point2D pos = getVertexLocations().getLocation(v);
        //    pos.setLocation(pos.getX() + (size - MIN_BD_WIDTH) * 2,
        //                    pos.getY());
        //    getVertexLocations().setLocation(v, pos);
        }
        vertexSize.put((Vertex) v, size);
        return str;
    }

    /**
     * Returns size of the service vertex ellipse.
     */
    protected final int getVertexSize(final Vertex v) {
        if (vertexSize.containsKey(v)) {
            return vertexSize.get(v);
        } else {
            getLabelForVertexStringer(v);
            return vertexSize.get(v);
        }
    }

    /**
     * Returns aspect ratio of the ellipse of the service vertex.
     */
    protected final float getVertexAspectRatio(final Vertex v) {
        return VERTEX_HEIGHT / getVertexSize(v);
    }

    /**
     * Returns shape of the service vertex.
     */
    protected final Shape getVertexShape(final Vertex v,
                                   final VertexShapeFactory factory) {
        if (vertexToHostMap.containsKey(v)) {
            return factory.getRectangle(v);
        } else {
            return factory.getRoundRectangle(v);
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
        final HbConnectionInfo hbci = edgeToHbconnectionMap.get(edge);
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
        x = x < MIN_X_POS ? MIN_X_POS : x;
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
            return vertexToHostMap.get(v).getToolTipText();
        }
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        return si.getToolTipText();
    }

    /**
     * Returns the tool tip for the edge.
     */
    public final String getEdgeToolTip(final Edge edge) {
        // TODO: move this to the clusterbrowser
        final Pair p = edge.getEndpoints();
        final Vertex v = (Vertex) p.getSecond();
        final Vertex parent = (Vertex) p.getFirst();
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        final ServiceInfo siP = (ServiceInfo) getInfo(parent);
        final StringBuffer s = new StringBuffer(100);
        String state = "";


        final boolean edgeIsOrder = edgeIsOrderList.contains(edge);
        final boolean edgeIsColocation = edgeIsColocationList.contains(edge);

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
            if (!hi.getHost().isHbStatus()) {
                return Tools.getDefaultColor(
                                            "HeartbeatGraph.FillPaintUnknown");
            } else {
                return vertexToHostMap.get(v).getHost().getColor();
            }
        }
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        if (si.isFailed()) {
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
        final boolean edgeIsOrder = edgeIsOrderList.contains((Edge) e);
        final boolean edgeIsColocation =
                                    edgeIsColocationList.contains((Edge) e);
        final Pair p = ((Edge) e).getEndpoints();
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
        if (edgeIsOrderList.contains(e) && edgeIsColocationList.contains(e)) {
            return super.getEdgeDrawPaint(e);
        } else if (edgeIsOrderList.contains(e)
                   || edgeIsColocationList.contains(e)) {
            return Tools.getDefaultColor(
                                      "ResourceGraph.EdgeDrawPaintBrighter");
        } else {
            return Tools.getDefaultColor(
                                      "ResourceGraph.EdgeDrawPaintRemoved");
        }
    }

    /**
     * Returns paint of the picked edge.
     */
    protected final Paint getEdgePickedPaint(final Edge e) {
        if (edgeIsOrderList.contains(e) && edgeIsColocationList.contains(e)) {
            return super.getEdgePickedPaint(e);
        } else if (edgeIsOrderList.contains(e)
                   || edgeIsColocationList.contains(e)) {
            return Tools.getDefaultColor(
                                    "ResourceGraph.EdgePickedPaintBrighter");
        } else {
            return Tools.getDefaultColor(
                                    "ResourceGraph.EdgePickedPaintRemoved");
        }
    }

    /**
     * Remove edges that were marked as not present, meaning there are no
     * colocation nor order constraints.
     */
    public final void killRemovedEdges() {
        final List<Edge> edges =
                            new ArrayList<Edge>(getGraph().getEdges().size());
        for (Object e : getGraph().getEdges()) {
            edges.add((Edge) e);
        }

        for (int i = 0; i < edges.size(); i++) {
            final Edge e = edges.get(i);
            if (!edgeIsOrderList.contains(e)
                && !edgeIsColocationList.contains(e)) {
                final Pair p = e.getEndpoints();
                final ServiceInfo s1 =
                                (ServiceInfo) getInfo((Vertex) p.getSecond());
                final ServiceInfo s2 =
                                (ServiceInfo) getInfo((Vertex) p.getFirst());
                if (!s1.getService().isNew() && !s2.getService().isNew()) {
                    getGraph().removeEdge(e);
                }
            }
        }
    }

    /**
     * Remove vertices that were marked as not present.
     */
    public final void killRemovedVertices() {
        /* Make copy first. */
        List<Vertex> vertices = new ArrayList<Vertex>();
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
        if (si.isStarted()) {
            return SERVICE_RUNNING_ICON;
        } else if (si.isStopped()) {
            return SERVICE_NOT_RUNNING_ICON;
        }
        final String node =
                        ((ServiceInfo) getInfo((Vertex) v)).getRunningOnNode();
        if (node == null) {
            return SERVICE_NOT_RUNNING_ICON;
        }
        return SERVICE_RUNNING_ICON;
    }

    /**
     * Returns whether to show an edge arrow.
     */
    protected final boolean showEdgeArrow(final Edge e) {
        return edgeIsOrderList.contains(e);
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
        final HbConnectionInfo hbci = edgeToHbconnectionMap.get(e);
        getClusterBrowser().setRightComponentInView(hbci);
    }

    /**
     * Removes the connection, the order, the colocation or both.
     */
    public final void removeConnection(
                                    final HbConnectionInfo hbConnectionInfo) {
        final ServiceInfo siP = hbConnectionInfo.getServiceInfoParent();
        final ServiceInfo si = hbConnectionInfo.getServiceInfo();
        final Edge edge = hbconnectionToEdgeMap.get(hbConnectionInfo);
        if (edgeIsOrderList.contains(edge)) {
            si.removeOrder(siP);
        }
        if (edgeIsColocationList.contains(edge)) {
            si.removeColocation(siP);
        }
        edgeIsOrderList.remove(edge);
        edgeIsColocationList.remove(edge);
        hbconnectionToEdgeMap.remove(hbconnectionToEdgeMap);
        edgeToHbconnectionMap.remove(edge);
    }

    /**
     * Removes order.
     */
    public final void removeOrder(final HbConnectionInfo hbConnectionInfo) {
        final ServiceInfo siP = hbConnectionInfo.getServiceInfoParent();
        final ServiceInfo si = hbConnectionInfo.getServiceInfo();
        final Edge edge = hbconnectionToEdgeMap.get(hbConnectionInfo);
        if (edgeIsOrderList.contains(edge)) {
            edgeIsOrderList.remove(edge);
            si.removeOrder(siP);
            if (!edgeIsColocationList.contains(edge)) {
                hbconnectionToEdgeMap.remove(hbConnectionInfo);
                edgeToHbconnectionMap.remove(edge);
            }
        }
    }

    /**
     * Adds order.
     */
    public final void addOrder(final HbConnectionInfo hbConnectionInfo) {
        final ServiceInfo siP = hbConnectionInfo.getServiceInfoParent();
        final ServiceInfo si = hbConnectionInfo.getServiceInfo();
        si.addOrder(siP);
    }

    /**
     * Removes colocation.
     */
    public final void removeColocation(
                                    final HbConnectionInfo hbConnectionInfo) {
        final ServiceInfo siP = hbConnectionInfo.getServiceInfoParent();
        final ServiceInfo si = hbConnectionInfo.getServiceInfo();
        final Edge edge = hbconnectionToEdgeMap.get(hbConnectionInfo);
        if (edgeIsColocationList.contains(edge)) {
            edgeIsColocationList.remove(edge);
            si.removeColocation(siP);
            if (!edgeIsOrderList.contains(edge)) {
                hbconnectionToEdgeMap.remove(hbConnectionInfo);
                edgeToHbconnectionMap.remove(edge);
            }
        }
    }

    /**
     * Adds colocation.
     */
    public final void addColocation(final HbConnectionInfo hbConnectionInfo) {
        final ServiceInfo siP = hbConnectionInfo.getServiceInfoParent();
        final ServiceInfo si = hbConnectionInfo.getServiceInfo();
        siP.addColocation(si);
    }

    /**
     * Returns whether this hb connection is order.
     */
    public final boolean isOrder(final HbConnectionInfo hbConnectionInfo) {
        final Edge edge = hbconnectionToEdgeMap.get(hbConnectionInfo);
        return edgeIsOrderList.contains(edge);
    }

    /**
     * Returns whether this hb connection is colocation.
     */
    public final boolean isColocation(
                                    final HbConnectionInfo hbConnectionInfo) {
        final Edge edge = hbconnectionToEdgeMap.get(hbConnectionInfo);
        return edgeIsColocationList.contains(edge);
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
            //SwingUtilities.invokeLater(new Runnable() {
            //    public void run() {
                    scale();
            //    }
            //});
        }
    }

    /**
     * Small text that appears above the icon.
     */
    protected String getIconText(final Vertex v) {
        if (vertexToHostMap.containsKey(v)) {
            // TODO: running etc
            return null;
        }
        final ServiceInfo si = (ServiceInfo)getInfo(v);
        String targetRole = null; 
        if (si.isStarted()) {
            targetRole = "started";
        } else if (si.isStopped()) {
            targetRole = "stopped";
        }
        return targetRole;
    }

    /**
     * Small text that appears in the right corner.
     */
    protected String getRightCornerText(final Vertex v) {
        if (vertexToHostMap.containsKey(v)) {
            return null;
        }
        final ServiceInfo si = (ServiceInfo)getInfo(v);
        if (!si.isManaged()) {
            return "(unmanaged)";
        }
        return null;
    }

    /**
     * Small text that appears down.
     */
    protected String getSubtext(final Vertex v) {
        if (vertexToHostMap.containsKey(v)) {
            return null;
        }
        final ServiceInfo si = (ServiceInfo)getInfo(v);
        if (si.isFailed()) {
            return "Failed";
        } else if (si.isStopped()) {
            return "not running";
        }
        String runningOnNode = si.getRunningOnNode(); 
        if (runningOnNode != null) {
            return "running on: " + runningOnNode;
        }
        return "";
    }
}
