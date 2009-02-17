/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.DirectedEdge;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedEdge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.Indexer;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.graph.impl.DirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.utils.GraphUtils;
import edu.uci.ics.jung.utils.MutableDouble;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.utils.PredicateUtils;
import edu.uci.ics.jung.utils.UserData;

/**
 * A file reader for Pajek .net files. At the moment, only supports the 
 * part of the specification that defines: 
 * <ul>
 * <li> node ids (must be ordered from 1 to n)
 * <li> node labels (must be in quotes)
 * <li> directed edge connections (single or list)
 * <li> undirected edge connections (single or list)
 * <li> edge weights (1 or more can be specified; not compatible with 
 * edges specified in list form) 
 * </ul> <p>
 *
 * Here is an example format for a directed graph without edge weights 
 * and edges specified in list form: <br>
 * <pre>
 * *vertices <# of vertices> 
 * 1 "a" 
 * 2 "b" 
 * 3 "c" 
 * *arcslist 
 * 1 2 3 
 * 2 3  
 * </pre>
 *
 * Here is an example format for an undirected graph with edge weights 
 * and edges specified in non-list form: <br>
 * <pre>
 * *vertices <# of vertices> 
 * 1 "a" 
 * 2 "b" 
 * 3 "c" 
 * *edges 
 * 1 2 0.1 
 * 1 3 0.9 
 * 2 3 1.0 
 * </pre>
 * @author Scott White, Joshua O'Madadhain
 * @deprecated As of version 1.4, replaced by {@link PajekNetReader} and {@link PajekNetWriter}
 * @see "'Pajek - Program for Large Network Analysis', Vladimir Batagelj and Andrej Mrvar, www.ucm.es/info/pecar/pajek.pdf"
 */
public class PajekNetFile implements GraphFile {
    public final static String EDGE_WEIGHT = "jung.io.PajekNetFile.EdgeWeight";
    private String[] mEdgeKeys;
    private boolean mCreateDirectedOnly;

    /**
     * Default constructor for pajek graph reader
     */
    public PajekNetFile() {
        mCreateDirectedOnly = false;
    }

    /**
     * Constructor which takes in the user datum keys for the edge weights
     * @param edgeKeys the user datum keys the algorithm should use to store the edge weights (as MutableDoubles)
     */
    public PajekNetFile(String[] edgeKeys) {
        mEdgeKeys = edgeKeys;
        mCreateDirectedOnly = false;
    }

    /**
     * retrieves the set of edge keys the algorithm will use to store the edge weights
     * @return the user datum keys the algorithm should is using to store the edge weights (as MutableDoubles)
     */
    public String[] getEdgeKeys() {
        return mEdgeKeys;
    }

    /**
     * set the edge the algorithm will use to store the edge weights
     * @param edgeKeys the user datum keys the algorithm should use to store the edge weights (as MutableDoubles)
     */
    public void setEdgeKeys(String[] edgeKeys) {
        this.mEdgeKeys = edgeKeys;
    }

    /**
     * Loads a graph from disk for the given .net file
     * If the edges are directed then a directed graph will be created, otherwise an undirected graph will be created
     * @param filename the fully specified file name of the pajek .net file
     * @return the corresponding graph
     */
    public Graph load(String filename) {
        try {
            Reader reader = new FileReader(filename);
            Graph graph = load(reader);
            reader.close();
            return graph;
        } catch (IOException ioe) {
            throw new FatalException("Error in loading file " + filename, ioe);
        }
    }

    /**
     * Forces a graph that is normally undirected to be loaded in as its directed equivalent
     * @param createDirectedOnly if true, force graph to be directed, false to not force this constraint
     */
    public void setCreateDirectedOnly(boolean createDirectedOnly) {
        mCreateDirectedOnly = createDirectedOnly;
    }

