package denoptim.fragmenter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.vecmath.Point3d;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.biojava.nbio.structure.geometry.CalcPoint;
import org.biojava.nbio.structure.geometry.SuperPositionSVD;

import denoptim.exception.DENOPTIMException;
import denoptim.graph.Fragment;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.utils.MathUtils;
import denoptim.utils.Randomizer;

/**
 * <p>This tool clusters fragments based on geometry features. For each fragment 
 * all atoms and all attachment points are used to define a set of points in 
 * space (see {@link ClusterableFragment}). Then the RMSD of the 
 * points' position upon superposition is used to decide if geometries 
 * belong to the same cluster. The threshold RMSD value used to take the decision
 * is calculated from a unimodal distribution of geometries generated from the 
 * centroid of the cluster by altering its set of geometries with normally 
 * distributed noise. The population of these normally distorted geoemtries
 * is unimodal, by definition, and is used to calculate the threshold RMSD as
 * <pre>
 * threshold = RMSD_mean + x * RMSD_Standard_deviation
 * </pre>
 * where mean and standard deviation are the values for a normally distributed
 * noise-distorted population generated on-the-fly for the cluster centroid of 
 * interest.</p>
 * <p>The factor x above, the size of the noise-distorted population, and the
 * max amount of noise are parameters that are defined via the 
 * {@link FragmenterParameters} object given to the constructor.</p>
 * 
 * @author Marco Foscato
 */

public class FragmentClusterer
{   
    /**
     * The list of fragments to be clustered.
     */
    private List<ClusterableFragment> data;
    
