package denoptimga;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.xml.internal.txw2.IllegalSignatureException;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.molecule.APMapping;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.DENOPTIMVertex.BBType;
import denoptim.utils.RandomUtils;

/**
 * An utility class to encapsulate the search for vertexes that can replace 
 * an original vertex while retaining the graph structure of the original 
 * vertex. 
 * 
 * @author Marco Foscato
 */

class GraphLinkFinder
{
    /**
     * The result of the search for a compatible building block, or null if
     * no compatible link was found. In case of multiple possibilities, the
     * result reported here is chosen randomly among the possible ones.
     */
    protected DENOPTIMVertex chosenNewLink;
    
    /**
     * The mapping of attachment points between the original vertex and the
     * chosen link. This mapping is reported and integer indexes, where each int
     * is the result of {@link DENOPTIMVertex#getIndexInOwner()}. 
     * In case of multiple possible AP mappings, the
     * result reported here is chosen randomly among the possible ones.
     */
    protected Map<Integer,Integer> chosenAPMap;
    
    /**
     * The collection of all alternative vertex that can replace the 
     * original vertex, with the consistent mappings.
     */
    protected Map<DENOPTIMVertex,List<Map<Integer,Integer>>> allCompatLinks = 
            new HashMap<DENOPTIMVertex,List<Map<Integer,Integer>>>();
    
    /**
     * Maximum number of combinations. This prevents combinatorial explosion, 
     * but it is ignored is the constructor is required to screen all.
     * Remember that the combinations are anyway randomised, so even with the 
     * maximum limit on the number of combination to consider, there is no
     * systematic exclusion of specific combinations.
     */
    private static int maxCombs = 50;
    
    /**
     * Flag recording if at least one alternative vertex was found.
     */
    protected boolean foundNewLink = false;
    
//------------------------------------------------------------------------------

