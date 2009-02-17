/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.statistics;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.Set;

import cern.colt.list.DoubleArrayList;
import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Vertex;

/**
 * Set of general-purpose functions for analyzing the degree distribution of a set of vertices (normally
 * the complete set of vertices associated with a graph). These include: <ul>
 * <li> statistical summary (set of fundamental point statistics) of outdegree distribution </li>
 * <li> statistical summary (set of fundamental point statistics) of indegree distribution </li>
 * <li> histogram of outdegree distribution </li>
 * <li> histogram of outdegree distribution </li>
 * <li> function for saving degree information to a file </li></ul>
 * @author Scott White
 */
public class DegreeDistributions 
{

    /**
     * Given a set of vertices, this function returns a list of degrees.
     * @param vertices the vertices whose degrees are to be analyzed
     * @return a list of degrees
     */
    public static DoubleArrayList getDegreeValues(Set vertices) 
    {
        DoubleArrayList degreeValues = new DoubleArrayList();

        for (Iterator i = vertices.iterator(); i.hasNext(); ) 
            degreeValues.add(((ArchetypeVertex)i.next()).degree());

        return degreeValues;
    }

    
     /**
     * Given a set of vertices, this function returns a list of outdegrees.
     * @param vertices the vertices whose outdegrees are to be analyzed
     * @return a list of outdegrees
     */
    public static DoubleArrayList getOutdegreeValues(Set vertices) {
        DoubleArrayList outDegreeValues = new DoubleArrayList();

        Vertex currentVertex = null;
		for (Iterator i = vertices.iterator(); i.hasNext(); ) {
			currentVertex = (Vertex) i.next();
			outDegreeValues.add(currentVertex.outDegree());
        }

        return outDegreeValues;
    }

    /**
     * Given a set of vertices, this function returns a list of indegrees.
     * @param vertices the vertices whose indegrees are to be analyzed
     * @return a list of indegrees
     */
	public static DoubleArrayList getIndegreeValues(Set vertices) {
		DoubleArrayList list = new DoubleArrayList();

		Vertex currentVertex = null;
		for (Iterator i = vertices.iterator(); i.hasNext(); ) {
			currentVertex = (Vertex) i.next();
			list.add(currentVertex.inDegree());
		}

		return list;
	}

    /**
     * Generates a histogram of the outdegree distribution for a set of vertices
     * @param vertices the set of vertices to be analyzed
     * @param min the minimum value of the data to be binned
     * @param max the maximum value of the data to be binned
     * @param numBins the number of bins to be created
     * @return the histogram instance
     */
	public static Histogram getOutdegreeHistogram(Set vertices, double min, double max, int numBins) {
		Histogram histogram = new Histogram(min,max,numBins);

		for (Iterator i = vertices.iterator(); i.hasNext(); ) {
			Vertex currentVertex = (Vertex) i.next();
			int currentOutdegree = currentVertex.outDegree();
			histogram.fill(currentOutdegree);
		}
		return histogram;
	}

    /**
     * Generates a histogram of the indegree distribution for a set of vertices
     * @param vertices the set of vertices to be analyzed
     * @param min the minimum value of the data to be binned
     * @param max the maximum value of the data to be binned
     * @param numBins the number of bins to be created
     * @return the histogram instance
     */
	public static Histogram getIndegreeHistogram(Set vertices, double min, double max, int numBins) {
		Histogram histogram = new Histogram(min,max,numBins);

		for (Iterator i = vertices.iterator(); i.hasNext(); ) {
			Vertex currentVertex = (Vertex) i.next();
			int currentIndegree = currentVertex.inDegree();
			histogram.fill(currentIndegree);
		}
		return histogram;
	}

    /**
     * Saves the empirical degree distribution to a file in the ascii flat file where each line has the
     * following format:
     * <degree> <# of vertices with this degree>
     * @param histogram a histogram representing a degree distribution
     * @param file the name of the file where the data is to be saved
     */
	public static void saveDistribution(Histogram histogram, String file) {

		try {
			BufferedWriter degreeWriter = new BufferedWriter(new FileWriter(file));
			for (int i = 0; i < histogram.size(); i++) {
				int currentDegree = (int) (i + histogram.getMinimum());
				degreeWriter.write(currentDegree + " " + histogram.yValueAt(i) + "\n");
			}
			degreeWriter.close();

		} catch (Exception e) {
			throw new FatalException("Error saving binned data to " + file,e);
		}
	}
}