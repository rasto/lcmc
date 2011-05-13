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

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.visualization.util.VertexShapeFactory;

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
public final class DrbdGraph extends ResourceGraph {
    /** Map from vertex to host. */
    private final Map<Vertex, HostDrbdInfo> vertexToHostMap =
                                 new LinkedHashMap<Vertex, HostDrbdInfo>();
    /** Map from host to vertex. */
    private final Map<HostDrbdInfo, Vertex> hostToVertexMap =
                                 new LinkedHashMap<HostDrbdInfo, Vertex>();
    /** Map from block device info object to vertex. */
    private final Map<BlockDevInfo, Vertex> bdiToVertexMap =
                                 new LinkedHashMap<BlockDevInfo, Vertex>();
    /** Map from block device to vertex. */
    private final Map<BlockDevice, Vertex> blockDeviceToVertexMap =
                                 new LinkedHashMap<BlockDevice, Vertex>();
    /** Map from host to the list of block devices. */
    private final Map<HostDrbdInfo, List<Vertex>> hostBDVerticesMap =
                               new LinkedHashMap<HostDrbdInfo, List<Vertex>>();
    /** Map from graph edge to the drbd resource info object. */
    private final Map<Edge, DrbdResourceInfo> edgeToDrbdResourceMap =
                                 new LinkedHashMap<Edge, DrbdResourceInfo>();
    /** Map from drbd resource info object to the graph edge. */
    private final Map<DrbdResourceInfo, Edge> drbdResourceToEdgeMap =
                                 new LinkedHashMap<DrbdResourceInfo, Edge>();

    /** Drbd info object to which this graph belongs. */
    private DrbdInfo drbdInfo;

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
    /** Postion offset of block devices from the host x position. */
    private static final int BD_X_OFFSET = 15;

    /** The first X position of the host. */
    private int hostDefaultXPos = 10;
    /** Minimum vertical position. */
    private static final int MIN_Y_POS = 20;
    /** Maximum horizontal position. */
    private static final int MAX_X_POS = 2600;
    /** Maximum vertical position. */
    private static final int MAX_Y_POS = 2600;

    /** Prepares a new <code>DrbdGraph</code> object. */
    public DrbdGraph(final ClusterBrowser clusterBrowser) {
        super(clusterBrowser);
    }

    /** Inits the graph. */
    @Override protected void initGraph() {
        super.initGraph(new DirectedSparseGraph<Vertex, Edge>());
    }

    /** Sets drbd info object. */
    public void setDrbdInfo(final DrbdInfo drbdInfo) {
        this.drbdInfo = drbdInfo;
    }

    /** Returns drbd info object. */
    public DrbdInfo getDrbdInfo() {
        return drbdInfo;
    }

    /** Returns whether vertex is block device. */
    private boolean isVertexBlockDevice(final Vertex v) {
        return vertexToHostMap.get(v) != getInfo(v);
    }

