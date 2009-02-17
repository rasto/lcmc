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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.Predicate;

import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Element;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Hypergraph;
import edu.uci.ics.jung.graph.KPartiteGraph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.NumberEdgeValue;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.DirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.utils.GraphUtils;
import edu.uci.ics.jung.utils.PredicateUtils;
import edu.uci.ics.jung.utils.TypedVertexGenerator;
import edu.uci.ics.jung.utils.UserData;
import edu.uci.ics.jung.utils.VertexGenerator;
import edu.uci.ics.jung.utils.UserDataContainer.CopyAction;

/**
 * A class for creating a "folded" graph based on a k-partite graph or a
 * hypergraph.  
 * 
 * <p>A "folded" graph is derived from a k-partite graph by identifying
 * a partition of vertices which will become the vertices of the new graph, copying
 * these vertices into the new graph, and then connecting those vertices whose
 * original analogues were connected indirectly through elements
 * of other partitions.  (See <code>fold(KPartiteGraph, Predicate, NumberEdgeValue)</code>
 * for more details.)</p>
 * 
 * <p>A "folded" graph is derived from a hypergraph by creating vertices based on
 * either the vertices or the hyperedges of the original graph, and connecting 
 * vertices in the new graph if their corresponding vertices/hyperedges share a 
 * connection with a common hyperedge/vertex.  (See <code>fold(Hypergraph, 
 * boolean, NumberEdgeValue)</code> for more details.)</p>   
 * 
 * @author Danyel Fisher
 * @author Joshua O'Madadhain
 */
public class FoldingTransformer 
{
    /**
     * Used in <code>fold()</code> as a user data key to the data attached to
     * the edges in the folded graph.
     */
    public static final String FOLDED_DATA = "edu.uci.ics.jung.graph.KPartiteFolder:Folded Data";

    protected boolean parallel;
    protected CopyAction copy_action = UserData.REMOVE;
    
    /**
     * Specifies whether the graph being folded is undirected or not.
     * Set by <code>checkGraphConstraints</code>.
     */
    private boolean undirected;
    
    /**
     * Creates an instance of this Folder. See the discussion of fold for notes
     * on the "parallel" argument.
     *  
     */
    public FoldingTransformer(boolean parallel) {
        this.parallel = parallel;
    }

