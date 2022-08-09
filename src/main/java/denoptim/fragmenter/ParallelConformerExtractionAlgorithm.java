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
import denoptim.files.UndetectedFileFormatException;
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
import denoptim.task.ParallelAsynchronousTaskExecutor;
import denoptim.task.Task;
import denoptim.utils.GraphUtils;
import denoptim.utils.MoleculeUtils;
import edu.uci.ics.jung.visualization.spatial.FastRenderingGraph;


/**
 * Runs threads that extract the most representative conformer of fragments
 * given as input.
 * 
 * @author Marco Foscato
 */

public class ParallelConformerExtractionAlgorithm extends ParallelAsynchronousTaskExecutor
{
    
    /**
     * All settings controlling the tasks executed by this class.
     */
    private FragmenterParameters settings = null;
    
    /**
     * List of pathnames collecting the most representative conformers, 
     * as defined
     * by the settings (i.e., either cluster centroids or fragment closest to
     * the centroids).
     */
    protected List<String> results = new ArrayList<String>();

    
//-----------------------------------------------------------------------------

    /**
     * Constructor. We expect to run this from 
     * {@link ParallelFragmentationAlgorithm}.
     */
    public ParallelConformerExtractionAlgorithm(FragmenterParameters settings)
    {
        super(settings.getNumTasks(), settings.getLogger());
        this.settings = settings;
    }

//------------------------------------------------------------------------------

    protected boolean doPreFlightOperations()
    {
        return true;
    }
        
//------------------------------------------------------------------------------

    protected void createAndSubmitTasks()
    {
        // Looping over the current champions. These are just the first fragment 
        // found for each isomorphic family's sample.
        for (File mwSlotFile : ParallelFragmentationAlgorithm
                .getFilesCollectingIsomorphicFamilyChampions(new File(
                        settings.getWorkDirectory())))
        {
            List<Vertex> oldChampions;
            try
            {
                oldChampions = DenoptimIO.readVertexes(mwSlotFile,
                        BBType.UNDEFINED);
            } catch (Throwable e)
            {
                
                throw new Error("Unable to extract representative "
                        + "conformations. Problems opening file '"
                        + mwSlotFile + "'.", e);
            }
            for (Vertex oldChampion : oldChampions)
            {
                ConformerExtractorTask task;
                try
                {
                    task = new ConformerExtractorTask(oldChampion, settings);
                } catch (SecurityException | IOException e)
                {
                    throw new Error("Unable to start fragmentation thread.",e);
                }
                submitTask(task, task.getLogFilePathname());
            }
        }
    }

//------------------------------------------------------------------------------

    protected boolean doPostFlightOperations()
    {
        for (Task t : submitted)
        {
            results.add(((ConformerExtractorTask)t).getResultFile());
        }
        return true;
    }
 
//------------------------------------------------------------------------------    

}
