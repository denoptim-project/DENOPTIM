/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no> and
 *   Marco Foscato <marco.foscato@uib.no>
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

package denoptimcg;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import denoptim.exception.DENOPTIMException;


/**
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class CGUtils 
{
    private static String lsep = System.getProperty("line.separator");
    
    
//------------------------------------------------------------------------------

    /**
     * Read the parameter settings to be used by PSSROT
     *
     * @param filename
     * @param data content of file (lines)
     * @throws DENOPTIMException
     */
    public static void readKeyFileParams(String filename, ArrayList<String> data)
            throws DENOPTIMException
    {
        BufferedReader br = null;
        String line;

        try
        {
            br = new BufferedReader(new FileReader(filename));
            while ((line = br.readLine()) != null)
            {
                line = line.trim();
                data.add(line);
            }
        }
        catch (IOException nfe)
        {
            throw new DENOPTIMException(nfe);
        }
        finally
        {
            try
            {
                if (br != null)
                {
                    br.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }

        if (data.isEmpty())
        {
            String msg = "No data found in file: " + filename;
            throw new DENOPTIMException(msg);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Count the number of lines starting with a keyword
     *
     * @param filename
     * @param keyword
     * @return number of lines in the file
     * @throws DENOPTIMException
     */
    public static int countLinesWKeywordInFile(String filename, String keyword)
                                 throws DENOPTIMException
    {

        BufferedReader br = null;
        String line;

        try
        {
            int count = 0;
            br = new BufferedReader(new FileReader(filename));

            while ((line = br.readLine()) != null)
            {
                if (line.startsWith(keyword))
                {
                    count++;
                }
            }
            return count;
        }
        catch (IOException ioe)
        {
            throw new DENOPTIMException(ioe);
        }
        finally
        {
            try
            {
                if (br != null)
                {
                    br.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }
    }   

//------------------------------------------------------------------------------   
}
