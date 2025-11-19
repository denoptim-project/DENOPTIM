/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.molecularmodeling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.openscience.cdk.graph.PathTools;
import org.openscience.cdk.graph.SpanningTree;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IRingSet;
import org.openscience.cdk.silent.RingSet;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.DGraph;
import denoptim.graph.Edge.BondType;
import denoptim.graph.Ring;
import denoptim.graph.rings.RingClosingAttractor;
import denoptim.graph.rings.RingClosure;
import denoptim.molecularmodeling.zmatrix.ZMatrix;
import denoptim.utils.MathUtils;
import denoptim.utils.ObjectPair;

/**
 * Collector of molecular information, related to a single chemical object,
 * that is deployed within the 3D builder.
 *
 * @author Marco Foscato
 */

public class ChemicalObjectModel
{
    /**
     * DENOPTIM representation
     */
    private DGraph molGraph;

    /**
     * CDK representation
     */
    private IAtomContainer fmol;

    /**
     * ZMatrix representation
     */
    private ZMatrix zmat;

    /**
     * Reference name
     */
    private String molName;

    /**
     * List of {@link RingClosingAttractor}s.
     */
    private List<RingClosingAttractor> attractors;

    /**
     * Relation between {@link RingClosingAttractor} and index in the ZMatrix
     */
    private Map<RingClosingAttractor,Integer> attToAtmID;

    /**
     * List of combinations of {@link RingClosingAttractor} (i.e., possible
     * set of rings to create)
     */
    private List<Set<ObjectPair>> allRCACombs;

    /**
     * List of rotatable bonds
     */
    private List<ObjectPair> rotatableBnds;

    /**
     * List of new ring closing environments
     */
    private List<RingClosure> newRingClosures;

    /**
     * Quality score of the list of ring closures
     */
    private double overalRCScore = Double.NaN;

    /**
     * Atom overlap score (for atoms not is 1-4 or lower relationship)
     */
    private double atmOveralScore = Double.NaN;

    /**
     * Program-specific logger
     */
    private Logger logger;
    
    private List<Integer> oldToNewOrder;
    private List<Integer> newToOldOrder;

//------------------------------------------------------------------------------

    /**
     * Constructs an item specifying all its features
     * @param molGraph the graph representation
     * @param fmol the CDK molecular representation
     * @param zmat the internal coordinates representation
     * @param molName the reference name of this molecule
     * @param rotatableBnds the list of rotatable bonds (as pairs of atom 
     * indexes)
     * @param attractors all the ring closing attractors (RCA)
     * @param attToAtmID the correspondence between RCA and atom index in the 
     * ZMatrix
     * @param allRCACombs all combinations of compatible pairs of RCAs
     * @param ringClosures the list of closed multi-fragment rings
     * @param logger the tool to use for logging.
     */

