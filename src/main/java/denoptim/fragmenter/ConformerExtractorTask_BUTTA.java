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

package denoptim.fragmenter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import javax.vecmath.Point3d;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.xerces.xni.parser.XMLDTDContentModelSource;
import org.jgrapht.GraphMapping;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.alignment.KabschAlignment;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.io.iterator.IteratingSDFReader;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.FragIsomorphEdge;
import denoptim.graph.FragIsomorphNode;
import denoptim.graph.Fragment;
import denoptim.graph.FragmentIsomorphismInspector;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.io.DenoptimIO;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.task.Task;
import denoptim.utils.MoleculeUtils;
import denoptim.utils.TaskUtils;
import edu.uci.ics.jung.algorithms.util.KMeansClusterer;

/**
 * Task that analyzes an isomorphic family of fragments to identify the most 
 * representative fragment (i.e., the champion). 
 * The champion is extracted as the result of this task so that it is made 
 * available for further usage.
 */

public class ConformerExtractorTask_BUTTA extends Task
{   
    /**
     * Identifier of the isomorphic family this task deals with
     */
    private String isomorphicFamilyId = null;
    
    /**
     * File collecting (among others) the sampled members of the isomorphic 
     * family.
     */
    private File isoFamMembersFile;
    
    /**
     * The data structure holding the results of this task.
     */
    protected String results = null;
   
    /**
     * Settings for the calculation of the fitness
     */
    protected FragmenterParameters settings;
    
    /**
     * Logger for this task.
     */
    private Logger logger = null;
    
    /**
     * Pathname to thread-specific log.
     */
    private String logFilePathname = "unset";

//------------------------------------------------------------------------------
    

