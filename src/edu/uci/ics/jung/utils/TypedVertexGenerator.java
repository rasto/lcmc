/*
 * Created on Apr 3, 2004
 *
 * Copyright (c) 2004, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.utils;

import java.util.Collection;

import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.DirectedSparseVertex;
import edu.uci.ics.jung.graph.impl.SimpleDirectedSparseVertex;
import edu.uci.ics.jung.graph.impl.SimpleSparseVertex;
import edu.uci.ics.jung.graph.impl.SimpleUndirectedSparseVertex;
import edu.uci.ics.jung.graph.impl.SparseVertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;


/**
 * Generates vertices according to the edge requirements  
 * submitted to the constructor.  This implementation 
 * respects edge direction (directed, undirected, mixed)
 * as well as edge multiplicity (parallel edges). See the
 * constructor for a list of the vertex types.
 * 
 * @author Joshua O'Madadhain
 */
public class TypedVertexGenerator implements VertexGenerator
{
    protected Object type;

    /**
     * if true, generated vertices won't support parallel edges
     */
    protected boolean simple;   
    
    protected final static Object UNDIRECTED = "UNDIRECTED";
    protected final static Object DIRECTED = "DIRECTED";
    protected final static Object MIXED = "MIXED";
//    protected final static Object TREE = "TREE";
    
    /**
     * Determines the type of vertices that this generator will
     * create, according to the edge requirements specified
     * in the constructor:
     * 
     * <ol>
     * <li/>undirected, no parallel edges - creates @link{SimpleUndirectedSparseVertex}
     * <li/>directed, no parallel edges - creates @link{SimpleDirectedSparseVertex}
     * <li/>mixed (directed and undirected), no parallel edges - creates @link{SimpleSparseVertex}
     * <li/>undirected, parallel edges allowed - creates @link{UndirectedSparseVertex}
     * <li/>directed, parallel edges allowed - creates @link{DirectedSparseVertex}
     * <li/>mixed, parallel edges allowed - creates @link{SparseVertex}
     * </ol>
     * 
     */
    public TypedVertexGenerator(Collection edge_requirements)
    {
        if (edge_requirements.contains(Graph.UNDIRECTED_EDGE))
            type = UNDIRECTED;
        else if (edge_requirements.contains(Graph.DIRECTED_EDGE))
            type = DIRECTED;
        else // mixed directed/undirected graph
            type = MIXED;
        
        simple = edge_requirements.contains(Graph.NOT_PARALLEL_EDGE);
        
//        if (edge_requirements.contains(TreePredicate.getInstance()))
//            type = TREE;
    }
    
    public TypedVertexGenerator(ArchetypeGraph g)
    {
        this(g.getEdgeConstraints());
    }
    
    /**
     * Creates a vertex whose type is determined by the requirements 
     * specified in the constructor.
     * 
     * @see edu.uci.ics.jung.utils.VertexGenerator#create()
     */
    public Vertex create()
    {
        if (type == UNDIRECTED)
        {
            if (simple)
                return new SimpleUndirectedSparseVertex();
            else
                return new UndirectedSparseVertex();
        }
        else if (type == DIRECTED)
        {
            if (simple)
                return new SimpleDirectedSparseVertex();
            else
                return new DirectedSparseVertex();
        }
        else if (type == MIXED)
        {
            if (simple)
                return new SimpleSparseVertex();
            else
                return new SparseVertex();
        }
//        else if (type == TREE)
//        {
//            return new SimpleSparseTreeVertex();
//        }
        else
        {
            throw new FatalException("Internal error: unrecognized vertex type");
        }
    }
}
