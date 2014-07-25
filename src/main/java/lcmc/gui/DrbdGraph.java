/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
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
package lcmc.gui;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.visualization.util.VertexShapeFactory;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import lcmc.model.Application;
import lcmc.model.Host;
import lcmc.model.Subtext;
import lcmc.model.resources.BlockDevice;
import lcmc.gui.resources.Info;
import lcmc.gui.resources.drbd.BlockDevInfo;
import lcmc.gui.resources.drbd.GlobalInfo;
import lcmc.gui.resources.drbd.HostDrbdInfo;
import lcmc.gui.resources.drbd.MultiSelectionInfo;
import lcmc.gui.resources.drbd.VolumeInfo;
import lcmc.utilities.Tools;

/**
 * This class creates graph and provides methods to add new block device
 * vertices and drbd volume edges, remove or modify them.
 */
public class DrbdGraph extends ResourceGraph {
    /** Horizontal step in pixels by which the block devices are drawn in the graph. */
    private static final int BD_STEP_Y = 60;
    /** Y position of the host. */
    private static final int HOST_Y_POS = 40;
    /** Vertical step in pixels by which the hosts are drawn in the graph. */
    private static final int HOST_STEP_X = 280;
    private static final int VERTEX_SIZE_BD = 200;
    private static final int VERTEX_SIZE_HOST = 150;
    private static final int HOST_VERTEX_HEIGHT = 50;
    /** Height of the block device vertices. */
    private static final int VERTEX_HEIGHT = 50;

    /** Maximum length of the label in the vertex, after which the string will be cut. */
    private static final int MAX_VERTEX_STRING_LENGTH = 18;
    /** Postion offset of block devices from the host x position. */
    private static final int BD_X_OFFSET = 15;
    /** Minimum vertical position. */
    private static final int MIN_Y_POS = 20;
    /** Maximum horizontal position. */
    private static final int MAX_X_POS = 2600;
    /** Maximum vertical position. */
    private static final int MAX_Y_POS = 2600;
    /** Map from vertex to host. */
    private final Map<Vertex, HostDrbdInfo> vertexToHostMap = new LinkedHashMap<Vertex, HostDrbdInfo>();
    /** Map from host to vertex. */
    private final Map<HostDrbdInfo, Vertex> hostToVertexMap = new LinkedHashMap<HostDrbdInfo, Vertex>();
    /** Map from block device info object to vertex. */
    private final Map<BlockDevInfo, Vertex> bdiToVertexMap = new LinkedHashMap<BlockDevInfo, Vertex>();
    /** Map from block device to vertex. */
    private final Map<BlockDevice, Vertex> blockDeviceToVertexMap = new LinkedHashMap<BlockDevice, Vertex>();
    /** Map from host to the list of block devices. */
    private final Map<HostDrbdInfo, List<Vertex>> hostBDVerticesMap = new LinkedHashMap<HostDrbdInfo, List<Vertex>>();
    /** Map from graph edge to the drbd volume info object. */
    private final Map<Edge, VolumeInfo> edgeToDrbdVolumeMap = new LinkedHashMap<Edge, VolumeInfo>();
    /** Map from drbd volume info object to the graph edge. */
    private final Map<VolumeInfo, Edge> drbdVolumeToEdgeMap = new LinkedHashMap<VolumeInfo, Edge>();

    private GlobalInfo globalInfo;
    private Info multiSelectionInfo = null;

    /** The first X position of the host. */
    private int hostDefaultXPos = 10;

    public DrbdGraph(final ClusterBrowser clusterBrowser) {
        super(clusterBrowser);
    }

    @Override
    protected void initGraph() {
        super.initGraph(new DirectedSparseGraph<Vertex, Edge>());
    }

    public void setDrbdInfo(final GlobalInfo globalInfo) {
        this.globalInfo = globalInfo;
    }

    public GlobalInfo getDrbdInfo() {
        return globalInfo;
    }

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
            putVertexToInfo(v, hostDrbdInfo);
            Point2D hostPos = getSavedPosition(hostDrbdInfo);

