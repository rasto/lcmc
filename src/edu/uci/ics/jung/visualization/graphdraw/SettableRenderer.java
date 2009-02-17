/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.visualization.graphdraw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

import edu.uci.ics.jung.graph.DirectedEdge;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.UndirectedEdge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.EdgeColorFunction;
import edu.uci.ics.jung.graph.decorators.EdgeStringer;
import edu.uci.ics.jung.graph.decorators.EdgeThicknessFunction;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.graph.decorators.VertexColorFunction;
import edu.uci.ics.jung.graph.predicates.EdgePredicate;
import edu.uci.ics.jung.graph.predicates.SelfLoopEdgePredicate;
import edu.uci.ics.jung.visualization.AbstractRenderer;
import edu.uci.ics.jung.visualization.contrib.Arrow;

/**
 * A renderer with all sorts of buttons to press and dials to turn. In general,
 * if a function is available to get an answer to questions of color. Otherwise,
 * the set fields are used.
 * <p>
 * The default is to paint vertices with Black foreground text and Red
 * backgrounds. Picked vertices are orange. [Whether a vertex is Picked is
 * resolved with <tt>v.getUserDatum(_VisualizationViewer.VIS_KEY);</tt>]
 * 
 * <p>
 * Note that setting a stroke width other than 1 is likely to slow down the
 * visualization noticably, as is using transparency.
 * 
 * @deprecated Replaced by PluggableRenderer.
 * @author danyelf
 */
public class SettableRenderer extends AbstractRenderer {

    protected Color vertexFGColor = Color.BLACK;

    protected Color vertexPickedColor = Color.ORANGE;

    protected Color vertexBGColor = Color.RED;

    protected VertexColorFunction vertexColorFunction;

    protected EdgeThicknessFunction edgeThicknessFunction;

    protected int edgeThickness;

    protected Color edgeColor = Color.BLACK;

    protected EdgeColorFunction edgeColorFunction;

    protected StringLabeller mStringLabeller;

    protected boolean mShouldDrawSelfLoops = false;

    protected boolean mDrawLightBoxes = true;

    protected boolean mShouldDrawArrowsUndirected = false;

    protected boolean mShouldDrawArrowsDirected = true;

    protected Arrow mArrow;

    protected EdgeStringer mEdgeLabelFunction;

    protected int mLineHeight;

    protected static EdgePredicate self_loop = SelfLoopEdgePredicate.getInstance();
    
    /**
     * This variant simply renders vertices as small empty boxes without labels.
     */
    public SettableRenderer() {
        this.mStringLabeller = null;
    }

    /**
     * Creates a SettableRenderer that will be drawn in the "heavy" style: a box
     * around the label
     * 
     * @param sl
     */
    public SettableRenderer(StringLabeller sl) {
        this.mStringLabeller = sl;
    }

    /**
     * Creates a SettableRenderer that will label edges with the given EdgeStringer.
     * (You may want EdgeWeightLabellerStringer, which uses an EdgeWeightLabeller to
     * label the weights.) 
     * @param sl
     * @param el
     */
    public SettableRenderer(StringLabeller sl, EdgeStringer el) {
        this.mStringLabeller = sl;
        this.mEdgeLabelFunction = el;
    }

    /**
     * Creates a SettableRenderer that will be drawn in the "light" style: a
     * colored box next to text, instead of text overlaying the box.
     */
    public void setLightDrawing(boolean b) {
        this.mDrawLightBoxes = b;
    }

    public void setStringLabeller(StringLabeller sl) {
        this.mStringLabeller = sl;
    }

    public void setEdgeColor(Color c) {
        edgeColor = c;
    }

    /**
     * Edges are drawn by calling <tt>EdgeColorFunction</tt> with the edge, to
     * decide how it is to be drawn.
     * 
     * @param ecf
     */
    public void setEdgeColorFunction(EdgeColorFunction ecf) {
        this.edgeColorFunction = ecf;
    }

    /**
     * Forces all edges to draw with this thickness. Sets the edge thickness
     * function to null.
     * 
     * @param i
     */
    public void setEdgeThickness(int i) {
        this.edgeThicknessFunction = null;
        this.edgeThickness = i;
    }

    /**
     * This version takes a function that dynamically chooses an edge thickness.
     * 
     * @param etf
     */
    public void setEdgeThicknessFunction(EdgeThicknessFunction etf) {
        this.edgeThicknessFunction = etf;
        this.edgeThickness = 0;
    }

    /**
     * Sets whether the system should draw arrows on directed edges. By default,
     * yes.
     * 
     * @param b
     */
    public void setShouldDrawDirectedArrows(boolean b) {
        this.mShouldDrawArrowsDirected = b;
    }

