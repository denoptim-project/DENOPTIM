package migratev2tov3;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.graph.Candidate;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMVertex;
import denoptim.io.DenoptimIO;
import denoptim.io.FileFormat;
import denoptim.io.UndetectedFileFormatException;

public class MigrateV2ToV3
{

//------------------------------------------------------------------------------

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            System.err.println("Usage: java -jar MigrateV2ToV3.jar parFile");
            System.exit(-1);
        }

        try
        {
            if (MigrateV2ToV3Parameters.verbosity > 0)
            {
                System.out.println("MigrateV2ToV3 Starts");
            }
            
            MigrateV2ToV3Parameters.readParameterFile(args[0]);
            MigrateV2ToV3Parameters.checkParameters();
            MigrateV2ToV3Parameters.processParameters();

            File outFile = new File(MigrateV2ToV3Parameters.outFile);
            File inFile = new File(MigrateV2ToV3Parameters.inpFile);
            FileFormat ff = null;
            try
            {
                ff = DenoptimIO.detectFileFormat(inFile);
            } catch (UndetectedFileFormatException | IOException e)
            {
                e.printStackTrace();
                System.out.println("ERROR! Unable to read input file '" + 
                        inFile + "'");
                System.exit(1);
            }
            
            ArrayList<Candidate> candidates;
            ArrayList<DENOPTIMGraph> graphs;
            ArrayList<DENOPTIMVertex> vertexes;
            switch (ff)
            {
                case CANDIDATESDF:
                    candidates = DenoptimIO.readCandidates(inFile, true, true);
                    DenoptimIO.writeCandidatesToFile(outFile, candidates, false);
                    break;
                    
                case GRAPHSDF:
                    graphs = DenoptimIO.readDENOPTIMGraphsFromSDFile(
                                inFile.getAbsolutePath(), true);
                    DenoptimIO.writeGraphsToFile(outFile, ff, graphs);
                    break;
                
                case GRAPHJSON:
                    graphs = DenoptimIO.readDENOPTIMGraphsFromJSONFile(
                                inFile.getAbsolutePath(), true);
                    DenoptimIO.writeGraphsToFile(outFile, ff, graphs);
                    break;
                
                case VRTXSDF:
                    try
                    {
                        vertexes = DenoptimIO.readVertexes(inFile);
                    } catch (IllegalArgumentException
                            | UndetectedFileFormatException | IOException e)
                    {
                        throw new DENOPTIMException("Unable to read "+inFile,e);
                    }
                    DenoptimIO.writeVertexesToFile(outFile, ff, vertexes);
                    break;
                
                case VRTXJSON:
                    try
                    {
                        vertexes = DenoptimIO.readVertexes(inFile);
                    } catch (IllegalArgumentException
                            | UndetectedFileFormatException | IOException e)
                    {
                        throw new DENOPTIMException("Unable to read "+inFile,e);
                    }
                    DenoptimIO.writeVertexesToFile(outFile, ff, vertexes);
                    break;
                    
                default:
                    throw new DENOPTIMException("ERROR! Not sure what to do with file " 
                            + inFile + " format " + ff);
            }
        }
        catch (DENOPTIMException de)
        {
            de.printStackTrace(System.err);
            System.exit(-1);
        }

        if (MigrateV2ToV3Parameters.verbosity > 0)
        {
            System.out.println("MigrateV2ToV3 run completed");
        }
        System.exit(0);
    }
}
