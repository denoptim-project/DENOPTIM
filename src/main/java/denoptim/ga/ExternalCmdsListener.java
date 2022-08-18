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

package denoptim.ga;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.exception.ExceptionUtils;
import denoptim.io.DenoptimIO;
import denoptim.logging.StaticLogger;
import denoptim.programs.denovo.GAParameters;

/**
 * Service that watches the <code>interface</code> folder (i.e., see 
 * {@link GAParameters#interfaceDir})
 * for instructions coming from outside the JVM. The instructions are given as
 * files in the <code>interface</code> folder.
 * 
 * @author Marco Foscato
 */
public class ExternalCmdsListener implements Runnable
{

	private final Path pathname;
    private final WatchService watcher;
    private final WatchKey key;
    private EvolutionaryAlgorithm ea;
    
    /**
     * Flag signaling that we are intentionally interrupting the service.
     */
    private boolean intendedStop = false; 
    
    private final String NL = System.getProperty("line.separator");
    
    /**
     * Program-specific logger
     */
    private Logger logger = null;
    
//------------------------------------------------------------------------------

    public ExternalCmdsListener(Path pathname, Logger logger) throws IOException 
    {
    	this.pathname = pathname;
    	this.logger = logger;
        logger.log(Level.INFO, "Watching pathname '" + pathname + "' "
                + "for live instructions.");
        this.watcher = FileSystems.getDefault().newWatchService();
        this.key = pathname.register(watcher, ENTRY_CREATE);
    }
    
//------------------------------------------------------------------------------

    /**
     * Starts listening for events and keeps listening in its own thread
     */
    public void run() 
    {
        try {
			for (;;) 
			{
			    // this will wait for an event (pauses the thread)
			    WatchKey key = watcher.take();
			    if (this.key != key)
			    {
			    	// The event is something else than what expected
			    	continue;
			    }
			    for (WatchEvent<?> event : key.pollEvents()) 
			    {
			        WatchEvent.Kind<?> kind = event.kind();

			        // There seems to be a possibility for nasty events that need 
			        // to be ignored.
			        if (kind == OVERFLOW) {
			            continue;
			        }

					WatchEvent<Path> ev = null;
					try {
						ev = (WatchEvent<Path>) event;
					} catch (Throwable t) {
						// The event is something else than what expected
				    	continue;
					}
			        Path name = ev.context();
			        Path child = pathname.resolve(name);
			        processExternalCmdFile(child.toFile());
			    }

			    // Remove key from the queue of signalled events
			    if (!key.reset())
			    {
			    	break;
			    }
			}
		} catch (InterruptedException e) {
			return;
		} catch (Throwable t) {
		    if (!intendedStop)
		    {
    		    String msg = this.getClass().getSimpleName() + " stopped! From "
    		            + "now on, we cannot listen to commands from the "
    		            + "interface." + DENOPTIMConstants.EOL + " Cause: " 
    		            + ExceptionUtils.getStackTraceAsString(t);
    		    if (logger!=null)
    		    {
    		        logger.log(Level.SEVERE, msg);
    		        t.printStackTrace();
    		    } else {
    		        StaticLogger.appLogger.log(Level.SEVERE, msg);
    		    }
		    }
		}
    }
    
//------------------------------------------------------------------------------
    
	private void processExternalCmdFile(File file) 
	{
		ArrayList<String> lines = null;
		try {
			lines = DenoptimIO.readList(file.getAbsolutePath(), true);
		} catch (DENOPTIMException e) {
		    logger.log(Level.WARNING, "Unable to read file '"  
	        		+ file.getAbsolutePath() + "'. Any instruction contained "
    		        + "in that file is ignored. Hint: " + e.getMessage() + NL);
			e.printStackTrace();
			return;
		}

		if (lines.size() == 0)
		{
			// Empty file is probably a sign that the file is being written
			logger.log(Level.WARNING, "Empty instructions in '"  
		    		+ file.getAbsolutePath() + "'.");
		}
		
		for (String line : lines)
		{
			if (line.startsWith("STOP_GA"))
			{
			    logger.log(Level.SEVERE, "GA run will be "
			    		+ "stopped upon external request from '"  
		        		+ file.getAbsolutePath() + "'." + NL);
				if (ea != null)
				{
					ea.stopRun();
				}
			}
		
	        if (line.startsWith("REMOVE_CANDIDATE"))
            {
	            String candIDs = line.substring(
	                    "REMOVE_CANDIDATE".length()).trim();
	            logger.log(Level.SEVERE, "Removing '"
	                    + candIDs + "' upon external request from '"  
                        + file.getAbsolutePath() + "'." + NL);
                String[] parts = candIDs.split("\\s+");
                Set<String> candNames = new HashSet<String>(
                        Arrays.asList(parts));
                if (ea != null)
                {   
                    ea.removeCandidates(candNames);
                }
            }
	        
	        if (line.startsWith("ADD_CANDIDATE"))
            {
                String fileNamesLst = line.substring(
                        "ADD_CANDIDATE".length()).trim();
                logger.log(Level.SEVERE, "Adding candidates from '"
                        + fileNamesLst + "' upon external request from '"  
                        + file.getAbsolutePath() + "'." + NL);
                String[] parts = fileNamesLst.split("\\s+");
                Set<String> paths = new HashSet<String>(Arrays.asList(parts));
                if (ea != null)
                {   
                    ea.addCandidates(paths);
                }
            }
		}
	}

//------------------------------------------------------------------------------
    
    public void closeWatcher() throws IOException
    {
        intendedStop = true;
    	this.watcher.close();
    }
    
//------------------------------------------------------------------------------
    
    public void setReferenceToRunningEAlgorithm(EvolutionaryAlgorithm ea)
    {
    	this.ea = ea;
    }
    
//------------------------------------------------------------------------------
    
}