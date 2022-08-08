package denoptim.fragmenter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.clustering.MultiKMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.clustering.evaluation.SumOfClusterVariances;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.biojava.nbio.structure.geometry.CalcPoint;
import org.biojava.nbio.structure.geometry.SuperPositionSVD;
import org.openscience.cdk.Atom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.DGraph.StringFormat;
import denoptim.graph.FragIsomorphNode;
import denoptim.io.DenoptimIO;
import denoptim.graph.Fragment;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.utils.MathUtils;
import denoptim.utils.Randomizer;

/**
 * TODO-gg
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
     * Currently defined list of clusters. Initially empty, then contains one
     * cluster per each data point, and the is pruned to retain only significant
     * clusters.
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
     * TODO-gg
     * Constructor for a clusterer of fragments. 
     * Note we first have to find an ordering of the atoms/AP that is consistent
     * throughout the dataset. We expect this ordering to be done prior to 
     * attempting the clustering.
     * @param data collection of fragments to clusterize. The coordinates vector
     * of each of these is expected to have a consistent ordering, but the 
     * value of the coordinates will be edited to align the geometries.
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
     * TODO-gg
     */
    public void cluster()
    {
        // Start by assigning each data to its own cluster
        for (int i=0; i<data.size(); i++)
        {
            ClusterableFragment centroid = data.get(i).clone();
            DynamicCentroidCluster cluster = new DynamicCentroidCluster(centroid);
            cluster.addPoint(data.get(i));
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
            
            ClusterableFragment centroidI = (ClusterableFragment) clusterI.getCentroid();
            
            //TODO-gg consider using RMSD of bond lengths and angles?
            
            // Define a distance (RMSD upon superposition) for discriminating
            // this geometry from the others.
            SummaryStatistics refRMSDStats = getRMSDStatsOfNoisyDistorsions(
                    centroidI.getPoint(), 20, 0.2); //TODO-gg tuneable params
            double rmsdThreshold = refRMSDStats.getMean() 
                    + 1.0*refRMSDStats.getStandardDeviation(); //TODO-gg tuneable params
            
            for (int j=i+1; j<clusters.size(); j++)
            {
                DynamicCentroidCluster clusterJ = 
                        clusters.get(j);
                
                if (toRemoveClusters.contains(clusterJ))
                    continue;
                
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
     * Produces an sample of N-dimensional points by adding normally distributed 
     * noise on the given N-dimensional center. Then, computes the new centroid 
     * of the dataset and produces the statistics of the RMDS between each point
     * and the new centroid.
     * @param center the N-dimensional point around which noise is added.
     * @param sampleSize the size of the distribution of N-dimensional points 
     * that we generate aroung the center.
     * @param maxNoise absolute value of the maximum noise. Noise is generated
     * with a Normal distribution centered at 0.0 and going from -maxNoise to
     * +maxNoise.
     * @return the statistics of the RMSD of the distribution.
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
     * @return the clusters.
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
     * Moreover, the fragments's coordinates a
     * @return the cluster centroids.
     */
    public List<Fragment> getClusterCentroids()
    {
        List<Fragment> centroids = new ArrayList<Fragment>();
        for (int i=0; i<clusters.size(); i++)
        {   
            DynamicCentroidCluster cluster = clusters.get(i);
            ClusterableFragment centroid = (ClusterableFragment) cluster.getCentroid();

            Fragment frag = centroid.getTransformedCopy();
            centroids.add(frag);
        }
        return centroids; 
    }

//------------------------------------------------------------------------------

}
