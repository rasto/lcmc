/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.visualization;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Point2D;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.utils.UserData;

/**
 * The SpringLayout package represents a visualization of a set of nodes. The
 * SpringLayout, which is initialized with a Graph, assigns X/Y locations to
 * each node. When called <code>relax()</code>, the SpringLayout moves the
 * visualization forward one step.
 * 
 * @author Danyel Fisher
 * @author Joshua O'Madadhain
 */
public class SpringLayout extends AbstractLayout implements LayoutMutable {

    private static final Object SPRING_KEY = "temp_edu.uci.ics.jung.Spring_Visualization_Key";
    protected double stretch = 0.70;
    protected LengthFunction lengthFunction;
    protected int repulsion_range = 100;
    protected double force_multiplier = 1.0 / 3.0;

    /**
     * Returns the status.
     */
    public String getStatus() {
        return null;
    }

    /**
     * Constructor for a SpringLayout for a raw graph with associated
     * dimension--the input knows how big the graph is. Defaults to the unit
     * length function.
     */
    public SpringLayout(Graph g) {
        this(g, UNITLENGTHFUNCTION);
    }

    /**
     * Constructor for a SpringLayout for a raw graph with associated component.
     * 
     * @param g
     *            the input Graph
     * @param f
     *            the length function
     */
    public SpringLayout(Graph g, LengthFunction f) {
        super(g);
        this.lengthFunction = f;
    }

    /**
     * @return the current value for the stretch parameter
     * @see #setStretch(double)
     */
    public double getStretch()
    {
        return stretch;
    }
    
    /**
     * <p>Sets the stretch parameter for this instance.  This value 
     * specifies how much the degrees of an edge's incident vertices
     * should influence how easily the endpoints of that edge
     * can move (that is, that edge's tendency to change its length).</p>
     * 
     * <p>The default value is 0.70.  Positive values less than 1 cause
     * high-degree vertices to move less than low-degree vertices, and 
     * values > 1 cause high-degree vertices to move more than 
     * low-degree vertices.  Negative values will have unpredictable
     * and inconsistent results.</p>
     * @param stretch
     */
    public void setStretch(double stretch)
    {
        this.stretch = stretch;
    }
    
    /**
     * @return the current value for the node repulsion range
     * @see #setRepulsionRange(int)
     */
    public int getRepulsionRange()
    {
        return repulsion_range;
    }

    /**
     * Sets the node repulsion range (in drawing area units) for this instance.  
     * Outside this range, nodes do not repel each other.  The default value 
     * is 100.  Negative values are treated as their positive equivalents.
     * @param range
     */
    public void setRepulsionRange(int range)
    {
        this.repulsion_range = range;
    }
    
    /**
     * @return the current value for the edge length force multiplier
     * @see #setForceMultiplier(double)
     */
    public double getForceMultiplier()
    {
        return force_multiplier;
    }
    
    /**
     * Sets the force multiplier for this instance.  This value is used to 
     * specify how strongly an edge "wants" to be its default length
     * (higher values indicate a greater attraction for the default length),
     * which affects how much its endpoints move at each timestep.
     * The default value is 1/3.  A value of 0 turns off any attempt by the
     * layout to cause edges to conform to the default length.  Negative
     * values cause long edges to get longer and short edges to get shorter; use
     * at your own risk.
     */
    public void setForceMultiplier(double force)
    {
        this.force_multiplier = force;
    }
    
    protected void initialize_local() {
        try {
        for (Iterator iter = getGraph().getEdges().iterator(); iter.hasNext();) {
            Edge e = (Edge) iter.next();
            SpringEdgeData sed = getSpringData(e);
            if (sed == null) {
                sed = new SpringEdgeData(e);
                e.addUserDatum(getSpringKey(), sed, UserData.REMOVE);
            }
            calcEdgeLength(sed, lengthFunction);
        }
        } catch(ConcurrentModificationException cme) {
            initialize_local();
        }
    }

    Object key = null;

    public Object getSpringKey() {
        if (key == null) key = new Pair(this, SPRING_KEY);
        return key;
    }

