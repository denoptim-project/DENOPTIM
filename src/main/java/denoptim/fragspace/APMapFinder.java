package denoptim.fragspace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import denoptim.exception.DENOPTIMException;
import denoptim.graph.APMapping;
import denoptim.graph.DENOPTIMAttachmentPoint;
import denoptim.graph.DENOPTIMVertex;
import denoptim.utils.RandomUtils;

/**
 * An utility class to encapsulate the search for an 
 * {@link DENOPTIMAttachmentPoint}-{@link DENOPTIMAttachmentPoint} mapping.
 * Ignores symmetry, which is a potential thing to improve.
 * @author Marco Foscato
 */

public class APMapFinder
{   
    /**
     * The chosen {@link DENOPTIMAttachmentPoint}-{@link DENOPTIMAttachmentPoint}
     * mapping. 
     * In case of multiple possible AP mappings, the
     * result reported here is a randomly chosen one among the possible ones. 
     */
    private APMapping chosenAPMap;
    
    /**
     * The collection of all 
     * {@link DENOPTIMAttachmentPoint}-{@link DENOPTIMAttachmentPoint} mappings
     * that have been found.
     */
    private List<APMapping> allAPMappings = new ArrayList<APMapping>();
    
    /**
     * Maximum number of combinations. This prevents combinatorial explosion, 
     * but it is ignored if the constructor is required to screen all.
     * Remember that the combinations are anyway randomized, so even with the 
     * maximum limit on the number of combination to consider, there is no
     * systematic exclusion of specific combinations.
     */
    private static int maxCombs = 250;
    
//------------------------------------------------------------------------------