            if (hostPos == null) {
                hostPos = new Point2D.Double(hostDefaultXPos + VERTEX_SIZE_HOST / 2, HOST_Y_POS);
                hostDefaultXPos += HOST_STEP_X;
            }
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
        final Set<BlockDevInfo> blockDevInfos = host.getBrowser().getSortedBlockDevInfos();
        if (oldVertexList != null) {
            for (final Vertex vertex : oldVertexList) {
                final BlockDevInfo bdi = (BlockDevInfo) getInfo(vertex);
                if (bdi == null) {
                    continue;
                }
                if (!blockDevInfos.contains(bdi)) {
                    /* removing */
                    final Vertex bdv = bdiToVertexMap.get(bdi);
                    final VolumeInfo dvi = bdi.getDrbdVolumeInfo();
                    if (dvi != null) {
                        removeDrbdVolume(dvi);
                        dvi.getDrbdResourceInfo().removeDrbdVolumeFromHashes(dvi);
                    }
                    Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                        @Override
                        public void run() {
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
        BlockDevInfo prevBdi = null;
        for (final BlockDevInfo bdi : blockDevInfos) {
            stopAnimation(bdi);
            Vertex bdv = null;
            if (!blockDeviceToVertexMap.containsKey(bdi.getBlockDevice())) {
                bdv = new Vertex();
                somethingChanged();
                bdiToVertexMap.put(bdi, bdv);
                blockDeviceToVertexMap.put(bdi.getBlockDevice(), bdv);
                putVertexToInfo(bdv, bdi);
                putInfoToVertex(bdi, bdv);
                vertexToHostMap.put(bdv, hostDrbdInfo);
                vertexList.add(bdv);
                // TODO: get saved position is disabled at the moment,
                // because it does more harm than good at the moment.
            }
            if (bdv == null) {
                bdv = blockDeviceToVertexMap.get(bdi.getBlockDevice());
            }
            if (prevBdi != null
                && bdi.getBlockDevice().isVolumeGroupOnPhysicalVolume()
                && bdi.getBlockDevice().getVolumeGroupOnPhysicalVolume().equals(
                                               prevBdi.getBlockDevice().getVolumeGroupOnPhysicalVolume())) {
                devYPos -= 8;
            } else if (prevBdi != null
                && (!bdi.getBlockDevice().isDrbd() || !prevBdi.getBlockDevice().isDrbd())) {
                devYPos -= 4;
            } else if (prevBdi != null
                      && bdi.getBlockDevice().isDrbd()
                      && prevBdi.getBlockDevice().isDrbd()
                      && bdi.getDrbdVolumeInfo().getDrbdResourceInfo()
                         == prevBdi.getDrbdVolumeInfo().getDrbdResourceInfo()) {
                devYPos -= 6;
            }
            final Point2D pos = new Point2D.Double(hostXPos + BD_X_OFFSET + VERTEX_SIZE_BD / 2, devYPos);
            devYPos += BD_STEP_Y;
            getVertexLocations().put(bdv, pos);
            putVertexLocations();
            getLayout().setLocation(bdv, pos);
            if (bdv != null) {
                lockGraph();
                getGraph().addVertex(bdv);
                unlockGraph();
            }
            prevBdi = bdi;
        }
    }

    /** Scale and add hosts if they appeared. */
    @Override
    public void scale() {
        for (final HostDrbdInfo hostDrbdInfo : hostBDVerticesMap.keySet()) {
            addHost(hostDrbdInfo);
        }
        super.scale();
    }

    /** Removes drbd volume from the graph. */
    public void removeDrbdVolume(final VolumeInfo dvi) {
        final Edge e = drbdVolumeToEdgeMap.get(dvi);
        if (e == null) {
            return;
        }
        e.reset();
        edgeToDrbdVolumeMap.remove(e);
        drbdVolumeToEdgeMap.remove(dvi);
        try {
            lockGraph();
            getGraph().removeEdge(e);
            unlockGraph();
        } catch (final Exception ignore) {
            unlockGraph();
            /* ignore */
        }
    }

    /**
     * Returns an icon for vertex, depending on if it is host or block device,
     * if it is started or stopped and so on.
     */
    @Override
    protected List<ImageIcon> getIconsForVertex(final Vertex v, final Application.RunMode runMode) {
        final List<ImageIcon> icons = new ArrayList<ImageIcon>();
        if (isVertexBlockDevice(v)) {
            final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
            if (bdi == null) {
                return icons;
            }
            if (bdi.getBlockDevice().isDrbd()) {
                icons.add(BlockDevInfo.HARDDISK_DRBD_ICON_LARGE);
            } else {
                icons.add(BlockDevInfo.HARDDISK_ICON_LARGE);
            }
            if (bdi.isDiskless(runMode)) {
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
            if (hi.getHost().isDrbdStatusOk() && hi.getHost().isDrbdLoaded()) {
                icons.add(HostBrowser.HOST_ON_ICON_LARGE);
            } else {
                icons.add(HostBrowser.HOST_ICON_LARGE);
            }
            return icons;
        }
    }

    /**
     * Returns label for drbd volume edge. If it is longer than 10
     * characters, it is shortened.
     */
    @Override
    protected String getLabelForEdgeStringer(final Edge edge) {
        final VolumeInfo dvi = edgeToDrbdVolumeMap.get(edge);
        if (dvi != null
            && dvi.getName() != null
            && dvi.getDrbdResourceInfo() != null) {
            final Vertex source = edge.getSource();
            final Vertex dest = edge.getDest();
            final BlockDevInfo sourceBDI = (BlockDevInfo) getInfo(source);
            if (sourceBDI == null) {
                return "";
            }
            final BlockDevInfo destBDI = (BlockDevInfo) getInfo(dest);
            if (destBDI == null) {
                return "";
            }
            final BlockDevice sourceBD = sourceBDI.getBlockDevice();
            final BlockDevice destBD = destBDI.getBlockDevice();
            final Application.RunMode runMode = getRunMode();
            if (!destBDI.isConnected(runMode)) {
                if (sourceBDI.isWFConnection(runMode)
                    && !destBDI.isWFConnection(runMode)) {
                    edge.setDirection(dest, source);
                    Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                        @Override
                        public void run() {
                            repaint();
                        }
                    });

                }
            } else if (!sourceBD.isPrimary() && destBD.isPrimary()) {
                edge.setDirection(dest, source);
                Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                    @Override
                    public void run() {
                        repaint();
                    }
                });
            }
            final StringBuilder l = new StringBuilder(dvi.getNameForGraph());
            final Map<Vertex, Point2D> vl = getVertexLocations();
            final Point2D sp = vl.get(source);
            final Point2D dp = vl.get(dest);
            putVertexLocations();
            final int len = (int) Math.sqrt(Math.pow(sp.getX() - dp.getX(), 2) + Math.pow(sp.getY() - dp.getY(), 2));
            final int maxLen = (len - 200) / 7;
            if (l.length() > maxLen) {
                l.delete(0, l.length() - maxLen + 3);
                l.insert(0, "...");
            }
            if (dvi.isSyncing()) {
                String syncedProgress = dvi.getSyncedProgress();
                if (syncedProgress == null) {
                    syncedProgress = "?.?";
                }
                final double sourceX = getLayout().transform(source).getX();
                final double destX = getLayout().transform(dest).getX();
                if (sourceBD.isPausedSync() || destBD.isPausedSync()) {
                    l.append(" (").append(syncedProgress).append("% paused)");
                } else if (sourceBD.isSyncSource() && sourceX < destX
                           || destBD.isSyncSource() && sourceX > destX) {
                    l.append(" (").append(syncedProgress).append("% \u2192)"); /* -> */
                } else {
                    l.append(" (\u2190 ").append(syncedProgress).append("%)"); /* <- */
                }
            } else if (dvi.isSplitBrain()) {
                l.append(" (split-brain)");
            } else if (!dvi.isConnected(runMode)) {
                l.append(" (disconnected)");
            } else if (dvi.isVerifying()) {
                l.append(" (verify)");
            }
            return l.toString();
        }
        return null;
    }

