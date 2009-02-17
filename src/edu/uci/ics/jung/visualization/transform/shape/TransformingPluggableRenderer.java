/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 *
 * Created on Jul 11, 2005
 */

package edu.uci.ics.jung.visualization.transform.shape;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.Icon;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.UndirectedEdge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.VertexIconFunction;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.visualization.PluggableRenderer;
import edu.uci.ics.jung.visualization.transform.HyperbolicTransformer;
import edu.uci.ics.jung.visualization.transform.MutableAffineTransformer;
import edu.uci.ics.jung.visualization.transform.Transformer;

/**
 * a subclass to apply a TransformingGraphics to certain operations
 * @author Tom Nelson - RABA Technologies
 *
 *
 */
public class TransformingPluggableRenderer extends PluggableRendererDecorator {
    
    /**
     * the transformer
     */
    Transformer transformer;
    
    /**
     * the Graphics wrapper that uses the transformer
     */
    TransformingGraphics tg2d;
    
    /**
     * create an instance
     *
     */
    public TransformingPluggableRenderer(PluggableRenderer delegate) {
        super(delegate);
        this.transformer = new MutableAffineTransformer();
        this.tg2d = new TransformingGraphics(transformer);
    }
    
    /**
     * @return Returns the transformer.
     */
    public Transformer getTransformer() {
        return transformer;
    }

    /**
     * @param transformer The transformer to set.
     */
    public void setTransformer(Transformer transformer) {
        this.transformer = transformer;
        this.tg2d.setTransformer(transformer);
    }