    /**
     * Current list of clusters. Initially empty, then contains one
     * cluster per each data point, and then is pruned to retain only 
     * the clusters surviving the merging.
     */
    private List<DynamicCentroidCluster> clusters =
            new ArrayList<DynamicCentroidCluster>();

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
     * Constructor for a clusterer of fragments. Clustering is based on the
     * geometry of the arrangement of atoms and attachment points. To compare
     * the positions in space of each point consistently, we need a consistent 
     * mapping of the points, i.e., a 
     * definition of that is the correct order of points for each fragment to
     * analyze. Use {@link FragmentAlignement} to find such mapping to produce
     * {@link ClusterableFragment} that have a ordered sets of points 
     * reflecting a consistent mapping throughout the list of fragments.
     * @param data collection of fragments to clusterize. The coordinates vector
     * of each of these is expected to have a consistent ordering, but the 
     * value of the coordinates will be edited to align the geometries.
     * @param settings configuration of the clustering method. This includes the 
     * size, max amount of noise of the reference unimodal population with 
     * normally distributed noise used to calculate the RMSD of a unimodal
     * distribution of distotsions. It also define the factor used to weight 
     * the standard deviation when adding it to the mean of the RMSD of the 
     * unimodal population. The resulting value is the threshold RMSD value that
     * is used to decide if two geometries are part of the same unimodal
     * distribution, i.e., the same cluster.
     *  
     * @throws DENOPTIMException if an isomorphism is not found.
     */
    public FragmentClusterer(List<ClusterableFragment> data,
            FragmenterParameters settings)
    {
        this.data = data;
        this.settings = settings;
        this.logger = null;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor for a clusterer of fragments. Clustering is based on the
     * geometry of the arrangement of atoms and attachment points. To compare
     * the positions in space of each point consistently, we need a consistent 
     * mapping of the points, i.e., a 
     * definition of that is the correct order of points for each fragment to
     * analyze. Use {@link FragmentAlignement} to find such mapping to produce
     * {@link ClusterableFragment} that have a ordered sets of points 
     * reflecting a consistent mapping throughout the list of fragments.
     * @param data collection of fragments to clusterize. The coordinates vector
     * of each of these is expected to have a consistent ordering, but the 
     * value of the coordinates will be edited to align the geometries.
     * @param settings configuration of the clustering method. This includes the 
     * size, max amount of noise of the reference unimodal population with 
     * normally distributed noise used to calculate the RMSD of a unimodal
     * distribution of distotsions. It also define the factor used to weight 
     * the standard deviation when adding it to the mean of the RMSD of the 
     * unimodal population. The resulting value is the threshold RMSD value that
     * is used to decide if two geometries are part of the same unimodal
     * distribution, i.e., the same cluster.
     * @param logger where to put all log. Note that due to the likely 
     * parallelization of fragment clustering tasks, usually there are
     * multiple instances of this class, and we usually prefer to log each
     * instance independently. Therefore, here one can offer a specific logger
     * to use instead of that from the {@link FragmenterParameters} parameter.
     *  
     * @throws DENOPTIMException if an isomorphism is not found.
     */
    public FragmentClusterer(List<ClusterableFragment> data,
            FragmenterParameters settings, Logger logger)
    {
        this.data = data;
        this.settings = settings;
        this.logger = logger;
    }
    
//------------------------------------------------------------------------------

    /**
     * Runs the clustering algorithm:
     * <ol>
     * <li>creates a cluster for each fragment</li>
     * <li>tries to merge clusters. The condition for merging is that the 
     * the centroids of the clusters have an RMSD upon superposition that is 
     * lower than the threshold (see below).</li>
     * <li>repeat the merging until no more changes occur in the list of 
     * clusters.</li>
     * </ol>
     * The threshold for merging is derived from the RMSD of a sample of 
     * distorted geometries of the centroid, where the distortion is normally 
     * distributed. The threshold is calculated as:
     * <pre>
     * threshold = RMSD_mean + x * RMSD_standard_deviation
     * </pre>
     * where mean and standard deviation are calculated on the sample of normally
     * distorted geometries of the centroid (see 
     * {@link #getRMSDStatsOfNoisyDistorsions(double[], int, double)}). 
     * The factor x, the maximum about of
     * noise, and size of the sample are controlled by the settings given upon
     * construction of an instance of this class.
     */
    public void cluster()
    {
        // Start by assigning each data to its own cluster
        for (int i=0; i<data.size(); i++)
        {
            DynamicCentroidCluster cluster = new DynamicCentroidCluster(data.get(i));
            clusters.add(cluster);
        }
        
        // Iteratively try to refine clusters
        boolean hasChanged = true;
        int i=0;
        while (hasChanged && i<5)
        {
            hasChanged = mergeClusters();
            i++;
        }
        
        // Print summary
        if (logger!=null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Final number of clusters: ").append(clusters.size());
            sb.append(" (#iter. "+i+")");
            sb.append(settings.NL);
            for (int j=0; j<clusters.size(); j++)
            {
                sb.append("  Size cluster "+j+": ");
                sb.append(clusters.get(j).getPoints().size());
                sb.append(settings.NL);
            }
            logger.log(Level.INFO, sb.toString());
        }
        
    }
    
//------------------------------------------------------------------------------
    
    private boolean mergeClusters()
    {
        boolean somethingMoved = false;
        
        SuperPositionSVD svd = new SuperPositionSVD(false);
        
        Set<DynamicCentroidCluster> toRemoveClusters = 
                new HashSet<DynamicCentroidCluster>();
        for (int i=0; i<clusters.size(); i++)
        {   
            DynamicCentroidCluster clusterI = clusters.get(i);

            if (toRemoveClusters.contains(clusterI))
                continue;
            
            if (logger!=null)
            {
                logger.log(Level.FINE,"Clustering around centroid "+i+"...");
            }
            
            ClusterableFragment centroidI = 
                    (ClusterableFragment) clusterI.getCentroid();
            
            // Define a distance (RMSD upon superposition) for discriminating
            // this geometry from the others.
            SummaryStatistics refRMSDStats = getRMSDStatsOfNoisyDistorsions(
                    centroidI.getPoint(),
                    settings.getSizeUnimodalPop(),
                    settings.getMaxNoiseUnimodalPop());
            double rmsdThreshold = refRMSDStats.getMean() 
                    + settings.getFactorForSDOnStatsOfUnimodalPop() 
                    * refRMSDStats.getStandardDeviation();
            
            for (int j=i+1; j<clusters.size(); j++)
            {
                DynamicCentroidCluster clusterJ = 
                        clusters.get(j);
                
                if (toRemoveClusters.contains(clusterJ))
                    continue;
                
                //TODO: consider re-aligning to test alternative mappings. This
                // because the mapping is done once against the first item in 
                // the sample, but to distinguish sample members N!=1 and M!=1
                // a different mapping (i.e., a different isomorphism) might be
                // preferable.
                // Essentially, this means "get rid of the assumption that one 
                // isomorphism is suitable to align all members of the sample.
                
                ClusterableFragment centroidJ = (ClusterableFragment) 
                        clusterJ.getCentroid();
                
                Point3d[] ptsCentroidI = ClusterableFragment.convertToPointArray(
                        centroidI.getPoint());
                Point3d[] ptsCentroidJ = ClusterableFragment.convertToPointArray(
                        centroidJ.getPoint());
                svd.superposeAndTransform(ptsCentroidI, ptsCentroidJ);
                double rmsd = CalcPoint.rmsd(ptsCentroidI, ptsCentroidJ);
                if (rmsd < rmsdThreshold)
                {
                    somethingMoved = true;
                    toRemoveClusters.add(clusterJ);
                    if (logger!=null)
                    {
                        logger.log(Level.FINEST,"Merging cluster " + j + " into "
                                + "cluster " + i + " (RMSD " 
                                + String.format("%.4f", rmsd) + "<"
                                + String.format("%.4f", rmsdThreshold) + ").");
                    }
                    for (ClusterableFragment pointJ : clusterJ.getPoints())
                    {
                        Point3d[] ptsPointJ = ClusterableFragment.convertToPointArray(
                                pointJ.getPoint());
                        svd.superposeAndTransform(ptsCentroidI, ptsPointJ);
                        pointJ.setCoordsVector(ptsPointJ);
                        clusterI.addPoint(pointJ);
                    }
                } else {
                    // J looks like a cluster distinct from I. Try to move members
                    Set<ClusterableFragment> toRemoveFromJ = 
                            new HashSet<ClusterableFragment>();
                    for (ClusterableFragment pointJ : clusterJ.getPoints())
                    {
                        
                        Point3d[] ptsPointJ = ClusterableFragment.convertToPointArray(
                                pointJ.getPoint());
                        svd.superposeAndTransform(ptsCentroidI, ptsPointJ);
                        double rmsdJ = CalcPoint.rmsd(ptsCentroidJ, ptsPointJ);
                        svd.superposeAndTransform(ptsCentroidI, ptsPointJ);
                        double rmsdI = CalcPoint.rmsd(ptsCentroidI, ptsPointJ);
                        if (rmsdI < rmsdJ)
                        {
                            somethingMoved = true;
                            pointJ.setCoordsVector(ptsPointJ);
                            clusterI.addPoint(pointJ);
                            toRemoveFromJ.add(pointJ);
                            if (logger!=null)
                            {
                                logger.log(Level.FINEST,"Moving one fragment "
                                        + "from cluster " + j + " to "
                                        + "cluster " + i + " (RMSD " 
                                        + String.format("%.4f", rmsd) + ">="
                                        + String.format("%.4f", rmsdThreshold) + ").");
                            }
                        }
                    }
                    clusterJ.removeAll(toRemoveFromJ);
                    if (clusterJ.getPoints().size()==0)
                        toRemoveClusters.add(clusterJ);
                }
            }
        }
        
        clusters.removeAll(toRemoveClusters);
        
        return somethingMoved;
    }
    
//------------------------------------------------------------------------------
   
    /**
     * Computes statistics for a unimodal, normally noise-distorted population
     * of points generated by distorting a given N-dimensional vector.
     * This is done by producing a dataset of N-dimensional points by adding
     * normally distributed 
     * noise on the given N-dimensional center. 
     * Then, this method computes the new centroid 
     * of the dataset and produces the statistics of the RMDS upon superposition
     * of new centroid to the centroid.
     * @param center the N-dimensional point around which noise is added.
     * @param sampleSize the size of the distribution of N-dimensional points 
     * that we generate around the center.
     * @param maxNoise absolute value of the maximum noise. Noise is generated
     * with a Normal distribution centered at 0.0 and going from -maxNoise to
     * +maxNoise.
     * @return the statistics of the RMSD for the normally
     * distributed noise-distorted population.
     */
    protected static SummaryStatistics getRMSDStatsOfNoisyDistorsions(
            double[] center, int sampleSize, double maxNoise)
    {
        // We use always the same randomizer to get reproducible values 
        // no matter what randomizer is the caller using.
        Randomizer rng = new Randomizer(1L);
        
        DistanceAsRMSD measure = new DistanceAsRMSD();
        
        SummaryStatistics stats = new SummaryStatistics();
        List<double[]> refClusterCoords = new ArrayList<double[]>();
        for (int k=0; k<sampleSize; k++)
        {
            double[] coords = Arrays.copyOf(center, center.length);
            for (int j=0; j<coords.length; j++)
            {
                coords[j] = coords[j] + maxNoise*(2*rng.nextNormalDouble()-1);
            }
            refClusterCoords.add(coords);
        }
        double[] refCentroidCoords = MathUtils.centroidOf(refClusterCoords, 
                center.length);
        
        for (double[] coords : refClusterCoords) 
        {
            double rmsd = measure.compute(coords,refCentroidCoords);
            stats.addValue(rmsd);
        }
        return stats;
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
            double rmsd = svd.getRmsd(ptsA,ptsB);
            return rmsd;
        }
        
        public double compute(Point3d[] ptsA, Point3d[] ptsB)
                throws DimensionMismatchException
        {
            SuperPositionSVD svd = new SuperPositionSVD(false);
            double rmsd = svd.getRmsd(ptsA,ptsB);
            return rmsd;
        }
    }
  
//------------------------------------------------------------------------------

