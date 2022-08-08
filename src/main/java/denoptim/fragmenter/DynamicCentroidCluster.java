package denoptim.fragmenter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.Clusterable;

import denoptim.utils.MathUtils;

/**
 * A cluster with a centroid that can be updated after definition of the cluster.
 * The centroid is updated every time we request the get the centroid and there
 * has been an operation that changed the members (i.e., a points in the cluster)
 * of this cluster (i.e., 
 * addition or removal of members).
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
     * Constructor that defines the current centroid.
     * @param centroid the current centroid.
     */
    public DynamicCentroidCluster(ClusterableFragment centroid)
    {
        this.points = new ArrayList<ClusterableFragment>();
        this.centroid = centroid;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Get the point chosen to be the centroid of this cluster.
     * @return chosen cluster centroid
     */
    public ClusterableFragment getCentroid() {
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
     * Add a new member of this cluster.
     * @param point the member to add.
     */
    public void addPoint(ClusterableFragment point)
    {
        updateCentroid = true;
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
