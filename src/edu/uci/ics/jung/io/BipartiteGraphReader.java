/*
 * Created on Apr 13, 2004
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections.Predicate;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.KPartiteGraph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.EdgeWeightLabeller;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.graph.decorators.StringLabeller.UniqueLabelException;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.KPartiteSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.predicates.UserDatumVertexPredicate;
import edu.uci.ics.jung.utils.TypedVertexGenerator;
import edu.uci.ics.jung.utils.UserData;
import edu.uci.ics.jung.utils.VertexGenerator;


/**
 * 
 * @author Joshua O'Madadhain
 */
public class BipartiteGraphReader
{
    /**
     * The user data key for the vertices' partitions.
     */
    public final static String PARTITION = "edu.uci.ics.jung.io:Partition";
    
    /**
     * The predicate for the vertices in partitions A and B.
     */
    public final static UserDatumVertexPredicate PART_A = 
        new UserDatumVertexPredicate(PARTITION, "vertex_ID_A");
    public final static UserDatumVertexPredicate PART_B = 
        new UserDatumVertexPredicate(PARTITION, "vertex_ID_B");

    /**
     * <p>If <code>true</code>, specifies that each line of input implicitly 
     * represents a list of edges, where the first token specifies one endpoint,
     * and the subsequent tokens specify a sequence of opposite endpoints.
     * Otherwise, each line of input represents a single edge.</p>
     */
    protected boolean asList;

    /**
     * <p>If <code>true</code>, the edges created
     * will be directed; otherwise, they will be undirected.
     * In either case, the graph is constrained to accept only the 
     * specified edge type.</p>
     * 
     * <p>NOTE: Currently, the data format specified only permits 
     * directed edges whose source is in partition A and whose
     * destination is in partition B.</p>
     */
    protected boolean directed;
    
    /**
     * <p>
     * If <code>true</code>, parallel edges may
     * be created if the edge is found more than once in the input file.
     * Otherwise, each edge is labelled with a weight by an 
     * <code>EdgeWeightLabeller</code>, and the graph is created with the
     * "no parallel edges" constraint.
     * The weight of each edge in the non-parallel case is the number of times 
     * that the edge is represented in the input file (that is, the edge's multiplicity).
     * </p>
     */
    protected boolean parallel;
    
    public BipartiteGraphReader(boolean asList, boolean directed, boolean parallel)
    {
        this.asList = asList;
        this.directed = directed;
        this.parallel = parallel;
    }

