package checkanchor;

import exception.DENOPTIMException;
import io.DenoptimIO;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.smsd.Isomorphism;
import org.openscience.cdk.smsd.interfaces.Algorithm;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import utils.DENOPTIMMoleculeUtils;

/**
 *
 * @author Vishwesh Venkatraman
 */
public class CheckAnchor
{
     private static final Logger LOGGER = Logger.getLogger(CheckAnchor.class.getName());

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        if (args.length < 2)
        {
            System.err.println("Usage: java  CheckAnchor infile.sdf outfile.sdf");
            System.exit(-1);
        }

        CheckAnchor chan = new CheckAnchor();

        String sdfFile = args[0];
        String outFile = args[1];
        try
        {
            boolean hasErr = false;
            IAtomContainer mol = DenoptimIO.readSingleSDFFile(sdfFile);
            String smiles = DENOPTIMMoleculeUtils.getSMILESForMolecule(mol);
            if (!chan.hasAnchoringGroup(smiles))
            {
                // write MOL_ERROR tag
                mol.setProperty("MOL_ERROR", "Issues with the anchoring group.");
                hasErr = true;
            }
            
            // Check charge on molecule
            if (! chan.isNeutral(mol))
            {
                // calculate charge
                double charge = AtomContainerManipulator.getTotalFormalCharge(mol);
                if (Math.abs(charge) > 0)
                {   // write MOL_ERROR tag
                    if (mol.getProperty("MOL_ERROR") != null)
                    {
                        String prop = mol.getProperty("MOL_ERROR").toString();
                        mol.setProperty("MOL_ERROR", prop + "\n" + "Molecule not neutral.");
                    }
                    else
                    {
                        mol.setProperty("MOL_ERROR", "Molecule not neutral.");
                    }
                    hasErr = true;
                }
            }
            
            if (hasErr)
            {
                DenoptimIO.writeMolecule(outFile, mol, false);
            }
            
        }
        catch (DENOPTIMException de)
        {
            LOGGER.log(Level.SEVERE, null, de);
            System.exit(-1);
        }

        System.exit(0);

    }
    
//------------------------------------------------------------------------------
    
    private boolean isNeutral(IAtomContainer mol)
    {
        boolean neutral = true;
        
        
        return neutral;
    }

//------------------------------------------------------------------------------

    /**
     * This is specific for metal free dyes, <code>OPVTask</code>
     * @return <code>true</code> if the molecule has an anchoring group
     */
    private boolean hasAnchoringGroup(String molsmiles) throws DENOPTIMException
    {
        String anchor_sulphonate = "[H]OS(=O)=O";
        String anchor_carboxyl = "[H]OC=O";
        String anchor_phosphonic = "[H]OP(=O)O[H]";

        boolean hasAnchor = false;

        SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        try
        {
            IAtomContainer A1 = sp.parseSmiles(anchor_sulphonate);
            IAtomContainer A2 = sp.parseSmiles(anchor_carboxyl);
            IAtomContainer A3 = sp.parseSmiles(anchor_phosphonic);
            IAtomContainer mol = sp.parseSmiles(molsmiles);

            //Turbo mode search
            //Bond Sensitive is set true
            Isomorphism cmpr_A1 = new Isomorphism(Algorithm.SubStructure, true);

            // set molecules, remove hydrogens, clean and configure molecule
            cmpr_A1.init(A1, mol, false, false);
            // set chemical filter true
            cmpr_A1.setChemFilters(false, false, false);

            int l1 = 0;

            if (cmpr_A1.isSubgraph())
            {
                int k = cmpr_A1.getAllMapping().size();
                l1 += k;
            }


            Isomorphism cmpr_A2 = new Isomorphism(Algorithm.SubStructure, true);
            int l2 = 0;

            // set molecules, remove hydrogens, clean and configure molecule
            //System.err.println("BEFORE ANCHOR CHECK ");
            cmpr_A2.init(A2, mol, false, false);
            // set chemical filter true
            cmpr_A2.setChemFilters(false, false, false);
            if (cmpr_A2.isSubgraph())
            {
                int k = cmpr_A2.getAllMapping().size();
                l2 += k;
            }


            Isomorphism cmpr_A3 = new Isomorphism(Algorithm.SubStructure, true);
            int l3 = 0;

            // set molecules, remove hydrogens, clean and configure molecule
            //System.err.println("BEFORE ANCHOR CHECK ");
            cmpr_A3.init(A3, mol, false, false);
            // set chemical filter true
            cmpr_A3.setChemFilters(false, false, false);
            if (cmpr_A3.isSubgraph())
            {
                int k = cmpr_A3.getAllMapping().size();
                l3 += k;
            }

            //System.out.println("DONE ANCHOR CHECK " + molsmiles + " " + l1 + " " + l2 + " " + l3);
            if (l1 > 0 & l1 < 2)
                hasAnchor = true;
            if (l2 > 0 & l2 < 2)
                hasAnchor = true;
            if (l3 > 0 & l3 < 2)
                hasAnchor = true;
            //System.out.println("DONE ANCHOR CHECK " + molsmiles + " " + l1 + " " + l2 + " " + l3);
            
        }
        catch (InvalidSmilesException ise)
        {
            throw new DENOPTIMException(ise);
        }
        catch (CDKException cdke)
        {
            throw new DENOPTIMException(cdke);
        }

        return hasAnchor;
    }

//------------------------------------------------------------------------------
}
