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

package denoptim.utils;


import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import denoptim.constants.DENOPTIMConstants;


/**
 * General utilities
 */

public class GeneralUtils
{
    private static Runtime RUNTIME = Runtime.getRuntime();
    private final static String NL = DENOPTIMConstants.EOL;

//------------------------------------------------------------------------------

    /**
     * returns the padded string with zeroes placed to the left of 'number' up to
     * reach the desired number of digits.
     * @param count the total number of digits.
     * @param number the number to be formatted.
     * @return the padded string.
     */
    public static String getPaddedString(int count, int number)
    {
        return String.format("%0" + count + "d", number);
    }

//------------------------------------------------------------------------------

    /**
     * Print an analysis of the current memory usage.
     */
    public static void printMemoryDetails()
    {
        NumberFormat format = NumberFormat.getInstance();
        StringBuilder sb = new StringBuilder();
        long maxMemory = RUNTIME.maxMemory();
        long allocatedMemory = RUNTIME.totalMemory();
        long freeMemory = RUNTIME.freeMemory();
        sb.append("Free memory: ");
        sb.append(format.format(freeMemory / 1024));
        sb.append(NL);
        sb.append("Allocated memory: ");
        sb.append(format.format(allocatedMemory / 1024));
        sb.append(NL);
        sb.append("Max memory: ");
        sb.append(format.format(maxMemory / 1024));
        sb.append(NL);
        sb.append("Total free memory: ");
        sb.append(format.format((freeMemory + (maxMemory - allocatedMemory)) 
                / 1024));
        sb.append(NL);
        
        System.out.println(sb.toString());
    }

//------------------------------------------------------------------------------

    /**
     * Return the index of the closing parenthesis. 
     * Ignores nested parenthesis
     * @param id identified of the parenthesis
     * (use 1 for round, 2 for square, and three for curly brackets)
     * @param s the string to analyze
     * @return the index of the closing parenthesis
     */

    public static int getIdxOfClosing(int id, String s)
    {
        String openingSing = "";
        String closingSign = ")";
        switch (id)
        {
            case (1):
                openingSing = "(";
                closingSign = ")";
                break;
            case (2):
                openingSing = "]";
                closingSign = "]";
                break;
            case (3):
                openingSing = "}";
                closingSign = "}";
                break;
        }

        int result = -1;
        int nOpen = 0;
        for (int i=0; i<s.length(); i++)
        {
            String ss = s.substring(i);
            if (ss.startsWith(closingSign))
            {
                if (nOpen == 0)
                {
                    result = i;
                    break;
                }
                else
                {
                    nOpen -= 1;
                }
            }
            else if (ss.startsWith(openingSing))
            {
                nOpen++;
            }
        }
        
        return result;
    }
    
//------------------------------------------------------------------------------

    /**
     * Takes the union of any two sets in this list that intersect. Performs
     * operations in-place.
     * @param list List to merge sets of
     */
    public static <T> void unionOfIntersectingSets(List<Set<T>> list) 
    {
        Iterator<Set<T>> iterator = list.iterator();
        
        int prevSize = 1;
        int currentSize = 0;
        while (prevSize > currentSize)
        {
            prevSize = list.size();
            while (iterator.hasNext())
            {
                Set<T> setA = iterator.next();
                Iterator<Set<T>> innerIterator = list.iterator();
                while (innerIterator.hasNext())
                {
                    Set<T> setB = innerIterator.next();
                    if (setA == setB)
                        continue;
                    if (!Collections.disjoint(setA,setB))
                    {
                        setB.addAll(setA);
                        iterator.remove();
                        break;
                    }
                }
            }
            currentSize = list.size();
        }
    }

//------------------------------------------------------------------------------
    
    /**
     * Formats a decimal number using the given pattern but with English format
     * as for separators.
     * @param pattern the pattern to use. Example "###.#"
     * @param decimals minimum number of decimal digits to print. Overwrites the
     * specific defined by the pattern.
     * @param value the value to format
     * @return the formatted string
     */
    public static String getEnglishFormattedDecimal(String pattern, 
            int decimals, double value)
    {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
        DecimalFormat df = (DecimalFormat) nf;
        df.applyPattern(pattern);
        df.setMinimumFractionDigits(decimals);
        return df.format(value);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Formats a decimal number using the given pattern but with English format
     * as for separators. Imposes 4 as the minimum number of fractional digits.
     * @param pattern the pattern to use. Example "###.####"
     * @param value the value to format
     * @return the formatted string
     */
    public static String getEnglishFormattedDecimal(String pattern, double value)
    {
        return getEnglishFormattedDecimal(pattern,4,value);
    }
    
//------------------------------------------------------------------------------
   
}
