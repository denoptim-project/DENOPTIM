/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.io.iterator.IteratingSDFReader;

import denoptim.combinatorial.GraphBuildingTask;
import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.FileFormat;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.fragspace.FragsCombination;
import denoptim.fragspace.FragsCombinationIterator;
import denoptim.fragspace.IdFragmentAndAP;
import denoptim.graph.DGraph;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.io.DenoptimIO;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.programs.combinatorial.CEBLParameters;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.utils.GraphUtils;
import denoptim.utils.MoleculeUtils;
import edu.uci.ics.jung.visualization.spatial.FastRenderingGraph;


/**
 * fragments a list of chemical systems by running parallel fragmentation tasks.
 *
 * @author Marco Foscato
 */

public class ParallelFragmentationAlgorithm
{
    /**
     * Storage of references to the submitted subtasks as <code>Future</code>
     */
    final List<Future<Object>> futures;

    /**
     * Storage of references to the submitted subtasks.
     */
    final ArrayList<FragmenterTask> submitted;

    /**
     * Asynchronous tasks manager 
     */
    final ThreadPoolExecutor tpe;

    /**
     * If any, here we stores the exception returned by a subtask
     */
    private Throwable thrownByTask;
    
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
        this.settings = settings;
        futures = new ArrayList<>();
        submitted = new ArrayList<>();

        tpe = new ThreadPoolExecutor(settings.getNumTasks(),
                settings.getNumTasks(),
                Long.MAX_VALUE,
                TimeUnit.NANOSECONDS,
                new ArrayBlockingQueue<Runnable>(1));

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                tpe.shutdown(); // Disable new tasks from being submitted
                try
                {
                    // Wait a while for existing tasks to terminate
                    if (!tpe.awaitTermination(30, TimeUnit.SECONDS))
                    {
                        tpe.shutdownNow(); // Cancel currently executing tasks
                    }

                    if (!tpe.awaitTermination(60, TimeUnit.SECONDS))
                    {
                        // pool didn't terminate after the second try
                    }
                }
                catch (InterruptedException ie)
                {
                    cleanup(tpe, futures, submitted);
                    // (Re-)Cancel if current thread also interrupted
                    tpe.shutdownNow();
                    // Preserve interrupt status
                    Thread.currentThread().interrupt();
                }
            }
        });

        // by default the ThreadPoolExecutor will throw an exception
        tpe.setRejectedExecutionHandler(new RejectedExecutionHandler()
        {
            @Override
            public void rejectedExecution(Runnable r, 
                    ThreadPoolExecutor executor)
            {
                try
                {
                    // this will block if the queue is full
                    executor.getQueue().put(r);
                }
                catch (InterruptedException ex)
                {
                    ex.printStackTrace();
                    String msg = "EXCEPTION in rejectedExecution.";
                    settings.getLogger().log(Level.WARNING,msg);
                }
            }
        });
    }

//------------------------------------------------------------------------------

    /**
     * Stops all subtasks and shutdown executor
     */

    public void stopRun()
    {
        cleanup(tpe, futures, submitted);
        tpe.shutdown();
    }

//------------------------------------------------------------------------------

    /**
     * Looks for exceptions in the subtasks and, if any, store its reference
     * locally to allow reporting it back from the main thread.
     * @return <code>true</code> if any of the subtasks has thrown an exception
     */

  //TODO-gg del
    private boolean subtaskHasException()
    {
        boolean hasException = false;
        for (FragmenterTask tsk : submitted)
        {
            if (tsk.foundException())
            {
                hasException = true;
                thrownByTask = tsk.getException();
                break;
            }
        }

        return hasException;
    }

//------------------------------------------------------------------------------

    /**
     * Check for completion of all subtasks
     * @return <code>true</code> if all subtasks are completed
     */
//TODO-gg del
    private boolean allTasksCompleted()
    {
        boolean allDone = true;
        for (FragmenterTask tsk : submitted)
        {
            if (!tsk.isCompleted())
            {
                allDone = false;
                break;
            }
        }

        return allDone;
    }

