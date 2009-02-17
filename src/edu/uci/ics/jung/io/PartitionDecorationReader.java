/*
 * Created on May 2, 2004
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.collections.Predicate;

import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.graph.decorators.StringLabeller.UniqueLabelException;
import edu.uci.ics.jung.utils.UserData;


/**
 * Reads decorations for vertices in a specified partition from a
 * <code>Reader</code>.
 * 
 * @author Joshua O'Madadhain
 */
public class PartitionDecorationReader
{

    /**
     * Decorates vertices in the specified partition with strings. The
     * decorations are specified by a text file with the following format:
     * 
     * <pre>
     * vid_1 label_1 
     * vid_2 label_2 ...
     * </pre>
     * 
     * <p>
     * The strings must be unique within this partition; duplicate strings will
     * cause a <code>UniqueLabelException</code> to be thrown.
     * 
     * <p>
     * The end of the file may be artificially set by putting the string <code>end_of_file</code>
     * on a line by itself.
     * </p>
     * 
     * @param bg
     *            the bipartite graph whose vertices are to be decorated
     * @param name_reader
     *            the reader containing the decoration information
     * @param partition
     *            the vertex partition whose decorations are specified by this
     *            file
     * @param string_key
     *            the user data key for the decorations created
     */
    public static void loadStrings(
        Graph bg,
        Reader name_reader,
        Predicate partition,
        Object string_key)
    {
        StringLabeller id_label = StringLabeller.getLabeller(bg, partition);
        StringLabeller string_label =
            StringLabeller.getLabeller(bg, string_key);
        try
        {
            BufferedReader br = new BufferedReader(name_reader);
            while (br.ready())
            {
                String curLine = br.readLine();
                if (curLine == null || curLine.equals("end_of_file"))
                    break;
                if (curLine.trim().length() == 0)
                    continue;
                String[] parts = curLine.trim().split("\\s+", 2);
    
                Vertex v = id_label.getVertex(parts[0]);
                if (v == null)
                    throw new FatalException("Invalid vertex label");
    
                // attach the string to this vertex
                string_label.setLabel(v, parts[1]);
            }
            br.close();
            name_reader.close();
        }
        catch (IOException ioe)
        {
            throw new FatalException(
                "Error loading names from reader " + name_reader,
                ioe);
        }
        catch (UniqueLabelException ule)
        {
            throw new FatalException(
                "Unexpected duplicate name in reader " + name_reader,
                ule);
        }
    }

    /**
     * Decorates vertices in the specified partition with typed count data. 
     * The data must be contained in a text file in the following format:
     * 
     * <pre>
     * vid_1    type_1  count_1
     * vid_2    type_2  count_2
     * ...
     * </pre>
     * 
     * <p>where <code>count_i</code> (an integer value) represents 
     * the number of elements of 
     * type <code>type_i</code> possessed by the vertex with ID 
     * <code>vid_i</code> (as defined by <code>BipartiteGraphReader.load()</code>)
     * for the <code>i</code>th line in the file.</p>
     * 
     * <p>For example, the vertices might represent authors, the type might
     * represent a topic, and the count might represent the number of 
     * papers that the specified author had written on that topic.<p>
     * 
     * <p>If <code>normalize</code> is <code>true</code>, then the 
     * count data will be scaled so that the counts for
     * each vertex will sum to 1.  (In this case, each vertex must have
     * a positive total count value.)
     * 
     * <p>The end of the file may be artificially set by putting the string
     * <code>end_of_file</code> on a line by itself.</p>
     * 
     * @return              the total number of types observed
     * @param bg            the bipartite graph whose vertices are to be decorated
     * @param count_reader  the reader containing the decoration data
     * @param partition     the partition whose decorations are specified by this file
     * @param count_key     the user key for the decorations
     * @param copyact       the copy action for the decorations
     */
    public static int loadCounts(Graph bg, Reader count_reader, 
                                  Predicate partition, 
                                  Object count_key, UserData.CopyAction copyact)
    {
        StringLabeller id_label = StringLabeller.getLabeller(bg, partition);
        Set types = new HashSet();
        try
        {    
            BufferedReader br = new BufferedReader(count_reader);
            
            while (br.ready())
            {
                String curLine = br.readLine();
                if (curLine == null || curLine.equals("end_of_file"))
                    break;
                if (curLine.trim().length() == 0)
                    continue;
                    
                StringTokenizer st = new StringTokenizer(curLine);
                String entity_id = st.nextToken();
                String type_id = st.nextToken();
                Integer count = new Integer(st.nextToken());
    
                types.add(type_id);
                
                Vertex v = id_label.getVertex(entity_id);
                
                if (v == null)
                    throw new IllegalArgumentException("Unrecognized vertex " + entity_id);
                
                Map count_map = (Map)v.getUserDatum(count_key);
                if (count_map == null)
                {
                    count_map = new HashMap();
                    v.addUserDatum(count_key, count_map, copyact); 
                }
                count_map.put(type_id, count);
                
            }
            br.close();
            count_reader.close();
        }
        catch (IOException ioe)
        {
            throw new FatalException("Error in loading counts from " + count_reader);
        }
        
        return types.size();
    }

    public static void loadCounts(Graph bg, Reader count_reader,
            Predicate partition, Object count_key, UserData.CopyAction copyact,
            int num_types)
    {
        StringLabeller id_label = StringLabeller.getLabeller(bg, partition);
        try
        {
            BufferedReader br = new BufferedReader(count_reader);
    
            while (br.ready())
            {
                String curLine = br.readLine();
                if (curLine == null || curLine.equals("end_of_file"))
                    break;
                if (curLine.trim().length() == 0)
                    continue;
    
                StringTokenizer st = new StringTokenizer(curLine);
                String entity_id = st.nextToken();
                int type_id = new Integer(st.nextToken()).intValue() - 1;
                int count = new Integer(st.nextToken()).intValue();
    
                Vertex v = id_label.getVertex(entity_id);
    
                if (v == null)
                    throw new IllegalArgumentException("Unrecognized vertex "
                            + entity_id);
    
                double[] counts = (double[])v.getUserDatum(count_key);
                if (counts == null)
                {
                    counts = new double[num_types];
                    v.addUserDatum(count_key, counts, copyact);
                }
                counts[type_id] = count;
            }
            br.close();
        }
        catch (IOException ioe)
        {
            throw new FatalException("Error in loading counts from "
                    + count_reader);
        }
    }

}
