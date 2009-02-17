/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 */
/*
 * Created on Jul 2, 2003
 *  
 */
package edu.uci.ics.jung.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.EdgeWeightLabeller;
import edu.uci.ics.jung.graph.decorators.Indexer;
import edu.uci.ics.jung.graph.decorators.NumberEdgeValue;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.graph.impl.AbstractSparseGraph;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.DirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.SparseGraph;
import edu.uci.ics.jung.graph.impl.SparseVertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;
import edu.uci.ics.jung.random.generators.BarabasiAlbertGenerator;

/**
 * 
 * Generates a series of potentially useful test graphs.
 * 
 * @author danyelf
 *  
 */
public class TestGraphs {

	/**
	 * A series of pairs that may be useful for generating graphs. The
	 * miniature graph consists of 8 edges, 10 nodes, and is formed of two
	 * connected components, one of 8 nodes, the other of 2.
	 *  
	 */
	public static String[][] pairs = { { "a", "b", "3" }, {
			"a", "c", "4" }, {
			"a", "d", "5" }, {
			"d", "c", "6" }, {
			"d", "e", "7" }, {
			"e", "f", "8" }, {
			"f", "g", "9" }, {
			"h", "i", "1" }
	};

	/**
	 * Creates a small sample graph that can be used for testing purposes. The
	 * graph is as described in the section on {@link #pairs pairs}. If <tt>isDirected</tt>,
	 * the graph is a {@link DirectedSparseGraph DirectedSparseGraph},
	 * otherwise, it is an {@link UndirectedSparseGraph UndirectedSparseGraph}.
	 * 
	 * @param isDirected:
	 *            Is the graph directed?
	 * @return a graph consisting of eight edges and ten nodes.
	 */
	public static AbstractSparseGraph createTestGraph(boolean isDirected) {
		AbstractSparseGraph g;
		if (isDirected) {
			g = new DirectedSparseGraph();
		} else {
			g = new UndirectedSparseGraph();
		}
		StringLabeller sl = StringLabeller.getLabeller(g);
		EdgeWeightLabeller el = EdgeWeightLabeller.getLabeller(g);
		for (int i = 0; i < pairs.length; i++) {
			String[] pair = pairs[i];
			createEdge(g, sl, el, pair[0], pair[1], Integer.parseInt(pair[2]));
		}
		return g;

	}

    /**
     * Returns a graph consisting of a chain of <code>vertex_count - 1</code> vertices
     * plus one isolated vertex.
     */
    public static Graph createChainPlusIsolates(int chain_length, int isolate_count)
    {
        Graph g = new UndirectedSparseGraph();
        if (chain_length > 0)
        {
            Vertex[] v = new Vertex[chain_length];
            v[0] = g.addVertex(new UndirectedSparseVertex());
            for (int i = 1; i < chain_length; i++)
            {
                v[i] = g.addVertex(new UndirectedSparseVertex());
                g.addEdge(new UndirectedSparseEdge(v[i], v[i-1]));
            }
        }
        for (int i = 0; i < isolate_count; i++)
            g.addVertex(new UndirectedSparseVertex());
        
        return g;
    }
    
	/**
	 * Creates a sample directed acyclic graph by generating several "layers",
	 * and connecting nodes (randomly) to nodes in earlier (but never later)
	 * layers. Each layer has some random number of nodes in it 1 less than n
	 * less than maxNodesPerLayer.
	 * 
	 * @return the created graph
	 */
	public static Graph createDirectedAcyclicGraph(
		int layers,
		int maxNodesPerLayer,
		double linkprob) {
		DirectedGraph dag = new DirectedSparseGraph();
		StringLabeller sl = StringLabeller.getLabeller(dag);
		Set previousLayers = new HashSet();
		Set inThisLayer = new HashSet();
		for (int i = 0; i < layers; i++) {

			int nodesThisLayer = (int) (Math.random() * maxNodesPerLayer) + 1;
			for (int j = 0; j < nodesThisLayer; j++) {
				Vertex v = dag.addVertex(new SparseVertex());
				inThisLayer.add(v);
				try {
					sl.setLabel(v, i + ":" + j);
				} catch (Exception e) {
				}
				// for each previous node...
				for (Iterator iter = previousLayers.iterator();
					iter.hasNext();
					) {
					Vertex v2 = (Vertex) iter.next();
					if (Math.random() < linkprob) {
						GraphUtils.addEdge(dag, v, v2);
					}
				}
			}

			previousLayers.addAll(inThisLayer);
			inThisLayer.clear();
		}
		return dag;
	}

	private static void createEdge(
		final AbstractSparseGraph g,
		StringLabeller sl,
		EdgeWeightLabeller el,
		String v1Label,
		String v2Label,
		int weight) {

		try {
			Vertex v1 = sl.getVertex(v1Label);
			if (v1 == null) {
				v1 = g.addVertex(new SparseVertex());
				sl.setLabel(v1, v1Label);
			}
			Vertex v2 = sl.getVertex(v2Label);
			if (v2 == null) {
				v2 = g.addVertex(new SparseVertex());
				sl.setLabel(v2, v2Label);
			}
			Edge e = GraphUtils.addEdge(g, v1, v2);
			el.setWeight(e, weight);
		} catch (StringLabeller.UniqueLabelException e) {
			throw new FatalException("This should not happen " + e);
		}
	}