    /**
     * Loads a graph for the given BufferedReader (where the data is assumed to be in Pajek NET format).
     * If the edges are directed then a directed graph will be created, otherwise an undirected 
     * graph will be created.
     * @param read the data stream that contains the graph data in .net format
     * @return the corresponding graph
     */
    public Graph load(Reader read) {

    	/*
    	 * Current running buglist: 
    	 *  * Crashes on *Network tag
    	 *  * unique label exception is possible
    	 *  * doesn't handle *PArtition, e.g.
//    	 *  * only one tag of "arc" or "edge"
    	 */
    	BufferedReader reader = new BufferedReader( read );
        int currentSourceId = -1;
        String currentLine = null;
        try {
            StringTokenizer tokenizer = null;
            currentLine = reader.readLine();
            tokenizer = new StringTokenizer(currentLine);
            if (!tokenizer.nextToken().toLowerCase().startsWith("*vertices")) {
                throw new ParseException("Pajek file parse error: '*vertices' not first token", 0);
            }
            int numVertices = Integer.parseInt(tokenizer.nextToken());
            Graph directedGraph = new DirectedSparseGraph();
            GraphUtils.addVertices( directedGraph, numVertices );
            Indexer directedGraphIndexer = Indexer.newIndexer( directedGraph , 1);

            Graph undirectedGraph = null;
            Indexer undirectedGraphIndexer = null;
            StringLabeller undirectedLabeler = null;

            if (!mCreateDirectedOnly) {
                undirectedGraph = new UndirectedSparseGraph();
                GraphUtils.addVertices(undirectedGraph,numVertices);
                undirectedGraphIndexer = Indexer.newIndexer( undirectedGraph , 1);
                undirectedLabeler = StringLabeller.getLabeller(undirectedGraph);
            }

            StringLabeller directedLabeler = StringLabeller.getLabeller(directedGraph);

            String currentVertexLabel = null;
            Vertex currentVertex = null;

//            currentLine = reader.readLine().trim();
            while (!(currentLine = reader.readLine().trim()).startsWith("*")) {
                tokenizer = new StringTokenizer(currentLine);
                currentSourceId = Integer.parseInt(tokenizer.nextToken());

                int openQuotePos = currentLine.indexOf("\"");
                int closeQuotePos = currentLine.lastIndexOf("\"");

                if ((openQuotePos > 0) && (openQuotePos != closeQuotePos)) {
                    currentVertexLabel = currentLine.substring(openQuotePos+1,closeQuotePos);
                    currentVertex = (Vertex)directedGraphIndexer.getVertex(currentSourceId);
                    directedLabeler.setLabel(currentVertex,currentVertexLabel);

                    if (!mCreateDirectedOnly) {
                        currentVertex = (Vertex)undirectedGraphIndexer.getVertex(currentSourceId);
                        undirectedLabeler.setLabel(currentVertex,currentVertexLabel);
                    }
                    continue;
                }
            }

            int currentTargetId = -1;
            boolean directed = false;
            boolean isList = false;
            Graph graph = null;
            Indexer id = null;
            if (currentLine.toLowerCase().indexOf("list") >= 0) {
                isList = true;
            }
            if (currentLine.toLowerCase().indexOf("arc") >= 0) {
                directed = true;
                graph = directedGraph;
                undirectedGraph = null;
                undirectedLabeler = null;
                id = directedGraphIndexer;
            } else {
                if (mCreateDirectedOnly) {
                    graph = directedGraph;
                    undirectedGraph = null;
                    undirectedGraphIndexer = null;
                    directedLabeler = null;
                    id = directedGraphIndexer;
                } else {
                    graph = undirectedGraph;
                    directedGraph = null;
                    directedGraphIndexer = null;
                    id = undirectedGraphIndexer;
                }
            }

            while ((currentLine = reader.readLine()) != null) 
            {
                currentLine = currentLine.trim();
                if (currentLine.length() == 0) {
                    break;
                }
                // right now we only support strictly directed or strictly undirected graphs
                
                if (currentLine.startsWith("*" ))
                    continue;
//              else if (currentLine.startsWith("*")) {
//              throw new FatalException("*edge/arcs(list) can only appear once.");
//              }
                tokenizer = new StringTokenizer(currentLine);
                currentSourceId = Integer.parseInt(tokenizer.nextToken());
                Edge currentEdge1 = null;
                Edge currentEdge2 =  null;
                
                while (tokenizer.hasMoreTokens()) {
                    currentTargetId = Integer.parseInt(tokenizer.nextToken());
                    
                    if (currentSourceId == currentTargetId) {
                        break;
                    }
                    
                    Vertex source = (Vertex)id.getVertex(currentSourceId);
                    Vertex target = (Vertex)id.getVertex(currentTargetId);
                    if(!source.isPredecessorOf(target))   {
                        currentEdge1 = GraphUtils.addEdge( graph, source,target);
                    }
                    
                    if (mCreateDirectedOnly && !directed) {
                        if(!target.isPredecessorOf(source))   {
                            currentEdge2 = GraphUtils.addEdge( graph, target,source);
                        }
                    }
                    
                    String[] edgeKeys = getEdgeKeys();
                    if (!isList && edgeKeys != null) {
                        int numTokens = tokenizer.countTokens();
                        numTokens = Math.min(numTokens,edgeKeys.length);
                        for (int edgeDescIdx=0;edgeDescIdx<numTokens;edgeDescIdx++) {
                            double val = Double.parseDouble(tokenizer.nextToken());
                            currentEdge1.setUserDatum(edgeKeys[edgeDescIdx],new MutableDouble(val),UserData.SHARED);
                            if (currentEdge2 != null) {
                                currentEdge2.setUserDatum(edgeKeys[edgeDescIdx],new MutableDouble(val),UserData.SHARED);
                            }
                            
                        }
                    } else if (isList) {
                        continue;
                    }
                    break;
                }
            }
            return graph;
        } 
        catch (IOException ioe)
        {
            throw new FatalException("I/O error in reading file: ", ioe);
        }
        catch (ParseException pe)
        {
            throw new FatalException("Parse exception in reading graph", pe);
        }
        catch (StringLabeller.UniqueLabelException sle)
        {
            throw new FatalException("Repeated vertex label in graph", sle);
        }
//        catch (Exception re) {
//            throw new FatalException("Fatal exception calling PajekNetFile.load(...)", re);
//        }
    }

