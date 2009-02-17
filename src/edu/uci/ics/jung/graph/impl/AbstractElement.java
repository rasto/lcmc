/*
 * Created on Apr 26, 2005
 *
 * Copyright (c) 2005, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.graph.impl;

import java.lang.ref.WeakReference;
import java.util.Map;

import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.Element;
import edu.uci.ics.jung.utils.GeneralUtils;
import edu.uci.ics.jung.utils.UserDataDelegate;

/**
 * 
 * @author Joshua O'Madadhain
 */
public abstract class AbstractElement extends UserDataDelegate
//extends UnifiedUserData // Delegate 
    implements Element, Cloneable
{
    /**
     * The graph of which this vertex is an element.
     */
    protected WeakReference m_Graph;

    /**
     * Used to define vertex equivalence.
     */
    protected int id = -1;
    
    /**
     * @see Element#getGraph()
     */
    public ArchetypeGraph getGraph() 
    {
        if(  m_Graph == null ) 
            return null;
        
        ArchetypeGraph g = (ArchetypeGraph) m_Graph.get(); 
        return g;
    }

    /**
     * Attaches this vertex to the specified graph <code>g</code>.
     */
    protected void addGraph_internal(AbstractArchetypeGraph g) {
        if (m_Graph == null )
        {
//            List l = getRepositoryData();
            this.m_Graph = new WeakReference(g) ;
//            updateRepository(l);
        } else {
            throw new FatalException("Internal error: element " + this +
                " is already part of graph " + this.getGraph());
        }
    }

//    private List getRepositoryData()
//    {
//        List list = new LinkedList();
//        for (Iterator iter = this.getUserDatumKeyIterator(); iter.hasNext(); )
//        {
//            Object key = iter.next();
//            Object value = this.getUserDatum(key);
//            CopyAction copyact = this.getUserDatumCopyAction(key);
//            list.add(new Object[]{key, value, copyact});
//            this.removeUserDatum(key);
//        }
//        return list;
//    }
//    
//    private void updateRepository(List l)
//    {
//        // re-insert all of the user data in l into repository; 
//        // for some types of repository this is necessary to update
//        // element's "in-graph" status
//        for (Iterator iter = l.iterator(); iter.hasNext(); )
//        {
//            Object[] kvc = (Object[])iter.next();
//            this.addUserDatum(kvc[0], kvc[1], (CopyAction)kvc[2]);
//        }
//    }
    
    /**
     * Cleans up internal data structures after this
     * element is removed from a graph.
     */
    protected void removeGraph_internal() 
    {
//        List l = getRepositoryData();
        this.m_Graph = null;
//        updateRepository(l);
    }
    
    /**
     * Returns the ID of this element.  This method is not intended
     * for general user access.
     */
    int getID()
    {
        return this.id;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return GeneralUtils.hash(this.id);
    }

    /**
     * Adds (<code>getID()</code>,<code>this</code>) to the specified
     * Map. Checks to determine whether the map already contains an element with
     * such an index, and throws an IllegalArgumentException if it does. This is
     * used to test for the presence of an equivalent vertex/edge in a graph;
     * it's not subsumed by the "NotInGraph*Predicate" check contains() check
     * done in the validation step, because contains depends on equals() ->
     * getEquivalent{Edge,Vertex}() -> getGraph()...which returns null because
     * addGraph_internal has not yet been called. So this really is the only way
     * we can tell, at this point in the appropriate add() method, whether
     * there's an equivalent vertex/edge.
     */
    void checkIDs(Map ids)
    {
        Integer newIndex = new Integer(getID());
        if (ids.containsKey(newIndex))
            throw new IllegalArgumentException(
                    "An equivalent element already exists in this graph");
        ids.put(newIndex, this);
    }

    /**
     * Initializes all the data structures for this element.
     * (This is used on cloned elements, since
     * <code>clone()</code> copies some information that should
     * not be in the new element.)
     */
    protected void initialize()
    {
        m_Graph = null;
    }
    
}
