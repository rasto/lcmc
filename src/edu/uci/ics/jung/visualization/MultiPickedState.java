/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 *
 * Created on Mar 28, 2005
 */
package edu.uci.ics.jung.visualization;

import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;

/**
 * Maintains the state of what has been 'picked' in the graph.
 * The <code>Sets</code> are constructed so that their iterators
 * will traverse them in the order in which they are picked.
 * 
 * @author Tom Nelson - RABA Technologies
 * @author Joshua O'Madadhain
 * 
 */
public class MultiPickedState extends AbstractPickedState implements PickedState
{
    /**
     * the 'picked' vertices
     */
    protected Set pickedVertices = new LinkedHashSet();
    
    /**
     * the 'picked' edges
     */
    protected Set pickedEdges = new LinkedHashSet();
    
    /**
     * @see PickedState#pick(ArchetypeVertex, boolean)
     */
    public boolean pick(ArchetypeVertex v, boolean picked)
    {
        boolean prior_state = pickedVertices.contains(v);
        if (picked) {
            pickedVertices.add(v);
            if(prior_state == false) {
                firePickEvent(v, true);
                fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED,
                        v, ItemEvent.SELECTED));
            }

        } else {
            pickedVertices.remove(v);
            if(prior_state == true) {
                firePickEvent(v, false);
                fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED,
                    v, ItemEvent.DESELECTED));
            }

        }
        return prior_state;
    }

    /**
     * @see PickedState#pick(ArchetypeVertex, boolean)
     */
    public boolean pick(ArchetypeEdge e, boolean picked)
    {
        boolean prior_state = pickedEdges.contains(e);
        if (picked) {
            pickedEdges.add(e);
            if(prior_state == false) {
                firePickEvent(e, true);
                fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED,
                        e, ItemEvent.SELECTED));
            }
        } else {
            pickedEdges.remove(e);
            if(prior_state == true) {
                firePickEvent(e, false);
                fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED,
                    e, ItemEvent.DESELECTED));
            }
        }
        return prior_state;
    }

    /**
     * @see edu.uci.ics.jung.visualization.PickedState#clearPickedVertices()
     */
    public void clearPickedVertices() 
    {
        Collection iterable_set = new LinkedList(pickedVertices);
        for (Iterator it = iterable_set.iterator(); it.hasNext(); )
            pick((ArchetypeVertex)it.next(), false);
    }


    /**
     * @see edu.uci.ics.jung.visualization.PickedState#clearPickedEdges()
     */
    public void clearPickedEdges() 
    {
        Collection iterable_set = new LinkedList(pickedEdges);
        for (Iterator it = iterable_set.iterator(); it.hasNext(); )
            pick((ArchetypeEdge)it.next(), false);
    }
    
    /**
     * @see edu.uci.ics.jung.visualization.PickedState#getPickedEdges()
     */
    public Set getPickedEdges() {
        return Collections.unmodifiableSet(pickedEdges);
    }
    
    /**
     * @see edu.uci.ics.jung.visualization.PickedState#isPicked(ArchetypeEdge)
     */
    public boolean isPicked(ArchetypeEdge e) {
        return pickedEdges.contains(e);
    }

    /**
     * @see edu.uci.ics.jung.visualization.PickedState#getPickedVertices()
     */
    public Set getPickedVertices() {
        return Collections.unmodifiableSet(pickedVertices);
    }
    
    /**
     * @see edu.uci.ics.jung.visualization.PickedState#isPicked(ArchetypeVertex)
     */
    public boolean isPicked(ArchetypeVertex v) 
    {
        return pickedVertices.contains(v);
    }
    
    /**
     * @deprecated Use addItemListener
     * @see edu.uci.ics.jung.visualization.PickedState#addListener(edu.uci.ics.jung.visualization.PickEventListener)
     */
    public void addListener(PickEventListener pel)
    {
        listenerList.add(PickEventListener.class, pel);
    }

    /**
     * @deprecated Use removeItemListener
     * @see edu.uci.ics.jung.visualization.PickedState#removeListener(edu.uci.ics.jung.visualization.PickEventListener)
     */
    public void removeListener(PickEventListener pel)
    {
        listenerList.remove(PickEventListener.class, pel);
    }

    /**
     * @see #isPicked(ArchetypeVertex)
     */
    public boolean isPicked(Vertex v)
    {
        return pickedVertices.contains(v);
    }

    /**
     * @see #isPicked(ArchetypeEdge)
     */
    public boolean isPicked(Edge e)
    {
        return pickedEdges.contains(e);
    }

    /**
     * for the ItemSelectable interface contract
     */
    public Object[] getSelectedObjects() {
        List list = new ArrayList(pickedVertices);
        list.addAll(pickedEdges);
        return list.toArray();
    }
    
    protected void firePickEvent(Object object, boolean picked) {
        Object[] listeners = listenerList.getListenerList();
        for ( int i = listeners.length-2; i>=0; i-=2 ) {
            if ( listeners[i]==PickEventListener.class ) {
                if(object instanceof Vertex) {
                    if(picked) {
                        ((PickEventListener)listeners[i+1]).vertexPicked((Vertex)object);
                    } else {
                        ((PickEventListener)listeners[i+1]).vertexUnpicked((Vertex)object);
                    }
                } else {
                    if(picked) {
                        ((PickEventListener)listeners[i+1]).edgePicked((Edge)object);
                    } else {
                        ((PickEventListener)listeners[i+1]).edgeUnpicked((Edge)object);
                    }
                    
                }
            }
        }
    }
}