    /**
     * Writes <code>graph</code> to the file specified by <code>filename</code>
     * in the Pajek NET format.
     * @param graph the graph to save
     * @param filename the fully specified file name where the graph is to be saved to disk
     */
    public void save(Graph graph,String filename) {
        
        /*
         * TODO: Changes we might want to make:
         * - convert to writing out with a Writer, not a String filename spec
         * - optionally writing out in list form
         */
        
        try {
            BufferedWriter writer = new BufferedWriter( new FileWriter(filename));
            writer.write("*Vertices " + graph.getVertices().size() + "\n");
            Vertex currentVertex = null;

            Indexer id = Indexer.newIndexer( graph ,1 ) ;
            StringLabeller labeller = StringLabeller.getLabeller(graph);
            for (Iterator i = graph.getVertices().iterator(); i.hasNext();) {
                currentVertex =  (Vertex) i.next();
                if (labeller.getLabel(currentVertex) != null) {
                    writer.write(id.getIndex(currentVertex) + " \"" + labeller.getLabel(currentVertex) + "\"\n");
                } else {
                    writer.write(id.getIndex(currentVertex) + "\n");
                }
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
            if (! d_set.isEmpty())
                writer.write("*Arcs\n");
            for (Iterator eIt = d_set.iterator(); eIt.hasNext();) 
            {
                DirectedEdge currentEdge = (DirectedEdge) eIt.next();
                Vertex source = currentEdge.getSource();
                Vertex target = currentEdge.getDest();
                writer.write(id.getIndex(source) + " " + id.getIndex(target) + "\n");
            }

            // write out undirected edges
            if (! u_set.isEmpty())
                writer.write("*Edges\n");
            for (Iterator eIt = u_set.iterator(); eIt.hasNext();) 
            {
                UndirectedEdge e = (UndirectedEdge)eIt.next();
                Pair endpoints = e.getEndpoints();
                Vertex v1 = (Vertex)endpoints.getFirst();
                Vertex v2 = (Vertex)endpoints.getSecond();
                writer.write(id.getIndex(v1) + " " + id.getIndex(v2) + "\n");
            }
            
            writer.close();
            
        } catch (Exception e) {
            throw new FatalException("Error saving file: " + filename,e);
        }
    }

}
