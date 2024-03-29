/*
 *   DENOPTIM
 *   Copyright (C) 2021 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.logging;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;

/**
 * A collection of counters user to count actions taken by the evolutionary 
 * algorithm.
 * 
 * @author Marco Foscato
 */

public class Monitor extends HashMap<CounterID,AtomicInteger> 
{
    /**
     * Version UID
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * A name that allows humans to understand what this is a monitor of.
     */
    public String name = "noname";
    
    /**
     * Pathname to a file where to dump data
     */
    private String monitorFile = "unset.eamonitor";
    
    /**
     * Number of steps (i.e., attempts to make new candidates) after which we 
     * dump data to file, if required.
     */
    private int dumpStep = 50;
    
    /**
     * Flag requesting to dump the monitor data on file
     */
    private boolean dumpData = false;
    
    /**
     * A generation number
     */
    public int generationId = 0;
    
    /**
     * Counter controlling dumps
     */
    private int dumpsId = 0;
    
    /**
     * Logger to use
     */
    private Logger logger;
    
    private final String NL = System.getProperty("line.separator");

//------------------------------------------------------------------------------
    
    /**
     * Creates an unnamed monitor.
     */
    public Monitor()
    {
        super();
        for (CounterID cid : CounterID.values())
        {
            this.put(cid,new AtomicInteger());
        }
    }

//------------------------------------------------------------------------------
    
    /**
     * Creates a named monitor that is marked with the given generation number
     * @param identifier the string identifying this monitor.
     * @param genId the generation number.
     * @param monitorFile the pathname of the file where to print monitor dumps.
     */
    public Monitor(String identifier, int genId, String monitorFile, 
            int dumpStep, boolean dumpData, Logger logger)
    {
        this();
        name = identifier;
        generationId = genId;
        this.monitorFile = monitorFile;
        this.dumpStep = dumpStep;
        this.dumpData = dumpData;
        this.logger = logger;
    }
    
//------------------------------------------------------------------------------
    
    public void changeBy(CounterID cid, int value)
    {
        String dump = "";
        synchronized (this)
        {
            this.get(cid).addAndGet(value);
            if (cid == CounterID.NEWCANDIDATEATTEMPTS)
            {
                dumpsId++;
                if (dumpData && dumpsId >= dumpStep)
                {
                    dumpsId = 0;
                    dump = getMonitorDataLine("DUMP");
                }
            }
        }
        if (!dump.equals(""))
        {
            try
            {
                printSnapshot(dump);
            } catch (DENOPTIMException e)
            {
                logger.log(Level.WARNING,
                        "Unable to print monitor report: "+e.getMessage() + NL
                        + "Monitor report: " + NL + dump);
            }
        }
    }
    
//------------------------------------------------------------------------------
    
    public void increase(CounterID cid)
    {
        changeBy(cid,1);
    }
    
//------------------------------------------------------------------------------
    
    public void increaseBy(CounterID cid, int value)
    {
        changeBy(cid,value);
    }
    
//------------------------------------------------------------------------------
    
    public void decrease(CounterID cid)
    {
        changeBy(cid,-1);
    }
    
//------------------------------------------------------------------------------
    
    public void decreaseBy(CounterID cid, int value)
    {
        changeBy(cid,-value);
    }
    
//------------------------------------------------------------------------------

    public void printHeader(String pathName) throws DENOPTIMException
    {
        DenoptimIO.writeData(pathName, getMonitorDataHeader(), true);
    }
    
//------------------------------------------------------------------------------

    public void printSummary() throws DENOPTIMException
    {
        DenoptimIO.writeData(monitorFile, getMonitorDataLine("SUMMARY"), true);
    }
    
//------------------------------------------------------------------------------

    public void printSnapshot(String snapshot) throws DENOPTIMException
    {
        DenoptimIO.writeData(monitorFile, snapshot, true);
    }

//------------------------------------------------------------------------------

    /**
     * Build a string with the names of all counters.
     * @return
     */
    private String getMonitorDataHeader()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("RecordType MonitorName Generation ");
        synchronized (this)
            {
            for (CounterID cid : CounterID.values())
            {
                sb.append(cid).append(" ");
            }
        }
        return sb.toString();
    }
    
//------------------------------------------------------------------------------

    /**
     * Build a string with the value of all counters.
     * @param prefix
     * @return
     */
    private String getMonitorDataLine(String prefix)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append(" ");
        sb.append(name).append(" ");
        sb.append(generationId).append(" ");
        synchronized (this)
            {
            for (CounterID cid : CounterID.values())
            {
                sb.append(this.get(cid).get()).append(" ");
            }
        }
        return sb.toString();
    }
    
//------------------------------------------------------------------------------
        
}
