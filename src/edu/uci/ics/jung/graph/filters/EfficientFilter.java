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

/**
 * An EfficientFilter can take in an UnassembledGraph
 * withut assembling it, and is used for non-structural
 * filters. In general, EfficientFilters
 * are those that look at vertices on their own, based
 * on their attributes, as opposed to examining their 
 * structure.
 * <p>
 * This is an efficiency measure for handling streams of
 * filters. Sometimes, a program needs to run a series of
 * filters--for example, to cut out all vertices with 
 * field "color" is "green" and field "size" is "large". 
 * Rather than creating one custom filter that checks both, 
 * one can run those two filters serially. The code might
 * look something like this:
 * <pre>
 * 		Filter f_1 = new ColorFilter( "green" );
 * 		Filter f_2 = new SizeFilter( "large ");
 * 		Graph green_graph = f_1.filter( graph ).assemble();
 * 		Graph large_green_graph = f_2.filter( green_graph ).assemble()
 * </pre>
 * Unfortunately, assembly can be an expensive process--it requires
 * a full pass through the graph, checking each vertex and each edge. 
 * The <code>EfficientFilter</code> takes this into account and lets
 * a user specify the faster parts:
 * <pre>
 * 		EfficientFilter f_1 = new ColorFilter( "green" );
 * 		EfficientFilter f_2 = new SizeFilter( "large ");
 * 		UnassembledGraph green_u_graph = f_1.filter( graph ); // note: no call to assemble
 * 		Graph large_green_graph = f_2.filter( green_u_graph ).assemble()
 * </pre>
 * which can be simplified to a chain as
 * <pre>
 * 		EfficientFilter f_1 = new ColorFilter( "green" );
 * 		EfficientFilter f_2 = new SizeFilter( "large ");
 * 		Graph large_green_graph = f_2.filter( f_1.filter( graph )).assemble(); // note: no call to assemble
 * </pre>
 * <h2>Filters that can safely use <tt>EfficientFilter</tt>s.</h2>
 * An <tt>EfficientFilter</tt> should judge vertices and edges only 
 * on their <tt>UserData</tt>: information like labels, weights, and
 * sizes. Because the vertices that an <tt>EfficientFilter</tt> takes in
 * from its <tt>UnassembledGraph</tt> are not correctly linked into a graph,
 * information like the 
 * <tt>{@link edu.uci.ics.jung.graph.ArchetypeVertex#degree() vertex degree}</tt> 
 * and 
 * <tt>{@link edu.uci.ics.jung.graph.ArchetypeVertex#getNeighbors() vertex neighbors}</tt> 
 * are likely to be inaccurate. Thus, the 
 * <tt>{@link edu.uci.ics.jung.graph.filters.impl.DropSoloNodesFilter DropSoloNodesFilter}</tt>
 * does not implement <tt>EfficientFilter</tt>, because it examines the degree of each vertex.
 * <p>
 * This is taken into consideration correctly for <tt>{@link SerialFilter SerialFilter}</tt>.
 * 
 * @author danyelf
 */
public interface EfficientFilter extends Filter {

	/**
	 * Filters a graph by returning an <tt>UnassembledGraph</tt> consisting
	 * of nodes and edges that pass the filter. Note that this method must
	 * be implemented <em>in addition</em> to the methods in <tt>{@link Filter Filter}</tt>.
	 * @param ug An unassembled graph to be filtered.
	 * @return an <tt>UnassembledGraph</tt> that contains the subset of vertices and edges
	 * from <tt>ug</tt> pass the filter.
	 */
	UnassembledGraph filter( UnassembledGraph ug );

}
