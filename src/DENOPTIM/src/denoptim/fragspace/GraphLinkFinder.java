package denoptim.fragspace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import denoptim.exception.DENOPTIMException;
import denoptim.graph.APClass;
import denoptim.graph.APMapping;
import denoptim.graph.DENOPTIMAttachmentPoint;
import denoptim.graph.DENOPTIMEdge;
import denoptim.graph.DENOPTIMVertex;
import denoptim.graph.DENOPTIMVertex.BBType;
import denoptim.utils.RandomUtils;

/**
 * An utility class to encapsulate the search for vertexes that satisfy 
 * constraints. Typically, this class can be used to find building blocks
 * that they replace other, non identical, building blocks in a graph
 * while retaining the graph structure, i.e., the new building blocks offers 
 * at least enough APs (and APs that are compatible with the surrounding
 * graph branches) to replace the original building block.
 * Another example of use case, is the identification of a building block that
 * can be inserted in between two connected vertexes of a graph.
 * 
 * @author Marco Foscato
 */

public class GraphLinkFinder
{
    /**
     * The result of the search for a compatible building block, or null if
     * no compatible link was found. In case of multiple possibilities, the
     * result reported here is chosen randomly among the possible ones.
     */
    private DENOPTIMVertex chosenNewLink;
    
    /**
     * The mapping of attachment points between the original vertex and the
     * chosen link. This mapping is reported and integer indexes, where each int
     * is the result of {@link DENOPTIMVertex#getIndexInOwner()}. 
     * In case of multiple possible AP mappings, the
     * result reported here is randomly chosen among the possible ones. This
     * field is null in case the constructor was asked to work on an
     * edge rather than a vertex.
     */
    //TODO: consider getting rid of this and use only chosenAPMap
    private Map<Integer,Integer> chosenAPMapInt;
    
    /**
     * The collection of all alternative vertex that can replace the 
     * original vertex, with the consistent mappings. 
     * This
     * field is null in case the constructor was asked to work on an
     * edge rather than a vertex.
     */
    private Map<DENOPTIMVertex,List<Map<Integer,Integer>>> allCompatLinksInt = 
            new HashMap<DENOPTIMVertex,List<Map<Integer,Integer>>>();
    
    /**
     * The mapping of attachment points between the original vertex/es and the
     * chosen link. 
     * In case of multiple possible AP mappings, the
     * result reported here is randomly chosen among the possible ones. 
     */
    private APMapping chosenAPMap;
    
    /**
     * The collection of all alternative vertex that can replace the 
     * original vertex, with the consistent mappings. 
     * This
     * field is null in case the constructor was asked to work on an
     * edge rather than a vertex.
     */
    private Map<DENOPTIMVertex,List<APMapping>> allCompatLinks = 
            new HashMap<DENOPTIMVertex,List<APMapping>>();
    
    /**
     * Maximum number of combinations. This prevents combinatorial explosion, 
     * but it is ignored if the constructor is required to screen all.
     * Remember that the combinations are anyway randomised, so even with the 
     * maximum limit on the number of combination to consider, there is no
     * systematic exclusion of specific combinations.
     */
    private static int maxCombs = 250;
    
    /**
     * Flag recording if at least one alternative vertex was found.
     */
    private boolean foundNewLink = false;
    
//------------------------------------------------------------------------------

