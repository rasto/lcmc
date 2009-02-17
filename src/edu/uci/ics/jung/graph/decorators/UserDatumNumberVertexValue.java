/*
 * Created on Nov 7, 2004
 *
 * Copyright (c) 2004, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.graph.decorators;

import java.util.Iterator;

import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.utils.UserData;
import edu.uci.ics.jung.utils.UserDataContainer.CopyAction;

/**
 * 
 * @author Joshua O'Madadhain
 */
public class UserDatumNumberVertexValue implements NumberVertexValue
{
    protected Object key;
    protected CopyAction copy_action;
    
    /**
     * Creates an instance with the specified key and with a
     * <code>CopyAction</code> of <code>REMOVE</code>.
     */
    public UserDatumNumberVertexValue(Object key)
    {
        this.key = key;
        this.copy_action = UserData.REMOVE;
    }
    
    /**
     * Creates an instance with the specified key and <code>CopyAction</code>.
     */
    public UserDatumNumberVertexValue(Object key, CopyAction copy_action)
    {
        this.key = key;
        this.copy_action = copy_action;
    }
    
    public void setCopyAction(CopyAction copy_action)
    {
        this.copy_action = copy_action;
    }

    /**
     * @see edu.uci.ics.jung.graph.decorators.NumberVertexValue#getNumber(edu.uci.ics.jung.graph.ArchetypeVertex)
     */
    public Number getNumber(ArchetypeVertex v)
    {
        return (Number)v.getUserDatum(key);
    }

    /**
     * @see edu.uci.ics.jung.graph.decorators.NumberVertexValue#setNumber(edu.uci.ics.jung.graph.ArchetypeVertex, java.lang.Number)
     */
    public void setNumber(ArchetypeVertex v, Number n)
    {
        v.setUserDatum(key, n, copy_action);
    }

    /**
     * Removes this decoration from <code>g</code>.
     */
    public void clear(ArchetypeGraph g)
    {
        for (Iterator iter = g.getVertices().iterator(); iter.hasNext(); )
        {
            ArchetypeVertex v = (ArchetypeVertex)iter.next();
            v.removeUserDatum(key);
        }
    }
}
