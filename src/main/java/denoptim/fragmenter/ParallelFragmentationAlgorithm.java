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
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.io.iterator.IteratingSMILESReader;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.FileFormat;
import denoptim.io.DenoptimIO;
import denoptim.io.IteratingAtomContainerReader;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.task.ParallelAsynchronousTaskExecutor;
import denoptim.utils.MoleculeUtils;


/**
 * Fragments a list of chemical systems by running parallel fragmentation tasks.
 *
 * @author Marco Foscato
 */

public class ParallelFragmentationAlgorithm extends ParallelAsynchronousTaskExecutor
{   
    
    /**
     * Collection of files with input
     */
    private File[] structures;
    
    /**
     * All settings controlling the tasks executed by this class.
     */
    private FragmenterParameters settings = null;

    
//-----------------------------------------------------------------------------

    /**
     * Constructor
     */

    public ParallelFragmentationAlgorithm(FragmenterParameters settings)
    {
        super(settings.getNumTasks(), settings.getLogger());
        this.settings = settings;
    }

//------------------------------------------------------------------------------

    protected boolean doPreFlightOperations()
    {
        IteratingAtomContainerReader reader;
        try
        {
            reader =  new IteratingAtomContainerReader
                    (new File(settings.getStructuresFile()));

        } catch (IOException | CDKException e1)
        {
            throw new Error("Error reading file '" + settings.getStructuresFile()
            + "'. " + e1.getMessage());
        }
        // Detect dimensionality of the molecules
        if (reader.getIteratorType().equals(IteratingSMILESReader.class))
        {
            settings.setWorkingIn3D(false);
        }
        // Split data in batches for parallelization
        
        // This is the collector of the mutating pathname to the file collecting
        // the input structures for each thread.
        structures = new File[settings.getNumTasks()];
        structures[0] = new File(settings.getStructuresFile());
        
        // WARNING while splitting the molecules we also do the preprocessing of
        // the molecules. This to avoid having to read them once again. Yet,
        // if we have no checks to be done, we are effectively copy-pasting
        // the file with the list of molecules to chop.
        splitInputForThreads(settings, reader);
        for (int i=0; i<settings.getNumTasks(); i++)
        {
            structures[i] = new File(getStructureFileNameBatch(settings, i));
        }
        return true;
    }
        
//------------------------------------------------------------------------------

    protected void createAndSubmitTasks()
    {
        for (int i=0; i<settings.getNumTasks(); i++)
        {
            FragmenterTask task;
            try
            {
                task = new FragmenterTask(structures[i], settings, i);
            } catch (SecurityException | IOException e)
            {
                throw new Error("Unable to start fragmentation thread.",e);
            }
            submitTask(task, task.getLogFilePathname());
        }
    }
    
//------------------------------------------------------------------------------

