/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 *
 * Created on Jul 21, 2005
 */

package edu.uci.ics.jung.visualization.transform.shape;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;

import javax.swing.CellRendererPane;
import javax.swing.JComponent;

import org.apache.commons.collections.Predicate;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.EdgeArrowFunction;
import edu.uci.ics.jung.graph.decorators.EdgeColorFunction;
import edu.uci.ics.jung.graph.decorators.EdgeFontFunction;
import edu.uci.ics.jung.graph.decorators.EdgePaintFunction;
import edu.uci.ics.jung.graph.decorators.EdgeShapeFunction;
import edu.uci.ics.jung.graph.decorators.EdgeStringer;
import edu.uci.ics.jung.graph.decorators.EdgeStrokeFunction;
import edu.uci.ics.jung.graph.decorators.NumberEdgeValue;
import edu.uci.ics.jung.graph.decorators.VertexColorFunction;
import edu.uci.ics.jung.graph.decorators.VertexFontFunction;
import edu.uci.ics.jung.graph.decorators.VertexIconFunction;
import edu.uci.ics.jung.graph.decorators.VertexPaintFunction;
import edu.uci.ics.jung.graph.decorators.VertexShapeFunction;
import edu.uci.ics.jung.graph.decorators.VertexStringer;
import edu.uci.ics.jung.graph.decorators.VertexStrokeFunction;
import edu.uci.ics.jung.utils.ParallelEdgeIndexFunction;
import edu.uci.ics.jung.visualization.GraphLabelRenderer;
import edu.uci.ics.jung.visualization.HasShapeFunctions;
import edu.uci.ics.jung.visualization.PickedInfo;
import edu.uci.ics.jung.visualization.PluggableRenderer;
import edu.uci.ics.jung.visualization.Renderer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;

/**
 * A complete wrapping of PluggableRenderer, used as a base class
 * 
 * @author Tom Nelson - RABA Technologies
 *
 */
public abstract class PluggableRendererDecorator implements Renderer, PickedInfo, HasShapeFunctions {

    protected PluggableRenderer delegate;
    
