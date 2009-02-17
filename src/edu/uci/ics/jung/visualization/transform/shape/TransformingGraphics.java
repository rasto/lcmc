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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

import edu.uci.ics.jung.visualization.transform.HyperbolicTransformer;
import edu.uci.ics.jung.visualization.transform.Transformer;


/**
 * subclassed to pass certain operations thru the transformer
 * before the base class method is applied
 * This is useful when you want to apply non-affine transformations
 * to the Graphics2D used to draw elements of the graph.
 * 
 * @author Tom Nelson - RABA Technologies
 *
 *
 */
public class TransformingGraphics extends GraphicsDecorator {
    
    /**
     * the transformer to apply
     */
    protected Transformer transformer;
    
    public TransformingGraphics(Transformer transformer) {
        this(transformer, null);
    }
    
    public TransformingGraphics(Transformer transformer, Graphics2D delegate) {
        super(delegate);
        this.transformer = transformer;
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
    }
    
    /**
     * transform the shape before letting the delegate draw it
     */
    public void draw(Shape s) {
        Shape shape = ((ShapeTransformer)transformer).transform(s);
        delegate.draw(shape);
    }
    
    public void draw(Shape s, float flatness) {
        Shape shape = null;
        if(transformer instanceof HyperbolicTransformer) {
            shape = ((HyperbolicShapeTransformer)transformer).transform(s, flatness);
        } else {
            shape = ((ShapeTransformer)transformer).transform(s);
        }
        delegate.draw(shape);
        
    }
    
    /**
     * transform the shape before letting the delegate fill it
     */
    public void fill(Shape s) {
        Shape shape = ((ShapeTransformer)transformer).transform(s);
        delegate.fill(shape);
    }
    
    public void fill(Shape s, float flatness) {
        Shape shape = null;
        if(transformer instanceof HyperbolicTransformer) {
            shape = ((HyperbolicShapeTransformer)transformer).transform(s, flatness);
        } else {
            shape = ((ShapeTransformer)transformer).transform(s);
        }
        delegate.fill(shape);
    }
    
    /**
     * transform the shape before letting the delegate apply 'hit'
     * with it
     */
    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        Shape shape = ((ShapeTransformer)transformer).transform(s);
        return delegate.hit(rect, shape, onStroke);
    }
    
    public Graphics create() {
        return delegate.create();
    }
    
    public void dispose() {
        delegate.dispose();
    }
    
}
