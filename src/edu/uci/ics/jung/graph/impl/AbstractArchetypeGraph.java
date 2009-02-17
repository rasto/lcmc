/*
 * Created on Mar 22, 2004
 * 
 * Copyright (c) 2004, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.graph.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.Predicate;

import edu.uci.ics.jung.exceptions.ConstraintViolationException;
import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.event.GraphEventListener;
import edu.uci.ics.jung.graph.event.GraphEventType;
import edu.uci.ics.jung.graph.event.GraphListenerHandler;
import edu.uci.ics.jung.graph.predicates.EdgePredicate;
import edu.uci.ics.jung.graph.predicates.GPredicate;
import edu.uci.ics.jung.graph.predicates.NotInGraphEdgePredicate;
import edu.uci.ics.jung.graph.predicates.NotInGraphVertexPredicate;
import edu.uci.ics.jung.graph.predicates.VertexPredicate;
import edu.uci.ics.jung.utils.UserDataDelegate;

/**
 * @author Joshua O'Madadhain
 */
public abstract class AbstractArchetypeGraph extends UserDataDelegate
        implements ArchetypeGraph, Cloneable {

    /**
     * GraphEventType -> Graph Listener list table
     */
    protected GraphListenerHandler mGraphListenerHandler;

    /**
     * ID -> Vertex lookup table.
     */
    protected Map mVertexIDs;

    /**
     * ID -> Edge lookup table.
     */
    protected Map mEdgeIDs;
    
//    /**
//     * A Collection for user-specified vertex constraints.  
//     */
//    protected Requirements user_vertex_requirements;
//
//    /**
//     * A Collection for user-specified edge constraints.  
//     */
//    protected Requirements user_edge_requirements;
//
//    /**
//     * A Collection for system-specified vertex constraints.
//     * May not be modified by the user; generally speaking,
//     * should be set at initialization.
//     */
//    protected Collection system_vertex_requirements;
//    
//    /**
//     * A Collection for system-specified edge constraints.
//     * May not be modified by the user; generally speaking,
//     * should be set at initialization.
//     */
//    protected Collection system_edge_requirements;

    protected Requirements edge_requirements;
    protected Requirements vertex_requirements;
    
    public AbstractArchetypeGraph() {
//        initialize();
    }

    /**
     * Initializes all of the graph's internal data structures. Should always be
     * called by any mechanism that creates a new instance of
     * <code>AbstractArchetypeGraph</code> (for example, constructors and implementations of
     * <code>newInstance()</code>).  Predicates, if added in this method, 
     * that should not be copied to other graphs
     * should implement <code>UncopyablePredicate</code>.
     * 
     * <P>Note: this method is not a substitute for
     * <code>removeAllVertices()</code>, as it will not notify the vertices
     * and edges that they have been removed from the graph.</p>
     */
    /**
     * Initializes all of the graph's internal data structures. Should always be
     * called by any mechanism that creates a new instance of
     * AbstractSparseGraph (for example, constructors and implementations of
     * newInstance()). If you add predicates here, please set their
     * isInitializeFlag to TRUE.
     * <P>
     * Note: this method is not a substitute for
     * <code>removeAllVertices()</code>, as it will not notify the vertices
     * and edges that they have been removed from the graph.
     */
    protected void initialize() {
        mGraphListenerHandler = new GraphListenerHandler(this);
        mVertexIDs = new HashMap();
        mEdgeIDs = new HashMap();
//        user_edge_requirements = new Requirements();
//        user_vertex_requirements = new Requirements();
//        system_edge_requirements = new Requirements();
//        system_vertex_requirements = new Requirements();
        edge_requirements = new Requirements();
        vertex_requirements = new Requirements();
        EdgePredicate ep = new NotInGraphEdgePredicate(this);
        edge_requirements.add( ep );
//        system_edge_requirements.add(ep);
        ep.isInitializationPredicate = true;
        VertexPredicate vp = new NotInGraphVertexPredicate(this);
        vertex_requirements.add( vp );
//        system_vertex_requirements.add(vp);
        vp.isInitializationPredicate = true;
    }

//    protected void finalize() throws Throwable
//    {
//        for (Iterator iter = getUserDatumKeyIterator(); iter.hasNext(); )
//        {
//            removeUserDatum(iter.next());
//        }
//        super.finalize();
//    }
    
    /**
     * Creates a new empty graph of the same type as this graph, by cloning this
     * graph and then clearing the extraneous fields.
     * 
     * @see edu.uci.ics.jung.graph.ArchetypeGraph#newInstance()
     */
    public ArchetypeGraph newInstance() {
        try {
            AbstractArchetypeGraph aag = (AbstractArchetypeGraph) this.clone();
            aag.initialize();
//            addAllCopyable(aag.system_edge_requirements, system_edge_requirements);
//            addAllCopyable(aag.system_vertex_requirements, system_vertex_requirements);
//            addAllCopyable(aag.user_edge_requirements, user_edge_requirements);
//            addAllCopyable(aag.user_vertex_requirements, user_vertex_requirements);
//            aag.user_edge_requirements.addAll(getEdgeConstraints());
//            aag.user_vertex_requirements.addAll(getVertexConstraints());
            addAllNotInitializers(aag.getEdgeConstraints(),
                    getEdgeConstraints());
            addAllNotInitializers(aag.getVertexConstraints(),
                    getVertexConstraints());
            return aag;
        } catch (CloneNotSupportedException e) {
            throw new FatalException("Failed attempt to clone graph", e);
        }
    }

//    /**
//     * Adds all the predicates in source to the list in target, except those
//     * that are instances of UncopyablePredicate.
//     * 
//     * @param targetPredicates
//     * @param sourcePredicates
//     */
//    protected void addAllCopyable(Collection targetPredicates, Collection sourcePredicates) 
//    {
//        for (Iterator iter = sourcePredicates.iterator(); iter.hasNext();) 
//        {
//            Predicate p = (Predicate) iter.next();
//            if (! (p instanceof UncopyablePredicate)) 
//                targetPredicates.add(p);
//        }
//    }

    /**
     * Adds all the predicates in source to the list in target, except those
     * that answer to isInitializationPredicate.
     * 
     * @param targetPredicates
     * @param sourcePredicates
     */
    protected void addAllNotInitializers(Collection targetPredicates,
            Collection sourcePredicates) {
        for (Iterator iter = sourcePredicates.iterator(); iter.hasNext();) {
            Predicate p = (Predicate) iter.next();
            if (p instanceof GPredicate) {
                GPredicate gp = (GPredicate) p;
                if (gp.isInitializationPredicate) continue;
            }
            targetPredicates.add(p);
        }
    }

    
    /**
     * Returns the vertex associated with the specified ID, or null if there is
     * no such vertex. Not intended for user access.
     */
    ArchetypeVertex getVertexByID(int id) {
        return (ArchetypeVertex) mVertexIDs.get(new Integer(id));
    }

    /**
     * Returns the vertex associated with the specified ID, or null if there is
     * no such vertex. Not intended for user access.
     */
    ArchetypeEdge getEdgeByID(int id) {
        return (ArchetypeEdge) mEdgeIDs.get(new Integer(id));
    }
    
    /**
     * Returns a human-readable representation of this graph.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "G" + hashCode() + getVertices();
    }

    /**
     * @see ArchetypeGraph#numVertices()
     */
    public int numVertices() {
        return getVertices().size();
    }

    /**
     * @see ArchetypeGraph#numEdges()
     */
    public int numEdges() {
        return getEdges().size();
    }

    /**
     * @see edu.uci.ics.jung.graph.ArchetypeGraph#getVertexConstraints()
     */
    public Collection getVertexConstraints() {
//        return user_vertex_requirements;
        return vertex_requirements;
    }

    /**
     * @see edu.uci.ics.jung.graph.ArchetypeGraph#getEdgeConstraints()
     */
    public Collection getEdgeConstraints() {
//        return user_edge_requirements;
        return edge_requirements;
    }

    /**
     * @see edu.uci.ics.jung.graph.ArchetypeGraph#addListener(edu.uci.ics.jung.graph.event.GraphEventListener,
     *      edu.uci.ics.jung.graph.event.GraphEventType)
     */
    public void addListener(GraphEventListener gel, GraphEventType get) {
        mGraphListenerHandler.addListener(gel, get);
    }

    /**
     * @see edu.uci.ics.jung.graph.ArchetypeGraph#removeListener(edu.uci.ics.jung.graph.event.GraphEventListener,
     *      edu.uci.ics.jung.graph.event.GraphEventType)
     */
    public void removeListener(GraphEventListener gel, GraphEventType get) {
        mGraphListenerHandler.removeListener(gel, get);
    }

    protected boolean listenersExist(GraphEventType type) {
        return mGraphListenerHandler.listenersExist(type);
    }

    /**
     * Creates a replica of this graph. Creates a new instance, then copies all
     * vertices, then all edges, and finally all user data.
     * 
     * @see ArchetypeGraph#copy()
     * @see AbstractSparseEdge#copy(ArchetypeGraph)
     * @see AbstractSparseVertex#copy(ArchetypeGraph)
     */
    public ArchetypeGraph copy() {
        ArchetypeGraph c = newInstance();
        for (Iterator iter = getVertices().iterator(); iter.hasNext();) {
            ArchetypeVertex av = (ArchetypeVertex) iter.next();
            av.copy(c);
        }
        for (Iterator iter = getEdges().iterator(); iter.hasNext();) {
            ArchetypeEdge ae = (ArchetypeEdge) iter.next();
            ae.copy(c);
        }
        c.importUserData(this);
        return c;
    }

    protected void checkConstraints(Object o, Collection c) {
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            Predicate p = (Predicate) iter.next();
            if (!p.evaluate(o)) 
            { 
                throw new ConstraintViolationException("Predicate " + 
                        p.getClass().getName() + " rejected " + o + ": " + p, p);
            }
        }
    }


    /**
     * Removes all vertices (and, therefore, all edges) from this graph.
     * Syntactic sugar for a loop that calls <code>removeVertex</code> on all
     * vertices of this graph.
     * 
     * @see ArchetypeGraph#removeAllVertices()
     */
    public void removeAllVertices() {
        removeVertices(getVertices());
    }

    /**
     * Removes all edges from this graph. Syntactic sugar for a loop that calls
     * <code>removeEdge</code> on all edges of this graph.
     * 
     * @see ArchetypeGraph#removeAllEdges()
     */
    public void removeAllEdges() {
        removeEdges(getEdges());
    }
    
//    /**
//     * Checks see whether <code>e</code> passes both user and system constraints. 
//     */
//    protected void validateEdge(ArchetypeEdge e)
//    {
//        validate(e, user_edge_requirements);
//        validate(e, system_edge_requirements);
//    }
//
//    /**
//     * Checks see whether <code>v</code> passes both user and system constraints. 
//     */
//    protected void validateVertex(ArchetypeVertex v)
//    {
//        validate(v, user_vertex_requirements);
//        validate(v, system_vertex_requirements);
//    }
    
    protected class Requirements extends LinkedList {

        public boolean add(Object o) 
        {
            Set edges = getEdges();
            Set vertices = getVertices();
            if (!this.contains(o)) 
            {
                if (!(edges == null || edges.isEmpty()) || !(vertices == null || vertices.isEmpty()))
                        throw new IllegalArgumentException("Cannot add "
                                + "requirements to a non-empty graph");
                super.add((Predicate) o);
                return true;
            } 
            else
                return false; // no duplicates allowed
        }

        public boolean evaluate(Object o) {
            for (Iterator iter = iterator(); iter.hasNext();) {
                Predicate p = (Predicate) iter.next();
                if (!p.evaluate(o)) return false;
            }
            return true;
        }
    }
}