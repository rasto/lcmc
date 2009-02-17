/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.visualization;

import java.awt.geom.Point2D;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.utils.UserData;

/**
 * Implements the Fruchterman-Reingold algorithm for node layout.
 * 
 * @author Scott White, Yan-Biao Boey, Danyel Fisher
 */
public class FRLayout extends AbstractLayout implements LayoutMutable {

    private static final Object FR_KEY = "edu.uci.ics.jung.FR_Visualization_Key";

    private double forceConstant;

    private double temperature;

    private int currentIteration;

    private String status = null;

    private int mMaxIterations = 700;

    public FRLayout(Graph g) {
        super(g);
    }

    private double attraction_multiplier = 0.75;
    
    private double attraction_constant;
    
    private double repulsion_multiplier = 0.75;
    
    private double repulsion_constant;
    
    public void setAttractionMultiplier(double attraction)
    {
        this.attraction_multiplier = attraction;
    }
    
    public void setRepulsionMultiplier(double repulsion)
    {
        this.repulsion_multiplier = repulsion;
    }
    
    /*
     * new function for handling updates and changes to the graph
     */
    public synchronized void update() {
        try {
            for (Iterator iter = getGraph().getVertices().iterator(); iter
            .hasNext();) {
                Vertex v = (Vertex) iter.next();
                Coordinates coord = getCoordinates(v);
//                Coordinates coord = (Coordinates) v.getUserDatum(getBaseKey());
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

    /**
     * Returns the current temperature and number of iterations elapsed, as a
     * string.
     */
    public String getStatus() {
        return status;
    }

    public void forceMove(Vertex picked,double x, double y) {
        super.forceMove(picked, x, y);
    }

    protected void initialize_local() {
        currentIteration = 0;
        temperature = getCurrentSize().getWidth() / 10;
        
        forceConstant = 
            Math
            .sqrt(getCurrentSize().getHeight()
                    * getCurrentSize().getWidth()
                    / getVisibleGraph().numVertices());
//                    / Math.max(getVisibleGraph().numEdges(), getVisibleGraph().numVertices()));
        
        attraction_constant = attraction_multiplier * forceConstant;
        repulsion_constant = repulsion_multiplier * forceConstant;
        
//        forceConstant = 0.75 * Math
//                .sqrt(getCurrentSize().getHeight()
//                        * getCurrentSize().getWidth()
//                        / getVisibleGraph().numVertices());
    }

    private Object key = null;

    private double EPSILON = 0.000001D;

    /**
     * Returns a visualization-specific key (that is, specific both to this
     * instance and <tt>AbstractLayout</tt>) that can be used to access
     * UserData related to the <tt>AbstractLayout</tt>.
     */
    public Object getKey() {
        if (key == null) key = new Pair(this, FR_KEY);
        return key;
    }

    protected void initialize_local_vertex(Vertex v) {
        if (v.getUserDatum(getKey()) == null) {
            v.addUserDatum(getKey(), new FRVertexData(), UserData.REMOVE);
        }
    }

    /**
     * Moves the iteration forward one notch, calculation attraction and
     * repulsion between vertices and edges and cooling the temperature.
     */
    public synchronized void advancePositions() {
        currentIteration++;
        status = "VV: " + getVisibleVertices().size() + " IT: "
                + currentIteration + " temp: " + temperature;
        /**
         * Calculate repulsion
         */
        while(true) {
            
            try {
                for (Iterator iter = getVisibleVertices().iterator(); iter.hasNext();) {
                    Vertex v1 = (Vertex) iter.next();
                    if (isLocked(v1)) continue;
                    calcRepulsion(v1);
                }
                break;
            } catch(ConcurrentModificationException cme) {}
        }

        /**
         * Calculate attraction
         */
        while(true) {
            try {
                for (Iterator iter = getVisibleEdges().iterator(); iter.hasNext();) {
                    Edge e = (Edge) iter.next();
                    
                    calcAttraction(e);
                }
                break;
            } catch(ConcurrentModificationException cme) {}
        }

//        double cumulativeChange = 0;

        while(true) {
            try {    
                
                for (Iterator iter = getVisibleVertices().iterator(); iter.hasNext();) {
                    Vertex v = (Vertex) iter.next();
                    if (isLocked(v)) continue;
                    calcPositions(v);
                }
                break;
            } catch(ConcurrentModificationException cme) {}
        }
        cool();
    }

    public synchronized void calcPositions(Vertex v) {
        FRVertexData fvd = getFRData(v);
        if(fvd == null) return;
        Coordinates xyd = getCoordinates(v);
        double deltaLength = Math.max(EPSILON, Math.sqrt(fvd.disp
                .zDotProduct(fvd.disp)));

        double newXDisp = fvd.getXDisp() / deltaLength
                * Math.min(deltaLength, temperature);

        if (Double.isNaN(newXDisp)) { throw new FatalException(
                "Unexpected mathematical result in FRLayout:calcPositions [xdisp]"); }

        double newYDisp = fvd.getYDisp() / deltaLength
                * Math.min(deltaLength, temperature);
        xyd.addX(newXDisp);
        xyd.addY(newYDisp);

        double borderWidth = getCurrentSize().getWidth() / 50.0;
        double newXPos = xyd.getX();
        if (newXPos < borderWidth) {
            newXPos = borderWidth + Math.random() * borderWidth * 2.0;
        } else if (newXPos > (getCurrentSize().getWidth() - borderWidth)) {
            newXPos = getCurrentSize().getWidth() - borderWidth - Math.random()
                    * borderWidth * 2.0;
        }
        //double newXPos = Math.min(getCurrentSize().getWidth() - 20.0,
        // Math.max(20.0, xyd.getX()));

        double newYPos = xyd.getY();
        if (newYPos < borderWidth) {
            newYPos = borderWidth + Math.random() * borderWidth * 2.0;
        } else if (newYPos > (getCurrentSize().getHeight() - borderWidth)) {
            newYPos = getCurrentSize().getHeight() - borderWidth
                    - Math.random() * borderWidth * 2.0;
        }
        //double newYPos = Math.min(getCurrentSize().getHeight() - 20.0,
        // Math.max(20.0, xyd.getY()));

        xyd.setX(newXPos);
        xyd.setY(newYPos);
    }

    public void calcAttraction(Edge e) {
        Pair endpoints = e.getEndpoints();
        Vertex v1 = (Vertex)endpoints.getFirst();
        Vertex v2 = (Vertex)endpoints.getSecond();
        boolean v1_locked = isLocked(v1);
        boolean v2_locked = isLocked(v2);
        
        // if both are locked, nothing to do; return
        if (v1_locked && v2_locked)
            return;

        Point2D p1 = getLocation(v1);
        Point2D p2 = getLocation(v2);
        if(p1 == null || p2 == null) return;
        double xDelta = p1.getX() - p2.getX();
        double yDelta = p1.getY() - p2.getY();

        double deltaLength = Math.max(EPSILON, Math.sqrt((xDelta * xDelta)
                + (yDelta * yDelta)));

//        double force = (deltaLength * deltaLength) / forceConstant;
        
        double force = (deltaLength * deltaLength) / attraction_constant;

        if (Double.isNaN(force)) { throw new FatalException(
                "Unexpected mathematical result in FRLayout:calcPositions [force]"); }

        double dx = (xDelta / deltaLength) * force;
        double dy = (yDelta / deltaLength) * force; 
        
        if (!v1_locked)
        {
            FRVertexData fvd1 = getFRData(v1);
            fvd1.decrementDisp(dx, dy);
        }
        if (!v2_locked)
        {
            FRVertexData fvd2 = getFRData(v2);
            fvd2.incrementDisp(dx, dy);
        }
    }

    public void calcRepulsion(Vertex v1) {
        FRVertexData fvd1 = getFRData(v1);
        if(fvd1 == null) return;
        fvd1.setDisp(0, 0);

        try {
            for (Iterator iter2 = getVisibleVertices().iterator(); iter2.hasNext();) {
                Vertex v2 = (Vertex) iter2.next();
                // if (isLocked(v2)) continue;
                if (v1 != v2) {
                    Point2D p1 = getLocation(v1);
                    Point2D p2 = getLocation(v2);
                    if(p1 == null || p2 == null) continue;
                    double xDelta = p1.getX() - p2.getX();
                    double yDelta = p1.getY() - p2.getY();
                    
                    double deltaLength = Math.max(EPSILON, Math
                            .sqrt((xDelta * xDelta) + (yDelta * yDelta)));
                    
//                    double force = (forceConstant * forceConstant) / deltaLength;
                    double force = (repulsion_constant * repulsion_constant) / deltaLength;
                    
                    if (Double.isNaN(force)) { throw new FatalException(
                    "Unexpected mathematical result in FRLayout:calcPositions [repulsion]"); }
                    
                    fvd1.incrementDisp((xDelta / deltaLength) * force,
                            (yDelta / deltaLength) * force);
                }
            }
        } catch(ConcurrentModificationException cme) {
            calcRepulsion(v1);
        }
    }

    private void cool() {
        temperature *= (1.0 - currentIteration / (double) mMaxIterations);
    }

    public void setMaxIterations(int maxIterations) {
        mMaxIterations = maxIterations;
    }

    public FRVertexData getFRData(Vertex v) {
        return (FRVertexData) (v.getUserDatum(getKey()));
    }

    /**
     * This one is an incremental visualization.
     */
    public boolean isIncremental() {
        return true;
    }

    /**
     * Returns true once the current iteration has passed the maximum count,
     * <tt>MAX_ITERATIONS</tt>.
     */
    public boolean incrementsAreDone() {
        if (currentIteration > mMaxIterations) { 
//            System.out.println("Reached currentIteration =" + currentIteration + ", maxIterations=" + mMaxIterations);
            return true; 
        } 
        return false;
    }

    public static class FRVertexData {

        private DoubleMatrix1D disp;

        public FRVertexData() {
            initialize();
        }

        public void initialize() {
            disp = new DenseDoubleMatrix1D(2);
        }

        public double getXDisp() {
            return disp.get(0);
        }

        public double getYDisp() {
            return disp.get(1);
        }

        public void setDisp(double x, double y) {
            disp.set(0, x);
            disp.set(1, y);
        }

        public void incrementDisp(double x, double y) {
            disp.set(0, disp.get(0) + x);
            disp.set(1, disp.get(1) + y);
        }

        public void decrementDisp(double x, double y) {
            disp.set(0, disp.get(0) - x);
            disp.set(1, disp.get(1) - y);
        }
    }
}