/*
 * Copyright (c) 2004, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 * 
 * Created on Aug 12, 2004
 */
package edu.uci.ics.jung.algorithms.cluster;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import cern.jet.random.engine.DRand;
import cern.jet.random.engine.RandomEngine;
import edu.uci.ics.jung.algorithms.cluster.KMeansClusterer.NotEnoughClustersException;
import edu.uci.ics.jung.algorithms.importance.VoltageRanker;
import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.UserDatumNumberVertexValue;
import edu.uci.ics.jung.statistics.DiscreteDistribution;

/**
 * <p>Clusters vertices of a <code>Graph</code> based on their ranks as 
 * calculated by <code>VoltageRanker</code>.  This algorithm is based on,
 * but not identical with, the method described in the paper below.
 * The primary difference is that Wu and Huberman assume a priori that the clusters
 * are of approximately the same size, and therefore use a more complex 
 * method than k-means (which is used here) for determining cluster 
 * membership based on co-occurrence data.</p>
 * 
 * <p>The algorithm proceeds as follows:
 * <ul>
 * <li/>first, generate a set of candidate clusters as follows:
 *      <ul>
 *      <li/>pick (widely separated) vertex pair, run VoltageRanker
 *      <li/>group the vertices in two clusters according to their voltages
 *      <li/>store resulting candidate clusters
 *      </ul>
 * <li/>second, generate k-1 clusters as follows:
 *      <ul>
 *      <li/>pick a vertex v as a cluster 'seed'
 *           <br>(Wu/Huberman: most frequent vertex in candidate clusters)
 *      <li/>calculate co-occurrence over all candidate clusters of v with each other
 *           vertex
 *      <li/>separate co-occurrence counts into high/low;
 *           high vertices constitute a cluster
 *      <li/>remove v's vertices from candidate clusters; continue
 *      </ul>
 * <li/>finally, remaining unassigned vertices are assigned to the kth ("garbage")
 * cluster.
 * </ul></p>
 * 
 * <p><b>NOTE</b>: Depending on how the co-occurrence data splits the data into
 * clusters, the number of clusters returned by this algorithm may be less than the
 * number of clusters requested.  The number of clusters will never be more than 
 * the number requested, however.</p>
 * 
 * @author Joshua O'Madadhain
 * @see <a href="http://www.hpl.hp.com/research/idl/papers/linear/">'Finding communities in linear time: a physics approach', Fang Wu and Bernardo Huberman</a>
 * @see VoltageRanker
 * @see KMeansClusterer
 */
public class VoltageClusterer
{
    public final static String VOLTAGE_KEY = "edu.uci.ics.jung.algorithms.cluster.VoltageClusterer.KEY";
    
    protected int num_candidates;
    protected KMeansClusterer kmc;
    protected UserDatumNumberVertexValue vv;
    protected VoltageRanker vr;
    protected RandomEngine rand;
    
    /**
     * Creates an instance of a VoltageCluster with the specified parameters.
     * These are mostly parameters that are passed directly to VoltageRanker
     * and KMeansClusterer.
     * 
     * @param num_candidates    the number of candidate clusters to create
     * @param rank_iterations   the number of iterations to run VoltageRanker
     * @param cluster_iterations    the number of iterations to run KMeansClusterer
     * @param cluster_convergence   the convergence value for KMeansClusterer
     */
    public VoltageClusterer(int num_candidates, int rank_iterations, 
        double rank_convergence, int cluster_iterations, double cluster_convergence)
    {
        if (num_candidates < 1)
            throw new IllegalArgumentException("must generate >=1 candidates");

        this.num_candidates = num_candidates;
        this.vv = new UserDatumNumberVertexValue(VOLTAGE_KEY);
        this.vr = new VoltageRanker(vv, rank_iterations, rank_convergence);
        this.kmc = new KMeansClusterer(cluster_iterations, cluster_convergence);
        rand = new DRand();
    }
    
    protected void setRandomSeed(int random_seed)
    {
        rand = new DRand(random_seed);
    }
    
    /**
     * Returns a community (cluster) centered around <code>v</code>.
     * @param v the vertex whose community we wish to discover
     */
    public Collection getCommunity(ArchetypeVertex v)
    {
        return cluster_internal(v.getGraph(), v, 2);
    }

    /**
     * Clusters the vertices of <code>g</code> into 
     * <code>num_clusters</code> clusters, based on their connectivity.
     * @param g             the graph whose vertices are to be clustered
     * @param num_clusters  the number of clusters to identify
     */
    public Collection cluster(ArchetypeGraph g, int num_clusters)
    {
        return cluster_internal(g, null, num_clusters);
    }
    
    /**
     * Clears the voltage decoration values from the vertices of <code>g</code>.
     */
    public void clear(ArchetypeGraph g)
    {
        vv.clear(g);
    }
    