    /** (non-Javadoc)
     * override to wrap the passed Graphics2D in my TransformingGraphics,
     * then call overloaded drawSimpleEdge
     */
    protected void drawSimpleEdge(Graphics2D g, Edge e, int x1, int y1, int x2, int y2) {
        this.tg2d.setDelegate(g);
        drawSimpleEdge(tg2d, e, x1, y1, x2, y2);
    }
    /**
     * overloaded to use TransformingGraphics
     * @param g
     * @param e
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     */
    protected void drawSimpleEdge(TransformingGraphics g, Edge e, int x1, int y1, int x2, int y2) {
        float flatness = 0;
        
        if(transformer instanceof HyperbolicTransformer) {
            HyperbolicTransformer ht = (HyperbolicTransformer)transformer;
            Ellipse2D hyperEllipse = ht.getEllipse();
            if(hyperEllipse.contains(x1,y1) || hyperEllipse.contains(x2,y2)) {
                flatness = .05f;
            }
        }
        Pair endpoints = e.getEndpoints();
        Vertex v1 = (Vertex)endpoints.getFirst();
        Vertex v2 = (Vertex)endpoints.getSecond();
        boolean isLoop = v1.equals(v2);
        Shape s2 = getVertexShapeFunction().getShape(v2);
        Shape edgeShape = getEdgeShapeFunction().getShape(e);
        
        boolean edgeHit = true;
        boolean arrowHit = true;
        Rectangle deviceRectangle = null;
        if(getScreenDevice() != null) {
            Dimension d = getScreenDevice().getSize();
            deviceRectangle = new Rectangle(0,0,d.width,d.height);
        }
        
        AffineTransform xform = AffineTransform.getTranslateInstance(x1, y1);
        
        if(isLoop) {
            // this is a self-loop. scale it is larger than the vertex
            // it decorates and translate it so that its nadir is
            // at the center of the vertex.
            Rectangle2D s2Bounds = s2.getBounds2D();
            xform.scale(s2Bounds.getWidth(),s2Bounds.getHeight());
            xform.translate(0, -edgeShape.getBounds2D().getWidth()/2);
        } else {
            // this is a normal edge. Rotate it to the angle between
            // vertex endpoints, then scale it to the distance between
            // the vertices
            float dx = x2-x1;
            float dy = y2-y1;
            float thetaRadians = (float) Math.atan2(dy, dx);
            xform.rotate(thetaRadians);
            float dist = (float) Math.sqrt(dx*dx + dy*dy);
            xform.scale(dist, 1.0);
        }
        
        edgeShape = xform.createTransformedShape(edgeShape);
        
        edgeHit = g.hit(deviceRectangle, edgeShape, true);
        
        if(edgeHit == true) {
            
            Paint oldPaint = g.getPaint();
            
            // get Paints for filling and drawing
            // (filling is done first so that drawing and label use same Paint)
            Paint fill_paint = getEdgePaintFunction().getFillPaint(e); 
            if (fill_paint != null)
            {
                g.setPaint(fill_paint);
                g.fill(edgeShape, flatness);
            }
            Paint draw_paint = getEdgePaintFunction().getDrawPaint(e);
            if (draw_paint != null)
            {
                g.setPaint(draw_paint);
                g.draw(edgeShape, flatness);
            }
            
            float scalex = (float)g.getTransform().getScaleX();
            float scaley = (float)g.getTransform().getScaleY();
            // see if arrows are too small to bother drawing
            if(scalex < .3 || scaley < .3) return;
            
            if (getEdgeArrowPredicate().evaluate(e)) {
                
                Shape destVertexShape = 
                    getVertexShapeFunction().getShape((Vertex)e.getEndpoints().getSecond());
                AffineTransform xf = AffineTransform.getTranslateInstance(x2, y2);
                destVertexShape = xf.createTransformedShape(destVertexShape);

                arrowHit = g.hit(deviceRectangle, destVertexShape, true);

                if(arrowHit) {
                    
                    AffineTransform at = 
                        getArrowTransform(new GeneralPath(edgeShape), destVertexShape);
                    if(at == null) return;
                    Shape arrow = getEdgeArrowFunction().getArrow(e);
                    arrow = at.createTransformedShape(arrow);
                    // note that arrows implicitly use the edge's draw paint
                    g.fill(arrow);
                    if (e instanceof UndirectedEdge) {
                        Shape vertexShape = 
                            getVertexShapeFunction().getShape((Vertex)e.getEndpoints().getFirst());
                        xf = AffineTransform.getTranslateInstance(x1, y1);
                        vertexShape = xf.createTransformedShape(vertexShape);

                        arrowHit = g.hit(deviceRectangle, vertexShape, true);
                        if(arrowHit) {
                            at = getReverseArrowTransform(new GeneralPath(edgeShape), vertexShape, !isLoop);
                            if(at == null) return;
                            arrow = getEdgeArrowFunction().getArrow(e);
                            arrow = at.createTransformedShape(arrow);
                            g.fill(arrow);
                        }
                    }
                }
            }
                // use existing paint for text if no draw paint specified
                if (draw_paint == null)
                    g.setPaint(oldPaint);
                String label = getEdgeStringer().getLabel(e);
                if (label != null) {
                    labelEdge(g, e, label, x1, x2, y1, y2);
                }
            
            
            // restore old paint
            g.setPaint(oldPaint);
        }
    }


