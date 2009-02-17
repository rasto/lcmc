/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.random.generators;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.DirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.DirectedSparseVertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.utils.UserData;

/**
 * <p>Simple evolving scale-free random graph generator. At each time
 * step, a new vertex is created and is connected to existing vertices
 * according to the principle of "preferential attachment", whereby 
 * vertices with higher degree have a higher probability of being 
 * selected for attachment.</p>
 * 
 * <p>At a given timestep, the probability <code>p</code> of creating an edge
 * between an existing vertex <code>v</code> and the newly added vertex is
 * <pre>
 * p = (degree(v) + 1) / (|E| + |V|);
 * </pre>
 * 
 * <p>where <code>|E|</code> and <code>|V|</code> are, respectively, the number 
 * of edges and vertices currently in the network (counting neither the new
 * vertex nor the other edges that are being attached to it).</p>
 * 
 * <p>Note that the formula specified in the original paper
 * (cited below) was
 * <pre>
 * p = degree(v) / |E|
 * </pre>
 * </p>
 * 
 * <p>However, this would have meant that the probability of attachment for any existing
 * isolated vertex would be 0.  This version uses Lagrangian smoothing to give
 * each existing vertex a positive attachment probability.</p>
 * 
 * <p>The graph created may be either directed or undirected (controlled by a constructor
 * parameter); the default is undirected.  
 * If the graph is specified to be directed, then the edges added will be directed
 * from the newly added vertex u to the existing vertex v, with probability proportional to the 
 * indegree of v (number of edges directed towards v).  If the graph is specified to be undirected,
 * then the (undirected) edges added will connect u to v, with probability proportional to the 
 * degree of v.</p> 
 * 
 * <p>The <code>parallel</code> constructor parameter specifies whether parallel edges
 * may be created.</p>
 * 
 * @see "A.-L. Barabasi and R. Albert, Emergence of scaling in random networks, Science 286, 1999."
 * @author Scott White
 * @author Joshua O'Madadhain
 */
public class BarabasiAlbertGenerator implements EvolvingGraphGenerator 
{
    private Graph mGraph = null;
    private int mNumEdgesToAttachPerStep;
    private int mElapsedTimeSteps;
    private Random mRandom;
    protected Vector vertex_index;
    protected int init_vertices;
    protected Map index_vertex;
    protected boolean directed;
    protected boolean parallel;
    
    /**
     * Tags the initial "seed" vertices that the graph starts with
     */
    public final static Object SEED = "edu.uci.ics.jung.random.generators.BarabasiAlbertGenerator.SEED";

    /**
     * Constructs a new instance of the generator.
     * @param init_vertices     number of unconnected 'seed' vertices that the graph should start with
     * @param numEdgesToAttach the number of edges that should be attached from the
     * new vertex to pre-existing vertices at each time step
     * @param directed  specifies whether the graph and edges to be created should be directed or not
     * @param parallel  specifies whether the algorithm permits parallel edges
     * @param seed  random number seed
     */
    public BarabasiAlbertGenerator(int init_vertices, int numEdgesToAttach, boolean directed, boolean parallel, int seed)
    {
        if (init_vertices <= 0)
            throw new IllegalArgumentException("Number of initial unconnected 'seed' vertices " + 
                    "must be positive");
        if (numEdgesToAttach <= 0) 
            throw new IllegalArgumentException("Number of edges to attach " +
                    "at each time step must be positive");
        
        if (!parallel && init_vertices < numEdgesToAttach)
            throw new IllegalArgumentException("If parallel edges disallowed, initial" +
                    "number of vertices must be >= number of edges to attach at each time step");
        mNumEdgesToAttachPerStep = numEdgesToAttach;
        mRandom = new Random(seed);
        this.init_vertices = init_vertices;
        this.directed = directed;
        this.parallel = parallel;
        initialize();
    }
    
    /**
     * Constructs a new instance of the generator, whose output will be an undirected graph.
     * @param init_vertices     number of unconnected 'seed' vertices that the graph should start with
     * @param numEdgesToAttach the number of edges that should be attached from the
     * new vertex to pre-existing vertices at each time step
     * @param seed  random number seed
     */
    public BarabasiAlbertGenerator(int init_vertices, int numEdgesToAttach, int seed) 
    {
        this(init_vertices, numEdgesToAttach, false, false, seed);
    }

