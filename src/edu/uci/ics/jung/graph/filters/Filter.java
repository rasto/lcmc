/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.graph.filters;

import edu.uci.ics.jung.graph.Graph;

/**
 * A <code>Filter</code> returns a subgraph of a <code>Graph</code>, in the 
 * form of an <code>UnsassembledGraph</code>. That <code>UnsassembledGraph</code>
 * can then be turned into a <code>Graph</code> with a call to <code>assemble()</code>.
 * A <code>Filter</code>, then, selects the subset of vertices and edges and from them creates
 * an <code>UnassembledGraph</code> (presumably, with one of its two constructors.)
 * <p>
 * An <code>UnsassembledGraph</code> represents all the vertices (and at least all the edges) of
 * a <code>Graph</code> that pass the subset. However, they have not been "assembled" into something
 * that fulfills the <code>Graph</code> contract. (In particular, the <code>Vertices</code> and 
 * <code>Edges</code> that make up
 * the <code>UnassembledGraph</code> still report, through <code>getGraph()</code>, 
 * that they are members of the
 * original, and they still are connected to all the original edges.
 * <p>
 * After the call to <code>assemble</code>, the new <code>Graph</code> is valid , following the
 * Graph contract fully.
 * <p>
 * <h2>Sample code</h2>
 * This code is taken from the <a href="http://www.junit.org">JUnit</a> tests for 
 * the filter code, with minor modifications. It demonstrates how to walk through the
 * filter process.
 * <pre>
 * Graph g = {@link edu.uci.ics.jung.utils.TestGraphs#createTestGraph(boolean) TestGraphs.createTestGraph}( true );                   
 * // creates a graph.
 * EdgeWeightLabeller ewl = {@link edu.uci.ics.jung.graph.decorators.EdgeWeightLabeller#getLabeller EdgeWeightLabeller.getLabeller( g );}
 * // links each edge to a weight.
 * WeightedEdgeGraphFilter wgf = new {@link edu.uci.ics.jung.graph.filters.impl.WeightedEdgeGraphFilter WeightedEdgeGraphFilter( 0, ewl )};
 * // creates a filter based on this weight
 *
 * wgf.{@link edu.uci.ics.jung.graph.filters.impl.WeightedEdgeGraphFilter#setValue setValue(3)};
 * // sets the threshold at 3.
 * // at this point, the Filter is ready to block any edges with a weight less than 3.
 *
 * {@link UnassembledGraph UnassembledGraph} ug = {@link Filter#filter wgf.filter(g)};
 * // this UnassembledGraph contains all edges of weight greater than three, and all vertices in g
 * 
 * Graph sub_g = {@link UnassembledGraph#assemble ug.assemble()};
 * </pre>
 * At the end of this code, the new graph <code>sub_g</code> contains a copy of
 * some edges, and all nodes, of <code>g</code>. From here, we can treat it as
 * a <code>Graph</code>. Note that calls like 
 * {@link edu.uci.ics.jung.graph.ArchetypeVertex#getEqualVertex(ArchetypeGraph) getEquivalentVertex( Graph )} 
 * will do the right thing on the vertices of <code>sub_g</code> and point back to <code>g</code>.
 * <p>
 * In addition, we now have access to the <code>{@link edu.uci.ics.jung.graph.filters.GraphAssemblyRecord GraphAssemblyRecord}</code>
 * that corresponds to the graph.
 * <pre>
 * 	GraphAssemblyRecord gar = getAssemblyRecord( sub_g );
 *  String filterName = gar.getName();
 * </pre>
 * 
 * @see edu.uci.ics.jung.graph.filters.EfficientFilter
 * @author danyelf
 */
public interface Filter {

	/**
	 * Filters a graph by returning an <tt>UnassembledGraph</tt> consisting
	 * of nodes and edges that pass the filter.
	 * @param g An input graph to be filtered.
	 * @return an <tt>UnassembledGraph</tt> that contains the subset of vertices and edges
	 * from <tt>g</tt> pass the filter.
	 */
	UnassembledGraph filter( Graph g );
	
	/**
	 * Gets a name that describes this filter. It is used by the auditing 
	 * methods in <tt>{@link GraphAssemblyRecord GraphAssemblyRecord}</tt>
	 * @return A string that describes the filter.
	 */
	String getName();

}
