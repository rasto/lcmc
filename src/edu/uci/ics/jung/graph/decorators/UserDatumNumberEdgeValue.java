/*
 * Copyright (c) 2004, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 * Created on Sep 10, 2004
 */
package edu.uci.ics.jung.graph.decorators;

import java.util.Iterator;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.utils.UserData;
import edu.uci.ics.jung.utils.UserDataContainer.CopyAction;

/**
 * An implementation of <code>NumberEdgeValue</code> that stores the values
 * in the UserData repository.
 *  
 * @author Joshua O'Madadhain
 */
public class UserDatumNumberEdgeValue implements NumberEdgeValue
{
    protected Object key;
    protected CopyAction copy_action;
    
    /**
     * Creates an instance with the specified key and with a
     * <code>CopyAction</code> of <code>REMOVE</code>.
     */
    public UserDatumNumberEdgeValue(Object key)
    {
        this.key = key;
        this.copy_action = UserData.REMOVE;
    }
    
    /**
     * Creates an instance with the specified key and <code>CopyAction</code>.
     */
    public UserDatumNumberEdgeValue(Object key, CopyAction copy_action)
    {
        this.key = key;
        this.copy_action = copy_action;
    }
    
    public void setCopyAction(CopyAction copy_action)
    {
        this.copy_action = copy_action;
    }
    
    /**
     * @see edu.uci.ics.jung.graph.decorators.NumberEdgeValue#getNumber(edu.uci.ics.jung.graph.ArchetypeEdge)
     */
    public Number getNumber(ArchetypeEdge e)
    {
        return (Number)e.getUserDatum(key);
    }

    /**
     * @see edu.uci.ics.jung.graph.decorators.NumberEdgeValue#setNumber(edu.uci.ics.jung.graph.ArchetypeEdge, java.lang.Number)
     */
    public void setNumber(ArchetypeEdge e, Number n)
    {
        e.setUserDatum(key, n, copy_action);
    }
    
    /**
     * Removes this decoration from <code>g</code>.
     */
    public void clear(ArchetypeGraph g)
    {
        for (Iterator iter = g.getEdges().iterator(); iter.hasNext(); )
        {
            ArchetypeEdge v = (ArchetypeEdge)iter.next();
            v.removeUserDatum(key);
        }
    }

}
