package denoptim.fragmenter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.vecmath.Point3d;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.clustering.MultiKMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.clustering.evaluation.SumOfClusterVariances;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.biojava.nbio.structure.geometry.SuperPositionSVD;

import denoptim.exception.DENOPTIMException;
import denoptim.utils.Randomizer;

/**
 * Performs k-mean clustering on the data using as measure of the distance 
 * between fragments the RMSD upon alignment of the structures. The latter 
 * considers atoms and attachment points as points in space, and their order
 * is expected to be consistent throughout the data (if not consider
 * using {@link FragmentAlignement} to fins an isomorphism and define an
 * order in the {@link ClusterableFragment} wrapper of the {@link Fragment}.
 * 
 * This implementation is inspired by that from "Data Science with JAVA, O'Reilly Media Inc.
 * 
 * @author Marco Foscato
 */

public class FragmentClusterer
{
    /**
     * Maximum number of clusters
     */
    private int maxK = 20;
    
    /**
     * Maximum number of cluster refinement iteration for a given K
     */
    private int maxIter = 50;
    
    /**
     * Maximum number of independent clustering trials for a given K. We'll 
     * take the best result from any of these trials.
     */
    private int maxTrials = 10;
    
    /**
     * Maximum number of fragment used to detect unimodality. This is the number
     * of fragment for which we calculate the distribution of distances to all 
     * other fragments and perform unimodality test on the distribution function.
     */
    private int maxSampleForUnimodal = 10;
    
    /**
     * The list of fragments that should be organized into clusters.
     */
    private List<ClusterableFragment> data;
    
    /**
     * Clusters organized by K
     */
    private Map<Integer,List<CentroidCluster<ClusterableFragment>>> clusters = 
            new HashMap<Integer,List<CentroidCluster<ClusterableFragment>>>();
    
    /**
     * Sum of cluster variance organized by K
     */
    private Map<Integer,Double> sumsOfVariance = new HashMap<Integer,Double>();
    
    /**
     * Silhouette coefficients organized by K
     */
    private Map<Integer,Double> silhouetteCoeffs = new HashMap<Integer,Double>();
    
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor for an alignment of two fragments. 
     * Note we first have to find an ordering of the atoms/AP that is consistent.
     * Thus we check for isomorphism between the fragments.
     * @param fragA
     * @param fragB
     * @throws DENOPTIMException if an isomorphism is not found.
     */
    public FragmentClusterer(List<ClusterableFragment> data)
    {
        this.data = data;
    }
    
//------------------------------------------------------------------------------

    public boolean cluster()
    {
        /*
        if (isDataUnimodal())
        {
            // This to run only the case where K = 1 and get the stats as if K>1
            maxK = 2; 
        }
        */
        // Clustering 
        for (int k=1; k<Math.min(data.size(), maxK); k++)
        {
            // Configure tools
            KMeansPlusPlusClusterer<ClusterableFragment> kmppClusterer = 
                    new KMeansPlusPlusClusterer<ClusterableFragment>(k, maxIter, 
                            new DistanceAsRMSD());
            MultiKMeansPlusPlusClusterer<ClusterableFragment> multiKmpp = 
                    new MultiKMeansPlusPlusClusterer<ClusterableFragment>(
                            kmppClusterer, maxTrials);
            
            // Perform multiple clustering attempts
            List<CentroidCluster<ClusterableFragment>> result = 
                    multiKmpp.cluster(data);
            this.clusters.put(k, result);
            
            // Evaluate clustering
            SumOfClusterVariances<ClusterableFragment> scv =
                    new SumOfClusterVariances<ClusterableFragment>(
                            new DistanceAsRMSD());
            sumsOfVariance.put(k, scv.score(result));
            if (k>1 && k<data.size()-1)
                silhouetteCoeffs.put(k, calculateSilhouetteCoefficient(result));
        }
        return true;
    }
    
//------------------------------------------------------------------------------
    
