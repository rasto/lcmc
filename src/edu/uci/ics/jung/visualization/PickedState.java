/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 *
 * Created on Apr 2, 2005
 */
package edu.uci.ics.jung.visualization;

import java.awt.ItemSelectable;
import java.util.Set;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeVertex;

/**
 * An interface for classes that keep track of the "picked" state
 * of edges and vertices.
 * 
 * @author Tom Nelson
 * @author Joshua O'Madadhain
 */
public interface PickedState extends PickedInfo, ItemSelectable
{
    /**
     * Marks <code>v</code> as "picked" if <code>b == true</code>,
     * and unmarks <code>v</code> as picked if <code>b == false</code>.
     * @return the "picked" state of <code>v</code> prior to this call
     */
    boolean pick(ArchetypeVertex v, boolean b);
    
    /**
     * Marks <code>e</code> as "picked" if <code>b == true</code>,
     * and unmarks <code>e</code> as picked if <code>b == false</code>.
     * @return the "picked" state of <code>e</code> prior to this call
     */
    boolean pick(ArchetypeEdge e, boolean b);
    
    /**
     * Clears the "picked" state from all vertices.
     */
    void clearPickedVertices();
    
    /**
     * Returns all "picked" vertices.
     */
    Set getPickedVertices();
    
    /** 
     * Returns <code>true</code> if <code>v</code> is currently "picked".
     */
    boolean isPicked(ArchetypeVertex v);
    
    /**
     * Clears the "picked" state from all edges.
     */
    void clearPickedEdges();

    /**
     * Returns all "picked" edges.
     */
    Set getPickedEdges();
    
    /** 
     * Returns <code>true</code> if <code>e</code> is currently "picked".
     */
    boolean isPicked(ArchetypeEdge e);
    
    /**
     * Adds a listener to this instance.
     * @deprecated Use addItemListener
     * @param pel
     */
    void addListener(PickEventListener pel);
    
    /**
     * Removes a listener from this instance.
     * @deprecated Use removeItemListener
     * @param pel
     */
    void removeListener(PickEventListener pel);
}