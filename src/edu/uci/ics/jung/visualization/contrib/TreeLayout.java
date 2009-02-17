/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 *
 * Created on Jul 9, 2005
 */

package edu.uci.ics.jung.visualization.contrib;
import java.awt.Dimension;
import java.awt.Point;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.SparseTree;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.utils.UserData;
import edu.uci.ics.jung.visualization.AbstractLayout;
import edu.uci.ics.jung.visualization.Coordinates;
import edu.uci.ics.jung.visualization.Layout;

/**
 * @author Karlheinz Toni
 *  
 */

public class TreeLayout extends AbstractLayout implements Layout {

    private static final String C_DIMENSION_X_BASE_KEY = "DimsionX";

    public static int DEFAULT_DISTX = 50;
    public static int DEFAULT_DISTY = 50;
   // private static Logger logger = Logger.getLogger(TreeLayout.class.getName());

    public static Vector getAtomics(Vertex p) {
        Vector v = new Vector();
        getAtomics(p, v);
        return v;
    }

    private static void getAtomics(Vertex p, Vector v) {
        for (Iterator i = p.getSuccessors().iterator(); i.hasNext();) {
            Vertex c = (Vertex) i.next();
            if (c.getSuccessors().isEmpty()) {
                v.add(c);
            } else {
                getAtomics(c, v);
            }
        }
    }
    private transient Set allreadyDone = new HashSet();
//    private transient int currentSiblings_;

    private int distX = DEFAULT_DISTX;
    private int distY = DEFAULT_DISTY;
    private transient Point m_currentPoint = new Point();

    private transient Vertex m_currentVertex;

    private Pair m_dimensionKey;
    private Vertex m_rootVertex;

    public TreeLayout(SparseTree g) {
        super(g);
        //logger.info("Constructor for TreeLayout called" + g.getClass());
        //logger.info("Setting root Node" + g.getRoot());
        this.m_rootVertex = g.getRoot();
    }

    public TreeLayout(SparseTree g, int distx) {
        super(g);
        //logger.info("Constructor for TreeLayout called" + g.getClass());
        //logger.info("Setting root Node" + g.getRoot());
        this.m_rootVertex = g.getRoot();
        this.distX = distx;
    }

    public TreeLayout(SparseTree g, int distx, int disty) {
        super(g);
        //logger.info("Constructor for TreeLayout called" + g.getClass());
        //logger.info("Setting root Node" + g.getRoot());
        this.m_rootVertex = g.getRoot();
        this.distX = distx;
        this.distY = disty;
    }

    /**
     * ?
     * 
     * @see edu.uci.ics.jung.visualization.Layout#advancePositions()
     */
    public void advancePositions() {
        ////logger.info("method called");
    }

    public void applyFilter(Graph g) {
        //logger.info("method called");
        super.applyFilter(g);
    }
    void buildTree() {
        this.m_currentPoint = new Point(this.getCurrentSize().width / 2, 20);
        if (m_rootVertex != null && getGraph() != null) {
            //logger.info("BUILDTREE called");
            //int size = 
            calculateDimensionX(m_rootVertex);
            //logger.info("The tree has got a x-dimension of:" + size);
            buildTree(m_rootVertex, this.m_currentPoint.x);
        }
    }

