/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 *
 * Created on Aug 23, 2005
 */

package edu.uci.ics.jung.visualization;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Set;

import javax.swing.event.ChangeListener;

import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.ChangeEventSupport;
import edu.uci.ics.jung.utils.DefaultChangeEventSupport;

/**
 * a pure decorator for the Layout interface. Intended to be overridden
 * to provide specific behavior decoration
 * @see PersistentLayoutImpl
 * @author Tom Nelson - RABA Technologies
 *
 *
 */
public abstract class LayoutDecorator implements Layout, ChangeEventSupport {
    
    protected Layout delegate;
    protected ChangeEventSupport changeSupport =
        new DefaultChangeEventSupport(this);

    public LayoutDecorator(Layout delegate) {
        this.delegate = delegate;
    }

    /**
     * getter for the delegate
     * @return the delegate
     */
    public Layout getDelegate() {
        return delegate;
    }

    /**
     * setter for the delegate
     * @param delegate the new delegate
     */
    public void setDelegate(Layout delegate) {
        this.delegate = delegate;
    }

    /**
     * @see edu.uci.ics.jung.visualization.Layout#advancePositions()
     */
    public void advancePositions() {
        delegate.advancePositions();
    }

    /**
     * @see edu.uci.ics.jung.visualization.Layout#applyFilter(edu.uci.ics.jung.graph.Graph)
     */
    public void applyFilter(Graph subgraph) {
        delegate.applyFilter(subgraph);
    }

    /**
     * @see edu.uci.ics.jung.visualization.Layout#forceMove(edu.uci.ics.jung.graph.Vertex, double, double)
     */
    public void forceMove(Vertex picked, double x, double y) {
        delegate.forceMove(picked, x, y);
    }

    /**
     * @see edu.uci.ics.jung.visualization.Layout#getCurrentSize()
     */
    public Dimension getCurrentSize() {
        return delegate.getCurrentSize();
    }

    /**
     * @see edu.uci.ics.jung.visualization.Layout#getGraph()
     */
    public Graph getGraph() {
        return delegate.getGraph();
    }

    /**
     * @see edu.uci.ics.jung.visualization.Layout#getLocation(edu.uci.ics.jung.graph.ArchetypeVertex)
     */
    public Point2D getLocation(ArchetypeVertex v) {
        return delegate.getLocation(v);
    }

    /**
     * @see edu.uci.ics.jung.visualization.Layout#getStatus()
     */
    public String getStatus() {
        return delegate.getStatus();
    }

    /**
     * @see edu.uci.ics.jung.visualization.Layout#getVertex(double, double, double)
     */
    public Vertex getVertex(double x, double y, double maxDistance) {
        return delegate.getVertex(x, y, maxDistance);
    }

    /**
     * @see edu.uci.ics.jung.visualization.Layout#getVertex(double, double)
     */
    public Vertex getVertex(double x, double y) {
        return delegate.getVertex(x, y);
    }

    /**
     * @see edu.uci.ics.jung.visualization.VertexLocationFunction#getVertexIterator()
     */
    public Iterator getVertexIterator() {
        return delegate.getVertexIterator();
    }

    /**
     * @see edu.uci.ics.jung.visualization.Layout#getVisibleEdges()
     */
    public Set getVisibleEdges() {
        return delegate.getVisibleEdges();
    }

    /**
     * @see edu.uci.ics.jung.visualization.Layout#getVisibleVertices()
     */
    public Set getVisibleVertices() {
        return delegate.getVisibleVertices();
    }

    /**
     * @see edu.uci.ics.jung.visualization.Layout#getX(edu.uci.ics.jung.graph.Vertex)
     */
    public double getX(Vertex v) {
        return delegate.getX(v);
    }

    /**
     * @see edu.uci.ics.jung.visualization.Layout#getY(edu.uci.ics.jung.graph.Vertex)
     */
    public double getY(Vertex v) {
        return delegate.getY(v);
    }

    /**
     * @see edu.uci.ics.jung.visualization.Layout#incrementsAreDone()
     */
    public boolean incrementsAreDone() {
        return delegate.incrementsAreDone();
    }

    /**
     * @see edu.uci.ics.jung.visualization.Layout#initialize(java.awt.Dimension)
     */
    public void initialize(Dimension currentSize) {
        delegate.initialize(currentSize);
    }

    /**
     * @see edu.uci.ics.jung.visualization.Layout#isIncremental()
     */
    public boolean isIncremental() {
        return delegate.isIncremental();
    }

    /**
     * @see edu.uci.ics.jung.visualization.Layout#lockVertex(edu.uci.ics.jung.graph.Vertex)
     */
    public void lockVertex(Vertex v) {
        delegate.lockVertex(v);
    }

    /**
     * @see edu.uci.ics.jung.visualization.Layout#isLocked(Vertex)
     */
    public boolean isLocked(Vertex v)
    {
        return delegate.isLocked(v);
    }
    
    /**
     * @see edu.uci.ics.jung.visualization.Layout#resize(java.awt.Dimension)
     */
    public void resize(Dimension d) {
        delegate.resize(d);
    }

    /**
     * @see edu.uci.ics.jung.visualization.Layout#restart()
     */
    public void restart() {
        delegate.restart();
    }

    /**
     * @see edu.uci.ics.jung.visualization.Layout#unlockVertex(edu.uci.ics.jung.graph.Vertex)
     */
    public void unlockVertex(Vertex v) {
        delegate.unlockVertex(v);
    }

    public void addChangeListener(ChangeListener l) {
        changeSupport.addChangeListener(l);
    }

    public void removeChangeListener(ChangeListener l) {
        changeSupport.removeChangeListener(l);
    }

    public ChangeListener[] getChangeListeners() {
        return changeSupport.getChangeListeners();
    }

    public void fireStateChanged() {
        changeSupport.fireStateChanged();
    }
}
