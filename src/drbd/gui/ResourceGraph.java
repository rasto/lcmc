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
import drbd.gui.resources.Info;
import drbd.utilities.MyMenuItem;
import drbd.data.Host;
import drbd.data.Subtext;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.decorators.ConstantDirectionalEdgeValue;
import edu.uci.ics.jung.graph.decorators.EdgeShape;
import edu.uci.ics.jung.graph.decorators.EdgeStringer;
import edu.uci.ics.jung.graph.decorators.DefaultToolTipFunction;
import edu.uci.ics.jung.graph.decorators.AbstractVertexShapeFunction;
import edu.uci.ics.jung.graph.decorators.VertexAspectRatioFunction;
import edu.uci.ics.jung.graph.decorators.PickableEdgePaintFunction;
import edu.uci.ics.jung.graph.decorators.DirectionalEdgeArrowFunction;
import edu.uci.ics.jung.graph.decorators.VertexSizeFunction;
import edu.uci.ics.jung.graph.decorators.PickableVertexPaintFunction;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;

import edu.uci.ics.jung.visualization.PickedInfo;
import edu.uci.ics.jung.visualization.PickedState;
import edu.uci.ics.jung.visualization.PickSupport;
import edu.uci.ics.jung.visualization.GraphMouseListener;

import edu.uci.ics.jung.visualization.PluggableRenderer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.ShapePickSupport;
import edu.uci.ics.jung.visualization.DefaultSettableVertexLocationFunction;
import edu.uci.ics.jung.visualization.StaticLayout;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VertexShapeFactory;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.EditingModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ViewScalingControl;
import edu.uci.ics.jung.visualization.control.EditingPopupGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.ScalingGraphMousePlugin;

import java.awt.Shape;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.GradientPaint;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.awt.Dimension;

import javax.swing.JPanel;
import java.awt.event.MouseEvent;
import java.awt.Paint;
import javax.swing.JPopupMenu;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.JMenuItem;

import java.awt.geom.Area;

import java.awt.Font;
import java.awt.font.TextLayout;
import java.awt.font.FontRenderContext;
import EDU.oswego.cs.dl.util.concurrent.Mutex;

/**
 * This class creates graph and provides methods for scaling etc.,
 * that are used in all graphs.
 *
 * @author Rasto Levrinc
 * @version $Id$/
 *
 */
public abstract class ResourceGraph {
    /** Cluster browser object. */
    private final ClusterBrowser clusterBrowser;
    /** Pluggable renderer. */
    private final PluggableRenderer pr = new MyPluggableRenderer();
    /** Vertex to resource info object map. */
    private final Map<Vertex, Info> vertexToInfoMap =
                                        new LinkedHashMap<Vertex, Info>();
    /** Resource info object to vertex map. */
    private final Map<Info, Vertex> infoToVertexMap =
                                        new LinkedHashMap<Info, Vertex>();
    /** Edge to popup menu map. */
    private final Map<Edge, JPopupMenu> edgeToPopupMap =
                                         new LinkedHashMap<Edge, JPopupMenu>();
    /** Vertex to menus map. */
    private final Map<Vertex, List<MyMenuItem>> vertexToMenus =
                                new LinkedHashMap<Vertex, List<MyMenuItem>>();
    /** Edge to menus map. */
    private final Map<Edge, List<MyMenuItem>> edgeToMenus =
                                new LinkedHashMap<Edge, List<MyMenuItem>>();
    /**
     * Empty shape for arrows. (to not show an arrow).
     */
    private final Area emptyShape = new Area();
    /** The graph object. */
    private Graph graph;
    /** Visualization viewer object. */
    private VisualizationViewer visualizationViewer;
    /** The layout object. */
    private StaticLayout layout;
    /** The graph's scroll pane. */
    private GraphZoomScrollPane scrollPane;
    /** The vertex locations object. */
    private DefaultSettableVertexLocationFunction vertexLocations;
    /** The scaler. */
    private ViewScalingControl myScaler;
    /** List with resources that should be animated. */
    private final List<Info> animationList = new ArrayList<Info>();
    /** This mutex is for protecting the animation list. */
    private final Mutex mAnimationListLock = new Mutex();
    /** List with resources that should be animated for test view. */
    private final List<JComponent> testAnimationList =
                                                   new ArrayList<JComponent>();
    /** This mutex is for protecting the test animation list. */
    private final Mutex mTestAnimationListLock = new Mutex();
    /** Animation thread. */
    private volatile Thread animationThread = null;
    /** This mutex is for protecting the animation thread. */
    private final Mutex mAnimationThreadLock = new Mutex();
    /** Map from vertex to its width. */
    private final Map<Vertex, Integer> vertexWidth =
                                               new HashMap<Vertex, Integer>();
    /** Map from vertex to its height. */
    private final Map<Vertex, Integer> vertexHeight =
                                               new HashMap<Vertex, Integer>();
    /** Whether something in the graph changed that requires vv to restart. */
    private volatile boolean changed = false;

    /** Whether only test or real thing should show. */
    private volatile boolean testOnlyFlag = false;
    /** This mutex is for protecting the testOnlyFlag. */
    private final Mutex mTestOnlyFlag = new Mutex();
    /** Test animation thread. */
    private volatile Thread testAnimationThread = null;
    /** This mutex is for protecting the test animation thread. */
    private final Mutex mTestAnimationThreadLock = new Mutex();
    /** List of edges that are made only during test. */
    private volatile Edge testEdge = null;
    /** List of edges that are being tested during test. */
    private volatile Edge existingTestEdge = null;
    /** Lock for test edge list. */
    private final Mutex mTestEdgeLock = new Mutex();
    /** Interval beetween two animation frames. */
    private final int animInterval =
                             (int) (1000 / Tools.getConfigData().getAnimFPS());

    /**
     * Prepares a new <code>ResourceGraph</code> object.
     */
    public ResourceGraph(final ClusterBrowser clusterBrowser) {
        this.clusterBrowser = clusterBrowser;
        initGraph();
    }

