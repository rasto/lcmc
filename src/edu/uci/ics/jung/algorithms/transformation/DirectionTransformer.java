/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
/*
 * Created on Apr 21, 2004
 */
package edu.uci.ics.jung.algorithms.transformation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.DirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.utils.TypedVertexGenerator;
import edu.uci.ics.jung.utils.VertexGenerator;

/**
 * <p>Transforms graphs of one directionality into the other.</p>
 * 
 * <P>The <code>copy</code> parameter of the transformation methods, if 
 * <code>true</code>, specifies
 * that the vertices of the input graph will be copied (using the 
 * <code>ArchetypeVertex.copy()</code> method) into the new graph; 
 * if <code>false</code>,
 * new vertices will be created (using the most restrictive vertex type
 * appropriate to the transformed graph type).  In 
 * either case, the user data repositories of the original vertices will be
 * imported into the corresponding vertices of the transformed graph.</p>  
 * 
 * <p>The advantage
 * of using the <code>copy</code> mode is that the vertex equality 
 * relationship reflected by <code>getEqualVertex()</code> will be
 * established between the vertices of the two graphs; however, the 
 * vertices of the original graph must be able to accommodate edges of
 * the types appropriate to both the original and the transformed graph.
 * (As of version 1.4, this means that the vertices of the input
 * graph must be instances of either <code>SparseVertex</code> or
 * <code>SimpleSparseVertex</code>.)</p>
 * 
 * <p>The advantage of not using the <code>copy</code> mode is that 
 * the vertices of the original graph do not need to be able to accommodate
 * both directed and undirected edges, which relieves the user from having
 * to worry about this matter.</p>
 * 
 * <p>Directed edges cannot be copied to undirected edges or vice versa,
 * so the edges of the transformed graph are always new edges; as above,
 * the user data repositories of the original edges are imported into
 * the edges of the transformed graph.</p>
 * 
 * <p><b>Known issues:</b> 
 * <ul>
 * <li/><code>toUndirected()</code>: if the edges
 * <code>(a,b)</code> and <code>(b,a)</code> both exist in the original 
 * graph, the user data from one will be imported into the new undirected
 * edge; the user data from the other will not be imported.  It is not
 * specified which edge's user data will be imported.
 * <li/>The resultant graphs will not have parallel edges, regardless of
 * whether the original graphs had parallel edges.
 * </ul>
 * 
 * @author Danyel Fisher
 * @author Joshua O'Madadhain
 */
public class DirectionTransformer 
{

    /**
     * Transforms <code>graph</code> (which may be of any directionality)
     * into an undirected graph without
     * parallel edges. (This is likely to be very useful for visualization
     * tasks). Creates exactly one undirected edge (a, b) iff a isNeighborOf b.
     * Equivalent to <code>toUndirected(dGraph, true)</code>.
     * 
     * @param graph    the graph to be transformed
     * @return          the transformed <code>UndirectedGraph</code>
     * @see toUndirected(Graph, boolean)
     */
    public static UndirectedGraph toUndirected(Graph graph) 
    {
        return toUndirected(graph, true);
    }

    
    /**
     * Transforms <code>graph</code> (which may be of any directionality)
     * into an undirected graph. (This is likely to be very useful for
     * visualization tasks). Creates exactly one undirected edge (a, b) iff a
     * isNeighborOf b. If <code>copy</code> is <code>true</code>, specifies
     * that the vertices of the input graph will be copied (using the
     * <code>ArchetypeVertex.copy()</code> method) into the new graph; if
     * <code>false</code>, new vertices will be created (using the most
     * restrictive vertex type appropriate to the transformed graph type).
     * 
     * @param graph    the graph to be transformed
     * @param copy      specifies whether the vertices are to be copied or duplicated
     * @return the transformed <code>UndirectedGraph</code>
     */
    public static UndirectedGraph toUndirected(Graph graph, boolean copy)
    {
        UndirectedGraph uGraph = new UndirectedSparseGraph();
        uGraph.importUserData(graph);

        Map vertex_map = convertVertices(graph, uGraph, copy);

        for (Iterator eIt = graph.getEdges().iterator(); eIt.hasNext();) {
            Edge e = (Edge) eIt.next();
            Vertex dv1 = (Vertex) e.getEndpoints().getFirst();
            Vertex dv2 = (Vertex) e.getEndpoints().getSecond();
            Vertex uv1 = (Vertex)vertex_map.get(dv1);
            Vertex uv2 = (Vertex)vertex_map.get(dv2);
            if (uv1.isNeighborOf(uv2)) continue;
            Edge uEdge = uGraph.addEdge(new UndirectedSparseEdge(uv1, uv2));
            uEdge.importUserData(e);
        }
        return uGraph;

    }
    
