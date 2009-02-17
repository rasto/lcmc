/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 * Created on Mar 8, 2005
 *
 */
package edu.uci.ics.jung.visualization.control;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

import javax.swing.JComponent;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.visualization.Layout;
import edu.uci.ics.jung.visualization.PickSupport;
import edu.uci.ics.jung.visualization.PickedState;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.VisualizationViewer.Paintable;

/** 
 * PickingGraphMousePlugin supports the picking of graph elements
 * with the mouse. MouseButtonOne picks a single vertex
 * or edge, and MouseButtonTwo adds to the set of selected Vertices
 * or Edges. If a Vertex is selected and the mouse is dragged while
 * on the selected Vertex, then that Vertex will be repositioned to
 * follow the mouse until the button is released.
 * 
 * @author Tom Nelson
 */
public class PickingGraphMousePlugin extends AbstractGraphMousePlugin
    implements MouseListener, MouseMotionListener {

	/**
	 * the picked Vertex, if any
	 */
    protected Vertex vertex;
    
    /**
     * the picked Edge, if any
     */
    protected Edge edge;
    
    /**
     * the x distance from the picked vertex center to the mouse point
     */
    protected double offsetx;
    
    /**
     * the y distance from the picked vertex center to the mouse point
     */
    protected double offsety;
    
    /**
     * controls whether the Vertices may be moved with the mouse
     */
    protected boolean locked;
    
    /**
     * additional modifiers for the action of adding to an existing
     * selection
     */
    protected int addToSelectionModifiers;
    
    /**
     * used to draw a rectangle to contain picked vertices
     */
    protected Rectangle2D rect = new Rectangle2D.Float();
    
    /**
     * the Paintable for the lens picking rectangle
     */
    protected Paintable lensPaintable;
    
    /**
     * color for the picking rectangle
     */
    protected Color lensColor = Color.cyan;

    /**
	 * create an instance with default settings
	 */
	public PickingGraphMousePlugin() {
	    this(InputEvent.BUTTON1_MASK, InputEvent.BUTTON1_MASK | InputEvent.SHIFT_MASK);
	}

	/**
	 * create an instance with overides
	 * @param selectionModifiers for primary selection
	 * @param addToSelectionModifiers for additional selection
	 */
    public PickingGraphMousePlugin(int selectionModifiers, int addToSelectionModifiers) {
        super(selectionModifiers);
        this.addToSelectionModifiers = addToSelectionModifiers;
        this.lensPaintable = new LensPaintable();
        this.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    }
    
    /**
     * @return Returns the lensColor.
     */
    public Color getLensColor() {
        return lensColor;
    }

    /**
     * @param lensColor The lensColor to set.
     */
    public void setLensColor(Color lensColor) {
        this.lensColor = lensColor;
    }

    /**
     * a Paintable to draw the rectangle used to pick multiple
     * Vertices
     * @author Tom Nelson - RABA Technologies
     *
     */
    class LensPaintable implements Paintable {

        public void paint(Graphics g) {
            Color oldColor = g.getColor();
            g.setColor(lensColor);
            ((Graphics2D)g).draw(rect);
            g.setColor(oldColor);
        }

        public boolean useTransform() {
            return false;
        }
    }

	/**
	 * For primary modifiers (default, MouseButton1):
	 * pick a single Vertex or Edge that
     * is under the mouse pointer. If no Vertex or edge is under
     * the pointer, unselect all picked Vertices and edges, and
     * set up to draw a rectangle for multiple selection
     * of contained Vertices.
     * For additional selection (default Shift+MouseButton1):
     * Add to the selection, a single Vertex or Edge that is
     * under the mouse pointer. If a previously picked Vertex
     * or Edge is under the pointer, it is un-picked.
     * If no vertex or Edge is under the pointer, set up
     * to draw a multiple selection rectangle (as above)
     * but do not unpick previously picked elements.
	 * 
	 * @param e the event
	 */
    public void mousePressed(MouseEvent e) {
        down = e.getPoint();
        VisualizationViewer vv = (VisualizationViewer)e.getSource();
        PickSupport pickSupport = vv.getPickSupport();
        PickedState pickedState = vv.getPickedState();
        if(pickSupport != null && pickedState != null) {
            Layout layout = vv.getGraphLayout();
            if(e.getModifiers() == modifiers) {
                vv.addPostRenderPaintable(lensPaintable);
                rect.setFrameFromDiagonal(down,down);
                // p is the screen point for the mouse event
                Point2D p = e.getPoint();
                // take away the view transform
                Point2D ip = vv.inverseViewTransform(p);
                
                vertex = pickSupport.getVertex(ip.getX(), ip.getY());
                if(vertex != null) {
                    if(pickedState.isPicked(vertex) == false) {
                        pickedState.clearPickedVertices();
                        pickedState.pick(vertex, true);
                    }
                    // layout.getLocation applies the layout transformer so
                    // q is transformed by the layout transformer only
                    Point2D q = layout.getLocation(vertex);
                    // transform the mouse point to graph coordinate system
                    Point2D gp = vv.inverseLayoutTransform(ip);

                    offsetx = (float) (gp.getX()-q.getX());
                    offsety = (float) (gp.getY()-q.getY());
                } else if((edge = pickSupport.getEdge(ip.getX(), ip.getY())) != null) {
                    pickedState.clearPickedEdges();
                    pickedState.pick(edge, true);
                } else {
                    pickedState.clearPickedEdges();
                    pickedState.clearPickedVertices();
                }
                
            } else if(e.getModifiers() == addToSelectionModifiers) {
                vv.addPostRenderPaintable(lensPaintable);
                rect.setFrameFromDiagonal(down,down);
                Point2D p = e.getPoint();
                // remove view transform
                Point2D ip = vv.inverseViewTransform(p);
                vertex = pickSupport.getVertex(ip.getX(), ip.getY());
                if(vertex != null) {
                    boolean wasThere = pickedState.pick(vertex, !pickedState.isPicked(vertex));
                    if(wasThere) {
                        vertex = null;
                    } else {

                        // layout.getLocation applies the layout transformer so
                        // q is transformed by the layout transformer only
                        Point2D q = layout.getLocation(vertex);
                        // translate mouse point to graph coord system
                        Point2D gp = vv.inverseLayoutTransform(ip);

                        offsetx = (float) (gp.getX()-q.getX());
                        offsety = (float) (gp.getY()-q.getY());
                    }
                } else if((edge = pickSupport.getEdge(ip.getX(), ip.getY())) != null) {
                    pickedState.pick(edge, !pickedState.isPicked(edge));
                }
            }
        }
        if(vertex != null) e.consume();
    }

    /**
	 * If the mouse is dragging a rectangle, pick the
	 * Vertices contained in that rectangle
	 * 
	 * clean up settings from mousePressed
	 */
    public void mouseReleased(MouseEvent e) {
        VisualizationViewer vv = (VisualizationViewer)e.getSource();
        if(e.getModifiers() == modifiers) {
            if(down != null) {
                Point2D out = e.getPoint();
                if(vertex == null && heyThatsTooClose(down, out, 5) == false) {
                    pickContainedVertices(vv, true);
                }
            }
        } else if(e.getModifiers() == this.addToSelectionModifiers) {
            if(down != null) {
                Point2D out = e.getPoint();
                if(vertex == null && heyThatsTooClose(down,out,5) == false) {
                    pickContainedVertices(vv, false);
                }
            }
        }
        down = null;
        vertex = null;
        edge = null;
        rect.setFrame(0,0,0,0);
        vv.removePostRenderPaintable(lensPaintable);
    }
    
    /**
	 * If the mouse is over a picked vertex, drag all picked
	 * vertices with the mouse.
	 * If the mouse is not over a Vertex, draw the rectangle
	 * to select multiple Vertices
	 * 
	 */
    public void mouseDragged(MouseEvent e) {
        if(locked == false) {
            VisualizationViewer vv = (VisualizationViewer)e.getSource();
            if(vertex != null) {
                Point p = e.getPoint();
                Point2D graphPoint = vv.inverseTransform(p);
                Point2D graphDown = vv.inverseTransform(down);
                Layout layout = vv.getGraphLayout();
                double dx = graphPoint.getX()-graphDown.getX();
                double dy = graphPoint.getY()-graphDown.getY();
                PickedState ps = vv.getPickedState();
                
                for(Iterator iterator=ps.getPickedVertices().iterator(); iterator.hasNext(); ) {
                    Vertex v = (Vertex)iterator.next();
                    Point2D vp = layout.getLocation(v);
                    layout.forceMove(v, vp.getX()+dx, vp.getY()+dy);
                }
                down = p;

            } else {
                Point2D out = e.getPoint();
                if(e.getModifiers() == this.addToSelectionModifiers ||
                        e.getModifiers() == modifiers) {
                    rect.setFrameFromDiagonal(down,out);
                }
            }
            if(vertex != null) e.consume();
        }
    }
    
    /**
     * rejects picking if the rectangle is too small, like
     * if the user meant to select one vertex but moved the
     * mouse slightly
     * @param p
     * @param q
     * @param min
     * @return
     */
    private boolean heyThatsTooClose(Point2D p, Point2D q, double min) {
        return Math.abs(p.getX()-q.getX()) < min &&
                Math.abs(p.getY()-q.getY()) < min;
    }
    
    /**
     * pick the vertices inside the rectangle
     *
     */
    protected void pickContainedVertices(VisualizationViewer vv, boolean clear) {
        
        Layout layout = vv.getGraphLayout();
        PickedState pickedState = vv.getPickedState();
        if(pickedState != null) {
            if(clear) {
                pickedState.clearPickedVertices();
            }
            while(true) {
                try {
                    for (Iterator iter=layout.getGraph().getVertices().iterator(); iter.hasNext();  ) {
                        Vertex v = (Vertex) iter.next();
                        if(rect.contains(vv.transform(layout.getLocation(v)))) {
                            pickedState.pick(v, true);
                        }
                    }
                    break;
                } catch(ConcurrentModificationException cme) {}
            }
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
        JComponent c = (JComponent)e.getSource();
        c.setCursor(cursor);
    }

    public void mouseExited(MouseEvent e) {
        JComponent c = (JComponent)e.getSource();
        c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    public void mouseMoved(MouseEvent e) {
    }

    /**
     * @return Returns the locked.
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * @param locked The locked to set.
     */
    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}