    /** Small text that appears above the icon. */
    @Override
    protected String getIconText(final Vertex v, final Application.RunMode runMode) {
        if (isVertexBlockDevice(v)) {
            final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
            if (bdi != null) {
                return bdi.getIconTextForGraph(runMode);
            }
        } else {
            final HostDrbdInfo hi = vertexToHostMap.get(v);
            if (hi != null) {
                return hi.getIconTextForDrbdGraph(runMode);
            }
        }
        return null;
    }

    /** Small text that appears in the right corner. */
    @Override
    protected Subtext getRightCornerText(final Vertex v, final Application.RunMode runMode) {
        if (isVertexBlockDevice(v)) {
            final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
            if (bdi != null) {
                return bdi.getRightCornerTextForDrbdGraph(runMode);

            }
        } else {
            final HostDrbdInfo hi = vertexToHostMap.get(v);
            if (hi != null) {
                return hi.getRightCornerTextForDrbdGraph(runMode);
            }
        }
        return null;
    }

    /** Small text that appears down. */
    @Override
    protected Subtext[] getSubtexts(final Vertex v, final Application.RunMode runMode) {
        if (isVertexBlockDevice(v)) {
            final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
            if (bdi != null && bdi.getBlockDevice().isDrbd()
                && bdi.getBlockDevice().getConnectionState() != null
                && bdi.getBlockDevice().getDiskState() != null) {
                final String connState = bdi.getBlockDevice().getConnectionState();
                final String diskState = bdi.getBlockDevice().getDiskState();
                String diskStateOther = null;
                final BlockDevInfo oBdi = bdi.getOtherBlockDevInfo();
                if (oBdi != null
                    && !diskState.equals(oBdi.getBlockDevice().getDiskStateOther())) {
                    diskStateOther = oBdi.getBlockDevice().getDiskStateOther();
                }

                Color color = null;
                Color textColor = Color.BLACK;
                final String proxyState = bdi.getProxyStateForGraph(runMode);
                if ("StandAlone".equals(connState)
                    || !"UpToDate".equals(diskState)
                    || (proxyState != null && !BlockDevInfo.PROXY_UP.equals(proxyState))) {
                    color = Color.RED;
                    textColor = Color.WHITE;
                }
                return new Subtext[]{
                    new Subtext(Tools.join(" / ", new String[]{connState, diskState, diskStateOther, proxyState}),
                                color,
                                textColor)};
            }
        } else {
            final HostDrbdInfo hi = vertexToHostMap.get(v);
            if (hi != null) {
                return hi.getSubtextsForDrbdGraph(runMode);
            }
        }
        return null;
    }