    /**
     * Specifies whether the folded graphs create parallel edges or a decorated
     * single edge.
     * @param parallel
     */
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }
    
    /**
     * Specifies the copy action used to attach data to edges.
     * @param copy_action
     */
    public void setCopyAction(CopyAction copy_action)
    {
        this.copy_action = copy_action;
    }

    /**
     * Equivalent to <code>fold(g, p, null)</code>.
     */
    public Graph fold(KPartiteGraph g, Predicate p) 
    {
        return fold(g, p, null);
    }

    /**
     * <p>
     * Converts <code>g</code> into a unipartite graph whose vertex set is the
     * vertices whose partition is specified by <code>p</code>. For vertices
     * <code>a</code> and <code>b</code> in this partition, the resultant
     * graph will include the edge <code>(a,b)</code> if the original graph
     * contains edges <code>(a,c)</code> and <code>(c,b)</code> for at least
     * one vertex <code>c</code>.
     * </p>
     * 
     * <p>
     * If <code>parallel</code> is true, then each such connecting vertex
     * <code>c</code> will be represented by a single edge in the resultant
     * graph, and the resultant graph may therefore contain parallel edges.
     * Otherwise, each edge <code>(a,b)</code> in the resultant graph will be
     * annotated with the set of vertices <code>c</code> that connected
     * <code>a</code> to <code>b</code> in the original graph, and the
     * graph's edge requirements will be set to refuse parallel edges.
     * </p>
     * 
     * <p>In either case, if the original graph contains both a directed edge from 
     * <code>a</code> to <code>b</code>, and a directed edge from <code>b</code>
     * <code>a</code>, then a self-loop will be created from <code>a</code>
     * to itself in the folded graph.  Undirected edges do not result in 
     * self-loops.
     * </p>
     * 
     * <p>
     * If <code>g</code> is neither strictly directed nor strictly undirected,
     * this method throws <code>IllegalArgumentException</code>.  Parallel edges
     * in the original graph have no effect on the resultant graph: only one edge
     * <code>(a,c)</code> and one edge <code>(c,b)</code> are necessary to 
     * induce a connection between <code>a</code> and <code>b</code> in the folded
     * graph, and any additional such edges are ignored.</p>
     * 
     * <p>If <code>nev</code> is null, 
     * adds the connecting element as a decoration on the corresponding edge in the new
     * graph; otherwise, sets the weight of each edge to the number of parallel 
     * paths between the corresponding vertices in the original graph that are 
     * encountered in the folding process.</p>
     * 
     * @param g the graph to fold
     * @param p the predicate which specifies the partition to fold into
     * @return the folded graph
     * @throws IllegalArgumentException
     */
    public Graph fold(KPartiteGraph g, Predicate p, NumberEdgeValue nev)
    {
        checkGraphConstraints(g);

        Graph newGraph = createGraph();

        // get vertices for the specified partition, copy into new graph
        Set vertices = PredicateUtils.getVertices(g, p);
        for (Iterator iter = vertices.iterator(); iter.hasNext();) {
            ArchetypeVertex v = (ArchetypeVertex) iter.next();
            v.copy(newGraph);
        }

        for (Iterator iter = vertices.iterator(); iter.hasNext();) {
            Vertex v = (Vertex) iter.next();
            Vertex v_new = (Vertex) v.getEqualVertex(newGraph);
            Set succs = v.getSuccessors();

            for (Iterator s_iter = succs.iterator(); s_iter.hasNext();) {
                Vertex s = (Vertex) s_iter.next();
                Set s_succs = s.getSuccessors();

                for (Iterator t_iter = s_succs.iterator(); t_iter.hasNext();) {
                    Vertex t = (Vertex) t_iter.next();

                    // if t is in the partition of interest
                    // and has not been covered (undirected graphs only)
                    if (!vertices.contains(t)) continue;

                    Vertex t_new = (Vertex) t.getEqualVertex(newGraph);
                    addEdge(newGraph, v_new, s, t_new, nev);

                }
            }
        }
        return newGraph;
    }