    /**
     * Does the work of <code>getCommunity</code> and <code>cluster</code>.
     * @param g             the graph whose vertices we're clustering
     * @param origin        the center (if one exists) of the graph to cluster
     * @param num_clusters  
     */
    protected Collection cluster_internal(ArchetypeGraph g, 
        ArchetypeVertex origin, int num_clusters)
    {
        // generate candidate clusters
        // repeat the following 'samples' times:
        // * pick (widely separated) vertex pair, run VoltageRanker
        // * use k-means to identify 2 communities in ranked graph
        // * store resulting candidate communities
        Set vertices = g.getVertices();
        int num_vertices = vertices.size();
        ArchetypeVertex[] v = new ArchetypeVertex[num_vertices];
        int i = 0;
        for (Iterator iter = vertices.iterator(); iter.hasNext(); )
            v[i++] = (ArchetypeVertex)iter.next();

        LinkedList candidates = new LinkedList();
        
        for (int j = 0; j < num_candidates; j++)
        {
            ArchetypeVertex source;
            if (origin == null)
                source = v[(int)(rand.nextDouble() * num_vertices)];
            else
                source = origin;
            ArchetypeVertex target = null;
            do 
            {
                target = v[(int)(rand.nextDouble() * num_vertices)];
            }
            while (source == target);
            vr.calculateVoltages((Vertex)source, (Vertex)target);
            
            Map voltage_ranks = new HashMap();
            for (Iterator iter = vertices.iterator(); iter.hasNext(); )
            {
                ArchetypeVertex w = (ArchetypeVertex)iter.next();
                double[] voltage = {vv.getNumber(w).doubleValue()};
                voltage_ranks.put(w, voltage);
            }
            
//            Map clusterMap;
            Collection clusters;
            try
            {
                clusters = kmc.cluster(voltage_ranks, 2);
                Iterator iter = clusters.iterator();
                candidates.add(((Map)iter.next()).keySet());
                candidates.add(((Map)iter.next()).keySet());
//                candidates.addAll(clusters);
            }
            catch (NotEnoughClustersException e)
            {
                // ignore this candidate, continue
            }
        }
        
        // repeat the following k-1 times:
        // * pick a vertex v as a cluster seed 
        //   (Wu/Huberman: most frequent vertex in candidates)
        // * calculate co-occurrence (in candidate clusters) 
        //   of this vertex with all others
        // * use k-means to separate co-occurrence counts into high/low; 
        //   high vertices are a cluster
        // * remove v's vertices from candidate clusters
        
        Collection clusters = new LinkedList();
        Collection remaining = new HashSet(g.getVertices());
        Object[] seed_candidates = getSeedCandidates(candidates);
        int seed_index = 0;
        
        for (int j = 0; j < (num_clusters - 1); j++)
        {
            if (remaining.isEmpty())
                break;
                
            Object seed;
            if (seed_index == 0 && origin != null)
                seed = origin;
            else
            {
                do { seed = seed_candidates[seed_index++]; } 
                while (!remaining.contains(seed));
            }
            
            Map occur_counts = getObjectCounts(candidates, seed);
            if (occur_counts.size() < 2)
                break;
            
            // now that we have the counts, cluster them...
            try
            {
                Collection high_low = kmc.cluster(occur_counts, 2);
                // ...get the cluster with the highest-valued centroid...
                Iterator h_iter = high_low.iterator();
                Map cluster1 = (Map)h_iter.next();
                Map cluster2 = (Map)h_iter.next();
                double[] centroid1 = DiscreteDistribution.mean(cluster1.values());
                double[] centroid2 = DiscreteDistribution.mean(cluster2.values());
//                double[] centroid1 = (double[])h_iter.next();
//                double[] centroid2 = (double[])h_iter.next();
                Collection new_cluster;
                if (centroid1[0] >= centroid2[0])
                    new_cluster = cluster1.keySet();
                else
                    new_cluster = cluster2.keySet();
                
                // ...remove the elements of new_cluster from each candidate...
                for (Iterator iter = candidates.iterator(); iter.hasNext(); )
                {
                    Collection cluster = (Collection)iter.next();
                    cluster.removeAll(new_cluster);
                }
                clusters.add(new_cluster);
                remaining.removeAll(new_cluster);
            }
            catch (NotEnoughClustersException nece)
            {
                // all remaining vertices are in the same cluster
                break;
            }
        }
        
        // identify remaining vertices (if any) as a 'garbage' cluster
        if (!remaining.isEmpty())
            clusters.add(remaining);
        
        return clusters;
    }
    
    protected Object getRandomElement(Collection c)
    {
        return c.toArray()[(int)(rand.nextDouble() * c.size())];
    }
    
    /**
     * Returns an array of cluster seeds, ranked in decreasing order
     * of number of appearances in the specified collection of candidate
     * clusters.
     * @param candidates
     */
    protected Object[] getSeedCandidates(Collection candidates)
    {
        final Map occur_counts = getObjectCounts(candidates, null);
        
        Object[] occurrences = occur_counts.keySet().toArray();
        Arrays.sort(occurrences, new Comparator() 
            {
                public int compare(Object arg0, Object arg1)
                {
                    double[] count0 = (double[])occur_counts.get(arg0);
                    double[] count1 = (double[])occur_counts.get(arg1);
                    if (count0[0] < count1[0])
                        return -1;
                    else if (count0[0] > count1[0])
                        return 1;
                    else
                        return 0;
                } 
            });
        return occurrences;
    }

    protected Map getObjectCounts(Collection candidates, Object seed)
    {
        Map occur_counts = new HashMap();
        for (Iterator iter = candidates.iterator(); iter.hasNext(); )
        {
            Collection candidate = (Collection)iter.next();
            if (seed == null || candidate.contains(seed))
            {
                for (Iterator c_iter = candidate.iterator(); c_iter.hasNext(); )
                {
                    Object element = c_iter.next();
                    double[] count = (double[])occur_counts.get(element);
                    if (count == null)
                    {
                        count = new double[1];
                        occur_counts.put(element, count);
                    }
                    else count[0]++;
                }
            }
        }
        
        return occur_counts;
    }
}
