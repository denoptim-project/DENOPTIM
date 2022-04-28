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

package denoptim.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 *
 * @author Vishwesh Venkatraman
 */
public class HTMLLogFormatter extends Formatter
{
    
//------------------------------------------------------------------------------    
    
    // This method is called for every log records
    @Override
    public String format(LogRecord rec)
    {
        StringBuilder buf = new StringBuilder(1000);
        // Bold any levels >= WARNING
        buf.append("<tr>");
        buf.append("<td>");

        if (rec.getLevel().intValue() >= Level.WARNING.intValue())
        {
            buf.append("<b>");
            buf.append(rec.getLevel());
            buf.append("</b>");
        } else
        {
            buf.append(rec.getLevel());
        }
        buf.append("</td>");
        buf.append("<td>");
        buf.append(calcDate(rec.getMillis()));
        buf.append(' ');
        buf.append(formatMessage(rec));
        buf.append('\n');
        buf.append("<td>");
        buf.append("</tr>\n");
        return buf.toString();
    }

//------------------------------------------------------------------------------
    
    private String calcDate(long millisecs)
    {
        SimpleDateFormat date_format = new SimpleDateFormat("MMM dd,yyyy HH:mm");
        Date resultdate = new Date(millisecs);
        return date_format.format(resultdate);
    }
    
//------------------------------------------------------------------------------    

    // This method is called just after the handler using this
    // formatter is created
    @Override
    public String getHead(Handler h)
    {
            return "<HTML>\n<HEAD>\n" + (new Date()) + "\n</HEAD>\n<BODY>\n<PRE>\n"
                            + "<table border>\n  "
                            + "<tr><th>Time</th><th>Log Message</th></tr>\n";
    }
    
//------------------------------------------------------------------------------    

    // This method is called just after the handler using this
    // formatter is closed
    @Override
    public String getTail(Handler h)
    {
            return "</table>\n  </PRE></BODY>\n</HTML>\n";
    }
    
//------------------------------------------------------------------------------    
    
}
