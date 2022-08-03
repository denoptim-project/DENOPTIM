package denoptim.graph;

import java.util.Comparator;
import java.util.Iterator;

import org.jgrapht.GraphMapping;
import org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector;

/**
 * <p>Inspector for isomorphism on fragments. The graph representation considered
 * is one where both atoms and {@link AttachmentPoint}s are represented as 
 * graph nodes, and edges are either bonds or atom-to-attachment point 
 * connections.</p>
 * <p>Effectively, this class considers the composition and connectivity 
 * (including that of {@link AttachmentPoint}s when comparing the fragments.
 * Geometry and stereochemistry are not considered.</p>
 * <p>This call works with the {@link VF2GraphIsomorphismInspector}, see the
 * dedicated documentation for information of patologial cases.</p>
 * 
 * @author Marco Foscato
 */
public class FragmentIsomorphismInspector
{
    /**
     * Implementation of the Vento-Foggia 2 algorithm.
     */
    VF2GraphIsomorphismInspector<FragIsomorphNode, FragIsomorphEdge> vf2;

//------------------------------------------------------------------------------
    
    public FragmentIsomorphismInspector(Fragment fragA, Fragment fragB)
    {
        Comparator<FragIsomorphNode> vComp = new Comparator<FragIsomorphNode>() 
        {
            @Override
            public int compare(FragIsomorphNode n1, FragIsomorphNode n2)
            {
                return n1.label.compareTo(n2.label);
            }
        };
        
        Comparator<FragIsomorphEdge> eComp = new Comparator<FragIsomorphEdge>() 
        {
            @Override
            public int compare(FragIsomorphEdge e1, FragIsomorphEdge e2)
            {
                return e1.label.compareTo(e2.label);
            }
        };
        
        vf2 = new VF2GraphIsomorphismInspector<>(
                        fragA.getJGraphFragIsomorphism(), 
                        fragB.getJGraphFragIsomorphism(), vComp, eComp);
    }
    
//------------------------------------------------------------------------------
    
    public boolean isomorphismExists()
    {
        return vf2.isomorphismExists();
    }
    
//------------------------------------------------------------------------------
    
    public Iterator<GraphMapping<FragIsomorphNode, FragIsomorphEdge>> getMappings()
    {
        return vf2.getMappings();
    }
    
//------------------------------------------------------------------------------    
    
}
