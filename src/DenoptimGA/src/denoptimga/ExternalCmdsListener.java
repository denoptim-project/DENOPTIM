package denoptimga;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;

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
    private ParallelEvolutionaryAlgorithm pea;
    
//------------------------------------------------------------------------------

    public ExternalCmdsListener(Path pathname) throws IOException 
    {
    	this.pathname = pathname;
        this.watcher = FileSystems.getDefault().newWatchService();
        this.key = pathname.register(watcher, ENTRY_CREATE);
        DENOPTIMLogger.appLogger.log(Level.INFO, "Watching pathname '" +  
        		pathname + "' for live instructions.");
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
			        WatchEvent.Kind kind = event.kind();

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
		}
    }
    
//------------------------------------------------------------------------------
    
	private void processExternalCmdFile(File file) 
	{
		ArrayList<String> lines = null;
		try {
			lines = DenoptimIO.readList(file.getAbsolutePath());
		} catch (DENOPTIMException e) {
			DENOPTIMLogger.appLogger.log(Level.WARNING, "Unable to read file '"  
	        		+ file.getAbsolutePath() + "'. Live instructions lost.");
		}

		if (lines.size() == 0)
		{
			// Empty file is probably a sign that the file is being written
			DENOPTIMLogger.appLogger.log(Level.WARNING, "Empty instructions in '"  
	        		+ file.getAbsolutePath() + "'.");
		}
		
		for (String line : lines)
		{
			if (line.startsWith("STOP_GA"))
			{
				//TODO-M del
				System.out.println("STIPPING request from file "+file);
				DENOPTIMLogger.appLogger.log(Level.SEVERE, "GA run will be "
						+ "stopped upon external request from '"  
		        		+ file.getAbsolutePath() + "'.");
				if (ea != null)
				{
					ea.stopRun();
				}	
				if (pea != null)
				{
					pea.stopRun();
				}
			}
			//TODO: add removal on one population member and its downstream relatives
		}
	}

//------------------------------------------------------------------------------
    
    public void closeWatcher() throws IOException
    {
    	this.watcher.close();
    }
    
//------------------------------------------------------------------------------
    //TODO, make interface for algorithms or eait untim parallel and normal evolutionaty algorithms have been merged
    public void setReferenceToRunningAlgorithm(ParallelEvolutionaryAlgorithm pea)
    {
    	this.pea = pea;
    }
    
//------------------------------------------------------------------------------
    
    public void setReferenceToRunningEAlgorithm(EvolutionaryAlgorithm ea)
    {
    	this.ea = ea;
    }
    
//------------------------------------------------------------------------------
    
}