    public ConformerExtractorTask_BUTTA(Vertex oldChampions, 
            FragmenterParameters settings) throws SecurityException, IOException
    {
    	super(TaskUtils.getUniqueTaskIndex());
    	Object isoFamIdObj = oldChampions.getProperty(
    	        DENOPTIMConstants.ISOMORPHICFAMILYID);
    	if (isoFamIdObj==null)
    	{
    	    throw new Error("Attempt to run analysis of isomorphic family for "
    	            + "a fragment that does not declare the identity of its "
    	            + "family. Missing '" + DENOPTIMConstants.ISOMORPHICFAMILYID 
    	            + "'.");
    	}
    	this.isomorphicFamilyId = isoFamIdObj.toString();
        String taskAndFamId = this.getClass().getSimpleName() + "-" + id + "_" 
                + isomorphicFamilyId;
    	this.settings = settings;
    	
    	//Create the task-specific logger
        this.logger = Logger.getLogger(taskAndFamId);
        int n = logger.getHandlers().length;
        for (int i=0; i<n; i++)
        {
            logger.removeHandler(logger.getHandlers()[0]);
        }
        this.logFilePathname = settings.getWorkDirectory() + DenoptimIO.FS 
                + taskAndFamId;
        FileHandler fileHdlr = new FileHandler(logFilePathname);
        SimpleFormatter formatterTxt = new SimpleFormatter();
        fileHdlr.setFormatter(formatterTxt);
        logger.setUseParentHandlers(false);
        logger.addHandler(fileHdlr);
        logger.setLevel(settings.getLogger().getLevel());
        String header = "Started logging for " + taskAndFamId;
        logger.log(Level.INFO,header);
        
        String mwSlotID = FragmenterTools.getMWSlotIdentifier(oldChampions, 
                settings.getMWSlotSize());
        this.isoFamMembersFile = settings.getMWSlotFileNameAllFrags(mwSlotID);
        if (!this.isoFamMembersFile.exists())
        {
            throw new Error("Expected file '" 
                    + isoFamMembersFile.getAbsolutePath() + "' not found!");
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the pathname of the log file.
     */
    public String getLogFilePathname()
    {
        return logFilePathname;
    }

//------------------------------------------------------------------------------

    @Override
    public Object call() throws Exception
    {
        List<Vertex> sample = collectIsomorphicFragments();
        
        for (int i=0; i<sample.size(); i++)
        {
            Fragment fragA = (Fragment) sample.get(i);
            for (int j=i+1; j<sample.size(); j++)
            {
                Fragment fragB = (Fragment) sample.get(j);
                double minRMSD = Double.MAX_VALUE;
                try {
                    minRMSD = getMinimumRMSD(fragA,fragB);
                } catch (DENOPTIMException de) {
                    logger.log(Level.WARNING,"Failed to find isomorphism "
                            + "between fragment "+i+" and "+j+". Removing "
                            + "fragment "+j+" from sample.");
                    break;
                } catch (CDKException ce) {
                    logger.log(Level.WARNING,"Failed to align "
                            + "fragment "+i+" and "+j+". Removing "
                            + "fragment "+j+" from sample.");
                    break;
                }
              
                //TODO-gg
                /*
                dm.setElement(i,j,minValue);
                dm.setElement(j,i,minValue);
                */
            }
        }
        
        KMeansPlusPlusClusterer<ClusterableFragment> kk = 
                new KMeansPlusPlusClusterer<ClusterableFragment>(3,100, new DistanceMeasure() {
                    
                    @Override
                    public double compute(double[] a, double[] b)
                            throws DimensionMismatchException
                    {
                        // TODO Auto-generated method stub
                        return 0;
                    }
                });
        
        
        // Final message
        logger.log(Level.INFO,"Analysis of isomorphic family completed.");
        
        // We stop the logger's file handler to remove the lock file.
        for (Handler h : logger.getHandlers()) 
        {
            if (h instanceof FileHandler) {
                logger.removeHandler(h);
                h.close();
            }
        }
        
        completed = true;
        return results;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * 
     * @param fragA first fragment
     * @param fragB second fragment
     * @param i identifier of first fragment (used only for logging)
     * @param j identifier of second fragment (used only for logging)
     * @return
     * @throws DENOPTIMException if no isomorphism is found between the two 
     * fragments.
     * @throws CDKException if something goes wrong with the alignment of the 
     * structures.
     */
    protected static double getMinimumRMSD(Fragment fragA, Fragment fragB) 
            throws DENOPTIMException, CDKException
    {
        DefaultUndirectedGraph<FragIsomorphNode, FragIsomorphEdge> graphA =
                fragA.getJGraphFragIsomorphism();
        DefaultUndirectedGraph<FragIsomorphNode, FragIsomorphEdge> graphB =
                fragB.getJGraphFragIsomorphism();
        
        // Map graph nodes (atom and APs)
        FragmentIsomorphismInspector fii = 
                new FragmentIsomorphismInspector(fragA, fragB);
        if(!fii.isomorphismExists())
        {
            throw new DENOPTIMException("Failed to find isomorphism.");
        }
        
        // Get lowest RMSD among all mappings
        Iterator<GraphMapping<FragIsomorphNode, FragIsomorphEdge>> 
            mapingIterator = fii.getMappings();
        double minRMSD = Double.MAX_VALUE;
        while (mapingIterator.hasNext())
        {
            GraphMapping<FragIsomorphNode, FragIsomorphEdge> mapping = 
                    mapingIterator.next();
            
            // Translate graph nodes/vertexes into atoms to align
            IAtom[] atmsA = new IAtom[graphA.vertexSet().size()];
            IAtom[] atmsB = new IAtom[graphB.vertexSet().size()];
            int index = -1;
            for (FragIsomorphNode nA : graphA.vertexSet())
            {
                index++;
                FragIsomorphNode nB = mapping.getVertexCorrespondence(nA,
                        true);
                atmsA[index] = nodeToAtom(nA);
                atmsB[index] = nodeToAtom(nB);
            }
            
            // Align atoms
            KabschAlignment ka = new KabschAlignment(atmsA,atmsB);
            ka.align();
            double rmsd = ka.getRMSD();
            
            if (rmsd < minRMSD)
                minRMSD = rmsd;
        }
        return minRMSD;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * To use the {@link KabschAlignment} we need atoms with Point3d and
     * atomic masses. This method generates such atoms for nodes that represent
     * native atoms and for those that represent attachment points. The latter 
     * are treated as H atoms.
     */
    private static IAtom nodeToAtom(FragIsomorphNode node)
    {
        IAtom atm = null;
        if (node.isAtm())
        {
            atm = new Atom(((IAtom)node.getOriginal()).getSymbol(),
                    node.getPoint3d());
        } else {
            atm = new Atom("H",node.getPoint3d());
        }
        return atm;
    }
    
//------------------------------------------------------------------------------

    private List<Vertex> collectIsomorphicFragments()
    {
        IteratingSDFReader reader;
        try
        {
            reader = new IteratingSDFReader(
                    new FileInputStream(isoFamMembersFile), 
                    DefaultChemObjectBuilder.getInstance());
        } catch (FileNotFoundException e1)
        {
            // Cannot happen: we ensured the file exist, but it might have been 
            // removed after the check
            throw new Error("File '" + isoFamMembersFile + "' can "
                    + "not be found anymore.");
        }
        List<Vertex> sample = new ArrayList<Vertex>();
        int molId = -1;
        while (reader.hasNext())
        {
            molId++;
            IAtomContainer mol = reader.next();
            Object prop = mol.getProperty(DENOPTIMConstants.ISOMORPHICFAMILYID);
            if (prop==null)
            {
                continue;
            }
            if (isomorphicFamilyId.equals(prop.toString()))
            {
                try
                {
                    logger.log(Level.INFO,"Adding fragment " + molId 
                            + " '" + mol.getTitle() + "' to the sample.");
                    sample.add(new Fragment(mol,BBType.UNDEFINED));
                } catch (DENOPTIMException e)
                {
                    logger.log(Level.WARNING, "Skipping fragment " + molId 
                            + " '" + mol.getTitle() + "' because it could not "
                            + "be converted into a fragment.");
                }
            }
            if (sample.size()==FragmenterParameters.MAXISOMORPHICSAMPLESIZE)
            {
                logger.log(Level.INFO,"Sample reached the maximum size: "
                        + sample.size() + ". Ignoring any further fragment.");
                break;
            }
        }
        return sample;
    }

//------------------------------------------------------------------------------

}
