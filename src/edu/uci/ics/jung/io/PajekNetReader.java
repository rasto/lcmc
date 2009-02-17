/*
 * Created on May 3, 2004
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
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.StringTokenizer;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.OrPredicate;

import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.Indexer;
import edu.uci.ics.jung.graph.decorators.NumberEdgeValue;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.SparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.predicates.ParallelEdgePredicate;
import edu.uci.ics.jung.utils.PredicateUtils;
import edu.uci.ics.jung.utils.TypedVertexGenerator;
import edu.uci.ics.jung.utils.UserData;
import edu.uci.ics.jung.utils.VertexGenerator;
import edu.uci.ics.jung.visualization.DefaultSettableVertexLocationFunction;
import edu.uci.ics.jung.visualization.SettableVertexLocationFunction;


/**
 * Reads a <code>Graph</code> from a Pajek NET formatted source.
 * 
 * <p>If the edge constraints specify that the graph is strictly undirected,
 * and an "*Arcs" section is encountered, or if the edge constraints specify that the 
 * graph is strictly directed, and an "*Edges" section is encountered,
 * an <code>IllegalArgumentException</code> is thrown.</p>
 * 
 * <p>If the edge constraints do not permit parallel edges, only the first encountered
 * of a set of parallel edges will be read; subsequent edges in that set will be ignored.</p>
 * 
 * <p>More restrictive edge constraints will cause vertices to be generated
 * that are more time- and space-efficient.</p>
 * 
 * At the moment, only supports the 
 * part of the specification that defines: 
 * <ul>
 * <li> vertex ids (each must have a value from 1 to n, where n is the number of vertices)
 * <li> vertex labels (must be in quotes if interrupted by whitespace)
 * <li> directed edge connections (single or list)
 * <li> undirected edge connections (single or list)
 * <li> edge weights (not compatible with edges specified in list form)
 * <br><b>note</b>: this version of PajekNetReader does not support multiple edge 
 * weights, as PajekNetFile does; this behavior is consistent with the NET format. 
 * <li/> vertex locations (x and y; z coordinate is ignored)
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
 * 
 * @author Joshua O'Madadhain
 * @see "'Pajek - Program for Analysis and Visualization of Large Networks', Vladimir Batagelj and Andrej Mrvar, http://vlado.fmf.uni-lj.si/pub/networks/pajek/doc/pajekman.pdf"
 */
public class PajekNetReader
{
    protected boolean unique_labels;

    /**
     * The key used to identify the vertex labels (if any) created by this class.
     */
    public static final String LABEL = "jung.io.PajekNetReader.LABEL";

    /**
     * The user data key used to retrieve the vertex locations (if any) defined by this class.
     */
    public static final String LOCATIONS = "jung.io.PajekNetReader.LOCATIONS";
    
    protected SettableVertexLocationFunction v_locations;
    protected boolean get_locations = false;
    
    /**
     * Used to specify whether the most recently read line is a 
     * Pajek-specific tag.
     */
    private static final Predicate v_pred = new TagPred("*vertices");
    private static final Predicate a_pred = new TagPred("*arcs");
    private static final Predicate e_pred = new TagPred("*edges");
    private static final Predicate t_pred = new TagPred("*");
    private static final Predicate c_pred = OrPredicate.getInstance(a_pred, e_pred);
    protected static final Predicate l_pred = ListTagPred.getInstance();
    protected static final Predicate p_pred = ParallelEdgePredicate.getInstance();
    
    /**
     * Creates a PajekNetReader with the specified labeling behavior, which does not 
     * read location information (if any).
     * 
     * @see #PajekNetReader(boolean, boolean)
     */
    public PajekNetReader(boolean unique_labels)
    {
        this(unique_labels, false);
    }
    
