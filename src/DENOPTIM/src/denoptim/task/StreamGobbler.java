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

package denoptim.task;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * See http://www.javaworld.com/jw-12-2000/jw-1229-traps.html?page=4
 * @author Vishwesh Venkatraman
 */
class StreamGobbler extends Thread
{
    InputStream is;
    String type;
    StringBuilder sb;
    
//------------------------------------------------------------------------------    
    
    StreamGobbler(InputStream is, String type)
    {
        this.is = is;
        this.type = type;
        this.sb = new StringBuilder();
    }
    
//------------------------------------------------------------------------------    
    
    public String getMessages()
    {
        return sb.toString();
    }
    
//------------------------------------------------------------------------------    
    
    @Override
    public void run()
    {
        try
        {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null)
                sb.append(type).append("> ").append(line).append("\n");

            br.close();
        }
        catch (IOException ioe)
        {
        }
    }

//------------------------------------------------------------------------------
}
