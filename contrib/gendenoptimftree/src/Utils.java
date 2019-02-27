/*******************************************************************************
 *
 * This file may be distributed and/or modified under the terms of the
 * GNU General Public License version 3 as published by the Free Software
 * Foundation and appearing in the file LICENSE.GPL included in the
 * packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
 * WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 ******************************************************************************/


package gendenoptimftree;

import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author Vishwesh Venkatraman
 */

public class Utils 
{

//------------------------------------------------------------------------------

    public static boolean isNumeric(String str)
    {
        try
        {
            double d = Double.parseDouble(str);
        }
        catch(NumberFormatException nfe)
        {
            return false;
        }
        return true;
    }
    
    
//------------------------------------------------------------------------------    
    
    public static double findDeviation(ArrayList<Double> nums) 
    {    
        double mean = findMean(nums);
        double squareSum = 0;
        for (int i = 0; i < nums.size(); i++) {            
            squareSum += Math.pow(nums.get(i) - mean, 2);

        }
        return Math.sqrt((squareSum) / (nums.size() - 1));        
    }
    
//------------------------------------------------------------------------------    
    
    public static double findMean(ArrayList<Double> nums) 
    {
        double sum = 0;
        for (int i = 0; i < nums.size(); i++) 
        {
            sum += nums.get(i);
        }
        return sum / nums.size();
    }    
    
//------------------------------------------------------------------------------

    // returns a padded string with zeroes for the count,
    public static String getPaddedString(int count, int number)
    {
        String formatted = String.format("%0" + count + "d", number);
        return formatted;
    }
        
    
//------------------------------------------------------------------------------    
    
    /**
     * C++ equivalent of a getchar()
     */
     
    public static void pause()
    {
        System.err.println("Press a key to continue");
        try 
        {
            int inchar = System.in.read();
        }
        catch (IOException e)
        {
            System.err.println("Error reading from user");
        }
    }
    
//------------------------------------------------------------------------------        

}
