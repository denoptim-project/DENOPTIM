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
    public static final String COPYRIGHT = "2019";
    public static final String AUTHORS = "Vishwesh Venkatraman, Marco Foscato";
    public static final String CONTRIBUTORS = "";
    public static final String CONTACT = "see https://github.com/denoptim-project";
    public static final String DATE = "Aug 5, 2019";
    public static final String MINIMUMJAVAVERSION = "1.5";
    
    /** The major version number. */
    public static final int MAJOR = 1;

    /** The minor version number. */
    public static final int MINOR = 0;

    /** The update letter. */
    public static final int UPDATE = 1;


    /** String for the current version. */
    public static final String VERSION = "V" + MAJOR + "." + MINOR + "." + UPDATE;

//------------------------------------------------------------------------------    

    public static String message()
    {
        Properties p = System.getProperties();
        String javaVersion = p.getProperty("java.version");
        String javaVM = p.getProperty("java.vm.name");
        String javaVMVersion = p.getProperty("java.vm.version");
        if (javaVM != null) 
            javaVersion = javaVersion + " / " + javaVM;
        if (javaVM != null && javaVMVersion != null)
            javaVersion = javaVersion + "-" + javaVMVersion;
    
        return 
            "\n| " + NAME + 
            "\n| DENovo OPTimization of Organic and Inorganic Molecules (" + VERSION + ")" +
            "\n| By " + AUTHORS + 
            "\n| Contributors: " + CONTRIBUTORS +
            "\n| Contact: " + CONTACT + 
            "\n| Date: " + DATE +
            "\n| Current Java: " + javaVersion +
            "\n| Required Minimum Java: " + MINIMUMJAVAVERSION +
            "\n\n";
    }
    
//------------------------------------------------------------------------------        
    
}
