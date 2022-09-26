/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
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

package denoptim.programs.denovo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import denoptim.ga.EvolutionaryAlgorithm;
import denoptim.ga.ExternalCmdsListener;
import denoptim.task.ProgramTask;

/**
 * Programs that runs de novo design by a genetic algorithm.
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class GARunner extends ProgramTask
{
    /**
     * The implementation of the evolutionary algorithm we run here.
     */
    private EvolutionaryAlgorithm ea = null;

    /**
     * The service that listens for commands from outside the JVM.
     */
    private ExternalCmdsListener ecl = null;

    /**
     * Executor of the service that listens for commands.
     */
    private ExecutorService executor = null;

    /**
     * Pending tasks of the service listening for commands.
     */
    private Future<?> futureWatchers = null;


//------------------------------------------------------------------------------

    /**
     * Creates and configures the program task.
     * @param configFile the file containing the configuration parameters.
     * @param workDir the file system location from which to run the program.
     */
    public GARunner(File configFile, File workDir)
    {
        super(configFile,workDir);
    }

//------------------------------------------------------------------------------

    @Override
    public void runProgramma() throws Throwable
    {
        GAParameters settings = new GAParameters();
        if (workDir != null)
        {
            settings.setWorkingDirectory(workDir.getAbsolutePath());
        }

        settings.readParameterFile(configFilePathName.getAbsolutePath());
        settings.checkParameters();
        settings.processParameters();
        settings.startProgramSpecificLogger(loggerIdentifier);
        settings.printParameters();

        ecl = new ExternalCmdsListener(Paths.get(settings.getInterfaceDir()),
                settings.getLogger());
        executor = Executors.newSingleThreadExecutor();
        futureWatchers = executor.submit(ecl);
        executor.shutdown();

        ea = new EvolutionaryAlgorithm(settings, ecl);
        ea.run();

        stopExternalCmdListener();
        stopLogger();
    }

//------------------------------------------------------------------------------

    protected void handleThrowable()
    {
        if (ea != null)
        {
            ea.stopRun();
        }
        stopExternalCmdListener();
        super.handleThrowable();
    }

//------------------------------------------------------------------------------

    /**
     * Stop the service that waits for instructions from the outside world.
     * @param ecl
     * @param executor
     * @param futureWatchers
     */
	private void stopExternalCmdListener()
	{
        if (executor != null)
        {
            try {
				executor.awaitTermination(2, TimeUnit.SECONDS);
				ecl.closeWatcher();
                futureWatchers.cancel(true);
                executor.awaitTermination(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				// we'll kill it anyway
			} catch (IOException e) {
				// we'll kill it anyway
			}
            executor.shutdownNow();
            executor = null;
        }
	}

//------------------------------------------------------------------------------

}