    /**
     * Sets whether the system should draw arrows on directed edges. By default,
     * yes.
     * 
     * @param b
     */
    public void setShouldDrawUndirectedArrows(boolean b) {
        this.mShouldDrawArrowsUndirected = b;
    }
    
    /**
     * Sets whether the system should draw self-loops. By default, no.
     * @param b
     */
    public void setShouldDrawSelfLoops( boolean b ) {
        this.mShouldDrawSelfLoops = b;        
    }

    /**
     * Paints the edge in the color specified by the EdgeColorFunction or the
     * hard-set color, and at the thickness set with an
     * <tt>EdgeThicknessFunction</tt>. Draws a self-loop if
     * <tt>shouldDrawSelfLoops()</tt> has been set (by default, no); draws an
     * arrow on directed edges if <tt>shouldDrawDirectedArrows()</tt> has been
     * set (by default, yes) and on both ends of undirected edges if
     * <tt>shouldDrawUndirectedArrows()</tt> has been set (by default, false).
     * Calls either drawEdge or drawEdgeSimple. Draws one arrow for
     * self-loops if needed. Note that x1, y1 always correspond to
     * e.getEndpoints.getFirst() and x2, y2 always correspond to
     * e.getEndpoints.getSecond()
     * 
     * @see EdgeThicknessFunction EdgeThicknessFunction
     * @see EdgeColorFunction EdgeColorFunction
     */
    public void paintEdge(Graphics g, Edge e, int x1, int y1, int x2, int y2) {
        Graphics2D g2d = (Graphics2D) g;
        mLineHeight = g2d.getFontMetrics().getHeight();

        float edgeWidth;
        if (edgeThicknessFunction != null)
            edgeWidth = edgeThicknessFunction.getEdgeThickness(e);
        else
            edgeWidth = edgeThickness;

        if (edgeColorFunction == null) {
            g.setColor(edgeColor);
        } else {
            g.setColor(edgeColorFunction.getEdgeColor(e));
        }

        if (edgeWidth == 1)
            drawEdgeSimple(g, e, x1, y1, x2, y2);
        else
            drawEdge(edgeWidth, g, e, x1, y1, x2, y2);

        if (mShouldDrawArrowsDirected && e instanceof DirectedEdge) {
            drawArrowhead(g2d, e, x1, y1, x2, y2);
        }
        if (mShouldDrawArrowsUndirected && e instanceof UndirectedEdge) {
            drawArrowhead(g2d, e, x1, y1, x2, y2);
            drawArrowhead(g2d, e, x2, y2, x1, y1);
        }

        String label = (mEdgeLabelFunction == null) ? null : mEdgeLabelFunction
                .getLabel(e);
        if (label != null) {
            labelEdge(g2d, e, label, x1, x2, y1, y2);
        }
    }

    /**
     * Labels the edge at the half-way point (if undirected) or three-quarters
     * if directed or 15 pixels above the vertex if self-loop.
     * 
     * @param g2d
     * @param e
     * @param label
     * @param x1
     * @param x2
     * @param y1
     * @param y2
     */
    public void labelEdge(Graphics2D g2d, Edge e, String label, int x1, int x2,
            int y1, int y2) {

        if (self_loop.evaluate(e)) {
            g2d.drawString(label, x1 - 3 , y1 - 10 - mLineHeight/2);
            return;
        }
        
        int distX = x2 - x1;
        int distY = y2 - y1;
        double totalLength = Math.sqrt(distX * distX + distY * distY);

        //closeness is a double in the range [0,1] that represents
        //how close to the target vertex the edge weight should be
        //drawn (0 means "on the source vertex", 1 means "on the target
        // vertex")
        //weights of undirected edges should be drawn at 0.5 (in the middle of
        // the edge)
        double closeness;
        if (e instanceof DirectedEdge) {
            closeness = 0.73;
        } else {
            closeness = 0.5;
        }

        int posX = (int) (x1 + (closeness) * distX);
        int posY = (int) (y1 + (closeness) * distY);

        int LEN = 10;
        int xDisplacement = (int) (LEN * (-distY / totalLength));
        int yDisplacement = (int) (LEN * (distX / totalLength));
        g2d.drawString(label, posX + xDisplacement, posY + yDisplacement + mLineHeight / 2);
    }

    /**
     * Draws an arrowhead on this edge in the direction from xsource,ysource to
     * xend, yend
     */
    protected void drawArrowhead(Graphics2D g2d, Edge e, int xsource,
            int ysource, int xdest, int ydest) {

        if (mArrow == null) {
            mArrow = new Arrow(Arrow.CLASSIC, 7, 9);
        }
        
        if (mShouldDrawSelfLoops && self_loop.evaluate(e)) {
            mArrow.drawArrow(g2d, xsource - 10, ysource - 5, xsource, ysource, 15);
            return;
        }
        
        if (mDrawLightBoxes) {
            mArrow.drawArrow(g2d, xsource, ysource, xdest, ydest, 12);
        } else {
            mArrow.drawArrow(g2d, xsource, ysource, xdest, ydest, 16);
        }

    }