    /**
     * Puts a version of each vertex from <code>old</code> into 
     * <code>transformed</code>.  See the class-level documentation for
     * the behavior of the <code>copy</code> parameter.
     */
    protected static Map convertVertices(Graph old, Graph transformed, boolean copy)
    {
        VertexGenerator vg = new TypedVertexGenerator(transformed);
        Map vertex_map = new HashMap();
        
        for (Iterator iter = old.getVertices().iterator(); iter.hasNext(); )
        {
            Vertex v = (Vertex)iter.next(); 
            Vertex v_t;
            if (copy)
                v_t = (Vertex)v.copy(transformed);
            else
            {
                v_t = transformed.addVertex(vg.create());
                v_t.importUserData(v);
            }
            vertex_map.put(v, v_t);
        }
        return vertex_map;
    }
    
    /**
     * Transforms <code>graph</code> (which may be of any directionality)
     * into a directed graph without
     * parallel edges. Creates exactly one directed edge (a, b) iff a
     * isPredecessorOf b (so an UndirectedEdge will actually produce two edges).
     * Equivalent to <code>toDirected(graph, true)</code>.
     * 
     * @param graph     the graph to be transformed
     * @return          the transformed <code>DirectedGraph</code>
     * @see toDirected(Graph, boolean)
     */
    public static DirectedGraph toDirected(Graph graph) 
    {
        return toDirected(graph, true);
    }

    /**
     * Transforms <code>graph</code> (which may be of any directionality)
     * into a directed graph. Creates exactly one directed edge (a, b) iff a
     * isPredecessorOf b (so an UndirectedEdge will actually produce two edges). 
     * If <code>copy</code> is <code>true</code>, specifies
     * that the vertices of the input graph will be copied (using the
     * <code>ArchetypeVertex.copy()</code> method) into the new graph; if
     * <code>false</code>, new vertices will be created (using the most
     * restrictive vertex type appropriate to the transformed graph type).
     * 
     * @param graph     the graph to be transformed
     * @param copy      specifies whether the vertices are to be copied or duplicated
     * @return          the transformed <code>DirectedGraph</code>
     */
    public static DirectedGraph toDirected(Graph graph, boolean copy)
    {
        DirectedGraph dGraph = new DirectedSparseGraph();
        dGraph.importUserData(graph);
        
        Map vertex_map = convertVertices(graph, dGraph, copy);

        for (Iterator eIt = graph.getEdges().iterator(); eIt.hasNext();) 
        {
            Edge e = (Edge) eIt.next();
            Vertex uv1 = (Vertex) e.getEndpoints().getFirst();
            Vertex uv2 = (Vertex) e.getEndpoints().getSecond();
            Vertex dv1 = (Vertex)vertex_map.get(uv1);
            Vertex dv2 = (Vertex)vertex_map.get(uv2);
            if (uv1.isPredecessorOf(uv2) && !dv1.isPredecessorOf(dv2)) {
                Edge dEdge = dGraph.addEdge(new DirectedSparseEdge(dv1, dv2));
                dEdge.importUserData(e);
            }
            if (uv2.isPredecessorOf(uv1) && !dv2.isPredecessorOf(dv1)) {
                Edge dEdge = dGraph.addEdge(new DirectedSparseEdge(dv2, dv1));
                dEdge.importUserData(e);
            }           
        }

        return dGraph;
    }
}