    /** Adds host with all its block devices to the graph. If it is there
     * already fix the positions of the block devices. */
    void addHost(final HostDrbdInfo hostDrbdInfo) {
        Vertex v = getVertex(hostDrbdInfo);
        if (v == null) {
            /* add host vertex */
            v = new Vertex();
            somethingChanged();
            putInfoToVertex(hostDrbdInfo, v);
            vertexToHostMap.put(v, hostDrbdInfo);
            hostToVertexMap.put(hostDrbdInfo, v);
            putVertexToInfo(v, (Info) hostDrbdInfo);
            Point2D hostPos = getSavedPosition(hostDrbdInfo);

            if (hostPos == null) {
                hostPos = new Point2D.Double(
                                hostDefaultXPos + VERTEX_SIZE_HOST / 2,
                                HOST_Y_POS);
                hostDefaultXPos += HOST_STEP_X;
            }
            final double hostXPos =
                                hostPos.getX() - VERTEX_SIZE_HOST / 2;
            getVertexLocations().put(v, hostPos);
            putVertexLocations();
            lockGraph();
            getGraph().addVertex(v);
            unlockGraph();
        }
        /* add block devices vertices */
        final Host host = hostDrbdInfo.getHost();
        final Point2D hostPos = getVertexLocations().get(v);
        putVertexLocations();
        final double hostXPos = hostPos.getX() - VERTEX_SIZE_HOST / 2;
        final double hostYPos = hostPos.getY();
        int devYPos = (int) hostYPos + BD_STEP_Y;
        List<Vertex> vertexList = hostBDVerticesMap.get(hostDrbdInfo);
        List<Vertex> oldVertexList = null;
        if (vertexList == null) {
            vertexList = new ArrayList<Vertex>();
            hostBDVerticesMap.put(hostDrbdInfo, vertexList);
        } else {
            oldVertexList = new ArrayList<Vertex>(vertexList);
        }
        final Set<BlockDevInfo> blockDevInfos =
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
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
                            lockGraph();
                            getGraph().removeVertex(bdv);
                            unlockGraph();
                        }
                    });
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
            Vertex bdv = null;
            if (!blockDeviceToVertexMap.containsKey(bdi.getBlockDevice())) {
                bdv = new Vertex();
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
            if (bdv == null) {
                bdv = blockDeviceToVertexMap.get(bdi.getBlockDevice());
            }
            Point2D pos = null; // getSavedPosition(bdi);
            if (pos == null) {
                pos = new Point2D.Double(
                    hostXPos + BD_X_OFFSET + VERTEX_SIZE_BD / 2,
                    devYPos);
            }
            getVertexLocations().put(bdv, pos);
            putVertexLocations();
            getLayout().setLocation(bdv, pos);
            devYPos += BD_STEP_Y;
            if (bdv != null) {
                lockGraph();
                getGraph().addVertex(bdv);
                unlockGraph();
            }
        }
    }

    /** Scale and add hosts if they appeared. */
    @Override public void scale() {
        for (final HostDrbdInfo hostDrbdInfo : hostBDVerticesMap.keySet()) {
            addHost(hostDrbdInfo);
        }
        super.scale();
    }

    /** Removes drbd resource from the graph. */
    public void removeDrbdResource(final DrbdResourceInfo dri) {
        final Edge e = drbdResourceToEdgeMap.get(dri);
        e.reset();
        edgeToDrbdResourceMap.remove(e);
        drbdResourceToEdgeMap.remove(dri);
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                try {
                    lockGraph();
                    getGraph().removeEdge(e);
                    unlockGraph();
                } catch (final Exception ignore) {
                    unlockGraph();
                    /* ignore */
                }
            }
        });
    }

    /**
     * Returns an icon for vertex, depending on if it is host or block device,
     * if it is started or stopped and so on.
     */
    @Override protected List<ImageIcon> getIconsForVertex(
                                                      final Vertex v,
                                                      final boolean testOnly) {
        final List<ImageIcon> icons = new ArrayList<ImageIcon>();
        if (isVertexBlockDevice(v)) {
            final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
            icons.add(BlockDevInfo.HARDDISK_ICON_LARGE);
            if (bdi.isDiskless(testOnly)) {
                icons.add(BlockDevInfo.NO_HARDDISK_ICON_LARGE);
                return icons;
            } else {
                return icons;
            }
        } else {
            final HostDrbdInfo hi = vertexToHostMap.get(v);
            if (hi == null) {
                return null;
            }
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
    @Override protected String getLabelForEdgeStringer(final Edge edge) {
        final DrbdResourceInfo dri = edgeToDrbdResourceMap.get(edge);
        if (dri != null && dri.getName() != null) {
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
                        @Override public void run() {
                            repaint();
                        }
                    });

                }
            } else if (!sourceBD.isPrimary() && destBD.isPrimary()) {
                edge.setDirection(dest, source);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        repaint();
                    }
                });
            }

            final StringBuilder l = new StringBuilder(dri.getName());
            if (l != null) {
                final Map<Vertex, Point2D> vl = getVertexLocations();
                final Point2D sp = vl.get(source);
                final Point2D dp = vl.get(dest);
                putVertexLocations();
                final int len = (int) Math.sqrt(Math.pow(sp.getX()
                                                         - dp.getX(), 2)
                                                + Math.pow(sp.getY()
                                                           - dp.getY(), 2));
                final int maxLen = (len - 200) / 7;
                if (l.length() > maxLen) {
                    l.delete(0, l.length() - maxLen + 3);
                    l.insert(0, "...");
                }
                if (dri.isSyncing()) {
                    String syncedProgress = dri.getSyncedProgress();
                    if (syncedProgress == null) {
                        syncedProgress = "?.?";
                    }
                    final double sourceX = getLayout().transform(source).getX();
                    final double destX = getLayout().transform(dest).getX();
                    if (sourceBD.isPausedSync() || destBD.isPausedSync()) {
                        l.append(" (" + syncedProgress + "% paused)");
                    } else if ((sourceBD.isSyncSource() && sourceX < destX)
                               || (destBD.isSyncSource() && sourceX > destX)) {
                        l.append(" (" + syncedProgress + "% \u2192)"); /* -> */
                    } else {
                        l.append(" (\u2190 " + syncedProgress + "%)"); /* <- */
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

    /** Small text that appears above the icon. */
    @Override protected String getIconText(final Vertex v,
                                           final boolean testOnly) {
        if (isVertexBlockDevice(v)) {
            final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
            if (bdi != null) {
                return bdi.getIconTextForGraph(testOnly);
            }
        } else {
            final HostDrbdInfo hi = vertexToHostMap.get(v);
            if (hi != null) {
                return hi.getIconTextForDrbdGraph(testOnly);
            }
        }
        return null;
    }

    /** Small text that appears in the right corner. */
    @Override protected Subtext getRightCornerText(final Vertex v,
                                                   final boolean testOnly) {
        if (isVertexBlockDevice(v)) {
            final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
            if (bdi != null) {
                return bdi.getRightCornerTextForDrbdGraph(testOnly);

            }
        } else {
            final HostDrbdInfo hi = vertexToHostMap.get(v);
            if (hi != null) {
                return hi.getRightCornerTextForDrbdGraph(testOnly);
            }
        }
        return null;
    }

    /** Small text that appears down. */
    @Override protected Subtext[] getSubtexts(final Vertex v,
                                              final boolean testOnly) {
        if (isVertexBlockDevice(v)) {
            final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
            if (bdi != null && bdi.getBlockDevice().isDrbd()
                && bdi.getBlockDevice().getConnectionState() != null
                && bdi.getBlockDevice().getDiskState() != null) {
                final String connState =
                                  bdi.getBlockDevice().getConnectionState();
                final String diskState = bdi.getBlockDevice().getDiskState();
                Color color = null;
                Color textColor = Color.BLACK;
                if ("StandAlone".equals(connState)
                    || !"UpToDate".equals(diskState)) {
                    color = Color.RED;
                    textColor = Color.WHITE;
                }
                return new Subtext[]{
                    new Subtext(connState + " / " + diskState,
                                color,
                                textColor)};
            }
        } else {
            final HostDrbdInfo hi = vertexToHostMap.get(v);
            if (hi != null) {
                return hi.getSubtextsForDrbdGraph(testOnly);
            }
        }
        return null;
    }

    /**
     * Returns label for block device vertex. If it is longer than 23
     * characters, it is shortened.
     */
    @Override protected String getMainText(final Vertex v,
                                           final boolean testOnly) {
        if (isVertexBlockDevice(v)) {
            String l;
            if (isVertexDrbd(v)) {
                final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
                l = bdi.getDrbdResourceInfo().getDevice();
            } else {
                l = getInfo(v).getName();
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

    /** Returns shape of the block device vertex. */
    @Override protected Shape getVertexShape(
                                    final Vertex v,
                                    final VertexShapeFactory<Vertex> factory) {
        return factory.getRectangle(v);
    }

    /** Handles popup in when block device vertex is clicked. */
    protected JPopupMenu handlePopupVertex(final Vertex v, final Point2D p) {
        if (isVertexBlockDevice(v)) {
            final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
            return bdi.getPopup();
        } else {
            /* host */
            final HostDrbdInfo hi = (HostDrbdInfo) getInfo(v);
            if (hi == null) {
                return null;
            } else {
                return hi.getPopup();
            }
        }
    }

    /** Adds drbd resource edge to the graph. */
    public void addDrbdResource(final DrbdResourceInfo dri,
                                final BlockDevInfo bdi1,
                                final BlockDevInfo bdi2) {
        if (bdi1 != null && bdi2 != null) {
            final Vertex v1 = bdiToVertexMap.get(bdi1);
            final Vertex v2 = bdiToVertexMap.get(bdi2);
            lockGraph();
            if (getGraph().findEdge(v1, v2) != null
                || getGraph().findEdge(v2, v1) != null) {
                unlockGraph();
                return;
            }
            final Edge e = new Edge(v1, v2);
            getGraph().addEdge(e, v1, v2);
            unlockGraph();
            edgeToDrbdResourceMap.put(e, dri);
            drbdResourceToEdgeMap.put(dri, e);
        }
    }

    /** Returns the source block device in a drbd connection. */
    public BlockDevInfo getSource(final DrbdResourceInfo dri) {
        final Edge edge = drbdResourceToEdgeMap.get(dri);
        final Vertex source = edge.getSource();
        return (BlockDevInfo) getInfo(source);
    }

    /** Returns the destination block device in a drbd connection. */
    public BlockDevInfo getDest(final DrbdResourceInfo dri) {
        final Edge edge = drbdResourceToEdgeMap.get(dri);
        if (edge == null) {
            return null;
        }
        final Vertex dest = edge.getDest();
        return (BlockDevInfo) getInfo(dest);
    }

    /** Picks vertex, that is associated with the specified info object. */
    @Override public void pickInfo(final Info i) {
        final Edge e = drbdResourceToEdgeMap.get(i);
        if (e == null) {
            super.pickInfo(i);
        } else {
            pickEdge(e);
        }
    }

    /** Is called after right click on the resource edge. */
    @Override protected JPopupMenu handlePopupEdge(final Edge edge) {
        final DrbdResourceInfo dri = edgeToDrbdResourceMap.get(edge);
        return dri.getPopup();
    }


    /**
     * Is called after right click on the background and it returns
     * background popup menu.
     */
    @Override protected JPopupMenu handlePopupBackground(final Point2D pos) {
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
    //    putVertexLocations();
    //    final double x = hl.getX() + BD_X_OFFSET;
    //    for (final Vertex v : hostBDVerticesMap.get(hi)) {
    //        if (!v.equals(vertex) && !ps.isPicked(v)) {
    //            final Point2D l = getVertexLocations().getLocation(v);
    //            putVertexLocations();
    //            final double y = l.getY();
    //            if (oldY >= 0) {
    //                if (y >= oldY && y <= newY) {
    //                    l.setLocation(x, y - BD_STEP_Y);
    //                    getVertexLocations().setLocation(v, l);
    //                    putVertexLocations();
    //                } else if (y <= oldY && y >= newY) {
    //                    l.setLocation(x, y + BD_STEP_Y);
    //                    getVertexLocations().setLocation(v, l);
    //                    putVertexLocations();
    //                }
    //            } else {
    //                l.setLocation(x, y);
    //                getVertexLocations().setLocation(v, l);
    //                putVertexLocations();
    //            }
    //        }
    //    }
    //}

    /**
     * Picks vertex representig specified block device info object in the
     * graph.
     */
    public void pickBlockDevice(final BlockDevInfo bdi) {
        final Vertex v = bdiToVertexMap.get(bdi);
        pickVertex(v);
        bdi.selectMyself();
    }

    /** Is called of a host is picked. Its terminal panel is set to view. */
    private void pickHost(final Vertex v) {
        pickVertex(v);
        final HostDrbdInfo hi = vertexToHostMap.get(v);
        if (hi == null) {
            return;
        }
        Tools.getGUIData().setTerminalPanel(hi.getHost().getTerminalPanel());
    }

    /** Is called when one block device vertex was pressed. */
    @Override protected void oneVertexPressed(final Vertex v) {
        if (isVertexBlockDevice(v)) {
            pickHost(v);
            final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
            drbdInfo.setSelectedNode(bdi);
            drbdInfo.selectMyself();
            getClusterBrowser().setRightComponentInView(bdi);
        } else {
            pickHost(v);
            final HostDrbdInfo hi = vertexToHostMap.get(v);
            if (hi == null) {
                return;
            }
            getClusterBrowser().setRightComponentInView(hi);
        }
    }

    /** Is called when block device vertex is released. */
    @Override protected void vertexReleased(final Vertex v, final Point2D pos) {
        double x = pos.getX();
        double y = pos.getY();
        final double minPos = (getVertexWidth(v)
                               - getDefaultVertexWidth(v)) / 2;
        x = x < minPos ? minPos : x;
        x = x > MAX_X_POS ? MAX_X_POS : x;
        y = y < MIN_Y_POS ? MIN_Y_POS : y;
        y = y > MAX_Y_POS ? MAX_Y_POS : y;
        pos.setLocation(x + (getDefaultVertexWidth(v)
                             - getVertexWidth(v)) / 2,
                        y);
        final Point2D loc = new Point2D.Double(x, y);
        getVertexLocations().put(v, pos);
        putVertexLocations();
        getLayout().setLocation(v, loc);
    }

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
    @Override protected void oneEdgePressed(final Edge e) {
        final DrbdResourceInfo dri = edgeToDrbdResourceMap.get(e);
        if (dri != null) {
            dri.selectMyself();
        }
    }

    /**
     * Is called, when background of the graph is clicked. It deselects
     * selected node.
     */
    @Override protected void backgroundClicked() {
        drbdInfo.setSelectedNode(null);
        drbdInfo.selectMyself();
    }

    /**
     * Returns fill color as paint object for for specified block device
     * vertex.
     */
    @Override protected Color getVertexFillColor(final Vertex v) {
        final HostDrbdInfo hi = vertexToHostMap.get(v);
        final Vertex hostVertex = getVertex(hi);
        if (v.equals(hostVertex)) {
            /* host */
            return hi.getHost().getDrbdColors()[0];
        } else if (hi != null && (hi.getHost() == null
                                  || (!hi.getHost().isDrbdStatus()
                                      && hi.getHost().isDrbdLoaded()))) {
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
     * Finds BlockDevice object on the specified host for block device
     * represented as a string and returns it.
     */
    BlockDevice findBlockDevice(final String hostName, final String disk) {
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
    public BlockDevInfo findBlockDevInfo(final String hostName,
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

    /** Returns tool tip when mouse is over a block device vertex. */
    @Override String getVertexToolTip(final Vertex v) {
        final Info i = getInfo(v);
        return i.getToolTipForGraph(isTestOnly());
    }

    /** Returns tool tip when mouse is over a resource edge. */
    @Override String getEdgeToolTip(final Edge edge) {
        final DrbdResourceInfo dri = edgeToDrbdResourceMap.get(edge);
        return dri.getToolTipForGraph(isTestOnly());
    }

    /**
     * Returns whether arrow shoud be shown. It is shown on the edge from
     * primary to secondory, or from connected node to the disconnected or
     * none at all.
     */
    @Override protected boolean showEdgeArrow(final Edge edge) {
        final BlockDevInfo sourceBDI = (BlockDevInfo) getInfo(edge.getSource());
        final BlockDevInfo destBDI = (BlockDevInfo) getInfo(edge.getDest());
        if (sourceBDI == null || destBDI == null) {
            return false;
        }
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
    @Override protected Paint getEdgeDrawPaint(final Edge edge) {
        final DrbdResourceInfo dri = edgeToDrbdResourceMap.get(edge);
        if (dri != null
            && dri.isConnected(isTestOnly())
            && !dri.isSplitBrain()) {
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
    @Override protected Paint getEdgePickedPaint(final Edge edge) {
        final DrbdResourceInfo dri = edgeToDrbdResourceMap.get(edge);
        if (dri != null
            && dri.isConnected(isTestOnly())
            && !dri.isSplitBrain()) {
            return super.getEdgePickedPaint(edge);
        } else {
            return Tools.getDefaultColor(
                            "DrbdGraph.EdgeDrawPaintDisconnectedBrighter");
        }

    }

    /** Returns id that is used for saving of the vertex positions to a file. */
    @Override protected String getId(final Info i) {
        final Vertex v = getVertex(i);
        String hiId = "";
        if (v != null) {
            final HostDrbdInfo hi = vertexToHostMap.get(v);
            if (hi != null) {
                hiId = hi.getId();
            }
        }
        return "dr=" + hiId + i.getId();
    }

    /** Returns the default vertex width. */
    @Override protected int getDefaultVertexWidth(final Vertex v) {
        if (isVertexBlockDevice(v)) {
            return VERTEX_SIZE_BD;
        } else {
            return VERTEX_SIZE_HOST;
        }
    }

    /** Returns height of the vertex. */
    @Override protected int getDefaultVertexHeight(final Vertex v) {
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
    protected int getUsed(final Vertex v) {
        if (isVertexBlockDevice(v)) {
            final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
            return bdi.getUsed();
        }
        final HostDrbdInfo hi = vertexToHostMap.get(v);
        if (hi == null) {
            return 0;
        } else {
            return hi.getUsed();
        }
    }

    /** This method draws how much of the vertex is used for something. */
    @Override protected void drawInside(final Vertex v,
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
            if (hi != null) {
                drawInsideVertex(g2d,
                                 v,
                                 hi.getHost().getDrbdColors(),
                                 x,
                                 y,
                                 height,
                                 width);
            }
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
                lockGraph();
                for (final Edge e : getGraph().getInEdges(v)) {
                    if (isPicked(e)) {
                        pickedResource = true;
                        break;
                    }
                }
                if (!pickedResource) {
                    for (final Edge e : getGraph().getOutEdges(v)) {
                        if (isPicked(e)) {
                            pickedResource = true;
                            break;
                        }
                    }
                }
                unlockGraph();
            }
            if (pickedResource) {
                g2d.setColor(Color.RED);
            } else {
                g2d.setColor(Color.WHITE);
            }
        }
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(shape);
    }

    /** Returns whether to show a hollow arrow. */
    @Override protected boolean showHollowArrow(final Edge e) {
        final DrbdResourceInfo dri = edgeToDrbdResourceMap.get(e);
        if (dri == null) {
            return false;
        }
        return !dri.isConnected(isTestOnly());
    }
}
