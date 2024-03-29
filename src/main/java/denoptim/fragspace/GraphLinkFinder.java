/*
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package denoptim.fragspace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import denoptim.exception.DENOPTIMException;
import denoptim.graph.APClass;
import denoptim.graph.APMapping;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.Edge;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;

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
    private Vertex chosenNewLink;

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
    private LinkedHashMap<Vertex,List<APMapping>> allCompatLinks = 
            new LinkedHashMap<Vertex,List<APMapping>>();
    
    /**
     * Maximum number of combinations. This prevents combinatorial explosion, 
     * but it is ignored if the constructor is required to screen all.
     * Remember that the combinations are anyway randomized, so even with the 
     * maximum limit on the number of combination to consider, there is no
     * systematic exclusion of specific combinations.
     */
    private static int maxCombs = 250;
    
    /**
     * Flag recording if at least one alternative vertex was found.
     */
    private boolean foundNewLink = false;
    
    /**
     * Parameter and reference to the fragment space
     */
    private FragmentSpace fragmentSpace = null;
    
//------------------------------------------------------------------------------

    /**
     * Constructor that specifies which vertex has to be replaced. The search 
     * for an alternative building block takes place within this constructor.
     * @param originalLink the vertex to replace.
     * @throws DENOPTIMException if the required new building block ID cannot
     * be used.
     */
    public GraphLinkFinder(FragmentSpace fragSpace, Vertex originalLink) 
            throws DENOPTIMException
    {  
        this(fragSpace, originalLink, -1, false);
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
    public GraphLinkFinder(FragmentSpace fragSpace,
            Vertex originalLink, int newBuildingBlockID) 
            throws DENOPTIMException
    {  
        this(fragSpace, originalLink, newBuildingBlockID, false);
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
    public GraphLinkFinder(FragmentSpace fragSpace,
            Vertex originalLink, int newBuildingBlockID,
            boolean screenAll) throws DENOPTIMException 
    {   
        this.fragmentSpace = fragSpace;
        
        ArrayList<Vertex> candidates = getCandidateBBs(
                originalLink.getBuildingBlockType(), newBuildingBlockID);
        
        int candidatesOrigSize = candidates.size();
        for (int i=0; i<candidatesOrigSize; i++)
        {
            if (foundNewLink && !screenAll)
                break;
            
            Vertex originalBB = fragSpace.getRandomizer().randomlyChooseOne(
                    candidates);
            candidates.remove(originalBB);
            try
            {
                chosenNewLink = fragmentSpace.getVertexFromLibrary(
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
            APMapFinder apmf = new APMapFinder(fragSpace, originalLink, 
                    chosenNewLink, screenAll);
            
            if (!apmf.foundMapping())
            {
                this.chosenNewLink = null;
                continue;
            } else {
                this.foundNewLink = true;
            }
            
            chosenAPMap = apmf.getChosenAPMapping();
            
            if (screenAll)
            {
                allCompatLinks.put(chosenNewLink, apmf.getAllAPMappings());
            }
        }
    }
    
//------------------------------------------------------------------------------
    
    private ArrayList<Vertex> getCandidateBBs(BBType bbt, int bbId) 
            throws DENOPTIMException 
    {
        ArrayList<Vertex> candidates = new ArrayList<Vertex>();
        if (bbId<0)
        {
            switch (bbt)
            {
                case FRAGMENT:
                    candidates.addAll(fragmentSpace.getFragmentLibrary());
                    break;
                case SCAFFOLD:
                    candidates.addAll(fragmentSpace.getScaffoldLibrary());
                case CAP:
                    break;
                default:
                    break;
            }
        } else {
            Vertex chosenOne =  fragmentSpace.getVertexFromLibrary(bbt, bbId);
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
    public GraphLinkFinder(FragmentSpace fragSpace, Edge originalEdge) 
            throws DENOPTIMException
    {  
        this(fragSpace, originalEdge,-1,false);
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
    public GraphLinkFinder(FragmentSpace fragSpace, Edge originalEdge, 
            int newBuildingBlockID) throws DENOPTIMException
    {  
        this(fragSpace, originalEdge,newBuildingBlockID,false);
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
    public GraphLinkFinder(FragmentSpace fragSpace,
            Edge originalEdge, int newBuildingBlockID,
            boolean screenAll) throws DENOPTIMException 
    {
        this.fragmentSpace = fragSpace;
        ArrayList<Vertex> candidates = getCandidateBBs(
                originalEdge.getTrgAP().getOwner().getBuildingBlockType(), 
                newBuildingBlockID);

        int candidatesOrigSize = candidates.size();
        for (int i=0; i<candidatesOrigSize; i++)
        {
            if (foundNewLink && !screenAll)
                break;
            
            Vertex originalBB = fragSpace.getRandomizer().randomlyChooseOne(
                    candidates);
            candidates.remove(originalBB);
            try
            {
                chosenNewLink = fragmentSpace.getVertexFromLibrary(
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
            LinkedHashMap<AttachmentPoint,List<AttachmentPoint>> apCompatilities =
                    new LinkedHashMap<AttachmentPoint,List<AttachmentPoint>>();
            
            List<AttachmentPoint> needeAPs = new ArrayList<AttachmentPoint>();
            needeAPs.add(originalEdge.getSrcAP());
            needeAPs.add(originalEdge.getTrgAP());
            for (int j=0; j<2;j++)
            {
                AttachmentPoint oAP = needeAPs.get(j);
                APClass oriAPC = oAP.getAPClass();
                for (AttachmentPoint cAP : chosenNewLink.getAttachmentPoints())
                {  
                    boolean compatible = false;
                    if (fragmentSpace.useAPclassBasedApproach())
                    {
                        if (j==0 
                                && oriAPC.isCPMapCompatibleWith(
                                        cAP.getAPClass(),
                                        fragmentSpace))
                            compatible = true;
                        else if (j==1 
                                && cAP.getAPClass().isCPMapCompatibleWith(
                                        oriAPC, fragmentSpace))
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
                            List<AttachmentPoint> lst = 
                                    new ArrayList<AttachmentPoint>();
                            lst.add(cAP);
                            apCompatilities.put(oAP,lst);
                        }
                    }
                }
            }
            // keys is used just to keep the map keys sorted in a separate list
            // so that the order is randomized only once, then it is retained.
            List<AttachmentPoint> keys = 
                    new ArrayList<AttachmentPoint>(
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
            FragmentSpaceUtils.recursiveCombiner(keys, 
                    currentKey, apCompatilities, currentMapping, apMappings, 
                    screenAll, maxCombs);
            
            if (apMappings.isEmpty())
            {
                this.chosenNewLink = null;
                continue;
            } else {
                this.foundNewLink = true;
            }
            
            this.chosenAPMap = fragSpace.getRandomizer().randomlyChooseOne(
                    apMappings);
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
     * {@link GraphLinkFinder#getChosenAPMapping()} is guaranteed.
     * @return the chosen alternative or null is none was found.
     */
    public Vertex getChosenAlternativeLink()
    {
        return chosenNewLink;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the AP mapping that allows the usage of the vertex chosen either
     * as alternative to be installed in the slot originally occupied by the 
     * original vertex, or to be inserted in between two vertexes. The meaning 
     * of the mapping depends on how this {@link GraphLinkFinder} was 
     * constructed. In particular, when this instance is constructed from an 
     * {@link Vertex}, the syntax of the AP mapping is:
     * <ul>
     * <li>keys: the APs of the original vertex from which we seek replacement.
     * </li>
     * <li>values: the APs that belong on the new vertex returned by 
     * {@link GraphLinkFinder#getChosenAlternativeLink()}.</li>
     * </ul>
     * Otherwise, when this instance if constructed from an 
     * {@link Edge}, the AP mapping's syntax is:
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
    public LinkedHashMap<Vertex, List<APMapping>> getAllAlternativesFound()
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