    /**
     * Creates a PajekNetReader with the specified labeling behavior and 
     * location assignment behavior.
     * 
     * <p>If <code>unique_labels</code> is true, vertices will be labelled 
     * using a <code>StringLabeller</code> with key <code>jung.io.PajekNetReader.LABEL</code>.
     * Otherwise, they will be labeled with a user data <code>String</code> with key 
     * <code>PajekNetReader.LABEL</code>.  (Vertices that have no apparent label
     * information will not be labelled.)</p>
     * 
     * <p>If <code>get_locations</code> is true, each vertex line in the file
     * will be assumed to contain (x,y) coordinates in the range [0,1]; if any line
     * lacks this data, an <code>IllegalArgumentException</code> will be thrown.  (The Pajek
     * format assumes coordinates are (x,y,z) but we ignore the z-coordinate.)  Location
     * data will be stored in a <code>SettabelVertexLocationDecorator</code> instance
     * in the graph's user data with key<code>jung.io.PajekNetReader.LOCATIONS</code>.</p>
     */
    public PajekNetReader(boolean unique_labels, boolean get_locations)
    {
        this.unique_labels = unique_labels;
        this.get_locations = get_locations;
        if (get_locations)
            this.v_locations = new DefaultSettableVertexLocationFunction();
    }
    
    /**
     * Creates a PajekNetReader with the specified labeling behavior and 
     * location assignment behavior.
     * 
     * <p>If <code>unique_labels</code> is true, vertices will be labelled 
     * using a <code>StringLabeller</code> with key <code>jung.io.PajekNetReader.LABEL</code>.
     * Otherwise, they will be labeled with a user data <code>String</code> with key 
     * <code>PajekNetReader.LABEL</code>.  (Vertices that have no apparent label
     * information will not be labelled.)</p>
     * 
     * <p>If <code>get_locations</code> is true, each vertex line in the file
     * will be assumed to contain (x,y) coordinates in the range [0,1]; if any line
     * lacks this data, an <code>IllegalArgumentException</code> will be thrown.  (The Pajek
     * format assumes coordinates are (x,y,z) but we ignore the z-coordinate.)  Location
     * data will be stored in <code>v_locations</code>, a reference to which will be
     * stored in the graph's user data with key <code>jung.io.PajekNetReader.LOCATIONS</code>.</p>
     */
    public PajekNetReader(boolean unique_labels, SettableVertexLocationFunction v_locations)
    {
        this.unique_labels = unique_labels;
        this.get_locations = true;
        this.v_locations = v_locations;
    }
    
    /**
     * Creates a PajekNetReader whose labels are not required to be unique.
     */
    public PajekNetReader()
    {
        this(false, false);
    }
    
    /**
     * Returns <code>load(filename, new SparseGraph(), null)</code>.
     * @throws IOException
     */
    public Graph load(String filename) throws IOException
    {
        return load(filename, new SparseGraph(), null);
    }

    /**
     * Returns <code>load(filename, new SparseGraph(), nev)</code>.
     * @throws IOException
     */
    public Graph load(String filename, NumberEdgeValue nev) throws IOException
    {
        return load(filename, new SparseGraph(), nev);
    }
    
    /**
     * Returns <code>load(filename, g, null)</code>.
     * @throws IOException
     */
    public Graph load(String filename, Graph g) throws IOException
    {
        return load(filename, g, null);
    }
    
    /**
     * Creates a <code>FileReader</code> from <code>filename</code>, calls
     * <code>load(reader, g, nev)</code>, closes the reader, and returns
     * the resultant graph.
     * @throws IOException
     */
    public Graph load(String filename, Graph g, NumberEdgeValue nev) throws IOException
    {
        Reader reader = new FileReader(filename);
        Graph graph = load(reader, g, nev);
        reader.close();
        return graph;
    }
    
    /**
     * Returns <code>load(reader, g, null)</code>.
     * @throws IOException
     */
    public Graph load(Reader reader, Graph g) throws IOException
    {
        return load(reader, g, null);
    }
    