    /**
     * Constructor that launches the search for a mapping between the
     * {@link DENOPTIMAttachmentPoint}s on the first vertex to those of the
     * second. Note that if APs are available throughout any template barrier
     * we consider only the existence of an AP to be a reason for a compatible 
     * AP-AP mapping, irrespectively of the {@link APClass}. All APs that are
     * available (i.e., available throughout the templates barriers) will be
     * considered compatible because they do not have any requirement from
     * APClass compatibility rules or bond types.
     * @param vA the first vertex. This vertex defines the minimal requirements,
     * i.e., all {@link DENOPTIMAttachmentPoint}s on this vertex that are used
     * (also throughout the template barriers) will have
     * to be mapped into {@link DENOPTIMAttachmentPoint}s on the other vertex 
     * for the mapping to be successful.
     * @param vB the second vertex.
     * @param screenAll use <code>true</code> to NOT stop at the first 
     * compatible combinations.
     */
    public APMapFinder(DENOPTIMVertex vA, DENOPTIMVertex vB, boolean screenAll)
    {
        initialize(vA.getAttachmentPoints(), vB.getAttachmentPoints(), null, 
                screenAll, false, true);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor that launches the search for a mapping between the
     * {@link DENOPTIMAttachmentPoint}s on the first vertex to those of the
     * second. Note that if APs are available throughout any template barrier
     * we consider only the existence of an AP to be a reason for a compatible 
     * AP-AP mapping, irrespectively of the {@link APClass}.
     * @param vA the first vertex. This vertex defines the minimal requirements,
     * i.e., all {@link DENOPTIMAttachmentPoint}s on this vertex that are used
     * (also throughout the template barriers) will have
     * to be mapped into {@link DENOPTIMAttachmentPoint}s on the other vertex 
     * for the mapping to be successful.
     * @param vB the second vertex.
     * @param fixedRootAPs if not <code>null</code>, sets a required mapping 
     * that must be present in the all the AP mappings.
     * @param screenAll use <code>true</code> to NOT stop at the first 
     * compatible combinations.
     * @param onlyCompleteMappings use <code>true</code> to collect only mappings 
     * that include all of the APs on the first vertex.
     * @param compatibleIfFree use <code>true</code> to make APs that are 
     * available (i.e., available throughout the template barriers) be compatible.
     */
    public APMapFinder(DENOPTIMVertex vA, DENOPTIMVertex vB, 
            APMapping fixedRootAPs, boolean screenAll,
            boolean onlyCompleteMappings, boolean compatibleIfFree) 
    {
        initialize(vA.getAttachmentPoints(), vB.getAttachmentPoints(), 
                fixedRootAPs, screenAll, onlyCompleteMappings, compatibleIfFree);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor that launches the search for a mapping between the
     * {@link DENOPTIMAttachmentPoint}s on two lists. 
     * Note that if APs are available throughout any template barrier
     * we consider only the existence of an AP to be a reason for a compatible 
     * AP-AP mapping, irrespectively of the {@link APClass}.
     * @param lstA the first list. This list defines the minimal requirements,
     * i.e., all {@link DENOPTIMAttachmentPoint}s on this list that are used
     * (also throughout the template barriers) will have
     * to be mapped into {@link DENOPTIMAttachmentPoint}s on the other list 
     * for the mapping to be successful.
     * @param vB the second list.
     * @param fixedRootAPs if not <code>null</code>, sets a required mapping 
     * that must be present in the all the AP mappings.
     * @param screenAll use <code>true</code> to NOT stop at the first 
     * compatible combinations.
     * @param onlyCompleteMappings use <code>true</code> to collect only mappings 
     * that include all of the APs on the first vertex.
     * @param compatibleIfFree use <code>true</code> to make APs that are 
     * available (i.e., available throughout the template barriers) be compatible.
     */
    public APMapFinder(List<DENOPTIMAttachmentPoint> lstA, 
            List<DENOPTIMAttachmentPoint> lstB, 
            APMapping fixedRootAPs, boolean screenAll,
            boolean onlyCompleteMappings, boolean compatibleIfFree) 
    {
        initialize(lstA, lstB, 
                fixedRootAPs, screenAll, onlyCompleteMappings, compatibleIfFree);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor that launches the search for a mapping between the
     * {@link DENOPTIMAttachmentPoint}s on the first vertex to those of the
     * second. Note that is APs are available throughout any template barrier
     * we consider only the existence of an AP to be a reason for a compatible 
     * AP-AP mapping, irrespectively of the {@link APClass}.
     * @param vA the first vertex. This vertex defined the minimal requirements,
     * i.e., all {@link DENOPTIMAttachmentPoint}s on this vertex that are used
     * (also throughout the template barriers) will have
     * to be mapped into {@link DENOPTIMAttachmentPoint}s on the other vertex 
     * for the mapping to be successful.
     * @param vB the second vertex.
     * @param fixedRootAPs if not <code>null</code>, sets a required mapping 
     * that must be present in the all the AP mappings.
     * @param screenAll use <code>true</code> to NOT stop at the first 
     * compatible combinations.
     * @param onlyCompleteMappings use <code>true</code> to collect only mappings 
     * that include all of the APs on the first vertex.
     * @param compatibleIfFree use <code>true</code> to make APs that are 
     * available (i.e., available throughout the template barriers) be compatible.
     */
    private void initialize(List<DENOPTIMAttachmentPoint> lstA, 
            List<DENOPTIMAttachmentPoint> lstB, 
            APMapping fixedRootAPs, boolean screenAll, 
            boolean onlyCompleteMappings, boolean compatibleIfFree) 
    {
        // We map all the compatibilities before choosing a specific mapping
        LinkedHashMap<DENOPTIMAttachmentPoint,List<DENOPTIMAttachmentPoint>> 
            apCompatilities = findAllPossibleMappings(lstA, lstB, 
                    compatibleIfFree);
        
        // keys is used just to keep the map keys sorted in a separate list
        // so that the order is randomized only once, then it is retained.
        List<DENOPTIMAttachmentPoint> keys = 
                new ArrayList<DENOPTIMAttachmentPoint>(
                        apCompatilities.keySet());
        if (fixedRootAPs!=null)
        {
            if (keys.size()+fixedRootAPs.size() < lstA.size())
            {
                return;
            }
            // Since these are constrained we do not need them among the keys for 
            // looking over the combinations.
            keys.removeAll(fixedRootAPs.keySet());
        } else {
            if (keys.size() < lstA.size())
            {
                return;
            }
        }
            
        // Identify APs in old link that are used: we want a mapping that
        // includes all of those. This, to be able to change the link without
        // removing any existing branch.
        ArrayList<DENOPTIMAttachmentPoint> oldAPsRequiredToHaveAMapping = new
                ArrayList<DENOPTIMAttachmentPoint>();
        if (fixedRootAPs!=null)
        {
            // There should be only one key.
            oldAPsRequiredToHaveAMapping.addAll(fixedRootAPs.keySet());
        }
        if (onlyCompleteMappings)
        {
            for (DENOPTIMAttachmentPoint oldAp : lstA)
            {
                if (!oldAPsRequiredToHaveAMapping.contains(oldAp))
                    oldAPsRequiredToHaveAMapping.add(oldAp);
            }
        } else {
            for (DENOPTIMAttachmentPoint oldAp : lstA)
            {
                if (oldAp.isAvailableThroughout())
                {
                    if (apCompatilities.containsKey(oldAp))
                    {
                        apCompatilities.get(oldAp).add(null);
                    } else {
                        List<DENOPTIMAttachmentPoint> lst = 
                                new ArrayList<DENOPTIMAttachmentPoint>();
                        lst.add(null);
                        apCompatilities.put(oldAp,lst);
                    }
                } else {
                    if (!oldAPsRequiredToHaveAMapping.contains(oldAp))
                    {
                        oldAPsRequiredToHaveAMapping.add(oldAp);
                    }
                    if (!keys.contains(oldAp))
                    {
                        // In this case we have no hope of finding a mapping 
                        // that satisfies out needs.
                        continue;
                    }
                }
            }
        }
        
        // Get all possible combinations of compatible AP pairs
        int currentKey = 0;
        APMapping currentMapping = new APMapping();
        if (fixedRootAPs!=null)
        {
            currentMapping = fixedRootAPs.clone(); //shallow
        }
        // We try the comprehensive approach, but if that is too demanding
        // and gets stopped, then we run a series of simplified attempts
        // to hit a decent combination with "lucky shots".
        Boolean stopped = false;
        if (keys.size()>0)
        {
            stopped = FragmentSpaceUtils.recursiveCombiner(keys, 
                    currentKey, apCompatilities, currentMapping, allAPMappings, 
                    screenAll, maxCombs);
        } else {
            // This would have been done by the recursive combiner
            allAPMappings.add(currentMapping);
        }
        
        // Keep only mappings that allow retaining the structure 
        // (i.e., include all required/used APs of the original vertex)
        List<APMapping> toRemove = new ArrayList<APMapping>();
        for (APMapping c : allAPMappings)
        {
            if (!c.containsAllKeys(oldAPsRequiredToHaveAMapping))
            {
                toRemove.add(c);
            }
        }
        allAPMappings.removeAll(toRemove);
        
        // If we where interrupted, we try to get a proper mapping again.
        // This time we ignore all the possibilities and try a more direct 
        // approach, which, however, cannot account for all possible combs.
        if (stopped && allAPMappings.isEmpty())
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
                    allAPMappings.add(apMap);
                    break;
                }
            }
        }
        if (allAPMappings.size() > 0)
            chosenAPMap = RandomUtils.randomlyChooseOne(allAPMappings);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Compares the {@link DENOPTIMAttachmentPoint} of two lists searching for
     * all the APs of the second list that are "compatible" with each of the APs
     * of the first list. Note that "compatible" does not mean {@link APClass}-
     * compatible to form connection, but that can be interchanges, i.e., one
     * AP from the second list is compatible with one from the first list, if it
     * can replace the latter in whatever role that AP has.
     * @param lstA the first list of APs.
     * @param lstB the second list ofAPs.
     * @param compatibleIfFree use <code>true</code> to make APs that are 
     * available (i.e., available throughout the template barriers) be 
     * compatible.
     * @return the map between the APs in the first list (keys) and the list 
     * (value) of all those from the second list that are "compatible" with it.
     */
    public static LinkedHashMap<DENOPTIMAttachmentPoint,List<DENOPTIMAttachmentPoint>> 
        findAllPossibleMappings(List<DENOPTIMAttachmentPoint> lstA, 
            List<DENOPTIMAttachmentPoint> lstB, boolean compatibleIfFree)
    {
        LinkedHashMap<DENOPTIMAttachmentPoint,List<DENOPTIMAttachmentPoint>> 
        apCompatilities = new LinkedHashMap<DENOPTIMAttachmentPoint,
        List<DENOPTIMAttachmentPoint>>();
    
        for (DENOPTIMAttachmentPoint oAP : lstA)
        {
            for (DENOPTIMAttachmentPoint cAP : lstB)
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
                        //TODO: template's required AP will have to be considered.
                        if (compatibleIfFree)
                        {
                            compatible = true;
                        }
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
        return apCompatilities;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns <code>true</code> if any mapping has been found.
     * @return  <code>true</code> if any mapping has been found.
     */
    public boolean foundMapping()
    {
        return !allAPMappings.isEmpty();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the 
     * {@link DENOPTIMAttachmentPoint}-{@link DENOPTIMAttachmentPoint} mapping 
     * chosen among the possible mappings.
     * If more than one possible mapping exist, then this will return a randomly 
     * chosen one.
     * @return the chosen AP mapping or null is none was found.
     */
    public APMapping getChosenAPMapping()
    {
        return chosenAPMap;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns all 
     * {@link DENOPTIMAttachmentPoint}-{@link DENOPTIMAttachmentPoint} mapping 
     * found.
     * @return the collection of all AP mappings (can be empty, but not null)
     * found. This is either the total amount of possible mappings or the 
     * amount found before stopping to prevent combinatorial explosion.
     */
    public List<APMapping> getAllAPMappings()
    {
        return allAPMappings;
    }
    
//------------------------------------------------------------------------------

}