    /**
     * Once the clustering is done, this method return the list of resulting 
     * clusters.
     * @return the clusters.
     */
    public List<DynamicCentroidCluster> getClusters()
    {
        return clusters; 
    }

//------------------------------------------------------------------------------

    /**
     * Once the clustering is done, this method return the list of clusters.
     * Each cluster contains objects that are transformed to best align with the
     * centroid of the cluster.
     * @return the list of clusters, or an empty list if {@link #clusters} has
     * not been called.
     */
    public List<List<Fragment>> getTransformedClusters()
    {
        List<List<Fragment>> transformedClusters = new ArrayList<List<Fragment>>();
        for (int i=0; i<clusters.size(); i++)
        {   
            List<Fragment> transformedCluster = new ArrayList<Fragment>();
            DynamicCentroidCluster cluster = clusters.get(i);
            for (ClusterableFragment cf : cluster.getPoints())
            {
                transformedCluster.add(cf.getTransformedCopy());
            }
            transformedClusters.add(transformedCluster);
        }
        return transformedClusters; 
    }
    
//------------------------------------------------------------------------------

    /**
     * Once the clustering is done, this method return the list of cluster 
     * centroids. Note the centroids are not part of the initial data. 
     * Use {@link #getNearestToClusterCentroids()} to get the actual fragments
     * from the initial dataset and that are closest to their respective
     * cluster centroid.
     * @return the cluster centroids, or an empty list if {@link #clusters} has
     * not been called.
     */
    public List<Fragment> getClusterCentroids()
    {
        List<Fragment> centroids = new ArrayList<Fragment>();
        for (int i=0; i<clusters.size(); i++)
        {   
            DynamicCentroidCluster cluster = clusters.get(i);
            ClusterableFragment centroid = cluster.getCentroid();
            Fragment frag = centroid.getTransformedCopy();
            centroids.add(frag);
        }
        return centroids; 
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Once the clustering is done, this method return the list of fragments 
     * that are nearest to the respective cluster centroid. 
     * Note the centroids are not part of the initial data, but the nearest to
     * the centroid is.
     * @return the fragment that is closest to each cluster centroid,
     *  or an empty list if {@link #clusters} has
     * not been called.
     */
    public List<Fragment> getNearestToClusterCentroids()
    {
        List<Fragment> nearestToCentroid = new ArrayList<Fragment>();
        for (int i=0; i<clusters.size(); i++)
        {   
            DynamicCentroidCluster cluster = clusters.get(i);
            ClusterableFragment nearest = cluster.getNearestToCentroid(
                    new DistanceAsRMSD());
            Fragment frag = nearest.getTransformedCopy();
            nearestToCentroid.add(frag);
        }
        return nearestToCentroid; 
    }

//------------------------------------------------------------------------------

}
