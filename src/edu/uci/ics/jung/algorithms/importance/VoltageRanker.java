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
 * Created on Aug 11, 2004
 *
 */
package edu.uci.ics.jung.algorithms.importance;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.ConstantEdgeValue;
import edu.uci.ics.jung.graph.decorators.Indexer;
import edu.uci.ics.jung.graph.decorators.NumberEdgeValue;
import edu.uci.ics.jung.graph.decorators.NumberVertexValue;


/**
 * Ranks vertices in a graph according to their 'voltage' in an approximate 
 * solution to the Kirchoff equations.  This is accomplished by tying "source"
 * vertices to specified positive voltages, "sink" vertices to 0 V, and 
 * iteratively updating the voltage of each other vertex to the (weighted) 
 * average of the voltages of its neighbors.
 * 
 * <p>The resultant voltages will all be in the range <code>[0, max]</code>
 * where <code>max</code> is the largest voltage of any source vertex (in the
 * absence of negative source voltages; see below).
 * 
 * <p>A few notes about this algorithm's interpretation of the graph data: 
 * <ul>
 * <li/>Higher edge weights are interpreted as indicative of greater 
 * influence/effect than lower edge weights.  
 * <li/>Negative edge weights (and negative "source" voltages) invalidate
 * the interpretation of the resultant values as voltages.  However, this 
 * algorithm will not reject graphs with negative edge weights or source voltages.
 * <li/>Parallel edges are equivalent to a single edge whose weight is the 
 * sum of the weights on the parallel edges.
 * <li/>Current flows along undirected edges in both directions, 
 * but only flows along directed edges in the direction of the edge.
 * </ul>
 * </p> 
 * 
 * @author Joshua O'Madadhain
 */
public class VoltageRanker
{
    protected NumberEdgeValue edge_weights;
    protected NumberVertexValue voltages;
    protected int max_iterations;
    protected double convergence_threshold;
    
    /**
     * Creates an instance of <code>VoltageRanker</code> which uses the 
     * edge weights specified by <code>edge_weights</code>, and which stores
     * the voltages (ranks) as specified by <code>voltages</code>.
     */
    public VoltageRanker(NumberEdgeValue edge_weights, NumberVertexValue voltages,
        int num_iterations, double convergence_threshold)
    {
        if (num_iterations < 1)
            throw new IllegalArgumentException("num_iterations must be >= 1");
        
        if (convergence_threshold < 0)
            throw new IllegalArgumentException("convergence_threshold must be >= 0");

        this.edge_weights = edge_weights;
        this.voltages = voltages;
        this.max_iterations = num_iterations;
        this.convergence_threshold = convergence_threshold;
    }

    /**
     * Creates an instance of <code>VoltageRanker</code> which treats the
     * edges as though they were unweighted, and which stores
     * the voltages (ranks) as specified by <code>voltages</code>.
     */
    public VoltageRanker(NumberVertexValue voltages, int num_iterations, 
        double threshold)
    {
        this(new ConstantEdgeValue(1), voltages, num_iterations, threshold);
    }
    
    /**
     * Calculates the voltages for <code>g</code> based on assigning each of the 
     * vertices in <code>source</code> a voltage of 1 V.
     * @param sources   vertices tied to 1 V
     * @param sinks     vertices tied to 0 V
     * @see #calculateVoltages(Graph, Map, Set)
     */
    public void calculateVoltages(Graph g, Set sources, Set sinks)
    {
        if (sources == null || sources.isEmpty() || 
            sinks == null || sinks.isEmpty())
            throw new IllegalArgumentException("at least one source and one " +
                    "sink must exist");

        if (sources.size() + sinks.size() > g.numVertices())
            throw new IllegalArgumentException("either sources and sinks overlap " + 
                "or sources and sinks contain vertices not in g");
        
        Map unit_sources = new HashMap();
        for (Iterator iter = sources.iterator(); iter.hasNext(); )
            unit_sources.put(iter.next(), new Double(1.0));
        
        calculateVoltages(g, unit_sources, sinks);
    }
    