    protected boolean doPostFlightOperations()
    {
        // Identify (and possibly collect) final results. The files collecting
        // results change depending on the task we have done, and on whether
        // we ran them in a parallelized fashion or not.
        List<File> resultFiles = new ArrayList<File>();
        if (settings.doExtactRepresentativeConformer())
        {
            ParallelConformerExtractionAlgorithm extractor = 
                    new ParallelConformerExtractionAlgorithm(settings);
            try
            {
                extractor.run();
            } catch (Exception e)
            {
                throw new Error("Could not extract the most common conformer. "
                        + e.getMessage(), e);
            }
            for (String pathname : extractor.getResults())
            {
                resultFiles.add(new File(pathname));
            }
        } else {
            if (settings.doManageIsomorphicFamilies())
            {
                // We collect only the unique champion of each isomorphic family.
                resultFiles = getFilesCollectingIsomorphicFamilyChampions(
                        new File(settings.getWorkDirectory()));
            } else if (settings.getNumTasks()>1) {
                resultFiles = results.stream()
                        .map(obj -> (String) obj)
                        .map(pathname -> new File(pathname))
                        .collect(Collectors.toList());
            } else if (settings.getNumTasks()==1) {
                // We should be here only when we run on single thread with no
                // handling of isomorphic families (i.e., no removal of 
                // duplicates)
                resultFiles.add(new File ((String) results.get(0)));
            }
        }
        
        // In case we did not produce anything
        if (resultFiles.size()==0)
        {
            settings.getLogger().log(Level.INFO, "No results to collect. "
                    + "All done.");
            return true;
        }
        
        // If we did produce something, we go ahead
        File allFragsFile;
        FileFormat outputFormat = null;
        if (settings.doFragmentation())
        {
            allFragsFile = new File(FragmenterTask.getFragmentsFileName(
                    settings));
            outputFormat = DENOPTIMConstants.TMPFRAGFILEFORMAT;
        } else {
            allFragsFile = new File(FragmenterTask.getResultsFileName(
                    settings));
            outputFormat = FileFormat.MOLSDF;
        }
        
        switch (outputFormat)
        {
            case MOLSDF:
                try
                {
                    FileUtils.copyFile(resultFiles.get(0), allFragsFile);
                    if (resultFiles.size()>1)
                    {
                        DenoptimIO.appendTxtFiles(allFragsFile, 
                                resultFiles.subList(1,resultFiles.size()));
                    }
                } catch (IOException e)
                {
                    throw new Error("Unable to create new file '" 
                            + allFragsFile + "'",e);
                }
                break;
            
            case VRTXSDF:
                try
                {
                    FileUtils.copyFile(resultFiles.get(0), allFragsFile);
                    if (resultFiles.size()>1)
                    {
                        DenoptimIO.appendTxtFiles(allFragsFile, 
                                resultFiles.subList(1,resultFiles.size()));
                    }
                } catch (IOException e)
                {
                    throw new Error("Unable to create new file '" 
                            + allFragsFile + "'",e);
                }
                break;
                
            case VRTXJSON:
                //TODO
                // also check allFragsFile: it already contains extension.
                throw new Error("NOT IMPLEMENTED YET!");
                
            
                
            default:
                throw new Error("Unexpected format "
                        + DENOPTIMConstants.TMPFRAGFILEFORMAT + " for "
                        + "for final collection of fragments");
        }
        
        settings.getLogger().log(Level.INFO, "Results "
                + "collected in file " + allFragsFile);
        
        return true;
    }
    
//------------------------------------------------------------------------------
    
    protected static List<File> getFilesCollectingIsomorphicFamilyChampions(
            File workDir)
    {
        List<File> files = Arrays.stream(workDir.listFiles(new FileFilter(){
            @Override
            public boolean accept(File pathname) {
                if (pathname.getName().startsWith(
                        DENOPTIMConstants.MWSLOTFRAGSFILENAMEROOT)
                    && pathname.getName().contains(
                        DENOPTIMConstants.MWSLOTFRAGSUNQFILENANEEND))
                {
                    return true;
                }
                return false;
            }
        })).collect(Collectors.toList());
        files.sort(new Comparator<File>() {
    
            @Override
            public int compare(File o1, File o2)
            {
                // The filename is like "MWSlot_50-52_Unq.sdf"
                String s1 = o1.getName().replace(
                        DENOPTIMConstants.MWSLOTFRAGSFILENAMEROOT,"");
                int i1 = Integer.valueOf(s1.substring(0,s1.indexOf("-")));
                String s2 = o2.getName().replace(
                        DENOPTIMConstants.MWSLOTFRAGSFILENAMEROOT,"");
                int i2 = Integer.valueOf(s2.substring(0,s2.indexOf("-")));
                return Integer.compare(i1, i2);
            }
        
        });
        return files;
    }

//------------------------------------------------------------------------------
    