	/**
	 * Returns a bigger, undirected test graph with a just one component. This
	 * graph consists of a clique of ten edges, a partial clique (randomly
	 * generated, with edges of 0.6 probability), and one series of edges
	 * running from the first node to the last.
	 * 
	 * @return the testgraph
	 */
	public static Graph getOneComponentGraph() {
		UndirectedSparseGraph g = new UndirectedSparseGraph();
		StringLabeller sl = StringLabeller.getLabeller(g);
		EdgeWeightLabeller el = EdgeWeightLabeller.getLabeller(g);

		// let's throw in a clique, too
		for (int i = 1; i <= 10; i++) {
			for (int j = i + 1; j <= 10; j++) {
				String i1 = "" + i;
				String i2 = "" + j;
				createEdge(g, sl, el, i1, i2, i + j);
			}
		}

		// and, last, a partial clique
		for (int i = 11; i <= 20; i++) {
			for (int j = i + 1; j <= 20; j++) {
				if (Math.random() > 0.6)
					continue;
				String i1 = "" + i;
				String i2 = "" + j;
				createEdge(g, sl, el, i1, i2, i + j);
			}
		}

		// and one edge to connect them all
		Indexer ind = Indexer.getIndexer(g);
		for (int i = 0; i < g.numVertices() - 1; i++) {
			try {
				GraphUtils.addEdge(g, (Vertex)ind.getVertex(i), (Vertex) ind.getVertex(i + 1));
			} catch (IllegalArgumentException fe) {
			}
		}

		return g;
	}

	/**
	 * Returns a bigger test graph with a clique, several components, and other
	 * parts.
	 * 
	 * @return a demonstration graph of type <tt>UndirectedSparseGraph</tt>
	 *         with 28 vertices.
	 */
	public static Graph getDemoGraph() {
		UndirectedSparseGraph g = new UndirectedSparseGraph();
		StringLabeller sl = StringLabeller.getLabeller(g);
		EdgeWeightLabeller el = EdgeWeightLabeller.getLabeller(g);

		for (int i = 0; i < pairs.length; i++) {
			String[] pair = pairs[i];
			createEdge(g, sl, el, pair[0], pair[1], Integer.parseInt(pair[2]));
		}

		// let's throw in a clique, too
		for (int i = 1; i <= 10; i++) {
			for (int j = i + 1; j <= 10; j++) {
				String i1 = "" + i;
				String i2 = "" + j;
				createEdge(g, sl, el, i1, i2, i + j);
			}
		}

		// and, last, a partial clique
		for (int i = 11; i <= 20; i++) {
			for (int j = i + 1; j <= 20; j++) {
				if (Math.random() > 0.6)
					continue;
				String i1 = "" + i;
				String i2 = "" + j;
				createEdge(g, sl, el, i1, i2, i + j);
			}
		}
		return g;
	}

	/**
	 * Equivalent to <code>generateMixedRandomGraph(edge_weight, num_vertices, true)</code>.
	 */
	public static Graph generateMixedRandomGraph(NumberEdgeValue edge_weight, int num_vertices)
	{
		return generateMixedRandomGraph(edge_weight, num_vertices, true);
	}

    /**
     * Returns a random mixed-mode graph.  Starts with a randomly generated 
     * Barabasi-Albert (preferential attachment) generator 
     * (4 initial vertices, 3 edges added at each step, and num_vertices - 4 evolution steps).
     * Then takes the resultant graph, replaces random undirected edges with directed
     * edges, and assigns random weights to each edge.
     */
    public static Graph generateMixedRandomGraph(NumberEdgeValue edge_weights, int num_vertices, boolean parallel)
    {
        int seed = (int)(Math.random() * 10000);
        BarabasiAlbertGenerator bag = new BarabasiAlbertGenerator(4, 3, false, parallel, seed);
        bag.evolveGraph(num_vertices - 4);
        Graph ug = (Graph)bag.generateGraph();
        SettableVertexMapper svm = new HashSettableVertexMapper();

        // create a SparseGraph version of g
        Graph g = new SparseGraph();
        for (Iterator iter = ug.getVertices().iterator(); iter.hasNext(); )
        {
            Vertex v = (Vertex)iter.next();
            Vertex w = new SparseVertex();
            g.addVertex(w);
            if (v.containsUserDatumKey(BarabasiAlbertGenerator.SEED))
                w.addUserDatum(BarabasiAlbertGenerator.SEED, BarabasiAlbertGenerator.SEED, UserData.REMOVE);
            svm.map(v, w);
        }
        
        // randomly replace some of the edges by directed edges to 
        // get a mixed-mode graph, add random weights
        for (Iterator iter = ug.getEdges().iterator(); iter.hasNext(); )
        {
            Edge e = (Edge)iter.next();
            Vertex v1 = (Vertex)e.getEndpoints().getFirst();
            Vertex v2 = (Vertex)e.getEndpoints().getSecond();
            Vertex mv1 = (Vertex)svm.getMappedVertex(v1);
            Vertex mv2 = (Vertex)svm.getMappedVertex(v2);
            Edge me;
            if (Math.random() < 0.5)
                me = new DirectedSparseEdge(mv1, mv2);
            else
                me = new UndirectedSparseEdge(mv1, mv2);
            g.addEdge(me);
            edge_weights.setNumber(me, new Double(Math.random()));
        }
        
        return g;
    }

    
}