    /**
     * (non-Javadoc)
     * 
     * @see edu.uci.ics.jung.visualization.AbstractLayout#initialize_local_vertex(edu.uci.ics.jung.graph.Vertex)
     */
    protected void initialize_local_vertex(Vertex v) {
        SpringVertexData vud = getSpringData(v);
        if (vud == null) {
            vud = new SpringVertexData();
            v.addUserDatum(getSpringKey(), vud, UserData.REMOVE);
        }
    }

    /* ------------------------- */

    protected void calcEdgeLength(SpringEdgeData sed, LengthFunction f) {
        sed.length = f.getLength(sed.e);
    }

    /* ------------------------- */


    /**
     * Relaxation step. Moves all nodes a smidge.
     */
    public void advancePositions() {
        try {
        for (Iterator iter = getVisibleVertices().iterator(); iter.hasNext();) {
            Vertex v = (Vertex) iter.next();
            SpringVertexData svd = getSpringData(v);
            if (svd == null) {
                continue;
            }
            svd.dx /= 4;
            svd.dy /= 4;
            svd.edgedx = svd.edgedy = 0;
            svd.repulsiondx = svd.repulsiondy = 0;
        }
        } catch(ConcurrentModificationException cme) {
            advancePositions();
        }

        relaxEdges();
        calculateRepulsion();
        moveNodes();
    }

    protected Vertex getAVertex(Edge e) {
        Vertex v = (Vertex) e.getIncidentVertices().iterator().next();
        return v;
    }

    protected void relaxEdges() {
        try {
        for (Iterator i = getVisibleEdges().iterator(); i.hasNext();) {
            Edge e = (Edge) i.next();

            Vertex v1 = getAVertex(e);
            Vertex v2 = e.getOpposite(v1);

            Point2D p1 = getLocation(v1);
            Point2D p2 = getLocation(v2);
            if(p1 == null || p2 == null) continue;
            double vx = p1.getX() - p2.getX();
            double vy = p1.getY() - p2.getY();
            double len = Math.sqrt(vx * vx + vy * vy);
            
            SpringEdgeData sed = getSpringData(e);
            if (sed == null) {
                continue;
            }
            double desiredLen = sed.length;

            // round from zero, if needed [zero would be Bad.].
            len = (len == 0) ? .0001 : len;

            double f = force_multiplier * (desiredLen - len) / len;

            f = f * Math.pow(stretch, (v1.degree() + v2.degree() - 2));

            // the actual movement distance 'dx' is the force multiplied by the
            // distance to go.
            double dx = f * vx;
            double dy = f * vy;
            SpringVertexData v1D, v2D;
            v1D = getSpringData(v1);
            v2D = getSpringData(v2);

            sed.f = f;

            v1D.edgedx += dx;
            v1D.edgedy += dy;
            v2D.edgedx += -dx;
            v2D.edgedy += -dy;
        }
        } catch(ConcurrentModificationException cme) {
            relaxEdges();
        }
    }

    protected void calculateRepulsion() {
        try {
        for (Iterator iter = getGraph().getVertices().iterator(); iter
                .hasNext();) {
            Vertex v = (Vertex) iter.next();
            if (isLocked(v)) continue;

            SpringVertexData svd = getSpringData(v);
            if(svd == null) continue;
            double dx = 0, dy = 0;

            for (Iterator iter2 = getGraph().getVertices().iterator(); iter2
                    .hasNext();) {
                Vertex v2 = (Vertex) iter2.next();
                if (v == v2) continue;
                Point2D p = getLocation(v);
                Point2D p2 = getLocation(v2);
                if(p == null || p2 == null) continue;
                double vx = p.getX() - p2.getX();
                double vy = p.getY() - p2.getY();
                double distance = vx * vx + vy * vy;
                if (distance == 0) {
                    dx += Math.random();
                    dy += Math.random();
                } else if (distance < repulsion_range * repulsion_range) {
                    double factor = 1;
                    dx += factor * vx / Math.pow(distance, 2);
                    dy += factor * vy / Math.pow(distance, 2);
                }
            }
            double dlen = dx * dx + dy * dy;
            if (dlen > 0) {
                dlen = Math.sqrt(dlen) / 2;
                svd.repulsiondx += dx / dlen;
                svd.repulsiondy += dy / dlen;
            }
        }
        } catch(ConcurrentModificationException cme) {
            calculateRepulsion();
        }
    }