    /**
     * Constructor that specifies which vertex has to be replaced. The search 
     * for an alternative building block takes place within this constructor.
     * @param originalLink the vertex to replace.
     * @throws DENOPTIMException if the required new building block ID cannot
     * be used.
     */
    public GraphLinkFinder(DENOPTIMVertex originalLink) throws DENOPTIMException
    {  
        this(originalLink,-1,false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor that specifies which vertex has to be replaced and which
     * building block ID to use as replacement.
     * @param originalLink the vertex to replace.
     * @param newBuildingBlockID the index specifying which building block to 
     * use as replacement. This can be -1, in which case, this method will
     * search suitable building block candidates and choose randomly.
     * @throws DENOPTIMException if the required new building block ID cannot
     * be used.
     */
    public GraphLinkFinder(DENOPTIMVertex originalLink, int newBuildingBlockID) 
            throws DENOPTIMException
    {  
        this(originalLink,newBuildingBlockID,false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor that specifies which vertex has to be replaced. The search 
     * for an alternative building block takes place within this constructor.
     * @param originalLink the vertex to replace.
     * @param newBuildingBlockID the index specifying which building block to 
     * use as replacement. This can be -1, in which case, this method will
     * search suitable building block candidates and choose randomly.
     * @param screenAll use <code>true</code> to NOT stop at the first 
     * compatible
     * vertex, but screen all the library of building blocks.
     * @throws DENOPTIMException if the required new building block ID cannot
     * be used.
     */
    public GraphLinkFinder(DENOPTIMVertex originalLink, int newBuildingBlockID,
            boolean screenAll) throws DENOPTIMException 
    {   
        ArrayList<DENOPTIMVertex> candidates = getCandidateBBs(
                originalLink.getBuildingBlockType(), newBuildingBlockID);
        
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
            
            // NB: the new link cannot be the same building block as the old one
            if (chosenNewLink.getBuildingBlockId() == 
                    originalLink.getBuildingBlockId())
            {
                continue;
            }
            
            if (chosenNewLink.getNumberOfAPs() < (originalLink.getNumberOfAPs() -
                    originalLink.getFreeAPCountThroughout()))
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
                    boolean compatible = false;
                    if (FragmentSpace.useAPclassBasedApproach())
                    {
                        // TODO: if the vertex is a template, we should
                        // consider the required APs.
                        
                        if (oAP.getAPClass().equals(cAP.getAPClass()))
                            compatible = true;
                        
                        if (oAP.isAvailableThroughout())
                        {
                            //NB:template's requires AP will have to be considered here as well
                            compatible = true;
                        } else {
                            DENOPTIMAttachmentPoint lAP = 
                                    oAP.getLinkedAPThroughout();
                            if (oAP.isSrcInUserThroughout())
                            {
                                if (lAP!=null && cAP.getAPClass()
                                      .isCPMapCompatibleWith(lAP.getAPClass()))
                                {
                                    compatible = true;
                                }
                            } else {
                                if (lAP!=null && lAP.getAPClass()
                                      .isCPMapCompatibleWith(cAP.getAPClass()))
                                {
                                    compatible = true;
                                }
                            }
                        }
                    } else {
                        compatible = true;
                    }
                    if (compatible)
                    {
                        if (apCompatilities.containsKey(oAP))
                        {
                            apCompatilities.get(oAP).add(cAP);
                        } else {
                            List<DENOPTIMAttachmentPoint> lst = 
                                    new ArrayList<DENOPTIMAttachmentPoint>();
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
            
            // Identify APs in old link that are used: we want a mapping that
            // includes all of those. This, to be able to change the link without
            // removing any existing branch.
            ArrayList<DENOPTIMAttachmentPoint> oldAPsRequiredToHaveAMapping = new
                    ArrayList<DENOPTIMAttachmentPoint>();
            for (DENOPTIMAttachmentPoint oldAp : originalLink.getAttachmentPoints())
            {
                if (oldAp.isAvailableThroughout())
                {
                    apCompatilities.get(oldAp).add(null);
                } else {
                    oldAPsRequiredToHaveAMapping.add(oldAp);
                    if (!keys.contains(oldAp))
                    {
                        // In this case we have no hope of finding a mapping 
                        // that satisfies out needs.
                        continue;
                    }
                }
            }
            
            // Get all possible combinations of compatible AP pairs
            List<APMapping> apMappings = new ArrayList<APMapping>();
            int currentKey = 0;
            APMapping currentMapping = new APMapping();
            // We try the comprehensive approach, but if that is too demanding
            // and gets stopped, then we run a a series of simplified attempts
            // to hit a decent combination with "lucky shots".
            Boolean stopped = FragmentSpaceUtils.recursiveCombiner(keys, 
                    currentKey, apCompatilities, currentMapping, apMappings, 
                    screenAll, maxCombs);
            
            // Keep only mappings that allow retaining the (i.e., include all APs of the 
            // original vertex)
            List<APMapping> toRemove = new ArrayList<APMapping>();
            for (APMapping c : apMappings)
            {
                if (!c.containsAllKeys(oldAPsRequiredToHaveAMapping))
                {
                    toRemove.add(c);
                }
            }
            apMappings.removeAll(toRemove);
            
            // If we where interrupted, we try to get a proper mapping again.
            // This time we ignore all the possibilities and try a more direct 
            // approach, which, however, cannot account for all possible combs.
            if (stopped && apMappings.isEmpty())
            {
                for (int iTry = 0; iTry < maxCombs; iTry++)
                {
                    APMapping apMap = new APMapping();
                    List<DENOPTIMAttachmentPoint> used = 
                            new ArrayList<DENOPTIMAttachmentPoint>();
                    List<DENOPTIMAttachmentPoint> availKeys = 
                            new ArrayList<DENOPTIMAttachmentPoint>();
                    availKeys.addAll(oldAPsRequiredToHaveAMapping);
                    boolean abandon = false;
                    for (int jj=0; jj<oldAPsRequiredToHaveAMapping.size(); jj++)
                    {
                        DENOPTIMAttachmentPoint ap =
                                RandomUtils.randomlyChooseOne(availKeys);
                        availKeys.remove(ap);
                        List<DENOPTIMAttachmentPoint> availPartners = 
                                new ArrayList<DENOPTIMAttachmentPoint>();
                        availPartners.addAll(apCompatilities.get(ap));
                        boolean done = false;
                        for (int j=0; j<apCompatilities.get(ap).size(); j++)
                        {
                            DENOPTIMAttachmentPoint chosenAvail = 
                                    RandomUtils.randomlyChooseOne(availPartners);
                            availPartners.remove(chosenAvail);
                            if (used.contains(chosenAvail))
                            {
                                continue;
                            }
                            used.add(chosenAvail);
                            apMap.put(ap,chosenAvail);
                            done = true;
                            break;
                        }
                        if (!done)
                        {
                            abandon = true;
                            break;
                        }
                    }
                    
                    if (!abandon)
                    {
                        apMappings.add(apMap);
                        break;
                    }
                }
            }
            
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
                this.chosenAPMapInt = chosen.toIntMappig();
            } catch (DENOPTIMException e)
            {
                throw new IllegalStateException(e);
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
                        throw new IllegalStateException(e);
                    }
                }
                allCompatLinksInt.put(chosenNewLink, lst);
            }
        }
    }
    
//------------------------------------------------------------------------------
    
    private ArrayList<DENOPTIMVertex> getCandidateBBs(BBType bbt, int bbId) 
            throws DENOPTIMException 
    {
        ArrayList<DENOPTIMVertex> candidates = new ArrayList<DENOPTIMVertex>();
        if (bbId<0)
        {
            switch (bbt)
            {
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
        } else {
            DENOPTIMVertex chosenOne =  FragmentSpace.getVertexFromLibrary(
                    bbt, bbId);
            candidates.add(chosenOne);
        }
        return candidates;
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor that specifies which edge has to be replaced with a vertex 
     * and two edges. The search 
     * for an alternative building block takes place within this constructor.
     * @param originalEdge the vertex to replace.
     * @param newBuildingBlockID the index specifying which building block to 
     * use for the linking vertex. This can be -1, in which case, 
     * this method will
     * search suitable building block candidates and choose randomly.
     * @throws DENOPTIMException if the required new building block ID cannot
     * be used.
     */
    public GraphLinkFinder(DENOPTIMEdge originalEdge) 
            throws DENOPTIMException
    {  
        this(originalEdge,-1,false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor that specifies which edge has to be replaced with a vertex 
     * and two edges. The search 
     * for an alternative building block takes place within this constructor.
     * @param originalEdge the vertex to replace.
     * @param newBuildingBlockID the index specifying which building block to 
     * use for the linking vertex. This can be -1, in which case, 
     * this method will
     * search suitable building block candidates and choose randomly.
     * @throws DENOPTIMException if the required new building block ID cannot
     * be used.
     */
    public GraphLinkFinder(DENOPTIMEdge originalEdge, int newBuildingBlockID) 
            throws DENOPTIMException
    {  
        this(originalEdge,newBuildingBlockID,false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor that specifies which edge has to be replaced with a vertex 
     * and two edges. The search 
     * for an alternative building block takes place within this constructor.
     * @param originalEdge the vertex to replace.
     * @param newBuildingBlockID the index specifying which building block to 
     * use for the linking vertex. This can be -1, in which case, 
     * this method will
     * search suitable building block candidates and choose randomly.
     * @param screenAll use <code>true</code> to NOT stop at the first 
     * compatible
     * vertex, but screen all the library of building blocks.
     * @throws DENOPTIMException if the required new building block ID cannot
     * be used.
     */
    public GraphLinkFinder(DENOPTIMEdge originalEdge, int newBuildingBlockID,
            boolean screenAll) throws DENOPTIMException 
    {   
        ArrayList<DENOPTIMVertex> candidates = getCandidateBBs(
                originalEdge.getTrgAP().getOwner().getBuildingBlockType(), 
                newBuildingBlockID);

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
            
            if (chosenNewLink.getNumberOfAPs() < 2)
            {
                continue;
            }
            
            // We map all the compatibilities before choosing a specific mapping
            Map<DENOPTIMAttachmentPoint,List<DENOPTIMAttachmentPoint>> apCompatilities =
                    new HashMap<DENOPTIMAttachmentPoint,List<DENOPTIMAttachmentPoint>>();
            
            List<DENOPTIMAttachmentPoint> needeAPs = new ArrayList<DENOPTIMAttachmentPoint>();
            needeAPs.add(originalEdge.getSrcAP());
            needeAPs.add(originalEdge.getTrgAP());
            for (int j=0; j<2;j++)
            {
                DENOPTIMAttachmentPoint oAP = needeAPs.get(j);
                APClass oriAPC = oAP.getAPClass();
                for (DENOPTIMAttachmentPoint cAP : chosenNewLink.getAttachmentPoints())
                {  
                    boolean compatible = false;
                    if (FragmentSpace.useAPclassBasedApproach())
                    {
                        if (j==0 
                                && oriAPC.isCPMapCompatibleWith(cAP.getAPClass()))
                            compatible = true;
                        else if (j==1 
                                && cAP.getAPClass().isCPMapCompatibleWith(oriAPC))
                            compatible = true;
                    } else {
                        compatible = true;
                    }
                    if (compatible)
                    {
                        if (apCompatilities.containsKey(oAP))
                        {
                            apCompatilities.get(oAP).add(cAP);
                        } else {
                            List<DENOPTIMAttachmentPoint> lst = 
                                    new ArrayList<DENOPTIMAttachmentPoint>();
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
            if (keys.size() < 2)
            {
                continue;
            }
            
            // Get all possible combinations of compatible AP pairs
            List<APMapping> apMappings = new ArrayList<APMapping>();
            int currentKey = 0;
            APMapping currentMapping = new APMapping();
            // yes, a nested loop would do it, but for now I use the same code
            // as for when there is no limit to the number of keys (here, always 2)
            Boolean stopped = FragmentSpaceUtils.recursiveCombiner(keys, 
                    currentKey, apCompatilities, currentMapping, apMappings, 
                    screenAll, maxCombs);
            
            if (apMappings.isEmpty())
            {
                this.chosenNewLink = null;
                continue;
            } else {
                this.foundNewLink = true;
            }
            
            this.chosenAPMap = RandomUtils.randomlyChooseOne(apMappings);
            if (screenAll)
            {
                allCompatLinks.put(chosenNewLink, apMappings);
            }
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the vertex chosen as alternative. If more than one possibilities
     * exist, then this will return a randomly chosen one. 
     * Consistency between the return value of this method and 
     * {@link GraphLinkFinder#getChosenAPMappingInt()} is guaranteed.
     * @return the chosen alternative or null is none was found.
     */
    public DENOPTIMVertex getChosenAlternativeLink()
    {
        return chosenNewLink;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the AP mapping that allows the vertex chosen as alternative to
     * be installed in the slot originally occupied by the original vertex.
     * If more than one possibilities exist, then this will return a randomly 
     * chosen one. Consistency between the return value of this method and 
     * {@link GraphLinkFinder#getChosenAlternativeLink()} is guaranteed.
     * @return the chosen alternative or null is none was found.
     */
    public Map<Integer, Integer> getChosenAPMappingInt()
    {
        return chosenAPMapInt;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns all the AP mapping that were identified for any candidate 
     * vertex that could 
     * be installed in the slot originally occupied by the original vertex.
     * @return map of all AP mappings for each alternative vertex. 
     */
    public Map<DENOPTIMVertex, List<Map<Integer, Integer>>> getAllAlternativesFoundInt()
    {
        return allCompatLinksInt;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the AP mapping that allows the usage of the vertex chosen either
     * as alternative to be installed in the slot originally occupied by the 
     * original vertex, or to be inserted in between two vertexes. The meaning 
     * of the mapping depends on how this {@link GraphLinkFinder} was 
     * constructed. In particular, when this instance if constructed from an 
     * {@link DENOPTIMVertex}, the syntax of the AP mapping is:
     * <ul>
     * <li>null (TODO: FIXME by making the constructor store 
     * (AP reference):(AP reference)
     * mapping in
     * addition to the int:int mapping)</li>
     * </ul>
     * Otherwise, when this instance if constructed from an 
     * {@link DENOPTIMEdge}, the AP mapping's syntax is:
     * <ul>
     * <li>keys: the APs originally involved in making that edge,</li>
     * <li>values: the APs that belong on the new vertex returned by 
     * {@link GraphLinkFinder#getChosenAlternativeLink()} and that can be 
     * connected to the corresponding AP in the key.</li>
     * </ul>
     * If more than one possible mapping exist, then this will return a randomly 
     * chosen one. Consistency between the return value of this method and 
     * {@link GraphLinkFinder#getChosenAlternativeLink()} is guaranteed.
     * @return the chosen AP mapping or null is none was found.
     */
    public APMapping getChosenAPMapping()
    {
        return chosenAPMap;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns all the AP mapping that were identified for any candidate 
     * vertex that could be used either
     * as alternative to be installed in the slot originally occupied by the 
     * original vertex, or to be inserted in between two vertexes. 
     * The meaning 
     * of the mapping depends on how this {@link GraphLinkFinder} was 
     * constructed. 
     * @return map of all AP mappings for each alternative vertex. 
     */
    public Map<DENOPTIMVertex, List<APMapping>> getAllAlternativesFound()
    {
        return allCompatLinks;
    }
    
//------------------------------------------------------------------------------
    
    public boolean foundAlternativeLink()
    {
        return foundNewLink;
    }
    
//------------------------------------------------------------------------------

}