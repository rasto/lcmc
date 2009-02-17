/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.algorithms.cluster;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.PredicateUtils;

/**
 * Finds all biconnected components (bicomponents) of an undirected graph.  
 * A graph is a biconnected component if 
 * at least 2 vertices must be removed in order to disconnect the graph.  (Graphs 
 * consisting of one vertex, or of two connected vertices, are also biconnected.)  Biconnected
 * components of three or more vertices have the property that every pair of vertices in the component
 * are connected by two or more vertex-disjoint paths.
 * <p>
 * Running time: O(|V| + |E|) where |V| is the number of vertices and |E| is the number of edges
 * @see "Depth first search and linear graph algorithms by R. E. Tarjan (1972), SIAM J. Comp."
 * 
 * @author Joshua O'Madadhain
 */
public class BicomponentClusterer implements GraphClusterer 
{
    protected Map dfs_num;
    protected Map high;
    protected Map parents;
    protected Stack stack;
    protected int converse_depth;

    /**
     * Constructs a new bicomponent finder
     */
    public BicomponentClusterer() {
    }

    /**
    * Extracts the bicomponents from the graph
    * @param theGraph the graph whose bicomponents are to be extracted
    * @return the <code>ClusterSet</code> of bicomponents
    */
    public ClusterSet extract(ArchetypeGraph theGraph) 
    {
        if (!PredicateUtils.enforcesEdgeConstraint(theGraph, Graph.UNDIRECTED_EDGE)) {
            throw new IllegalArgumentException("Algorithm currently only handles undirected graphs.");
        }
        
        ClusterSet bicomponents = new VertexClusterSet(theGraph);

        if (theGraph.getVertices().isEmpty())
            return bicomponents;

        // initialize DFS number for each vertex to 0
        dfs_num = new HashMap();
        for (Iterator it = theGraph.getVertices().iterator(); it.hasNext(); )
        {
            Vertex v = (Vertex)it.next();
            set(v, dfs_num, 0);
        }

        for (Iterator iter = theGraph.getVertices().iterator(); iter.hasNext(); )
        {
            Vertex v = (Vertex)iter.next(); 
            if (get(v, dfs_num) == 0) // if we haven't hit this vertex yet...
            {
                high = new HashMap();
                stack = new Stack();
                parents = new HashMap();
                converse_depth = theGraph.numVertices();
                // find the biconnected components for this subgraph, starting from v
                findBiconnectedComponents(v, bicomponents);
                
                // if we only visited one vertex, this method won't have
                // ID'd it as a biconnected component, so mark it as one
                if (theGraph.numVertices() - converse_depth == 1)
                {
                    Set s = new HashSet();
                    s.add(v);
                    bicomponents.addCluster(s);
                }
            }
        }
        
        return bicomponents;
    }

    /**
     * <p>Stores, in <code>bicomponents</code>, all the biconnected
     * components that are reachable from <code>v</code>.</p>
     * 
     * <p>The algorithm basically proceeds as follows: do a depth-first
     * traversal starting from <code>v</code>, marking each vertex with
     * a value that indicates the order in which it was encountered (dfs_num), 
     * and with
     * a value that indicates the highest point in the DFS tree that is known
     * to be reachable from this vertex using non-DFS edges (high).  (Since it
     * is measured on non-DFS edges, "high" tells you how far back in the DFS
     * tree you can reach by two distinct paths, hence biconnectivity.) 
     * Each time a new vertex w is encountered, push the edge just traversed
     * on a stack, and call this method recursively.  If w.high is no greater than
     * v.dfs_num, then the contents of the stack down to (v,w) is a 
     * biconnected component (and v is an articulation point, that is, a 
     * component boundary).  In either case, set v.high to max(v.high, w.high), 
     * and continue.  If w has already been encountered but is 
     * not v's parent, set v.high max(v.high, w.dfs_num) and continue. 
     * 
     * <p>(In case anyone cares, the version of this algorithm on p. 224 of 
     * Udi Manber's "Introduction to Algorithms: A Creative Approach" seems to be
     * wrong: the stack should be initialized outside this method, 
     * (v,w) should only be put on the stack if w hasn't been seen already,
     * and there's no real benefit to putting v on the stack separately: just
     * check for (v,w) on the stack rather than v.  Had I known this, I could
     * have saved myself a few days.  JRTOM)</p>
     * 
     */
    protected void findBiconnectedComponents(Vertex v, ClusterSet bicomponents)
    {
        int v_dfs_num = converse_depth;
        set(v, dfs_num, v_dfs_num);
        converse_depth--;
        set(v, high, v_dfs_num);

        for (Iterator iter = v.getNeighbors().iterator(); iter.hasNext();)
        {
            Vertex w = (Vertex) iter.next();
            int w_dfs_num = get(w, dfs_num);
            Edge vw = v.findEdge(w);
            if (w_dfs_num == 0) // w hasn't yet been visited
            {
                parents.put(w, v); // v is w's parent in the DFS tree
                stack.push(vw);
                findBiconnectedComponents(w, bicomponents);
                int w_high = get(w, high);
                if (w_high <= v_dfs_num)
                {
                    // v disconnects w from the rest of the graph,
                    // i.e., v is an articulation point
                    // thus, everything between the top of the stack and
                    // v is part of a single biconnected component
                    Set bicomponent = new HashSet();
                    Edge e;
                    do
                    {
                        e = (Edge) stack.pop();
                        bicomponent.addAll(e.getIncidentVertices());
                    }
                    while (e != vw);
                    bicomponents.addCluster(bicomponent);
                }
                set(v, high, (int) Math.max(w_high, get(v, high)));
            }
            else if (w != parents.get(v)) // (v,w) is a back or a forward edge
                set(v, high, (int) Math.max(w_dfs_num, get(v, high)));
        }
    }

    /**
     * A convenience method for getting the integer value for
     * <code>v</code> which is stored in Map <code>m</code>.
     * Does no error checking.
     */
    protected int get(Vertex v, Map m)
    {
        return ((Integer)m.get(v)).intValue();
    }

    /**
     * A convenience method for setting an integer value
     * for <code>v</code> in Map <code>m</code>.
     */
    protected void set(Vertex v, Map m, int value)
    {
        m.put(v, new Integer(value));
    }
}
