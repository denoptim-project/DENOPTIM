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

import java.io.IOException;
import java.text.NumberFormat;


/**
 * General utilities
 */
public class GenUtils
{
    private static Runtime RUNTIME = Runtime.getRuntime();


//------------------------------------------------------------------------------
    
    // this is the javaboutique version
    public static void printExceptionChain(Throwable thr) {
        
        StackTraceElement[] elements = thr.getStackTrace();
        for (StackTraceElement element : elements) {
            System.err.println(element.getFileName() + ":"
                    + element.getLineNumber() + ">> "
                    + element.getMethodName() + "()"
            );
        }
    
    
        StackTraceElement[] steArr;
        Throwable cause = thr;
        while (cause != null) {
            System.err.println("-------------------------------");
            steArr = cause.getStackTrace();
            StackTraceElement s0 = steArr[0];
            System.err.println(cause.toString());
            System.err.println("  at " + s0.toString());
            if (cause.getCause() == null) 
            {
                System.err.println("-------------------------------");
                cause.printStackTrace();
            }
            cause = cause.getCause();
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * C++ equivalent of a getchar()
     */
     
    public static void pause() {
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

    public static String getFileExtension(String fname)
    {
        String ext = "";
        if (fname.contains("."))
        {
	        int dotPos = fname.lastIndexOf(".");
	        ext = fname.substring(dotPos);
        }
        return ext;
    }

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

    public static void printMemoryDetails()
    {
        NumberFormat format = NumberFormat.getInstance();
        StringBuilder sb = new StringBuilder();
        long maxMemory = RUNTIME.maxMemory();
        long allocatedMemory = RUNTIME.totalMemory();
        long freeMemory = RUNTIME.freeMemory();
        sb.append("Free memory: ");
        sb.append(format.format(freeMemory / 1024));
        sb.append("\n");
        sb.append("Allocated memory: ");
        sb.append(format.format(allocatedMemory / 1024));
        sb.append("\n");
        sb.append("Max memory: ");
        sb.append(format.format(maxMemory / 1024));
        sb.append("\n");
        sb.append("Total free memory: ");
        sb.append(format.format((freeMemory + (maxMemory - allocatedMemory)) 
                / 1024));
        sb.append("\n");
        
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
   
}
