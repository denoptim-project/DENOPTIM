package denoptim.fragmenter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.ml.clustering.Clusterable;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.openscience.cdk.interfaces.IAtom;

import denoptim.graph.AttachmentPoint;
import denoptim.graph.FragIsomorphEdge;
import denoptim.graph.FragIsomorphNode;
import denoptim.graph.Fragment;

public class ClusterableFragment implements Clusterable
{
    /**
     * Reference to original fragment
     */
    private Fragment frag;
    
    /**
     * Ordered list of nodes determined by isomorphism.
     */
    List<FragIsomorphNode> orderedNodes;
    
    /**
     * Ordered list of coordinated reflecting the ordered list of atoms/APs.
     */
    double[] allCoords;
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor. Does not set the ordering of the atoms/APs. 
     * For that use {@link #setOrderOfNodes(Collection)}.
     * @param frag the fragment to wrap.
     */
    public ClusterableFragment(Fragment frag)
    {
        this.frag = frag;
    }

//------------------------------------------------------------------------------
    
    /**
     * sets the order of nodes (i.e., atoms/APs) to use for geometric comparison
     * with other fragments.
     * @param c the new order.
     */
    public void setOrderOfNodes(Collection<FragIsomorphNode> c)
    {
        orderedNodes = new ArrayList<FragIsomorphNode>(c);
        
        allCoords = new double[orderedNodes.size()*3];
        int i=-1;
        for (FragIsomorphNode node : orderedNodes)
        {
            i++;
            allCoords[i] = node.getPoint3d().x;
            i++;
            allCoords[i] = node.getPoint3d().y;
            i++;
            allCoords[i] = node.getPoint3d().z;
        }
    }
    
//------------------------------------------------------------------------------
    
    protected Fragment getOriginalFragment()
    {
        return frag;
    }

//------------------------------------------------------------------------------
    
    protected DefaultUndirectedGraph<FragIsomorphNode, FragIsomorphEdge> getJGraphFragIsomorphism()
    {
        return frag.getJGraphFragIsomorphism();
    }

//------------------------------------------------------------------------------
    
    /**
     * @return the vector of 3*N coordinates, if the ordering has been set, 
     * or null.
     */
    @Override
    public double[] getPoint()
    {       
        return allCoords;
    }

//------------------------------------------------------------------------------
    
}