    /**
     * Starts the animation if vertex is being updated.
     */
    public final void startAnimation(final Info info) {
        try {
            mAnimationListLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        if (animationList.isEmpty()) {
            /* start animation thread */
            try {
                mAnimationThreadLock.acquire();
            } catch (java.lang.InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            if (animationThread == null) {
                animationThread = new Thread(new Runnable() {
                    public void run() {
                        while (true) {
                            try {
                                Thread.sleep(animInterval);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }

                            try {
                                mAnimationListLock.acquire();
                            } catch (java.lang.InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                            if (animationList.isEmpty()) {
                                mAnimationListLock.release();
                                repaint();
                                try {
                                    mAnimationThreadLock.acquire();
                                } catch (java.lang.InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                }
                                animationThread = null;
                                mAnimationThreadLock.release();
                                break;
                            }
                            for (final Info info : animationList) {
                                info.incAnimationIndex();
                            }
                            mAnimationListLock.release();
                            repaint();
                        }
                    }
                });
                animationThread.start();
            }
            mAnimationThreadLock.release();
        }
        animationList.add(info);
        mAnimationListLock.release();
    }

    /**
     * Stops the animation.
     */
    public final void stopAnimation(final Info info) {
        try {
            mAnimationListLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        animationList.remove(info);
        mAnimationListLock.release();
    }

    /**
     * Starts the animation if vertex is being tested.
     */
    public final void startTestAnimation(final JComponent component,
                                         final CountDownLatch startTestLatch) {
        //Tools.setMenuOpaque(component, false);
        try {
            mTestAnimationListLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        try {
            mTestOnlyFlag.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        testOnlyFlag = false;
        mTestOnlyFlag.release();
        if (testAnimationList.isEmpty()) {
            try {
                mTestAnimationThreadLock.acquire();
            } catch (java.lang.InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            if (testAnimationThread == null) {
                /* start test animation thread */
                testAnimationThread = new Thread(new Runnable() {
                    public void run() {
                        FOREVER: while (true) {
                            try {
                                startTestLatch.await();
                            } catch (InterruptedException ignored) {
                                Thread.currentThread().interrupt();
                            }
                            try {
                                mTestOnlyFlag.acquire();
                            } catch (java.lang.InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                            testOnlyFlag = !testOnlyFlag;
                            final boolean testOnlyFlagLast = testOnlyFlag;
                            mTestOnlyFlag.release();
                                    Tools.setMenuOpaque(component,
                                                        !testOnlyFlag);
                            repaint();
                            int sleep = 300;
                            if (testOnlyFlag) {
                                sleep = 1200;
                            }
                            for (int s = 0; s < sleep; s += animInterval) {
                                try {
                                    mTestOnlyFlag.acquire();
                                } catch (java.lang.InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                }
                                if (testOnlyFlag == testOnlyFlagLast) {
                                    mTestOnlyFlag.release();
                                } else {
                                    mTestOnlyFlag.release();
                                    repaint();
                                }
                                if (!component.isShowing()) {
                                    stopTestAnimation(component);
                                }
                                try {
                                    mTestAnimationListLock.acquire();
                                } catch (java.lang.InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                }

                                if (testAnimationList.isEmpty()) {
                                    Tools.setMenuOpaque(component, true);
                                    mTestAnimationListLock.release();
                                    try {
                                        mTestOnlyFlag.acquire();
                                    } catch (java.lang.InterruptedException i) {
                                        Thread.currentThread().interrupt();
                                    }
                                    testOnlyFlag = false;
                                    mTestOnlyFlag.release();
                                    repaint();
                                    try {
                                        mTestAnimationThreadLock.acquire();
                                    } catch (java.lang.InterruptedException i) {
                                        Thread.currentThread().interrupt();
                                    }
                                    testAnimationThread = null;
                                    mTestAnimationThreadLock.release();
                                    break FOREVER;
                                }
                                mTestAnimationListLock.release();
                                Tools.sleep(animInterval);
                            }
                        }
                    }
                });
                testAnimationThread.start();
            }
            mTestAnimationThreadLock.release();
        }
        testAnimationList.add(component);
        mTestAnimationListLock.release();
    }

    /** Stops the test animation. */
    public final void stopTestAnimation(final JComponent component) {
        try {
            mTestAnimationListLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        testAnimationList.remove(component);
        mTestAnimationListLock.release();
        removeExistingTestEdge();
        removeTestEdge();
        Tools.setMenuOpaque(component, true);
    }

    /** Is test animation running. */
    public final boolean isTestAnimation() {
        boolean running;
        try {
            mTestAnimationListLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        running = !testAnimationList.isEmpty();
        mTestAnimationListLock.release();
        return running;
    }


    /**
     * Returns the cluster browser object.
     */
    protected final ClusterBrowser getClusterBrowser() {
        return clusterBrowser;
    }

    /**
     * Initializes the graph.
     */
    protected abstract void initGraph();

    /**
     * Inits the graph.
     */
    protected final void initGraph(final Graph graph) {
        this.graph = graph;

        vertexLocations = new DefaultSettableVertexLocationFunction();
        layout = new StaticLayout(graph);
        pr.setEdgeStringer(new MyEdgeStringer());
        pr.setVertexShapeFunction(new MyVertexShapeSize());
        pr.setVertexPaintFunction(new MyPickableVertexPaintFunction(
                    pr,
                    (Paint) Tools.getDefaultColor("ResourceGraph.DrawPaint"),
                    (Paint) Tools.getDefaultColor("ResourceGraph.FillPaint"),
                    (Paint) Tools.getDefaultColor("ResourceGraph.PickedPaint")
                    ));
        pr.setEdgePaintFunction(new MyPickableEdgePaintFunction(
                    pr,
                    (Paint) Tools.getDefaultColor(
                                                "ResourceGraph.EdgeDrawPaint"),
                    (Paint) Tools.getDefaultColor(
                                              "ResourceGraph.EdgePickedPaint")
                    ));
        pr.setEdgeArrowFunction(new MyEdgeArrowFunction());
        pr.setEdgeLabelClosenessFunction(
                                new ConstantDirectionalEdgeValue(0.5, 0.5));
        visualizationViewer = new VisualizationViewer(layout, pr);
        pr.setEdgeShapeFunction(new EdgeShape.Line());
        visualizationViewer.setBackground(
                        Tools.getDefaultColor("ResourceGraph.Background"));
        visualizationViewer.setToolTipFunction(new MyToolTipFunction());

        /* scaling */

        /* overwriting scaler so that zooming starts from point (0, 0) */
        myScaler = new ViewScalingControl() {
            public void scale(final VisualizationViewer vv,
                              final float amount,
                              final Point2D from) {
                super.scale(vv, amount, new Point2D.Double(0, 0));
            }
        };

        /* picking and popups */
        /* overwriting loadPlugins method only to set scaler */
        final EditingModalGraphMouse graphMouse =
                                            new EditingModalGraphMouse() {
            protected void loadPlugins() {
                super.loadPlugins();
                ((ScalingGraphMousePlugin) scalingPlugin).setScaler(myScaler);
            }
        };
        graphMouse.add(new MyPopupGraphMousePlugin(vertexLocations));
        graphMouse.setVertexLocations(vertexLocations);
        visualizationViewer.setGraphMouse(graphMouse);
        visualizationViewer.addGraphMouseListener(new MyGraphMouseListener());
        visualizationViewer.setPickSupport(new ShapePickSupport(50));
        graphMouse.setMode(ModalGraphMouse.Mode.PICKING);
        layout.initialize(new Dimension(3600, 3600), vertexLocations);
        scrollPane = new GraphZoomScrollPane(visualizationViewer);
    }

    /**
     * Repaints the graph.
     */
    public final void repaint() {
        visualizationViewer.repaint();
    }

    /**
     * Returns the graph object.
     */
    protected final Graph getGraph() {
        return graph;
    }

    /**
     * Returns the vertex locations function.
     */
    protected final DefaultSettableVertexLocationFunction getVertexLocations() {
        return vertexLocations;
    }

    /**
     * Returns the layout object.
     */
    protected final StaticLayout getLayout() {
        return layout;
    }

    /**
     * Returns the visualization viewer.
     */
    protected final VisualizationViewer getVisualizationViewer() {
        return visualizationViewer;
    }

    /**
     * Returns the hash with vertex to menus map.
     */
    protected final Map<Vertex, List<MyMenuItem>> getVertexToMenus() {
        return vertexToMenus;
    }

    /**
     * Returns the vertex that represents the specified resource.
     */
    protected Vertex getVertex(final Info i) {
        return infoToVertexMap.get(i);
    }

    /**
     * Removes the vertex that represents the specified resource.
     */
    protected final void removeVertex(final Info i) {
        infoToVertexMap.remove(i);
    }

    /**
     * Inserts the hash that maps resource info to its vertex.
     */
    protected final void putInfoToVertex(final Info i, final Vertex v) {
        infoToVertexMap.remove(i);
        infoToVertexMap.put(i, v);
    }

    /**
     * Returns all resources.
     */
    protected final Set<Info> infoToVertexKeySet() {
        return infoToVertexMap.keySet();
    }

    /**
     * Returns the resource info object for specified vertex v.
     */
    protected final Info getInfo(final Vertex v) {
        return vertexToInfoMap.get(v);
    }

    /**
     * Removes the specified vertex from the hash.
     */
    protected final void removeInfo(final Vertex v) {
        vertexToInfoMap.remove(v);
    }

    /**
     * Puts the vertex to resource info object map to the hash.
     */
    protected final void putVertexToInfo(final Vertex v, final Info i) {
        vertexToInfoMap.remove(v);
        vertexToInfoMap.put(v, i);
    }

    /**
     * Returns popup menu for the edge e.
     */
    protected final JPopupMenu getPopup(final Edge e) {
        return edgeToPopupMap.get(e);
    }

    /**
     * Removes popup menu for the edge e.
     */
    protected final void removePopup(final Edge e) {
        edgeToPopupMap.remove(e);
    }

    /**
     * Returns whether popup menu exists for this edge.
     */
    protected final boolean popupExists(final Edge v) {
        return edgeToPopupMap.containsKey(v);
    }

    /**
     * Enters the map from edge to its popup menu in the hash.
     */
    protected final void putEdgeToPopup(final Edge e, final JPopupMenu p) {
        edgeToPopupMap.remove(e);
        edgeToPopupMap.put(e, p);
    }

    /**
     * Returns the minimal size of the graph that has all vertices in it.
     */
    protected final Point2D.Float getFilledGraphSize() {
        Float maxYPos = new Float(0);
        Float maxXPos = new Float(0);

        // TODO: have to lock vertex locations
        for (final Iterator it = vertexLocations.getVertexIterator();
             it.hasNext();) {
            final Vertex vn = (Vertex) it.next();
            final Point2D loc = vertexLocations.getLocation(vn);
            if (loc != null) {
                final Float pX = new Float(loc.getX());
                final Float pY = new Float(loc.getY());

                if (pX > maxXPos) {
                    maxXPos = pX;
                }
                if (pY > maxYPos) {
                    maxYPos = pY;
                }
            }
        }
        maxXPos += 80;
        maxYPos += 32;
        return new Point2D.Float(maxXPos, maxYPos);
    }

    /**
     * Scales the graph, so that all vertices can be seen. The graph can
     * get smaller but not bigger.
     */
    public void scale() { // TODO: synchronize differently
        //TODO: disabling it till it works properly
        //Point2D max = getFilledGraphSize();
        //max = visualizationViewer.inverseLayoutTransform(max);
        //final float maxXPos = (float)max.getX();
        //final float maxYPos = (float)max.getY();
        //if (maxXPos <= 0 || maxYPos <= 0) {
        //    return;
        //}
        //visualizationViewer.stop();
        //final Float vvX =
        //             new Float(visualizationViewer.getSize(null).getWidth());
        //final Float vvY =
        //            new Float(visualizationViewer.getSize(null).getHeight());
        //final float factorX = vvX / maxXPos;
        //final float factorY = vvY / maxYPos;
        //final float factor = (factorX < factorY)?factorX:factorY;
        //final MutableTransformer vt =
        //                            visualizationViewer.getViewTransformer();
        //if (vvX > 0 && vvY > 0) {
        //    final float scale = (float)vt.getScale();
        //    if (factor <= scale) {
        //        myScaler.scale(visualizationViewer,
        //                       factor / scale, new Point2D.Double(0,0));
        //    }
        //}
        //TODO: it may hang here, check it
        if (changed) {
            somethingChangedReset();
            //SwingUtilities.invokeLater(new Runnable() {
            //    public void run() {
                    visualizationViewer.restart();
            //    }
            //});
        }
        visualizationViewer.repaint();
    }

    /**
     * This class allows to change direction of the edge.
     */
    class MyEdge extends DirectedSparseEdge {
        /** Originaly from. */
        private final Vertex origFrom;
        /** Originaly to. */
        private final Vertex origTo;
        /**
         * Creates new <code>MyEdge</code> object.
         */
        public MyEdge(final Vertex from, final Vertex to) {
            super(from, to);
            origFrom = from;
            origTo   = to;
        }

        /**
         * Reverse direction of the edge.
         */
        public void reverse() {
            //final Vertex f = mFrom;
            //final Vertex t = mTo;
            //setDirection(t, f);
            setDirection(origTo, origFrom);
        }

        /**
         * Sets direction of the edge.
         */
        public void setDirection(final Vertex from, final Vertex to) {
            mFrom = from;
            mTo   = to;
        }

        /**
         * Sets direction to the original state.
         */
        public void reset() {
            mFrom = origFrom;
            mTo = origTo;
        }
    }

    /**
     * Returns graph in the scroll pane.
     */
    public final JPanel getGraphPanel() {
        return scrollPane;
    }

    /**
     * Returns label for vertex v.
     */
    protected abstract String getMainText(final ArchetypeVertex v,
                                          final boolean testOnly);

    /**
     * Returns label for edge e.
     */
    protected abstract String getLabelForEdgeStringer(ArchetypeEdge e);

    /**
     * This class takes care for the string in the edge.
     */
    class MyEdgeStringer implements EdgeStringer {
        /**
         * Returns label of edge e.
         */
        public String getLabel(final ArchetypeEdge e) {
            return " " + getLabelForEdgeStringer(e) + " ";
        }
    }

    /**
     * This class provides tool tips for the vertices and edges.
     */
    class MyToolTipFunction extends DefaultToolTipFunction {
        /**
         * Returns tool tip for vertex v.
         */
        public String getToolTipText(final Vertex v) {
            return Tools.html(getVertexToolTip(v));
        }

        /**
         * Returns tool tip for edge.
         */
        public String getToolTipText(final Edge edge) {
            return Tools.html(getEdgeToolTip(edge));
        }
    }

    /**
     * Returns tool tip for vertex v.
     */
    public abstract String getVertexToolTip(final Vertex v);

    /**
     * Returns tool tip for edge.
     */
    public abstract String getEdgeToolTip(final Edge edge);

    /** Returns the width of the service vertex shape. */
    protected int getVertexWidth(final Vertex v) {
        if (vertexWidth.containsKey(v)) {
            return vertexWidth.get(v);
        } else {
            return getDefaultVertexWidth(v);
        }
    }

    /** Returns the height of the service vertex shape. */
    protected int getVertexHeight(final Vertex v) {
        if (vertexHeight.containsKey(v)) {
            return vertexHeight.get(v);
        } else {
            return getDefaultVertexHeight(v);
        }
    }

    /** Returns the default vertex width. */
    protected int getDefaultVertexWidth(final Vertex v) {
        return 1;
    }

    /** Returns the default vertex height. */
    protected int getDefaultVertexHeight(final Vertex v) {
        return 1;
    }

    /** Sets the vertex width. */
    protected void setVertexWidth(final Vertex v, final int size) {
        vertexWidth.put(v, size);
    }

    /** Sets the vertex height. */
    protected void setVertexHeight(final Vertex v, final int size) {
        vertexHeight.put(v, size);
    }

    /** Returns aspect ratio of the vertex v. */
    protected float getVertexAspectRatio(final Vertex v) {
        return (float) getVertexHeight(v) / (float) getVertexWidth(v);
    }

    /** Returns shape of the vertex v. */
    protected Shape getVertexShape(final Vertex v,
                                   final VertexShapeFactory factory) {
        return factory.getEllipse(v);
    }

    /**
     * This class provides size and shape of the vertices.
     * To change the values, following methods can be overwritten:
     *
     * getVertexWidth
     * getVertexHeight
     * getVertexAspectRatio
     * getVertexShape
     */
    class MyVertexShapeSize extends AbstractVertexShapeFunction
        implements VertexSizeFunction, VertexAspectRatioFunction {

        /**
         * Creates new <code>MyVertexShapeSize</code> object.
         */
        public MyVertexShapeSize() {
            super();
            setSizeFunction(this);
            setAspectRatioFunction(this);
        }

        /**
         * Returns size for vertex v.
         */
        public int getSize(final Vertex v) {
            return getVertexWidth(v);
        }

        /**
         * Returns aspect ratio for vertex v.
         */
        public float getAspectRatio(final Vertex v) {
            return getVertexAspectRatio(v);
        }

        /**
         * Returns shape for vertex v.
         */
        public Shape getShape(final Vertex v) {
            return getVertexShape(v, factory);
        }
    }

    /**
     * Handles right click on the vertex.
     */
    protected abstract JPopupMenu handlePopupVertex(final Vertex v,
                                                    final Point2D p);

    /**
     * Adds popup menu item for vertex.
     */
    public final void addPopupItem(final Vertex v, final MyMenuItem item) {
        vertexToMenus.get(v).add(item);
    }

    /**
     * Adds popup menu item for edge.
     */
    public final void addPopupItem(final Edge e, final MyMenuItem item) {
        edgeToMenus.get(e).add(item);
    }

    /**
     * Handles right click on the edge.
     */
    protected abstract JPopupMenu handlePopupEdge(final Edge edge);

    /**
     * Handles right click on the background.
     */
    protected JPopupMenu handlePopupBackground(final Point2D pos) {
        return null;
    }

    /**
     * Updates all popup menus.
     */
    public final void updatePopupMenus() {
        for (final Object v : graph.getVertices()) {
            vertexToMenus.remove((Vertex) v);
        }
        for (final Object e : graph.getEdges()) {
            edgeToMenus.remove((Edge) e);
            updatePopupEdge((Edge) e);
        }
    }

    /**
     * Updates edge popup.
     */
    protected final void updatePopupEdge(final Edge edge) {
        final List<MyMenuItem> menus = edgeToMenus.get(edge);
        if (menus != null) {
            for (final MyMenuItem menu : menus) {
                menu.update();
            }
        }
    }

    /**
     * This class handles popup menus in the graph.
     */
    class MyPopupGraphMousePlugin extends EditingPopupGraphMousePlugin {
        /** Serial version ID. */
        private static final long serialVersionUID = 1L;

        /**
         * Creates new <code>MyPopupGraphMousePlugin</code> object.
         */
        MyPopupGraphMousePlugin(
                final DefaultSettableVertexLocationFunction vertexLocations) {
            super(vertexLocations);
        }

        /**
         * Is called when mouse was clicked.
         */
        public void mouseClicked(final MouseEvent e) {
            super.mouseClicked(e);
            final PickedState ps = visualizationViewer.getPickedState();
            if (ps.getPickedEdges().size() == 1) {
                final Edge edge = (Edge) ps.getPickedEdges().toArray()[0];
                oneEdgePressed(edge);
            } else if (ps.getPickedVertices().size() == 0) {
                backgroundClicked();
            }
        }

        /**
         * Creates and displays popup menus for vertices and edges.
         */
        protected void handlePopup(final MouseEvent me) {
            final Thread thread = new Thread(new Runnable() {
                public void run() {
                    // TODO: it comes here twice
                    final VisualizationViewer vv =
                                        (VisualizationViewer) me.getSource();
                    final Point2D p = vv.inverseViewTransform(me.getPoint());
                    final PickSupport pickSupport = vv.getPickSupport();
                    final Vertex v = pickSupport.getVertex(p.getX(), p.getY());
                    final Point2D popP = me.getPoint();

                    final int posX = (int) popP.getX();
                    final int posY = (int) popP.getY();

                    final JPopupMenu empty = new JPopupMenu();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            empty.add(new JMenuItem("wait for popup..."));
                            empty.show(vv, posX, posY);
                        }
                    });

                    if (v == null) {
                        final Edge edge = pickSupport.getEdge(p.getX(),
                                                              p.getY());
                        if (edge == null) {
                            /* background was clicked */
                            final JPopupMenu backgroundPopup =
                                                    handlePopupBackground(p);
                            if (backgroundPopup != null) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        empty.setVisible(false);
                                        backgroundPopup.show(vv, posX, posY);
                                    }
                                });
                            }
                            backgroundClicked();
                        } else {
                            final JPopupMenu edgePopup = handlePopupEdge(edge);
                            if (edgePopup != null) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        empty.setVisible(false);
                                        edgePopup.show(vv, posX, posY);
                                    }
                                });
                            } else {
                            }
                            oneEdgePressed(edge);
                        }
                    } else {
                        final JPopupMenu vertexPopup = handlePopupVertex(v, p);
                        if (vertexPopup != null) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    empty.setVisible(false);
                                    vertexPopup.show(vv, posX, posY);
                                }
                            });
                        }
                        oneVertexPressed(v); /* select this vertex */
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            empty.setVisible(false);
                        }
                    });
                }
            });
            thread.start();
        }
    }

    /**
     * Removes info from the graph.
     */
    protected void removeInfo(final Info i) {
        //SwingUtilities.invokeLater(new Runnable() {
        //    public void run() {
                graph.removeVertex(getVertex(i));
        //    }
        //});
        getVertexLocations().reset();
    }

    /**
     * Picks and highlights vertex with Info i in the graph.
     */
    public void pickInfo(final Info i) {
        final Vertex v = getVertex(i);
        final PickedState ps = visualizationViewer.getPickedState();
        ps.clearPickedEdges();
        ps.clearPickedVertices();
        ps.pick(v, true);
    }

    /**
     * Picks edge e.
     */
    public final void pickEdge(final Edge e) {
        final PickedState ps = visualizationViewer.getPickedState();
        ps.clearPickedEdges();
        ps.clearPickedVertices();
        ps.pick(e, true);
    }

    /**
     * Picks vertex v.
     */
    public final void pickVertex(final Vertex v) {
        final PickedState ps = visualizationViewer.getPickedState();
        ps.clearPickedEdges();
        ps.clearPickedVertices();
        ps.pick(v, true);
    }

    /**
     * Picks background.
     */
    public final void pickBackground() {
        final PickedState ps = visualizationViewer.getPickedState();
        ps.clearPickedEdges();
        ps.clearPickedVertices();
    }

    /**
     * Is called when vertex is released.
     */
    protected abstract void vertexReleased(final Vertex v, Point2D pos);

    /**
     * Is called when exactly one vertex is pressed.
     */
    protected abstract void oneVertexPressed(Vertex v);

    /**
     * Is called when one edge is pressed.
     */
    protected abstract void oneEdgePressed(final Edge e);

    /**
     * Is called when background was clicked.
     */
    protected abstract void backgroundClicked();

    /**
     * This class is used to change view, if graph was clicked.
     */
    class MyGraphMouseListener implements GraphMouseListener {
        /** Serial version UID. */
        private static final long serialVersionUID = 1L;

        /**
         * Graph was clicked.
         */
        public void graphClicked(final Vertex v, final MouseEvent me) {
            /* do nothing */
        }

        /**
         * Graph was released.
         */
        public void graphReleased(final Vertex v, final MouseEvent me) {
            final PickedState ps = visualizationViewer.getPickedState();
            for (final Object vertex : ps.getPickedVertices()) {
                // TODO: if vertex is removed a race condition can be here
                final Point2D p = layout.getLocation((Vertex) vertex);
                vertexReleased((Vertex) vertex, p);
            }
            //scale();
        }

        /**
         * Graph was pressed.
         */
        public void graphPressed(final Vertex v, final MouseEvent me) {
            final PickedState ps = visualizationViewer.getPickedState();
            if (ps.getPickedVertices().size() == 1) {
                oneVertexPressed(v);
                ps.clearPickedEdges();
            }
        }
    }

    /** Retuns border paint color for vertex v. */
    protected final Paint getVertexDrawPaint(final Vertex v) {
        return Tools.getDefaultColor("ResourceGraph.DrawPaint");
    }

    /** Retuns border paint color of not picked vertex v, null for no border. */
    protected Paint getVertexDrawPaintNotPicked(final Vertex v) {
        return null;
    }

    /**
     * Returns fill paint color for vertex v.
     */
    protected Color getVertexFillColor(final Vertex v) {
        return Tools.getDefaultColor("ResourceGraph.FillPaint");
    }

    /**
     * Returns fill paint color for the specified resource.
     */
    protected final Color getVertexFillColor(final Info i) {
        return getVertexFillColor(infoToVertexMap.get(i));
    }

    /**
     * Returns secondary gradient fill paint color for vertex v.
     */
    protected Color getVertexFillSecondaryColor(final Vertex v) {
        return Color.WHITE;
    }

    /**
     * Returns whether the vertex is picked.
     */
    public final boolean isPicked(final Vertex v) {
        return pr.isPicked(v);
    }

    /**
     * Returns whether the edge is picked.
     */
    public final boolean isPicked(final Edge e) {
        return pr.isPicked(e);
    }

    /**
     * This class provides methods for different paint colors for different
     * conditions.
     */
    class MyPickableVertexPaintFunction extends PickableVertexPaintFunction {

        /**
         * Creates new <code>MyPickableVertexPaintFunction</code> object.
         */
        MyPickableVertexPaintFunction(final PickedInfo pi,
                                      final Paint drawPaint,
                                      final Paint fillPaint,
                                      final Paint pickedPaint) {
            super(pi, drawPaint, fillPaint, pickedPaint);
        }

        /**
         * Returns paint color for border of vertex v.
         */
        public Paint getDrawPaint(final Vertex v)  {
            if (pi.isPicked(v)) {
                return getVertexDrawPaint(v);
            } else {
                final Paint drawPaintNotPicked = getVertexDrawPaintNotPicked(v);
                if (drawPaintNotPicked == null) {
                    return getFillPaint(v);
                } else {
                    return drawPaintNotPicked;
                }
            }
        }

        /**
         * Returns whether the vertex is picked.
         */
        public final boolean isPicked(final Vertex v) {
            return pi.isPicked(v);
        }

        /**
         * Returns fill paint color for of vertex v.
         */
        public Paint getFillPaint(final Vertex v)  {
            final Point2D p =
                    visualizationViewer.layoutTransform(layout.getLocation(v));
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
    }

    /**
     * Retuns border paint color for edge e.
     */
    protected Paint getEdgeDrawPaint(final Edge e) {
        return Tools.getDefaultColor("ResourceGraph.EdgeDrawPaint");
    }

    /**
     * Retuns fill paint color for edge e.
     */
    protected Paint getEdgePickedPaint(final Edge e) {
        return Tools.getDefaultColor("ResourceGraph.EdgePickedPaint");
    }

    /**
     * This class defines paints for the edges.
     */
    class MyPickableEdgePaintFunction extends PickableEdgePaintFunction {

        /**
         * Creates new <code>MyPickableEdgePaintFunction</code> object.
         */
        MyPickableEdgePaintFunction(final PickedInfo pi,
                                    final Paint drawPaint,
                                    final Paint pickedPaint) {
            super(pi, drawPaint, pickedPaint);
        }

        /**
         * Returns paint of the edge.
         */
        public Paint getDrawPaint(final Edge e) {
            if (pi.isPicked(e)) {
                return getEdgePickedPaint(e);
            } else {
                return getEdgeDrawPaint(e);
            }
        }

        /**
         * Returns paint color for border of edge e.
         */
        public Paint getFillPaint(final Edge e)  {
            return getDrawPaint(e);
        }
    }

    /**
     * This class defines what arrow and if at all should be painted.
     */
    class MyEdgeArrowFunction extends DirectionalEdgeArrowFunction {
        /**
         * Creates new MyEdgeArrowFunction object.
         */
        public MyEdgeArrowFunction() {
            super(20, 8, 4);
        }

        /**
         * Returns the shape of the arrow, or not if no arrow should be
         * painted.
         */
        public Shape getArrow(final Edge e) {
            if (showEdgeArrow(e)) {
                return super.getArrow(e);
            } else {
                return emptyShape;
            }
        }
    }

    /**
     * Returns whether an arrow on edge should be painted.
     */
    protected abstract boolean showEdgeArrow(final Edge e);

    /**
     * Returns an icon for vertex.
     */
    protected abstract List<ImageIcon> getIconsForVertex(
                                                      final ArchetypeVertex v,
                                                      final boolean testOnly);

    /**
     * This method draws in the host vertex.
     */
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
                final Paint p = new GradientPaint(
                                            (float) x + width / number,
                                            (float) y,
                                            getVertexFillSecondaryColor(v),
                                            (float) x + width / number,
                                            (float) y + height,
                                            colors[i],
                                            false);
                g2d.setPaint(p);
                final Rectangle2D s =
                       new Rectangle2D.Double(
                                    x + width / 2 + (width / number / 2) * i,
                                    y,
                                    width / number / 2,
                                    height - 2);
                g2d.fill(s);
            }
        }
    }
    /**
     * This method must be overridden to draw something in the vertex.
     */
    protected abstract void drawInside(final Vertex v,
                                       final Graphics2D g2d,
                                       final double x,
                                       final double y,
                                       final Shape shape);

    /**
     * This class is for rendering of the vertices.
     */
    class MyPluggableRenderer extends PluggableRenderer {
        /**
         * Paints the shape for vertex and all icons and texts inside. It
         * resizes and repositions the vertex if neccessary.
         */
        public final void paintShapeForVertex(final Graphics2D g2d,
                                              final Vertex v,
                                              final Shape shape) {
            int shapeWidth = getDefaultVertexWidth(v);
            int shapeHeight = getDefaultVertexHeight(v);
            /* icons */
            final List<ImageIcon> icons = getIconsForVertex(v, isTestOnly());
            /* main text */
            final String mainText = getMainText(v, isTestOnly());
            TextLayout mainTextLayout = null;
            if (mainText != null && !mainText.equals("")) {
                mainTextLayout = getVertexTextLayout(g2d, mainText, 1);
                int iconWidth = 64;
                if (icons == null) {
                    iconWidth = 4;
                }
                final int mainTextWidth =
                       (int) mainTextLayout.getBounds().getWidth() + iconWidth;
                if (mainTextWidth > shapeWidth) {
                    shapeWidth = mainTextWidth;
                }
            }

            /* icon text */
            final String iconText = getIconText(v, isTestOnly());
            int iconTextWidth = 0;
            TextLayout iconTextLayout = null;
            if (iconText != null && !iconText.equals("")) {
                iconTextLayout = getVertexTextLayout(g2d, iconText, 0.8);
                iconTextWidth =
                            (int) iconTextLayout.getBounds().getWidth();
            }

            /* right corner text */
            final Subtext rightCornerText = getRightCornerText(v, isTestOnly());
            TextLayout rightCornerTextLayout = null;
            if (rightCornerText != null && !rightCornerText.equals("")) {
                rightCornerTextLayout = getVertexTextLayout(
                               g2d,
                               rightCornerText.getSubtext(),
                               0.8);
                final int rightCornerTextWidth =
                        (int) rightCornerTextLayout.getBounds().getWidth();

                if (iconTextWidth + rightCornerTextWidth + 10 > shapeWidth) {
                    shapeWidth = iconTextWidth + rightCornerTextWidth + 10;
                }
            }

            /* subtext */
            final Subtext[] subtexts = getSubtexts(v, isTestOnly());
            TextLayout[] subtextLayouts = null;
            if (subtexts != null) {
                subtextLayouts = new TextLayout[subtexts.length];
                int i = 0;
                for (final Subtext subtext : subtexts) {
                    subtextLayouts[i] =
                           getVertexTextLayout(g2d,
                                               subtext.getSubtext(),
                                               0.8);
                    final int subtextWidth =
                                (int) subtextLayouts[i].getBounds().getWidth();
                    if (subtextWidth + 10 > shapeWidth) {
                        shapeWidth = subtextWidth + 10;
                    }
                    i++;
                }
                if (i > 1) {
                    shapeHeight += (i - 1) * 8;
                }
                shapeHeight += 3;
            }
            final int oldShapeWidth = getVertexWidth(v);
            final int oldShapeHeight = getVertexHeight(v);
            if (isTestOnlyAnimation()) {
                if (oldShapeWidth > shapeWidth) {
                    shapeWidth = oldShapeWidth;
                }
                if (oldShapeHeight > shapeHeight) {
                    shapeHeight = oldShapeHeight;
                }
            }
            final boolean widthChanged =
                                    Math.abs(oldShapeWidth - shapeWidth) > 5;
            final boolean heightChanged =
                                    Math.abs(oldShapeHeight - shapeHeight) > 1;
            if (widthChanged || heightChanged) {
                somethingChanged();
                /* move it, so that left side has the same position, if it is
                 * resized */
                final Point2D pos = getVertexLocations().getLocation(v);
                if (pos != null) {
                    double x = pos.getX();
                    double y = pos.getY();
                    if (widthChanged) {
                        setVertexWidth(v, shapeWidth);
                        x = x - (oldShapeWidth - getVertexWidth(v)) / 2;
                    }
                    if (heightChanged) {
                        setVertexHeight(v, shapeHeight);
                        y = y - (oldShapeHeight - getVertexHeight(v)) / 2;
                    }
                    pos.setLocation(x, y);
                    getVertexLocations().setLocation(v, pos);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            scale();
                        }
                    });
                }
            }
            /* shape */
            super.paintShapeForVertex(g2d, v, shape);
            final Point2D loc = visualizationViewer.layoutTransform(
                                                    layout.getLocation(v));
            final double x = loc.getX() - getVertexWidth(v) / 2;
            final double height = getDefaultVertexHeight(v);
            final double y = loc.getY() - getVertexHeight(v) / 2;
            drawInside(v, g2d, x, y, shape);

            /* icon */
            if (icons != null) {
                for (final ImageIcon icon : icons) {
                    icon.setDescription("sdf");
                    g2d.drawImage(
                          icon.getImage(),
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
                drawVertexText(g2d,
                               iconTextLayout,
                               x + 4,
                               y + 11,
                               new Color(0, 0, 0),
                               255);
            }
            if (rightCornerTextLayout != null) {
                drawVertexText(
                   g2d,
                   rightCornerTextLayout,
                   x + shapeWidth
                     - rightCornerTextLayout.getBounds().getWidth() - 4,
                   y + 11,
                   rightCornerText.getColor(),
                   255);
            }
            if (subtextLayouts != null) {
                int i = 0;
                for (final TextLayout l : subtextLayouts) {
                    int alpha = 255;
                    final Subtext subtext = subtexts[i];
                    if (subtext.getSubtext().substring(0, 1).equals(" ")) {
                        alpha = 128;
                    }
                    final Color color = subtext.getColor();
                    if (color != null) {
                        final Paint p =
                               new GradientPaint((float) x + shapeWidth / 2,
                                                 (float) y,
                                                 getVertexFillSecondaryColor(v),
                                                 (float) x + shapeWidth / 2,
                                                 (float) y + shapeHeight,
                                                 color,
                                                 false);
                        g2d.setPaint(p);
                        g2d.fillRect((int) x + 4,
                                     (int) (y + height - 3 + 8 * (i - 1)),
                                     shapeWidth - 8, 9);
                    }
                    drawVertexText(
                               g2d,
                               l,
                               x + 4,
                               y + height - 4 + 8 * i,
                               new Color(0, 0, 0),
                               alpha);
                    i++;
                }
            }

            final Info info = getInfo(v);
            try {
                mAnimationListLock.acquire();
            } catch (java.lang.InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            if (animationList.contains(info)) {
                /* update animation */
                final double i = info.getAnimationIndex();
                mAnimationListLock.release();
                final int barPos =
                           (int) (i * (shapeWidth) / 100);
                g2d.setColor(new Color(250, 133, 34,
                                       50));
                if (barPos > shapeWidth / 2) {
                    g2d.fillRect((int) (x + (barPos / 2)),
                                 (int) y,
                                 shapeWidth - barPos,
                                 shapeHeight);
                } else {
                    g2d.fillRect((int) (x + shapeWidth / 2 - barPos / 2),
                                 (int) y,
                                 barPos,
                                 shapeHeight);
                }
            } else {
                mAnimationListLock.release();
            }
        }
    }

    /**
     * Small text that appears above the icon.
     */
    protected abstract String getIconText(final Vertex v,
                                          final boolean testOnly);

    /**
     * Small text that appears in the right corner.
     */
    protected abstract Subtext getRightCornerText(final Vertex v,
                                                  final boolean testOnly);

    /**
     * Small text that appears down.
     */
    protected abstract Subtext[] getSubtexts(final Vertex v,
                                             final boolean testOnly);

    /**
     * Returns positions of the vertices (by value).
     */
    public final void getPositions(final Map<String, Point2D> positions) {
        for (final Object v : graph.getVertices()) {
            final Info info = getInfo((Vertex) v);
            final Point2D p = new Point2D.Double();
            final Point2D loc = layout.getLocation((Vertex) v);
            if (loc == null) {
                positions.clear();
                return;
            }
            p.setLocation(loc);
            p.setLocation(p.getX() + (getDefaultVertexWidth((Vertex) v)
                                      - getVertexWidth((Vertex) v)) / 2,
                          p.getY() + (getDefaultVertexHeight((Vertex) v)
                                      - getVertexHeight((Vertex) v)) / 2);
            if (info != null) {
                final String id = getId(info);
                if (id != null) {
                    positions.put(id, p);
                }
            }
        }
    }

    /**
     * Returns id that is used for saving of the vertex positions to a file.
     */
    protected abstract String getId(final Info i);

    /**
     * Returns saved position for the specified resource.
     */
    public final Point2D getSavedPosition(final Info info) {
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

    /**
     * Reset saved position for the specified resource.
     */
    public final void resetSavedPosition(final Info info) {
        final Host[] hosts = clusterBrowser.getClusterHosts();
        for (final Host host : hosts) {
            host.resetGraphPosition(getId(info));
        }
    }

    /**
     * Returns layout of the text that will be drawn on the vertex.
     */
    private TextLayout getVertexTextLayout(final Graphics2D g2d,
                                           final String text,
                                           final double fontSizeFactor) {
        final Font font = Tools.getGUIData().getMainFrame().getFont();
        final FontRenderContext context = g2d.getFontRenderContext();
        return new TextLayout(text,
                              new Font(font.getName(),
                                       font.getStyle(),
                                       (int) (font.getSize() * fontSizeFactor)),
                              context);
    }

    /**
     * Draws text on the vertex.
     */
    private void drawVertexText(final Graphics2D g2d,
                                final TextLayout textLayout,
                                final double x,
                                final double y,
                                final Color color,
                                final int alpha) {
        if (color != null) {
            g2d.setColor(new Color(color.getRed(),
                                   color.getGreen(),
                                   color.getBlue(),
                                   alpha));
        }
        textLayout.draw(g2d, (float) x, (float) y);
    }

    /**
     * Resets something changed flag.
     */
    private void somethingChangedReset() {
        changed = false;
    }
    /**
     * Is called when something in the graph changed.
     */
    protected final void somethingChanged() {
        changed = true;
    }

    /**
     * Returns if it is testOnly.
     */
    protected final boolean isTestOnly() {
        try {
            mTestOnlyFlag.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final boolean tof = testOnlyFlag;
        mTestOnlyFlag.release();
        return tof;
    }

    /**
     * Returns if it test animation is running.
     */
    protected final boolean isTestOnlyAnimation() {
        try {
            mTestAnimationListLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final boolean empty = testAnimationList.isEmpty();
        mTestAnimationListLock.release();
        return !empty;
    }

    /**
     * Removes test edges.
     */
    protected void removeTestEdge() {
        try {
            mTestEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        try {
            if (testEdge != null) {
                getGraph().removeEdge(testEdge);
                testEdge = null;
            }
        } catch (final Exception ignore) {
            /* ignore */
        }
        mTestEdgeLock.release();
    }

    /**
     * Creates a test edge.
     */
    protected final void addTestEdge(final Vertex vP, final Vertex v) {
        boolean gotlock = false;
        try {
            gotlock = mTestEdgeLock.attempt(0);
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        if (!gotlock) {
            return;
        }
        if (testEdge != null) {
            try {
                getGraph().removeEdge(testEdge);
            } catch (final Exception e) {
                /* ignore */
            }
        }
        if (!isTestAnimation()) {
            mTestEdgeLock.release();
            return;
        }
        try {
            final Edge edge = getGraph().addEdge(new MyEdge(vP, v));
            testEdge = edge;
        } catch (final Exception e) {
            /* ignore */
        }
        mTestEdgeLock.release();
    }

    /** Adds an existing edge to the test edges. */
    protected final void addExistingTestEdge(final Edge edge) {
        boolean gotlock = false;
        try {
            gotlock = mTestEdgeLock.attempt(0);
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        if (!gotlock) {
            return;
        }
        if (!isTestAnimation()) {
            existingTestEdge = null;
            mTestEdgeLock.release();
            return;
        }
        existingTestEdge = edge;
        mTestEdgeLock.release();
    }

    /** Removes existing test edges. */
    protected final void removeExistingTestEdge() {
        try {
            mTestEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        existingTestEdge = null;
        mTestEdgeLock.release();
    }

    /**
     * Returns whether the edge is a test edge.
     */
    protected final boolean isTestEdge(final Edge e) {
        try {
            mTestEdgeLock.acquire();
        } catch (java.lang.InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final boolean is = testEdge == e || existingTestEdge == e;
        mTestEdgeLock.release();
        return is;
    }
}
