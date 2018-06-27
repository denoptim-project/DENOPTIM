package denoptimcg;

import java.io.IOException;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import exception.DENOPTIMException;
import molecule.DENOPTIMAttachmentPoint;
import molecule.DENOPTIMEdge;
import molecule.DENOPTIMGraph;
import molecule.DENOPTIMVertex;
import molecule.DENOPTIMRing;
import utils.GenUtils;
import utils.DENOPTIMMathUtils;
import fragspace.FragmentSpace;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IAtomContainer;


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
