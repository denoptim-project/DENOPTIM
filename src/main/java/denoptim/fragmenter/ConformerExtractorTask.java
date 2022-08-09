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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import javax.swing.RepaintManager;
import javax.vecmath.Point3d;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.clustering.MultiKMeansPlusPlusClusterer;
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
import denoptim.files.FileFormat;
import denoptim.fragmenter.FragmentClusterer.DistanceAsRMSD;
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
import denoptim.utils.TaskUtils;
import edu.uci.ics.jung.algorithms.util.KMeansClusterer;
import org.biojava.nbio.structure.geometry.SuperPositionSVD;

/**
 * Task that analyzes an isomorphic family of fragments to identify the most 
 * representative fragment (i.e., the champion). 
 * The champion is extracted as the result of this task so that it is made 
 * available for further usage.
 */

public class ConformerExtractorTask extends Task
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
    

    public ConformerExtractorTask(Vertex oldChampions, 
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
                + taskAndFamId + ".log";
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
        List<ClusterableFragment> sample = collectClusterableFragmentsFromFile();
        
        FragmentClusterer clusterer = new FragmentClusterer(sample, settings, 
                logger);
        clusterer.cluster();
        
        List<Fragment> representativeFragments = null;
        String pathname = "";
        if (settings.isUseCentroidsAsRepresentativeConformer())
        {
            representativeFragments = clusterer.getClusterCentroids();
            pathname = getClusterCentroidsPathname(settings, isomorphicFamilyId);
        } else {
            representativeFragments = clusterer.getNearestToClusterCentroids();
            pathname = getChosenFragPathname(settings, isomorphicFamilyId);
        }
        List<Vertex> representativeVertexes = new ArrayList<Vertex>();
        representativeVertexes.addAll(representativeFragments);
        DenoptimIO.writeVertexesToFile(new File(pathname), FileFormat.VRTXSDF, 
                representativeVertexes);
        results = pathname;
        
        if (settings.isSaveClustersOfConformerToFile())
        {
            List<List<Fragment>> clusters = clusterer.getTransformedClusters();
            for (int iCluster=0; iCluster<clusters.size(); iCluster++)
            {
                List<Vertex> clusterMembers = new ArrayList<Vertex>();
                clusterMembers.addAll(clusters.get(iCluster));
                DenoptimIO.writeVertexesToFile(new File(getClusterPathname(
                        settings, isomorphicFamilyId, iCluster)),
                        FileFormat.VRTXSDF, clusterMembers);
            }
        }
        
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
     * Collects the clusterable fragments from the disk.
     * These fragments are fragment with a list having a consistent
     * ordering of the atoms/APs, so that such order can be used
     * to calculate RMSD between fragments.
     * @return
     * @throws DENOPTIMException
     */
    private List<ClusterableFragment> collectClusterableFragmentsFromFile() 
            throws DENOPTIMException
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
        return extractClusterableFragments(reader,
                isomorphicFamilyId, logger);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Analyzes all the entries provided by the iterator and extracts those
     * that pertain the specified isomorphic family.
     * @param reader the iterator providing the input.
     * @param isomorphicFamilyId the identifier of the isomorphic family to work
     * with.
     * @param logger task dedicated logger.
     * @return a list of fragments where each fragment has a consistent ordering 
     * of its atoms/APs that reflects the isomorphic mapping and allows to 
     * compare the fragments geometrically. 
     */
    public static List<ClusterableFragment> extractClusterableFragments(
            Iterator<IAtomContainer> reader, String isomorphicFamilyId, 
            Logger logger)
    {
        List<ClusterableFragment> sample = new ArrayList<ClusterableFragment>();
        int molId = -1;
        Fragment firstFrag = null;
        while (reader.hasNext())
        {
            if (sample.size()==FragmenterParameters.MAXISOMORPHICSAMPLESIZE)
            {
                logger.log(Level.INFO,"Sample reached the maximum size: "
                        + sample.size() + ". Ignoring any further fragment.");
                break;
            }
            
            // Read in the next fragment
            molId++;
            IAtomContainer mol = reader.next();
            Object prop = mol.getProperty(DENOPTIMConstants.ISOMORPHICFAMILYID);
            if (prop==null)
            {
                continue;
            }
            
            String molName = "";
            if (mol.getTitle()!=null && !mol.getTitle().isBlank())
                molName = "'" + mol.getTitle() + "' ";
            
            Fragment frag = null;
            if (isomorphicFamilyId.equals(prop.toString()))
            {
                try
                {
                    logger.log(Level.INFO,"Adding fragment " + molId 
                            + " " + molName + "to the sample of isomorphic "
                            + "family.");
                    frag = new Fragment(mol,BBType.UNDEFINED);
                } catch (DENOPTIMException e)
                {
                    logger.log(Level.WARNING, "Skipping fragment " + molId 
                            + " " + molName + "because it could not "
                            + "be converted into a fragment.");
                    continue;
                }
            } else {
                logger.log(Level.INFO, "Skipping fragment " + molId 
                        + " " + molName + "because it does not "
                        + "belong to isomorphic family '" + isomorphicFamilyId 
                        + "'.");
                continue;
            }
            
            // The clusterable fragments are fragment with a consistent
            // ordering of the atoms/APs list, so that such order can be used
            // to calculate RMSD between fragments.
            ClusterableFragment clusterable =  new ClusterableFragment(frag);
            if (sample.size()==0)
            {
                clusterable.setOrderOfNodes(
                        clusterable.getJGraphFragIsomorphism().vertexSet());
                sample.add(clusterable);
                firstFrag = frag;
            } else {
                FragmentAlignement fa;
                try
                {
                    fa = new FragmentAlignement(firstFrag, frag);
                } catch (DENOPTIMException e)
                {
                    logger.log(Level.WARNING, "Skipping fragment " + molId 
                            + " '" + mol.getTitle() + "' because no "
                            + "isomorphism could be found with the first "
                            + "fragment in the sample.");
                    continue;
                }
                
                List<FragIsomorphNode> orderedNodes = 
                        new ArrayList<FragIsomorphNode>();
                for (FragIsomorphNode nOnFirst : sample.get(0).getOrderedNodes())
                {
                    orderedNodes.add(
                            fa.getLowestRMSDMapping().getVertexCorrespondence(
                                    nOnFirst, true));
                }
                clusterable.setOrderOfNodes(orderedNodes);
                sample.add(clusterable);
            }
        }
        return sample;
    }
    
//------------------------------------------------------------------------------

    /**
     * Builds the pathname for the file where we save the members of a given 
     * cluster.
     * @param settings settings we work with.
     * @param i the index of the cluster
     * @return the pathname
     */
    static String getClusterPathname(FragmenterParameters settings, 
            String isomorphicFamilyId, int i)
    {
        return settings.getWorkDirectory() + DenoptimIO.FS 
                + isomorphicFamilyId + "_cluster-" + i + ".sdf";
    }

//------------------------------------------------------------------------------

    /**
     * Builds the pathname for the file where we save all the centroids of 
     * clusters.
     * @param settings settings we work with.
     * @return the pathname
     */
    static String getClusterCentroidsPathname(FragmenterParameters settings, 
            String isomorphicFamilyId)
    {
        return settings.getWorkDirectory() + DenoptimIO.FS 
                + isomorphicFamilyId + "_centroids.sdf";
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Builds the pathname for the file where we save all the fragments that we
     * found to be closest to each cluster's centroid.
     * @param settings settings we work with.
     * @return the pathname
     */
    static String getChosenFragPathname(FragmenterParameters settings, 
            String isomorphicFamilyId)
    {
        return settings.getWorkDirectory() + DenoptimIO.FS 
                + isomorphicFamilyId + "_chosen.sdf";
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the pathname to the files collecting results
     */
    public String getResultFile()
    {
        return results;
    }

//------------------------------------------------------------------------------

}
