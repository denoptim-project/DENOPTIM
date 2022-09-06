package denoptim.fragmenter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.ml.distance.DistanceMeasure;

import denoptim.utils.MathUtils;

/**
 * A cluster with a centroid that can be updated after definition of the cluster.
 * The centroid is updated every time we request to get the centroid and there
 * has been an operation that changed the members (i.e., a points in the cluster)
 * of this cluster (i.e., addition or removal of members).
 * 
 * @author Marco Foscato
 */
public class DynamicCentroidCluster 
{
    /** 
     * The points contained in this cluster.
     */
    private List<ClusterableFragment> points;
    
    /**
     * Current centroid.
     */
    private ClusterableFragment centroid;
    
    /**
     * Flag requesting the update of the centroid. Turns <code>true</code> when
     * we modify the list of members of this cluster.
     */
    private boolean updateCentroid = false;

//------------------------------------------------------------------------------
    
    /**
     * Constructor that defines an empty cluster. Centroid will be defined in
     * the first call of {@link #addPoint(ClusterableFragment)} as a clone of 
     * the added fragment.
     */
    public DynamicCentroidCluster()
    {
        this.points = new ArrayList<ClusterableFragment>();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor that defines the current centroid to be a clone of the given
     * fragment and adds the given fragment as a member of the cluster.
     * @param centroid the current centroid.
     */
    public DynamicCentroidCluster(ClusterableFragment centroid)
    {
        this.points = new ArrayList<ClusterableFragment>();
        this.centroid = centroid.clone();
        this.points.add(centroid);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Get the point chosen to be the centroid of this cluster.
     * @return chosen cluster centroid
     */
    public ClusterableFragment getCentroid() 
    {
        if (updateCentroid)
        {
            List<double[]> membersCoords = new ArrayList<double[]>();
            for (ClusterableFragment cf : getPoints())
            {
                membersCoords.add(cf.getPoint());
            }
            double[] refCentroidCoords = MathUtils.centroidOf(membersCoords, 
                    centroid.getPoint().length);
            centroid.setCoordsVector(refCentroidCoords);
            
            updateCentroid = false;
        }
        return centroid;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Gets the original data that upon clustering is closest to the cluster 
     * centroid according to the given distance metrics.
     * @param measure the distance metrics to use to determine which data is 
     * closest to the centroid.
     * @return the cluster member that is closest to the cluster centroid.
     */
    public ClusterableFragment getNearestToCentroid(DistanceMeasure measure)
    {
        if (points.size() == 1)
            return points.get(0);
        
        ClusterableFragment centroid = getCentroid();
        
        ClusterableFragment nearest = null;
        double smallestDistance = Double.MAX_VALUE;
        for (ClusterableFragment cf : points)
        {
            double distance = measure.compute(centroid.getPoint(),cf.getPoint());
            if (distance < smallestDistance)
            {
                nearest = cf;
                smallestDistance = distance;
            }       
        }
        return nearest;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Add a new member of this cluster.
     * @param point the member to add.
     */
    public void addPoint(ClusterableFragment point)
    {
        if (centroid == null)
        {
            // First time ever we add a point
            centroid = point.clone();
        } else {
            updateCentroid = true;
        }
        points.add(point);
    }

//------------------------------------------------------------------------------
    
    /**
     * Remove the given cluster member, if it is a member of this cluster.
     * @param points the member to remove.
     */
    public void removeAll(Collection<ClusterableFragment> points)
    {
        updateCentroid = true;
        points.removeAll(points);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Get the points contained in the cluster.
     * @return points contained in the cluster
     */
    public List<ClusterableFragment> getPoints() 
    {
        return points;
    }

//------------------------------------------------------------------------------
    
}
