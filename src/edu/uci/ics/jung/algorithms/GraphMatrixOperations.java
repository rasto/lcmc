/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved. This software is open-source under the BSD
 * license; see either "license.txt" or http://jung.sourceforge.net/license.txt
 * for a description.
 */
package edu.uci.ics.jung.algorithms;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.ConstantEdgeValue;
import edu.uci.ics.jung.graph.decorators.Indexer;
import edu.uci.ics.jung.graph.decorators.NumberEdgeValue;
import edu.uci.ics.jung.graph.decorators.UserDatumNumberEdgeValue;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.DirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.utils.GraphUtils;
import edu.uci.ics.jung.utils.UserData;

/**
 * Contains methods for performing the analogues of certain matrix operations on
 * graphs.
 * <p>
 * These implementations are efficient on sparse graphs, but may not be the best
 * implementations for very dense graphs.
 * <P>
 * Anticipated additions to this class: methods for taking products and inverses
 * of graphs.
 * 
 * @author Joshua O'Madadhain
 * @see MatrixElementOperations
 */
public class GraphMatrixOperations
{
    /**
     * Returns the graph that corresponds to the square of the (weighted)
     * adjacency matrix that the specified graph <code>g</code> encodes. The
     * implementation of MatrixElementOperations that is furnished to the
     * constructor specifies the implementation of the dot product, which is an
     * integral part of matrix multiplication.
     * 
     * @param g
     *            the graph to be squared
     * @return the result of squaring g
     */
    public static Graph square(Graph g, MatrixElementOperations meo)
    {
        // create new graph of same type
        Graph G2 = (Graph) g.newInstance();
        Set V = g.getVertices();
        for (Iterator it = V.iterator(); it.hasNext();)
        {
            Vertex v = (Vertex) it.next();
            v.copy(G2);
        }
        Iterator vertices = V.iterator();
        while (vertices.hasNext())
        {
            Vertex v = (Vertex) vertices.next();
            Iterator preds = v.getPredecessors().iterator();
            while (preds.hasNext())
            {
                Vertex src = (Vertex) preds.next();
                // get vertex in G2 with same ID
                Vertex d2_src = (Vertex) src.getEqualVertex(G2);
                // get the edge connecting src to v in G
                Edge e1 = src.findEdge(v);
                Iterator succs = v.getSuccessors().iterator();
                while (succs.hasNext())
                {
                    Vertex dest = (Vertex) succs.next();
                    // get vertex in G2 with same ID
                    Vertex d2_dest = (Vertex) dest.getEqualVertex(G2);
                    // get edge connecting v to dest in G
                    Edge e2 = v.findEdge(dest);
                    // collect data on path composed of e1 and e2
                    Object pathData = meo.computePathData(e1, e2);
                    Edge e = d2_src.findEdge(d2_dest);
                    // if no edge from src to dest exists in G2, create one
                    if (e == null)
                        e = GraphUtils.addEdge(G2, d2_src, d2_dest);
                    meo.mergePaths(e, pathData);
                }
            }
        }
        return G2;
    }

    /**
     * Creates a graph from a square (weighted) adjacency matrix. If 
     * <code>nev</code> is non-null then
     * the weight is stored as a Double as specified by the implementation
     * of <code>nev</code>.   If the matrix is symmetric, then the graph will
     * be constructed as a sparse undirected graph; otherwise, 
     * it will be constructed as a sparse directed graph.
     * 
     * @return a representation of <code>matrix</code> as a JUNG
     *         <code>Graph</code>
     */
    public static Graph matrixToGraph(DoubleMatrix2D matrix, NumberEdgeValue nev)
    {
        if (matrix.rows() != matrix.columns())
        {
            throw new IllegalArgumentException("Matrix must be square.");
        }
        int size = matrix.rows();
        boolean isSymmetric = true;
        outer: for (int i = 0; i < size; i++)
        {
            for (int j = 0; j < size; j++)
            {
                if (matrix.getQuick(i, j) != matrix.getQuick(j, i))
                {
                    isSymmetric = false;
                    break outer;
                }
            }
        }
        
        Graph graph;
        if (isSymmetric)
            graph = new UndirectedSparseGraph();
        else
            graph = new DirectedSparseGraph();
        
        GraphUtils.addVertices(graph, size);
        Indexer id = Indexer.getIndexer(graph);
        for (int i = 0; i < size; i++)
        {
            for (int j = 0; j < size; j++)
            {
                double value = matrix.getQuick(i, j);
                if (value != 0)
                {
                    Vertex vI = (Vertex) id.getVertex(i);
                    Vertex vJ = (Vertex) id.getVertex(j);
                    Edge e = null;
                    if (isSymmetric)
                    {
                        if (i <= j)
                            e = graph.addEdge(new UndirectedSparseEdge(vI, vJ));
                    }
                    else
                    {
                        e = graph.addEdge(new DirectedSparseEdge(vI, vJ));
                    }
                    if (e != null && nev != null)
                        nev.setNumber(e, new Double(value));
                }
            }
        }
        return graph;
    }
    