    /**
     * Returns label for block device vertex. If it is longer than 23
     * characters, it is shortened.
     */
    @Override
    public String getMainText(final Vertex v, final Application.RunMode runMode) {
        if (isVertexBlockDevice(v)) {
            String l;
            if (isVertexDrbd(v)) {
                final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
                if (bdi == null) {
                    return "";
                }
                l = bdi.getDrbdVolumeInfo().getDevice();
            } else {
                final Info info = getInfo(v);
                if (info == null) {
                    return "";
                }
                l = info.getMainTextForGraph();
            }
            if (l.length() > MAX_VERTEX_STRING_LENGTH) {
                l = "..." + l.substring(l.length() - MAX_VERTEX_STRING_LENGTH + 3, l.length());
            }
            return l;
        } else if (vertexToHostMap.containsKey(v)) {
            return vertexToHostMap.get(v).toString();
        } else {
            return "";
        }
    }

    /** Returns shape of the block device vertex. */
    @Override
    protected Shape getVertexShape(final Vertex v, final VertexShapeFactory<Vertex> factory) {
        return factory.getRectangle(v);
    }

    /** Handles popup in when block device vertex is clicked. */
    @Override
    protected void handlePopupVertex(final Vertex v, final List<Vertex> pickedV, final Point2D pos) {
        final Info info;
        if (pickedV.size() > 1) {
            info = multiSelectionInfo;
        } else if (isVertexBlockDevice(v)) {
            info = getInfo(v);
        } else {
            /* host */
            info = getInfo(v);
        }
        if (info != null) {
            final JPopupMenu p = info.getPopup();
            info.updateMenus(pos);
            showPopup(p, pos);
        }
    }