//    /**
//     * Equivalent to <code>fold(h, use_vertices, null)</code>.
//     */
//    public Graph fold(Hypergraph h, boolean use_vertices)
//    {
//        return fold(h, use_vertices, null);
//    }
//    
//    public Graph fold(Hypergraph h, boolean use_vertices, NumberEdgeValue nev)
//    {
//        return fold(h, null, use_vertices, nev, new BidirectionalHashMap());
//    }
    
    /**
     * Creates a <code>Graph</code> which is a "folded" version of <code>h</code>.
     * 
     * <p>If <code>use_vertices</code> is true, the vertices of the new graph 
     * correspond to the vertices of <code>h</code>, and <code>a</code> 
     * is connected to <code>b</code> in the new graph if the corresponding vertices
     * in <code>h</code> are connected by a hyperedge.  Thus, each hyperedge with 
     * <i>k</i> vertices in <code>h</code> would induce a <i>k</i>-clique in the new graph.</p>
     * 
     * <p>If <code>use_vertices</code> is false, then the vertices of the new
     * graph correspond to the hyperedges of <code>h</code>, and <code>a</code>
     * is connected to <code>b</code> in the new graph if the corresponding hyperedges
     * in <code>h</code> share a vertex.  Thus, each vertex connected to <i>k</i> 
     * hyperedges in <code>h</code> would induce a <i>k</i>-clique in the new graph.</p>
     * 
     * <p>If <code>nev</code> is null, 
     * adds the connecting element as a decoration on the corresponding edge in the new
     * graph; otherwise, sets the weight of each edge to the number of parallel 
     * paths between the corresponding vertices in the original graph that are 
     * encountered in the folding process.</p>
     */
    public Graph fold(Hypergraph h, Graph target, boolean use_vertices, NumberEdgeValue nev, BidiMap map)
    {
        undirected = true;
        
        if (target == null)
            target = createGraph();

        VertexGenerator vg = GraphUtils.getVertexGenerator(target);
        if (vg == null)
            vg = new TypedVertexGenerator(target);
        
        Map m = new HashMap();
        Set vertices;
        Set edges;
        
        // vertices and hyperedges are duals of one another; we can treat
        // them equivalently for this purpose
        if (use_vertices)
        {
            vertices = h.getVertices();
            edges = h.getEdges();
        }
        else
        {
            vertices = h.getEdges();
            edges = h.getVertices();
        }
        
        // create vertices:
        // for each "vertex", create a new vertex and import user data
        for (Iterator iter = vertices.iterator(); iter.hasNext(); )
        {
            Element av = (Element)iter.next();
            Vertex v = vg.create();
            v.importUserData(av);
            target.addVertex(v);
            m.put(av, v);
            map.put(v, av);
        }
        
        // create edges:
        // for each "hyperedge", create an edge between each incident vertex pair
        for (Iterator iter = edges.iterator(); iter.hasNext(); )
        {
            Element he = (Element)iter.next();
            Set elts = he.getIncidentElements();
            Vertex[] v_array = new Vertex[elts.size()];
            int i = 0;
            for (Iterator e_iter = elts.iterator(); e_iter.hasNext(); )
                v_array[i++] = (Vertex)(m.get(e_iter.next()));
            for (i = 0; i < v_array.length; i++)
                for (int j = i + 1; j < v_array.length; j++)
                    addEdge(target, v_array[i], he, v_array[j], nev);
        }
        
        return target;
    }

    
    /**
     * Creates a new edge from <code>firstEnd</code> to <code>secondEnd</code> 
     * in <code>newGraph</code>. Note that
     * <code>firstEnd</code> and <code>secondEnd</code> are both parts of 
     * <code>newGraph</code>, while
     * <code>intermediate</code> is part of the original graph. If <code>parallel</code> is set,
     * adds a new edge from <code>firstEnd</code> to <code>secondEnd</code>.
     * If <code>parallel</code> is not set, then (as appropriate) adds an edge
     * or creates one from <code>firstEnd</code> to <code>secondEnd</code>. 
     */
    protected void addEdge(Graph newGraph, Vertex firstEnd,
            Element intermediate, Vertex secondEnd, NumberEdgeValue nev) 
    {
        if( undirected && firstEnd == secondEnd ) return;
        if (parallel) {
            Edge v_t;
            if (undirected)
                v_t = new UndirectedSparseEdge(firstEnd, secondEnd);
            else
                v_t = new DirectedSparseEdge(firstEnd, secondEnd);
            if (nev != null)
                nev.setNumber(v_t, new Integer(1));
            else
                v_t.addUserDatum(FOLDED_DATA, intermediate, copy_action);
            newGraph.addEdge(v_t);
        } else {
            Edge v_t = firstEnd.findEdge(secondEnd);
            if (v_t == null) {
                if (undirected)
                    v_t = new UndirectedSparseEdge(firstEnd, secondEnd);
                else
                    v_t = new DirectedSparseEdge(firstEnd, secondEnd);
                if (nev != null)
                    nev.setNumber(v_t, new Integer(0));
                else
                    v_t.addUserDatum(FOLDED_DATA, new HashSet(), copy_action);
                newGraph.addEdge(v_t);
            }
            if (nev != null)
                nev.setNumber(v_t, new Integer(nev.getNumber(v_t).intValue() + 1));
            else
            {
                Set folded_vertices = (Set) v_t.getUserDatum(FOLDED_DATA);
                folded_vertices.add(intermediate);
            }
        }
    }

    /**
     * Returns a base graph to use.
     */
    protected Graph createGraph() 
    {
        Graph newGraph;
        if (undirected)
            newGraph = new UndirectedSparseGraph();
        else
            newGraph = new DirectedSparseGraph();
        
        if (parallel) 
            newGraph.getEdgeConstraints().remove(Graph.NOT_PARALLEL_EDGE);

        return newGraph;
    }

    /**
     * Checks for, and rejects, mixed-mode graphs, and sets the <code>undirected</code>
     * class variable state.
     */
    protected void checkGraphConstraints(KPartiteGraph g) {
        undirected = PredicateUtils.enforcesUndirected(g);
        if (!undirected && !PredicateUtils.enforcesDirected(g))
                throw new IllegalArgumentException(
                        "Graph must be strictly "
                                + "directed or strictly undirected (no mixed graphs allowed)");
    }

}