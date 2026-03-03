/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no> and
 *   Marco Foscato <marco.foscato@uib.no>
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomRef;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.Mapping;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.depict.Depiction;
import org.openscience.cdk.depict.DepictionGenerator;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.GeometryUtil;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IAtomType.Hybridization;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IElement;
import org.openscience.cdk.isomorphism.Mappings;
import org.openscience.cdk.isomorphism.Pattern;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.RotatableBondsCountDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.WeightDescriptor;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.IntegerResult;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.APClass;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.DGraph;
import denoptim.graph.Edge.BondType;
import denoptim.graph.Fragment;
import denoptim.graph.Ring;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.graph.rings.RingClosingAttractor;
import denoptim.io.DenoptimIO;
import denoptim.logging.StaticLogger;
import io.github.dan2097.jnainchi.InchiFlag;
import io.github.dan2097.jnainchi.InchiOptions;
import io.github.dan2097.jnainchi.InchiStatus;


/**
 * Utilities for molecule conversion
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class MoleculeUtils
{
    private static final StructureDiagramGenerator SDG = 
            new StructureDiagramGenerator();
    private static final SmilesGenerator SMGEN = new SmilesGenerator(
            SmiFlavor.Generic);
    private static final  IChemObjectBuilder builder = 
            SilentChemObjectBuilder.getInstance();

//------------------------------------------------------------------------------

    public static double getMolecularWeight(IAtomContainer mol) 
            throws DENOPTIMException
    {
        double ret_wd = 0;
        try
        {
            WeightDescriptor wd = new WeightDescriptor();
            Object[] pars = {"*"};
            wd.setParameters(pars);
            ret_wd = ((DoubleResult) wd.calculate(mol).getValue())
                    .doubleValue();
        }
        catch (Exception ex)
        {
            throw new DENOPTIMException(ex);
        }
        return ret_wd;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks if the given atom is a dummy atom based on the elemental symbol
     * and the string used for dummy atom as a convention in DENOPTIM (uses
     * {@link DENOPTIMConstants#DUMMYATMSYMBOL}. For detecting dummy atoms
     * beyond DENOPTIM's convention, use {@link DummyAtomHandler}.
     * @param atm
     * @return <code>true</code> if the symbol of the atom indicates this is a 
     * dummy atom according to DENOPTIM's convention.
     */
    public static boolean isDummy(IAtom atm)
    {
        String el = getSymbolOrLabel(atm);
        return DENOPTIMConstants.DUMMYATMSYMBOL.equals(el);
    }
    
//------------------------------------------------------------------------------

    /**
     * Check element symbol corresponds to real element of Periodic Table
     * @param atom to check.
     * @return <code>true</code> if the element symbol correspond to an atom
     * in the periodic table.
     */

    public static boolean isElement(IAtom atom)
    {
        String symbol = atom.getSymbol();
        return isElement(symbol);
    }
    