    protected void moveNodes() {

        synchronized (getCurrentSize()) {
            try {
                for (Iterator i = getVisibleVertices().iterator(); i.hasNext();) {
                    Vertex v = (Vertex) i.next();
                    if (isLocked(v)) continue;
                    SpringVertexData vd = getSpringData(v);
                    if(vd == null) continue;
                    Coordinates xyd = getCoordinates(v);
                    
                    vd.dx += vd.repulsiondx + vd.edgedx;
                    vd.dy += vd.repulsiondy + vd.edgedy;
                    
                    // keeps nodes from moving any faster than 5 per time unit
                    xyd.addX(Math.max(-5, Math.min(5, vd.dx)));
                    xyd.addY(Math.max(-5, Math.min(5, vd.dy)));
                    
                    int width = getCurrentSize().width;
                    int height = getCurrentSize().height;
                    
                    if (xyd.getX() < 0) {
                        xyd.setX(0);
                    } else if (xyd.getX() > width) {
                        xyd.setX(width);
                    }
                    if (xyd.getY() < 0) {
                        xyd.setY(0);
                    } else if (xyd.getY() > height) {
                        xyd.setY(height);
                    }
                    
                }
            } catch(ConcurrentModificationException cme) {
                moveNodes();
            }
        }
    }

    public SpringVertexData getSpringData(Vertex v) {
        return (SpringVertexData) (v.getUserDatum(getSpringKey()));
    }

    public SpringEdgeData getSpringData(Edge e) {
        try {
            return (SpringEdgeData) (e.getUserDatum(getSpringKey()));
        } catch (ClassCastException cce) {
            System.out.println(e.getUserDatum(getSpringKey()).getClass());
            throw cce;
        }
    }

    public double getLength(Edge e) {
        return ((SpringEdgeData) e.getUserDatum(getSpringKey())).length;
    }

    /* ---------------Length Function------------------ */

    /**
     * If the edge is weighted, then override this method to show what the
     * visualized length is.
     * 
     * @author Danyel Fisher
     */
    public static interface LengthFunction {

        public double getLength(Edge e);
    }

    /**
     * Returns all edges as the same length: the input value
     * @author danyelf
     */
    public static final class UnitLengthFunction implements LengthFunction {

        int length;

        public UnitLengthFunction(int length) {
            this.length = length;
        }

        public double getLength(Edge e) {
            return length;
        }
    }

    public static final LengthFunction UNITLENGTHFUNCTION = new UnitLengthFunction(
            30);

    /* ---------------User Data------------------ */

    protected static class SpringVertexData {

        public double edgedx;

        public double edgedy;

        public double repulsiondx;

        public double repulsiondy;

        public SpringVertexData() {
        }

        /** movement speed, x */
        public double dx;

        /** movement speed, y */
        public double dy;
    }

    protected static class SpringEdgeData {

        public double f;

        public SpringEdgeData(Edge e) {
            this.e = e;
        }

        Edge e;

        double length;
    }

    /* ---------------Resize handler------------------ */

    public class SpringDimensionChecker extends ComponentAdapter {

        public void componentResized(ComponentEvent e) {
            resize(e.getComponent().getSize());
        }
    }

    /**
     * This one is an incremental visualization
     */
    public boolean isIncremental() {
        return true;
    }

    /**
     * For now, we pretend it never finishes.
     */
    public boolean incrementsAreDone() {
        return false;
    }

    /**
     * @see edu.uci.ics.jung.visualization.LayoutMutable#update()
     */
    public void update() {
        try {
        for (Iterator iter = getGraph().getVertices().iterator(); iter
                .hasNext();) {
            Vertex v = (Vertex) iter.next();
            Coordinates coord = (Coordinates) v.getUserDatum(getBaseKey());
            if (coord == null) {
                coord = new Coordinates();
                v.addUserDatum(getBaseKey(), coord, UserData.REMOVE);
                initializeLocation(v, coord, getCurrentSize());
                initialize_local_vertex(v);
            }
        }
        } catch(ConcurrentModificationException cme) {
            update();
        }
        initialize_local();
    }

}