    /**
     * Creates a graph from a square (weighted) adjacency matrix.  
     * If the weight key is non-null then
     * the weight is stored as a Double in the given edge's user data under the
     * specified key name.  If the matrix is symmetric, then the graph will
     * be constructed as a sparse undirected graph; otherwise 
     * it will be constructed as a sparse directed graph.
     * 
     * @param weightKey the user data key to use to store or retrieve the edge weights
     * @return a representation of <code>matrix</code> as a JUNG <code>Graph</code>
     */
    public static Graph matrixToGraph(DoubleMatrix2D matrix, String weightKey)
    {
        if (weightKey == null)
            return matrixToGraph(matrix, (NumberEdgeValue)null);
        else
        {
            UserDatumNumberEdgeValue nev = new UserDatumNumberEdgeValue(weightKey);
            nev.setCopyAction(UserData.SHARED);
            return matrixToGraph(matrix, nev);
        }
    }

    
    
    /**
     * Creates a graph from a square (weighted) adjacency matrix.
     * Equivalent to <code>matrixToGraph(matrix, (NumberEdgeValue)null)</code>.
     *  
     * @return a representation of <code>matrix</code> as a JUNG <code>Graph</code>
     * 
     * @see #matrixToGraph(DoubleMatrix2D, NumberEdgeValue)
     */
    public static Graph matrixToGraph(DoubleMatrix2D matrix)
    {
        return matrixToGraph(matrix, (NumberEdgeValue)null);
    }
    
    /**
     * Returns a SparseDoubleMatrix2D which represents the edge weights of the
     * input Graph.
     * 
     * @return SparseDoubleMatrix2D
     */
    public static SparseDoubleMatrix2D graphToSparseMatrix(Graph g,
            Object edgeWeightKey)
    {
        if (edgeWeightKey == null)
            return graphToSparseMatrix(g);
        else
            return graphToSparseMatrix(g, new UserDatumNumberEdgeValue(edgeWeightKey));
    }
    
    public static SparseDoubleMatrix2D graphToSparseMatrix(Graph g)
    {
        return graphToSparseMatrix(g, new ConstantEdgeValue(1));
    }
    
    /**
     * Returns a SparseDoubleMatrix2D whose entries represent the edge weights for the
     * edges in <code>g</code>, as specified by <code>nev</code>.  
     * 
     * <p>The <code>(i,j)</code> entry of the matrix returned will be equal to the sum
     * of the weights of the edges connecting the vertex with index <code>i</code> to 
     * <code>j</code>.
     * 
     * <p>If <code>nev</code> is <code>null</code>, then a constant edge weight of 1 is used.
     * 
     * @param g
     * @param nev
     */
    public static SparseDoubleMatrix2D graphToSparseMatrix(Graph g, NumberEdgeValue nev)
    {
        if (nev == null)
            nev = new ConstantEdgeValue(1);
        int numVertices = g.getVertices().size();
        SparseDoubleMatrix2D matrix = new SparseDoubleMatrix2D(numVertices,
                numVertices);
        Indexer id = Indexer.getIndexer(g);
        for (int i = 0; i < numVertices; i++)
        {
            Vertex v = (Vertex)id.getVertex(i);
            for (Iterator o_iter = v.getOutEdges().iterator(); o_iter.hasNext(); )
            {
                Edge e = (Edge)o_iter.next();
                Vertex w = e.getOpposite(v);
                int j = id.getIndex(w);
                matrix.set(i, j, matrix.getQuick(i,j) + nev.getNumber(e).doubleValue());
            }
        }
        return matrix;
    }

    /**
     * Returns a diagonal matrix whose diagonal entries contain the degree for
     * the corresponding node.
     * 
     * @return SparseDoubleMatrix2D
     */
    public static SparseDoubleMatrix2D createVertexDegreeDiagonalMatrix(Graph G)
    {
        int numVertices = G.getVertices().size();
        SparseDoubleMatrix2D matrix = new SparseDoubleMatrix2D(numVertices,
                numVertices);
        Indexer id = Indexer.getIndexer(G);
        for (Iterator v_iter = G.getVertices().iterator(); v_iter.hasNext();)
        {
            Vertex v = (Vertex) v_iter.next();
            matrix.set(id.getIndex(v), id.getIndex(v), v.degree());
        }
        return matrix;
    }

