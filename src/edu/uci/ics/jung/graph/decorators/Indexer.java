/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 */
/*
 * Created on Jun 13, 2003
 *  
 */
package edu.uci.ics.jung.graph.decorators;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.utils.UserData;

/**
 * 
 * An Indexer applies an index to a Graph. The Indexer, specifically, attaches
 * itself to a Graph's UserData and keeps a set of vertex keys as integers. An
 * indexer can be used to look up both forward (Vertex - Index) and backward
 * (Index - Vertex) .
 * 
 * FIXME: note that there's currently no way to ask an Indexer instance what its
 * offset is.
 * 
 * @author danyelf
 *  
 */
public class Indexer {

	/** This is the key in the Graph's UserData where the Indexer is stored */
	static final Object INDEX_DEFAULT_KEY = "IndexDefaultKey";

	/**
	 * Gets the indexer associated with this graph. This uses the default
	 * INDEX_DEFAULT_KEY as its user data key.
	 * 
	 * @throws FatalException
	 *             if the graph has changed detectably since the last run. Note
	 *             that "has changed" merely looks at the number of nodes for
	 *             now.
	 */
	public static Indexer getIndexer(ArchetypeGraph g) {
		return getIndexer(g, INDEX_DEFAULT_KEY, false, false, 0);
	}

	/**
	 * Gets the indexer associated with this graph. Forces the system to create
	 * a new Index on the graph.
	 * 
	 * This uses the default INDEX_DEFAULT_KEY as its user data key.
	 */
	public static Indexer getAndUpdateIndexer(ArchetypeGraph g) {
		return getIndexer(g, INDEX_DEFAULT_KEY, true, false, 0);
	}

	/**
	 * Creates a new indexer associated with this graph. Starts the count at
	 * "offset". WARNING: This graph may be hard to use in some other methods
	 * that assume a zero offset. If the Graph has changed, this will update
	 * the index. Note that the "has Changed" parameter is a little thin; it
	 * merely checks whether the size has changed or not
	 * 
	 * This uses the default INDEX_DEFAULT_KEY as its user data key.
	 * 
	 * @param g
	 *            the Graph to index.
	 * @param offset
	 *            a starting value to index from
	 * @return an indexer that has been indexed
	 */
	public static Indexer newIndexer(ArchetypeGraph g, int offset) {
		return getIndexer(g, INDEX_DEFAULT_KEY, false, true, offset);
	}

	/**
	 * * Gets an indexer associated with this graph at this key
	 * 
	 * @param g
	 *            The graph to check
	 * @param key
	 *            The user data key to check
	 * @return the indexer
	 * 
	 * @throws FatalException
	 *             if the graph has changed detectably since the last run. Note
	 *             that "has changed" merely looks at the number of nodes for
	 *             now.
	 *  
	 */
	public static Indexer getIndexer(ArchetypeGraph g, Object key) {
		return getIndexer(g, key, false, false, 0);
	}

	/**
	 * Gets the indexer associated with this graph. Forces the system to create
	 * a new Index on the graph at the given key.
	 * 
	 * @throws FatalException
	 *             if the graph has changed detectably since the last run. Note
	 *             that "has changed" merely looks at the number of nodes for
	 *             now.
	 */
	public static Indexer getAndUpdateIndexer(ArchetypeGraph g, Object key) {
		return getIndexer(g, key, true, false, 0);
	}

	/**
	 * Checks if there is an indexer assocated with this graph.
	 * 
	 * This uses the default INDEX_DEFAULT_KEY as its user data key.
	 * 
	 * @param g
	 *            The graph to check
	 * @return true if there is an indexer associated with this graph.
	 */
	public static boolean hasIndexer(ArchetypeGraph g) {
		return hasIndexer(g, INDEX_DEFAULT_KEY);
	}

