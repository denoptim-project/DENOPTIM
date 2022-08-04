package denoptim.fragmenter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.biojava.nbio.structure.geometry.SuperPositionSVD;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.DGraph.StringFormat;
import denoptim.graph.Fragment;
import denoptim.programs.fragmenter.FragmenterParameters;

/**
 * Performs k-means clustering on the data using as measure of the distance 
 * between fragments the RMSD upon alignment of the structures. The latter 
 * considers atoms and attachment points as points in space, and their order
 * is expected to be consistent throughout the data (if not, consider
 * using {@link FragmentAlignement} to fins an isomorphism and define an
 * order in the {@link ClusterableFragment} wrapper of the {@link Fragment}.
 * 
 * This implementation of the clustering algorithm exploring multiple ks,
 * is inspired by that from "Data Science with JAVA, O'Reilly Media Inc.
 * 
 * Note, that unimodal data, which should not be clusterized because it is a
 * single cluster including all points, is detected empirically only by 
 * checking for small changes in the profile of the elbow plot. This mechanism
 * can be controlled by the parameter 
 * {@link FragmenterParameters#setThresholdRangeSoV(double)}.
 * 
 * @author Marco Foscato
 */

public class FragmentClusterer
{   
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
    
    /**
     * Settings from the user
     */
    protected FragmenterParameters settings;
    
    /**
     * Logger
     */
    private Logger logger;
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor for an alignment of two fragments. 
     * Note we first have to find an ordering of the atoms/AP that is consistent.
     * Thus we check for isomorphism between the fragments.
     * @param data collection of fragments to clusterize.
     * @param settings configuration of the clustering method
     * @throws DENOPTIMException if an isomorphism is not found.
     */
    public FragmentClusterer(List<ClusterableFragment> data,
            FragmenterParameters settings)
    {
        this.data = data;
        this.settings = settings;
        this.logger = settings.getLogger();
    }
    
//------------------------------------------------------------------------------

    /**
     * Performs k-means clustering of the collection of fragments given to the
     * constructor. 
     * The maximum number of clusters (k) is 
     * defined as the minimum value between 
     * 1/{@link #dataFraction} of the data size or 
     * {@value #maxK}, yet the minimum k is 1.
     */
    public void cluster()
    {
        int effectiveMaxK = Math.max(1, Math.min(data.size() 
                / settings.getDataFraction(), settings.getMaxK()));
        for (int k=1; k<effectiveMaxK; k++)
        {
            if (logger!=null)
            {
                logger.log(Level.FINE,"Starting k-means clustering with k="+k);
            }
            
            // Configure tools
            KMeansPlusPlusClusterer<ClusterableFragment> kmppClusterer = 
                    new KMeansPlusPlusClusterer<ClusterableFragment>(k, 
                            settings.getMaxIter(), new DistanceAsRMSD());
            MultiKMeansPlusPlusClusterer<ClusterableFragment> multiKmpp = 
                    new MultiKMeansPlusPlusClusterer<ClusterableFragment>(
                            kmppClusterer, settings.getMaxTrials());
            
            // Perform multiple clustering attempts
            List<CentroidCluster<ClusterableFragment>> result = 
                    multiKmpp.cluster(data);
            this.clusters.put(k, result);
            
            // Evaluate clustering
            SumOfClusterVariances<ClusterableFragment> scv =
                    new SumOfClusterVariances<ClusterableFragment>(
                            new DistanceAsRMSD());
            
            // Collect data used by elbow evaluation method
            sumsOfVariance.put(k, scv.score(result));
            
            // Collect data used by silhouette evaluation method
            if (k>1 && k<data.size()-1)
                silhouetteCoeffs.put(k, calculateSilhouetteCoefficient(result));
        }
        
        if (logger!=null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("K-means clustering done").append(settings.NL);
            sb.append("    k     Sum of     Silhouette").append(settings.NL);
            sb.append("         variance      coeff.  ");
            sb.append(settings.NL);
            for (int k=1; k<effectiveMaxK; k++)
            {
                sb.append(String.format(" %4d    ",k));
                sb.append(String.format("%7.4f      ",sumsOfVariance.get(k)));
                if (k==1)
                    sb.append(String.format("    -",silhouetteCoeffs.get(k)));
                else
                    sb.append(String.format("%7.4f ",silhouetteCoeffs.get(k)));
                sb.append(settings.NL);
            }
            sb.append(settings.NL);
            logger.log(Level.INFO,sb.toString());
        }
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
     * Returns the set of clusters that best describes the set of 
     * fragments provided to the constructor. The number of clusters
     * (i.e., k) is chosen by combining these decision criteria:
     * <ul>
     * <li>A small value for the sum of variance at k=1 and a too small maximum
     * value of the silhouette coefficient are interpreted as signs of an 
     * unimodal distribution. In such cases,
     * only one cluster is returned (best k=1).
     * <li>Identification of the elbow in the profile of the sum of variance 
     * over the number of clusters k (Elbow method).</li>
     * <li>Identification of the value of k leading to the largest silhouette 
     * coefficient above the threshold defined by 
     * {@value FragmenterParameters#throwsholdSilhouetteSignificance} (can
     * be configured by 
     * {@link FragmenterParameters#setThrowsholdSilhouetteSignificance(double)}
     * .</li>
     * </ul>
     * If the elbow and silhouette strategies disagree on the choice of k,
     * this method chooses the largest k from the two methods.
     * <p>To retrieve other sets of clusters beyond the chosen one, use 
     * {@link #getClusters(int)}</p>
     * 
     * @return the chosen clustering.
     */
    public List<CentroidCluster<ClusterableFragment>> getBestSet()
    {
        // Elbow method
        int bestKfromElbow = 1; 
        int size = sumsOfVariance.size();
        double[] kvalues = new double[size-1];
        double[] sumOfVariance = new double[size-1];
        for (int k=1; k<size; k++)
        {
            kvalues[k-1] = k;
            sumOfVariance[k-1] = sumsOfVariance.get(k);
        }

        // Verify elbow is significant
        double minSoV = Collections.min(sumsOfVariance.values());
        double maxSoV = Collections.max(sumsOfVariance.values());
        if ((maxSoV-minSoV) > settings.getThresholdRangeSoV())
        {
            // Numerically identify the elbow point
            bestKfromElbow = findElbow(kvalues, sumOfVariance);
        } else {
            if (logger!=null)
            {
                logger.log(Level.WARNING, "Range of sumOfVariance is "
                        + (maxSoV-minSoV) + ", i.e., lower than significance "
                        + "threshold "
                        + settings.getThresholdRangeSoV() 
                        + ". Elbow is too shallow! This looks like an unimodal "
                        + "dataset where there is only one cluster.");
            }  
        }
        
        // Silhouette method
        int bestKfromSilhouette = 1;
        double maxSilhouetteCoeff = Collections.max(silhouetteCoeffs.values());
        if (maxSilhouetteCoeff > settings.getThrowsholdSilhouetteSignificance())
        {
            double maxVal = Double.MIN_VALUE;
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
        } else {
            if (logger!=null)
            {
                logger.log(Level.INFO, "Elbow and Silhouette method "
                        + "consistently suggest to use k="+bestK);
            }
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
     * with a given k in k-means method.
     * @param k the number of cluster.
     * @return the clusters.
     */
    public List<CentroidCluster<ClusterableFragment>> getClusters(int k)
    {
        return clusters.get(k); 
    }

//------------------------------------------------------------------------------

}
