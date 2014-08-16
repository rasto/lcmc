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

import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractPopupGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.GraphMouseListener;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.ViewScalingControl;
import edu.uci.ics.jung.visualization.decorators.AbstractEdgeShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.AbstractVertexShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.ConstantDirectionalEdgeValueTransformer;
import edu.uci.ics.jung.visualization.decorators.DirectionalEdgeArrowTransformer;
import edu.uci.ics.jung.visualization.decorators.PickableEdgePaintTransformer;
import edu.uci.ics.jung.visualization.decorators.PickableVertexPaintTransformer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.picking.PickedInfo;
import edu.uci.ics.jung.visualization.picking.PickedState;
import edu.uci.ics.jung.visualization.picking.ShapePickSupport;
import edu.uci.ics.jung.visualization.renderers.BasicVertexRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import edu.uci.ics.jung.visualization.util.VertexShapeFactory;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;

import lcmc.LCMC;
import lcmc.model.Application;
import lcmc.model.ColorText;
import lcmc.model.Host;
import lcmc.gui.resources.Info;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Tools;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.TransformerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * This class creates graph and provides methods for scaling etc.,
 * that are used in all graphs.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public abstract class ResourceGraph {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceGraph.class);
    /** Empty shape for arrows. (to not show an arrow). */
    private static final Shape EMPTY_SHAPE = new Area();
    /** Singleton instance of the Line2D edge shape. */
    private static final Shape INSTANCE = new Line2D.Float(0.0f, 0.0f, 1.0f, 0.0f);
    /** Singleton instance of dotted line edge shape. */
    private static final Path2D HOLLOW_INSTANCE = new Path2D.Float();
    private static final Paint EDGE_DRAW_PAINT = Tools.getDefaultColor("ResourceGraph.EdgeDrawPaint");
    private static final Paint EDGE_PICKED_PAINT = Tools.getDefaultColor("ResourceGraph.EdgePickedPaint");
    static {
        final float d = 0.05f;
        for (float i = 0; i < 1.0f; i += d) {
            HOLLOW_INSTANCE.moveTo(i, 0.0f);
            HOLLOW_INSTANCE.lineTo(i + d * 0.7, 0.0f);
        }
        HOLLOW_INSTANCE.lineTo(1.0f, 0.0f);
    }
    private final Renderer.Vertex<Vertex, Edge> pluggableRenderer = new MyPluggableRenderer<Vertex, Edge>();
    private final Map<Vertex, Info> vertexToInfoMap = new LinkedHashMap<Vertex, Info>();
    private final Map<Info, Vertex> infoToVertexMap = new LinkedHashMap<Info, Vertex>();
    private final Map<Edge, JPopupMenu> edgeToPopupMap = new LinkedHashMap<Edge, JPopupMenu>();
    private final Map<Vertex, List<MyMenuItem>> vertexToMenus = new LinkedHashMap<Vertex, List<MyMenuItem>>();
    private final Map<Edge, List<MyMenuItem>> edgeToMenus = new LinkedHashMap<Edge, List<MyMenuItem>>();
    private final Lock mGraphLock = new ReentrantLock();
    private Graph<Vertex, Edge> graph;
    private VisualizationViewer<Vertex, Edge> visualizationViewer;
    private StaticLayout<Vertex, Edge> layout;
    private GraphZoomScrollPane scrollPane;
    private final Lock mVertexLocationsLock = new ReentrantLock();
    private final Map<Vertex, Point2D> vertexLocations = new HashMap<Vertex, Point2D>();
    private ViewScalingControl myScaler;
    /** List with resources that should be animated. */
    private final Collection<Info> animationList = new ArrayList<Info>();
    private final Lock mAnimationListLock = new ReentrantLock();
    /** List with resources that should be animated for test view. */
    private final Collection<JComponent> testAnimationList = new ArrayList<JComponent>();
    /** This mutex is for protecting the test animation list. */
    private final Lock mTestAnimationListLock = new ReentrantLock();
    private volatile Thread animationThread = null;
    private final Lock mAnimationThreadLock = new ReentrantLock();
    private final Map<Vertex, Integer> vertexWidth = new HashMap<Vertex, Integer>();
    private final Map<Vertex, Integer> vertexHeight = new HashMap<Vertex, Integer>();
    /** Whether something in the graph changed that requires visualizationViewer to restart. */
    private volatile boolean changed = false;

    /** Whether only test or real thing should show. */
    private volatile Application.RunMode runModeFlag = Application.RunMode.LIVE;
    private final Lock mRunModeFlag = new ReentrantLock();
    private volatile Thread testAnimationThread = null;
    private final Lock mTestAnimationThreadLock = new ReentrantLock();
    /** List of edges that are made only during test. */
    private volatile Edge testEdge = null;
    /** List of edges that are being tested during test. */
    private volatile Edge existingTestEdge = null;
    private final Lock mTestEdgeLock = new ReentrantLock();
    @Autowired
    private Application application;
    private final Map<String, TextLayout> textLayoutCache = new HashMap<String, TextLayout>();
    private double scaledSoFar = 1.0;

    private ClusterBrowser clusterBrowser;
    @Autowired
    private GUIData guiData;

    /** Starts the animation if vertex is being updated. */
    public final void startAnimation(final Info info) {
        final int animInterval = (int) (1000 / application.getAnimFPS());
        mAnimationListLock.lock();
        if (animationList.isEmpty()) {
            /* start animation thread */
            mAnimationThreadLock.lock();
            if (animationThread == null) {
                animationThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            try {
                                Thread.sleep(animInterval);
                            } catch (final InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }

                            mAnimationListLock.lock();
                            if (animationList.isEmpty()) {
                                mAnimationListLock.unlock();
                                repaint();
                                mAnimationThreadLock.lock();
                                animationThread = null;
                                mAnimationThreadLock.unlock();
                                break;
                            }
                            for (final Info animation : animationList) {
                                animation.incAnimationIndex();
                            }
                            mAnimationListLock.unlock();
                            repaint();
                        }
                    }
                });
                animationThread.start();
            }
            mAnimationThreadLock.unlock();
        }
        animationList.add(info);
        mAnimationListLock.unlock();
    }

    public final void stopAnimation(final Info info) {
        mAnimationListLock.lock();
        try {
            animationList.remove(info);
        } finally {
            mAnimationListLock.unlock();
        }
    }

    /** Starts the animation if vertex is being tested. */
    public final void startTestAnimation(final JComponent component, final CountDownLatch startTestLatch) {
        final int animInterval = (int) (1000 / application.getAnimFPS());
        mTestAnimationListLock.lock();
        mRunModeFlag.lock();
        runModeFlag = Application.RunMode.LIVE;
        mRunModeFlag.unlock();
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                Tools.setMenuOpaque(component, false);
            }
        });
        if (testAnimationList.isEmpty()) {
            mTestAnimationThreadLock.lock();
            if (testAnimationThread == null) {
                /* start test animation thread */
                testAnimationThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        FOREVER: while (true) {
                            try {
                                startTestLatch.await();
                            } catch (final InterruptedException ignored) {
                                Thread.currentThread().interrupt();
                            }
                            mRunModeFlag.lock();
                            final Application.RunMode runModeFlagLast;
                            try {
                                /* invert run mode */
                                if (Application.isTest(runModeFlag)) {
                                    runModeFlag = Application.RunMode.LIVE;
                                } else {
                                    runModeFlag = Application.RunMode.TEST;
                                }
                                runModeFlagLast = runModeFlag;
                            } finally {
                                mRunModeFlag.unlock();
                            }
                            repaint();
                            int sleep = 300;
                            if (Application.isTest(runModeFlag)) {
                                sleep = 1200;
                            }
                            for (int s = 0; s < sleep; s += animInterval) {
                                mRunModeFlag.lock();
                                if (runModeFlag == runModeFlagLast) {
                                    mRunModeFlag.unlock();
                                } else {
                                    mRunModeFlag.unlock();
                                    repaint();
                                }
                                if (!component.isShowing()) {
                                    stopTestAnimation(component);
                                }
                                mTestAnimationListLock.lock();

                                if (testAnimationList.isEmpty()) {
                                    mTestAnimationListLock.unlock();
                                    mRunModeFlag.lock();
                                    try {
                                        runModeFlag = Application.RunMode.LIVE;
                                    } finally {
                                        mRunModeFlag.unlock();
                                    }
                                    repaint();
                                    mTestAnimationThreadLock.lock();
                                    try {
                                        testAnimationThread = null;
                                    } finally {
                                        mTestAnimationThreadLock.unlock();
                                    }
                                    break FOREVER;
                                }
                                mTestAnimationListLock.unlock();
                                Tools.sleep(animInterval);
                            }
                        }
                    }
                });
                testAnimationThread.start();
            }
            mTestAnimationThreadLock.unlock();
        }
        testAnimationList.add(component);
        mTestAnimationListLock.unlock();
    }

    public final void stopTestAnimation(final JComponent component) {
        mTestAnimationListLock.lock();
        try {
            testAnimationList.remove(component);
        } finally {
            mTestAnimationListLock.unlock();
        }
        removeExistingTestEdge();
        removeTestEdge();
        Tools.setMenuOpaque(component, true);
    }

    final boolean isTestAnimation() {
        mTestAnimationListLock.lock();
        boolean running;
        try {
            running = !testAnimationList.isEmpty();
        } finally {
            mTestAnimationListLock.unlock();
        }
        return running;
    }

    protected void initGraph(final ClusterBrowser clusterBrowser) {
        this.clusterBrowser = clusterBrowser;

    }

    protected final void initGraph(final Graph<Vertex, Edge> graph) {
        this.graph = graph;

        final Transformer<Vertex, Point2D> vlf = TransformerUtils.mapTransformer(getVertexLocations());
        putVertexLocations();

        layout = new StaticLayout<Vertex, Edge>(graph, vlf) {
            /* remove the adjust locations part, because scaling is from 0, 0 */
            @Override
            public void setSize(final Dimension size) {
                if (size != null) {
                    this.size = size;
                    initialize();
                }
            }
        };
        visualizationViewer = new VisualizationViewer<Vertex, Edge>(layout);
        visualizationViewer.getRenderContext().setEdgeArrowTransformer(new MyEdgeArrowFunction<Vertex, Edge>());
        visualizationViewer.getRenderContext().setEdgeLabelClosenessTransformer(
                                                new ConstantDirectionalEdgeValueTransformer<Vertex, Edge>(0.5, 0.5));
        visualizationViewer.getRenderContext().setVertexShapeTransformer(
                                                    new MyVertexShapeSize<Vertex, Edge>(graph, vlf));
        visualizationViewer.getRenderContext().setVertexFillPaintTransformer(
                new MyPickableVertexPaintFunction<Vertex>(visualizationViewer.getPickedVertexState(), false));
        visualizationViewer.getRenderContext().setVertexDrawPaintTransformer(
                        new MyPickableVertexPaintFunction<Vertex>(visualizationViewer.getPickedVertexState(), true));
        visualizationViewer.getRenderer().setVertexRenderer(pluggableRenderer);

        visualizationViewer.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller<Edge>());
        visualizationViewer.setBackground(Tools.getDefaultColor("ResourceGraph.Background"));
        visualizationViewer.setVertexToolTipTransformer(new MyVertexToolTipFunction<Vertex>());
        visualizationViewer.setEdgeToolTipTransformer(new MyEdgeToolTipFunction<Edge>());
        visualizationViewer.getRenderContext().setEdgeShapeTransformer(new MyLine<Vertex, Edge>());
        visualizationViewer.getRenderContext().setEdgeDrawPaintTransformer(
                                    new MyPickableEdgePaintFunction<Edge>(visualizationViewer.getPickedEdgeState(),
                                                                          EDGE_DRAW_PAINT,
                                                                          EDGE_PICKED_PAINT));
        visualizationViewer.getRenderContext().setEdgeFillPaintTransformer(
                                    new MyPickableEdgePaintFunction<Edge>(visualizationViewer.getPickedEdgeState(),
                                                                          EDGE_DRAW_PAINT,
                                                                          EDGE_PICKED_PAINT));
        visualizationViewer.getRenderContext().setArrowDrawPaintTransformer(
                                    new MyPickableEdgePaintFunction<Edge>(visualizationViewer.getPickedEdgeState(),
                                                                          EDGE_DRAW_PAINT,
                                                                          EDGE_PICKED_PAINT));
        visualizationViewer.getRenderContext().setArrowFillPaintTransformer(
                                    new MyPickableArrowEdgePaintFunction<Edge>(
                                                                          visualizationViewer.getPickedEdgeState(),
                                                                          EDGE_DRAW_PAINT,
                                                                          EDGE_PICKED_PAINT));

        /* scaling */
        /* overwriting scaler so that zooming starts from point (0, 0) */
        myScaler = getScalingControl();

        /* picking and popups */
        /* overwriting loadPlugins method only to set scaler */
        final DefaultModalGraphMouse<Vertex, Edge> graphMouse =
            new DefaultModalGraphMouse<Vertex, Edge>() {
            @Override
                protected void loadPlugins() {
                    super.loadPlugins();
                    ((ScalingGraphMousePlugin) scalingPlugin).setScaler(myScaler);
                    remove(animatedPickingPlugin);
                    animatedPickingPlugin = null;
                }
        };
        visualizationViewer.setGraphMouse(graphMouse);
        graphMouse.add(new MyPopupGraphMousePlugin<Vertex, Edge>());
        visualizationViewer.addGraphMouseListener(new MyGraphMouseListener<Vertex>());
        visualizationViewer.setPickSupport(new ShapePickSupport<Vertex, Edge>(visualizationViewer, 50));
        graphMouse.setMode(ModalGraphMouse.Mode.PICKING);
        layout.initialize();
        scrollPane = new GraphZoomScrollPane(visualizationViewer);
        final JScrollBar vScrollBar = scrollPane.getVerticalScrollBar();
        visualizationViewer.addMouseWheelListener(
            new MouseWheelListener() {
                @Override
                public void mouseWheelMoved(final MouseWheelEvent e) {
                    if ((e.getModifiers() & MouseWheelEvent.CTRL_MASK) > 0) {
                        final int amount = e.getWheelRotation();
                        vScrollBar.setValue(vScrollBar.getValue() + amount * 20);
                        e.consume();
                        visualizationViewer.repaint();

                    }
                }
            });
    }

    /** Repaints the graph. */
    public final void repaint() {
        visualizationViewer.repaint();
    }

    protected final Graph<Vertex, Edge> getGraph() {
        return graph;
    }

    /** Returns the vertex locations function and locks them. Must be followed
        by putVertexLocations. */
    protected final Map<Vertex, Point2D> getVertexLocations() {
        mVertexLocationsLock.lock();
        return vertexLocations;
    }

    protected final void putVertexLocations() {
        mVertexLocationsLock.unlock();
    }

    protected final StaticLayout<Vertex, Edge> getLayout() {
        return layout;
    }

    public final VisualizationViewer<Vertex, Edge> getVisualizationViewer() {
        return visualizationViewer;
    }

    /** Returns the hash with vertex to menus map. */
    protected final Map<Vertex, List<MyMenuItem>> getVertexToMenus() {
        return vertexToMenus;
    }

    /** Returns the vertex that represents the specified resource. */
    public Vertex getVertex(final Info i) {
        final Vertex v = infoToVertexMap.get(i);
        if (v == null) {
            LOG.debug1("getVertex: no vertex for: " + i);
        }
        return v;
    }

    /** Removes the vertex that represents the specified resource. */
    protected final void removeVertex(final Info i) {
        infoToVertexMap.remove(i);
    }

    /** Inserts the hash that maps resource info to its vertex. */
    protected final void putInfoToVertex(final Info i, final Vertex v) {
        infoToVertexMap.remove(i);
        infoToVertexMap.put(i, v);
    }

    /** Returns all resources. */
    public final Iterable<Info> infoToVertexKeySet() {
        return infoToVertexMap.keySet();
    }

    /** Returns the resource info object for specified vertex v. */
    protected final Info getInfo(final Vertex v) {
        return vertexToInfoMap.get(v);
    }

    /** Removes the specified vertex from the hash. */
    protected final void removeInfo(final Vertex v) {
        LOG.debug1("removeInfo: vertex: " + vertexToInfoMap.get(v));
        vertexToInfoMap.remove(v);
    }

    /** Puts the vertex to resource info object map to the hash. */
    protected final void putVertexToInfo(final Vertex v, final Info i) {
        LOG.debug1("putVertexToInfo: vertex: " + i);
        vertexToInfoMap.put(v, i);
    }

    /** Removes popup menu for the edge e. */
    protected final void removePopup(final Edge e) {
        edgeToPopupMap.remove(e);
    }

    /**
     * Scales the graph, so that all vertices can be seen. The graph can
     * get smaller but not bigger.
     */
    public void scale() {
        final Point2D max = getLastPosition();
        final float maxXPos = (float) max.getX();
        final float maxYPos = (float) max.getY();
        if (maxXPos <= 0 || maxYPos <= 0) {
            return;
        }
        final Float vvX = new Float(getLayout().getSize().getWidth());
        final Float vvY = new Float(getLayout().getSize().getHeight());
        if (maxXPos > vvX || maxYPos > vvY) {
            final float x = maxXPos > vvX ? maxXPos : vvX;
            final float y = maxYPos > vvY ? maxYPos : vvY;
            getLayout().setSize(new Dimension((int) x, (int) y));
            visualizationViewer.setGraphLayout(getLayout());
        }
        if (changed) {
            somethingChangedReset();
        }
        visualizationViewer.repaint();
    }

    /** Returns position adjusted to scrollbar. */
    protected Point2D posWithScrollbar(final Point2D oldPos) {
        final double newX = oldPos.getX() + scrollPane.getHorizontalScrollBar().getValue();
        final double newY = oldPos.getY() + scrollPane.getVerticalScrollBar().getValue();
        return new Point2D.Double(newX, newY);
    }

    /** Returns graph in the scroll pane. */
    public final JPanel getGraphPanel() {
        return scrollPane;
    }

    /** Returns the scrollpane. */
    final GraphZoomScrollPane getScrollPane() {
        return scrollPane;
    }

    protected abstract String getMainText(final Vertex v, final Application.RunMode runMode);

    protected abstract String getLabelForEdgeStringer(Edge e);

    abstract String getVertexToolTip(final Vertex v);

    abstract String getEdgeToolTip(final Edge edge);

    protected int getVertexWidth(final Vertex v) {
        if (vertexWidth.containsKey(v)) {
            return vertexWidth.get(v);
        } else {
            return getDefaultVertexWidth(v);
        }
    }

    protected int getVertexHeight(final Vertex v) {
        if (vertexHeight.containsKey(v)) {
            return vertexHeight.get(v);
        } else {
            return getDefaultVertexHeight(v);
        }
    }

    protected int getDefaultVertexWidth(final Vertex v) {
        return 1;
    }

    protected int getDefaultVertexHeight(final Vertex v) {
        return 1;
    }

    protected void setVertexWidth(final Vertex v, final int size) {
        vertexWidth.put(v, size);
    }

    protected void setVertexHeight(final Vertex v, final int size) {
        vertexHeight.put(v, size);
    }

    protected float getVertexAspectRatio(final Vertex v) {
        return getVertexHeight(v) / (float) getVertexWidth(v);
    }

    protected Shape getVertexShape(final Vertex v, final VertexShapeFactory<Vertex> factory) {
        return factory.getEllipse(v);
    }

    /** Handles right click on the vertex. */
    protected abstract void handlePopupVertex(final Vertex vertex, final List<Vertex> pickedV, final Point2D pos);

    /** Handles right click on the edge. */
    protected abstract void handlePopupEdge(final Edge edge, final Point2D pos);

    /** Handles right click on the background. */
    protected abstract void handlePopupBackground(final Point2D pos);

    public final void updatePopupMenus() {
        mGraphLock.lock();
        try {
            for (final Vertex v : graph.getVertices()) {
                vertexToMenus.remove(v);
            }
            for (final Edge e : graph.getEdges()) {
                edgeToMenus.remove(e);
                updatePopupEdge(e);
            }
        } finally {
            mGraphLock.unlock();
        }
    }

    protected final void updatePopupEdge(final Edge edge) {
        final List<MyMenuItem> menus = edgeToMenus.get(edge);
        if (menus != null) {
            for (final MyMenuItem menu : menus) {
                menu.updateAndWait();
            }
        }
    }

    protected final void showPopup(final JPopupMenu popup, final Point2D p) {
        final int posX = (int) p.getX();
        final int posY = (int) p.getY();
        application.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                if (visualizationViewer.isShowing() && visualizationViewer.isDisplayable()) {
                    popup.show(visualizationViewer, posX, posY);
                    popup.repaint();
                }
            }
        });
    }

    /** Removes info from the graph. */
    protected void removeInfo(final Info i) {
        mGraphLock.lock();
        try {
            graph.removeVertex(getVertex(i));
        } finally {
            mGraphLock.unlock();
        }
    }

    /** Picks and highlights vertex with Info i in the graph. */
    void pickInfo(final Info i) {
        mGraphLock.lock();
        try {
            final Vertex v = getVertex(i);
            final PickedState<Edge> psEdge = visualizationViewer.getRenderContext().getPickedEdgeState();
            final PickedState<Vertex> psVertex = visualizationViewer.getRenderContext().getPickedVertexState();
            psEdge.clear();
            psVertex.clear();
            psVertex.pick(v, true);
        } finally {
            mGraphLock.unlock();
        }
    }

    /** Picks edge e. */
    final void pickEdge(final Edge e) {
        mGraphLock.lock();
        try {
            final PickedState<Edge> psEdge = visualizationViewer.getRenderContext().getPickedEdgeState();
            final PickedState<Vertex> psVertex = visualizationViewer.getRenderContext().getPickedVertexState();
            psEdge.clear();
            psVertex.clear();
            psEdge.pick(e, true);
        } finally {
            mGraphLock.unlock();
        }
    }

    /** Picks vertex v. */
    final void pickVertex(final Vertex v) {
        mGraphLock.lock();
        try {
            final PickedState<Edge> psEdge = visualizationViewer.getRenderContext().getPickedEdgeState();
            final PickedState<Vertex> psVertex = visualizationViewer.getRenderContext().getPickedVertexState();
            psEdge.clear();
            psVertex.clear();
            psVertex.pick(v, true);
        } finally {
            mGraphLock.unlock();
        }
    }

    /** Picks background. */
    public final void pickBackground() {
        mGraphLock.lock();
        try {
            final PickedState<Edge> psEdge = visualizationViewer.getRenderContext().getPickedEdgeState();
            final PickedState<Vertex> psVertex = visualizationViewer.getRenderContext().getPickedVertexState();
            psEdge.clear();
            psVertex.clear();
        } finally {
            mGraphLock.unlock();
        }
    }

    protected abstract void vertexReleased(final Vertex v, Point2D pos);

    protected abstract void oneVertexPressed(Vertex v);

    protected abstract void oneEdgePressed(final Edge e);

    protected abstract void backgroundClicked();

    /** Retuns border paint color for vertex v. */
    protected final Paint getVertexDrawPaint(final Vertex v) {
        return Tools.getDefaultColor("ResourceGraph.DrawPaint");
    }

    /** Retuns border paint color of not picked vertex v, null for no border. */
    protected Paint getVertexDrawPaintNotPicked(final Vertex v) {
        return null;
    }

    /** Returns fill paint color for vertex v. */
    protected Color getVertexFillColor(final Vertex v) {
        return Tools.getDefaultColor("ResourceGraph.FillPaint");
    }

    /** Returns secondary gradient fill paint color for vertex v. */
    protected Color getVertexFillSecondaryColor(final Vertex v) {
        return Color.WHITE;
    }

    /** Returns whether the vertex is picked. */
    final boolean isPicked(final Vertex v) {
        return visualizationViewer.getPickedVertexState().isPicked(v);
    }

    /** Returns whether the edge is picked. */
    final boolean isPicked(final Edge e) {
        return visualizationViewer.getPickedEdgeState().isPicked(e);
    }

    protected Paint getEdgeDrawPaint(final Edge e) {
        return EDGE_DRAW_PAINT;
    }

    protected Paint getEdgePickedPaint(final Edge e) {
        return EDGE_PICKED_PAINT;
    }

    protected boolean showHollowArrow(final Edge e) {
        return false;
    }

    protected abstract boolean showEdgeArrow(final Edge e);

    protected abstract List<ImageIcon> getIconsForVertex(final Vertex v, final Application.RunMode runMode);

    protected final void drawInsideVertex(final Graphics2D g2d,
                                          final Vertex v,
                                          final Color[] colors,
                                          final double x,
                                          final double y,
                                          final float height,
                                          final float width) {
        final int number = colors.length;
        if (number > 1) {
            for (int i = 1; i < number; i++) {
                final Paint p = new GradientPaint((float) x + width / number,
                                                  (float) y,
                                                  getVertexFillSecondaryColor(v),
                                                  (float) x + width / number,
                                                  (float) y + height,
                                                  colors[i],
                                                  false);
                g2d.setPaint(p);
                final Shape s = new Rectangle2D.Double(x + width / 2 + (width / number / 2) * i,
                                                       y,
                                                       width / number / 2,
                                                       height - 2);
                g2d.fill(s);
            }
        }
    }
    /** This method must be overridden to draw something in the vertex. */
    protected abstract void drawInside(final Vertex v,
                                       final Graphics2D g2d,
                                       final double x,
                                       final double y,
                                       final Shape shape);

    /** Small text that appears above the icon. */
    protected abstract String getIconText(final Vertex v, final Application.RunMode runMode);

    /** Small text that appears in the right corner. */
    protected abstract ColorText getRightCornerText(final Vertex v, final Application.RunMode runMode);

    /** Small text that appears down. */
    protected abstract ColorText[] getSubtexts(final Vertex v, final Application.RunMode runMode);
    /** Returns positions of the vertices (by value). */
    final void getPositions(final Map<String, Point2D> positions) {
        mGraphLock.lock();
        try {
            for (final Vertex v : graph.getVertices()) {
                final Info info = getInfo(v);
                final Point2D p = new Point2D.Double();
                final Point2D loc = layout.transform(v);
                if (loc == null) {
                    continue;
                }
                p.setLocation(loc);
                p.setLocation(p.getX() + (getDefaultVertexWidth(v) - getVertexWidth(v)) / 2,
                              p.getY() + (getDefaultVertexHeight(v) - getVertexHeight(v)) / 2);
                if (info != null) {
                    final String id = getId(info);
                    if (id != null) {
                        positions.put(id, p);
                    }
                }
            }
        } finally {
            mGraphLock.unlock();
        }
    }

    /** Returns id that is used for saving of the vertex positions to a file. */
    protected abstract String getId(final Info i);

    /** Select multiple services. */
    protected abstract void multiSelection();

    /** Returns saved position for the specified resource. */
    final Point2D getSavedPosition(final Info info) {
        if (info == null) {
            return null;
        }
        final Host[] hosts = clusterBrowser.getClusterHosts();
        Point2D p = null;
        for (final Host host : hosts) {
            p = host.getGraphPosition(getId(info));
            if (p != null) {
                break;
            }
        }
        return p;
    }

    /** Reset saved position for the specified resource. */
    final void resetSavedPosition(final Info info) {
        final Host[] hosts = clusterBrowser.getClusterHosts();
        for (final Host host : hosts) {
            host.resetGraphPosition(getId(info));
        }
    }

    /** Returns layout of the text that will be drawn on the vertex. */
    private TextLayout getVertexTextLayout(final Graphics2D g2d, final String text, final double fontSizeFactor) {
        final TextLayout ctl = textLayoutCache.get(fontSizeFactor + ":" + text);
        if (ctl != null) {
            return ctl;
        }
        final Font font = LCMC.MAIN_FRAME.getFont();
        final FontRenderContext context = g2d.getFontRenderContext();
        final TextLayout tl = new TextLayout(text,
                                             new Font(font.getName(),
                                                      font.getStyle(),
                                                      (int) (font.getSize() * fontSizeFactor)),
                                             context);
        textLayoutCache.put(fontSizeFactor + ":" + text, tl);
        return tl;
    }

    /** Draws text on the vertex. */
    private void drawVertexText(final Graphics2D g2d,
                                final TextLayout textLayout,
                                final double x,
                                final double y,
                                final Color color,
                                final int alpha) {
        if (color != null) {
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        }
        textLayout.draw(g2d, (float) x, (float) y);
    }

    /** Resets something changed flag. */
    private void somethingChangedReset() {
        changed = false;
    }

    /** Is called when something in the graph changed. */
    protected final void somethingChanged() {
        changed = true;
    }

    /** Return the run mode. */
    protected final Application.RunMode getRunMode() {
        mRunModeFlag.lock();
        try {
            return runModeFlag;
        } finally {
            mRunModeFlag.unlock();
        }
    }

    /** Returns if it test animation is running. */
    protected final boolean isRunModeTestAnimation() {
        mTestAnimationListLock.lock();
        try {
            return !testAnimationList.isEmpty();
        } finally {
            mTestAnimationListLock.unlock();
        }
    }

    protected final void removeTestEdge() {
        if (testEdge != null) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    mTestEdgeLock.lock();
                    mGraphLock.lock();
                    try {
                        getGraph().removeEdge(testEdge);
                    } finally {
                        mGraphLock.unlock();
                    }
                    testEdge = null;
                    mTestEdgeLock.unlock();
                }
            });
        }
    }

    protected final void addTestEdge(final Vertex vP, final Vertex v) {
        if (vP == null || v == null) {
            throw new IllegalArgumentException("addTestEdge: vP: " + vP + ", v: " + v);
        }
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!mTestEdgeLock.tryLock()) {
                    return;
                }
                if (testEdge != null) {
                    mGraphLock.lock();
                    try {
                        getGraph().removeEdge(testEdge);
                    } finally {
                        mGraphLock.unlock();
                    }
                }
                if (!isTestAnimation()) {
                    mTestEdgeLock.unlock();
                    return;
                }
                final Edge edge = new Edge(vP, v);
                mGraphLock.lock();
                try {
                    getGraph().addEdge(edge, vP, v);
                } finally {
                    mGraphLock.unlock();
                }
                testEdge = edge;
                mTestEdgeLock.unlock();
            }
        });
    }

    /** Adds an existing edge to the test edges. */
    protected final void addExistingTestEdge(final Edge edge) {
        if (!mTestEdgeLock.tryLock()) {
            return;
        }
        if (!isTestAnimation()) {
            existingTestEdge = null;
            mTestEdgeLock.unlock();
            return;
        }
        existingTestEdge = edge;
        mTestEdgeLock.unlock();
    }

    /** Removes existing test edges. */
    protected final void removeExistingTestEdge() {
        mTestEdgeLock.lock();
        try {
            existingTestEdge = null;
        } finally {
            mTestEdgeLock.unlock();
        }
    }

    /** Returns whether the edge is a test edge. */
    protected final boolean isTestEdge(final Edge e) {
        mTestEdgeLock.lock();
        try {
            return testEdge == e || existingTestEdge == e;
        } finally {
            mTestEdgeLock.unlock();
        }
    }

    /** Locking graph's vertex and edge lists. */
    protected final void lockGraph() {
        mGraphLock.lock();
    }

    /** Unlocking graph's vertex and edge lists. */
    protected final void unlockGraph() {
        mGraphLock.unlock();
    }

    protected final ViewScalingControl getScalingControl() {
        return new ViewScalingControl() {
            void superScale(final VisualizationServer thisVV, final float amount, final Point2D from) {
                super.scale(thisVV, amount, from);
            }

            @Override
            public void scale(final VisualizationServer vv, final float amount, final Point2D from) {
                final JScrollBar sbV = getScrollPane().getVerticalScrollBar();
                final JScrollBar sbH = getScrollPane().getHorizontalScrollBar();
                application.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        final Point2D prevPoint = getVisualizationViewer().getRenderContext()
                                                                          .getMultiLayerTransformer()
                                                                          .inverseTransform(Layer.VIEW, from);
                        final double scaledSoFar0 = getScaledSoFar();
                        float am = amount;
                        if (am < 1) {
                            if (scaledSoFar0 < 0.3) {
                                am = 1;
                            } else {
                                superScale(vv, 1 / am, new Point2D.Double(0, 0));
                            }
                        } else if (am > 1) {
                            if (scaledSoFar0 > 5) {
                                am = 1;
                            } else {
                                superScale(vv, 1 / am, new Point2D.Double(0, 0));
                            }
                        }
                        setScaledSoFar(scaledSoFar0 * am);
                        final Point2D p2 =
                            getVisualizationViewer().getRenderContext()
                                                    .getMultiLayerTransformer()
                                                    .inverseTransform(Layer.VIEW, from);
                        final int valueY = (int) (sbV.getValue() + prevPoint.getY() - p2.getY());
                        sbV.setValue(valueY);
                        sbV.repaint();
                        final int valueX = (int) (sbH.getValue() + prevPoint.getX() - p2.getX());
                        sbH.setValue(valueX);
                        sbH.repaint();

                        vv.repaint();
                    }
                });
            }
        };
    }

    protected final double getScaledSoFar() {
        return scaledSoFar;
    }

    protected final void setScaledSoFar(final double scaledSoFar) {
        this.scaledSoFar = scaledSoFar;
    }

    /** Returns position of the last vertex. */
    protected final Point2D getLastPosition() {
        double lastX = 0;
        double lastY = 0;
        final Map<Vertex, Point2D> vl = getVertexLocations();
        for (final Map.Entry<Vertex, Point2D> localtionEntry : vl.entrySet()) {
            final Point2D last = localtionEntry.getValue();
            if (last != null) {
                if (last.getX() > lastX) {
                    lastX = last.getX();
                }
                if (last.getY() > lastY) {
                    lastY = last.getY();
                }
            }
        }
        putVertexLocations();
        return new Point2D.Double(lastX, lastY + 40);
    }

    /** Get selected components for copy/paste. */
    public List<Info> getSelectedComponents() {
        final String cn = clusterBrowser.getCluster().getName();
        guiData.startProgressIndicator(cn, "copy");
        final List<Info> selected = new ArrayList<Info>();
        for (final Vertex v : getPickedVertices()) {
            final Info i = getInfo(v);
            selected.add(i);
        }
        guiData.stopProgressIndicator(cn, "copy");
        return selected;
    }

    public final Point2D getLocation(final Info i) {
        return layout.transform(getVertex(i));
    }

    /** Number of vertices. It is used to check in tests. */
    public int getNumberOfVertices() {
        return getGraph().getVertices().size();
    }

    /** Return picked vertices. */
    protected List<Vertex> getPickedVertices() {
        final PickedState<Vertex> ps = visualizationViewer.getRenderContext().getPickedVertexState();
        /* workaround for ConcurrentModificationException */
        for (int i = 0; i < 3; i++) {
            try {
                return new ArrayList<Vertex>(ps.getPicked());
            } catch (final ConcurrentModificationException cme) {
                /* try it again */
                LOG.appWarning("getPickedVertices: ignoring " + "ConcurrentModificationException");
            }
        }
        return new ArrayList<Vertex>();
    }

    /** This class allows to change direction of the edge. */
    protected static class Vertex {
        Vertex() {
            super();
        }

        @Override
        public String toString() {
            return "V";
        }
    }

    /** This class allows to change direction of the edge. */
    protected class Edge {
        private Vertex mFrom;
        private Vertex mTo;
        private final Vertex origFrom;
        private final Vertex origTo;
        /** Colocation in the same direction as an order. */
        private boolean wrongColocation = false;

        Edge(final Vertex from, final Vertex to) {
            mFrom = from;
            mTo = to;
            origFrom = from;
            origTo   = to;
        }

        final Vertex getSource() {
            return mFrom;
        }

        final Vertex getDest() {
            return mTo;
        }

        /** Reverse direction of the edge. */
        void reverse() {
            setDirection(mTo, mFrom);
        }

        /** Sets direction of the edge. */
        void setDirection(final Vertex from, final Vertex to) {
            final Edge thisEdge = this;
            if (mFrom != from || mTo != to) {
                mGraphLock.lock();
                try {
                    getGraph().removeEdge(thisEdge);
                    mFrom = from;
                    mTo   = to;
                    getGraph().addEdge(thisEdge, mFrom, mTo);
                } finally {
                    mGraphLock.unlock();
                }
            }
        }

        /** Sets direction to the original state. */
        void reset() {
            setDirection(origFrom, origTo);
        }

        /** Returns edge label. */
        @Override
        public final String toString() {
            return ' ' + getLabelForEdgeStringer(this) + ' ';
        }

        void setWrongColocation(final boolean wrongColocation) {
            this.wrongColocation = wrongColocation;
        }

        boolean isWrongColocation() {
            return wrongColocation;
        }
    }

    /** This class provides tool tips for the vertices. */
    class MyVertexToolTipFunction<V> implements Transformer<V, String> {

        /** Returns tool tip for vertex v. */
        @Override
        public String transform(final V v) {
            return Tools.html(getVertexToolTip((Vertex) v));
        }
    }

    /** This class provides tool tips for the vertices. */
    class MyEdgeToolTipFunction<E> implements Transformer<E, String> {

        /** Returns tool tip for edge. */
        @Override
        public String transform(final E edge) {
            return Tools.html(getEdgeToolTip((Edge) edge));
        }
    }

    /** Controls the shape, size, and aspect ratio for each vertex. */
    private final class MyVertexShapeSize<V, E> extends AbstractVertexShapeTransformer<V> {
        private final Transformer<Vertex, Point2D> vlf;
        private final Graph<V, E> graph;

        MyVertexShapeSize(final Graph<V, E> graphIn, final Transformer<Vertex, Point2D> vlfIn) {
            super();
            graph = graphIn;
            vlf = vlfIn;
            setSizeTransformer(new Transformer<V, Integer>() {
                                   @Override
                                   public Integer transform(final V v) {
                                       return getVertexWidth((Vertex) v);
                                   }
                               });
            setAspectRatioTransformer(new Transformer<V, Float>() {
                                          @Override
                                          public Float transform(final V v) {
                                              return getVertexAspectRatio((Vertex) v);
                                          }
                                      });
        }

        @SuppressWarnings("unchecked")
        @Override
        public Shape transform(final V v) {
            return getVertexShape((Vertex) v, (VertexShapeFactory<Vertex>) factory);
        }
    }

    /** This class handles popup menus in the graph. */
    class MyPopupGraphMousePlugin<V, E> extends AbstractPopupGraphMousePlugin {

        MyPopupGraphMousePlugin() {
            this(MouseEvent.BUTTON3_MASK);
        }

        MyPopupGraphMousePlugin(final int modifiers) {
            super(modifiers);
        }

        /**
         * Is called when mouse was released. Create a multi selection object. */
        @Override
        public void mouseReleased(final MouseEvent e) {
            if (getPickedVertices().size() > 1) {
                multiSelection();
            }
            if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) {
                handlePopup0(e);
            }
        }

        /** Is called when mouse was clicked. */
        @Override
        public void mouseClicked(final MouseEvent e) {

            super.mouseClicked(e);
            final PickedState<Edge> psEdge = visualizationViewer.getRenderContext().getPickedEdgeState();
            if (psEdge.getPicked().size() == 1) {
                final Edge edge = (Edge) psEdge.getPicked().toArray()[0];
                oneEdgePressed(edge);
            } else if (getPickedVertices().isEmpty() && psEdge.getPicked().isEmpty()) {
                backgroundClicked();
            }
        }

        /** Creates and displays popup menus for vertices and edges. */
        @Override
        protected void handlePopup(final MouseEvent me) {
            /* doesn't work on Windows along the mouseReleased handler. */
        }

        /** Creates and displays popup menus for vertices and edges. */
        private void handlePopup0(final MouseEvent me) {
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    final Point2D popP = me.getPoint();

                    final int posX = (int) popP.getX();
                    final int posY = (int) popP.getY();

                    final GraphElementAccessor<Vertex, Edge> pickSupport = visualizationViewer.getPickSupport();
                    final Vertex v = pickSupport.getVertex(layout, posX, posY);
                    if (v == null) {
                        final Edge edge = pickSupport.getEdge(layout, posX, posY);
                        if (edge == null) {
                            /* background was clicked */
                            handlePopupBackground(popP);
                            backgroundClicked();
                        } else {
                            handlePopupEdge(edge, popP);
                            oneEdgePressed(edge);
                        }
                    } else {
                        final List<Vertex> pickedV = getPickedVertices();
                        handlePopupVertex(v, pickedV, popP);
                        if (pickedV.size() < 2) {
                            oneVertexPressed(v); /* select this vertex */
                        }
                    }
                }
            });
            thread.start();
        }
    }

    /** This class is used to change view, if graph was clicked. */
    class MyGraphMouseListener<V> implements GraphMouseListener<V> {
        @Override
        public void graphClicked(final V v, final MouseEvent me) {
            /* do nothing */
        }

        /** Graph was released. */
        @Override
        public void graphReleased(final V v, final MouseEvent me) {
            for (final Vertex vertex : getPickedVertices()) {
                // TODO: if vertex is removed a race condition can be here
                if (vertex == null) {
                    continue;
                }
                final double x = layout.getX(vertex);
                final double y = layout.getY(vertex);
                final Point2D p = new Point2D.Double(x, y);
                vertexReleased(vertex, p);
            }
            scale();
        }

        @Override
        public void graphPressed(final V v, final MouseEvent me) {
            final Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    final PickedState<Vertex> psVertex =
                        visualizationViewer.getRenderContext().getPickedVertexState();
                    if ((me.getModifiers() & MouseEvent.CTRL_MASK) == 0) {
                        final List<Vertex> picked = getPickedVertices();
                        if (picked.size() == 1 || !picked.contains(v)) {
                            oneVertexPressed((Vertex) v);
                        }
                    } else {
                        /* ctrl-click */
                        psVertex.pick((Vertex) v, true);
                    }
                }
            });
            t.start();
        }
    }

    /**
     * This class provides methods for different paint colors for different
     * conditions.
     */
    class MyPickableVertexPaintFunction<V> extends PickableVertexPaintTransformer<V> {
        /** Whether it is the draw paint. */
        private final boolean draw;

        /** Creates new {@code MyPickableVertexPaintFunction} object. */
        MyPickableVertexPaintFunction(
            final PickedInfo<V> pi, final boolean draw) {
            super(pi, null, null);
            this.draw = draw;
        }

        /** Returns paint color for border of vertex v. */
        @Override
        public Paint transform(final V v) {
            if (draw && isPicked(v)) {
                return getVertexDrawPaint((Vertex) v);
            } else {
                final Paint drawPaintNotPicked =
                    getVertexDrawPaintNotPicked((Vertex) v);
                if (drawPaintNotPicked == null) {
                    return getFillPaint((Vertex) v);
                } else {
                    return drawPaintNotPicked;
                }
            }
        }

        /** Returns fill paint color for of vertex v. */
        public Paint getFillPaint(final Vertex v)  {
            Point2D p = layout.transform(v);
            p = visualizationViewer.getRenderContext().getMultiLayerTransformer().transform(Layer.LAYOUT, p);
            final float x = (float) p.getX();
            final float y = (float) p.getY();
            final Color col = getVertexFillColor(v);
            final Color secCol = getVertexFillSecondaryColor(v);
            if (col == null || secCol == null) {
                return null;
            }
            return new GradientPaint(x,
                                     y - getVertexHeight(v) / 2,
                                     secCol,
                                     x,
                                     y + getVertexHeight(v) / 2,
                                     col,
                                     false);
        }

        /** Returns whether the vertex is picked. */
        final boolean isPicked(final V v) {
            return visualizationViewer.getPickedVertexState().isPicked((Vertex) v);
        }
    }

    /** This class defines paints for the edges. */
    class MyPickableEdgePaintFunction<E> extends PickableEdgePaintTransformer<E> {

        MyPickableEdgePaintFunction(final PickedInfo<E> ps, final Paint drawPaint, final Paint pickedPaint) {
            super(ps, drawPaint, pickedPaint);
        }

        Paint getDrawPaint(final Edge e) {
            if (isPicked(e)) {
                return getEdgePickedPaint(e);
            } else {
                return getEdgeDrawPaint(e);
            }
        }

        /** Returns paint color for border of edge e. */
        @Override
        public Paint transform(final E e)  {
            return getDrawPaint((Edge) e);
        }
    }

    /** This class defines paints for the arrows. */
    class MyPickableArrowEdgePaintFunction<E> extends PickableEdgePaintTransformer<E> {
        /** Creates new {@code MyPickableArrowEdgePaintFunction} object.*/
        MyPickableArrowEdgePaintFunction(final PickedInfo<E> ps, final Paint drawPaint, final Paint pickedPaint) {
            super(ps, drawPaint, pickedPaint);
        }

        public Paint getDrawPaint(final Edge e) {
            if (isPicked(e)) {
                return getEdgePickedPaint(e);
            } else {
                return getEdgeDrawPaint(e);
            }
        }

        /** Returns paint color for border of edge e. */
        @Override
        public Paint transform(final E e)  {
            if (showHollowArrow((Edge) e)) {
                return Color.WHITE;
            }
            return getDrawPaint((Edge) e);
        }
    }

    /** This class defines what arrow and if at all should be painted. */
    class MyEdgeArrowFunction<V, E> extends DirectionalEdgeArrowTransformer<V, E> {
        /** Creates new MyEdgeArrowFunction object. */
        MyEdgeArrowFunction() {
            super(20, 8, 4);
        }

        /**
         * Returns the shape of the arrow, or not if no arrow should be
         * painted.
         */
        @Override
        public Shape transform(final Context<Graph<V, E>, E> context) {
            if (showEdgeArrow((Edge) context.element)) {
                return super.transform(context);
            } else {
                return EMPTY_SHAPE;
            }
        }
    }

    /** This class is for rendering of the vertices. */
    class MyPluggableRenderer<V, E> extends BasicVertexRenderer<V, E> {
        /**
         * Paints the shape for vertex and all icons and texts inside. It
         * resizes and repositions the vertex if neccessary.
         */
        @Override
        protected final void paintShapeForVertex(final RenderContext<V, E> rc, final V v, final Shape shape) {
            final Graphics2D g2d = rc.getGraphicsContext().getDelegate();
            int shapeWidth = getDefaultVertexWidth((Vertex) v);
            int shapeHeight = getDefaultVertexHeight((Vertex) v);

            /* icons */
            final List<ImageIcon> icons = getIconsForVertex((Vertex) v, getRunMode());
            /* main text */
            final String mainText = getMainText((Vertex) v, getRunMode());
            TextLayout mainTextLayout = null;
            if (mainText != null && !mainText.isEmpty()) {
                mainTextLayout = getVertexTextLayout(g2d, mainText, 1);
                int iconWidth = 64;
                if (icons == null) {
                    iconWidth = 4;
                }
                final int mainTextWidth = (int) mainTextLayout.getBounds().getWidth() + iconWidth;
                if (mainTextWidth > shapeWidth) {
                    shapeWidth = mainTextWidth;
                }
            }

            /* icon text */
            final String iconText = getIconText((Vertex) v, getRunMode());
            int iconTextWidth = 0;
            TextLayout iconTextLayout = null;
            if (iconText != null && !iconText.isEmpty()) {
                iconTextLayout = getVertexTextLayout(g2d, iconText, 0.8);
                iconTextWidth = (int) iconTextLayout.getBounds().getWidth();
            }

            /* right corner text */
            final ColorText rightCornerText = getRightCornerText((Vertex) v, getRunMode());
            TextLayout rightCornerTextLayout = null;
            if (rightCornerText != null && !"".equals(rightCornerText.getSubtext())) {
                rightCornerTextLayout = getVertexTextLayout(g2d, rightCornerText.getSubtext(), 0.8);
                final int rightCornerTextWidth = (int) rightCornerTextLayout.getBounds().getWidth();

                if (iconTextWidth + rightCornerTextWidth + 10 > shapeWidth) {
                    shapeWidth = iconTextWidth + rightCornerTextWidth + 10;
                }
            }

            /* subtext */
            final ColorText[] colorTexts = getSubtexts((Vertex) v, getRunMode());
            TextLayout[] subtextLayouts = null;
            if (colorTexts != null) {
                subtextLayouts = new TextLayout[colorTexts.length];
                int i = 0;
                for (final ColorText colorText : colorTexts) {
                    subtextLayouts[i] = getVertexTextLayout(g2d, colorText.getSubtext(), 0.8);
                    final int subtextWidth = (int) subtextLayouts[i].getBounds().getWidth();
                    if (subtextWidth + 10 > shapeWidth) {
                        shapeWidth = subtextWidth + 10;
                    }
                    i++;
                }
                if (i > 1) {
                    shapeHeight += (i - 1) << 3;
                }
                shapeHeight += 3;
            }
            final int oldShapeWidth = getVertexWidth((Vertex) v);
            final int oldShapeHeight = getVertexHeight((Vertex) v);
            if (isRunModeTestAnimation()) {
                if (oldShapeWidth > shapeWidth) {
                    shapeWidth = oldShapeWidth;
                }
                if (oldShapeHeight > shapeHeight) {
                    shapeHeight = oldShapeHeight;
                }
            }
            final boolean widthChanged = Math.abs(oldShapeWidth - shapeWidth) > 5;
            final boolean heightChanged = Math.abs(oldShapeHeight - shapeHeight) > 1;
            if (widthChanged || heightChanged) {
                somethingChanged();
                /* move it, so that left side has the same position, if it is
                * resized */
                final Point2D pos = layout.transform((Vertex) v);
                if (pos != null) {
                    double x = pos.getX();
                    double y = pos.getY();
                    if (widthChanged) {
                        setVertexWidth((Vertex) v, shapeWidth);
                        x -= (oldShapeWidth - getVertexWidth((Vertex) v)) / 2;
                    }
                    if (heightChanged) {
                        setVertexHeight((Vertex) v, shapeHeight);
                        y -= (oldShapeHeight - getVertexHeight((Vertex) v)) / 2;
                    }
                    pos.setLocation(x, y);
                    application.invokeLater(!Application.CHECK_SWING_THREAD,
                                      new Runnable() {
                                          @Override
                                          public void run() {
                                              scale();
                                          }
                                      });
                }
            }

            /* shape */
            super.paintShapeForVertex(rc, v, shape);
            Point2D loc = layout.transform((Vertex) v);
            loc = rc.getMultiLayerTransformer().transform(Layer.LAYOUT, loc);
            final double x = loc.getX() - getVertexWidth((Vertex) v) / 2;
            final double height = getDefaultVertexHeight((Vertex) v);
            final double y = loc.getY() - getVertexHeight((Vertex) v) / 2;
            drawInside((Vertex) v, g2d, x, y, shape);

            /* icon */
            if (icons != null) {
                for (final ImageIcon icon : icons) {
                    icon.setDescription("");
                    g2d.drawImage(icon.getImage(),
                                  (int) (x + 4),
                                  (int) (y + height / 2 - icon.getIconHeight() / 2),
                                  null);
                }
            }

            /* texts are drawn from left down corner. */
            if (mainTextLayout != null) {
                final int textW = (int) mainTextLayout.getBounds().getWidth();
                final int textH = (int) mainTextLayout.getBounds().getHeight();
                drawVertexText(g2d,
                               mainTextLayout,
                               x + shapeWidth / 2 - textW / 2, /* middle */
                               y + height / 2 + textH / 2,
                               new Color(0, 0, 0),
                               255);
            }
            if (iconTextLayout != null) {
                drawVertexText(g2d, iconTextLayout, x + 4, y + 11, new Color(0, 0, 0), 255);
            }
            if (rightCornerTextLayout != null) {
                drawVertexText(g2d,
                               rightCornerTextLayout,
                               x + shapeWidth - rightCornerTextLayout.getBounds().getWidth() - 4,
                               y + 11,
                               rightCornerText.getTextColor(),
                               255);
            }
            if (subtextLayouts != null) {
                int i = 0;
                for (final TextLayout l : subtextLayouts) {
                    int alpha = 255;
                    final ColorText colorText = colorTexts[i];
                    if (" ".equals(colorText.getSubtext().substring(0, 1))) {
                        alpha = 128;
                    }
                    final Color color = colorText.getColor();
                    if (color != null) {
                        final Paint p =
                            new GradientPaint((float) x + shapeWidth / 2,
                                              (float) y,
                                              getVertexFillSecondaryColor((Vertex) v),
                                              (float) x + shapeWidth / 2,
                                              (float) y + shapeHeight,
                                              color,
                                              false);
                        g2d.setPaint(p);
                        g2d.fillRect((int) x + 4, (int) (y + height - 3 + 8 * (i - 1)), shapeWidth - 8, 9);
                    }
                    final Color textColor = colorText.getTextColor();
                    drawVertexText(g2d, l, x + 4, y + height - 4 + 8 * i, textColor, alpha);
                    i++;
                }
            }

            final Info info = getInfo((Vertex) v);
            mAnimationListLock.lock();
            if (animationList.contains(info)) {
                /* update animation */
                final double i = info.getAnimationIndex();
                mAnimationListLock.unlock();
                final int barPos = (int) (i * (shapeWidth) / 100);
                g2d.setColor(new Color(250, 133, 34, 50));
                if (barPos > shapeWidth / 2) {
                    g2d.fillRect((int) (x + (barPos / 2)), (int) y, shapeWidth - barPos, shapeHeight);
                } else {
                    g2d.fillRect((int) (x + shapeWidth / 2 - barPos / 2), (int) y, barPos, shapeHeight);
                }
            } else {
                mAnimationListLock.unlock();
            }
        }
    }

    /**
     * An edge shape that renders as a straight line between
     * the vertex endpoints.
     */
    private class MyLine<V, E> extends AbstractEdgeShapeTransformer<V, E> {
        /**
         * Get the shape for this edge, returning either the
         * shared instance or, in the case of self-loop edges, the
         * SimpleLoop shared instance.
         */
        @Override
        public Shape transform(final Context<Graph<V, E>, E> context) {
            final Graph<V, E> g = context.graph;
            final E e = context.element;
            if (!(e instanceof Edge)) {
                return null;
            }

            final Pair<V> endpoints = g.getEndpoints(e);
            if (endpoints != null) {
                final boolean isLoop = endpoints.getFirst().equals(endpoints.getSecond());
                if (isLoop) {
                    LOG.appWarning("transform: an illegal loop: "
                                   + vertexToInfoMap.get(endpoints.getFirst())
                                   + ' ' + e + ' '
                                   + vertexToInfoMap.get(endpoints.getSecond()));
                    return EMPTY_SHAPE;
                }
            }
            if (showHollowArrow((Edge) e)) {
                return HOLLOW_INSTANCE;
            } else {
                return INSTANCE;
            }
        }
    }

    protected ClusterBrowser getClusterBrowser() {
        return clusterBrowser;
    }
}
