package stringconverter;

import java.util.ArrayList;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMGraph.StringFormat;
import denoptim.threedim.ThreeDimTreeBuilder;
import denoptim.molecule.DENOPTIMTemplate;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.utils.GenUtils;
import denoptim.utils.GraphConversionTool;

public class StringConverter
{

//------------------------------------------------------------------------------

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            System.err.println("Usage: java -jar StringConverter.jar parFile");
            System.exit(-1);
        }

        try
        {
            if (StringConverterParameters.verbosity > 0)
            {
                System.out.println("StringConverter Starts");
            }
            
            StringConverterParameters.readParameterFile(args[0]);
            StringConverterParameters.checkParameters();
            StringConverterParameters.processParameters();
            
            if (StringConverterParameters.verbosity > 0)
            {
                System.out.println("Reading input string representation ("
                    + StringConverterParameters.inpFormat + ")");
            }
            String inpFileName = StringConverterParameters.inpFile;
            String outFileName = StringConverterParameters.outFile;
            StringFormat inpFormat = StringConverterParameters.inpFormat;
            StringFormat outFormat = StringConverterParameters.outFormat;
            
            // Read the input
            String input = "";
            if (inpFileName.endsWith(".sdf"))
            {
                input = DenoptimIO.readSDFFile(inpFileName).get(0).getProperty(
                        inpFormat.toString()).toString();
            } else if (inpFileName.endsWith(".txt")) {
                ArrayList<String> lines = DenoptimIO.readList(inpFileName);
                switch (inpFormat) 
                {
                    case GraphENC:
                        if (lines.size() > 1) 
                        {
                            System.out.println("ERROR! "
                                    + "Found multiple lines in '"
                                    + inpFileName 
                                    + "'. Expecting a single line.");
                            System.exit(-1);
                        }
                        input = lines.get(0);
                        break;
                        
                    case JSON:
                        for (String line : lines)
                        {
                            input = input + System.getProperty("line.separator")
                                + line;
                        }
                        break;                        
                }
            } else {
                System.out.println("ERROR! "
                        + "Could not understand file extension of '"
                        + inpFileName + "'. Expecting '.sdf' or '.txt'.");
                System.exit(-1);
            }
            
            if (StringConverterParameters.verbosity > 1)
            {
                System.out.println("INPUT: "+ input);
            }

            DENOPTIMGraph g = null;
            switch (inpFormat) 
            {
                case GraphENC:
                    g = GraphConversionTool.getGraphFromString(input);
                    break;
                    
                case JSON:
                    g = DENOPTIMGraph.fromJson(input);
                    break;
            }
            
            
            if (StringConverterParameters.verbosity > 1)
            {
                System.out.println("Vertices in input graph:");
                for (DENOPTIMVertex v : g.getVertexList())
                {
                    System.out.println(" -> "
                            +v.getVertexId()+" "+v.getClass().getName()
                            +" BBType:"+v.getBuildingBlockType()
                            +" MolID:"+v.getBuildingBlockId());
                    if (v instanceof DENOPTIMTemplate)
                    {
                        for (DENOPTIMVertex inner : ((DENOPTIMTemplate) v)
                                .getInnerGraph().getVertexList())
                        {
                            System.out.println("    -> " + inner.getVertexId()
                            +" "+inner.getClass().getName()
                            +" BBType:"+inner.getBuildingBlockType()
                            +" MolID:"+inner.getBuildingBlockId());
                            if (inner instanceof DENOPTIMTemplate)
                            {
                                for (DENOPTIMVertex innerinner : 
                                    ((DENOPTIMTemplate) inner)
                                    .getInnerGraph().getVertexList())
                                {
                                    System.out.println("       -> " 
                                            + innerinner.getVertexId()+" "
                                            + innerinner.getClass().getName() 
                                            + " BBType:" 
                                            + innerinner.getBuildingBlockType() 
                                            + " MolID:" 
                                            + innerinner.getBuildingBlockId());
                                }
                            }
                        }
                    }
                }
            }
                
            
            if (StringConverterParameters.verbosity > 0)
            {
                System.out.println("Converting " +inpFormat + " to " 
                    + StringConverterParameters.outFormat + "");
            }
            
            // Build new string representation
            String output = "";
            switch (outFormat) 
            {
                case GraphENC:
                    output = g.toString();
                    break;
                    
                case JSON:
                    output = g.toJson();
                    break;                        
            }
            
            // Write the output file
            if (outFileName.endsWith(".sdf"))
            {
                IAtomContainer iac = null;
                try 
                {
                    ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder();
                    iac = t3d.convertGraphTo3DAtomContainer(g,false);
                } catch (DENOPTIMException e) {
                    throw new DENOPTIMException("Failed conversion of graph to "
                            + "chemical representation.",e);
                }
                if (outFormat == StringFormat.JSON)
                {
                    iac.setProperty(DENOPTIMConstants.GRAPHJSONTAG, output);
                }
                DenoptimIO.writeMolecule(outFileName, iac, false);
            } else if (inpFileName.endsWith(".txt")) {
                DenoptimIO.writeData(outFileName, output, false);
            } else {
                System.out.println("ERROR! "
                        + "Could not understand file extension of '"
                        + inpFileName + "'. Expecting '.sdf' or '.txt'.");
                System.exit(-1);
            }
        }
        catch (DENOPTIMException de)
        {
            GenUtils.printExceptionChain(de);
            System.exit(-1);
        }

        if (StringConverterParameters.verbosity > 0)
        {
            System.out.println("StringConverter run completed");
        }
        System.exit(0);
    }
}