    public ChemicalObjectModel(DGraph molGraph, 
            IAtomContainer fmol, 
            ZMatrix zmat, 
            String molName, 
            List<ObjectPair> rotatableBnds,
            List<RingClosingAttractor> attractors,
            Map<RingClosingAttractor,Integer> attToAtmID,
            List<Set<ObjectPair>> allRCACombs,
            List<RingClosure> ringClosures,
            List<Integer> oldToNewOrder,
            List<Integer> newToOldOrder,
            Logger logger)
    {
        this.logger = logger;
        this.molGraph = molGraph;
        this.fmol = fmol;
        this.zmat = zmat;
        this.attractors = attractors;
        this.attToAtmID = attToAtmID;
        this.allRCACombs = allRCACombs;
        this.rotatableBnds = rotatableBnds;
        this.molName = molName;
        this.newRingClosures = ringClosures;
        this.overalRCScore = Double.NaN;
        this.atmOveralScore = Double.NaN;
        if (this.attractors.size() != 0)
        {
            if (molGraph.hasOrEmbedsRings())
            {
                // ring closures defined in the input
                convertRingsToRCACombinations();
            }
        }
        this.oldToNewOrder = oldToNewOrder;
        this.newToOldOrder = newToOldOrder;
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a <code>Molecule3DBuilder</code> specifying all its features
     * @param molGraph the graph representation
     * @param fmol the CDK molecular representation
     * @param zmat the internal coordinates representation
     * @param molName the reference name of this molecule
     * @param rotatableBnds the list of rotatable bonds (as pairs of atom 
     * @param oldToNewOrder indexes that allow to map the atom position 
     * before and after the reordering needed to make use of internal coordinates
     * possible.
     * @param newToOldOrder indexes that allow to map the atom position 
     * before and after the reordering needed to make use of internal coordinates
     * possible.
     * @param logger the tool dealing with log messages.
     * indexes)
     */

    public ChemicalObjectModel(DGraph molGraph, IAtomContainer fmol, 
            ZMatrix zmat, String molName,
            List<ObjectPair> rotatableBnds,
            List<Integer> oldToNewOrder,
            List<Integer> newToOldOrder,
            Logger logger) throws DENOPTIMException
    {
        this.logger = logger;
        this.molGraph = molGraph;
        this.fmol = fmol;
        this.zmat = zmat;
        updateXYZFromINT();
        this.molName = molName;
        this.rotatableBnds = rotatableBnds;
        this.molName = molName;
        this.attractors = new ArrayList<RingClosingAttractor>();
        this.attToAtmID = new HashMap<RingClosingAttractor,Integer>();
        findAttractors();
        this.allRCACombs = new ArrayList<Set<ObjectPair>>();
        if (this.attractors.size() != 0)
        {
            if (molGraph.hasOrEmbedsRings())
            {
                // ring closures defined in the input
                convertRingsToRCACombinations();
            }
        }
        this.newRingClosures = new ArrayList<RingClosure>();
        this.overalRCScore = Double.NaN;
        this.atmOveralScore = Double.NaN;
        this.oldToNewOrder = oldToNewOrder;
        this.newToOldOrder = newToOldOrder;
    }
    
//------------------------------------------------------------------------------
    
    public List<Integer> getOldToNewOrder()
    {
        return oldToNewOrder;
    }
    
//------------------------------------------------------------------------------
    
    public List<Integer> getNewToOldOrder()
    {
        return newToOldOrder;
    }
    
//------------------------------------------------------------------------------    

    private void findAttractors()
    {
        // Identify all RingClosingAttractors
        for (IAtom atm : fmol.atoms())
        {
            RingClosingAttractor rca = new RingClosingAttractor(atm,fmol);
            if (rca.isAttractor())
            {
                attractors.add(rca);
                attToAtmID.put(rca, fmol.indexOf(atm));
            }
        }
    }

//------------------------------------------------------------------------------

    private void convertRingsToRCACombinations()
    {
        //When we give the a graph with rings, we are defining the RCA 
        //combinations, so there will be only one RCA combination
        Set<ObjectPair> singleRCAcomb = new HashSet<ObjectPair>();
        
        for (int i=0; i<attractors.size(); i++)
        {
            RingClosingAttractor rcaI = attractors.get(i);
            Ring ringOwnerI = rcaI.getRingUser();
            if (ringOwnerI==null)
                continue;
            
            for (int j=i+1; j<attractors.size(); j++)
            {
                RingClosingAttractor rcaJ = attractors.get(j);
                Ring ringOwnerJ = rcaJ.getRingUser();
                if (ringOwnerJ==null)
                    continue;
                if (ringOwnerI==ringOwnerJ)
                {
                    singleRCAcomb.add(new ObjectPair(rcaI, rcaJ));
                }
            }
        }
        allRCACombs.add(singleRCAcomb);
    }

//------------------------------------------------------------------------------

    /**
     * Converts currently loaded internal coordinates into Cartesian 
     * overwriting the current XYZ.
     */

    public void updateXYZFromINT() throws DENOPTIMException
    {
        List<Point3d> newCoords = new ArrayList<Point3d>();
        for (int i=0; i<fmol.getAtomCount(); i++)
        {
            int ia = zmat.getBondRefAtomIndex(i);
            int ib = zmat.getAngleRefAtomIndex(i);
            int ic = zmat.getAngle2RefAtomIndex(i);
            if ((ia != -1 && ia >  i) || (ib != -1 && ib > i) || (ic != -1 && ic > i))
            {
                String msg = "ERROR! Cannot convert internal coordinates of "
                                   + "atom " + i + ". Reference "
                                   + "atoms have higher ID. The atoms list in "
                                   + "Molecule3DBuilder needs to be reordered.";
                throw new DENOPTIMException(msg);
            }
            Integer chirality = zmat.getChiralFlag(i);
            Double bond = zmat.getBondLength(i);
            Double angle1 = zmat.getAngleValue(i);
            Double angle2 = zmat.getAngle2Value(i);

            double[] newXYZ = {0.0, 0.0, 0.0};

            double smallDble = DENOPTIMConstants.FLOATCOMPARISONTOLERANCE;
            if (ia == -1 && ib == -1 && ic == -1)
            {
                //first atom remains on the origin
            }
            else if (ib == -1 && ic == -1)
            {
                if (bond == null)
                {
                    String msg = "ERROR! Cannot convert internal coordinates of "
                                   + "atom " + i + ". Bond length is null.";
                    throw new DENOPTIMException(msg);
                }
                newXYZ[2] = bond;
            }
            else if (ic == -1)
            {
                if (bond == null)
                {
                    String msg = "ERROR! Cannot convert internal coordinates of "
                                   + "atom " + i + ". Bond length is null.";
                    throw new DENOPTIMException(msg);
                }
                if (angle1 == null)
                {
                    String msg = "ERROR! Cannot convert internal coordinates of "
                                   + "atom " + i + ". Angle 1 is null.";
                    throw new DENOPTIMException(msg);
                }
                double s1 = Math.sin(angle1 * 2 * Math.PI / 360.0);
                double c1 = Math.cos(angle1 * 2 * Math.PI / 360.0);
                Vector3d nab = MathUtils.normDist(
                                       newCoords.get(ia),newCoords.get(ib));
                double rab = MathUtils.distance(
                                       newCoords.get(ia),newCoords.get(ib));
                newXYZ[0] = bond * s1;
                newXYZ[2] = newCoords.get(ib).z + (rab - bond*c1)*nab.z;
            }
            else
            {
                if (bond == null)
                {
                    String msg = "ERROR! Cannot convert internal coordinates of "
                                   + "atom " + i + ". Bond length is null.";
                    throw new DENOPTIMException(msg);
                }
                if (angle1 == null)
                {
                    String msg = "ERROR! Cannot convert internal coordinates of "
                                   + "atom " + i + ". Angle 1 is null.";
                    throw new DENOPTIMException(msg);
                }
                if (angle2 == null)
                {
                    String msg = "ERROR! Cannot convert internal coordinates of "
                                   + "atom " + i + ". Angle 2 is null.";
                    throw new DENOPTIMException(msg);
                }
                if (chirality == null)
                {
                    String msg = "ERROR! Cannot convert internal coordinates of "
                                   + "atom " + i + ". Chirality is null.";
                    throw new DENOPTIMException(msg);
                }
                double s1 = Math.sin(angle1 * 2 * Math.PI / 360.0); 
                double s2 = Math.sin(angle2 * 2 * Math.PI / 360.0);
                double c1 = Math.cos(angle1 * 2 * Math.PI / 360.0);
                double c2 = Math.cos(angle2 * 2 * Math.PI / 360.0);

                if (chirality.equals(0))
                {
                    Vector3d nab = MathUtils.normDist(
                                        newCoords.get(ia),newCoords.get(ib));
                    Vector3d nbc = MathUtils.normDist(
                                        newCoords.get(ib),newCoords.get(ic));
                    Vector3d t = new Vector3d();
                    t.cross(nbc,nab);
                    double c = nab.x*nbc.x + nab.y*nbc.y + nab.z*nbc.z;
                    if (Math.abs(c) > (1.0 - smallDble))
                    {
                        String msg = "ERROR! Linearity does not allow definition "
                            + "of the dihedral angle for atom " + i  
                            + " (Value of c="+c+" too close to unity; threshold is "
                            + (1.0 - smallDble) + "). "
                            + "You better use dummy atoms to avoid linearities.";
                        throw new DENOPTIMException(msg);
                    }
                    t.scale(1/Math.sqrt(Math.max(1.00 - c*c, smallDble)));
                    Vector3d u = new Vector3d();
                    u.cross(t,nab);
                    newXYZ[0] = newCoords.get(ia).x
                                    + bond*(u.x*s1*c2 + t.x*s1*s2 - nab.x*c1);
                    newXYZ[1] = newCoords.get(ia).y
                                    + bond*(u.y*s1*c2 + t.y*s1*s2 - nab.y*c1);
                    newXYZ[2] = newCoords.get(ia).z
                                    + bond*(u.z*s1*c2 + t.z*s1*s2 - nab.z*c1);
                }
                else if (chirality.equals(1) || chirality.equals(-1))
                {
                    Vector3d nba = MathUtils.normDist(
                                        newCoords.get(ib),newCoords.get(ia));
                    Vector3d nac = MathUtils.normDist(
                                        newCoords.get(ia),newCoords.get(ic));
                    Vector3d t = new Vector3d();
                    t.cross(nac,nba);
                    double c = nba.x*nac.x + nba.y*nac.y + nba.z*nac.z;
                    if (Math.abs(c) > (1.0 - smallDble))
                    {
                        logger.log(Level.WARNING, "WARNING! close-to-linear system "
                            + "in the definition of atom " + i + ". "
                            + "You better use dummy atoms to avoid linearities.");
                    }
                    double s = Math.max(1.00 - c*c, smallDble);
                    double a = (-c2 - c*c1) / s;
                    double b = (c1 + c*c2) / s;
                    double ci = (1.00 + a*c2 - b*c1) / s;
                    if (ci > smallDble)
                    {
                        ci = chirality * Math.sqrt(ci);
                    } 
                    else if (ci < -smallDble)
                    {
                        ci = Math.sqrt((a*nac.x+b*nba.x)*(a*nac.x+b*nba.x) 
                                    + (a*nac.y+b*nba.y)*(a*nac.y+b*nba.y)
                                    + (a*nac.z+b*nba.z)*(a*nac.z+b*nba.z));
                        a = a / ci;
                        b = b / ci;
                        ci = 0.0;
                        logger.log(Level.WARNING, "WARNING! close-to-linear system "
                            + "in the definition of atom " + i + ". "
                            + "You better use dummy atoms to avoid linearities.");
                    }
                    else 
                    {
                        ci = 0.0;
                    }
                    newXYZ[0] = newCoords.get(ia).x 
                                            + bond*(a*nac.x + b*nba.x + ci*t.x);
                    newXYZ[1] = newCoords.get(ia).y 
                                            + bond*(a*nac.y + b*nba.y + ci*t.y);
                    newXYZ[2] = newCoords.get(ia).z 
                                            + bond*(a*nac.z + b*nba.z + ci*t.z);
                }
            }
            Point3d p3d = new Point3d(newXYZ[0],newXYZ[1],newXYZ[2]);
            newCoords.add(p3d);
        }

        // Update Cartesian coordinates
        for (int i=0; i<fmol.getAtomCount(); i++)
        {
            fmol.getAtom(i).setPoint3d(newCoords.get(i));
        }
    }

//------------------------------------------------------------------------------

    /**
     * Returns the graph representation of this molecule as it was originally
     * generated by DEOPTIM. No change is expected after ring-closing step
     */

    public DGraph getGraph()
    {
        return molGraph;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the CDK representation of the molecular system
     */

    public IAtomContainer getIAtomContainer()
    {
        return fmol;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the ZMatrix representation of the molecule
     */

    public ZMatrix getZMatrix()
    {
        return zmat;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of RuingClosingAttractors.
     */

    public List<RingClosingAttractor> getAttractorsList()
    {
        return attractors;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the attractor given its position in the list of attractors
     */

    public RingClosingAttractor getAttractor(int i)
    {
        return attractors.get(i);
    }

//------------------------------------------------------------------------------

    /**
     * Returns the index of the given 
     * {@link RingClosingAttractor} in the ZMatrix representation
     */

    public int getZMatIdxOfRCA(RingClosingAttractor rca)
    {
        return attToAtmID.get(rca) + 1;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of combinations of {@link RingClosingAttractor}. 
     * This method
     * require that there are {@link Ring}s in the {@link DGraph} representation
     * of this object.
     */

    public List<Set<ObjectPair>> getRCACombinations()
    {
        return allRCACombs;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of rotatable bonds
     */

    public List<ObjectPair> getRotatableBonds()
    {
        return rotatableBnds;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the size of the current list of rotatable bonds
     */

    public int getNumberRotatableBonds()
    {
        return rotatableBnds.size();
    }

//------------------------------------------------------------------------------

    /**
     * Return the list of RingClosures that have been identified as closable
     * head/tail of atom chains during RC-PSSROT conformational adaptation.
     */

    public List<RingClosure> getNewRingClosures()
    {
        return newRingClosures;
    }

//------------------------------------------------------------------------------

    /**
     * Return the overal evaluation of the whole list of RingClosures
     */

    public double getNewRingClosuresQuality()
    {
        if (Double.isNaN(overalRCScore))
        {
            double score = 0.0;
            for (RingClosure rc : newRingClosures)
            {
                score = score + rc.getRingClosureQuality();
            }
            overalRCScore = score;
        }
        return overalRCScore;
    }

//------------------------------------------------------------------------------

    /**
     * Return the atoms overlap score which is calculated for all atoms pairs
     * not in 1-4 or lower relationship
     */

    public double getAtomOverlapScore()
    {
        if (Double.isNaN(atmOveralScore))
        {
            double score = 0.0;
            for (IAtom atmA : fmol.atoms())
            {
                String elA = atmA.getSymbol();

                if (DENOPTIMConstants.DUMMYATMSYMBOL.equals(elA))
                    continue;

                IAtom[] toExclude = PathTools.findClosestByBond(fmol,atmA,4);
                for (IAtom atmB : fmol.atoms())
                {
                    if (atmA == atmB)
                        continue;

                    if (Arrays.asList(toExclude).contains(atmB))
                        continue;

                    String elB = atmB.getSymbol();
                    if (DENOPTIMConstants.DUMMYATMSYMBOL.equals(elB))
                        continue;

                    double dist = atmA.getPoint3d().distance(atmB.getPoint3d());
                    score = score + dist;
                }
            }
            atmOveralScore = score;
        }
        return atmOveralScore;
    }

//------------------------------------------------------------------------------

    /**
     * Return the name of this molecule
     */

    public String getName()
    {
        return molName;
    }

//------------------------------------------------------------------------------

    /**
     * Modify the molecule adding a cyclic bond between two atoms. This
     * method is ONLY meant to add new cyclic bonds and requires that
     * the pair of involved atoms comes with the object {@link RingClosure}.
     * @param atmA the first atom
     * @param atmA the second atom
     * @param nRc the {@link RingClosure} object describing the ring-closing arrangement
     * of atoms.
     */

    public void addBond(IAtom atmA, IAtom atmB, RingClosure nRc, 
            BondType bndTyp)
    {
        this.newRingClosures.add(nRc);
        this.overalRCScore = Double.NaN;

        int iA = fmol.indexOf(atmA);
        int iB = fmol.indexOf(atmB);
       
        if (bndTyp.hasCDKAnalogue())
        {
            this.fmol.addBond(iA, iB, bndTyp.getCDKOrder());
            logger.log(Level.FINE, "ADDING BOND: "+iA+" "+iB);
        } else {
            logger.log(Level.FINE, "WARNING! Attempt to add ring closing bond "
                    + "did not add any actual chemical bond because the "
                    + "bond type of the chord is '" + bndTyp +"'.");
        }

        if (iA < iB)
        {
            this.zmat.addBond(iA, iB);
        } else {
            this.zmat.addBond(iB, iA);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Remove the cyclic bonds from the list of rotatable bonds.
     *
     * @throws DENOPTIMException
     */
    public void purgeListRotatableBonds() throws DENOPTIMException
    {
        //Get all rings
        SpanningTree st = new SpanningTree(fmol);
        IRingSet allRings = new RingSet();
        try {
            allRings = st.getAllRings();
        } catch (Exception ex) {
            throw new DENOPTIMException(ex);
        }

        //Identify bonds to remove (cyclic bonds)
        List<ObjectPair> toRemove = new ArrayList<ObjectPair>();
        for (ObjectPair op : rotatableBnds)
        {
            int i1 = ((Integer)op.getFirst()).intValue();
            int i2 = ((Integer)op.getSecond()).intValue();

            IBond bnd = fmol.getBond(fmol.getAtom(i1), fmol.getAtom(i2));
            IRingSet rs = allRings.getRings(bnd);
            if (!rs.isEmpty())
            {
                toRemove.add(op);
                logger.log(Level.FINE, "Bond " + i1 + "-" + i2 + " (TnkID:"
                    + (i1+1) + "-" + (i2+1) + ") is not rotatable anymore");
            }
        }

        //Remove bonds
        for (ObjectPair op : toRemove)
        {
            rotatableBnds.remove(op);
        }        
    }

//------------------------------------------------------------------------------

    /**
     * Return a new Molecule3DBuilder having exactly the same features of this
     * Molecule3DBuilder.
     *
     * @throws DENOPTIMException
     */ 

    public ChemicalObjectModel deepcopy() throws DENOPTIMException
    {
        String nMolName = this.molName;
        DGraph nMolGraph = this.molGraph.clone();
        IAtomContainer nFMol;
        ZMatrix nzmat;
        List<ObjectPair> nRotBnds;

        try 
        {
            nFMol = this.fmol.clone();
            nzmat = (ZMatrix) this.zmat.clone();
        }
        catch (CloneNotSupportedException cns) 
        {
            throw new DENOPTIMException(cns);
        }

        nRotBnds = new ArrayList<>(this.rotatableBnds);

        List<RingClosingAttractor> nAttractors = 
                new ArrayList<RingClosingAttractor>();
        Map<RingClosingAttractor,Integer> nAttToAtmID = 
                new HashMap<RingClosingAttractor,Integer>();
        Map<RingClosingAttractor,RingClosingAttractor> oldToNewRCA =
                new HashMap<RingClosingAttractor,RingClosingAttractor>();
        for (int iorca=0; iorca<attractors.size(); iorca++)
        {
            RingClosingAttractor oRca = attractors.get(iorca);
            int ioatm = this.attToAtmID.get(oRca);
            IAtom atm = nFMol.getAtom(ioatm);
            RingClosingAttractor nRca = new RingClosingAttractor(atm,nFMol);
            nAttractors.add(nRca);
            nAttToAtmID.put(nRca, ioatm);
            oldToNewRCA.put(oRca, nRca);
        }

        List<Set<ObjectPair>> nAllRCACombs = 
                new ArrayList<Set<ObjectPair>>();
        for (Set<ObjectPair> sop : this.allRCACombs)
        {
            Set<ObjectPair> nSop = new HashSet<ObjectPair>();
            for (ObjectPair op : sop)
            {
                ObjectPair nOp = new ObjectPair(
                        oldToNewRCA.get(op.getFirst()), 
                        oldToNewRCA.get(op.getSecond()));
                nSop.add(nOp);
            }
            nAllRCACombs.add(nSop);
        }

        List<RingClosure> nNewRingClosures = new ArrayList<RingClosure>();
        for (RingClosure rc : this.newRingClosures)
        {
            nNewRingClosures.add(rc.deepCopy());
        }
        
        List<Integer> newOldToNewOrder = new ArrayList<Integer>();
        for (Integer i : oldToNewOrder)
            newOldToNewOrder.add(i.intValue());
        
        List<Integer> newNewToOldOrder = new ArrayList<Integer>();
        for (Integer i : newToOldOrder)
            newNewToOldOrder.add(i.intValue());
        
        ChemicalObjectModel molClone = new ChemicalObjectModel(nMolGraph,
                nFMol,
                nzmat,
                nMolName,
                nRotBnds,
                nAttractors,
                nAttToAtmID,
                nAllRCACombs,
                nNewRingClosures,
                newOldToNewOrder,
                newNewToOldOrder, logger);

        return molClone;
    }

//------------------------------------------------------------------------------
}