    /**
     * Constructor that specifies which vertex has to be replaced. The search 
     * for an alternative building block takes place within this constructor.
     * @param originalLink the vertex to replace.
     */
    public GraphLinkFinder(DENOPTIMVertex originalLink)
    {  
        this(originalLink,false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor that specifies which vertex has to be replaced. The search 
     * for an alternative building block takes place within this constructor.
     * @param originalLink the vertex to replace.
     * @param screenAll use <code>true</code> to NOT stop at the first 
     * compatible
     * vertex, but screen all the library of building blocks.
     */
    public GraphLinkFinder(DENOPTIMVertex originalLink, boolean screenAll) 
    {   
        BBType bbt = originalLink.getBuildingBlockType();
        ArrayList<DENOPTIMVertex> candidates = new ArrayList<DENOPTIMVertex>();
        switch (bbt)
        {
            //TODO: limit search to building blocks with a minimal number of APs
            case FRAGMENT:
                candidates.addAll(FragmentSpace.getFragmentLibrary());
                break;
            case SCAFFOLD:
                candidates.addAll(FragmentSpace.getScaffoldLibrary());
            case CAP:
                break;
            default:
                break;
        }

        int candidatesOrigSize = candidates.size();
        for (int i=0; i<candidatesOrigSize; i++)
        {
            if (foundNewLink && !screenAll)
                break;
            
            DENOPTIMVertex originalBB = RandomUtils.randomlyChooseOne(
                    candidates);
            candidates.remove(originalBB);
            try
            {
                chosenNewLink = FragmentSpace.getVertexFromLibrary(
                        originalBB.getBuildingBlockType(), 
                        originalBB.getBuildingBlockId());
            } catch (DENOPTIMException e)
            {
                e.printStackTrace();
                continue;
            }
            
          //TODO-GG del
            //System.out.println("Considering " + chosenNewLink.getBuildingBlockId());
            /*
            for (DENOPTIMAttachmentPoint oAP : originalLink.getAttachmentPoints())
            {
                System.out.println("   O: "+oAP.getAPClass());
            }

            for (DENOPTIMAttachmentPoint cAP : newLink.getAttachmentPoints())
            {
                System.out.println("   N: "+cAP.getAPClass());
            }
            */
            
            // NB: the new link cannot be the same building block as the old one
            if (chosenNewLink.getBuildingBlockId() == 
                    originalLink.getBuildingBlockId())
            {
                continue;
            }
            
            if (chosenNewLink.getNumberOfAPs() < originalLink.getNumberOfAPs())
            {
                continue;
            }
            
            // We map all the compatibilities before choosing a specific mapping
            Map<DENOPTIMAttachmentPoint,List<DENOPTIMAttachmentPoint>> apCompatilities =
                    new HashMap<DENOPTIMAttachmentPoint,List<DENOPTIMAttachmentPoint>>();
            for (DENOPTIMAttachmentPoint oAP : originalLink.getAttachmentPoints())
            {
                for (DENOPTIMAttachmentPoint cAP : chosenNewLink.getAttachmentPoints())
                {  
                  //TODO-GG del
                    //System.out.println("   Comparing "+oAP.getAPClass()+"("+oAP.getID()+") "+cAP.getAPClass()+"("+cAP.getID()+")");
                    
                    boolean compatible = false;
                    if (FragmentSpace.useAPclassBasedApproach())
                    {
                        // TODO-GG: same APClass or an APClass compatible with 
                        // the APClass on the child/parent AP?
                        // TODO-GG: Also, if the vertex is a template, we should
                        // consider the required APs.
                        if (oAP.getAPClass().equals(cAP.getAPClass()))
                            compatible = true;
                    } else {
                        if (oAP.getTotalConnections() == cAP.getTotalConnections())
                            compatible = true;
                    }
                    if (compatible)
                    {
                      //TODO-GG del
                      //System.out.println("       COMPATIBLE");
                        if (apCompatilities.containsKey(oAP))
                        {
                            apCompatilities.get(oAP).add(cAP);
                        } else {
                            List<DENOPTIMAttachmentPoint> lst = 
                                    new ArrayList<DENOPTIMAttachmentPoint>();
                            lst.add(null);
                            lst.add(cAP);
                            apCompatilities.put(oAP,lst);
                        }
                    }
                }
            }
            // keys is used just to keep the map keys sorted in a separate list
            // so that the order is randomized only once, then it is retained.
            List<DENOPTIMAttachmentPoint> keys = 
                    new ArrayList<DENOPTIMAttachmentPoint>(
                            apCompatilities.keySet());
            if (keys.size() < originalLink.getNumberOfAPs())
            {
                continue;
            }
            
            // Get all possible combinations of compatible AP pairs
            // (Includes "empty", i.e., ignoring one pair of compatible APs) 
            List<APMapping> apMappings = new ArrayList<APMapping>();
            int currentKey = 0;
            APMapping currentMapping = new APMapping();
            recursiveCombiner(keys, currentKey, apCompatilities, currentMapping, 
                    apMappings, screenAll, false);
            
            // Keep only complete mappings (i.e., include all APs of the 
            // original vertex)
            List<APMapping> toRemove = new ArrayList<APMapping>();
            for (APMapping c : apMappings)
            {
                if (!c.containsAllKeys(originalLink.getAttachmentPoints()))
                {
                    toRemove.add(c);
                }
            }
            apMappings.removeAll(toRemove);
            
            //TODO-GG del
            //System.out.println("completeCombinations: "+apMappings);
            
            if (apMappings.isEmpty())
            {
                this.chosenNewLink = null;
                continue;
            } else {
                this.foundNewLink = true;
            }
            
            APMapping chosen = RandomUtils.randomlyChooseOne(apMappings);
            try
            {
                this.chosenAPMap = chosen.toIntMappig();
            } catch (DENOPTIMException e)
            {
                throw new IllegalSignatureException(e);
            }
            if (screenAll)
            {
                List<Map<Integer,Integer>> lst = new ArrayList<Map<Integer,Integer>>();
                for (APMapping apm : apMappings)
                {
                    try
                    {
                        lst.add(apm.toIntMappig());
                    } catch (DENOPTIMException e)
                    {
                        throw new IllegalSignatureException(e);
                    }
                }
                allCompatLinks.put(chosenNewLink, lst);
            }
        }
    }
    
//------------------------------------------------------------------------------
    
    private static void recursiveCombiner(List<DENOPTIMAttachmentPoint> keys,
            int currentKey, Map<DENOPTIMAttachmentPoint,
                List<DENOPTIMAttachmentPoint>> possibilities,
            APMapping combination, List<APMapping> completeCombinations, 
            boolean screenAll, boolean stop)
    {
        DENOPTIMAttachmentPoint apA = keys.get(currentKey);
        for (int i=0; i<possibilities.get(apA).size(); i++)
        {
            // Prevent combinatorial explosion.
            if (stop)
                break;
            
            DENOPTIMAttachmentPoint apB = possibilities.get(apA).get(i);
            
            // Move on if apB is already used by another pairing
            if (combination.containsValue(apB))
                continue;

            // add this pairing to the growing combinations
            APMapping priorCombination = combination.clone();
            if (apA != null && apB != null)
            {
                combination.put(apA,apB);
            }
            
            // go deeper, to the next key
            if (currentKey+1 < keys.size())
            {
                recursiveCombiner(keys, currentKey+1, possibilities, 
                        combination, completeCombinations, screenAll, stop);
            }
            
            // we reached the deepest level: save combination
            if (currentKey+1 == keys.size() && !combination.isEmpty())
            {
                APMapping storable = combination.clone(); //Shallow clone
                completeCombinations.add(storable);
                if (!screenAll && completeCombinations.size() >= maxCombs)
                {
                    stop = true;
                }
            }
            
            // Restart building a new combination from the previous combination
            combination = priorCombination;
        }
    }
    
//------------------------------------------------------------------------------

}