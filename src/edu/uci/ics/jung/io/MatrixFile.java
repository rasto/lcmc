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
 * Created on Jan 6, 2002
 *
 */
package edu.uci.ics.jung.io;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import corejava.Format;
import edu.uci.ics.jung.algorithms.GraphMatrixOperations;
import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.Graph;
/**
 * Basic I/O handler for ascii matrix files. An ascii matrix is simply
 * a square matrix where 0 values for cell (i,j) indicates no edge exists between
 * vertex i and vertex j and non-zero values indicates there is an edge. If
 * a non-null weight key is specified then it will be used to treat the non-zero
 * values as a weight stored in the edges' user data keyed off the specified weight key value.
 * <p>  
 * When loading a graph from a file, a symmetric graph will result in the construction of
 * an undirected sparse graph while a non-symmetric graph will result in the construction of
 * a directed sparse graph. 
 * <p>
 * For example the following ascii matrix when loaded using the code:<br><code>
 * MatrixFile mf = new MatrixFile(null); <br>
 * Graph g = mf.load(filename); </code><br>
 * will produce an undirected sparse matrix with no weights: <br>
 * <pre>
 * 0 1 0 1
 * 1 0 0 1
 * 0 0 0 0
 * 1 1 0 0
 * </pre><p>
 * whereas the following ascii matrix when loaded using the code:<br><code>
 * MatrixFile mf = new MatrixFile("WEIGHT"); <br>
 * Graph g = mf.load(filename); </code> <br>
 * will produce a directed sparse matrix with double weight values stored in
 * the edges user data under the key "WEIGHT" : <br>
 * <pre>
  * 0 .5 10 0
 * 0 1 0 0
 * 0 0 0 -30
 * 5 0 0 0
 * </pre>
 * @author Scott
 *
 */
public class MatrixFile implements GraphFile {
	private String mWeightKey;
	/**
	 * Constructs MatrixFile instance. If weightKey is not null then, it will
	 * attempt to use that key to store and retreive weights from the edges'
	 * UserData.
	 */
	public MatrixFile(String weightKey) {
		mWeightKey = weightKey;
	}
	
	/**
	 * Loads a graph from an input reader
	 * @param reader the input reader
	 * @return the graph
	 */
	public Graph load(BufferedReader reader) {
		Graph graph = null;
		try {
			DoubleMatrix2D matrix = createMatrixFromFile(reader);
			graph = GraphMatrixOperations.matrixToGraph(matrix,mWeightKey);
		} catch (Exception e) {
			throw new FatalException(
				"Fatal exception calling MatrixFile.load(...)",
				e);
		}
		return graph;
	}

	private DoubleMatrix2D createMatrixFromFile(BufferedReader reader)
		throws IOException, ParseException {
		List rows = new ArrayList();
		String currentLine = null;
		while ((currentLine = reader.readLine()) != null) {
			StringTokenizer tokenizer = new StringTokenizer(currentLine);
			if (tokenizer.countTokens() == 0) {
				break;
			}
			DoubleArrayList currentRow = new DoubleArrayList();
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				currentRow.add(Double.parseDouble(token));
			}
			rows.add(currentRow);
		}
		int size = rows.size();
		DoubleMatrix2D matrix = new SparseDoubleMatrix2D(size, size);
		for (int i = 0; i < size; i++) {
			DoubleArrayList currentRow = (DoubleArrayList) rows.get(i);
			if (currentRow.size() != size) {
				throw new ParseException(
					"Matrix must have the same number of rows as columns",
					0);
			}
			for (int j = 0; j < size; j++) {
				double currentVal = currentRow.get(j);
				if (currentVal != 0) {
					matrix.setQuick(i, j, currentVal);
				}					
			}
		}
		return matrix;
	}
	/* 
	 * Loads a graph from a file
	 * @see edu.uci.ics.jung.io.GraphFile#load(java.lang.String)
	 */
	public Graph load(String filename) {
		try {
			BufferedReader reader =
				new BufferedReader(new FileReader(filename));
			Graph graph = load(reader);
			reader.close();
			return graph;
		} catch (IOException ioe) {
			throw new FatalException("Error in loading file " + filename, ioe);
		}
	}
	/* 
	 * Saves a graph to a file
	 * @see edu.uci.ics.jung.io.GraphFile#save(edu.uci.ics.jung.graph.Graph, java.lang.String)
	 */
	public void save(Graph graph, String filename) {
		try {
			BufferedWriter writer =
				new BufferedWriter(new FileWriter(filename));
//			Vertex currentVertex = null;
			DoubleMatrix2D matrix = GraphMatrixOperations.graphToSparseMatrix(graph,mWeightKey);
			Format labelFormat = new Format("%4.2f");
			for (int i=0;i<matrix.rows();i++) {
				for (int j=0;j<matrix.columns();j++) {
					writer.write(labelFormat.format(matrix.getQuick(i,j)) + " ");				
				}
				writer.write("\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new FatalException("Error saving file: " + filename, e);
		}
	}
}