    void buildTree(Vertex v, int x) {

        if (!allreadyDone.contains(v)) {
            allreadyDone.add(v);

            //go one level further down
            this.m_currentPoint.y += this.distY;
            this.m_currentPoint.x = x;

            this.setCurrentPositionFor(v);

            int sizeXofCurrent = ((Integer) v.getUserDatum(this
                    .getDimensionBaseKey())).intValue();

            int lastX = x - sizeXofCurrent / 2;

            int sizeXofChild;
            int startXofChild;

            for (Iterator j = v.getSuccessors().iterator(); j.hasNext();) {
                Vertex element = (Vertex) j.next();
                sizeXofChild = ((Integer) element.getUserDatum(this
                        .getDimensionBaseKey())).intValue();
                startXofChild = lastX + sizeXofChild / 2;
                buildTree(element, startXofChild);
                lastX = lastX + sizeXofChild + distX;
            }
            this.m_currentPoint.y -= this.distY;
        }
    }
    private int calculateDimensionX(Vertex v) {
        //logger.info("calculating dimension for vertex " + v);
        int size = 0;
        int childrenNum = v.getSuccessors().size();
        //logger.info("vertex " + v + " has got " + childrenNum + " successors");
        if (childrenNum != 0) {
            Vertex element;
            for (Iterator iter = v.getSuccessors().iterator(); iter.hasNext();)
{
                element = (Vertex) iter.next();
                size += calculateDimensionX(element) + distX;
            }
        }
        size = Math.max(0, size - distX);
        v.setUserDatum(this.getDimensionBaseKey(), new Integer(size),
                UserData.REMOVE);
        //logger.info("dimension for vertex " + v + " is " + size);
        return size;
    }

    public int getDepth(Vertex v) {
        int depth = 0;
        for (Iterator i = v.getSuccessors().iterator(); i.hasNext();) {
            Vertex c = (Vertex) i.next();
            if (c.getSuccessors().isEmpty()) {
                depth = 0;
            } else {
                depth = Math.max(depth, getDepth(c));
            }
        }

        return depth + 1;
    }

    private Object getDimensionBaseKey() {
        if (m_dimensionKey == null) {
            m_dimensionKey = new Pair(this, C_DIMENSION_X_BASE_KEY);
        }
        return m_dimensionKey;
    }
    /**
     * @return Returns the rootVertex_.
     */
    public Vertex getRootVertex() {
        return m_rootVertex;
    }

    /**
     * ?
     * 
     * @see edu.uci.ics.jung.visualization.Layout#incrementsAreDone()
     */
    public boolean incrementsAreDone() {
        return true;
    }
    public void initialize(Dimension size) {
        //logger.info("method called " + size);
        super.initialize(size);
        buildTree();
    }
    /**
     * ?
     * 
     * @see edu.uci.ics.jung.visualization.AbstractLayout#initialize_local()
     */
//    protected void initialize_local() {
//
//    }

    /**
     * ?
     * 
     * @see edu.uci.ics.jung.visualization.AbstractLayout#initialize_local_vertex(edu.uci.ics.jung.graph.Vertex)
     */
    protected void initialize_local_vertex(Vertex v) {
        //logger.info("method called");
    }

    protected void initializeLocations() {
        for (Iterator iter = this.getGraph().getVertices().iterator(); iter
                .hasNext();) {
            Vertex v = (Vertex) iter.next();

            Coordinates coord = (Coordinates) v.getUserDatum(getBaseKey());
            if (coord == null) {
                coord = new Coordinates();
                v.addUserDatum(getBaseKey(), coord, UserData.REMOVE);
            }
            initialize_local_vertex(v);
        }
        //logger.info("we have  " + getVisibleVertices().size()
        //        + " visible vertices in our graph");
    }

    /**
     * ?
     * 
     * @see edu.uci.ics.jung.visualization.Layout#isIncremental()
     */
    public boolean isIncremental() {
        return false;
    }

    private void setCurrentPositionFor(Vertex vertex) {
        Coordinates coord = getCoordinates(vertex);
        //logger.info("coordinates for vertex before change" + vertex + "("
             //   + (int) coord.getX() + "," + (int) coord.getY() + ")");
        coord.setX(m_currentPoint.x);
        coord.setY(m_currentPoint.y);
        //logger.info("coordinates for vertex after change" + vertex + "("
          //      + (int) coord.getX() + "," + (int) coord.getY() + ")");
    }

    /**
     * @param rootVertex_
     *            The rootVertex_ to set.
     */
    public void setRootVertex(Vertex rootVertex_) {
        this.m_rootVertex = rootVertex_;
        m_currentVertex = rootVertex_;
    }
}
