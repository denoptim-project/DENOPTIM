package denoptim.io;

import java.io.File;

/**
 * Exception thrown when the format of a file is not recognised.
 */

public class UndetectedFileFormatException extends Exception
{
    private static final long serialVersionUID = 1L;

    public UndetectedFileFormatException(File file)
    {
        super("Format of file '" + file + "' is not recognized.");
    }
}