	/**
	 * Checks if there is an indexer assocated with this graph.
	 * 
	 * @param g
	 *            The graph to check
	 * @return true if there is an indexer associated with this graph.
	 */
	public static boolean hasIndexer(ArchetypeGraph g, Object key) {
		Indexer id = (Indexer) g.getUserDatum(key);
		return (id != null);
	}

	// reCreate: create a new index on the graph.
	// reIndex: only applicable if recreate is false and the graph has an old
	// index: we shoudl throw an exception if the graph has changed
	private static Indexer getIndexer(
		ArchetypeGraph g,
		Object key,
		boolean reIndex,
		boolean recreate,
		int offset) {
		Indexer id = (Indexer) g.getUserDatum(key);
		if (!recreate && id != null) {
			if (id.numNodes != g.getVertices().size()) {
				if (reIndex == false) {
					throw new FatalException("Graph changed since last index update");
				} else {
					id = null;
				}
			} else {
				return id;
			}
		}
		id = new Indexer(g);
		id.updateIndex(offset);
		g.setUserDatum(key, id, UserData.REMOVE);
		return id;
	}

	int numNodes;

	/**
	 * Clears previous index (if it existed); puts in a new one. Merely follows
	 * graph.getVertices() iterator order, which is not guaranteed to have any
	 * nice properties at all. When complete, the index will be numbered from <code>offset</code>
	 * to <code>offset + n - 1</code> (where <code>n = g.numVertices()</code>), 
     * and will be accessible through 
     * <code>getIndex( Vertex)</code> and <code>getVertex( index )</code>.
	 */
	public void updateIndex(int offset) {
//		indexToVertex.clear();
        indexToVertex = new ArchetypeVertex[graph.numVertices() + offset];
		vertexToIndex.clear();
		int i = offset;
		for (Iterator iter = graph.getVertices().iterator(); iter.hasNext();) {
			ArchetypeVertex v = (ArchetypeVertex) iter.next();
			Integer ix = new Integer(i);
            indexToVertex[i] = v;
//			indexToVertex.put(ix, v);
			vertexToIndex.put(v, ix);
			i++;
		}
		numNodes = graph.getVertices().size();
	}

	/**
	 * Forces an index update, reindexing from zero.
     * Equivalent to <code>updateIndex(0)</code>.
	 */
	public void updateIndex() {
		updateIndex(0);
	}

	/**
	 * Gets the index assocated with this vertex.
	 */
	public int getIndex(ArchetypeVertex v) {
		return ((Integer) vertexToIndex.get(v)).intValue();
	}

	/**
	 * Gets the vertex associated with this index.
	 */
	public ArchetypeVertex getVertex(int i) {
//		return (ArchetypeVertex) indexToVertex.get(new Integer(i));
        return indexToVertex[i];
	}

//	private Map indexToVertex = new HashMap();
    private ArchetypeVertex[] indexToVertex;
	private Map vertexToIndex = new HashMap();
	private ArchetypeGraph graph;

	private Indexer(ArchetypeGraph g) {
		this.graph = g;
	}

//    public void setIndex(Vertex v, Integer i) { //throws ImproperIndexException {
//        if (graph.getVertices().contains(v)) {
//           //if (indexToVertex.containsKey(i)) {
//                // we already have a vertex with this label
//            //    throw new ImproperIndexException(i + "is already on vertex " + indexToVertex.get(i));
//           // }
//            // ok, we know we don't have this label anywhere yet
//            if (vertexToIndex.containsKey(v)) {
//                Object junk = vertexToIndex.get(v);
//                indexToVertex.remove(junk);
//            }
//            vertexToIndex.put(v, i);
//            indexToVertex.put(i, v);
//        } else {
//            // throw some sort of exception here
//            throw new IllegalArgumentException("This vertex is not a part of this graph");
//        }
//
//    }
//
//    public static class ImproperIndexException extends Exception {
//
//        public ImproperIndexException(String string) {
//            super(string);
//        }
//
//    }

}