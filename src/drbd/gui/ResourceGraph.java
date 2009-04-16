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
import drbd.gui.Browser.Info;
import drbd.utilities.MyMenuItem;
import drbd.data.Host;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.decorators.ConstantDirectionalEdgeValue;
import edu.uci.ics.jung.graph.decorators.EdgeShape;
import edu.uci.ics.jung.graph.decorators.VertexStringer;
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
import edu.uci.ics.jung.visualization.transform.MutableTransformer;

import java.awt.Shape;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.awt.Dimension;

import javax.swing.JPanel;
import java.awt.event.MouseEvent;
import java.awt.Paint;
import java.awt.GradientPaint;
import javax.swing.JPopupMenu;
import javax.swing.ImageIcon;

import java.awt.geom.Area;
import java.awt.geom.AffineTransform;


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
    /** Vertex to resource info object map. */
    private final Map<Vertex, Info>vertexToInfoMap =
                                        new LinkedHashMap<Vertex, Info>();
    /** Resource info object to vertex map. */
    private final Map<Info, Vertex>infoToVertexMap =
                                        new LinkedHashMap<Info,Vertex>();
    /** Edge to popup menu map. */
    private final Map<Edge, JPopupMenu>edgeToPopupMap =
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
    /** Rotation angle of the animation in the graph. */
    private volatile int rotation = 0;

    /**
     * Prepares a new <code>ResourceGraph</code> object.
     */
    public ResourceGraph(final ClusterBrowser clusterBrowser) {
        this.clusterBrowser = clusterBrowser;
        initGraph();
        /* start animation thread */
        final Thread thread = new Thread(new Runnable() {
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    rotation += 20;
                    if (rotation >= 360) {
                        rotation = 0;
                    }
                    repaint();
                }
            }
        });
        thread.start();
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
    protected void initGraph(final Graph graph) {
        this.graph = graph;

        vertexLocations = new DefaultSettableVertexLocationFunction();
        layout = new StaticLayout(graph);
        final PluggableRenderer pr = new MyPluggableRenderer();
        pr.setVertexStringer(new MyVertexStringer());
        pr.setEdgeStringer(new MyEdgeStringer());
        pr.setVertexShapeFunction(new MyVertexShapeSize());
        pr.setVertexPaintFunction(new MyPickableVertexPaintFunction(
                    pr,
                    (Paint)Tools.getDefaultColor("ResourceGraph.DrawPaint"),
                    (Paint)Tools.getDefaultColor("ResourceGraph.FillPaint"),
                    (Paint)Tools.getDefaultColor("ResourceGraph.PickedPaint")
                    ));
        pr.setEdgePaintFunction(new MyPickableEdgePaintFunction(
                    pr,
                    (Paint)Tools.getDefaultColor(
                                                "ResourceGraph.EdgeDrawPaint"),
                    (Paint)Tools.getDefaultColor(
                                              "ResourceGraph.EdgePickedPaint")
                    ));
        pr.setVertexLabelCentering(true);
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
                super.scale(vv, amount, new Point2D.Double(0,0));
            }
        };

        /* picking and popups */
        /* overwriting loadPlugins method only to set scaler */
        final EditingModalGraphMouse graphMouse =
                                            new EditingModalGraphMouse() {
            protected void loadPlugins() {
                super.loadPlugins();
                ((ScalingGraphMousePlugin)scalingPlugin).setScaler(myScaler);
            }
        };
        graphMouse.add(new MyPopupGraphMousePlugin(vertexLocations));
        graphMouse.setVertexLocations(vertexLocations);
        visualizationViewer.setGraphMouse(graphMouse);
        visualizationViewer.addGraphMouseListener(new MyGraphMouseListener() );
        visualizationViewer.setPickSupport(new ShapePickSupport(50));
        graphMouse.setMode(ModalGraphMouse.Mode.PICKING);
        layout.initialize(new Dimension(3600, 3600), vertexLocations);
        scrollPane = new GraphZoomScrollPane(visualizationViewer);
    }

    /**
     * Repaints the graph.
     */
    public void repaint() {
        visualizationViewer.repaint();
    }

    /**
     * Returns the graph object.
     */
    protected Graph getGraph() {
        return graph;
    }

    /**
     * Returns the vertex locations function.
     */
    protected DefaultSettableVertexLocationFunction getVertexLocations() {
        return vertexLocations;
    }

    /**
     * Returns the layout object.
     */
    protected StaticLayout getLayout() {
        return layout;
    }

    /**
     * Returns the visualization viewer.
     */
    protected VisualizationViewer getVisualizationViewer() {
        return visualizationViewer;
    }

    /**
     * Returns the hash with vertex to menus map.
     */
    protected Map<Vertex,List<MyMenuItem>> getVertexToMenus() {
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
    protected void removeVertex(final Info i) {
        infoToVertexMap.remove(i);
    }

    /**
     * Inserts the hash that maps resource info to its vertex.
     */
    protected void putInfoToVertex(final Info i, final Vertex v) {
        infoToVertexMap.remove(i);
        infoToVertexMap.put(i, v);
    }

    /**
     * Returns all resources.
     */
    protected Set infoToVertexKeySet() {
        return infoToVertexMap.keySet();
    }

    /**
     * Returns the resource info object for specified vertex v.
     */
    protected Info getInfo(final Vertex v) {
        return vertexToInfoMap.get(v);
    }

    /**
     * Removes the specified vertex from the hash.
     */
    protected void removeInfo(final Vertex v) {
        vertexToInfoMap.remove(v);
    }

    /**
     * Puts the vertex to resource info object map to the hash.
     */
    protected void putVertexToInfo(final Vertex v, final Info i) {
        vertexToInfoMap.remove(v);
        vertexToInfoMap.put(v, i);
    }

    /**
     * Returns popup menu for the edge e.
     */
    protected JPopupMenu getPopup(final Edge e) {
        return edgeToPopupMap.get(e);
    }

    /**
     * Removes popup menu for the edge e.
     */
    protected void removePopup(final Edge e) {
        edgeToPopupMap.remove(e);
    }

    /**
     * Returns whether popup menu exists for this edge.
     */
    protected boolean popupExists(final Edge v) {
        return edgeToPopupMap.containsKey(v);
    }

    /**
     * Enters the map from edge to its popup menu in the hash.
     */
    protected void putEdgeToPopup(final Edge e, final JPopupMenu p) {
        edgeToPopupMap.remove(e);
        edgeToPopupMap.put(e, p);
    }

    /**
     * Returns the minimal size of the graph that has all vertices in it.
     */
    protected Point2D.Float getFilledGraphSize() {
        Float maxYPos = new Float(0);
        Float maxXPos = new Float(0);

        // TODO: have to lock vertex locations
        for (final Iterator it = vertexLocations.getVertexIterator(); it.hasNext();) {
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
        Point2D max = getFilledGraphSize();
        max = visualizationViewer.inverseLayoutTransform(max);
        final float maxXPos = (float)max.getX();
        final float maxYPos = (float)max.getY();
        if (maxXPos <= 0 || maxYPos <= 0) {
            return;
        }
        visualizationViewer.stop();
        final Float vvX = new Float(visualizationViewer.getSize(null).getWidth());
        final Float vvY = new Float(visualizationViewer.getSize(null).getHeight());
        final float factorX = vvX / maxXPos;
        final float factorY = vvY / maxYPos;
        final float factor = (factorX < factorY)?factorX:factorY;
        final MutableTransformer vt = visualizationViewer.getViewTransformer();
        if (vvX > 0 && vvY > 0) {
            final float scale = (float)vt.getScale();
            if (factor <= scale) {
                myScaler.scale(visualizationViewer, factor / scale, new Point2D.Double(0,0));
            }
        }
        visualizationViewer.restart();
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
            final Vertex f = mFrom;
            final Vertex t = mTo;
            setDirection(t, f);
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
    public JPanel getGraphPanel() {
        return scrollPane;
    }

    /**
     * Returns label for vertex v.
     */
    protected abstract String getLabelForVertexStringer(final ArchetypeVertex v);

    /**
     * This class takes care for the string in the node.
     */
    class MyVertexStringer implements VertexStringer {
        /**
         * Returns label of vertex v. Label consists of name of the service
         * with id in the parenthesis, if it is not an empty string.
         */
        public String getLabel(final ArchetypeVertex v) {
            return " " + getLabelForVertexStringer(v) + " ";
        }
    }

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

    /**
     * Returns size of the vertex v
     */
    protected abstract int getVertexSize(final Vertex v);

    /**
     * Returns aspect ratio of the vertex v
     */
    protected abstract float getVertexAspectRatio(final Vertex v);

    /**
     * Returns shape of the vertex v
     */
    protected Shape getVertexShape(final Vertex v, final VertexShapeFactory factory) {
        return factory.getEllipse(v);
    }

    /**
     * This class provides size and shape of the vertices.
     * To change the values, following methods can be overwritten:
     *
     * getVertexSize
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
            return getVertexSize(v);
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
    public void addPopupItem(final Vertex v, final MyMenuItem item) {
        vertexToMenus.get(v).add(item);
    }

    /**
     * Adds popup menu item for edge.
     */
    public void addPopupItem(final Edge e, final MyMenuItem item) {
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
        /* should be overridden to do something. */
        return null;
    }

    /**
     * Updates all popup menus.
     */
    public void updatePopupMenus() {
        for (final Iterator i = graph.getVertices().iterator(); i.hasNext();  ) {
            final Vertex v = (Vertex)i.next();
            vertexToMenus.remove(v);
        }
        for (final Iterator i = graph.getEdges().iterator(); i.hasNext();  ) {
            final Edge e = (Edge)i.next();
            edgeToMenus.remove(e);
            updatePopupEdge(e);
        }
    }

    /**
     * Updates edge popup
     */
    protected void updatePopupEdge(final Edge edge) {
        final List<MyMenuItem> menus = edgeToMenus.get(edge);
        if (menus != null) {
            for (final Iterator it = menus.iterator(); it.hasNext(); ) {
                ((MyMenuItem)it.next()).update();
            }
        }
    }

    /**
     * This class handles popup menus in the graph.
     */
    class MyPopupGraphMousePlugin extends EditingPopupGraphMousePlugin {

        private static final long serialVersionUID = 1L;

        /**
         * Creates new <code>MyPopupGraphMousePlugin</code> object.
         */
        MyPopupGraphMousePlugin(final DefaultSettableVertexLocationFunction vertexLocations) {
            super(vertexLocations);
        }

        /**
         * Is called when mouse was clicked.
         */
        public void mouseClicked(final MouseEvent e) {
            super.mouseClicked(e);
            final PickedState ps = visualizationViewer.getPickedState();
            if (ps.getPickedEdges().size() == 1) {
                final Edge edge = (Edge)ps.getPickedEdges().toArray()[0];
                oneEdgePressed(edge);
            } else if (ps.getPickedVertices().size() == 0) {
                backgroundClicked();
            }
        }

        /**
         * Creates and displays popup menus for vertices and edges.
         */
        protected void handlePopup(final MouseEvent me) {
            // TODO: it comes here twice
            final VisualizationViewer vv = (VisualizationViewer)me.getSource();
            final Point2D p = vv.inverseViewTransform(me.getPoint());
            final PickSupport pickSupport = vv.getPickSupport();
            final Vertex v = pickSupport.getVertex(p.getX(), p.getY());
            final Point2D popP = me.getPoint();

            final int posX = (int)popP.getX();
            final int posY = (int)popP.getY();
            if(v == null) {
                final Edge edge = pickSupport.getEdge(p.getX(), p.getY());
                if (edge == null) {
                    /* background was clicked */
                    final JPopupMenu backgroundPopup = handlePopupBackground(p);
                    if (backgroundPopup != null) {
                        backgroundPopup.show(vv, posX, posY);
                    }
                    backgroundClicked();
                } else {
                    final JPopupMenu edgePopup = handlePopupEdge(edge);
                    if (edgePopup != null) {
                        edgePopup.show(vv, posX, posY);
                    }
                    oneEdgePressed(edge);
                }
            } else {
                final JPopupMenu vertexPopup = handlePopupVertex(v, p);
                if (vertexPopup != null) {
                    vertexPopup.show(vv, posX, posY);
                }
                oneVertexPressed(v); // select this vertex
            }
        }
    }

    /**
     * Removes info from the graph.
     */
    protected void removeInfo(final Info i) {
        graph.removeVertex(getVertex(i));
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
    public void pickEdge(final Edge e) {
        final PickedState ps = visualizationViewer.getPickedState();
        ps.clearPickedEdges();
        ps.clearPickedVertices();
        ps.pick(e, true);
    }

    /**
     * Picks vertex v.
     */
    public void pickVertex(final Vertex v) {
        final PickedState ps = visualizationViewer.getPickedState();
        ps.clearPickedEdges();
        ps.clearPickedVertices();
        ps.pick(v, true);
    }

    /**
     * Picks background.
     */
    public void pickBackground() {
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
            Vertex vertex;
            for (final Iterator it = ps.getPickedVertices().iterator(); it.hasNext(); ) {
                vertex = (Vertex)it.next();
                final Point2D p = layout.getLocation(vertex);
                //Point2D p = vertexLocations.getLocation(vertex);
                vertexReleased(vertex, p);
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

    /**
     * Retuns border paint color for vertex v.
     */
    protected Paint getVertexDrawPaint(final Vertex v) {
        return Tools.getDefaultColor("ResourceGraph.DrawPaint");
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
    protected Color getVertexFillColor(final Info i) {
        return getVertexFillColor(infoToVertexMap.get(i));
    }

    /**
     * Returns secondary gradient fill paint color for vertex v.
     */
    protected Color getVertexFillSecondaryColor(final Vertex v) {
        return Color.WHITE;
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
                                      final Paint fill_paint,
                                      final Paint pickedPaint) {
            super(pi, drawPaint, fill_paint, pickedPaint);
        }

        /**
         * Returns paint color for border of vertex v.
         */
        public Paint getDrawPaint(final Vertex v)  {
            if (pi.isPicked(v)) {
                return getVertexDrawPaint(v);
            } else {
                return getFillPaint(v);
            }
        }

        /**
         * Returns fill paint color for of vertex v.
         */
        public Paint getFillPaint(final Vertex v)  {
            final Point2D p = visualizationViewer.layoutTransform(layout.getLocation(v));
            final float x = (float) p.getX();
            final float y = (float) p.getY();

            return new GradientPaint(x,
                                     y - getVertexSize(v) * getVertexAspectRatio(v) / 2,
                                     getVertexFillSecondaryColor(v),
                                     x,
                                     y + getVertexSize(v) * getVertexAspectRatio(v) / 2,
                                     getVertexFillColor(v),
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

    class MyPickableEdgePaintFunction extends PickableEdgePaintFunction {

        /**
         * Creates new <code>MyPickableEdgePaintFunction</code> object.
         */
        MyPickableEdgePaintFunction(final PickedInfo pi,
                                    final Paint drawPaint,
                                    final Paint pickedPaint) {
            super(pi, drawPaint, pickedPaint);
        }

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

    class MyEdgeArrowFunction extends DirectionalEdgeArrowFunction {
        public MyEdgeArrowFunction() {
            super(20, 8, 4);
        }

        public Shape getArrow(final Edge e) {
            if (showEdgeArrow(e)) {
                return super.getArrow(e);
            } else {
                return emptyShape;
            }
        }
    }

    protected abstract boolean showEdgeArrow(final Edge e);

    protected abstract ImageIcon getIconForVertex(final ArchetypeVertex v);

    class MyPluggableRenderer extends PluggableRenderer {

        public void paintShapeForVertex(final Graphics2D g2d,
                                        final Vertex v,
                                        final Shape shape) {
            super.paintShapeForVertex(g2d, v, shape);
            final Point2D loc = visualizationViewer.layoutTransform(layout.getLocation(v));
            final ImageIcon icon = getIconForVertex(v);
            if (icon != null) {
                icon.setDescription("asdf");
                final double x = loc.getX() - getVertexSize(v) / 2;
                final double y = loc.getY() - icon.getIconHeight() / 2;
                if (getInfo(v).isUpdated()) {
                    /* update animation */
                    AffineTransform origXform = g2d.getTransform();
                    AffineTransform newXform = (AffineTransform)(origXform.clone());
                    int xRot = icon.getIconWidth() / 2;
                    int yRot = icon.getIconHeight() / 2;
                    newXform.setToTranslation(x, y);
                    newXform.rotate(Math.toRadians(rotation),
                                          xRot,
                                          yRot);
                    g2d.drawImage(icon.getImage(), newXform, null);
                } else {
                    g2d.drawImage(icon.getImage(), (int)x, (int)y, null);
                }
            }
        }
    }

    public void getPositions(final Map<String,Point2D> positions) {
        for (final Iterator i = graph.getVertices().iterator(); i.hasNext();  ) {
            final Vertex v = (Vertex) i.next();
            final Info info = getInfo(v);
            final Point2D p = layout.getLocation(v);
            if (info != null) {
                final String id = getId(info);
                if (id != null) {
                    positions.remove(id);
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
        for (int i = 0; i < hosts.length; i++) {
            p = hosts[i].getGraphPosition(getId(info));
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
        for (int i = 0; i < hosts.length; i++) {
            hosts[i].resetGraphPosition(getId(info));
        }
    }
}