    /**
     * Returns <code>load(reader, new SparseGraph(), nev)</code>.
     * @throws IOException
     */
    public Graph load(Reader reader, NumberEdgeValue nev) throws IOException
    {
        return load(reader, new SparseGraph(), nev);
    }
    
    /**
     * Returns <code>load(reader, new SparseGraph(), null)</code>.
     * @throws IOException
     */
    public Graph load(Reader reader) throws IOException
    {
        return load(reader, new SparseGraph(), null);
    }
    
    /**
     * Returns <code>load(reader, g, nev, new TypedVertexGenerator(g))</code>.
     * @throws IOException
     * @see edu.uci.ics.jung.utils.TypedVertexGenerator
     */
    public Graph load(Reader reader, Graph g, NumberEdgeValue nev) throws IOException
    {
        return load(reader, g, nev, new TypedVertexGenerator(g));
    }
    
    /**
     * Populates the graph <code>g</code> with the graph represented by the
     * Pajek-format data supplied by <code>reader</code>.  Stores edge weights,
     * if any, according to <code>nev</code> (if non-null).
     * Any existing vertices/edges of <code>g</code>, if any, are unaffected.
     * The edge data are filtered according to <code>g</code>'s constraints, if any; thus, if 
     * <code>g</code> only accepts directed edges, any undirected edges in the 
     * input are ignored.
     * Vertices are created with the generator <code>vg</code>.  The user is responsible
     * for supplying a generator whose output is compatible with this graph and its contents;
     * users that don't want to deal with this issue may use a <code>TypedVertexGenerator</code>
     * or call <code>load(reader, g, nev)</code> for a default generator.
     * @throws IOException
     */
    public Graph load(Reader reader, Graph g, NumberEdgeValue nev, VertexGenerator vg) throws IOException
    {
        BufferedReader br = new BufferedReader(reader);
                
        // ignore everything until we see '*Vertices'
        String curLine = skip(br, v_pred);
        
        if (curLine == null) // no vertices in the graph; return empty graph
            return g;
        
        if (get_locations)
            g.addUserDatum(LOCATIONS, v_locations, UserData.SHARED);
        
        // create appropriate number of vertices
        StringTokenizer st = new StringTokenizer(curLine);
        st.nextToken(); // skip past "*vertices";
        int num_vertices = Integer.parseInt(st.nextToken());
        for (int i = 1; i <= num_vertices; i++)
            g.addVertex(vg.create());
        Indexer id = Indexer.getIndexer(g);

        // read vertices until we see any Pajek format tag ('*...')
        curLine = null;
        while (br.ready())
        {
            curLine = br.readLine();
            if (curLine == null || t_pred.evaluate(curLine))
                break;
            if (curLine == "") // skip blank lines
                continue;
            
            try
            {
                readVertex(curLine, id, num_vertices);
            }
            catch (IllegalArgumentException iae)
            {
                br.close();
                reader.close();
                throw iae;
            }
        }   

        // skip over the intermediate stuff (if any) 
        // and read the next arcs/edges section that we find
        curLine = readArcsOrEdges(curLine, br, g, nev);

        // ditto
        readArcsOrEdges(curLine, br, g, nev);
        
        br.close();
        reader.close();
        
        return g;
    }

