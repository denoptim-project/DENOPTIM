package denoptim.io;

import java.util.List;

import org.openscience.cdk.io.formats.IChemFormatMatcher;
import org.openscience.cdk.tools.DataFeatures;

/**
 * Class for recognizing file containing a list of SMILES .
 * One SMILES string in each line. Since SMILES do not contain spaces, absence
 * of spaces in each line is the condition identifying a list of SMILES
 *
 * @author marcellocostamagna
 *
*/
public class SMILESListFormat implements IChemFormatMatcher
{
    @Override
    public String getReaderClassName()
    {
        return null;
    }
    
    @Override
    public String getWriterClassName()
    {
        return null;
    }
    
    @Override
    public int getSupportedDataFeatures()
    {
        return DataFeatures.NONE;
    }

    @Override
    public int getRequiredDataFeatures()
    {
        return DataFeatures.NONE;
    }

    @Override
    public String getFormatName()
    {
        return "SMILES List";
    }
    
    @Override
    public String getPreferredNameExtension()
    {
        return null;
    }
    
    @Override
    public String[] getNameExtensions()
    {
        return new String[0];
    }
    
    @Override
    public String getMIMEType()
    {
        return "chemical/smiles";
    }

    @Override
    public boolean isXMLBased()
    {
    return false;
    }
    
    @Override
    public final MatchResult matches(final List<String> lines) {
        for (int i = 0; i < Math.min(lines.size(), 100); i++) 
        {
            if (lines.get(i).contains(" ")) return NO_MATCH;
        }
        return new MatchResult(true, this, lines.size());
    }
}
