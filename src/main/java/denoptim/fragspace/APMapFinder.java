package denoptim.fragspace;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import denoptim.graph.APClass;
import denoptim.graph.APMapping;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.DGraph;
import denoptim.graph.Vertex;
import denoptim.utils.Randomizer;

/**
 * An utility class to encapsulate the search for an 
 * {@link AttachmentPoint}-{@link AttachmentPoint} mapping.
 * Ignores symmetry, which is a potential thing to improve.
 * @author Marco Foscato
 */

public class APMapFinder
{   
    /**
     * The chosen {@link AttachmentPoint}-{@link AttachmentPoint}
     * mapping. 
     * In case of multiple possible AP mappings, the
     * result reported here is a randomly chosen one among the possible ones. 
     */
    private APMapping chosenAPMap;
    
    /**
     * The collection of all 
     * {@link AttachmentPoint}-{@link AttachmentPoint} mappings
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
    
    /**
     * Program-specific fragment space
     */
    private FragmentSpace fragSpace = null;
    
//------------------------------------------------------------------------------

    /**
     * Constructor that launches the search for a mapping between the
     * {@link AttachmentPoint}s on the first vertex to those of the
     * second. Note that if APs are available throughout any template barrier
     * we consider only the existence of an AP to be a reason for a compatible 
     * AP-AP mapping, irrespectively of the {@link APClass}. All APs that are
     * available (i.e., available throughout the templates barriers) will be
     * considered compatible because they do not have any requirement from
     * APClass compatibility rules or bond types.
     * The present status of the APs on each vertex (i.e., whether they are used
     * or available as defined by the 
     * {@link DGraph#getInterfaceAPs(List)} method when considering a
     * single-vertex subgraph) 
     * determines which APs will be required to have a mapping.
     * @param fragSpace the fragment space to use.
     * @param vA the first vertex. 
     * @param vB the second vertex.
     * @param screenAll use <code>true</code> to NOT stop at the first 
     * compatible combinations.
     */
    public APMapFinder(FragmentSpace fragSpace,
            Vertex vA, Vertex vB, boolean screenAll)
    {
        this(fragSpace, vA, vB, null, screenAll, false, true);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor that launches the search for a mapping between the
     * {@link AttachmentPoint}s on the first vertex to those of the
     * second. Note that if APs are available throughout any template barrier
     * we consider only the existence of an AP to be a reason for a compatible 
     * AP-AP mapping, irrespectively of the {@link APClass}.
     * The present status of the APs on each vertex (i.e., whether they are used
     * or available as defined by the 
     * {@link DGraph#getInterfaceAPs(List)} method when considering a
     * single-vertex subgraph) 
     * determines which APs will be required to have a mapping.
     * @param fragSpace the fragment space to use.
     * @param vA the first vertex.
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
    public APMapFinder(FragmentSpace fragSpace, 
            Vertex vA, Vertex vB, 
            APMapping fixedRootAPs, boolean screenAll,
            boolean onlyCompleteMappings, boolean compatibleIfFree) 
    {
        this.fragSpace = fragSpace;
        List<AttachmentPoint> needyAPsA = 
                new ArrayList<AttachmentPoint>();
        if (vA.getGraphOwner()!=null)
        {
            List<Vertex> subgraph = new ArrayList<Vertex>();
            subgraph.add(vA);
            needyAPsA = vA.getGraphOwner().getInterfaceAPs(subgraph);
        }
        List<AttachmentPoint> needyAPsB = 
                new ArrayList<AttachmentPoint>();
        if (vB.getGraphOwner()!=null)
        {
            List<Vertex> subgraph = new ArrayList<Vertex>();
            subgraph.add(vB);
            needyAPsB = vB.getGraphOwner().getInterfaceAPs(subgraph);
        }
        findAllMappings(vA.getAttachmentPoints(), needyAPsA, 
                vB.getAttachmentPoints(), needyAPsB,
                fixedRootAPs, screenAll, onlyCompleteMappings, compatibleIfFree);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor that launches the search for a mapping between the
     * {@link AttachmentPoint}s on two lists. 
     * Note that if APs are available throughout any template barrier
     * we consider only the existence of an AP to be a reason for a compatible 
     * AP-AP mapping, irrespectively of the {@link APClass}.
     * @param lstA the first list.
     * @param needyAPsA a subset of the attachment points in the first list and 
     * that are required to have appear in any mapping.
     * @param lstB the second list.
     * @param needyAPsB a subset of the attachment points in the second list and 
     * that are required to have appear in any mapping.
     * @param fixedRootAPs if not <code>null</code>, sets a required mapping 
     * that must be present in the all the AP mappings.
     * @param screenAll use <code>true</code> to NOT stop at the first 
     * compatible combinations.
     * @param onlyCompleteMappings use <code>true</code> to collect only mappings 
     * that include all of the APs on the first vertex.
     * @param compatibleIfFree use <code>true</code> to make APs that are 
     * available (i.e., available throughout the template barriers) be compatible.
     */
    public APMapFinder(FragmentSpace fragSpace,
            List<AttachmentPoint> lstA, 
            List<AttachmentPoint> needyAPsA,
            List<AttachmentPoint> lstB, 
            List<AttachmentPoint> needyAPsB,
            APMapping fixedRootAPs, boolean screenAll,
            boolean onlyCompleteMappings, boolean compatibleIfFree) 
    {
        this.fragSpace = fragSpace;
        findAllMappings(lstA, needyAPsA, lstB, needyAPsB,
                fixedRootAPs, screenAll, onlyCompleteMappings, compatibleIfFree);
    }
    
//------------------------------------------------------------------------------

    /**
     * Searches for mappings between the
     * {@link AttachmentPoint}s on the two lists. 
     * Note that is APs are available throughout any template barrier
     * we consider only the existence of an AP to be a reason for a compatible 
     * AP-AP mapping, irrespectively of the {@link APClass}.
     * @param lstA the first list
     * @param lstB the second list.
     * @param fixedRootAPs if not <code>null</code>, sets a required mapping 
     * that will be added to all the AP mappings.
     * @param screenAll use <code>true</code> to NOT stop at the first 
     * compatible combinations.
     * @param onlyCompleteMappings use <code>true</code> to collect only mappings 
     * that include all of the APs on the first vertex.
     * @param compatibleIfFree use <code>true</code> to make APs that are 
     * available (i.e., available throughout the template barriers) be compatible.
     */
    private void findAllMappings(List<AttachmentPoint> lstA, 
            List<AttachmentPoint> needyAPsA,
            List<AttachmentPoint> lstB, 
            List<AttachmentPoint> needyAPsB,
            APMapping fixedRootAPs, boolean screenAll, 
            boolean onlyCompleteMappings, boolean compatibleIfFree) 
    {
        // Remove from the lists those APs that have already a mapping
        List<AttachmentPoint> purgedLstA = new ArrayList<AttachmentPoint>(lstA);
        if (fixedRootAPs!=null)
        {
            purgedLstA.removeAll(fixedRootAPs.keySet());
        }
        List<AttachmentPoint> purgedLstB = new ArrayList<AttachmentPoint>(lstB);
        if (fixedRootAPs!=null)
        {
            purgedLstB.removeAll(fixedRootAPs.values());
        }
        
        // Map all the compatibilities before choosing a specific mapping
        LinkedHashMap<AttachmentPoint,List<AttachmentPoint>> apCompatilities = 
                findMappingCompatibileAPs(purgedLstA, purgedLstB, 
                    compatibleIfFree, fragSpace);
        
        // The 'keys' is used just to keep the map keys sorted in a separate list
        // so that the order is randomized only once, then it is retained.
        List<AttachmentPoint> keys = new ArrayList<AttachmentPoint>(
                        apCompatilities.keySet());
        if (fixedRootAPs!=null)
        {
            // Since these are constrained we do not need them among the keys 
            // when looking over the combinations of keys
            keys.removeAll(fixedRootAPs.keySet());
        }
        
        // Test if we have enough AP compatibilities to satisfy the constraints
        Set<AttachmentPoint> doableAPsA = new HashSet<AttachmentPoint>(keys);
        if (fixedRootAPs!=null)
            doableAPsA.addAll(fixedRootAPs.keySet());
        Set<AttachmentPoint> doableAPsB = new HashSet<AttachmentPoint>();
        apCompatilities.values().stream().forEach(l -> doableAPsB.addAll(l));
        if (onlyCompleteMappings)
        {
            for (AttachmentPoint oldAp : lstA)
            {
                if (!needyAPsA.contains(oldAp))
                    needyAPsA.add(oldAp);
            }
            for (AttachmentPoint oldAp : lstB)
            {
                if (!needyAPsB.contains(oldAp))
                    needyAPsB.add(oldAp);
            }
        }
        Set<AttachmentPoint> mustBeDoableA = new HashSet<AttachmentPoint>(
                needyAPsA);
        Set<AttachmentPoint> mustBeDoableB = new HashSet<AttachmentPoint>(
                needyAPsB);
        if (fixedRootAPs!=null)
        {
            mustBeDoableA.removeAll(fixedRootAPs.keySet());
            mustBeDoableB.removeAll(fixedRootAPs.values());
        }
        if (!doableAPsA.containsAll(mustBeDoableA) 
                || !doableAPsB.containsAll(mustBeDoableB))
        {
            return;
        }
        
        if (!onlyCompleteMappings)
        {
            for (AttachmentPoint oldAp : lstA)
            {
                if (oldAp.isAvailableThroughout())
                {
                    // NB: adding 'null' will allow the presence of AP mappings
                    // where oldAP is intentionally left out of the mapping thus
                    // allowing to let it stay free.
                    if (apCompatilities.containsKey(oldAp))
                    {
                        apCompatilities.get(oldAp).add(null);
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
        
        // Purge according to lists of APs that needs a mapping
        List<APMapping> toRemove = new ArrayList<APMapping>();
        for (APMapping c : allAPMappings)
        {
            if (!c.containsAllKeys(needyAPsA))
            {
                toRemove.add(c);
            }
            if (!c.containsAllValues(needyAPsB))
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
                List<AttachmentPoint> used = 
                        new ArrayList<AttachmentPoint>();
                List<AttachmentPoint> availKeys = 
                        new ArrayList<AttachmentPoint>();
                availKeys.addAll(needyAPsA);
                boolean abandon = false;
                for (int jj=0; jj<needyAPsA.size(); jj++)
                {
                    AttachmentPoint ap = 
                            fragSpace.getRandomizer().randomlyChooseOne(
                                    availKeys);
                    availKeys.remove(ap);
                    List<AttachmentPoint> availPartners = 
                            new ArrayList<AttachmentPoint>();
                    availPartners.addAll(apCompatilities.get(ap));
                    boolean done = false;
                    for (int j=0; j<apCompatilities.get(ap).size(); j++)
                    {
                        AttachmentPoint chosenAvail = 
                                fragSpace.getRandomizer().randomlyChooseOne(
                                        availPartners);
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
            chosenAPMap = fragSpace.getRandomizer().randomlyChooseOne(
                    allAPMappings);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Compares the {@link AttachmentPoint} of two lists searching for
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
    public static LinkedHashMap<AttachmentPoint,List<AttachmentPoint>> 
        findMappingCompatibileAPs(List<AttachmentPoint> lstA, 
            List<AttachmentPoint> lstB, boolean compatibleIfFree,
            FragmentSpace fragSpace)
    {
        LinkedHashMap<AttachmentPoint,List<AttachmentPoint>> 
        apCompatilities = new LinkedHashMap<AttachmentPoint,
        List<AttachmentPoint>>();
    
        for (AttachmentPoint oAP : lstA)
        {
            for (AttachmentPoint cAP : lstB)
            {  
                boolean compatible = false;
                if (fragSpace.useAPclassBasedApproach())
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
                        AttachmentPoint lAP = 
                                oAP.getLinkedAPThroughout();
                        if (oAP.isSrcInUserThroughout())
                        {
                            if (lAP!=null && cAP.getAPClass()
                                  .isCPMapCompatibleWith(lAP.getAPClass(), 
                                          fragSpace))
                            {
                                compatible = true;
                            }
                        } else {
                            if (lAP!=null && lAP.getAPClass()
                                  .isCPMapCompatibleWith(cAP.getAPClass(), 
                                          fragSpace))
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
                        List<AttachmentPoint> lst = 
                                new ArrayList<AttachmentPoint>();
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
     * {@link AttachmentPoint}-{@link AttachmentPoint} mapping 
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
     * {@link AttachmentPoint}-{@link AttachmentPoint} mapping 
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