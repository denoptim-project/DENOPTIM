package logging;

import java.util.Properties;

/**
 *  Version numbers used here are broken into 3 parts: major, minor, and 
 *  update, and are written as V<major>.<minor>.<update> (e.g. V0.1.1).  
 *  Major numbers will change at the time of major reworking of some 
 *  part of the system.  Minor numbers for each public release or 
 *  change big enough to cause incompatibilities.  Finally update
 *  will be incremented for small bug fixes and changes that
 *  probably wouldn't be too noticeable .  

 * @author Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
 */
public class Version
{
    public static final String NAME = "DENOPTIM";
    public static final String COPYRIGHT = "2014";
    public static final String AUTHORS = "Vishwesh Venkatraman, Marco Foscato";
    public static final String CONTRIBUTORS = "";
    public static final String CONTACT = "bjorn.k.alsberg@ntnu.no; vidar.jensen@kj.uib.no";
    public static final String DATE = "Dec 31, 2014";
    public static final String MINIMUMJAVAVERSION = "1.5";
    
    /** The major version number. */
    public static final int MAJOR = 0;

    /** The minor version number. */
    public static final int MINOR = 5;

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
