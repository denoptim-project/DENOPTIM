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
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.iterator.IteratingSDFReader;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.FileFormat;
import denoptim.graph.FragIsomorphNode;
import denoptim.graph.Fragment;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.io.DenoptimIO;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.task.Task;
import denoptim.utils.TaskUtils;

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
     * List of fragments defining an isomorphic family to analyse.
     */
    private List<ClusterableFragment> sample;
    
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

    /**
     * Constructs a task that will analyze the given isomorphic family.
     * @param isomorphicFamily the fragments belonging to the isomorphic 
     * family to analyze. We do check for isomorphism and keep only the 
     * fragments that have an isomorphism with the first fragment in this list.
     * @param settings parameters controlling the job.
     * @throws SecurityException
     * @throws IOException
     */
    public ConformerExtractorTask(List<Vertex> isomorphicFamily, 
            FragmenterParameters settings) throws SecurityException, IOException
    {
        super(TaskUtils.getUniqueTaskIndex());
        if (isomorphicFamily.size()==0)
        {
            throw new Error("Attempt to create a " 
                    + this.getClass().getSimpleName() + " from empty list of "
                    + "fragments.");
        }
        this.isomorphicFamilyId = "undefinedIsoFamID";
        this.settings = settings;
        this.logger = settings.getLogger();
        
        List<ClusterableFragment> sample = new ArrayList<ClusterableFragment>();
        for (int i=0; i<isomorphicFamily.size(); i++)
        {
            Fragment frag = (Fragment) isomorphicFamily.get(i);
            populateListOfClusterizableFragments(sample, frag, logger, 
                    i + " ");
        }
        this.sample = sample;
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructs a task that will analyze the isomorphic family of the given 
     * fragment. We expect to find the rest of its family in filenames
     * conventionally names according to 
     * {@link FragmenterParameters#getMWSlotFileNameAllFrags(String)}.
     * @param oldChampions one of the fragments belonging to the isomorphic 
     * family to analyze. 
     * @param settings parameters controlling the job.
     * @throws SecurityException
     * @throws IOException
     */
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
        if (isoFamMembersFile!=null)
            sample = collectClusterableFragmentsFromFile();
        
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
        if (settings.isStandaloneFragmentClustering())
        {
            pathname = FragmenterTask.getResultsFileName(settings);
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
        List<ClusterableFragment> result;
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
        try 
        {
            result =  extractClusterableFragments(reader,
                    isomorphicFamilyId, logger);
        } finally {
            try
            {
                reader.close();
            } catch (IOException e)
            {
                throw new DENOPTIMException("Couls not close reader on file '"
                        + isoFamMembersFile + "'.",e);
            }
        }
        return result;
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
        while (reader.hasNext())
        {
            if (sample.size()==FragmenterParameters.MAXISOMORPHICSAMPLESIZE)
            {
                if (logger !=null)
                    logger.log(Level.INFO,"Sample reached the maximum size: "
                            + sample.size() 
                            + ". Ignoring any further fragment.");
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
                    if (logger !=null)
                        logger.log(Level.FINE,"Adding fragment " + molId 
                            + " " + molName + "to the sample of isomorphic "
                            + "family.");
                    frag = new Fragment(mol,BBType.UNDEFINED);
                } catch (DENOPTIMException e)
                {
                    if (logger !=null)
                        logger.log(Level.WARNING, "Skipping fragment " + molId 
                            + " " + molName + "because it could not "
                            + "be converted into a fragment.");
                    continue;
                }
            } else {
                if (logger !=null)
                    logger.log(Level.FINE, "Skipping fragment " + molId 
                        + " " + molName + "because it does not "
                        + "belong to isomorphic family '" + isomorphicFamilyId 
                        + "'.");
                continue;
            }
            
            populateListOfClusterizableFragments(sample, frag, logger, 
                    molId + " " + molName);
        }
        if (logger !=null)
            logger.log(Level.INFO, "Sample for " + isomorphicFamilyId 
                    + " contains " + sample.size() + " fragments.");
        return sample;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Tries to add a fragment into a sample of isomorphic fragments. Looks
     * for an isomorphism to define a consistent ordering of 
     * {@link FragIsomorphNode} that allows clustering of fragments.
     * @param sample the collection of isomorphic fragments. Can be empty, in 
     * which case, we just add the fragment into this list.
     * @param frag the fragment that we try to add to the sample.
     * @param logger where any log should be posted.
     * @param fragId a string identifying the fragment. Typically the index in 
     * a list of fragments.
     * @return <code>true</code> if the fragment has been added to the sample
     * of <code>false</code> if no isomorphism could be found and, therefore,
     * the fragment is not added to the sample.
     */
    public static boolean populateListOfClusterizableFragments(
            List<ClusterableFragment> sample, Fragment frag, Logger logger,
            String fragId)
    {
        // The clusterable fragments are fragment with a consistent
        // ordering of the atoms/APs list, so that such order can be used
        // to calculate RMSD between fragments.
        ClusterableFragment clusterable =  new ClusterableFragment(frag);
        if (sample.size()==0)
        {
            clusterable.setOrderOfNodes(
                    clusterable.getJGraphFragIsomorphism().vertexSet());
            sample.add(clusterable);
        } else {
            FragmentAlignement fa;
            try
            {
                fa = new FragmentAlignement(sample.get(0).getOriginalFragment(),
                        frag);
            } catch (DENOPTIMException e)
            {
                if (logger !=null)
                    logger.log(Level.WARNING, "Skipping fragment " + fragId 
                        + " because no "
                        + "isomorphism could be found with the first "
                        + "fragment in the sample.");
                return false;
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
        return true;
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
                + isomorphicFamilyId + "_mostCentralFrags.sdf";
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
