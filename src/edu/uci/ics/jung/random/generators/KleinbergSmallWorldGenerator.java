/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.random.generators;

import java.util.Arrays;

import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.Indexer;
import edu.uci.ics.jung.utils.GraphUtils;

/**
 * Graph generator that produces a random graph with small world properties. The underlying model is
 * an nxn toroidal lattice. Each node u has four local connections, one to each of its neighbors, and
 * in addition one long range connection to some node v where v is chosen randomly according to
 * probability proportional to d^-alpha where d is the lattice distance between u and v and alpha
 * is the clustering expononent.
 * @see "Navigation in a small world J. Kleinberg, Nature 406(2000), 845."
 * @author Scott White
 */
public class KleinbergSmallWorldGenerator extends Lattice2DGenerator {
    //private int mNumNodes;
    private double mClusteringExponent;
    private int mLongRangeDistanceDistributionsSize;
    private int[] mLongRangeDistanceDistributions;

   /**
    * Constructs the small world graph generator.
    * @param latticeSize the lattice size (length of row or column dimension)
    * @param clusteringExponent the clustering exponent parameter (somewhere around 2 is a good choice)
    */
    public KleinbergSmallWorldGenerator(int latticeSize, double clusteringExponent) {
        super(latticeSize,true);

		mLongRangeDistanceDistributionsSize = getLatticeSize() * 1000;
        mClusteringExponent = clusteringExponent;
    }

    /**
     * Generates a random small world network according to the parameters given
     * @return a random small world graph
     */
    public ArchetypeGraph generateGraph() {
    	
    	Lattice2DGenerator generator = new Lattice2DGenerator(getLatticeSize(),true);
    	Graph graph = (Graph) generator.generateGraph();
    	
		Indexer id = Indexer.getIndexer( graph ) ;
		
		computeLongDistanceEdgeDistributionSample();
		
		int numNodes = (int) Math.pow(getLatticeSize(), 2);
		
		//Add long range connections
		for (int i = 0; i < numNodes; i++) {            

            //choose random distance
            int sampleNodeIndex = (int) (Math.random() * mLongRangeDistanceDistributionsSize);
            int randomDistance = mLongRangeDistanceDistributions[sampleNodeIndex];
            while (true) {
                int randomNodeIndex = simulatePath(i, randomDistance);

                Vertex source =  (Vertex) id.getVertex(i);
                Vertex target =  (Vertex) id.getVertex(randomNodeIndex);

                if (!target.isSuccessorOf(source)) {
                    GraphUtils.addEdge( graph, source, target);
                    break;
                }

            }
        }

        return graph;
    }

    private static int pickValue(boolean[] choices) {

        int totalNumChoicesLeft= 0;
        for (int x =0;x<choices.length;x++) {
            if (choices[x]) {
                  totalNumChoicesLeft++;
            }
        }

        double randValue = Math.random();
        int i = 1;

        for (;i<=totalNumChoicesLeft; i++) {
            if (randValue < (double) i/ (double) totalNumChoicesLeft) {
                break;
            }
        }

        int currentChoice = 1;
        for (int j=0;i<choices.length;j++) {
            if (choices[j]) {
                if (currentChoice == i) {
                    return j+1;
                }
                currentChoice++;
            }
        }

        return currentChoice;
    }

    private int simulatePath(int index, int distance) {

        //1 = up,2 = down,3 = left, 4 = right
        boolean[] currentChoices = new boolean[4];
        Arrays.fill(currentChoices, true);

        int numSteps = 0;
        int currentChoice = 0;
        int newIndex = 0;
        int xCoordinate = index / getLatticeSize();
        int yCoordinate = index % getLatticeSize();


        while (numSteps < distance) {

            currentChoice = pickValue(currentChoices);

            switch (currentChoice) {
                case 1:
                    {
                        currentChoices[1] = false;
                        newIndex = upIndex(xCoordinate, yCoordinate);
                        break;
                    }
                case 2:
                    {
                        currentChoices[0] = false;
                        newIndex = downIndex(xCoordinate, yCoordinate);
                        break;
                    }
                case 3:
                    {
                        currentChoices[3] = false;
                        newIndex = leftIndex(xCoordinate, yCoordinate);
                        break;
                    }
                case 4:
                    {
                        currentChoices[2] = false;
                        newIndex = rightIndex(xCoordinate, yCoordinate);
                        break;
                    }
            }
            xCoordinate = newIndex / getLatticeSize();
            yCoordinate = newIndex % getLatticeSize();

            numSteps++;
        }

        return newIndex;
    }


    public double getClusteringExponent() {
        return mClusteringExponent;
    }

    public void setClusteringExponent(double clusteringExponent) {
        this.mClusteringExponent = clusteringExponent;
    }

    private void computeLongDistanceEdgeDistributionSample() {
        int numLongRangeLevels = getLatticeSize() - 2;
        if ((getLatticeSize() % 2) == 0) {
            numLongRangeLevels = getLatticeSize() - 1;
        }

        double[] probDists = new double[numLongRangeLevels];
        double normalizingFactor = 0;
        int currentDistance = 2;
        for (int i = 0; i < numLongRangeLevels; i++) {
            probDists[i] = Math.pow(currentDistance, -1 * mClusteringExponent);
            normalizingFactor += probDists[i];
            currentDistance++;
        }

        for (int i = 0; i < numLongRangeLevels; i++) {
            probDists[i] /= normalizingFactor;
        }

        mLongRangeDistanceDistributions = new int[mLongRangeDistanceDistributionsSize];


        for (int i = 0; i < mLongRangeDistanceDistributionsSize; i++) {
            currentDistance = 2;
            double currentCumProb = 0;
            double randomVal = Math.random();

            for (int j = 0; j < numLongRangeLevels; j++) {
                currentCumProb += probDists[j];
                if (randomVal < currentCumProb) {
                    mLongRangeDistanceDistributions[i] = currentDistance;
                    break;
                }
                currentDistance += 1;
            }
        }
    }
}
