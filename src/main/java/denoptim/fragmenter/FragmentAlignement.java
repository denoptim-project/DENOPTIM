package denoptim.fragmenter;

import java.util.Iterator;

import javax.vecmath.Point3d;

import org.biojava.nbio.structure.geometry.SuperPositionSVD;
import org.jgrapht.GraphMapping;

import denoptim.exception.DENOPTIMException;
import denoptim.graph.FragIsomorphEdge;
import denoptim.graph.FragIsomorphNode;
import denoptim.graph.Fragment;
import denoptim.graph.FragmentIsomorphismInspector;

public class FragmentAlignement
{
    /**
     * Lowest RMSD upon alignment
     */
    private double minRMSD = Double.MAX_VALUE;
    
    /**
     * Mapping of nodes leading to lowest RMSD upon alignment.
     */
    private GraphMapping<FragIsomorphNode, FragIsomorphEdge> bestMapping = null;
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor for an alignment of two fragments. 
     * Note we first have to find an ordering of the atoms/AP that is consistent.
     * Thus we check for isomorphism between the fragments.
     * @param fragA
     * @param fragB
     * @throws DENOPTIMException if an isomorphism is not found.
     */
    public FragmentAlignement(Fragment fragA, Fragment fragB) throws DENOPTIMException 
    {
        // Map graph nodes (atom and APs)
        FragmentIsomorphismInspector fii = 
                new FragmentIsomorphismInspector(fragA, fragB);
        if(!fii.isomorphismExists())
        {
            throw new DENOPTIMException("Failed to find isomorphism.");
        }

        int nPoints = fragA.getJGraphFragIsomorphism().vertexSet().size();
        
        // Get lowest RMSD among all mappings
        Iterator<GraphMapping<FragIsomorphNode, FragIsomorphEdge>> 
            mapingIterator = fii.getMappings();
        while (mapingIterator.hasNext())
        {
            GraphMapping<FragIsomorphNode, FragIsomorphEdge> mapping = 
                    mapingIterator.next();
            
            // Translate graph nodes/vertexes into points to align
            Point3d[] ptsA = new Point3d[nPoints];
            Point3d[] ptsB = new Point3d[nPoints];
            int index = -1;
            for (FragIsomorphNode nA : fragA.getJGraphFragIsomorphism().vertexSet())
            {
                index++;
                FragIsomorphNode nB = mapping.getVertexCorrespondence(nA,
                        true);
                ptsA[index] = nA.getPoint3d();
                ptsB[index] = nB.getPoint3d();
            }
            
            // Align atoms
            SuperPositionSVD svd = new SuperPositionSVD(false);
            double rmsd = svd.getRmsd(ptsA,ptsB);
            if (rmsd < minRMSD)
            {
                minRMSD = rmsd;
                bestMapping = mapping;
            }
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the mapping leading to the lowest RMSD that could be found among 
     * all isomorphic mappings.
     * @return
     */
    public GraphMapping<FragIsomorphNode, FragIsomorphEdge> getLowestRMSDMapping()
    {
        return bestMapping;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the lowest RMSD that could be found among all isomorphic mappings.
     * @return
     */
    public double getMinimumRMSD()
    {
        return minRMSD;
    }
    
//------------------------------------------------------------------------------

}
