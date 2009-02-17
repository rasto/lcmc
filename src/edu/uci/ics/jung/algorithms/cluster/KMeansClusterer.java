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
 * Created on Aug 9, 2004
 *
 */
package edu.uci.ics.jung.algorithms.cluster;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import cern.jet.random.engine.DRand;
import cern.jet.random.engine.RandomEngine;
import edu.uci.ics.jung.statistics.DiscreteDistribution;



/**
 * Groups Objects into a specified number of clusters, based on their 
 * proximity in d-dimensional space, using the k-means algorithm. 
 *  
 * @author Joshua O'Madadhain
 */
public class KMeansClusterer
{
    protected int max_iterations;
    protected double convergence_threshold;
    protected RandomEngine rand = new DRand();
    
    /**
     * Creates an instance for which calls to <code>cluster</code> will terminate
     * when either of the two following conditions is true:
     * <ul>
     * <li/>the number of iterations is > <code>max_iterations</code>
     * <li/>none of the centroids has moved as much as <code>convergence_threshold</code>
     * since the previous iteration
     * </ul>
     * @param max_iterations
     * @param convergence_threshold
     */
    public KMeansClusterer(int max_iterations, double convergence_threshold)
    {
        if (max_iterations < 0)
            throw new IllegalArgumentException("max iterations must be >= 0");
        
        if (convergence_threshold <= 0)
            throw new IllegalArgumentException("convergence threshold " + 
                "must be > 0");
        
        this.max_iterations = max_iterations;
        this.convergence_threshold = convergence_threshold;
    }
    
    /**
     * Returns a <code>Collection</code> of clusters, where each cluster is
     * represented as a <code>Map</code> of <code>Objects</code> to locations
     * in d-dimensional space. 
     * @param object_locations  a map of the Objects to cluster, to  
     * <code>double</code> arrays that specify their locations in d-dimensional space.
     * @param num_clusters  the number of clusters to create
     * @throws NotEnoughClustersException
     */
    public Collection cluster(Map object_locations, int num_clusters)
    {
        if (num_clusters < 2 || num_clusters > object_locations.size())
            throw new IllegalArgumentException("number of clusters " + 
                "must be >= 2 and <= number of objects (" + 
                object_locations.size() + ")");
        
        if (object_locations == null || object_locations.isEmpty())
            throw new IllegalArgumentException("'objects' must be non-empty");

        Set centroids = new HashSet();
        Object[] obj_array = object_locations.keySet().toArray();
        Set tried = new HashSet();
        
        // create the specified number of clusters
        while (centroids.size() < num_clusters && tried.size() < object_locations.size())
        {
            Object o = obj_array[(int)(rand.nextDouble() * obj_array.length)];
            tried.add(o);
            double[] mean_value = (double[])object_locations.get(o);
            boolean duplicate = false;
            for (Iterator iter = centroids.iterator(); iter.hasNext(); )
            {
                double[] cur = (double[])iter.next();
                if (Arrays.equals(mean_value, cur))
                    duplicate = true;
            }
            if (!duplicate)
                centroids.add(mean_value);
        }
        
        if (tried.size() >= object_locations.size())
            throw new NotEnoughClustersException();
        
        // put items in their initial clusters
        Map clusterMap = assignToClusters(object_locations, centroids);
        
        // keep reconstituting clusters until either 
        // (a) membership is stable, or
        // (b) number of iterations passes max_iterations, or
        // (c) max movement of any centroid is <= convergence_threshold
        int iterations = 0;
        double max_movement = Double.POSITIVE_INFINITY;
        while (iterations++ < max_iterations && max_movement > convergence_threshold)
        {
            max_movement = 0;
            Set new_centroids = new HashSet();
            // calculate new mean for each cluster
            for (Iterator iter = clusterMap.keySet().iterator(); iter.hasNext(); )
            {
                double[] centroid = (double[])iter.next();
                Map elements = (Map)clusterMap.get(centroid);
                double[][] locations = new double[elements.size()][];
                int i = 0;
                for (Iterator e_iter = elements.keySet().iterator(); e_iter.hasNext(); )
                    locations[i++] = (double[])object_locations.get(e_iter.next());
                
                double[] mean = DiscreteDistribution.mean(locations);
                max_movement = Math.max(max_movement, 
                    Math.sqrt(DiscreteDistribution.squaredError(centroid, mean)));
                new_centroids.add(mean);
            }
            
            // TODO: check membership of clusters: have they changed?

            // regenerate cluster membership based on means
            clusterMap = assignToClusters(object_locations, new_centroids);
        }
        return (Collection)clusterMap.values();
    }

    /**
     * Assigns each object to the cluster whose centroid is closest to the
     * object.
     * @param object_locations  a map of objects to locations
     * @param centroids         the centroids of the clusters to be formed
     * @return a map of objects to assigned clusters
     */
    protected Map assignToClusters(Map object_locations, Set centroids)
    {
        Map clusterMap = new HashMap();
        for (Iterator c_iter = centroids.iterator(); c_iter.hasNext(); )
            clusterMap.put(c_iter.next(), new HashMap());
        
        for (Iterator o_iter = object_locations.keySet().iterator(); o_iter.hasNext(); )
        {
            Object o = o_iter.next();
            double[] location = (double[])object_locations.get(o);

            // find the cluster with the closest centroid
            Iterator c_iter = centroids.iterator();
            double[] closest = (double[])c_iter.next();
            double distance = DiscreteDistribution.squaredError(location, closest);
            
            while (c_iter.hasNext())
            {
                double[] centroid = (double[])c_iter.next();
                double dist_cur = DiscreteDistribution.squaredError(location, centroid);
                if (dist_cur < distance)
                {
                    distance = dist_cur;
                    closest = centroid;
                }
            }
            Map elements = (Map)clusterMap.get(closest);
            elements.put(o, location);
        }
        
        return clusterMap;
    }
    
    public void setSeed(int random_seed)
    {
        this.rand = new DRand(random_seed);
    }
    
    /**
     * An exception that indicates that the specified data points cannot be
     * clustered into the number of clusters requested by the user.
     * This will happen if and only if there are fewer distinct points than 
     * requested clusters.  (If there are fewer total data points than 
     * requested clusters, <code>IllegalArgumentException</code> will be thrown.)
     *  
     * @author Joshua O'Madadhain
     */
    public static class NotEnoughClustersException extends RuntimeException
    {
        public String getMessage()
        {
            return "Not enough distinct points in the input data set to form " +
                    "the requested number of clusters";
        }
    }
}