    /**
     * overridden to wrap passed Graphics2D in my TransformingGraphics, then call
     * overloaded labelEdge
     */
    protected void labelEdge(Graphics2D g2d, Edge e, String label, int x1, int x2, int y1, int y2) {
        tg2d.setDelegate(g2d);
        labelEdge(tg2d, e, label, x1, x2, y1, y2);
    }
    /**
     * overloaded to use TransformingGraphics
     * @param g2d
     * @param e
     * @param label
     * @param x1
     * @param x2
     * @param y1
     * @param y2
     */
    protected void labelEdge(TransformingGraphics g2d, Edge e, String label, int x1, int x2, int y1, int y2) 
    {
        Point2D p = g2d.getTransformer().transform(new Point2D.Float(x1, y1));
        Point2D q = g2d.getTransformer().transform(new Point2D.Float(x2, y2));
        x1 = (int)p.getX();
        y1 = (int)p.getY();
        x2 = (int)q.getX();
        y2 = (int)q.getY();
        
        int distX = x2 - x1;
        int distY = y2 - y1;
        double totalLength = Math.sqrt(distX * distX + distY * distY);

        double closeness = getEdgeLabelClosenessFunction().getNumber(e).doubleValue();

        int posX = (int) (x1 + (closeness) * distX);
        int posY = (int) (y1 + (closeness) * distY);

        int xDisplacement = (int) (PluggableRenderer.LABEL_OFFSET * (distY / totalLength));
        int yDisplacement = (int) (PluggableRenderer.LABEL_OFFSET * (-distX / totalLength));
        
        Component component = prepareRenderer(getGraphLabelRenderer(), label, isPicked(e), e);
        Dimension d = component.getPreferredSize();
        
        Font font = getEdgeFontFunction().getFont(e);
        if(font != null)
            component.setFont(font);    
        
        Shape edgeShape = getEdgeShapeFunction().getShape(e);
        
        double parallelOffset = 1;
        Integer parallelCount = (Integer)e.getUserDatum("parallelCount");
        if(parallelCount != null) {
            parallelOffset += parallelCount.intValue();
        }

        if(edgeShape instanceof Ellipse2D) {
            parallelOffset += edgeShape.getBounds().getHeight();
            parallelOffset = -parallelOffset;
        }
        
        parallelOffset *= d.height;
        
        AffineTransform old = g2d.getTransform();
        AffineTransform xform = new AffineTransform(old);
        xform.translate(posX+xDisplacement, posY+yDisplacement);
        if(getGraphLabelRenderer().isRotateEdgeLabels()) {
            // float thetaRadians = (float) Math.atan2(dy, dx);
            double dx = x2 - x1;
            double dy = y2 - y1;
            double theta = Math.atan2(dy, dx);
            if(dx < 0) {
                theta += Math.PI;
                parallelOffset = -parallelOffset;
            }
            xform.rotate(theta);
        }
        
        xform.translate(-d.width/2, -(d.height/2-parallelOffset));
        g2d.setTransform(xform);
        getRendererPane().paintComponent(g2d.getDelegate(), component, getScreenDevice(), 
                0, 0,
                d.width, d.height, true);
        g2d.setTransform(old);
    }

    /**
     * overridden to wrap passed Graphics in my TransformingGraphics, then
     * call overloaded labelVertex
     */
    protected void labelVertex(Graphics g, Vertex v, String label, int x, int y) {
        tg2d.setDelegate((Graphics2D)g);
        labelVertex(tg2d, v, label, x, y);
    }
    /**
     * overloaded to use TransformingGraphics
     * @param g
     * @param v
     * @param label
     * @param x
     * @param y
     */
    protected void labelVertex(TransformingGraphics g, Vertex v, String label, int x, int y) {
        Component component = prepareRenderer(getGraphLabelRenderer(), label, isPicked(v), v);
        Font font = getVertexFontFunction().getFont(v);
        if (font != null)
            component.setFont(font);
        
        Dimension d = component.getPreferredSize();
        
        Point2D p = g.getTransformer().transform(new Point2D.Float(x, y));
        x = (int)p.getX();
        y = (int)p.getY();
        
        int h_offset;
        int v_offset;
        if (getVertexLabelCentering())
        {
            h_offset = -d.width / 2;
            v_offset = -d.height / 2;

        }
        else
        {
            Rectangle2D bounds = getVertexShapeFunction().getShape(v).getBounds2D();
            h_offset = (int)(bounds.getWidth() / 2) + 5;
            v_offset = (int)(bounds.getHeight() / 2) + 5 -d.height;
        }
        
        getRendererPane().paintComponent(g.getDelegate(), component, getScreenDevice(), x+h_offset, y+v_offset,
                d.width, d.height, true);
        
    }

