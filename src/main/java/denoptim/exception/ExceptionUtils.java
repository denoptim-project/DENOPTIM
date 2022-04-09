package denoptim.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtils
{
    /**
     * Prints the stack trace of an exception into a string.
     * @param t the exception to work with
     * @return a string that corresponds to what 
     * {@link Throwable#printStackTrace()} would print on standard output 
     * stream.
     */
    public static String getStackTraceAsString(Throwable t)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }
}