    /**
     * Splits the input data (from {@link FragmenterParameters}) into batches 
     * suitable for parallel batch processing. Since we have to read all the 
     * atom containers, we use this chance to do any preparation of the 
     * molecular representation (see 
     * {@link FragmenterTools#prepareMolToFragmentation(IAtomContainer, FragmenterParameters, int)}) 
     * and store the molecular formula in 
     * the property {@link DENOPTIMConstants#FORMULASTR}.
     * @throws DENOPTIMException
     * @throws FileNotFoundException
     */
    static void splitInputForThreads(FragmenterParameters settings, 
            IteratingAtomContainerReader reader)
    {
        int maxBuffersSize = 50000;
        int numBatches = settings.getNumTasks();
        
        //If available we record CSD formula in properties of atom container
        LinkedHashMap<String,String> formulae = settings.getFormulae();
        
        if (settings.doCheckFormula())
        {
            settings.getLogger().log(Level.INFO, "Combining structures and "
                + "formulae...");
        }
        int index = -1;
        int batchId = 0;
        int buffersSize = 0;
        boolean relyingOnListSize = false;
        List<ArrayList<IAtomContainer>> batches = 
                new ArrayList<ArrayList<IAtomContainer>>();
        for (int i=0; i<numBatches; i++)
        {
            batches.add(new ArrayList<IAtomContainer>());
        }
        try
        {
            while (reader.hasNext())
            {
                index++;
                buffersSize++;
                IAtomContainer mol = reader.next();
                
                // Adjust molecular representation to our settings
                if (!FragmenterTools.prepareMolToFragmentation(mol, settings, 
                        index))
                    continue;
                
                // It is convenient to place the formula in the atom container
                if (formulae!=null && settings.doCheckFormula())
                {
                    getFormulaForMol(mol, index, formulae);
                }
                
                batches.get(batchId).add(mol);
                
                // Update batch ID for next mol
                batchId++;
                if (batchId >= numBatches)
                    batchId = 0;
                
                // If max buffer size is reached, then bump to file
                if (buffersSize >= maxBuffersSize)
                {
                    buffersSize = 0;
                    for (int i=0; i<numBatches; i++)
                    {
                        String filename = getStructureFileNameBatch(settings, i);
                        try
                        {
                            DenoptimIO.writeSDFFile(filename, batches.get(i), true);
                        } catch (DENOPTIMException e)
                        {
                            throw new Error("Cannot write to '" + filename + "'.");
                        }
                        batches.get(i).clear();
                    }
                }
            }
        } finally {
            try {
                reader.close();
            } catch (IOException e1)
            {
                throw new Error("Could not close reader of SDF file '"
                        + settings.getStructuresFile() + "'",e1);
            }
        }
        
        if (buffersSize < maxBuffersSize)
        {
            for (int i=0; i<numBatches; i++)
            {
                String filename = getStructureFileNameBatch(settings, i);
                try
                {
                    DenoptimIO.writeSDFFile(filename, batches.get(i), true);
                } catch (DENOPTIMException e)
                {
                    throw new Error("Cannot write to '" + filename + "'.");
                }
                batches.get(i).clear();
            }
        }
        
        // Check for consistency in the list of formulae
        if (formulae!=null && relyingOnListSize 
                && index != (formulae.size()-1))
        {
            throw new Error("Inconsistent number of formulae "
                    + "(" + formulae.size() + ") "
                    + "and structures ("+ index + ") when using the index "
                    + "in the list of formulae as identifier. For your "
                    + "sake this in not allowed.");
        }
    }

//------------------------------------------------------------------------------

    /**
     * Builds the pathname of the structure file generated for one of the
     * parallel threads.
     * @param settings settings we work with.
     * @param i the index of the thread
     * @return the pathname
     */
    static String getStructureFileNameBatch(
            FragmenterParameters settings, int i)
    {
        return settings.getWorkDirectory() + DenoptimIO.FS 
                + "structuresBatch-" + i + ".sdf";
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Takes the molecular formula from the given list of formulae and 
     * using the 'Title' property of the index or the molecule. The formula 
     * taken from the list in argument is then placed among the properties
     * of the chemical object.
     */
    private static boolean getFormulaForMol(IAtomContainer mol, int index,
            LinkedHashMap<String,String> formulae)
    {
        boolean relyingOnListSize = false;

        List<String> formulaeList = new ArrayList<String>(formulae.values());
        
        String molName = mol.getTitle();
        if (molName!=null && !molName.isBlank())
        {
            if (formulae.containsKey(molName))
            {
                mol.setProperty(DENOPTIMConstants.FORMULASTR, 
                        formulae.get(molName));
            } else {
                relyingOnListSize = true;
                if (index<formulae.size())
                {
                    mol.setProperty(DENOPTIMConstants.FORMULASTR,
                            formulaeList.get(index));
                } else {
                    throw new Error("There are not "
                            + "enough formulae! Looking for "
                            + "formula #"+ index + " but there are "
                            + "only " + formulae.size() 
                            + "entries.");
                }
            }
        } else {
            relyingOnListSize = true;
            if (index<formulae.size())
            {
                mol.setProperty(DENOPTIMConstants.FORMULASTR,
                        formulaeList.get(index));
            } else {
                throw new Error("There are not "
                        + "enough formulae! Looking for "
                        + "formula #"+ index + " but there are "
                        + "only " + formulae.size() 
                        + "entries.");
            }
        }
        return relyingOnListSize;
    }

//------------------------------------------------------------------------------    

}