    /**
     * overridded to wrap passed Graphics in TransformingGraphics then call
     * overloaded paintEdge
     */
    public void paintEdge(Graphics g, Edge e, int x1, int y1, int x2, int y2) {
        this.tg2d.setDelegate((Graphics2D)g);
        paintEdge(tg2d, e, x1, y1, x2, y2);
    }
    /**
     * overloaded to use TransformingGraphics
     * @param g2d
     * @param e
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     */
    public void paintEdge(TransformingGraphics g2d, Edge e, int x1, int y1, int x2, int y2) {
        if (!getEdgeIncludePredicate().evaluate(e))
            return;
        
        // don't draw edge if either incident vertex is not drawn
        Pair endpoints = e.getEndpoints();
        Vertex v1 = (Vertex)endpoints.getFirst();
        Vertex v2 = (Vertex)endpoints.getSecond();
        if (!getVertexIncludePredicate().evaluate(v1) || 
            !getVertexIncludePredicate().evaluate(v2))
            return;
        
        Stroke new_stroke = getEdgeStrokeFunction().getStroke(e);
        Stroke old_stroke = g2d.getStroke();
        if (new_stroke != null)
            g2d.setStroke(new_stroke);
        
        drawSimpleEdge(g2d, e, x1, y1, x2, y2);

        // restore paint and stroke
        if (new_stroke != null)
            g2d.setStroke(old_stroke);

    }

    /**
     * overridden to wrap passed Graphics in TransformingGraphics then
     * call overloaded paintVertex
     */
    public void paintVertex(Graphics g, Vertex v, int x, int y) {
        tg2d.setDelegate((Graphics2D)g);
        paintVertex(tg2d, v, x, y);
    }
    /**
     * overloaded to use TransformingGraphics
     * @param g2d
     * @param v
     * @param x
     * @param y
     */
    public void paintVertex(TransformingGraphics g2d, Vertex v, int x, int y) {
        if (!getVertexIncludePredicate().evaluate(v))
            return;
        
        boolean vertexHit = true;
        Rectangle deviceRectangle = null;
        if(getScreenDevice() != null) {
            Dimension d = getScreenDevice().getSize();
            deviceRectangle = new Rectangle(0,0,d.width,d.height);
        }
        
        Stroke old_stroke = g2d.getStroke();
        Stroke new_stroke = getVertexStrokeFunction().getStroke(v);
        if (new_stroke != null) {
            g2d.setStroke(new_stroke);
        }
        // get the shape to be rendered
        Shape s = getVertexShapeFunction().getShape(v);
        
        // create a transform that translates to the location of
        // the vertex to be rendered
        AffineTransform xform = AffineTransform.getTranslateInstance(x,y);
        // transform the vertex shape with xtransform
        s = xform.createTransformedShape(s);
        

        vertexHit = g2d.hit(deviceRectangle, s, true);

        if (vertexHit) {
			VertexIconFunction imager = getVertexIconFunction();

    			Icon icon = null;
			if (imager != null && (icon = imager.getIcon(v)) != null) {
				paintIconForVertex(g2d, icon, x, y);
			} else {
				paintShapeForVertex(g2d, v, s);
			}

			if (new_stroke != null) {
				g2d.setStroke(old_stroke);
			}
			String label = getVertexStringer().getLabel(v);
			if (label != null) {
				labelVertex(g2d, v, label, x, y);
			}
		}
    }
    
    protected void paintShapeForVertex(TransformingGraphics g2d, Vertex v, Shape shape) {
        Paint oldPaint = g2d.getPaint();
        Paint fillPaint = getVertexPaintFunction().getFillPaint(v);
        if(fillPaint != null) {
            g2d.setPaint(fillPaint);
            g2d.fill(shape);
            g2d.setPaint(oldPaint);
        }
        Paint drawPaint = getVertexPaintFunction().getDrawPaint(v);
        if(drawPaint != null) {
            g2d.setPaint(drawPaint);
            g2d.draw(shape);
            g2d.setPaint(oldPaint);
        }
    }

    public void paintIconForVertex(TransformingGraphics g, Icon icon, int x, int y) {
         if(icon != null) {
             int xLoc = x - icon.getIconWidth()/2;
             int yLoc = y - icon.getIconHeight()/2;
             icon.paintIcon(getScreenDevice(), g.getDelegate(), xLoc, yLoc);
         }
     }
}