    /** Adds drbd volume edge to the graph. */
    public void addDrbdVolume(final VolumeInfo dvi, final BlockDevInfo bdi1, final BlockDevInfo bdi2) {
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
            edgeToDrbdVolumeMap.put(e, dvi);
            drbdVolumeToEdgeMap.put(dvi, e);
        }
    }

    /** Returns the source block device in a drbd connection. */
    public BlockDevInfo getSource(final VolumeInfo dvi) {
        final Edge edge = drbdVolumeToEdgeMap.get(dvi);
        if (edge == null) {
            return null;
        }
        final Vertex source = edge.getSource();
        return (BlockDevInfo) getInfo(source);
    }

    /** Returns the destination block device in a drbd connection. */
    public BlockDevInfo getDest(final VolumeInfo dvi) {
        final Edge edge = drbdVolumeToEdgeMap.get(dvi);
        if (edge == null) {
            return null;
        }
        final Vertex dest = edge.getDest();
        return (BlockDevInfo) getInfo(dest);
    }

    /** Picks vertex, that is associated with the specified info object. */
    @Override
    public void pickInfo(final Info i) {
        final Edge e = drbdVolumeToEdgeMap.get(i);
        if (e == null) {
            super.pickInfo(i);
        } else {
            pickEdge(e);
        }
    }

    /** Is called after right click on the resource edge. */
    @Override
    protected void handlePopupEdge(final Edge edge, final Point2D pos) {
        final VolumeInfo info = edgeToDrbdVolumeMap.get(edge);
        final JPopupMenu p = info.getPopup();
        info.updateMenus(pos);
        showPopup(p, pos);
    }

    /**
     * Is called after right click on the background and it returns
     * background popup menu.
     */
    @Override
    protected void handlePopupBackground(final Point2D pos) {
        final GlobalInfo info = getDrbdInfo();
        final JPopupMenu p = info.getPopup();
        info.updateMenus(pos);
        showPopup(p, pos);
    }

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
    @Override
    protected void oneVertexPressed(final Vertex v) {
        if (isVertexBlockDevice(v)) {
            pickHost(v);
            final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
            if (bdi == null) {
                return;
            }
            globalInfo.setSelectedNode(bdi);
            globalInfo.selectMyself();
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
    @Override
    protected void vertexReleased(final Vertex v, final Point2D pos) {
        double x = pos.getX();
        double y = pos.getY();
        final double minPos = (getVertexWidth(v) - getDefaultVertexWidth(v)) / 2;
        x = x < minPos ? minPos : x;
        x = x > MAX_X_POS ? MAX_X_POS : x;
        y = y < MIN_Y_POS ? MIN_Y_POS : y;
        y = y > MAX_Y_POS ? MAX_Y_POS : y;
        pos.setLocation(x + (getDefaultVertexWidth(v) - getVertexWidth(v)) / 2, y);
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
    @Override
    protected void oneEdgePressed(final Edge e) {
        final VolumeInfo dvi = edgeToDrbdVolumeMap.get(e);
        if (dvi != null) {
            dvi.selectMyself();
        }
    }

    /**
     * Is called, when background of the graph is clicked. It deselects
     * selected node.
     */
    @Override
    protected void backgroundClicked() {
        globalInfo.setSelectedNode(null);
        globalInfo.selectMyself();
    }

    /**
     * Returns fill color as paint object for for specified block device
     * vertex.
     */
    @Override
    protected Color getVertexFillColor(final Vertex v) {
        final HostDrbdInfo hi = vertexToHostMap.get(v);
        final Vertex hostVertex = getVertex(hi);
        if (v.equals(hostVertex)) {
            return hi.getHost().getDrbdColors()[0];
        } else if (hi != null &&
                   (hi.getHost() == null || (!hi.getHost().isDrbdStatusOk() && hi.getHost().isDrbdLoaded()))) {
            return Tools.getDefaultColor("DrbdGraph.FillPaintUnknown");
        } else {
            if (!isVertexDrbd(v)) {
                if (isVertexAvailable(v)) {
                    return super.getVertexFillColor(v);
                } else {
                    return Tools.getDefaultColor("DrbdGraph.FillPaintNotAvailable");
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
     * Returns secondary gradient fill paint color for vertex v. If it is
     * a volume and there is previous volume don't show gradient.
     */
    @Override
    protected Color getVertexFillSecondaryColor(final Vertex v) {
        if (!isVertexBlockDevice(v)) {
            return Color.WHITE;
        }
        final BlockDevInfo bdi = (BlockDevInfo) getInfo(v);
        if (bdi == null) {
            return Color.WHITE;
        }
        if (bdi.isFirstDrbdVolume()) {
            return Color.WHITE;
        } else {
            return getVertexFillColor(v);
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
     * TODO: move it to BlockDevInfo
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
            if (bdi == null) {
                continue;
            }
            if (disk.equals(bdi.getName())
                || disk.equals(bdi.getBlockDevice().getDiskUuid())
                || bdi.getBlockDevice().getDiskIds().contains(disk)) {
                return bdi;
            }
        }
        return null;
    }

    /** Returns tool tip when mouse is over a block device vertex. */
    @Override
    String getVertexToolTip(final Vertex v) {
        final Info i = getInfo(v);
        if (i == null) {
            return null;
        }
        return i.getToolTipForGraph(getRunMode());
    }

    /** Returns tool tip when mouse is over a resource edge. */
    @Override
    String getEdgeToolTip(final Edge edge) {
        final VolumeInfo dvi = edgeToDrbdVolumeMap.get(edge);
        return dvi.getToolTipForGraph(getRunMode());
    }

    /**
     * Returns whether arrow shoud be shown. It is shown on the edge from
     * primary to secondory, or from connected node to the disconnected or
     * none at all.
     */
    @Override
    protected boolean showEdgeArrow(final Edge edge) {
        final BlockDevInfo sourceBDI = (BlockDevInfo) getInfo(edge.getSource());
        final BlockDevInfo destBDI = (BlockDevInfo) getInfo(edge.getDest());
        if (sourceBDI == null || destBDI == null) {
            return false;
        }
        final BlockDevice sourceBD = sourceBDI.getBlockDevice();
        final BlockDevice destBD = destBDI.getBlockDevice();
        final Application.RunMode runMode = getRunMode();
        if (sourceBDI.isConnected(runMode)
            && sourceBD.isPrimary() != destBD.isPrimary()) {
            return true;
        } else if (sourceBDI.isWFConnection(runMode) ^ destBDI.isWFConnection(runMode)) {
            /* show arrow from wf connection */
            return true;
        }
        return false;
    }

    /**
     * Returns the color of the edge, depending on if the drbds are connected
     * and so on.
     */
    @Override
    protected Paint getEdgeDrawPaint(final Edge edge) {
        final VolumeInfo dvi = edgeToDrbdVolumeMap.get(edge);
        if (dvi != null
            && dvi.isConnected(getRunMode())
            && !dvi.isSplitBrain()) {
            return super.getEdgeDrawPaint(edge);
        } else {
            return Tools.getDefaultColor("DrbdGraph.EdgeDrawPaintDisconnected");
        }

    }

    /**
     * Returns paint for picked edge. It returns different colors if drbd is
     * disconnected.
     */
    @Override
    protected Paint getEdgePickedPaint(final Edge edge) {
        final VolumeInfo dvi = edgeToDrbdVolumeMap.get(edge);
        if (dvi != null && dvi.isConnected(getRunMode()) && !dvi.isSplitBrain()) {
            return super.getEdgePickedPaint(edge);
        } else {
            return Tools.getDefaultColor("DrbdGraph.EdgeDrawPaintDisconnectedBrighter");
        }
    }

    /** Returns id that is used for saving of the vertex positions to a file. */
    @Override
    protected String getId(final Info i) {
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

    @Override
    protected int getDefaultVertexWidth(final Vertex v) {
        if (isVertexBlockDevice(v)) {
            return VERTEX_SIZE_BD;
        } else {
            return VERTEX_SIZE_HOST;
        }
    }

    @Override
    protected int getDefaultVertexHeight(final Vertex v) {
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
            if (bdi == null) {
                return 0;
            }
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
    @Override
    protected void drawInside(final Vertex v,
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
                drawInsideVertex(g2d, v, colors, x, y, height, width);
            }
        } else {
            final HostDrbdInfo hi = (HostDrbdInfo) getInfo(v);
            if (hi != null) {
                drawInsideVertex(g2d, v, hi.getHost().getDrbdColors(), x, y, height, width);
            }
        }
        final Application.RunMode runMode = getRunMode();
        if (used > 0) {
            /** Show how much is used. */
            final double freeWidth = width * (100 - used) / 100;
            g2d.setColor(new Color(255, 255, 255, 220));
            g2d.fillRect((int) (x + width - freeWidth), (int) (y), (int) (freeWidth), (int) (height));
        }
        if (isPicked(v)) {
            if (Application.isTest(runMode)) {
                g2d.setColor(Color.RED);
            } else {
                g2d.setColor(Color.BLACK);
            }
        } else {
            boolean pickedResource = false;
            if (Application.isTest(runMode)) {
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

    @Override
    protected boolean showHollowArrow(final Edge e) {
        final VolumeInfo dvi = edgeToDrbdVolumeMap.get(e);
        if (dvi == null) {
            return false;
        }
        return !dvi.isConnected(getRunMode());
    }

    /** Select multiple elements. */
    @Override
    protected void multiSelection() {
        final List<Info> selectedInfos = new ArrayList<Info>();
        for (final Vertex v : getPickedVertices()) {
            final Info i = getInfo(v);
            if (i != null) {
                selectedInfos.add(i);
            }
        }
        multiSelectionInfo = new MultiSelectionInfo(selectedInfos, getClusterBrowser());
        getClusterBrowser().setRightComponentInView(multiSelectionInfo);
    }

    Map<VolumeInfo, Edge> getDrbdVolumeToEdgeMap() {
        return drbdVolumeToEdgeMap;
    }
}