    /**
     * Parses <code>curLine</code> as a reference to a vertex, and optionally assigns 
     * label and location information.
     * @throws IOException
     */
    private void readVertex(String curLine, Indexer id, int num_vertices) throws IOException
    {
        Vertex v;
        String[] parts = null;
        int coord_idx = -1;     // index of first coordinate in parts; -1 indicates no coordinates found
        String index;
        String label = null;
        // if there are quote marks on this line, split on them; label is surrounded by them
        if (curLine.indexOf('"') != -1)
        {
            String[] initial_split = curLine.trim().split("\"");
            // if there are any quote marks, there should be exactly 2
            if (initial_split.length < 2 || initial_split.length > 3)
                throw new IllegalArgumentException("Unbalanced (or too many) quote marks in " + curLine);
            index = initial_split[0].trim();
            label = initial_split[1].trim();
            if (initial_split.length == 3)
                parts = initial_split[2].trim().split("\\s+", -1);
            coord_idx = 0;
        }
        else // no quote marks, but are there coordinates?
        {
            parts = curLine.trim().split("\\s+", -1);
            index = parts[0];
            switch (parts.length)
            {
                case 1:         // just the ID; nothing to do, continue
                    break;  
                case 2:         // just the ID and a label
                    label = parts[1];
                    break;
                case 3:         // ID, no label, coordinates
                    coord_idx = 1;
                    break;
                case 4:         // ID, label, (x,y) coordinates
                    coord_idx = 2;
                    break;
            }
        }
        int v_id = Integer.parseInt(index) - 1; // go from 1-based to 0-based index
        if (v_id >= num_vertices || v_id < 0)
            throw new IllegalArgumentException("Vertex number " + v_id +
                    "is not in the range [1," + num_vertices + "]");
        v = (Vertex) id.getVertex(v_id);
        // only attach the label if there's one to attach
        if (label != null && label.length() > 0)
            attachLabel(v, label);
        // parse the rest of the line
        if (get_locations)
        {
            if (coord_idx == -1 || parts == null || parts.length < coord_idx+2)
                throw new IllegalArgumentException("Coordinates requested, but" +
                        curLine + " does not include coordinates");
            double x = Double.parseDouble(parts[coord_idx]);
            double y = Double.parseDouble(parts[coord_idx+1]);
//            if (x < 0 || x > 1 || y < 0 || y > 1)
//                throw new IllegalArgumentException("Coordinates in line " + 
//                        curLine + " are not all in the range [0,1]");
                
            v_locations.setLocation(v, new Point2D.Double(x,y));
        }
    }

    
    
    private String readArcsOrEdges(String curLine, BufferedReader br, Graph g,
            NumberEdgeValue nev) 
        throws IOException
    {
        String nextLine = curLine;
        
        Indexer id = Indexer.getIndexer(g);
        
        // in case we're not there yet (i.e., format tag isn't arcs or edges)
        if (! c_pred.evaluate(curLine))
//            nextLine = skip(br, e_pred);
            nextLine = skip(br, c_pred);

        // in "*Arcs" and this graph is not strictly undirected
//        boolean reading_arcs = a_pred.evaluate(nextLine) && 
//            !PredicateUtils.enforcesUndirected(g);
//        // in "*Edges" and this graph is not strictly directed
//        boolean reading_edges = e_pred.evaluate(nextLine) && 
//            !PredicateUtils.enforcesDirected(g);

        boolean reading_arcs = false;
        boolean reading_edges = false;
        if (a_pred.evaluate(nextLine))
        {
            if (PredicateUtils.enforcesUndirected(g))
                throw new IllegalArgumentException("Supplied undirected-only graph cannot be populated with directed edges");
            else
                reading_arcs = true;
        }
        if (e_pred.evaluate(nextLine))
        {
            if (PredicateUtils.enforcesDirected(g))
                throw new IllegalArgumentException("Supplied directed-only graph cannot be populated with undirected edges");
            else
                reading_edges = true;
        }
        
        if (!(reading_arcs || reading_edges))
            return nextLine;
        
        boolean is_list = l_pred.evaluate(nextLine);

        boolean parallel_ok = !PredicateUtils.enforcesNotParallel(g);

        while (br.ready())
        {
            nextLine = br.readLine();
            if (nextLine == null || t_pred.evaluate(nextLine))
                break;
            if (curLine == "") // skip blank lines
                continue;
            
            StringTokenizer st = new StringTokenizer(nextLine.trim());
            
            int vid1 = Integer.parseInt(st.nextToken()) - 1;
            Vertex v1 = (Vertex) id.getVertex(vid1);
            
            if (is_list) // one source, multiple destinations
            {
                do
                {
                    createAddEdge(st, v1, reading_arcs, g, id, parallel_ok);
                } while (st.hasMoreTokens());
            }
            else // one source, one destination, at most one weight
            {
                Edge e = createAddEdge(st, v1, reading_arcs, g, id, parallel_ok);
                // get the edge weight if we care
                if (nev != null)
                    nev.setNumber(e, new Float(st.nextToken()));
            }
        }
        return nextLine;
    }