    /**
     * Constructs a new instance of the generator, whose output will be an undirected graph,
     * and which will use the current time as a seed for the random number generation.
     * @param init_vertices     number of vertices that the graph should start with
     * @param numEdgesToAttach the number of edges that should be attached from the
     * new vertex to pre-existing vertices at each time step
     */
    public BarabasiAlbertGenerator(int init_vertices, int numEdgesToAttach) {
        this(init_vertices, numEdgesToAttach, (int) System.currentTimeMillis());
    }
    
    private void initialize() 
    {
        if (directed)
            mGraph = new DirectedSparseGraph();
        else
            mGraph = new UndirectedSparseGraph();
        if (parallel)
            mGraph.getEdgeConstraints().remove(Graph.NOT_PARALLEL_EDGE);
        vertex_index = new Vector(2*init_vertices);
        index_vertex = new HashMap(2*init_vertices);
        for (int i = 0; i < init_vertices; i++)
        {
            Vertex v = new UndirectedSparseVertex();
            mGraph.addVertex(v);
            vertex_index.add(v);
            index_vertex.put(v, new Integer(i));
            v.addUserDatum(SEED, SEED, UserData.REMOVE);
        }
            
        mElapsedTimeSteps = 0;
    }

    private Edge createRandomEdge(Set preexistingNodes, Vertex newVertex, Set added_pairs) 
    {
        Vertex attach_point;
        boolean created_edge = false;
        Pair endpoints;
        do
        {
            attach_point = (Vertex)vertex_index.elementAt(mRandom.nextInt(vertex_index.size()));
            
            endpoints = new Pair(newVertex, attach_point);
            
            // if parallel edges are not allowed, skip attach_point if <newVertex, attach_point>
            // already exists; note that because of the way edges are added, we only need to check
            // the list of candidate edges for duplicates.
            if (!parallel && added_pairs.contains(endpoints))
                continue;
            
            double degree = directed ? attach_point.inDegree() : attach_point.degree();
            
            // subtract 1 from numVertices because we don't want to count newVertex
            // (which has already been added to the graph, but not to vertex_index)
            double attach_prob = (degree + 1) / (mGraph.numEdges() + mGraph.numVertices() - 1);
            if (attach_prob >= mRandom.nextDouble())
                created_edge = true;
        }
        while (!created_edge);

        Edge to_add;
        
        if (directed)
        {
            to_add = new DirectedSparseEdge(newVertex, attach_point);
            added_pairs.add(endpoints);
        }
        else
        {
            to_add = new UndirectedSparseEdge(newVertex, attach_point);
            added_pairs.add(endpoints);
            added_pairs.add(new Pair(attach_point, newVertex));
        }
        
        return to_add;
    }

    public void evolveGraph(int numTimeSteps) {

        for (int i = 0; i < numTimeSteps; i++) {
            evolveGraph();
            mElapsedTimeSteps++;
        }
    }

    private void evolveGraph() 
    {
        Set preexistingNodes = mGraph.getVertices();
        Vertex newVertex;
        if (directed)
            newVertex = new DirectedSparseVertex();
        else
            newVertex = new UndirectedSparseVertex();
        mGraph.addVertex(newVertex);

        // generate and store the new edges; don't add them to the graph
        // yet because we don't want to bias the degree calculations
        // (all new edges in a timestep should be added in parallel)
        List edges = new LinkedList();
        HashSet added_pairs = new HashSet(mNumEdgesToAttachPerStep*3);
        for (int i = 0; i < mNumEdgesToAttachPerStep; i++) 
            edges.add(createRandomEdge(preexistingNodes, newVertex, added_pairs));
        
        // add edges to graph, now that we have them all
        for (Iterator iter = edges.iterator(); iter.hasNext(); )
            mGraph.addEdge((Edge)iter.next());
        
        // now that we're done attaching edges to this new vertex, 
        // add it to the index
        vertex_index.add(newVertex);
        index_vertex.put(newVertex, new Integer(vertex_index.size() - 1));
    }

    public int getIndex(Vertex v)
    {
        return ((Integer)index_vertex.get(v)).intValue();
    }
    
    public int getNumElapsedTimeSteps() {
        return mElapsedTimeSteps;
    }

    public ArchetypeGraph generateGraph() {
        return mGraph;
    }

    public void reset() {
        initialize();
    }
}
