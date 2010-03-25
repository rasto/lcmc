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
import drbd.data.Host;
import drbd.data.Subtext;
import drbd.data.resources.BlockDevice;
import drbd.gui.resources.HostDrbdInfo;
import drbd.gui.resources.DrbdInfo;
import drbd.gui.resources.DrbdResourceInfo;
import drbd.gui.resources.BlockDevInfo;
import drbd.gui.resources.Info;

import java.awt.Shape;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.Paint;
import java.awt.Color;
import java.awt.BasicStroke;

import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.impl.SparseVertex;
import edu.uci.ics.jung.graph.impl.DirectedSparseGraph;
import edu.uci.ics.jung.visualization.VertexShapeFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import javax.swing.JPopupMenu;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

/**
 * This class creates graph and provides methods to add new block device
 * vertices and drbd resource edges, remove or modify them.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class DrbdGraph extends ResourceGraph {
    /** Map from vertex to host. */
    private final Map<Vertex, HostDrbdInfo>vertexToHostMap =
                                 new LinkedHashMap<Vertex, HostDrbdInfo>();
    /** Map from host to vertex. */
    private final Map<HostDrbdInfo, Vertex>hostToVertexMap =
                                 new LinkedHashMap<HostDrbdInfo, Vertex>();
    /** Map from block device info object to vertex. */
    private final Map<BlockDevInfo, Vertex>bdiToVertexMap =
                                 new LinkedHashMap<BlockDevInfo, Vertex>();
    /** Map from block device to vertex. */
    private final Map<BlockDevice, Vertex>blockDeviceToVertexMap =
                                 new LinkedHashMap<BlockDevice, Vertex>();
    /** Map from host to the list of block devices. */
    private final Map<HostDrbdInfo, List<Vertex>>hostBDVerticesMap =
                               new LinkedHashMap<HostDrbdInfo, List<Vertex>>();
    /** Map from graph edge to the drbd resource info object. */
    private final Map<Edge, DrbdResourceInfo>edgeToDrbdResourceMap =
                                 new LinkedHashMap<Edge, DrbdResourceInfo>();
    /** Map from drbd resource info object to the graph edge. */
    private final Map<DrbdResourceInfo, Edge>drbdResourceToEdgeMap =
                                 new LinkedHashMap<DrbdResourceInfo, Edge>();

    /** Drbd info object to which this graph belongs. */
    private DrbdInfo drbdInfo;
    ///** Old location of the moved vertex. */
    //private double oldLocation;

    /** Horizontal step in pixels by which the block devices are drawn in
     * the graph. */
    private static final int BD_STEP_Y = 55;
    /** Y position of the host. */
    private static final int HOST_Y_POS = 40;
    /** Vertical step in pixels by which the hosts are drawn in the graph. */
    private static final int HOST_STEP_X = 280;
    /** Block device vertex size. */
    private static final int VERTEX_SIZE_BD = 200;
    /** Host vertex size. */
    private static final int VERTEX_SIZE_HOST = 150;
    /** Height of the host vertices. */
    private static final int HOST_VERTEX_HEIGHT = 50;
    /** Height of the block device vertices. */
    private static final int VERTEX_HEIGHT = 50;

    /** Maximum length of the label in the vertex, after which the string will
     * be cut. */
    private static final int MAX_VERTEX_STRING_LENGTH = 18;
    /** Maximum length of the label in the edge, after which the string will
     * be cut. */
    private static final int MAX_EDGE_STRING_LENGTH = 10;
    /** String length after the cut. */
    private static final int EDGE_STRING_LENGTH = 7;
    /** Postion offset of block devices from the host x position. */
    private static final int BD_X_OFFSET = 15;

    /** The first X position of the host. */
    private int hostDefaultXPos = 10;

    /**
     * Prepares a new <code>DrbdGraph</code> object.
     */
    public DrbdGraph(final ClusterBrowser clusterBrowser) {
        super(clusterBrowser);
    }

    /**
     * Inits the graph.
     */
    protected final void initGraph() {
        super.initGraph(new DirectedSparseGraph());
    }

    /**
     * Sets drbd info object.
     */
    public final void setDrbdInfo(final DrbdInfo drbdInfo) {
        this.drbdInfo = drbdInfo;
    }

    /**
     * Returns drbd info object.
     */
    public final DrbdInfo getDrbdInfo() {
        return drbdInfo;
    }

    /**
     * Returns whether vertex is block device.
     */
    private boolean isVertexBlockDevice(final Vertex v) {
        return vertexToHostMap.get(v) != getInfo(v);
    }

    /**
     * Adds host with all its block devices to the graph.
     */
    public final void addHost(final HostDrbdInfo hostDrbdInfo) {
        Vertex v = getVertex(hostDrbdInfo);
        if (v == null) {
            /* add host vertex */
            final SparseVertex sv = new SparseVertex();
            v = getGraph().addVertex(sv);
            somethingChanged();
            putInfoToVertex(hostDrbdInfo, v);
            vertexToHostMap.put(v, hostDrbdInfo);
            hostToVertexMap.put(hostDrbdInfo, v);
            putVertexToInfo(v, (Info) hostDrbdInfo);
            // TODO: get saved position is disabled at the moment,
            // because it does more harm than good at the moment.
            Point2D hostPos = null; // getSavedPosition(hostDrbdInfo);

            if (hostPos == null) {
                hostPos = new Point2D.Double(
                                hostDefaultXPos + getDefaultVertexWidth(v) / 2,
                                HOST_Y_POS);
                hostDefaultXPos += HOST_STEP_X;
            }
            //final double hostXPos =
            //                    hostPos.getX() - getDefaultVertexWidth(v) / 2;
            getVertexLocations().setLocation(sv, hostPos);
        }
        /* add block devices vertices */
        final Host host = hostDrbdInfo.getHost();
        final Point2D hostPos = getVertexLocations().getLocation(v);
        final double hostXPos = hostPos.getX() - getDefaultVertexWidth(v) / 2;
        int devYPos = HOST_Y_POS + BD_STEP_Y;
        List<Vertex> vertexList = hostBDVerticesMap.get(hostDrbdInfo);
        List<Vertex> oldVertexList = null;
        if (vertexList == null) {
            vertexList = new ArrayList<Vertex>();
            hostBDVerticesMap.put(hostDrbdInfo, vertexList);
        } else {
            oldVertexList = new ArrayList<Vertex>(vertexList);
        }
        final List<BlockDevInfo> blockDevInfos =
                                        host.getBrowser().getBlockDevInfos();
        if (oldVertexList != null) {
            for (final Vertex vertex : oldVertexList) {
                final BlockDevInfo bdi = (BlockDevInfo) getInfo(vertex);
                if (!blockDevInfos.contains(bdi)) {
                    /* removing */
                    final Vertex bdv = bdiToVertexMap.get(bdi);
                    final DrbdResourceInfo dri = bdi.getDrbdResourceInfo();
                    if (dri != null) {
                        removeDrbdResource(dri);
                        dri.removeFromHashes();
                    }
                    getGraph().removeVertex(bdv);
                    removeInfo(bdv);
                    removeVertex(bdi);
                    getVertexToMenus().remove(bdv);
                    bdiToVertexMap.remove(bdi);
                    blockDeviceToVertexMap.remove(bdi.getBlockDevice());
                    vertexToHostMap.remove(bdv);
                    vertexList.remove(bdv);
                    somethingChanged();
                }
            }
        }
        for (final BlockDevInfo bdi : blockDevInfos) {
            if (!blockDeviceToVertexMap.containsKey(bdi.getBlockDevice())) {
                final SparseVertex bdsv = new SparseVertex();
                final Vertex bdv = getGraph().addVertex(bdsv);
                somethingChanged();
                bdiToVertexMap.put(bdi, bdv);
                blockDeviceToVertexMap.put(bdi.getBlockDevice(), bdv);
                putVertexToInfo(bdv, (Info) bdi);
                putInfoToVertex(bdi, bdv);
                vertexToHostMap.put(bdv, hostDrbdInfo);
                vertexList.add(bdv);
                // TODO: get saved position is disabled at the moment,
                // because it does more harm than good at the moment.
            }
            final Vertex bdv = blockDeviceToVertexMap.get(
                                                    bdi.getBlockDevice());
            Point2D pos = null; // getSavedPosition(bdi);
            if (pos == null) {
                pos = new Point2D.Double(
                    hostXPos + BD_X_OFFSET + getDefaultVertexWidth(bdv) / 2,
                    devYPos);
            }
            getVertexLocations().setLocation(bdv, pos);
            devYPos += BD_STEP_Y;
        }
    }

    /**
     * Removes drbd resource from the graph.
     */
    public final void removeDrbdResource(final DrbdResourceInfo dri) {
        final MyEdge e = (MyEdge) drbdResourceToEdgeMap.get(dri);
        e.reset();
        getGraph().removeEdge(e);
    }

    /**
     * Returns an icon for vertex, depending on if it is host or block device,
     * if it is started or stopped and so on.
     */
    protected final List<ImageIcon> getIconsForVertex(final ArchetypeVertex v,
                                                      final boolean testOnly) {
        final List<ImageIcon> icons = new ArrayList<ImageIcon>();
        if (isVertexBlockDevice((Vertex) v)) {
            final BlockDevInfo bdi = (BlockDevInfo) getInfo((Vertex) v);
            icons.add(BlockDevInfo.HARDDISK_ICON_LARGE);
            if (bdi.isDiskless(testOnly)) {
                icons.add(BlockDevInfo.NO_HARDDISK_ICON_LARGE);
                return icons;
            } else {
                return icons;
            }
        } else {
            final HostDrbdInfo hi = vertexToHostMap.get(v);
            if (hi.getHost().isDrbdStatus()
                && hi.getHost().isDrbdLoaded()) {
                icons.add(HostBrowser.HOST_ON_ICON_LARGE);
            } else {
                icons.add(HostBrowser.HOST_ICON_LARGE);
            }
            return icons;
        }
    }

    /**
     * Returns label for drbd resource edge. If it is longer than 10
     * characters, it is shortened.
     */
    protected final String getLabelForEdgeStringer(final ArchetypeEdge e) {
        final DrbdResourceInfo dri = edgeToDrbdResourceMap.get(e);
        if (dri != null) {
            final MyEdge edge = (MyEdge) e;
            final Vertex source = edge.getSource();
            final Vertex dest = edge.getDest();
            final BlockDevInfo sourceBDI = (BlockDevInfo) getInfo(source);
            final BlockDevInfo destBDI = (BlockDevInfo) getInfo(dest);
            final BlockDevice sourceBD = sourceBDI.getBlockDevice();
            final BlockDevice destBD = destBDI.getBlockDevice();
            final boolean tOnly = isTestOnly();
            if (!destBDI.isConnected(tOnly)) {
                if (sourceBDI.isWFConnection(tOnly)
                    && !destBDI.isWFConnection(tOnly)) {
                    edge.setDirection(dest, source);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            repaint();
                        }
                    });

                }
            } else if (!sourceBD.isPrimary() && destBD.isPrimary()) {
                edge.setDirection(dest, source);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        repaint();
                    }
                });
            }

            final StringBuffer l = new StringBuffer(dri.getName());
            if (l != null) {
                if (l.length() > MAX_EDGE_STRING_LENGTH) {
                    l.delete(0, l.length() - EDGE_STRING_LENGTH);
                    l.insert(0, "...");
                }
                if (dri.isSyncing()) {
                    String syncedProgress = dri.getSyncedProgress();
                    if (syncedProgress == null) {
                        syncedProgress = "?.?";
                    }
                    final double sourceX =
                            getVertexLocations().getLocation(source).getX();
                    final double destX =
                                getVertexLocations().getLocation(dest).getX();
                    if (sourceBD.isPausedSync() || destBD.isPausedSync()) {
                        l.append(" (" + syncedProgress + "% paused)");
                    } else if (sourceBD.isSyncSource() && sourceX < destX
                               || destBD.isSyncSource() && sourceX > destX) {
                        l.append(" (" + syncedProgress + "% ->)");
                    } else {
                        l.append(" (<- " + syncedProgress + "%)");
                    }
                } else if (dri.isSplitBrain()) {
                    l.append(" (split-brain)");
                } else if (!dri.isConnected(tOnly)) {
                    l.append(" (disconnected)");
                } else if (dri.isVerifying()) {
                    l.append(" (verify)");
                }
                return l.toString();
            }
        }
        return null;
    }

    /**
     * Small text that appears above the icon.
     */
    protected final String getIconText(final Vertex v, final boolean testOnly) {
        if (isVertexBlockDevice(v)) {
            final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
            if (bdi != null) {
                return bdi.getIconTextForGraph(testOnly);
            }
        } else {
            vertexToHostMap.get(v).getIconTextForDrbdGraph(testOnly);
        }
        return null;
    }

    /**
     * Small text that appears in the right corner.
     */
    protected final Subtext getRightCornerText(final Vertex v,
                                               final boolean testOnly) {
        if (isVertexBlockDevice(v)) {
            final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
            if (bdi != null) {
                return bdi.getRightCornerTextForDrbdGraph(testOnly);

            }
        } else {
            return vertexToHostMap.get(v).getRightCornerTextForDrbdGraph(
                                                                      testOnly);
        }
        return null;
    }

    /**
     * Small text that appears down.
     */
    protected final Subtext[] getSubtexts(final Vertex v,
                                          final boolean testOnly) {
        if (isVertexBlockDevice(v)) {
            final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
            if (bdi != null && bdi.getBlockDevice().isDrbd()
                && bdi.getBlockDevice().getConnectionState() != null
                && bdi.getBlockDevice().getDiskState() != null) {
                return new Subtext[]{
                    new Subtext(bdi.getBlockDevice().getConnectionState()
                                     + " / "
                                     + bdi.getBlockDevice().getDiskState(),
                                null)};
            }
        } else {
            return vertexToHostMap.get(v).getSubtextsForDrbdGraph(testOnly);
        }
        return null;
    }

    /**
     * Returns label for block device vertex. If it is longer than 23
     * characters, it is shortened.
     */
    protected final String getMainText(final ArchetypeVertex v,
                                       final boolean testOnly) {
        if (isVertexBlockDevice((Vertex) v)) {
            String l;
            if (isVertexDrbd((Vertex) v)) {
                final BlockDevInfo bdi = (BlockDevInfo) getInfo((Vertex) v);
                l = bdi.getDrbdResourceInfo().getDevice();
            } else {
                l = getInfo((Vertex) v).getName();
            }
            if (l.length() > MAX_VERTEX_STRING_LENGTH) {
                l = "..." + l.substring(l.length()
                                        - MAX_VERTEX_STRING_LENGTH + 3,
                                        l.length());
            }
            return l;
        } else if (vertexToHostMap.containsKey(v)) {
            return vertexToHostMap.get(v).toString();
        } else {
            return "";
        }
    }

    /**
     * Returns shape of the block device vertex.
     */
    protected final Shape getVertexShape(final Vertex v,
                                         final VertexShapeFactory factory) {
        return factory.getRectangle(v);
    }

    /**
     * Handles popup in when block device vertex is clicked.
     */
    protected final JPopupMenu handlePopupVertex(final Vertex v,
                                                 final Point2D p) {
        if (isVertexBlockDevice(v)) {
            final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
            return bdi.getPopup();
        } else {
            /* host */
            final HostDrbdInfo hi = (HostDrbdInfo) getInfo(v);
            return hi.getPopup();
        }
    }

    /**
     * Adds drbd resource edge to the graph.
     */
    public final void addDrbdResource(final DrbdResourceInfo dri,
                                      final BlockDevInfo bdi1,
                                      final BlockDevInfo bdi2) {
        if (bdi1 != null && bdi2 != null) {
            final Vertex v1 = bdiToVertexMap.get(bdi1);
            final Vertex v2 = bdiToVertexMap.get(bdi2);
            if (v1.findEdge(v2) != null || v2.findEdge(v1) != null) {
                return;
            }
            final MyEdge edge = new MyEdge(v1, v2);
            final Edge e = getGraph().addEdge(edge);
            edgeToDrbdResourceMap.put(e, dri);
            drbdResourceToEdgeMap.put(dri, e);
        }
    }

    /**
     * Returns the source block device in a drbd connection.
     */
    public final BlockDevInfo getSource(final DrbdResourceInfo dri) {
        final Edge edge = drbdResourceToEdgeMap.get(dri);
        final Vertex source = ((MyEdge) edge).getSource();
        return (BlockDevInfo) getInfo(source);
    }

    /**
     * Returns the destination block device in a drbd connection.
     */
    public final BlockDevInfo getDest(final DrbdResourceInfo dri) {
        final Edge edge = drbdResourceToEdgeMap.get(dri);
        if (edge == null) {
            return null;
        }
        final Vertex dest = ((MyEdge) edge).getDest();
        return (BlockDevInfo) getInfo(dest);
    }

    /**
     * Picks vertex, that is associated with the specified info object.
     */
    public final void pickInfo(final Info i) {
        final Edge e = drbdResourceToEdgeMap.get(i);
        if (e == null) {
            super.pickInfo(i);
        } else {
            pickEdge(e);
        }
    }

    /**
     * Is called after right click on the resource edge.
     */
    protected final JPopupMenu handlePopupEdge(final Edge edge) {
        final DrbdResourceInfo dri = edgeToDrbdResourceMap.get(edge);
        return dri.getPopup();
    }


    /**
     * Is called after right click on the background and it returns
     * background popup menu.
     */
    protected final JPopupMenu handlePopupBackground(final Point2D pos) {
        super.handlePopupBackground(pos);
        return null;
    }

    ///**
    // * Fixes locations of the block device vertices after they where moved.
    // * TODO: fix for more moved vertices.
    // */
    //private void fixLocations(final Vertex vertex,
    //                          final Point2D newLocation) {
    //    final double oldY = oldLocation;
    //    final double newY = newLocation.getY();
    //    final HostDrbdInfo hi = vertexToHostMap.get(vertex);
    //    final PickedState ps = getVisualizationViewer().getPickedState();
    //    final Point2D hl = getVertexLocations().getLocation(getVertex(hi));
    //    final double x = hl.getX() + BD_X_OFFSET;
    //    for (final Vertex v : hostBDVerticesMap.get(hi)) {
    //        if (!v.equals(vertex) && !ps.isPicked(v)) {
    //            final Point2D l = getVertexLocations().getLocation(v);
    //            final double y = l.getY();
    //            if (oldY >= 0) {
    //                if (y >= oldY && y <= newY) {
    //                    l.setLocation(x, y - BD_STEP_Y);
    //                    getVertexLocations().setLocation(v, l);
    //                } else if (y <= oldY && y >= newY) {
    //                    l.setLocation(x, y + BD_STEP_Y);
    //                    getVertexLocations().setLocation(v, l);
    //                }
    //            } else {
    //                l.setLocation(x, y);
    //                getVertexLocations().setLocation(v, l);
    //            }
    //        }
    //    }
    //}

    /**
     * Picks vertex representig specified block device info object in the
     * graph.
     */
    public final void pickBlockDevice(final BlockDevInfo bdi) {
        final Vertex v = bdiToVertexMap.get(bdi);
        pickVertex(v);
        bdi.selectMyself();
    }

    /**
     * Is called of a host is picked. Its terminal panel is set to view.
     */
    private void pickHost(final Vertex v) {
        pickVertex(v);
        final HostDrbdInfo hi = vertexToHostMap.get(v);
        Tools.getGUIData().setTerminalPanel(hi.getHost().getTerminalPanel());
    }

    /**
     * Is called when one block device vertex was pressed.
     */
    protected final void oneVertexPressed(final Vertex v) {
        if (isVertexBlockDevice(v)) {
            final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
            drbdInfo.setSelectedNode(bdi);
            drbdInfo.selectMyself();
            getClusterBrowser().setRightComponentInView(bdi);
            //oldLocation = getVertexLocations().getLocation(v).getY();
        } else {
            pickHost(v);
            //oldLocation = getVertexLocations().getLocation(v).getY();
            final HostDrbdInfo hi = vertexToHostMap.get(v);
            getClusterBrowser().setRightComponentInView(hi);
        }
    }

    /**
     * Is called when block device vertex is released.
     */
    protected final void vertexReleased(final Vertex v, final Point2D pos) {
        // TODO: make it work
    }
    //protected final void vertexReleased(final Vertex v, final Point2D pos) {
    //    double y = pos.getY();
    //    double x = pos.getX();
    //    final HostDrbdInfo hi = vertexToHostMap.get(v);
    //    final Vertex hostVertex = getVertex(hi);
    //    if (hostVertex.equals(v)) {
    //        getVertexLocations().setLocation(v, pos);

    //        final Point2D hl = getVertexLocations().getLocation(v);
    //        double hx = hl.getX();
    //        double hy = hl.getY();
    //        hy = hy < 0 ? 0 : hy;
    //        hy = hy > MAX_Y_POS ? MAX_Y_POS : hy;
    //        hx = hx < 0 ? 0 : hx;
    //        hl.setLocation(hx, hy);
    //        getVertexLocations().setLocation(v, hl);

    //        for (Vertex vb : hostBDVerticesMap.get(hi)) {
    //            final Point2D l = getVertexLocations().getLocation(vb);
    //            final double yb = l.getY();
    //            l.setLocation(hx + BD_X_OFFSET, hy - oldLocation + yb);
    //            getVertexLocations().setLocation(vb, l);
    //        }
    //    } else {
    //        final PickedState ps = getVisualizationViewer().getPickedState();
    //        for (final Object vo : ps.getPickedVertices()) {
    //            final Vertex vertex = (Vertex) vo;
    //            x = getVertexLocations().getLocation(hostVertex).getX()
    //                + BD_X_OFFSET;
    //            final double minY =
    //                    getVertexLocations().getLocation(hostVertex).getY()
    //                    + BD_STEP_Y;

    //            y = y < minY ? minY : y;
    //            y = Math.floor((y - minY + BD_STEP_Y / 2) / BD_STEP_Y)
    //                           * BD_STEP_Y + minY;
    //            y = y > MAX_Y_POS ? MAX_Y_POS : y;
    //            final Coordinates c = getLayout().getCoordinates(vertex);
    //            c.setX(x);
    //            c.setY(y);
    //            pos.setLocation(x, y);
    //            getVertexLocations().setLocation(vertex, pos);
    //            fixLocations(vertex, pos);
    //        }
    //    }
    //}

    /**
     * Returns whether block device belonging to this vertex is available for
     * drbd or not. Returns false if this is not a block device.
     */
    private boolean isVertexAvailable(final Vertex v) {
        final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
        if (bdi != null) {
            return bdi.getBlockDevice().isAvailable();
        }
        return false;
    }

    /**
     * Returns true if block device represented by specified vertex is
     * drbd device.
     */
    private boolean isVertexDrbd(final Vertex v) {
        final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
        if (bdi != null) {
            return bdi.getBlockDevice().isDrbd();
        }
        return false;
    }

    /**
     * Returns true if block device represented by specified vertex is
     * primary.
     */
    private boolean isVertexPrimary(final Vertex v) {
        final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
        if (bdi != null) {
            return bdi.getBlockDevice().isPrimary();
        }
        return false;
    }

    /**
     * Returns true if block device represented by specified vertex is
     * secondary.
     */
    private boolean isVertexSecondary(final Vertex v) {
        final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
        if (bdi != null) {
            return bdi.getBlockDevice().isSecondary();
        }
        return false;
    }

    /**
     * Is called when resource edge is pressed. It selects the asspociated
     * resource.
     */
    protected final void oneEdgePressed(final Edge e) {
        edgeToDrbdResourceMap.get(e).selectMyself();
    }

    /**
     * Is called, when background of the graph is clicked. It deselects
     * selected node.
     */
    protected final void backgroundClicked() {
        drbdInfo.setSelectedNode(null);
        drbdInfo.selectMyself();
    }

    /**
     * Returns fill color as paint object for for specified block device
     * vertex.
     */
    protected final Color getVertexFillColor(final Vertex v) {

        final HostDrbdInfo hi = vertexToHostMap.get(v);
        final Vertex hostVertex = getVertex(hi);
        if (v.equals(hostVertex)) {
            /* host */
            return hi.getHost().getDrbdColors()[0];
        } else if (hi.getHost() == null
                   || (!hi.getHost().isDrbdStatus()
                       && hi.getHost().isDrbdLoaded())) {
            return Tools.getDefaultColor("DrbdGraph.FillPaintUnknown");
        } else {
            if (!isVertexDrbd(v)) {
                if (isVertexAvailable(v)) {
                    return super.getVertexFillColor(v);
                } else {
                    return Tools.getDefaultColor(
                                            "DrbdGraph.FillPaintNotAvailable");
                }
            }
            if (isVertexPrimary(v)) {
                return Tools.getDefaultColor("DrbdGraph.FillPaintPrimary");
            } else if (isVertexSecondary(v)) {
                return Tools.getDefaultColor("DrbdGraph.FillPaintSecondary");
            } else {
                return Tools.getDefaultColor("DrbdGraph.FillPaintUnknown");
            }
        }
    }

    /**
     * Returns secondary color in the gradient.
     */
    protected final Color getVertexFillSecondaryColor(final Vertex v) {
        return super.getVertexFillSecondaryColor(v);
    }

    /**
     * Finds BlockDevice object on the specified host for block device
     * represented as a string and returns it.
     */
    public final BlockDevice findBlockDevice(final String hostName,
                                             final String disk) {
        final BlockDevInfo bdi = findBlockDevInfo(hostName, disk);
        if (bdi == null) {
            return null;
        }
        return bdi.getBlockDevice();
    }

    /**
     * Finds BlockDevInfo object on the specified host for block device
     * represented as a string and returns it.
     */
    public final BlockDevInfo findBlockDevInfo(final String hostName,
                                               final String disk) {
        HostDrbdInfo hi = null;
        for (final HostDrbdInfo h : hostBDVerticesMap.keySet()) {
            hi = h;
            if (hi.toString().equals(hostName)) {
                break;
            }
        }
        if (hi == null) {
            return null;
        }
        for (final Vertex v : hostBDVerticesMap.get(hi)) {
            final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
            if (bdi.getName().equals(disk)
                || bdi.getBlockDevice().getReadlink().equals(disk)) {
                return bdi;
            }
        }
        return null;
    }

    /**
     * Returns tool tip when mouse is over a block device vertex.
     */
    public final String getVertexToolTip(final Vertex v) {
        final Info i = getInfo(v);
        return i.getToolTipForGraph(isTestOnly());
    }

    /**
     * Returns tool tip when mouse is over a resource edge.
     */
    public final String getEdgeToolTip(final Edge edge) {
        final DrbdResourceInfo dri = edgeToDrbdResourceMap.get(edge);
        return dri.getToolTipForGraph(isTestOnly());
    }

    /**
     * Returns whether arrow shoud be shown. It is shown on the edge from
     * primary to secondory, or from connected node to the disconnected or
     * none at all.
     */
    protected final boolean showEdgeArrow(final Edge e) {
        final MyEdge edge = (MyEdge) e;
        final BlockDevInfo sourceBDI = (BlockDevInfo) getInfo(edge.getSource());
        final BlockDevInfo destBDI = (BlockDevInfo) getInfo(edge.getDest());
        final BlockDevice sourceBD = sourceBDI.getBlockDevice();
        final BlockDevice destBD = destBDI.getBlockDevice();
        final boolean tOnly = isTestOnly();
        if (sourceBDI.isConnected(tOnly)
            && sourceBD.isPrimary() != destBD.isPrimary()) {
            return true;
        } else if (sourceBDI.isWFConnection(tOnly)
                   ^ destBDI.isWFConnection(tOnly)) {
            /* show arrow from wf connection */
            return true;
        }
        return false;
    }

    /**
     * Returns the color of the edge, depending on if the drbds are connected
     * and so on.
     */
    protected final Paint getEdgeDrawPaint(final Edge e) {
        final MyEdge edge = (MyEdge) e;
        final DrbdResourceInfo dri = edgeToDrbdResourceMap.get(e);
        if (dri.isConnected(isTestOnly()) && !dri.isSplitBrain()) {
            return super.getEdgeDrawPaint(edge);
        } else {
            return Tools.getDefaultColor(
                                    "DrbdGraph.EdgeDrawPaintDisconnected");
        }

    }

    /**
     * Returns paint for picked edge. It returns different colors if drbd is
     * disconnected.
     */
    protected final Paint getEdgePickedPaint(final Edge e) {
        final MyEdge edge = (MyEdge) e;
        final DrbdResourceInfo dri = edgeToDrbdResourceMap.get(e);
        if (dri.isConnected(isTestOnly()) && !dri.isSplitBrain()) {
            return super.getEdgePickedPaint(edge);
        } else {
            return Tools.getDefaultColor(
                            "DrbdGraph.EdgeDrawPaintDisconnectedBrighter");
        }

    }

    /**
     * Returns id that is used for saving of the vertex positions to a file.
     */
    protected final String getId(final Info i) {
        final Vertex v = getVertex(i);
        String hiId = "";
        if (v != null) {
            final HostDrbdInfo hi = vertexToHostMap.get(v);
            hiId = hi.getId();
        }
        return "dr=" + hiId + i.getId();
    }

    /**
     * Returns the default vertex width.
     */
    protected final int getDefaultVertexWidth(final Vertex v) {
        if (isVertexBlockDevice(v)) {
            return VERTEX_SIZE_BD;
        } else {
            return VERTEX_SIZE_HOST;
        }
    }

    /**
     * Returns height of the vertex.
     */
    protected final int getDefaultVertexHeight(final Vertex v) {
        if (isVertexBlockDevice(v)) {
            return VERTEX_HEIGHT;
        } else {
            return HOST_VERTEX_HEIGHT;
        }
    }


    /**
     * Returns how much of the disk is used.
     * -1 for not used or not applicable.
     */
    protected final int getUsed(final Vertex v) {
        if (isVertexBlockDevice(v)) {
            final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
            return bdi.getUsed();
        }
        final HostDrbdInfo hi = vertexToHostMap.get(v);
        return hi.getUsed();
    }

    /**
     * This method draws how much of the vertex is used for something.
     */
    protected final void drawInside(final Vertex v,
                                    final Graphics2D g2d,
                                    final double x,
                                    final double y,
                                    final Shape shape) {
        final double used = getUsed(v);
        final float height = (float) shape.getBounds().getHeight();
        final float width = (float) shape.getBounds().getWidth();
        if (isVertexBlockDevice(v)) {
            final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
            if (bdi != null && bdi.getBlockDevice().isDrbdMetaDisk()) {
                final Color[] colors = {null, null};
                colors[1] = getVertexFillColor(blockDeviceToVertexMap.get(
                     bdi.getBlockDevice().getMetaDiskOfBlockDevices().get(0)));
                drawInsideVertex(g2d,
                                 v,
                                 colors,
                                 x,
                                 y,
                                 height,
                                 width);
            }
        } else {
            final HostDrbdInfo hi = (HostDrbdInfo) getInfo(v);
            drawInsideVertex(g2d,
                             v,
                             hi.getHost().getDrbdColors(),
                             x,
                             y,
                             height,
                             width);
        }
        final boolean tOnly = isTestOnly();
        if (used > 0) {
            /** Show how much is used. */
            final double freeWidth = width * (100 - used) / 100;
            g2d.setColor(new Color(255, 255, 255, 220));
            g2d.fillRect((int) (x + width - freeWidth),
                         (int) (y),
                         (int) (freeWidth),
                         (int) (height));
        }
        if (isPicked(v)) {
            if (tOnly) {
                g2d.setColor(Color.RED);
            } else {
                g2d.setColor(Color.BLACK);
            }
        } else {
            boolean pickedResource = false;
            if (tOnly) {
                final Set inEdges = v.getInEdges();

                for (final Object e : inEdges) {
                    if (isPicked((Edge) e)) {
                        pickedResource = true;
                        break;
                    }
                }
                if (!pickedResource) {
                    final Set outEdges = v.getOutEdges();
                    for (final Object e : outEdges) {
                        if (isPicked((Edge) e)) {
                            pickedResource = true;
                            break;
                        }
                    }
                }
            }
            if (pickedResource) {
                g2d.setColor(Color.red);
            } else {
                g2d.setColor(Color.WHITE);
            }
        }
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(shape);
    }
}