    /**
     * The idea here is based on the metaphor of an electric circuit. We assume
     * that an undirected graph represents the structure of an electrical
     * circuit where each edge has unit resistance. One unit of current is
     * injected into any arbitrary vertex s and one unit of current is extracted
     * from any arbitrary vertex t. The voltage at some vertex i for source
     * vertex s and target vertex t can then be measured according to the
     * equation: V_i^(s,t) = T_is - T-it where T is the voltage potential matrix
     * returned by this method. *
     * 
     * @param graph
     *            an undirected graph representing an electrical circuit
     * @return the voltage potential matrix
     * @see "P. Doyle and J. Snell, 'Random walks and electric networks,', 1989"
     * @see "M. Newman, 'A measure of betweenness centrality based on random
     *      walks', pp. 5-7, 2003"
     */
    public static DoubleMatrix2D computeVoltagePotentialMatrix(
            UndirectedGraph graph)
    {
        int numVertices = graph.numVertices();
        //create adjacency matrix from graph
        DoubleMatrix2D A = GraphMatrixOperations.graphToSparseMatrix(graph,
                null);
        //create diagonal matrix of vertex degrees
        DoubleMatrix2D D = GraphMatrixOperations
                .createVertexDegreeDiagonalMatrix(graph);
        DoubleMatrix2D temp = new SparseDoubleMatrix2D(numVertices - 1,
                numVertices - 1);
        //compute D - A except for last row and column
        for (int i = 0; i < numVertices - 1; i++)
        {
            for (int j = 0; j < numVertices - 1; j++)
            {
                temp.set(i, j, D.get(i, j) - A.get(i, j));
            }
        }
        Algebra algebra = new Algebra();
        DoubleMatrix2D tempInverse = algebra.inverse(temp);
        DoubleMatrix2D T = new SparseDoubleMatrix2D(numVertices, numVertices);
        //compute "voltage" matrix
        for (int i = 0; i < numVertices - 1; i++)
        {
            for (int j = 0; j < numVertices - 1; j++)
            {
                T.set(i, j, tempInverse.get(i, j));
            }
        }
        return T;
    }

    /**
     * Converts a Map of (Vertex, Double) pairs to a DoubleMatrix1D.
     */
    public static DoubleMatrix1D mapTo1DMatrix(Map M)
    {
        int numVertices = M.size();
        DoubleMatrix1D vector = new DenseDoubleMatrix1D(numVertices);
        Set vertices = M.keySet();
        Indexer id = Indexer.getIndexer(((Vertex) vertices.iterator().next())
                .getGraph());
        for (Iterator v_iter = vertices.iterator(); v_iter.hasNext();)
        {
            Vertex v = (Vertex) v_iter.next();
            int v_id = id.getIndex(v);
            if (v_id < 0 || v_id > numVertices)
                throw new IllegalArgumentException("Vertex ID not "
                        + "supported by mapTo1DMatrix: outside range [0,n-1]");
            vector.set(v_id, ((Double) M.get(v)).doubleValue());
        }
        return vector;
    }

    /**
     * Computes the all-pairs mean first passage time for the specified graph,
     * given an existing stationary probability distribution.
     * <P>
     * The mean first passage time from vertex v to vertex w is defined, for a
     * Markov network (in which the vertices represent states and the edge
     * weights represent state->state transition probabilities), as the expected
     * number of steps required to travel from v to w if the steps occur
     * according to the transition probabilities.
     * <P>
     * The stationary distribution is the fraction of time, in the limit as the
     * number of state transitions approaches infinity, that a given state will
     * have been visited. Equivalently, it is the probability that a given state
     * will be the current state after an arbitrarily large number of state
     * transitions.
     * 
     * @param G
     *            the graph on which the MFPT will be calculated
     * @param edgeWeightKey
     *            the user data key for the edge weights
     * @param stationaryDistribution
     *            the asymptotic state probabilities
     * @return the mean first passage time matrix
     */
    public static DoubleMatrix2D computeMeanFirstPassageMatrix(Graph G,
            Object edgeWeightKey, DoubleMatrix1D stationaryDistribution)
    {
        DoubleMatrix2D temp = GraphMatrixOperations.graphToSparseMatrix(G,
                edgeWeightKey);
        for (int i = 0; i < temp.rows(); i++)
        {
            for (int j = 0; j < temp.columns(); j++)
            {
                double value = -1 * temp.get(i, j)
                        + stationaryDistribution.get(j);
                if (i == j)
                    value += 1;
                if (value != 0)
                    temp.set(i, j, value);
            }
        }
        Algebra algebra = new Algebra();
        DoubleMatrix2D fundamentalMatrix = algebra.inverse(temp);
        temp = new SparseDoubleMatrix2D(temp.rows(), temp.columns());
        for (int i = 0; i < temp.rows(); i++)
        {
            for (int j = 0; j < temp.columns(); j++)
            {
                double value = -1.0 * fundamentalMatrix.get(i, j);
                value += fundamentalMatrix.get(j, j);
                if (i == j)
                    value += 1;
                if (value != 0)
                    temp.set(i, j, value);
            }
        }
        DoubleMatrix2D stationaryMatrixDiagonal = new SparseDoubleMatrix2D(temp
                .rows(), temp.columns());
        int numVertices = stationaryDistribution.size();
        for (int i = 0; i < numVertices; i++)
            stationaryMatrixDiagonal.set(i, i, 1.0 / stationaryDistribution
                    .get(i));
        DoubleMatrix2D meanFirstPassageMatrix = algebra.mult(temp,
                stationaryMatrixDiagonal);
        return meanFirstPassageMatrix;
    }
}