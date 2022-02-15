package denoptim.files;

import java.io.File;

/**
 * A file with a conventional representation of its format.
 */

public class FileAndFormat
{
    public File file;
    public FileFormat format;
    
    public FileAndFormat(File file, FileFormat format)
    {
        this.file = file;
        this.format = format;
    }
}