//------------------------------------------------------------------------------

    /**
     * Run the parallelized task. 
     */

    public void run() throws DENOPTIMException, IOException
    {
        String msg = "";
        StopWatch watch = new StopWatch();
        watch.start();
        
        // Split data in batches for parallelization
        
        // This is the collector of the mutating pathname to the file collecting
        // the input structures for each thread.
        File[] structures = new File[settings.getNumTasks()];
        structures[0] = new File(settings.getStructuresFile());
        if (settings.getNumTasks()>1 || settings.doCheckFormula())
        {
            settings.getLogger().log(Level.INFO, "Combining structures and "
                    + "formulas...");
            splitInputForThreads(settings);
            for (int i=0; i<settings.getNumTasks(); i++)
            {
                structures[i] = new File(getStructureFileNameBatch(settings, i));
            }
        }
        
        // Start the parallel threads
        tpe.prestartAllCoreThreads();
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
            submitted.add(task);
            futures.add(tpe.submit(task));
            settings.getLogger().log(Level.INFO, "Fragmenter task "
                    + i + " submitted. Log file: " + task.getLogFilePathname());
        }
        
        // This sounds weird when reading the doc, but the following does wait
        // for the threads to complete.
        
        // shutdown thread pool
        tpe.shutdown();
        try
        {
            // wait a bit for pending tasks to finish
            while (!tpe.awaitTermination(5, TimeUnit.SECONDS))
            {
                // do nothing
            }
        }
        catch (InterruptedException ex)
        {
            //Do nothing
        }
        
        if (settings.doManageIsomorphicFamilies())
        {
            // Collect MW-split fragments in one basket
            // We collect only the unique champion of each isomorphic family.
            File workDir = new File(settings.getWorkDirectory());
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
                    // The filename is like "Fragments-MWSlot_50-52_Unq.sdf"
                    String s1 = o1.getName().replace(
                            DENOPTIMConstants.MWSLOTFRAGSFILENAMEROOT,"");
                    int i1 = Integer.valueOf(s1.substring(0,s1.indexOf("-")));
                    String s2 = o2.getName().replace(
                            DENOPTIMConstants.MWSLOTFRAGSFILENAMEROOT,"");
                    int i2 = Integer.valueOf(s2.substring(0,s2.indexOf("-")));
                    return Integer.compare(i1, i2);
                }
            
            });
            File allFragsFile = new File(FragmenterTask.getFragmentsFileName(
                    settings));
            switch (DENOPTIMConstants.TMPFRAGFILEFORMAT)
            {
                case VRTXSDF:
                    FileUtils.copyFile(files.get(0), allFragsFile);
                    DenoptimIO.appendTxtFiles(allFragsFile, 
                            files.subList(1,files.size()));
                    break;
                    
                case VRTXJSON:
                    //TODO
                    // also check allFragsFile: it already contains extension.
                    throw new DENOPTIMException("NOT IMPLEMENTED YET!");
                    
                default:
                    throw new DENOPTIMException("Unexpected format "
                            + DENOPTIMConstants.TMPFRAGFILEFORMAT + " for "
                            + "for final collection of fragments");
            }
            settings.getLogger().log(Level.INFO, "Unique fragments "
                    + "collected in file " + allFragsFile);
        } else if (settings.getNumTasks()>1) {
            List<File> files = submitted.stream()
                    .map(FragmenterTask::getResultFile)
                    .map(pathname -> new File(pathname))
                    .collect(Collectors.toList());
            File allFragsFile;
            if (settings.doFragmentation())
            {
                allFragsFile = new File(FragmenterTask.getFragmentsFileName(
                        settings));
            } else {
                allFragsFile = new File(FragmenterTask.getResultsFileName(
                        settings));
            }
            switch (DENOPTIMConstants.TMPFRAGFILEFORMAT)
            {
                case VRTXSDF:
                    FileUtils.copyFile(files.get(0), allFragsFile);
                    DenoptimIO.appendTxtFiles(allFragsFile, 
                            files.subList(1,files.size()));
                    break;
                    
                case VRTXJSON:
                    //TODO
                    // also check allFragsFile: it already contains extension.
                    throw new DENOPTIMException("NOT IMPLEMENTED YET!");
                    
                default:
                    throw new DENOPTIMException("Unexpected format "
                            + DENOPTIMConstants.TMPFRAGFILEFORMAT + " for "
                            + "for final collection of fragments");
            }
            settings.getLogger().log(Level.INFO, "Results "
                    + "collected in file " + allFragsFile);
            
        } else if (settings.getNumTasks()==1) {
            // We should be here only when we run on single thread with no
            // handling of isomorphic families (i.e., no removal of duplicates)
            settings.getLogger().log(Level.INFO, "Results "
                    + "in file " + submitted.get(0).results);
        }        
        
        //TODO-GG deal with exceptions generated by tasks (e.g., when adding dummy on linearities)
        
        // closing messages
        watch.stop();
        msg = "Overall time: " + watch.toString() + ". " 
            + DENOPTIMConstants.EOL
            + "ParallelFragemtationAlgorithm run completed." 
            + DENOPTIMConstants.EOL;
        settings.getLogger().log(Level.INFO, msg);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Splits the input data (from {@link FragmenterParameters}) into batches 
     * suitable for parallel batch processing. Since we have to read all the 
     * atom containers, we use this chance to store the molecular formula in 
     * the property {@link DENOPTIMConstants#FORMULASTR}.
     * @throws DENOPTIMException
     * @throws FileNotFoundException
     */
    static void splitInputForThreads(FragmenterParameters settings) 
            throws DENOPTIMException, FileNotFoundException
    {
        int maxBuffersSize = 50000;
        int numBatches = settings.getNumTasks();
        
        IteratingSDFReader reader = new IteratingSDFReader(
                new FileInputStream(settings.getStructuresFile()), 
                DefaultChemObjectBuilder.getInstance());
        
        //If available we record CSD formula in properties of atom container
        LinkedHashMap<String,String> formulae = settings.getFormulae();
        
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
        while (reader.hasNext())
        {
            index++;
            buffersSize++;
            IAtomContainer mol = reader.next();
            
            // Comply to requirements to write SDF files: unset bond orders 
            // can only be used in query-type files. So types 4 and 8 are not
            // expected to be found (but CSD uses them...)
            try
            {
                MoleculeUtils.setZeroImplicitHydrogensToAllAtoms(mol);
                MoleculeUtils.ensureNoUnsetBondOrders(mol);
            } catch (CDKException e)
            {
                if (!settings.acceptUnsetToSingeBO())
                {
                    settings.getLogger().log(Level.WARNING,"Some bond order are "
                            + "unset and attempt to kekulize the system has failed "
                            + "for structure " + index + "."
                            + "This hampers use of SMARTS queries, which may very "
                            + "not work as expected. Structure " + index + " will "
                            + "be rejected. You can avoid rejection by using "
                            + "keyword " 
                            + ParametersType.FRG_PARAMS.getKeywordRoot() 
                            + "UNSETTOSINGLEBO, but you'll "
                            + "still be using a peculiar connectivity table were"
                            + "many bonds are artificially markes as single to "
                            + "avoid use of 'UNSET' bond order. "
                            + "Further details on the problem: " + e.getMessage());
                    continue;
                } else {
                    settings.getLogger().log(Level.WARNING,"Failed kekulization "
                            + "for structure " + index + " but UNSETTOSINGLEBO "
                            + "keyword used. Forcing use of single bonds to "
                            + "replace bonds with unset order.");
                    for (IBond bnd : mol.bonds())
                    {
                        if (bnd.getOrder().equals(IBond.Order.UNSET)) 
                        {
                            bnd.setOrder(IBond.Order.SINGLE);
                        }
                    }
                }
            }
            
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
                    DenoptimIO.writeSDFFile(
                            getStructureFileNameBatch(settings, i),
                            batches.get(i), true);
                    batches.get(i).clear();
                }
            }
        }
        if (buffersSize < maxBuffersSize)
        {
            for (int i=0; i<numBatches; i++)
            {
                DenoptimIO.writeSDFFile(
                        getStructureFileNameBatch(settings, i),
                        batches.get(i), true);
                batches.get(i).clear();
            }
        }
        
        // Check for consistency in the list of formulae
        if (formulae!=null && relyingOnListSize 
                && index != (formulae.size()-1))
        {
            throw new DENOPTIMException("Inconsistent number of formulae "
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
            LinkedHashMap<String,String> formulae) throws DENOPTIMException
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
                    throw new DENOPTIMException("There are not "
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
                throw new DENOPTIMException("There are not "
                        + "enough formulae! Looking for "
                        + "formula #"+ index + " but there are "
                        + "only " + formulae.size() 
                        + "entries.");
            }
        }
        return relyingOnListSize;
    }
    
//------------------------------------------------------------------------------

    /**
     * clean all reference to submitted tasks
     */

    private void cleanup(ThreadPoolExecutor tpe, List<Future<Object>> futures,
                            ArrayList<FragmenterTask> submitted)
    {
        for (Future<Object> f : futures)
        {
            f.cancel(true);
        }

        for (FragmenterTask tsk: submitted)
        {
            tsk.stopTask();
        }

        submitted.clear();

        tpe.getQueue().clear();
    }

//------------------------------------------------------------------------------    

}
