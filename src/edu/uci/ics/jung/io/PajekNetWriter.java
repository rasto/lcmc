/*
 * Created on May 4, 2004
 *
 * Copyright (c) 2004, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.io;

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.uci.ics.jung.graph.DirectedEdge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedEdge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.ConstantEdgeValue;
import edu.uci.ics.jung.graph.decorators.Indexer;
import edu.uci.ics.jung.graph.decorators.NumberEdgeValue;
import edu.uci.ics.jung.graph.decorators.VertexStringer;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.utils.PredicateUtils;
import edu.uci.ics.jung.visualization.VertexLocationFunction;


/**
 * Writes graphs in the Pajek NET format.
 * 
 * <p>Labels for vertices may optionally be specified by implementations of 
 * <code>VertexStringer</code>.  Edge weights are optionally specified by 
 * implementations of <code>NumberEdgeValue</code>.  Vertex locations
 * are optionally specified by implementations of 
 * <code>VertexLocationFunction</code>.  Note that vertex location coordinates 
 * must be normalized to the interval [0, 1] on each axis in order to conform to the 
 * Pajek specification.</p>
 * 
 * @author Joshua O'Madadhain
 */
public class PajekNetWriter
{
    public PajekNetWriter()
    {
    }

    /**
     * Saves <code>g</code> to <code>filename</code>.  Labels for vertices may
     * be supplied by <code>vs</code>.  Edge weights are specified by <code>nev</code>.
     * 
     * @see #save(Graph, Writer, VertexStringer, NumberEdgeValue, VertexLocationFunction)
     * @throws IOException
     */
    public void save(Graph g, String filename, VertexStringer vs, 
            NumberEdgeValue nev, VertexLocationFunction vld) throws IOException
    {
        save(g, new FileWriter(filename), vs, nev, vld);
    }
    
    public void save(Graph g, String filename, VertexStringer vs, 
            NumberEdgeValue nev) throws IOException
    {
        save(g, new FileWriter(filename), vs, nev, null);
    }
    
    /**
     * Saves <code>g</code> to <code>filename</code>; no vertex labels are written out,
     * and the edge weights are written as 1.0.
     * 
     * @throws IOException
     */
    public void save(Graph g, String filename) throws IOException
    {
        save(g, filename, null, null, null);
    }

    /**
     * Saves <code>g</code> to <code>w</code>; no vertex labels are written out,
     * and the edge weights are written as 1.0.
     * 
     * @throws IOException
     */
    public void save(Graph g, Writer w) throws IOException
    {
        save(g, w, null, null, null);
    }

    public void save(Graph g, Writer w, VertexStringer vs, 
            NumberEdgeValue nev) throws IOException
    {
        save(g, w, vs, nev, null);
    }
    
    /**
     * Writes <code>graph</code> to <code>w</code>.  Labels for vertices may
     * be supplied by <code>vs</code> (defaults to no labels if null), 
     * edge weights may be specified by <code>nev</code>
     * (defaults to weights of 1.0 if null), 
     * and vertex locations may be specified by <code>vld</code> (defaults
     * to no locations if null). 
     */
    public void save(Graph graph, Writer w, VertexStringer vs, NumberEdgeValue nev, VertexLocationFunction vld) throws IOException
    {
        /*
         * TODO: Changes we might want to make:
         * - optionally writing out in list form
         */
        
        BufferedWriter writer = new BufferedWriter(w);
        if (nev == null)
            nev = new ConstantEdgeValue(1);
        writer.write("*Vertices " + graph.numVertices());
        writer.newLine();
        Vertex currentVertex = null;

//        if (vld != null)
//            vld = VertexLocationUtils.scale(vld, 1.0, 1.0);
        
        Indexer id = Indexer.getIndexer(graph);
        for (Iterator i = graph.getVertices().iterator(); i.hasNext();)
        {
            currentVertex = (Vertex) i.next();
            // convert from 0-based to 1-based index
            Integer v_id = new Integer(id.getIndex(currentVertex) + 1);
            writer.write(v_id.toString()); 
            if (vs != null)
            { 
                String label = vs.getLabel(currentVertex);
                if (label != null)
                    writer.write (" \"" + label + "\"");
            }
            if (vld != null)
            {
                Point2D location = vld.getLocation(currentVertex);
                if (location != null)
                    writer.write (" " + location.getX() + " " + location.getY() + " 0.0");
            }
            writer.newLine();
        }

        Set d_set = new HashSet();
        Set u_set = new HashSet();

        boolean directed = PredicateUtils.enforcesDirected(graph);
        boolean undirected = PredicateUtils.enforcesUndirected(graph);
        // if it's strictly one or the other, no need to create extra sets
        if (directed)
            d_set = graph.getEdges();
        if (undirected)
            u_set = graph.getEdges();
        if (!directed && !undirected) // mixed-mode graph
        {
            d_set = PredicateUtils.getEdges(graph, Graph.DIRECTED_EDGE);
            u_set = PredicateUtils.getEdges(graph, Graph.UNDIRECTED_EDGE);
        }

        // write out directed edges
        if (!d_set.isEmpty())
        {
            writer.write("*Arcs");
            writer.newLine();
        }
        for (Iterator eIt = d_set.iterator(); eIt.hasNext();)
        {
            DirectedEdge e = (DirectedEdge) eIt.next();
            int source_id = id.getIndex(e.getSource()) + 1;
            int target_id = id.getIndex(e.getDest()) + 1;
            float weight = nev.getNumber(e).floatValue();
            writer.write(source_id + " " + target_id + " " + weight);
            writer.newLine();
        }

        // write out undirected edges
        if (!u_set.isEmpty())
        {
            writer.write("*Edges");
            writer.newLine();
        }
        for (Iterator eIt = u_set.iterator(); eIt.hasNext();)
        {
            UndirectedEdge e = (UndirectedEdge) eIt.next();
            Pair endpoints = e.getEndpoints();
            int v1_id = id.getIndex((Vertex) endpoints.getFirst()) + 1;
            int v2_id = id.getIndex((Vertex) endpoints.getSecond()) + 1;
            float weight = nev.getNumber(e).floatValue();
            writer.write(v1_id + " " + v2_id + " " + weight);
            writer.newLine();
        }
        writer.close();
    }
}