    private boolean isDataUnimodal()
    {
        // We test for unimodality on a limited sample of points
        Randomizer rng = new Randomizer();
        Set<ClusterableFragment> sampled = new HashSet<ClusterableFragment>();
        while (sampled.size() < Math.min(data.size(),maxSampleForUnimodal))
        {
            sampled.add(rng.randomlyChooseOne(data));
        }
        
        int numUnimodalResults = 0;
        DistanceAsRMSD distanceCalc = new DistanceAsRMSD();
        for (ClusterableFragment thisFrag : sampled)
        {
            int i=0;
            double[] rmsds = new double[data.size()-1];
            for (ClusterableFragment otherFrag : data)
            {
                rmsds[i] = distanceCalc.compute(thisFrag.getPoint(), 
                        otherFrag.getPoint());
                i++;
            }
            

            //TODO-gg 
            
            if (false)
                numUnimodalResults++;
        }
        
        if (numUnimodalResults > ((sampled.size()/2)+1))
            return true;
        else 
            return false;
    }
    
//------------------------------------------------------------------------------
    
    
    public static boolean testUnimodality(double[] data) throws DENOPTIMException
    {
        if (data.length<10)
            throw new DENOPTIMException("Need at least ten data points to do "
                    + "statistics and infere modality of data.");
        
        int nBins = Math.max(data.length/10, 6);
        EmpiricalDistribution dataDistribution = new EmpiricalDistribution(nBins);
        dataDistribution.load(data);
        
        List<SummaryStatistics> ss = dataDistribution.getBinStats();
        double[] distributionProfile = new double[nBins];
        for (int j=0; j<nBins; j++)
        {
            SummaryStatistics s = ss.get(j);
            distributionProfile[j] = s.getN();
          //TODO-gg del
            System.out.println("-> "+s.getN());
        }
        EmpiricalDistribution distDistribution = new EmpiricalDistribution(nBins);
        distDistribution.load(distributionProfile);
        double origVariance = distDistribution.getNumericalVariance();
        
      //TODO-gg del
        System.out.println("OV "+origVariance);
        
        double minVariance = Double.MAX_VALUE;
        for (int splitPt=1; splitPt<nBins-1; splitPt++)
        {
            double movingPartUpperLimit = dataDistribution.getUpperBounds()[splitPt];
            
            // Build a new distribution using all point above movingPartUpperLimit
            // as they are, and mirroring all those below movingPartUpperLimit
            // onto the other side of movingPartUpperLimit.
            List<Double> foldedDataLst = new ArrayList<Double>();
            for (int dataIdx=0; dataIdx<data.length; dataIdx++)
            {
                double value = data[dataIdx];
                if (value < movingPartUpperLimit)
                {
                    double newValue = movingPartUpperLimit 
                            + (movingPartUpperLimit-value);
                    foldedDataLst.add(newValue);
                } else {
                    foldedDataLst.add(value);
                }
            }
            double[] foldedData = new double[foldedDataLst.size()];
            int id = 0;
            for (Double d : foldedDataLst)
            {
                foldedData[id] = d;
                id++;
            }
            EmpiricalDistribution foldedDataDistribution = 
                    new EmpiricalDistribution(nBins);
            foldedDataDistribution.load(foldedData);
            
            // Analyze the folded data to get the variance of the distribution
            List<SummaryStatistics> foldedSs = foldedDataDistribution.getBinStats();
            double[] foldedDistributionProfile = new double[nBins];
            for (int j=0; j<nBins; j++)
            {
                SummaryStatistics s = foldedSs.get(j);
                foldedDistributionProfile[j] = s.getN();
            }
            EmpiricalDistribution foldedDistDistribution = 
                    new EmpiricalDistribution(nBins);
            foldedDistDistribution.load(foldedDistributionProfile);
            double foldedVariance = foldedDistDistribution.getNumericalVariance();
            
            if (foldedVariance < minVariance)
                minVariance = foldedVariance;

            //TODO-gg del
            System.out.println(" "+splitPt+" "+foldedVariance);
        }
        
        System.out.println(" ");
        
        //TODO-gg deldouble[] dipTestResult = DistributionTest.diptest(distProfile);
        
        //TODO-gg
        return true;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Distance in terms of RMSD between sets of 3D points expressed as a 
     * single vector of coordinates [x1,y1,z1,x2,y2,z2,...xN,yN,zN]
     */
    @SuppressWarnings("serial")
    public static class DistanceAsRMSD implements DistanceMeasure
    {
        @Override
        public double compute(double[] coordsA, double[] coordsB)
                throws DimensionMismatchException
        {
            Point3d[] ptsA = new Point3d[coordsA.length/3];
            Point3d[] ptsB = new Point3d[coordsB.length/3];
            for (int i=0; i<coordsA.length/3; i++)
            {
                int j = i*3;
                ptsA[i] = new Point3d(coordsA[j],
                        coordsA[j+1],
                        coordsA[j+2]);
                ptsB[i] = new Point3d(coordsB[j],
                        coordsB[j+1],
                        coordsB[j+2]);
            }
            SuperPositionSVD svd = new SuperPositionSVD(false);
            return svd.getRmsd(ptsA,ptsB);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Calculated the silhouette score as the mean of the sample silhouette
     * coefficient for each point as calculated by 
     * <pre>
     *          b_i - a_i
     * S_i = --------------
     *        max(a_i ,b_i)
     * </pre>
     * Where:<ul>
     * <li>a_i is the mean distance between a point and the other points in the 
     * cluster that point belongs to.</li>
     * <li>b_i is the mean distance between the point and all the points in the 
     * cluster that is closes to the cluster where that the point belongs to. 
     * Note we calculate all the possible b_i and chose the minimum, i.e., that
     * for the closest cluster).</li>
     * </ul>.
     * This method calculates the mean of S_i.
     * @param clusters the cluster collection to analyze.
     * @param the silhouette coefficient.
     */
    public static double calculateSilhouetteCoefficient(
            List<CentroidCluster<ClusterableFragment>> clusters)
    {
        SummaryStatistics stats = new SummaryStatistics();
        int clusterNumber = 0;
        for (CentroidCluster<ClusterableFragment> cluster : clusters) 
        {
            for (ClusterableFragment cf : cluster.getPoints()) 
            {
                double s = calculateCoefficientForOnePoint(cf, clusterNumber, 
                        clusters);
                stats.addValue(s);
            }
            clusterNumber++;
        }
        return stats.getMean();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Calculates the sample silhouette coefficient:
     * <pre>
     *         b_i - a_i
     * S_i = --------------
     *        max(a_i ,b_i)
     * </pre>
     * Where:<ul>
     * <li>a_i is the mean distance between a point and the other points in the 
     * cluster that point belongs to.</li>
     * <li>b_i is the mean distance between the point and all the points in the 
     * cluster that is closes to the cluster where that the point belongs to. 
     * Note we calculate all the possible b_i and chose the minimum, i.e., that
     * for the closest cluster).</li>
     * </ul>
     * @param thisCf the point for which we calculate the distances.
     * @param thisCfClusterNumber the index of the cluster hosting thisCf.
     * @param clusters the collection of clusters
     * @return S_i
     */
    public static double calculateCoefficientForOnePoint(
            ClusterableFragment thisCf,
            int thisCfClusterNumber,
            List<CentroidCluster<ClusterableFragment>> clusters)
    {     
        DistanceAsRMSD distceMeter = new DistanceAsRMSD();
        double meanInnerDist = 0.0;
        double minMeanExternalDist = Double.MAX_VALUE;
        int clusterNumber = 0;
        for (CentroidCluster<ClusterableFragment> cluster : clusters) 
        {
            SummaryStatistics clusterStats = new SummaryStatistics(); 
            for (ClusterableFragment otherCf : cluster.getPoints()) 
            {
                double dist = 0.0;
                try
                {
                    if (thisCf!=otherCf)
                    {
                        dist = distceMeter.compute(thisCf.getPoint(),
                                otherCf.getPoint());
                    }
                } catch (DimensionMismatchException e)
                {
                    //Should never happen, we have done it before.
                    e.printStackTrace();
                }
                clusterStats.addValue(dist); 
            }
            double meanDist = clusterStats.getMean();
            if(clusterNumber == thisCfClusterNumber) 
            {
                double n = Double.valueOf(clusterStats.getN());
                if (n!=1)
                    meanInnerDist = meanDist * n / (n - 1.0); 
            } else {
                minMeanExternalDist = Math.min(meanDist, minMeanExternalDist); 
            }
            clusterNumber++; 
        }
        return (minMeanExternalDist-meanInnerDist)
                / Math.max(meanInnerDist, minMeanExternalDist); 
    }
    
//------------------------------------------------------------------------------

    /**
     * Once the clustering is done, this method returns the values of sum of 
     * variance for number of clusters (k).
     * @return the values of sum of variance for number of clusters (k).
     */
    public Map<Integer,Double> getSunOfVariance()
    {
        return sumsOfVariance;
    }
    
//------------------------------------------------------------------------------

    /**
     * Once the clustering is done, this method returns the values of 
     * silhouette coefficient for number of clusters (k).
     * @return the values of sum of variance for number of clusters (k).
     */
    public Map<Integer,Double> getSilhouetteCoeffs()
    {
        return silhouetteCoeffs;
    }
    
//------------------------------------------------------------------------------

    /**
     * Once the clustering is done, this method return the clusters obtained
     * with a given K in k-mean method.
     * @param k the number of cluster.
     * @return the clusters.
     */
    public List<CentroidCluster<ClusterableFragment>> getClusters(int k)
    {
        return clusters.get(k); 
    }

//------------------------------------------------------------------------------
    
    /**
     * Returns the set of k-mean clusters that best describes the set of 
     * clusterable fragments provided to the constructor. The number of clusters
     * (i.e., k) is chosen by combining two decision criteria:
     * <ul>
     * <li>determining the elbow in the plot
     * of the sum of variance (y-axis) by the number of clusters k (x-axis), 
     * and</li>
     * <li>determining the value of k leading to the largest silhouette 
     * coefficient above 0.65.</li>
     * </ul>
     * If the elbow and silhouette strategies disagree on the choice of k,
     * this method chooses the largest k from the two methods.
     * @return the chosen clustering.
     */
    public List<CentroidCluster<ClusterableFragment>> getBestSet()
    {
        return getBestSet(null);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the set of k-mean clusters that best describes the set of 
     * clusterable fragments provided to the constructor. The number of clusters
     * (i.e., k) is chosen by combining two decision criteria:
     * <ul>
     * <li>determining the elbow in the plot
     * of the sum of variance (y-axis) by the number of clusters k (x-axis), 
     * and</li>
     * <li>determining the value of k leading to the largest silhouette 
     * coefficient above 0.65.</li>
     * </ul>
     * If the elbow and silhouette strategies disagree on the choice of k,
     * this method chooses the largest k from the two methods.
     * @param logger where log messages should be directed.
     * @return the chosen clustering.
     */
    public List<CentroidCluster<ClusterableFragment>> getBestSet(Logger logger)
    {
        // Elbow method
        int size = sumsOfVariance.size();
        double[] kvalues = new double[size-1];
        double[] sumOfVariance = new double[size-1];
        for (int k=1; k<size; k++)
        {
            kvalues[k-1] = k;
            sumOfVariance[k-1] = sumsOfVariance.get(k);
        }
        int bestKfromElbow = findElbow(kvalues, sumOfVariance);
        
        // Silhouette method
        int bestKfromSilhouette = 1;
        double maxSilhouetteCoeff = Collections.max(silhouetteCoeffs.values());
        if (maxSilhouetteCoeff > 0.65)
        {
            double maxVal = Double.MAX_VALUE;
            for (Integer k : silhouetteCoeffs.keySet())
            {
                if (silhouetteCoeffs.get(k) > maxVal)
                {
                    maxVal = silhouetteCoeffs.get(k);
                    bestKfromSilhouette = k;
                }
            }
        } else {
            if (logger!=null)
                logger.log(Level.WARNING, "Max silhouette coefficient is too "
                    + "small (" + maxSilhouetteCoeff + ").");
        }
        
        int bestK = bestKfromElbow;
        if (bestKfromElbow != bestKfromSilhouette)
        {
            if (logger!=null)
            {
                logger.log(Level.WARNING, "Disagreement between elbow (best k = " 
                        + bestKfromElbow + ") and silhouette (best k = " 
                        + bestKfromSilhouette + ") methods. "
                        + "Using largest value for k-means clustering.");
            }
            bestK = Math.max(bestKfromElbow, bestKfromSilhouette);
        }
        return clusters.get(bestK); 
    }
    
 //-----------------------------------------------------------------------------

    /**
     * Detects the elbow in the profile defined by the two vectors. Implements
     * the AutoElbow method described in 
     * Appl. Sci. 2022, 12(15), 7515; https://doi.org/10.3390/app12157515.
     * We assume a lower-left elbow shape of the graph {x,y}
     * @param x the x-coordinates on the graph where to detect the lower-left 
     * elbow. The coordinate is expected to be integer values reported as doubles.
     * @param y the x-coordinates on the graph where to detect the lower-left 
     * elbow.
     * @return the value of x at the elbow.
     */
    protected static int findElbow(double[] x, double[] y)
    {
        RealVector vx = new ArrayRealVector(x); // x is k
        RealVector vy = new ArrayRealVector(y); // y is eval measure
        
        int size = vx.getDimension();
        if (size==1)
            return 1;
        
        // Normalize
        double minx = Collections.min(Arrays.asList(ArrayUtils.toObject(x)));
        double maxx = Collections.max(Arrays.asList(ArrayUtils.toObject(x)));
        double miny = Collections.min(Arrays.asList(ArrayUtils.toObject(y)));
        double maxy = Collections.max(Arrays.asList(ArrayUtils.toObject(y)));
        RealVector vxn = new ArrayRealVector(size);
        RealVector vyn = new ArrayRealVector(size);
        for (int i=0; i<size; i++)
        {
            vxn.setEntry(i, (vx.getEntry(i) - minx) / (maxx-minx));
            vyn.setEntry(i, (vy.getEntry(i) - miny) / (maxy-miny));
        }
        
        // Smooth y profile.
        for (int i=1; i<size; i++)
        {
            if (vyn.getEntry(i) > vyn.getEntry(i-1))
            {
                // NB: this is a modification of the smoothing suggested in 
                // cited reference. It is needed because otherwise, with the
                // original smoothing (i.e., vyn.setEntry(i, vyn.getEntry(i+1))
                // elbow is moved of one step earlier in our tests.
                vyn.setEntry(i, vyn.getEntry(i+1) 
                        + (vyn.getEntry(i)-vyn.getEntry(i+1))/2);
            }
        }
        
        // Compute squares Euclidean distances
        RealVector ak = new ArrayRealVector(size);
        for (int i=0; i<size; i++)
        {
            ak.setEntry(i, Math.pow(vxn.getEntry(i),2)
                    + Math.pow(vyn.getEntry(i),2));
        }
        RealVector bk = new ArrayRealVector(size);
        for (int i=0; i<size; i++)
        {
            bk.setEntry(i, Math.pow(vxn.getEntry(i)-1, 2)
                    + Math.pow(vyn.getEntry(i)-1, 2));
        }
        RealVector ck = new ArrayRealVector(size);
        for (int i=0; i<size; i++)
        {
            ck.setEntry(i, Math.pow(vyn.getEntry(i),2));
        }
        
        // AutoElbow function
        RealVector fk = new ArrayRealVector(size);
        for (int i=0; i<size; i++)
        {
            fk.setEntry(i, bk.getEntry(i) / (ak.getEntry(i) + ck.getEntry(i)));
        }
        
        // k is the index of the best (+1 because we are 0-based here
        // but k = 1, ..., N)
        return fk.getMaxIndex()+1;
    }
    
//------------------------------------------------------------------------------

}