    /**
     * Calculates the voltages for <code>g</code> based on the specified source
     * and sink vertex sets.
     * 
     * @param g                 the graph for which voltages will be calculated
     * @param source_voltages   a map from vertices to source voltage values
     * @param sinks             a set of vertices to tie to 0 V
     */
    public void calculateVoltages(Graph g, Map source_voltages, Set sinks)
    {
        if (source_voltages == null || source_voltages.isEmpty() || 
            sinks == null || sinks.isEmpty())
            throw new IllegalArgumentException("at least one source and one " +
                "sink must exist");
        
        if (source_voltages.size() + sinks.size() > g.numVertices())
            throw new IllegalArgumentException("either sources and sinks overlap " + 
                "or sources and sinks contain vertices not in g");

        Set sources = source_voltages.keySet();
        
        // set up initial voltages
        Indexer id = Indexer.getIndexer(g);
        Set vertices = g.getVertices();
        double[] volt_array = new double[vertices.size()];
        for (int i = 0; i < volt_array.length; i++)
        {
            Vertex v = (Vertex)id.getVertex(i);
            if (sources.contains(v))
            {
                Number voltage = (Number)source_voltages.get(v);
                volt_array[i] = voltage.doubleValue();
                voltages.setNumber(v, voltage);
            }
            else
            {
                volt_array[i] = 0;
                voltages.setNumber(v, new Double(0));
            }
        }
        
        // update voltages of each vertex to the (weighted) average of its 
        // neighbors, until either (a) the number of iterations exceeds the
        // maximum number of iterations specified, or (b) the largest change of
        // any voltage is no greater than the specified convergence threshold. 
        int iteration = 0;
        double max_change = Double.POSITIVE_INFINITY;
        while (iteration++ < max_iterations && max_change > convergence_threshold)
        {
            max_change = 0;
            for (Iterator iter = vertices.iterator(); iter.hasNext(); )
            {
                Vertex v = (Vertex)iter.next();
                if (sources.contains(v) || sinks.contains(v))
                    continue;
                Set edges = v.getInEdges();
                double voltage_sum = 0;
                double weight_sum = 0;
                for (Iterator e_iter = edges.iterator(); e_iter.hasNext(); )
                {
                    Edge e = (Edge)e_iter.next();
                    Vertex w = e.getOpposite(v);
                    double weight = edge_weights.getNumber(e).doubleValue();
                    voltage_sum += volt_array[id.getIndex(w)] * weight;
                    weight_sum += weight;
                }
                
                double new_voltage;
                if (voltage_sum == 0 && weight_sum == 0)
                    new_voltage = 0;
                else
                    new_voltage = voltage_sum / weight_sum;
                max_change = Math.max(max_change, 
                    Math.abs(voltages.getNumber(v).doubleValue() - new_voltage));
                voltages.setNumber(v, new Double(new_voltage));
            }
            
            // set up volt_array for next iteration
            for (int i = 0; i < volt_array.length; i++)
                volt_array[i] = voltages.getNumber(id.getVertex(i)).doubleValue();
        }
    }
        
    
    /**
     * Calculates an approximation of the solution of the Kirchhoff equations
     * for voltage, given that <code>source</code> supplies 1 V and <code>target</code>
     * is tied to ground (O V).  Each other vertex will be assigned a voltage (rank) 
     * in the range [0,1].
     * 
     * @param source            the vertex whose voltage is tied to 1 V
     * @param target            the vertex whose voltage is tied to 0 V
     */
    public void calculateVoltages(Vertex source, Vertex target)
    {
        Set sources = new HashSet();
        Set sinks = new HashSet();
        sources.add(source);
        sinks.add(target);
        calculateVoltages((Graph)source.getGraph(), sources, sinks);
    }
}