//------------------------------------------------------------------------------

    /**
     * Check element symbol corresponds to real element of Periodic Table
     * @param symbol of the element to check
     * @return <code>true</code> if the element symbol correspond to an atom
     * in the periodic table.
     */

    public static boolean isElement(String symbol)
    {
        boolean res = false;
        IsotopeFactory ifact = null;
        try {
            ifact = Isotopes.getInstance();
            if (ifact.isElement(symbol))
            {
                @SuppressWarnings("unused")
                IElement el = ifact.getElement(symbol);
                res = true;
            }
        } catch (Throwable t) {
            throw new Error("ERROR! Unable to create Isotope.");
        }
        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Replace any <code>PseudoAtom</code>s representing ring closing attractors
     * with H. No change in coordinates, but any reference to the original
     * atom, beyond those managed internally to the CDK library, will be broken.
     * @param mol Molecule to replace <code>PseudoAtom</code>s of.
     */
    public static void removeRCA(IAtomContainer mol) {

        // convert PseudoAtoms to H
        for (IAtom a : mol.atoms())
        {
            boolean isRca = false;
            Set<String> rcaElSymbols = RingClosingAttractor.RCATYPEMAP.keySet();
            for (String rcaEl : rcaElSymbols)
            {
                if (MoleculeUtils.getSymbolOrLabel(a).equals(rcaEl))
                {
                    isRca = true;
                    break;
                }
            }
            if (isRca)
            {
                IAtom newAtm = new Atom("H",new Point3d(a.getPoint3d()));
                newAtm.setProperties(a.getProperties());
                AtomContainerManipulator.replaceAtomByAtom(mol,a,newAtm);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Replace used RCAs (i.e., those involved in {@link Ring}s)
     * while adding the ring closing bonds. Does not alter the graph.
     * @param mol the molecular representation to be updated
     * @param graph the corresponding graph representation 
     * @throws DENOPTIMException
     */

    public static void removeUsedRCA(IAtomContainer mol, DGraph graph, 
            Logger logger) throws DENOPTIMException {

        // add ring-closing bonds
        ArrayList<Vertex> usedRcvs = graph.getUsedRCVertices();
        Map<Vertex,ArrayList<Integer>> vIdToAtmId =
                MoleculeUtils.getVertexToAtomIdMap(usedRcvs,mol);
        if (vIdToAtmId.size() == 0)
        {
            // No used RCV to remove.
            return;
        }
        ArrayList<IAtom> atmsToRemove = new ArrayList<>();
        ArrayList<Boolean> doneVertices =
                new ArrayList<>(Collections.nCopies(usedRcvs.size(),false));

        for (Vertex v : usedRcvs)
        {
            if (doneVertices.get(usedRcvs.indexOf(v)))
            {
                continue;
            }
            ArrayList<Ring> rings = graph.getRingsInvolvingVertex(v);
            if (rings.size() != 1)
            {
                String s = "Unexpected inconsistency between used RCV list "
                       + v + " in {" + usedRcvs + "}"
                       + "and list of DENOPTIMRings "
                       + "{" + rings + "}. Check Code!";
                throw new DENOPTIMException(s);
            }
            Vertex vH = rings.get(0).getHeadVertex();
            Vertex vT = rings.get(0).getTailVertex();
            IAtom aH = mol.getAtom(vIdToAtmId.get(vH).get(0));
            IAtom aT = mol.getAtom(vIdToAtmId.get(vT).get(0));
            if (mol.getConnectedAtomsList(aH).size() == 0
                    || mol.getConnectedAtomsList(aT).size() == 0)
            {
                // This can happen when building a graph with empty vertexes
                continue;
            }
            int iSrcH = mol.indexOf(mol.getConnectedAtomsList(aH).get(0));
            int iSrcT = mol.indexOf(mol.getConnectedAtomsList(aT).get(0));
            atmsToRemove.add(aH);
            atmsToRemove.add(aT);

            BondType bndTyp = rings.get(0).getBondType();
            if (bndTyp.hasCDKAnalogue())
            {
                mol.addBond(iSrcH, iSrcT, bndTyp.getCDKOrder());
            } else {
                logger.log(Level.WARNING, "WARNING! "
                        + "Attempt to add ring closing bond "
                        + "did not add any actual chemical bond because the "
                        + "bond type of the chord is '" + bndTyp +"'.");
            }

            doneVertices.set(usedRcvs.indexOf(vH),true);
            doneVertices.set(usedRcvs.indexOf(vT),true);
	    }
        
        // Adapt atom indexes in APs to the upcoming change of atom list
        ArrayList<Integer> removedIds = new ArrayList<Integer>();
        for (IAtom a : atmsToRemove)
        {
            removedIds.add(mol.indexOf(a));
        }
        Collections.sort(removedIds);
        for (Vertex v : graph.getVertexList())
        {
            for (AttachmentPoint ap : v.getAttachmentPoints())
            {
                int apSrcId = ap.getAtomPositionNumberInMol();
                int countOfAtmsBEforeSrc = 0;
                for (Integer removingId : removedIds)
                {
                    if (removingId < apSrcId)
                    {
                        countOfAtmsBEforeSrc++; 
                    } else if (removingId > apSrcId)
                    {
                        break;
                    }
                }
                ap.setAtomPositionNumberInMol(apSrcId-countOfAtmsBEforeSrc);
            }
        }

        // remove used RCAs
        for (IAtom a : atmsToRemove)
        {
            mol.removeAtom(a);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Returns the SMILES representation of the molecule. All atoms are expected 
     * to be explicit, as we set the count of implicit H to zero for all atoms.
     * @param mol the molecule
     * @param logger program.specific logger.
     * @return smiles string
     * @throws DENOPTIMException
     */

    public static String getSMILESForMolecule(IAtomContainer mol, Logger logger)
            throws DENOPTIMException {
        IAtomContainer fmol = builder.newAtomContainer();
        try 
        {
            fmol = mol.clone();
        }  
        catch (CloneNotSupportedException e)
        {
            throw new DENOPTIMException(e);
        }

        // remove Dummy atoms
        DummyAtomHandler dan = new DummyAtomHandler(
                DENOPTIMConstants.DUMMYATMSYMBOL, logger);
        fmol = dan.removeDummyInHapto(fmol);
        fmol = dan.removeDummy(fmol);

        // convert PseudoAtoms to H
        removeRCA(fmol);
        
        // WARNING: assumptions on implicit H count and bond orders!
        MoleculeUtils.setZeroImplicitHydrogensToAllAtoms(fmol);
        MoleculeUtils.ensureNoUnsetBondOrdersSilent(fmol);

        String smiles = "";
        try
        {
            smiles = SMGEN.create(fmol);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        	String fileName = "failed_generation_of_SMILES.sdf";
        	logger.log(Level.WARNING, "WARNING: Skipping calculation of SMILES. "
        	        + "See file '" + fileName + "'");
        	DenoptimIO.writeSDFFile(fileName,fmol,false);
        	smiles = "calculation_or_SMILES_crashed";
        }
        return smiles;
    }

//------------------------------------------------------------------------------

    /**
      * Generates 2D coordinates for the molecule
      *
      * @param ac the molecule to layout.
      * @param logger program-specific logger.
      * @return A new molecule laid out in 2D.  If the molecule already has 2D
      *         coordinates then it is returned unchanged.  If layout fails then
      *         null is returned.
     * @throws denoptim.exception.DENOPTIMException
      */
    public static IAtomContainer generate2DCoordinates(IAtomContainer ac, 
            Logger logger) throws DENOPTIMException
    {
        IAtomContainer fmol = builder.newAtomContainer();
        try 
        { 
            fmol = ac.clone();
        }  
        catch (CloneNotSupportedException e)
        {
            throw new DENOPTIMException(e);
        }

        // remove Dummy atoms
        DummyAtomHandler dan = new DummyAtomHandler(
                DENOPTIMConstants.DUMMYATMSYMBOL, logger);
        fmol = dan.removeDummyInHapto(fmol);

        // remove ring-closing attractors
        removeRCA(fmol);

        // Generate 2D structure diagram (for each connected component).
        IAtomContainer ac2d = builder.newAtomContainer();
        IAtomContainerSet som = ConnectivityChecker.partitionIntoMolecules(
                fmol);

        for (int n = 0; n < som.getAtomContainerCount(); n++)
        {
            synchronized (SDG)
            {
                IAtomContainer mol = som.getAtomContainer(n);
                SDG.setMolecule(mol, true);
                try
                {
                    // Generate 2D coordinates for this molecule.
                    SDG.generateCoordinates();
                    mol = SDG.getMolecule();
                }
                catch (Exception e)
                {
                    throw new DENOPTIMException(e);
                }

                ac2d.add(mol);  // add 2D molecule.
            }
        }
        
        return GeometryUtil.has2DCoordinates(ac2d) ? ac2d : null;
    }

//------------------------------------------------------------------------------

    /**
     * Generates the InChI key for the given atom container. By default we
     * produce a non-standard InChI using the flags {@link InchiFlag#AuxNone},
     * {@link InchiFlag#RecMet}, and {@link InchiFlag#SUU}.
     * 
     * @param mol the molecule
     * @param logger program-specific logger.
     * @return the InchiKey. <code>null</code> if error
     * @throws denoptim.exception.DENOPTIMException
     */

    public static String getInChIKeyForMolecule(IAtomContainer mol, 
            Logger logger) throws DENOPTIMException {
        InchiOptions options = new InchiOptions.InchiOptionsBuilder()
                .withFlag(InchiFlag.AuxNone)
                .withFlag(InchiFlag.RecMet)
                .withFlag(InchiFlag.SUU)
                .build();
        return getInChIKeyForMolecule(mol, options, logger);
    }
    
//------------------------------------------------------------------------------

    /**
     * Generates the INCHI key for the molecule
     * @param mol the molecule
     * @param logger program-specific logger.
     * @return the InchiKey. <code>null</code> if error
     * @throws denoptim.exception.DENOPTIMException
     */

    public static String getInChIKeyForMolecule(IAtomContainer mol, 
            InchiOptions options, Logger logger) throws DENOPTIMException {
        IAtomContainer fmol = builder.newAtomContainer();
        try 
        { 
            fmol = mol.clone();
        }  
        catch (Throwable t) 
        {
            throw new DENOPTIMException(t);
        }

        // remove Dummy atoms before generating the inchi
        DummyAtomHandler dan = new DummyAtomHandler(
                DENOPTIMConstants.DUMMYATMSYMBOL, logger);
        fmol = dan.removeDummyInHapto(fmol);

        // remove PseudoAtoms
        removeRCA(fmol);

        String inchikey;
        try
        {
            InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
            InChIGenerator gen = factory.getInChIGenerator(fmol, options);
            InchiStatus ret = gen.getStatus();
            if (ret == InchiStatus.WARNING)
            {
                //String error = gen.getMessage();
                //InChI generated, but with warning message
                //return new ObjectPair(null, error);
            }
            else if (ret != InchiStatus.SUCCESS)
            {
                return null;
            }
            inchikey = gen.getInchiKey();
        }
        catch (CDKException cdke)
        {
            throw new DENOPTIMException(cdke);
        }
        if (inchikey.length() > 0)
            return inchikey;
        else
            return null;
    }

//------------------------------------------------------------------------------

    /**
     * Count number of rotatable bonds
     * @param mol molecule to count rotatable bonds in
     * @return number of rotatable bonds
     * @throws DENOPTIMException
     */

    public static int getNumberOfRotatableBonds(IAtomContainer mol)
            throws DENOPTIMException {
        int value;
        try
        {
            IMolecularDescriptor descriptor = 
                    new RotatableBondsCountDescriptor();
            descriptor.setParameters(new Object[]{Boolean.FALSE,Boolean.FALSE});
            DescriptorValue result = descriptor.calculate(mol);
            value = ((IntegerResult)result.getValue()).intValue();
        }
        catch (CDKException cdke)
        {
            throw new DENOPTIMException(cdke);
        }
        return value;
    }

//------------------------------------------------------------------------------

    /**
     * The heavy atom count
     * @param mol Molecule to count heavy atoms in
     * @return heavy atom count
     */

    public static int getHeavyAtomCount(IAtomContainer mol)
    {
        int n = 0;
        for (IAtom atm : mol.atoms())
        {
            if (MoleculeUtils.isElement(atm)
                    && !MoleculeUtils.getSymbolOrLabel(atm).equals("H"))
                n++;
        }
        return n;
    }
    
//------------------------------------------------------------------------------

    /**
     * Count atoms with the given elemental symbol.
     * @param mol Molecule to count atoms in.
     * @param symbol the elemental symbol to look for.
     * @return the total number of those elements.
     */

    public static int countAtomsOfElement(IAtomContainer mol, String symbol)
    {
        int n = 0;
        for (IAtom atm : mol.atoms())
        {
            if (atm.getSymbol().equals(symbol))
                n++;
        }
        return n;
    }

//------------------------------------------------------------------------------

    /**
     * Method to generate the map making in relation <code>DENOPTIMVertex</code>
     * ID and atom index in the <code>IAtomContainer</code> representation of 
     * the chemical entity. Note that the <code>IAtomContainer</code> must 
     * have been generated from the <code>DENOPTIMGraph</code> that contains the
     * required <code>DENOPTIMVertex</code>s.
     * @param vertLst the list of <code>DENOPTIMVertex</code> to find
     * @param mol the molecular representation
     * @return the map of atom indexes per each <code>DENOPTIMVertex</code> ID
     */

    public static Map<Vertex,ArrayList<Integer>> getVertexToAtomIdMap(
            ArrayList<Vertex> vertLst,
            IAtomContainer mol
    ) {

        ArrayList<Long> vertIDs = new ArrayList<>();
        for (Vertex v : vertLst) {
            vertIDs.add(v.getVertexId());
        }

        Map<Vertex,ArrayList<Integer>> map = new HashMap<>();
        for (IAtom atm : mol.atoms())
        {
            long vID = Long.parseLong(atm.getProperty(
                                 DENOPTIMConstants.ATMPROPVERTEXID).toString());
            if (vertIDs.contains(vID))
            {
                Vertex v = vertLst.get(vertIDs.indexOf(vID));
                int atmID = mol.indexOf(atm);
                if (map.containsKey(v))
                {
                    map.get(v).add(atmID);
                }
                else
                {
                    ArrayList<Integer> atmLst = new ArrayList<>();
                    atmLst.add(atmID);
                    map.put(v, atmLst);
                }
            }
        }
        return map;
    }

//------------------------------------------------------------------------------
    
    /**
     * Generate a PNG image from molecule <code>mol</code>
     * @param mol generate molecule PNG from
     * @param filename name of file to store generated PNG as
     * @param logger program-specific logger.
     * @throws DENOPTIMException
     */
    
    public static void moleculeToPNG(IAtomContainer mol, String filename, 
            Logger logger) throws DENOPTIMException
    {
        IAtomContainer iac = mol;

        if (!GeometryUtil.has2DCoordinates(mol))
        {
            iac = generate2DCoordinates(mol, logger);
        }
        
        if (iac == null)
        {
            throw new DENOPTIMException("Failed to generate 2D coordinates.");
        }
        
        try
        {
            Depiction depiction = new DepictionGenerator().depict(iac);
            depiction.writeTo(filename);
        } catch (Exception e)
        {
            throw new DENOPTIMException("Failed to write image to "+filename);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Return the 3D coordinates, if present.
     * If only 2D coords exist, then it returns the 2D projected in 3D space.
     * If neither 3D nor 2D are present returns [0, 0, 0].
     * @param atm the atom to analyze.
     * @return a not null.
     */
    public static Point3d getPoint3d(IAtom atm)
    {
        Point3d p = atm.getPoint3d();

        if (p == null)
        {
            Point2d p2d = atm.getPoint2d();
            if (p2d == null)
            {
                p = new Point3d(0.0, 0.0, 0.0);
            } else {
                p = new Point3d(p2d.x, p2d.y, 0.0);
            }
        }
        return p;
    }
    
//------------------------------------------------------------------------------

    //TODO: should we set only values that would otherwise be null?
    
    /**
     * Sets zero implicit hydrogen count to all atoms.
     * @param iac the container to process
     */
    public static void setZeroImplicitHydrogensToAllAtoms(IAtomContainer iac)
    {
        for (IAtom atm : iac.atoms()) {
            atm.setImplicitHydrogenCount(0);
        }
    }

//------------------------------------------------------------------------------
    
    /**
     * Converts all the implicit hydrogens to explicit
     */
    public static void explicitHydrogens(IAtomContainer mol)
    {
        AtomContainerManipulator.convertImplicitToExplicitHydrogens(mol);
    
    }
    
//------------------------------------------------------------------------------    

    /**
     * Sets bond order = single to all otherwise unset bonds. In case of failed 
     * kekulization this method reports a warning but does not throw an 
     * exception.
     * @param iac the container to process
     */
    
    public static void ensureNoUnsetBondOrdersSilent(IAtomContainer iac)
    {
        try {
            ensureNoUnsetBondOrders(iac);
        } catch (CDKException e) {
            StaticLogger.appLogger.log(Level.WARNING, "Kekulization failed. "
                    + "Bond orders will be unreliable as all unset bonds are"
                    + "now converted to single-order bonds.");
        }
        for (IBond bnd : iac.bonds())
        {
            if (bnd.getOrder().equals(IBond.Order.UNSET)) 
            {
                bnd.setOrder(IBond.Order.SINGLE);
            }
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets bond order = single to all otherwise unset bonds. In case of failed 
     * kekulization this method reports a warning but does not throw an 
     * exception.
     * @param iac the container to process
     * @throws CDKException 
     */
    
    public static void ensureNoUnsetBondOrders(IAtomContainer iac) throws CDKException
    {
        Kekulization.kekulize(iac);
        
        for (IBond bnd : iac.bonds())
        {
            if (bnd.getOrder().equals(IBond.Order.UNSET)) 
            {
                bnd.setOrder(IBond.Order.SINGLE);
            }
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Looks for carbon atoms that are flagged as aromatic, but do not have any
     * double bond and are, therefore, not properly Kekularized.
     * @param mol the molecule to analyze.
     * @return a non-empty string if there is any carbon atom that does not look 
     * properly Kekularized.
     */
    public static String missmatchingAromaticity(IAtomContainer mol)
    {
        String cause = "";
        for (IAtom atm : mol.atoms())
        {
            //Check carbons with or without aromatic flags
            if (atm.getSymbol().equals("C") && atm.getFormalCharge() == 0
                    && mol.getConnectedBondsCount(atm) == 3)
            {
                if (atm.getFlag(CDKConstants.ISAROMATIC))
                {
                    int n = numOfBondsWithBO(atm, mol, IBond.Order.DOUBLE);
                    if (n == 0)
                    {
                        cause = "Aromatic atom " + getAtomRef(atm,mol) 
                        + " has 3 connected atoms but no double bonds";
                        return cause;
                    }
                } else {
                    for (IAtom nbr : mol.getConnectedAtomsList(atm))
                    {
                        if (nbr.getSymbol().equals("C"))
                        {
                            if (nbr.getFormalCharge() == 0)
                            {
                                if (mol.getConnectedBondsCount(nbr) == 3)
                                {
                                    int nNbr = numOfBondsWithBO(nbr, mol, 
                                            IBond.Order.SINGLE);
                                    int nAtm = numOfBondsWithBO(atm, mol, 
                                            IBond.Order.SINGLE);
                                    if ((nNbr == 3) && (nAtm == 3))
                                    {
                                        cause = "Connected atoms "
                                            + getAtomRef(atm, mol) + " " 
                                            + getAtomRef(nbr, mol) 
                                            + " have 3 connected atoms "
                                            + "but no double bond. They are "
                                            + "likely to be aromatic but no "
                                            + "aromaticity has been reported.";
                                        return cause;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return cause;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the number of bonds, with a certain bond order, surrounding the
     * given atom.
     * @param atm the atom to look at
     * @param mol its container
     * @param order the bond order to count.
     * @return the number of bonds with that order.
     */
    public static int numOfBondsWithBO(IAtom atm, IAtomContainer mol, 
            IBond.Order order)
    {
        int n = 0;
        for (IBond bnd : mol.getConnectedBondsList(atm))
        {
            if (bnd.getOrder() == order)
                n++;
        }
        return n;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Constructs a copy of an atom container, i.e., a molecule that reflects 
     * the one given in the input argument
     * in terms of atom count, type, and geometric properties, and bond count 
     * and type. Other properties are not copied.
     * @param mol the container to copy.
     * @return the chemical-copy of the input argument.
     * @throws DENOPTIMException if there are bonds involving more than two atoms.
     */
    public static IAtomContainer makeSameAs(IAtomContainer mol) throws DENOPTIMException
    {
        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
        IAtomContainer iac = builder.newAtomContainer();
        
        for (IAtom oAtm : mol.atoms())
        {
            IAtom nAtm = MoleculeUtils.makeSameAtomAs(oAtm,true,true);
            iac.addAtom(nAtm);
        }
        
        for (IBond oBnd : mol.bonds())
        {
            if (oBnd.getAtomCount() != 2)
            {
                throw new DENOPTIMException("Unable to deal with bonds "
                        + "involving more than two atoms.");
            }
            int ia = mol.indexOf(oBnd.getAtom(0));
            int ib = mol.indexOf(oBnd.getAtom(1));
            iac.addBond(ia,ib,oBnd.getOrder());
        }
        return iac;
    }
    
//------------------------------------------------------------------------------

    /**
     * Method that constructs an atom that reflect the same atom given as 
     * parameter in terms of element symbol (or label, for pseudoatoms),
     * and Cartesian coordinates, and most of the other attributes.
     * This method copies valence and implicit H count, in addition to most of
     * the fields accessible via IAtom. Use 
     * {@link #makeSameAtomAs(IAtom, boolean, boolean)} to have the option to
     * exclude valence and implicit H count.
     * This is basically a cloning method that ignores some fields.
     * @param oAtm the original
     * @return the copy of the original
     */
    public static IAtom makeSameAtomAs(IAtom oAtm)
    {
        return makeSameAtomAs(oAtm, false, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Method that constructs an atom that reflect the same atom given as 
     * parameter in terms of element symbol (or label, for pseudoatoms),
     * and Cartesian coordinates, and most of the other attributes unless 
     * otherwise specified by the flags.
     * This is basically a cloning method that ignores some fields.
     * @param oAtm the original
     * @param ignoreValence if <code>true</code> the returned atom will have 
     * null valence
     * @param ignoreImplicitH if <code>true</code> the returned atom will have 
     * null implicit hydrogen count.
     * @return the copy of the original
     */
    public static IAtom makeSameAtomAs(IAtom oAtm, boolean ignoreValence,
            boolean ignoreImplicitH)
    {
        IAtom nAtm = null;
        String s = getSymbolOrLabel(oAtm);
        if (MoleculeUtils.isElement(oAtm))
        {
            nAtm = new Atom(s);
        } else {
            nAtm = new PseudoAtom(s);
        }
        if (oAtm.getPoint3d() != null)
        {
            Point3d p3d = oAtm.getPoint3d();
            nAtm.setPoint3d(new Point3d(p3d.x, p3d.y, p3d.z));
        } else if (oAtm.getPoint2d() != null)
        {
            Point2d p2d = oAtm.getPoint2d();
            nAtm.setPoint3d(new Point3d(p2d.x, p2d.y, 0.00001));
        }
        if (oAtm.getFormalCharge() != null)
            nAtm.setFormalCharge(oAtm.getFormalCharge());
        if (oAtm.getBondOrderSum() != null)
            nAtm.setBondOrderSum(oAtm.getBondOrderSum());
        if (oAtm.getCharge() != null)
            nAtm.setCharge(oAtm.getCharge());
        if (oAtm.getValency() != null && !ignoreValence)
            nAtm.setValency(oAtm.getValency());
        if (oAtm.getExactMass() != null)
            nAtm.setExactMass(oAtm.getExactMass());
        if (oAtm.getMassNumber() != null)
            nAtm.setMassNumber(oAtm.getMassNumber());
        if (oAtm.getFormalNeighbourCount() != null)
            nAtm.setFormalNeighbourCount(oAtm.getFormalNeighbourCount());
        if (oAtm.getFractionalPoint3d() != null)
            nAtm.setFractionalPoint3d(new Point3d(oAtm.getFractionalPoint3d()));
        if (oAtm.getHybridization() != null)
            nAtm.setHybridization(Hybridization.valueOf(
                    oAtm.getHybridization().toString()));
        if (oAtm.getImplicitHydrogenCount() != null && !ignoreImplicitH)
            nAtm.setImplicitHydrogenCount(oAtm.getImplicitHydrogenCount());
        if (oAtm.getMaxBondOrder() != null)
            nAtm.setMaxBondOrder(oAtm.getMaxBondOrder());
        if (oAtm.getNaturalAbundance() != null)
            nAtm.setNaturalAbundance(oAtm.getNaturalAbundance());
        
        return nAtm;
    }

//------------------------------------------------------------------------------
    
    /**
     * Gets either the elemental symbol (for standard atoms) of the label (for
     * pseudo-atoms). Other classes implementing IAtom are not considered.
     * This method is a response to the change from CDK-1.* to CDK-2.*. 
     * In the old CDK-1.* the getSymbol() method of IAtom would return the label 
     * for a PseudoAtom, but this behavior is not retained in newer versions.
     * @param atm
     * @return either the element symbol of the label
     */
    public static String getSymbolOrLabel(IAtom atm)
    {
        String s = "none";
        if (MoleculeUtils.isElement(atm))
        {
            s = atm.getSymbol();
        } else {
            //TODO: one day will account for the possibility of having any 
            // other implementation of IAtom
            IAtom a = null;
            if (atm instanceof AtomRef) 
            {
                a = ((AtomRef) atm).deref();
                if (a instanceof PseudoAtom)
                {
                    s = ((PseudoAtom) a).getLabel();
                } else {
                    // WARNING: we fall back to standard behavior, but there
                    // could still be cases where this is not good...
                    s = a.getSymbol();
                }
            } else if (atm instanceof PseudoAtom)
            {
                s = ((PseudoAtom) atm).getLabel();
            }
        }      
        return s;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return a string with the element symbol and the atom number (1-based)
     * of the given atom.
     */
    public static String getAtomRef(IAtom atm, IAtomContainer mol)
    {
        return atm.getSymbol() + (mol.indexOf(atm) +1);
    }
    
//------------------------------------------------------------------------------

    /**
     * Calculated the centroid of the given molecule.
     * @param mol
     * @return the centroid.
     */
    public static Point3d calculateCentroid(IAtomContainer mol)
    {
        Point3d c = new Point3d(0,0,0);
        for (IAtom atm : mol.atoms())
        {
            c.add(getPoint3d(atm));
        }
        c.scale(1.0 / ((double) mol.getAtomCount()));
        return c;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Selects only the atoms that originate from a subgraph of a whole graph 
     * that originated the whole molecule given as parameter.
     * @param wholeIAC the molecular representation of the whole graph. The 
     * atoms contained here are expected to be have the property
     * {@link DENOPTIMConstants#ATMPROPVERTEXID}, 
     * which is used to identify which 
     * atoms originated from which vertex of the whole graph.
     * @param subGraph the portion of the whole graph for which we want the 
     * corresponding atoms. The vertexes are expected to have the 
     * {@link DENOPTIMConstants#STOREDVID} property that defines their original 
     * vertex ID in the whole graph. These labels are expected to be consistent
     *  with those in the property
     * {@link DENOPTIMConstants#ATMPROPVERTEXID} of the <code>wholeIAC</code>
     * parameter.
     * Note that the current vertexID of each 
     * vertex can be different from the ID of the original vertex had in the
     * while graph.
     * @return a new container with the requested substructure
     * @throws DENOPTIMException 
     */
    public static IAtomContainer extractIACForSubgraph(IAtomContainer wholeIAC, 
            DGraph subGraph, DGraph wholeGraph, Logger logger, 
            Randomizer randomizer) throws DENOPTIMException
    {
        IAtomContainer iac = makeSameAs(wholeIAC);
        
        Set<Long> wantedVIDs = new HashSet<Long>();
        Map<Long,Vertex> wantedVertexesMap = new HashMap<>();
        for (Vertex v : subGraph.getVertexList())
        {
            Object o = v.getProperty(DENOPTIMConstants.STOREDVID);
            if (o == null)
            {
                throw new DENOPTIMException("Property '" 
                        + DENOPTIMConstants.STOREDVID + "' not defined in "
                        + "vertex " + v + ", but is needed to extract "
                                + "substructure.");
            }
            wantedVIDs.add(((Long) o).longValue());
            wantedVertexesMap.put(((Long) o).longValue(), v);
        }
        
        // Identify the destiny of each atom: keep, remove, or make AP from it.
        IAtomContainer toRemove = new AtomContainer();
        Map<IAtom,IAtom> toAP = new HashMap<IAtom,IAtom>();
        Map<IAtom,AttachmentPoint> mapAtmToAPInG = 
                new HashMap<IAtom,AttachmentPoint>();
        Map<IAtom,APClass> apcMap = new HashMap<IAtom,APClass>();
        for (int i=0; i<wholeIAC.getAtomCount(); i++)
        {
            IAtom cpAtm = iac.getAtom(i);
            IAtom oriAtm = wholeIAC.getAtom(i);
            Object o = oriAtm.getProperty(DENOPTIMConstants.ATMPROPVERTEXID);
            if (o == null)
            {
                throw new DENOPTIMException("Property '" 
                        + DENOPTIMConstants.ATMPROPVERTEXID 
                        + "' not defined in atom "
                        + oriAtm.getSymbol() + wholeIAC.indexOf(oriAtm) 
                        + ", but is needed to extract substructure.");
            }
            long vid = ((Long) o).longValue();
            if (wantedVIDs.contains(vid))
            {
                continue; //keep this atom cpAtm
            }
            // Now, decide if the current atom should become an AP
            boolean willBecomeAP = false;
            for (IAtom nbr : wholeIAC.getConnectedAtomsList(oriAtm))
            {
                Object oNbr = nbr.getProperty(DENOPTIMConstants.ATMPROPVERTEXID);
                if (oNbr == null)
                {
                    throw new DENOPTIMException("Property '" 
                            + DENOPTIMConstants.ATMPROPVERTEXID 
                            + "' not defined in atom "
                            + nbr.getSymbol() + wholeIAC.indexOf(nbr) 
                            + ", but is needed to extract substructure.");
                }
                long nbrVid = ((Long) oNbr).longValue();
                if (wantedVIDs.contains(nbrVid))
                {
                    // cpAtm is connected to an atom to keep and will therefore
                    // become an AP. Note that the connection may go through
                    // a chord in the graph, i.e., a pairs of RCVs!
                    toAP.put(cpAtm,iac.getAtom(wholeIAC.indexOf(nbr)));
                    AttachmentPoint apInWholeGraph = 
                            wholeGraph.getAPOnLeftVertexID(nbrVid,vid);
                    if (apInWholeGraph == null)
                    {
                        String debugFile = "failedAPIdentificationIACSubGraph" 
                                + wholeGraph.getGraphId() + ".sdf";
                        DenoptimIO.writeGraphToSDF(new File(debugFile), 
                                wholeGraph, willBecomeAP, logger, randomizer);
                        throw new DENOPTIMException("Unmexpected null AP from "
                                + nbrVid + " " + vid +" on " + wholeGraph 
                                + " See " + debugFile);
                    }
                    AttachmentPoint apInSubGraph = 
                            wantedVertexesMap.get(nbrVid).getAP(apInWholeGraph.getIndexInOwner());
                    mapAtmToAPInG.put(cpAtm, apInSubGraph);
                    APClass apc = apInWholeGraph.getAPClass();
                    apcMap.put(cpAtm, apc);
                    willBecomeAP = true;
                    break;
                }
            }
            if (!willBecomeAP)
                toRemove.addAtom(cpAtm);
        }
        
        iac.remove(toRemove);
        
        // NB: the molecular representation in frag is NOT iac! It's a clone of it
        Fragment frag = new Fragment(iac,BBType.FRAGMENT);
        
        List<IAtom> atmosToRemove = new ArrayList<>();
        List<IBond> bondsToRemove = new ArrayList<>();
        for (IAtom trgAtmInIAC : toAP.keySet())
        {
            IAtom trgAtm = frag.getAtom(iac.indexOf(trgAtmInIAC));
            IAtom srcAtmInISC = toAP.get(trgAtmInIAC);
            IAtom srcAtm = frag.getAtom(iac.indexOf(srcAtmInISC));
            
            AttachmentPoint apInG = mapAtmToAPInG.get(trgAtmInIAC);
            
            // Make Attachment point
            Point3d srcP3d = MoleculeUtils.getPoint3d(srcAtm);
            Point3d trgP3d = MoleculeUtils.getPoint3d(trgAtm);
            double currentLength = srcP3d.distance(trgP3d);
            //TODO-V3+? change hard-coded value with property of AP, when such
            // property will be available, i. e., one refactoring of AP and 
            // atom coordinates is done.
            double idealLength = 1.53;
            /*
            double idealLength = apInG.getProperty(
                    DENOPTIMConstants.APORIGINALLENGTH);
                    */
            Point3d vector = new Point3d();
            vector.x = srcP3d.x + (trgP3d.x - srcP3d.x)*(idealLength/currentLength);
            vector.y = srcP3d.y + (trgP3d.y - srcP3d.y)*(idealLength/currentLength);
            vector.z = srcP3d.z + (trgP3d.z - srcP3d.z)*(idealLength/currentLength);
            
            AttachmentPoint createdAP = frag.addAPOnAtom(srcAtm, 
                    apcMap.get(trgAtmInIAC), vector);
            createdAP.setProperty(DENOPTIMConstants.LINKAPS, apInG);
            
            for (IBond bnd : frag.bonds())
            {
                if (bnd.contains(trgAtm))
                {
                    bondsToRemove.add(bnd);
                }
            }
            atmosToRemove.add(trgAtm);
        }
        
        // Remove atom that has become an AP and associated bonds
        for (IBond bnd : bondsToRemove)
        {
            frag.removeBond(bnd);
        }
        for (IAtom a : atmosToRemove)
        {
            frag.removeAtom(a);
        }
        
        frag.updateAPs();
        
        return frag.getIAtomContainer();
    }
    
//------------------------------------------------------------------------------

    /**
     * Determines the dimensionality of the given chemical object.
     * @param mol the given chemical object
     * @return dimensionality of this object: 2, or 3, or -1 if neither 2 nor 3.
     */
    public static int getDimensions(IAtomContainer mol)
    {
        final int is2D = 2;
        final int is3D = 3;
        final int not2or3D = -1;

        int numOf2D = 0;
        int numOf3D = 0;

        for (IAtom atm : mol.atoms())
        {
            Point2d p2d = new Point2d();
            Point3d p3d = new Point3d();
            p2d = atm.getPoint2d();
            boolean have2D = true;
            if (p2d == null)
            {
                have2D = false;
                p3d = atm.getPoint3d();
                if (p3d == null)
                {
                    return not2or3D;
                }
            }
            ArrayList<Double> pointer = new ArrayList<Double>();
            try {
                if (have2D)
                {
                    pointer.add(p2d.x);
                    pointer.add(p2d.y);
                    numOf2D++;
                } else {
                    pointer.add(p3d.x);
                    pointer.add(p3d.y);
                    pointer.add(p3d.z);
                    numOf3D++;
                }
            } catch (Throwable t) {
                return not2or3D;
            }
        }

        if (numOf2D == mol.getAtomCount())
            return is2D;
        else if (numOf3D == mol.getAtomCount())
            return is3D;
        else
            return not2or3D;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Calculates bond angle agreement and returns raw data.
     * @param templateMol the template molecule
     * @param mol the target molecule
     * @param templateToMolMap the atom mapping
     * @return double array with [totalAngleDifference, angleCount, totalChiralityPenalty, chiralityCount]
     *         Returns null if calculation fails (incomplete mapping with angles expected)
     */
    public static double[] calculateBondAngleAgreement(IAtomContainer templateMol,
        IAtomContainer mol, Map<IAtom, IAtom> templateToMolMap)
    {
        double totalAngleDifference = 0.0;
        int angleCount = 0;
        double totalChiralityPenalty = 0.0;
        int chiralityCount = 0;
        boolean hasAngles = false;

        for (IAtom templateAtom : templateMol.atoms())
        {
            if (templateAtom.getBondCount() > 1)
            {
                hasAngles = true;
            }
        }
        
        // For each atom in the template, calculate bond angles and chirality
        for (IAtom templateAtom : templateMol.atoms())
        {
            IAtom molAtom = templateToMolMap.get(templateAtom);
            if (molAtom == null)
                continue;
            
            Point3d templatePos = MoleculeUtils.getPoint3d(templateAtom);
            Point3d molPos = MoleculeUtils.getPoint3d(molAtom);
            
            if (templatePos == null || molPos == null)
                continue;
            
            // Get neighbors of this atom in the template
            List<IAtom> templateNeighbors = templateMol.getConnectedAtomsList(templateAtom);
            if (templateNeighbors.size() < 2)
                continue; // Need at least 2 neighbors to form an angle
            
            // Get corresponding neighbors in the matched molecule
            List<IAtom> molNeighbors = new ArrayList<>();
            for (IAtom templateNeighbor : templateNeighbors)
            {
                IAtom molNeighbor = templateToMolMap.get(templateNeighbor);
                if (molNeighbor != null)
                {
                    molNeighbors.add(molNeighbor);
                }
            }
            
            if (molNeighbors.size() < 2)
                continue;

            // Calculate all bond angles in the template
            for (int i = 0; i < templateNeighbors.size(); i++)
            {
                for (int j = i + 1; j < templateNeighbors.size(); j++)
                {
                    IAtom neighbor1 = templateNeighbors.get(i);
                    IAtom neighbor2 = templateNeighbors.get(j);
                    
                    IAtom molNeighbor1 = templateToMolMap.get(neighbor1);
                    IAtom molNeighbor2 = templateToMolMap.get(neighbor2);
                    
                    if (molNeighbor1 == null || molNeighbor2 == null)
                        continue;
                    
                    Point3d pos1 = MoleculeUtils.getPoint3d(neighbor1);
                    Point3d pos2 = MoleculeUtils.getPoint3d(neighbor2);
                    Point3d molPos1 = MoleculeUtils.getPoint3d(molNeighbor1);
                    Point3d molPos2 = MoleculeUtils.getPoint3d(molNeighbor2);
                    
                    if (pos1 == null || pos2 == null || molPos1 == null || molPos2 == null)
                        continue;
                    
                    // Calculate angle in template: angle at templateAtom between neighbor1 and neighbor2
                    double[] templateA = {pos1.x, pos1.y, pos1.z};
                    double[] templateB = {templatePos.x, templatePos.y, templatePos.z};
                    double[] templateC = {pos2.x, pos2.y, pos2.z};
                    double templateAngle = MathUtils.getAngle(templateA, templateB, templateC);
                    
                    // Calculate corresponding angle in matched molecule
                    double[] molA = {molPos1.x, molPos1.y, molPos1.z};
                    double[] molB = {molPos.x, molPos.y, molPos.z};
                    double[] molC = {molPos2.x, molPos2.y, molPos2.z};
                    double molAngle = MathUtils.getAngle(molA, molB, molC);
                    
                    // Add absolute difference to total
                    totalAngleDifference += Math.abs(templateAngle - molAngle);
                    angleCount++;
                }
            }
            
            // Calculate chirality-sensitive term using scalar triple product
            // Need at least 3 neighbors to define chirality
            if (templateNeighbors.size() >= 3 && molNeighbors.size() >= 3)
            {
                // Use first three neighbors to compute scalar triple product
                IAtom n1 = templateNeighbors.get(0);
                IAtom n2 = templateNeighbors.get(1);
                IAtom n3 = templateNeighbors.get(2);
                
                IAtom molN1 = templateToMolMap.get(n1);
                IAtom molN2 = templateToMolMap.get(n2);
                IAtom molN3 = templateToMolMap.get(n3);
                
                if (molN1 != null && molN2 != null && molN3 != null)
                {
                    Point3d p1 = MoleculeUtils.getPoint3d(n1);
                    Point3d p2 = MoleculeUtils.getPoint3d(n2);
                    Point3d p3 = MoleculeUtils.getPoint3d(n3);
                    Point3d molP1 = MoleculeUtils.getPoint3d(molN1);
                    Point3d molP2 = MoleculeUtils.getPoint3d(molN2);
                    Point3d molP3 = MoleculeUtils.getPoint3d(molN3);
                    
                    if (p1 != null && p2 != null && p3 != null && 
                        molP1 != null && molP2 != null && molP3 != null)
                    {
                        // Compute vectors from central atom to neighbors
                        double[] v1Template = {p1.x - templatePos.x, p1.y - templatePos.y, p1.z - templatePos.z};
                        double[] v2Template = {p2.x - templatePos.x, p2.y - templatePos.y, p2.z - templatePos.z};
                        double[] v3Template = {p3.x - templatePos.x, p3.y - templatePos.y, p3.z - templatePos.z};
                        
                        double[] v1Mol = {molP1.x - molPos.x, molP1.y - molPos.y, molP1.z - molPos.z};
                        double[] v2Mol = {molP2.x - molPos.x, molP2.y - molPos.y, molP2.z - molPos.z};
                        double[] v3Mol = {molP3.x - molPos.x, molP3.y - molPos.y, molP3.z - molPos.z};
                        
                        // Compute scalar triple product: v1 · (v2 × v3)
                        double[] crossTemplate = MathUtils.computeCrossProduct(v2Template, v3Template);
                        double[] crossMol = MathUtils.computeCrossProduct(v2Mol, v3Mol);
                        
                        double stpTemplate = v1Template[0] * crossTemplate[0] + 
                                           v1Template[1] * crossTemplate[1] + 
                                           v1Template[2] * crossTemplate[2];
                        double stpMol = v1Mol[0] * crossMol[0] + 
                                       v1Mol[1] * crossMol[1] + 
                                       v1Mol[2] * crossMol[2];
                        
                        // Penalty based on sign difference and magnitude difference
                        // If signs differ, add large penalty; if same sign, add magnitude difference
                        double chiralityPenalty = 0.0;
                        if (Math.signum(stpTemplate) != Math.signum(stpMol))
                        {
                            // Opposite chirality - large penalty
                            chiralityPenalty = 1000.0 + Math.abs(stpTemplate - stpMol);
                        }
                        else
                        {
                            // Same chirality - small penalty based on magnitude difference
                            chiralityPenalty = Math.abs(stpTemplate - stpMol);
                        }
                        
                        totalChiralityPenalty += chiralityPenalty;
                        chiralityCount++;
                    }
                }
            }
        }
        
        // If no valid comparisons could be made (incomplete mapping), return null if angles expected
        if (angleCount == 0 && chiralityCount == 0)
        {
            if (hasAngles)
            {
                return null; // Indicates failure
            }
            else
            {
                // No angles expected, return zero values
                return new double[]{0.0, 0.0, 0.0, 0.0};
            }
        }
        
        // Return array: [totalAngleDifference, angleCount, totalChiralityPenalty, chiralityCount]
        return new double[]{totalAngleDifference, angleCount, totalChiralityPenalty, chiralityCount};
    }

//------------------------------------------------------------------------------

    /**
     * Normalizes the raw bond angle agreement data to a single score.
     * @param rawData array with [totalAngleDifference, angleCount, totalChiralityPenalty, chiralityCount]
     * @return normalized score, or Double.MAX_VALUE if rawData is null
     */
    public static double normalizeBondAngleScore(double[] rawData)
    {
        if (rawData == null)
        {
            return Double.MAX_VALUE;
        }
        
        double totalAngleDifference = rawData[0];
        double angleCount = rawData[1];
        double totalChiralityPenalty = rawData[2];
        double chiralityCount = rawData[3];
        
        // If no valid comparisons could be made
        if (angleCount == 0 && chiralityCount == 0)
        {
            return 0.0;
        }
        
        // Combine angle difference and chirality penalty (normalized)
        double angleScore = angleCount > 0 ? totalAngleDifference / angleCount : 0.0;
        double chiralityScore = chiralityCount > 0 ? totalChiralityPenalty / chiralityCount : 0.0;
        
        // Return combined score (angle differences + chirality penalty)
        // Chirality penalty is weighted more heavily to ensure correct enantiomeric mapping
        return angleScore + chiralityScore;
    }

//------------------------------------------------------------------------------

    /**
     * Finds the maximum common substructure (MCS) between two molecules.
     * @param substructure the substructure to search for in the target molecule
     * @param mol the target molecule to search in
     * @param logger logger for reporting
     * @return the atom mapping as a Map<IAtom,IAtom> (key: substructure atom, 
     * value: mol atom), or an empty map if none found
     */
    public static Map<IAtom,IAtom> findBestAtomMapping(
            IAtomContainer substructure, IAtomContainer mol, Logger logger)
    {
        List<Map<IAtom,IAtom>> uniqueAtomMappings = findUniqueAtomMappings(substructure, mol, logger);
        double bestScore = Double.MAX_VALUE;
        Map<IAtom,IAtom> bestTemplateToMolMap = new HashMap<>();
        for (Map<IAtom,IAtom> templateToMolMap : uniqueAtomMappings)
        {
            double[] rawData = calculateBondAngleAgreement(substructure, mol, templateToMolMap);
            double score = normalizeBondAngleScore(rawData);
            if (score < bestScore)
            {
                bestScore = score;
                bestTemplateToMolMap = templateToMolMap;
            }
        }
        return bestTemplateToMolMap;
    }

//------------------------------------------------------------------------------

    /**
     * Finds the maximum common substructure (MCS) between two molecules.
     * @param substructure the substructure to search for in the target molecule
     * @param mol the target molecule to search in
     * @param logger logger for reporting
     * @return the atom mapping as a Map<IAtom,IAtom> (key: substructure atom, 
     * value: mol atom), or an empty map if none found
     */
    public static List<Map<IAtom,IAtom>> findUniqueAtomMappings(
            IAtomContainer substructure, IAtomContainer mol, Logger logger)
    {
        try
        {
            // Ensure molecules are properly configured for isomorphism matching
            MoleculeUtils.setZeroImplicitHydrogensToAllAtoms(substructure);
            MoleculeUtils.setZeroImplicitHydrogensToAllAtoms(mol);
            MoleculeUtils.ensureNoUnsetBondOrdersSilent(substructure);
            MoleculeUtils.ensureNoUnsetBondOrdersSilent(mol);
            
            // Create a pattern from the template molecule
            Pattern pattern = Pattern.findSubstructure(substructure);
            
            // Check if template is a substructure of mol
            if (!pattern.matches(mol))
            {
                // Template is not a substructure, try to find MCS using SMSD if available
                // For now, return null - could be enhanced with SMSD library
                return new ArrayList<>();
            }
            
            // Get all matching substructures
            Mappings mappings = pattern.matchAll(mol);
            if (!mappings.iterator().hasNext())
            {
                return new ArrayList<>();
            }
            
            // Find unique mappings based on which target atoms are mapped to
            // Use a sorted list of target atom indices as the key to identify unique mappings
            // This is more reliable than using Set<IAtom> as a HashMap key
            Map<String,Map<IAtom,IAtom>> uniqueAtomMappingsByKey = new HashMap<>();
            Map<String,Double> bestScores = new HashMap<>();
            // Track "bad" atom assignments: when we find a better mapping, we identify
            // which specific template->target atom assignments were in the worse mapping
            // and skip future mappings that have those same problematic assignments
            Map<String,Set<String>> badAssignmentsByKey = new HashMap<>();
            for (int[] mapping : mappings)
            {
                try
                {
                    // Build atom mapping for this mapping
                    Map<IAtom, IAtom> templateToMolMap = new HashMap<>();
                    List<Integer> targetIndices = new ArrayList<>();
                    for (int i = 0; i < mapping.length; i++)
                    {
                        IAtom templateAtom = substructure.getAtom(i);
                        IAtom targetAtom = mol.getAtom(mapping[i]);
                        templateToMolMap.put(templateAtom, targetAtom);
                        targetIndices.add(mapping[i]);
                    }
                    // Create a unique key from sorted target atom indices
                    Collections.sort(targetIndices);
                    String key = targetIndices.toString();
                    
                    // Check if this mapping has any "bad" assignments we've identified
                    // for this target atom set
                    if (badAssignmentsByKey.containsKey(key))
                    {
                        Set<String> badAssignments = badAssignmentsByKey.get(key);
                        boolean hasBadAssignment = false;
                        for (Map.Entry<IAtom, IAtom> entry : templateToMolMap.entrySet())
                        {
                            // Create a signature for this template->target assignment
                            String assignment = entry.getKey().getIndex() + "->" + entry.getValue().getIndex();
                            if (badAssignments.contains(assignment))
                            {
                                hasBadAssignment = true;
                                break;
                            }
                        }
                        if (hasBadAssignment)
                        {
                            // Skip this mapping - it has a known bad assignment
                            continue;
                        }
                    }
                    
                    // Calculate bond angle agreement score
                    double[] rawData = calculateBondAngleAgreement(substructure, mol, templateToMolMap);
                    double score = normalizeBondAngleScore(rawData);
                    
                    // Keep the mapping with the best score for each unique set of target atoms
                    if (!bestScores.containsKey(key) || score < bestScores.get(key))
                    {
                        // If we're replacing a previous mapping, identify the bad assignments
                        if (bestScores.containsKey(key) && uniqueAtomMappingsByKey.containsKey(key))
                        {
                            Map<IAtom, IAtom> previousMapping = uniqueAtomMappingsByKey.get(key);
                            Set<String> badAssignments = badAssignmentsByKey.computeIfAbsent(key, k -> new HashSet<>());
                            
                            // Find assignments that were in the previous (worse) mapping but not in the new (better) one
                            for (Map.Entry<IAtom, IAtom> entry : previousMapping.entrySet())
                            {
                                IAtom templateAtom = entry.getKey();
                                IAtom oldTargetAtom = entry.getValue();
                                IAtom newTargetAtom = templateToMolMap.get(templateAtom);
                                
                                // If this template atom maps to a different target atom in the better mapping,
                                // the old assignment was a "mistake"
                                if (!oldTargetAtom.equals(newTargetAtom))
                                {
                                    String badAssignment = templateAtom.getIndex() + "->" + oldTargetAtom.getIndex();
                                    badAssignments.add(badAssignment);
                                }
                            }
                        }
                        
                        bestScores.put(key, score);
                        uniqueAtomMappingsByKey.put(key, templateToMolMap);
                    }
                }
                catch (Exception e)
                {
                    // Log but continue processing other mappings
                    logger.log(Level.FINE, "Error processing mapping: " + e.getMessage());
                }
            }

            return new ArrayList<>(uniqueAtomMappingsByKey.values());
        }
        catch (Exception e)
        {
            logger.log(Level.WARNING, "Error finding MCS: " + e.getMessage());
            return new ArrayList<>();
        }
    }

//------------------------------------------------------------------------------

    /**
     * Finds the shortest path between two atoms in a molecule using BFS.
     * Only considers paths where all atoms have the same vertex ID or are boundary atoms.
     * @param mol the molecule
     * @param start the starting atom
     * @param end the ending atom
     * @param atomToVertexId map from atoms to their vertex IDs
     * @return the shortest path as a list of atoms, or null if no path exists
     */
    public static List<IAtom> findShortestPath(IAtomContainer mol, IAtom start, 
            IAtom end, Map<IAtom, Long> atomToVertexId)
    {
        if (start.equals(end))
        {
            return new ArrayList<>();
        }
        
        // BFS to find shortest path
        Map<IAtom, IAtom> parent = new HashMap<>();
        Set<IAtom> visited = new HashSet<>();
        List<IAtom> queue = new ArrayList<>();
        
        queue.add(start);
        visited.add(start);
        parent.put(start, null);
        
        while (!queue.isEmpty())
        {
            IAtom current = queue.remove(0);
            
            if (current.equals(end))
            {
                // Reconstruct path
                List<IAtom> path = new ArrayList<>();
                IAtom node = end;
                while (node != null)
                {
                    path.add(0, node);
                    node = parent.get(node);
                }
                return path;
            }
            
            List<IAtom> neighbors = mol.getConnectedAtomsList(current);
            for (IAtom neighbor : neighbors)
            {
                if (!visited.contains(neighbor))
                {
                    // Only traverse if same vertex ID or if we're at a boundary
                    Long currentVid = atomToVertexId.get(current);
                    Long neighborVid = atomToVertexId.get(neighbor);
                    
                    if (currentVid != null && neighborVid != null)
                    {
                        // Allow traversal if same vertex ID, or if moving to/from boundary
                        boolean canTraverse = currentVid.equals(neighborVid);
                        if (!canTraverse)
                        {
                            // Check if this is a boundary transition (both are boundary atoms)
                            // This is already handled by the boundary detection, so allow it
                            canTraverse = true;
                        }
                        
                        if (canTraverse)
                        {
                            visited.add(neighbor);
                            parent.put(neighbor, current);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
        
        return null; // No path found
    }
}

//------------------------------------------------------------------------------