    protected Edge createAddEdge(StringTokenizer st, Vertex v1, 
            boolean directed, Graph g, Indexer id, boolean parallel_ok)
    {
        int vid2 = Integer.parseInt(st.nextToken()) - 1;
        Vertex v2 = (Vertex) id.getVertex(vid2);
        Edge e = null;
        if (directed)
            e = new DirectedSparseEdge(v1, v2);
        else
            e = new UndirectedSparseEdge(v1, v2);

        // add this edge if parallel edges are OK,
        // or if this isn't one; otherwise ignore it
        if (parallel_ok || !p_pred.evaluate(e))
            g.addEdge(e);

        return e;
    }
    
    /**
     * Returns the first line read from <code>br</code> for which <code>p</code> 
     * returns <code>true</code>, or <code>null</code> if there is no
     * such line.
     * @throws IOException
     */
    protected String skip(BufferedReader br, Predicate p) throws IOException
    {
        while (br.ready())
        {
            String curLine = br.readLine();
            if (curLine == null)
                break;
            curLine = curLine.trim();
            if (p.evaluate(curLine))
                return curLine;
        }
        return null;
    }
    
    /**
     * Labels <code>v</code> with <code>string</code>, according to the 
     * labeling mechanism specified by <code>unique_labels</code>.
     * Removes quotation marks from the string if present.
     */
    private void attachLabel(Vertex v, String string) throws IOException
    {
        if (string == null || string.length() == 0)
            return;
        String label = string.trim();
//        String label = trimQuotes(string).trim();
//        if (label.length() == 0)
//            return;
        if (unique_labels)
        {
            try
            {
                StringLabeller sl = StringLabeller.getLabeller((Graph)v.getGraph(), LABEL);
                sl.setLabel(v, label);
            }
            catch (StringLabeller.UniqueLabelException slule)
            {
                throw new FatalException("Non-unique label found: " + slule);
            }
        }
        else
        {
            v.addUserDatum(LABEL, label, UserData.SHARED);
        }
    }

    /**
     * Sets or clears the <code>unique_labels</code> boolean.
     * @see #PajekNetReader(boolean, boolean)
     */
    public void setUniqueLabels(boolean unique_labels)
    {
        this.unique_labels = unique_labels;
    }

    /**
     * Sets or clears the <code>get_locations</code> boolean.
     * @see #PajekNetReader(boolean, boolean)
     */
    public void setGetLocations(boolean get_locations)
    {
        this.get_locations = get_locations;
    }
    
    /**
     * A Predicate which evaluates to <code>true</code> if the
     * argument starts with the constructor-specified String.
     * 
     * @author Joshua O'Madadhain
     */
    protected static class TagPred implements Predicate
    {
        private String tag;
        
        public TagPred(String s)
        {
            this.tag = s;
        }
        
        public boolean evaluate(Object arg0)
        {
            String s = (String)arg0;
            return (s != null && s.toLowerCase().startsWith(tag));
        }
    }
    
    /**
     * A Predicate which evaluates to <code>true</code> if the
     * argument ends with the string "list".
     * 
     * @author Joshua O'Madadhain
     */
    protected static class ListTagPred implements Predicate
    {
        protected static ListTagPred instance;
        
        protected ListTagPred() {}
        
        public static ListTagPred getInstance()
        {
            if (instance == null)
                instance = new ListTagPred();
            return instance;
        }
        
        public boolean evaluate(Object arg0)
        {
            String s = (String)arg0;
            return (s != null && s.toLowerCase().endsWith("list"));
        }
    }
    

}
