/*
 * Created on Jul 8, 2005
 *
 * Copyright (c) 2005, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import edu.uci.ics.jung.graph.Hyperedge;
import edu.uci.ics.jung.graph.Hypergraph;
import edu.uci.ics.jung.graph.Hypervertex;
import edu.uci.ics.jung.graph.impl.SetHyperedge;
import edu.uci.ics.jung.graph.impl.SetHypergraph;
import edu.uci.ics.jung.graph.impl.SetHypervertex;
import edu.uci.ics.jung.utils.UserData;
import edu.uci.ics.jung.utils.UserDataContainer.CopyAction;

/**
 * 
 * 
 * @author Joshua O'Madadhain
 */
public class HypergraphReader
{
    public static final Object LABEL = "edu.ics.uci.jung.io.HypergraphReader.LABEL";
    
    protected boolean verbose = false;
    
    /**
     * <p>If <code>true</code>, specifies that each line of input implicitly 
     * represents a list of edges, where the first token specifies one endpoint,
     * and the subsequent tokens specify a sequence of opposite endpoints.
     * Otherwise, each line of input represents a single edge.</p>
     */
    protected boolean as_list;
    
    
    protected boolean edge_first;
    protected CopyAction copy_action;

    /**
     * 
     * @param as_list
     * @param edge_first
     * @param copy_action
     */
    public HypergraphReader(boolean as_list, boolean edge_first, CopyAction copy_action)
    {
        this.as_list = as_list;
        this.edge_first = edge_first;
        this.copy_action = copy_action;
    }
    
    public HypergraphReader()
    {
        this(false, false, UserData.SHARED);
    }
    
    /**
     * 
     * @param reader
     * @return the hypergraph represented by the reader
     * @throws IOException
     */
    public Hypergraph load(Reader reader) throws IOException
    {
        Hypergraph h = new SetHypergraph();
        BufferedReader br = new BufferedReader(reader);
        Map vertex_names = new HashMap();
        Map edge_names = new HashMap();
        
        while (br.ready())
        {
            String curLine = br.readLine();
            if (curLine == null || curLine.equals("end_of_file"))
                break;
            if (curLine.trim().length() == 0)
                continue;
            String[] parts;
            
            if (as_list)
                parts = curLine.trim().split("\\s+");
            else
                parts = curLine.trim().split("\\s+", 2);

            if (edge_first)
            {
                Hyperedge e = (Hyperedge)edge_names.get(parts[0]);
                if (e == null)
                {
                    e = new SetHyperedge();
                    h.addEdge(e);
                    edge_names.put(parts[0], e);
                    e.addUserDatum(LABEL, parts[0], copy_action);
                }
                for (int i = 1; i < parts.length; i++)
                {
                    Hypervertex v = (Hypervertex)vertex_names.get(parts[i]);
                    if (v == null)
                    {
                        v = new SetHypervertex();
                        h.addVertex(v);
                        vertex_names.put(parts[i], v);
                        v.addUserDatum(LABEL, parts[i], copy_action);
                    }
                    if (!e.isIncident(v))
                        e.connectVertex(v);
                    else
                        if (verbose)
                            System.out.println("duplicate line: " + curLine);
                }
            }
            else // vertex first, then edge(s)
            {
                Hypervertex v = (Hypervertex)vertex_names.get(parts[0]);
                if (v == null)
                {
                    v = new SetHypervertex();
                    h.addVertex(v);
                    vertex_names.put(parts[0], v);
                    v.addUserDatum(LABEL, parts[0], copy_action);
                }
                for (int i = 1; i < parts.length; i++)
                {
                    Hyperedge e = (Hyperedge)edge_names.get(parts[i]);
                    if (e == null)
                    {
                        e = new SetHyperedge();
                        h.addEdge(e);
                        edge_names.put(parts[i], e);
                        e.addUserDatum(LABEL, parts[i], copy_action);
                    }
                    if (!e.isIncident(v))
                        e.connectVertex(v);
                    else
                        if (verbose)
                            System.out.println("duplicate line: " + curLine);
                }
            }

        }
        
        return h;
    }
    
    public void setVerboseMode(boolean b)
    {
        verbose = b;
    }
}