    /**
     * Draws the edge at the given width, then restores the previous stroke.
     * Calls drawEdgeSimple to accomplish this task.
     * 
     * @param edgeWidth
     *            the width of the stroke.
     */
    protected void drawEdge(float edgeWidth, Graphics g, Edge e, int x1,
            int y1, int x2, int y2) {
        Graphics2D g2d = (Graphics2D) g;

        Stroke previous = g2d.getStroke();

//        if (Math.floor(edgeWidth) == edgeWidth) {
//            if (strokeTable[(int) edgeWidth] == null) {
//                Stroke s = new BasicStroke(edgeWidth);
//                strokeTable[(int) edgeWidth] = s;
//                g2d.setStroke(strokeTable[(int) edgeWidth]);
//            } else {
                g2d.setStroke(new BasicStroke(edgeWidth));
//            }
//        }
        drawEdgeSimple(g, e, x1, y1, x2, y2);
        g2d.setStroke(previous);
    }

    protected void drawEdgeSimple(Graphics g, Edge e, int x1, int y1, int x2,
            int y2) {

        if (mShouldDrawSelfLoops && self_loop.evaluate(e)) {
            // self-loops
            g.drawOval(x1 - 15, y1 - 30, 30, 30);
        } else {
            g.drawLine(x1, y1, x2, y2);
        }
    }

    /**
     * Manually sets the color of a Vertex's foreground (i.e. its text).
     * 
     * @param vertexColor
     */
    public void setVertexForegroundColor(Color vertexColor) {
        this.vertexFGColor = vertexColor;
    }

    /**
     * Manually sets the color of a picked Vertex's background (i.e. its field).
     * 
     * @param vertexColor
     */
    public void setVertexPickedColor(Color vertexColor) {
        this.vertexPickedColor = vertexColor;
    }

    /**
     * Manually sets the color of an unpicked Vertex's background (i.e. its
     * field).
     * 
     * @param vertexColor
     */
    public void setVertexBGColor(Color vertexColor) {
        this.vertexBGColor = vertexColor;
    }

    /**
     * Finds the color of a vertex with a VertexColorFunction.
     * 
     * @param vcf
     */
    public void setVertexColorFunction(VertexColorFunction vcf) {
        this.vertexColorFunction = vcf;
    }

    /**
     * Simple label function returns the StringLabeller's notion of v's label.
     * It may be sometimes useful to override this.
     * 
     * @param v
     *            a vertex
     * @return the label on the vertex.
     */
    protected String getLabel(Vertex v) {
        if (mStringLabeller == null) return "";
        String s = mStringLabeller.getLabel(v);
        if (s == null) {
            return "?";
        } else {
            return s;
        }
    }

    /**
     * Paints the vertex, using the settings above (VertexColors, etc). In this
     * implmenetation, vertices are painted as filled squares with textual
     * labels over the filled square.
     */
    public void paintVertex(Graphics g, Vertex v, int x, int y) {
        String label = getLabel(v);
        if (mDrawLightBoxes) {
            paintLightVertex(g, v, x, y, label);
            return;
        }

        Color fg = (vertexColorFunction == null) ? vertexFGColor
                : vertexColorFunction.getForeColor(v);

        if (vertexColorFunction == null) {
            if (isPicked(v)) {
                g.setColor(vertexPickedColor);
            } else
                g.setColor(vertexBGColor);
        } else {
            g.setColor(vertexColorFunction.getBackColor(v));
        }

        g.fillRect(x - 8, y - 6, g.getFontMetrics().stringWidth(label) + 8, 16);
        g.setColor(fg);
        g.drawString(label, x - 4, y + 6);
    }

    /**
     * @param g
     * @param v
     * @param x
     * @param y
     */
    protected void paintLightVertex(Graphics g, Vertex v, int x, int y,
            String label) {
        Color bg;
        if (vertexColorFunction == null) {
            if (isPicked(v)) {
                bg = vertexPickedColor;
            } else
                bg = vertexBGColor;
        } else {
            bg = vertexColorFunction.getBackColor(v);
        }

        Color fg = (vertexColorFunction == null) ? vertexFGColor
                : vertexColorFunction.getForeColor(v);

        g.setColor(fg);
        g.fillRect(x - 7, y - 7, 14, 14);
        g.setColor(bg);
        g.fillRect(x - 6, y - 6, 12, 12);
        if (label.equals("?")) return;
        g.setColor(fg);
        g.drawString(label, x + 8, y + 6 ); //  + g.getFontMetrics().getHeight());
    }

}