    public PluggableRendererDecorator(PluggableRenderer delegate) {
        this.delegate = delegate;
    }
    
    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getEdgeArrowFunction()
     */
    public EdgeArrowFunction getEdgeArrowFunction() {
        return delegate.getEdgeArrowFunction();
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getEdgeArrowPredicate()
     */
    public Predicate getEdgeArrowPredicate() {
        return delegate.getEdgeArrowPredicate();
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getEdgeFontFunction()
     */
    public EdgeFontFunction getEdgeFontFunction() {
        return delegate.getEdgeFontFunction();
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getEdgeIncludePredicate()
     */
    public Predicate getEdgeIncludePredicate() {
        return delegate.getEdgeIncludePredicate();
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getEdgeLabelClosenessFunction()
     */
    public NumberEdgeValue getEdgeLabelClosenessFunction() {
        return delegate.getEdgeLabelClosenessFunction();
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getEdgePaintFunction()
     */
    public EdgePaintFunction getEdgePaintFunction() {
        return delegate.getEdgePaintFunction();
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getEdgeStringer()
     */
    public EdgeStringer getEdgeStringer() {
        return delegate.getEdgeStringer();
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getEdgeStrokeFunction()
     */
    public EdgeStrokeFunction getEdgeStrokeFunction() {
        return delegate.getEdgeStrokeFunction();
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getRendererPane()
     */
    public CellRendererPane getRendererPane() {
        return delegate.getRendererPane();
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getScreenDevice()
     */
    public JComponent getScreenDevice() {
        return delegate.getScreenDevice();
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getVertexFontFunction()
     */
    public VertexFontFunction getVertexFontFunction() {
        return delegate.getVertexFontFunction();
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getVertexIncludePredicate()
     */
    public Predicate getVertexIncludePredicate() {
        return delegate.getVertexIncludePredicate();
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getVertexPaintFunction()
     */
    public VertexPaintFunction getVertexPaintFunction() {
        return delegate.getVertexPaintFunction();
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getVertexStringer()
     */
    public VertexStringer getVertexStringer() {
        return delegate.getVertexStringer();
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getVertexStrokeFunction()
     */
    public VertexStrokeFunction getVertexStrokeFunction() {
        return delegate.getVertexStrokeFunction();
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setRendererPane(javax.swing.CellRendererPane)
     */
    public void setRendererPane(CellRendererPane rendererPane) {
        delegate.setRendererPane(rendererPane);
    }

    /**
     * @return Returns the delegate.
     */
    public PluggableRenderer getDelegate() {
        return delegate;
    }


    /**
     * @param delegate The delegate to set.
     */
    public void setDelegate(PluggableRenderer delegate) {
        this.delegate = delegate;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getEdgeShapeFunction()
     */
    public EdgeShapeFunction getEdgeShapeFunction() {
        return delegate.getEdgeShapeFunction();
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getGraphLabelRenderer()
     */
    public GraphLabelRenderer getGraphLabelRenderer() {
        return delegate.getGraphLabelRenderer();
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.AbstractRenderer#getPickedKey()
     */
    public PickedInfo getPickedKey() {
        return delegate.getPickedKey();
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getVertexLabelCentering()
     */
    public boolean getVertexLabelCentering() {
        return delegate.getVertexLabelCentering();
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getVertexShapeFunction()
     */
    public VertexShapeFunction getVertexShapeFunction() {
        return delegate.getVertexShapeFunction();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return delegate.hashCode();
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#isPicked(edu.uci.ics.jung.graph.Edge)
     */
    public boolean isPicked(ArchetypeEdge e) {
        return delegate.isPicked(e);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#isPicked(edu.uci.ics.jung.graph.Vertex)
     */
    public boolean isPicked(ArchetypeVertex v) {
        return delegate.isPicked(v);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#paintEdge(java.awt.Graphics, edu.uci.ics.jung.graph.Edge, int, int, int, int)
     */
    public void paintEdge(Graphics g, Edge e, int x1, int y1, int x2, int y2) {
        delegate.paintEdge(g, e, x1, y1, x2, y2);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#paintVertex(java.awt.Graphics, edu.uci.ics.jung.graph.Vertex, int, int)
     */
    public void paintVertex(Graphics g, Vertex v, int x, int y) {
        delegate.paintVertex(g, v, x, y);
    }
    
    public void paintIconForVertex(Graphics g, Vertex v, int x, int y) {
    	    delegate.paintIconForVertex(g, v, x, y);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#prepareRenderer(edu.uci.ics.jung.visualization.GraphLabelRenderer, java.lang.Object, boolean, edu.uci.ics.jung.graph.Edge)
     */
    public Component prepareRenderer(GraphLabelRenderer renderer, Object value, boolean isSelected, Edge edge) {
        return delegate.prepareRenderer(renderer, value, isSelected, edge);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#prepareRenderer(edu.uci.ics.jung.visualization.GraphLabelRenderer, java.lang.Object, boolean, edu.uci.ics.jung.graph.Vertex)
     */
    public Component prepareRenderer(GraphLabelRenderer graphLabelRenderer, Object value, boolean isSelected, Vertex vertex) {
        return delegate.prepareRenderer(graphLabelRenderer, value, isSelected, vertex);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setArrowPlacementTolerance(float)
     */
    public void setArrowPlacementTolerance(float tolerance) {
        delegate.setArrowPlacementTolerance(tolerance);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setEdgeArrowFunction(edu.uci.ics.jung.graph.decorators.EdgeArrowFunction)
     */
    public void setEdgeArrowFunction(EdgeArrowFunction eaf) {
        delegate.setEdgeArrowFunction(eaf);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setEdgeArrowPredicate(org.apache.commons.collections.Predicate)
     */
    public void setEdgeArrowPredicate(Predicate p) {
        delegate.setEdgeArrowPredicate(p);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setEdgeColorFunction(edu.uci.ics.jung.graph.decorators.EdgeColorFunction)
     */
    public void setEdgeColorFunction(EdgeColorFunction ecf) {
        delegate.setEdgeColorFunction(ecf);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setEdgeFontFunction(edu.uci.ics.jung.graph.decorators.EdgeFontFunction)
     */
    public void setEdgeFontFunction(EdgeFontFunction eff) {
        delegate.setEdgeFontFunction(eff);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setEdgeIncludePredicate(org.apache.commons.collections.Predicate)
     */
    public void setEdgeIncludePredicate(Predicate p) {
        delegate.setEdgeIncludePredicate(p);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setEdgeLabelClosenessFunction(edu.uci.ics.jung.graph.decorators.NumberEdgeValue)
     */
    public void setEdgeLabelClosenessFunction(NumberEdgeValue nev) {
        delegate.setEdgeLabelClosenessFunction(nev);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setEdgePaintFunction(edu.uci.ics.jung.graph.decorators.EdgePaintFunction)
     */
    public void setEdgePaintFunction(EdgePaintFunction impl) {
        delegate.setEdgePaintFunction(impl);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setEdgeShapeFunction(edu.uci.ics.jung.graph.decorators.EdgeShapeFunction)
     */
    public void setEdgeShapeFunction(EdgeShapeFunction impl) {
        delegate.setEdgeShapeFunction(impl);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setEdgeStringer(edu.uci.ics.jung.graph.decorators.EdgeStringer)
     */
    public void setEdgeStringer(EdgeStringer es) {
        delegate.setEdgeStringer(es);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setEdgeStrokeFunction(edu.uci.ics.jung.graph.decorators.EdgeStrokeFunction)
     */
    public void setEdgeStrokeFunction(EdgeStrokeFunction esf) {
        delegate.setEdgeStrokeFunction(esf);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setGraphLabelRenderer(edu.uci.ics.jung.visualization.GraphLabelRenderer)
     */
    public void setGraphLabelRenderer(GraphLabelRenderer graphLabelRenderer) {
        delegate.setGraphLabelRenderer(graphLabelRenderer);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.AbstractRenderer#setPickedKey(edu.uci.ics.jung.visualization.PickedInfo)
     */
    public void setPickedKey(PickedInfo pk) {
        delegate.setPickedKey(pk);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setScreenDevice(javax.swing.JComponent)
     */
    public void setScreenDevice(JComponent screenDevice) {
        delegate.setScreenDevice(screenDevice);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setVertexColorFunction(edu.uci.ics.jung.graph.decorators.VertexColorFunction)
     */
    public void setVertexColorFunction(VertexColorFunction vcf) {
        delegate.setVertexColorFunction(vcf);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setVertexFontFunction(edu.uci.ics.jung.graph.decorators.VertexFontFunction)
     */
    public void setVertexFontFunction(VertexFontFunction vff) {
        delegate.setVertexFontFunction(vff);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setVertexIncludePredicate(org.apache.commons.collections.Predicate)
     */
    public void setVertexIncludePredicate(Predicate p) {
        delegate.setVertexIncludePredicate(p);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setVertexLabelCentering(boolean)
     */
    public void setVertexLabelCentering(boolean b) {
        delegate.setVertexLabelCentering(b);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setVertexPaintFunction(edu.uci.ics.jung.graph.decorators.VertexPaintFunction)
     */
    public void setVertexPaintFunction(VertexPaintFunction vpf) {
        delegate.setVertexPaintFunction(vpf);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setVertexShapeFunction(edu.uci.ics.jung.graph.decorators.VertexShapeFunction)
     */
    public void setVertexShapeFunction(VertexShapeFunction vsf) {
        delegate.setVertexShapeFunction(vsf);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setVertexStringer(edu.uci.ics.jung.graph.decorators.VertexStringer)
     */
    public void setVertexStringer(VertexStringer vs) {
        delegate.setVertexStringer(vs);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setVertexStrokeFunction(edu.uci.ics.jung.graph.decorators.VertexStrokeFunction)
     */
    public void setVertexStrokeFunction(VertexStrokeFunction vsf) {
        delegate.setVertexStrokeFunction(vsf);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return delegate.toString();
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getVertexImager()
     */
    public VertexIconFunction getVertexIconFunction() {
        return delegate.getVertexIconFunction();
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#setVertexImager(edu.uci.ics.jung.graph.decorators.VertexIconFunction)
     */
    public void setVertexIconFunction(VertexIconFunction vertexImager) {
        delegate.setVertexIconFunction(vertexImager);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getArrowTransform(java.awt.geom.GeneralPath, java.awt.Shape)
     */
    public AffineTransform getArrowTransform(GeneralPath edgeShape, Shape vertexShape) {
        return delegate.getArrowTransform(edgeShape, vertexShape);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getReverseArrowTransform(java.awt.geom.GeneralPath, java.awt.Shape)
     */
    public AffineTransform getReverseArrowTransform(GeneralPath edgeShape, Shape vertexShape) {
        return delegate.getReverseArrowTransform(edgeShape, vertexShape);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#paintShapeForVertex(java.awt.Graphics2D, edu.uci.ics.jung.graph.Vertex, java.awt.Shape)
     */
    public void paintShapeForVertex(Graphics2D g2d, Vertex v, Shape shape) {
        delegate.paintShapeForVertex(g2d, v, shape);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getArrowTransform(java.awt.geom.Line2D, java.awt.Shape)
     */
    public AffineTransform getArrowTransform(Line2D edgeShape, Shape vertexShape) {
        return delegate.getArrowTransform(edgeShape, vertexShape);
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.PluggableRenderer#getReverseArrowTransform(java.awt.geom.GeneralPath, java.awt.Shape, boolean)
     */
    public AffineTransform getReverseArrowTransform(GeneralPath edgeShape, Shape vertexShape, boolean passedGo) {
        return delegate.getReverseArrowTransform(edgeShape, vertexShape, passedGo);
    }

    public ParallelEdgeIndexFunction getParallelEdgeIndexFunction() {
        return delegate.getParallelEdgeIndexFunction();
    }

    public void setParallelEdgeIndexFunction(ParallelEdgeIndexFunction parallelEdgeIndexFunction) {
        delegate.setParallelEdgeIndexFunction(parallelEdgeIndexFunction);
    }

    public void setViewTransformer(MutableTransformer viewTransformer) {
        delegate.setViewTransformer(viewTransformer);
    }
}
