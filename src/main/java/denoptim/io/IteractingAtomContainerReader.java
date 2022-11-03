package denoptim.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.FormatFactory;
import org.openscience.cdk.io.formats.IChemFormat;
import org.openscience.cdk.io.formats.INChIPlainTextFormat;
import org.openscience.cdk.io.formats.MDLV2000Format;
import org.openscience.cdk.io.formats.MDLV3000Format;
import org.openscience.cdk.io.formats.SMILESFIXFormat;
import org.openscience.cdk.io.formats.SMILESFormat;
import org.openscience.cdk.io.iterator.DefaultIteratingChemObjectReader;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.io.iterator.IteratingSMILESReader;

/**
 * An iterator that take {@link IAtomContainer}s from a file, possibly using
 * an available iterating reader, or, if such reader does not exist, reads
 * the whole file and collects the containers exposing the iterator over the 
 * list of containers. In the latter case, the memory demands are important,
 * so unless a memory-efficient reader is available (see implementations
 * of {@link IteratingSDFReader}, this iterator should be used only if the
 * expected size of the input file is small.
 * 
 * @author Marco Foscato
 */
public class IteractingAtomContainerReader implements Iterator<IAtomContainer>
{

    /**
     * The containers loaded in memory, or null if we use an Iterator.
     */
    private List<IAtomContainer> results;
    
    /**
     * Reference to the iterator over pre-loaded containers or null if we 
     * use the memory-efficient iterator over the file content.
     */
    private Iterator<IAtomContainer> listIterator;
    
    /**
     * Reference to the memory-efficient iterator, or null if we have had to 
     * pre-load all containers.
     */
    private DefaultIteratingChemObjectReader<IAtomContainer> fileIterator;
    
    /**
     * Flag indicating whether we are using a memory-efficient iterator
     * (<code>true</code> of id we have had to pre-load all containers.
     */
    private boolean usingIteratingReader = false;
    
//------------------------------------------------------------------------------
    
    /**
     * Constructs an iterator over the containers that can be found in the given 
     * file.
     * @param input the file to read.
     * @throws FileNotFoundException
     * @throws IOException
     * @throws CDKException
     */
    public IteractingAtomContainerReader(File input) 
            throws FileNotFoundException, IOException, CDKException
    {
        FormatFactory factory = new FormatFactory();
        factory.registerFormat(new SMILESListFormat());
        
        IChemFormat chemFormat = factory.guessFormat(
                new BufferedReader(new FileReader(input)));
        if (chemFormat instanceof MDLV2000Format
                || chemFormat instanceof MDLV3000Format)
        {
            FileInputStream fis = new FileInputStream(input);
            fileIterator = new IteratingSDFReader(fis, 
                    DefaultChemObjectBuilder.getInstance());
            usingIteratingReader = true;
        } else if (chemFormat instanceof SMILESListFormat) {

            FileInputStream fis = new FileInputStream(input);
            fileIterator = new IteratingSMILESReader(fis,
                    DefaultChemObjectBuilder.getInstance());
            usingIteratingReader = true;
        } else { 
            results = DenoptimIO.readAllAtomContainers(input);
            listIterator = results.iterator();
        } 
    }

//------------------------------------------------------------------------------
      
    @Override
    public boolean hasNext()
    {
        if (usingIteratingReader)
            return fileIterator.hasNext();
        else
            return listIterator.hasNext();
    }

//------------------------------------------------------------------------------
      
    @Override
    public IAtomContainer next()
    {
        if (usingIteratingReader)
            return fileIterator.next();
        else
            return listIterator.next();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Close the memory-efficient iterator if any is open. Does nothing if we
     * are using the iterator over pre-loaded containers.
     * @throws IOException if the wrapper cannot be closed.
     */
    public void close() throws IOException
    {
        if (fileIterator!=null)
            fileIterator.close();
    }
  
//------------------------------------------------------------------------------
    
}
