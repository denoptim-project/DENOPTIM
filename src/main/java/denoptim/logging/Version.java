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

import java.io.IOException;
import java.util.Properties;

import denoptim.constants.DENOPTIMConstants;
import denoptim.main.Main;

/**
 * Class handling DENOPTIM's version identifier for headers. Note that the 
 * numerical version ID is written in the form 
 * v&lt;major&gt;.&lt;minor&gt;.&lt;update&gt;
 *  (e.g. v0.1.1) and is defined in the pol.xml
 * 
 * @author Vishwesh Venkatraman 
 */
public class Version
{
    public static final String NAME = "DENOPTIM";
    public static final String AUTHORS = "Vishwesh Venkatraman, Marco Foscato";
    public static final String CONTRIBUTORS = "David Grellscheid, "
            + "Einar Kjellback, "
            + "Marcello Costamagna";
    public static final String CONTACT = "see https://github.com/denoptim-project";
    public static final String MINIMUMJAVAVERSION = "1.8";

    /**
     * Version identifier (from pom.xml via Maven properties) 
     */
    public static final String VERSION;
    static {
        final Properties properties = new Properties();
        String tmpVersion = "VersionNotFound";
        try
        {
            properties.load(Main.class.getClassLoader().getResourceAsStream(
                    "project.properties"));
            tmpVersion = properties.getProperty("version");
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        VERSION = tmpVersion;
    };
    
    /**
     * Build date (from Maven properties)
     */
    public static final String DATE;
    static {
        final Properties properties = new Properties();
        String tmpVersion = "BuildDateNotFound";
        try
        {
            properties.load(Main.class.getClassLoader().getResourceAsStream(
                    "project.properties"));
            tmpVersion = properties.getProperty("build_date");
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        DATE = tmpVersion;
    };

//------------------------------------------------------------------------------    

    /**
     * Builds the header with version, authors, contributors, and
     * @return
     */
    public static String buildDenoptimHeader()
    {
        Properties p = System.getProperties();
        String javaVersion = p.getProperty("java.version");
        String javaVM = p.getProperty("java.vm.name");
        String javaVMVersion = p.getProperty("java.vm.version");
        if (javaVM != null) 
            javaVersion = javaVersion + " / " + javaVM;
        if (javaVM != null && javaVMVersion != null)
            javaVersion = javaVersion + "-" + javaVMVersion;
    
        String NL = DENOPTIMConstants.EOL;
        return NL + "| " + NAME + 
            NL + "| DENovo OPTimization of In/organic Molecules (" + VERSION + ")" +
            NL + "| By " + AUTHORS + 
            NL + "| Contributors: " + CONTRIBUTORS +
            NL + "| Contact: " + CONTACT + 
            NL + "| Date: " + DATE +
            NL + "| Current Java: " + javaVersion +
            NL + "| Required Minimum Java: " + MINIMUMJAVAVERSION +
            NL + NL;
    }
    
//------------------------------------------------------------------------------        
    
}
