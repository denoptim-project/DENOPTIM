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

import java.util.Properties;

import denoptim.constants.DENOPTIMConstants;

/**
 *  Version numbers used here are broken into 3 parts: major, minor, and 
 *  update, and are written as V<major>.<minor>.<update> (e.g. V0.1.1).  
 *  Major numbers will change at the time of major reworking of some 
 *  part of the system.  Minor numbers for each public release or 
 *  change big enough to cause incompatibilities.  Finally update
 *  will be incremented for small bug fixes and changes that
 *  probably wouldn't be too noticeable .  

 * @author Vishwesh Venkatraman 
 */
public class Version
{
    public static final String NAME = "DENOPTIM";
    public static final String AUTHORS = "Vishwesh Venkatraman, Marco Foscato";
    public static final String CONTRIBUTORS = "David Grellscheid​, "
            + "Einar Kjellback, "
            + "Marcello Costamagna";
    public static final String CONTACT = "see https://github.com/denoptim-project";
    public static final String DATE = "Apr 10, 2022";
    public static final String MINIMUMJAVAVERSION = "1.8";
    
    //NB: Remember the version has to be consistent with the pom.xml file
    
    /** The major version number. */
    public static final int MAJOR = 3;

    /** The minor version number. */
    public static final int MINOR = 0;

    /** The update letter. */
    public static final int UPDATE = 0;

    /** String for the current version. */
    public static final String VERSION = "V" + MAJOR + "." + MINOR + "." + UPDATE;

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
