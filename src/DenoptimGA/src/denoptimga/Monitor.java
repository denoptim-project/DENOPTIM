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

package denoptimga;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;

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
     * A generation number
     */
    public int generationId = 0;
    
    /**
     * Counter controlling dumps
     */
    private int dumpsId = 0;
    
    private final String NL = System.getProperty("line.separator");

//------------------------------------------------------------------------------
    
    public Monitor()
    {
        super();
        for (CounterID cid : CounterID.values())
        {
            this.put(cid,new AtomicInteger());
        }
    }

//------------------------------------------------------------------------------
    
    public Monitor(String identifier, int genId)
    {
        this();
        name = identifier;
        generationId = genId;
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
                if (dumpsId >= GAParameters.getMonitorDumpStep()
                        && GAParameters.dumpMonitor)
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
                DENOPTIMLogger.appLogger.log(Level.WARNING,
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

    public void printHeader() throws DENOPTIMException
    {
        DenoptimIO.writeData(GAParameters.getMonitorFile(),
                getMonitorDataHeader(),true);
    }
    
//------------------------------------------------------------------------------

    public void printSummary() throws DENOPTIMException
    {
        DenoptimIO.writeData(GAParameters.getMonitorFile(),
                getMonitorDataLine("SUMMARY"),true);
    }
    
//------------------------------------------------------------------------------

    public void printSnapshot(String snapshot) throws DENOPTIMException
    {
        DenoptimIO.writeData(GAParameters.getMonitorFile(),snapshot,true);
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