    /**
     * Creates a BipartiteGraphReader with default behavior (one edge per line, 
     * undirected, no parallel edges created).
     */
    public BipartiteGraphReader()
    {
        this(false, false, false);
    }
    
    
    /**
     * <p>Creates a <code>KPartiteGraph</code> (where k = 2) based on connection
     * data gathered from a Reader.  
     * The data must be in one of the two following formats:</p>
     * 
     * <pre>
     * a_1 b_1 
     * a_2 b_1 
     * a_2 b_2 
     * a_3 b_3 ...
     * </pre>
     * 
     * <p>or</p>
     * 
     * <pre>
     *  a_1 b_1 b_2 b_3 
     *  a_2 b_2 b_3 
     *  a_3 b_3 ...
     * </pre>
     * 
     * <p>
     * where <code>x_i</code> is a unique label (ID) for vertex <code>i</code>
     * in partition <code>x</code>. Each line in the file defines an edge
     * between the specified vertices. The vertices themselves are defined
     * implicitly: if a label is read from the file for which no vertex yet
     * exists, a new vertex is created and that label is attached to it.
     * </p>
     * 
     * <p>The first format is the default; the second is assumed if the 
     * <code>asList</code> flag is set to <code>true</code>.  In
     * the default format, everything after the first whitespace is 
     * interpreted as part of the label for the second vertex.</p>
     * 
     * <p>
     * Vertex labels are only required to be unique within their
     * partitions. Each partition has its own <code>StringLabeller</code>,
     * which is accessed via the key <code>VID_A</code> or <code>VID_B</code>,
     * as appropriate.
     * </p>
     * 
     * <p>
     * The end of the file may be artificially set by putting the string <code>end_of_file</code>
     * on a line by itself.
     * </p>
     * 
     *  
     * 
     * @return the 2-partite graph loaded with these vertices, and labelled with two StringLabellers
     * @throws
     *         IOException  May occur in the course of reading from a stream.
     */
    public KPartiteGraph load(Reader reader) throws IOException
    {
        List predicates = new LinkedList();
        predicates.add(PART_A);
        predicates.add(PART_B);
        
        KPartiteGraph bg = new KPartiteSparseGraph(predicates, true);
        
        Collection edge_constraints = bg.getEdgeConstraints();
        if (directed)
            edge_constraints.add(Graph.DIRECTED_EDGE);
        else
            edge_constraints.add(Graph.UNDIRECTED_EDGE);
        if (!parallel)
            edge_constraints.add(Graph.NOT_PARALLEL_EDGE);

        VertexGenerator vg = new TypedVertexGenerator(edge_constraints);
        
        EdgeWeightLabeller ewl = EdgeWeightLabeller.getLabeller(bg);

        BufferedReader br = new BufferedReader(reader);

        while (br.ready())
        {
            // read the line in, break it into 2 parts (one for each
            // vertex)
            String curLine = br.readLine();
            if (curLine == null || curLine.equals("end_of_file"))
                break;
            if (curLine.trim().length() == 0)
                continue;
            String[] parts;
            if (asList)
                parts = curLine.trim().split("\\s+");
            else
                parts = curLine.trim().split("\\s+", 2);

            // fetch/create vertices for each part of the string
            Vertex v_a = getOrCreateVertexByName(bg, vg, parts[0], PART_A);

            int i = 1;
            while (i < parts.length)
            {
                Vertex v_b = getOrCreateVertexByName(bg, vg, parts[i++], PART_B);

                Edge e = v_a.findEdge(v_b);
                boolean absent = (e == null);
                if (absent || parallel)
                {
                    if (directed)
                        e = bg.addEdge(new DirectedSparseEdge(v_a, v_b));
                    else
                        e = bg.addEdge(new UndirectedSparseEdge(v_a, v_b));
                }
                if (!parallel)  // weight represents multiplicity of edge
                {
                    if (absent)
                        ewl.setWeight(e, 1);
                    else
                        ewl.setWeight(e, ewl.getWeight(e) + 1);
                }
            }
        }
        br.close();
        reader.close();
        return bg;
    }

    private static Vertex getOrCreateVertexByName(Graph bg, VertexGenerator vg,
        String label, UserDatumVertexPredicate partition)
    {
        StringLabeller vID_label = StringLabeller.getLabeller(bg, partition);
        Vertex v = (Vertex) vID_label.getVertex(label);
        if (v == null)
        {
            v = (Vertex)vg.create();
            v.addUserDatum(partition.getKey(), partition.getDatum(), UserData.SHARED);
            bg.addVertex(v);
            try
            {
                vID_label.setLabel(v, label);
            }
            catch (UniqueLabelException e1) {}
        }
        return v;
    }

    public static Predicate getPartition(Vertex v)
    {
        if (PART_A.evaluate(v))
            return PART_A;
        else if (PART_B.evaluate(v))
            return PART_B;
        else
            throw new IllegalArgumentException("Specified vertex " + v +
                    "is not in any registered partition");
    }
    
    public void setAsList(boolean asList)
    {
        this.asList = asList;
    }
    
    public void setDirected(boolean directed)
    {
        this.directed = directed;
    }
    
    public void setParallel(boolean parallel)
    {
        this.parallel = parallel;
    }

    public boolean isAsList()
    {
        return asList;
    }

    public boolean isDirected()
    {
        return directed;
    }

    public boolean isParallel()
    {
        return parallel;
    }
}
