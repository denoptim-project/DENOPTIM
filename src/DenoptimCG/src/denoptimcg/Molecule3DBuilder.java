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

package denoptimcg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.graph.PathTools;
import org.openscience.cdk.graph.SpanningTree;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IRingSet;
import org.openscience.cdk.silent.RingSet;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.integration.tinker.TinkerAtom;
import denoptim.integration.tinker.TinkerMolecule;
import denoptim.molecule.APClass;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMRing;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.rings.RingClosingAttractor;
import denoptim.rings.RingClosure;
import denoptim.utils.DENOPTIMMathUtils;
import denoptim.utils.DummyAtomHandler;
import denoptim.utils.GraphConversionTool;
import denoptim.utils.ObjectPair;

/**
 * Collector of molecular information, related to a single chemical object,
 * that is deployed within the 3DBuilder
 *
 * @author Marco Foscato
 */

public class Molecule3DBuilder
{
    /**
     * DENOPTIM representation
     */
    private DENOPTIMGraph molGraph;

    /**
     * CDK representation
     */
    private IAtomContainer fmol;

    /**
     * Tinker internal coordinates representation
     */
    private TinkerMolecule tmol;

    /**
     * Reference name
     */
    private String molName;

    /**
     * List of Ring Closing Attractors (for closing new rings)
     */
    private ArrayList<RingClosingAttractor> attractors;

    /**
     * Relation between RingClosingAttractor and atom ID
     */
    private Map<RingClosingAttractor,Integer> attToAtmID;

    /**
     * List of combinations of RingClosingAttractors (i.e., possible
     * set of rings to create)
     */
    private ArrayList<Set<ObjectPair>> allRCACombs;

    /**
     * Relation between pairs of RingClosingAttractors and DENOPTIMRings
     */
    private Map<ObjectPair,DENOPTIMRing> mapDRingsRCACombs;

    /**
     * List of rotatable bonds
     */
    private ArrayList<ObjectPair> rotatableBnds;

    /**
     * Verbosity level
     */
    private int verbosity = CGParameters.getVerbosity();

    /**
     * List of new ring closing environments
     */
    private ArrayList<RingClosure> newRingClosures;

    /**
     * Quality score of the list of RingClosures
     */
    private double overalRCScore = Double.NaN;

    /**
     * Atom overlap score (for atoms not is 1-4 or lower relationship)
     */
    private double atmOveralScore = Double.NaN;


//------------------------------------------------------------------------------

    /**
     * Constructs an empty <code>Molecule3DBuilder</code>
     */

    public Molecule3DBuilder()
    {
        this.molGraph = new DENOPTIMGraph();
        this.fmol = new AtomContainer();
        this.tmol = new TinkerMolecule();
        this.attractors = new ArrayList<RingClosingAttractor>();
        this.attToAtmID = new HashMap<RingClosingAttractor,Integer>();
        this.allRCACombs = new ArrayList<Set<ObjectPair>>();
        this.mapDRingsRCACombs = new HashMap<ObjectPair,DENOPTIMRing>();
        this.rotatableBnds = new ArrayList<ObjectPair>();
        this.newRingClosures = new ArrayList<RingClosure>();
        this.molName = "none";
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a <code>Molecule3DBuilder</code> specifying all its features
     * @param molGraph the graph representation
     * @param fmol the CDK molecular representation
     * @param tmol the intermal coordinates representation
     * @param molName the reference name of this molecule
     * @param rotatableBnds the list of rotatable bonds (as pairs of atom 
     * indeces)
     * @param attractors all the ring closing attractors (RCA)
     * @param attToAtmID the correspondence between RCA and atom index
     * @param allRCACombs all combinations of compatible pairs of RCAs
     * @param ringClosures the list of closed multifragment rings
     */

    public Molecule3DBuilder(DENOPTIMGraph molGraph, 
                                IAtomContainer fmol, 
                                TinkerMolecule tmol, 
                                     String molName, 
                ArrayList<ObjectPair> rotatableBnds,
         ArrayList<RingClosingAttractor> attractors,
       Map<RingClosingAttractor,Integer> attToAtmID,
             ArrayList<Set<ObjectPair>> allRCACombs,
             ArrayList<RingClosure> ringClosures)
    {
        this.molGraph = molGraph;
        this.fmol = fmol;
        this.tmol = tmol;
        this.attractors = attractors;
        this.attToAtmID = attToAtmID;
        this.allRCACombs = allRCACombs;
        this.rotatableBnds = rotatableBnds;
        this.molName = molName;
        this.newRingClosures = ringClosures;
        this.overalRCScore = Double.NaN;
        this.atmOveralScore = Double.NaN;
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a <code>Molecule3DBuilder</code> specifying all its features
     * @param molGraph the graph representation
     * @param fmol the CDK molecular representation
     * @param tmol the intermal coordinates representation
     * @param molName the reference name of this molecule
     * @param rotatableBnds the list of rotatable bonds (as pairs of atom 
     * indeces)
     */

    public Molecule3DBuilder(DENOPTIMGraph molGraph, 
                                IAtomContainer fmol, 
                                TinkerMolecule tmol, 
                                     String molName, 
                ArrayList<ObjectPair> rotatableBnds) throws DENOPTIMException
    {
        this.molGraph = molGraph;
        this.fmol = fmol;
        this.tmol = tmol;
        updateXYZFromINT();
        this.molName = molName;
        this.rotatableBnds = rotatableBnds;
        this.molName = molName;
        this.attractors = new ArrayList<RingClosingAttractor>();
        this.attToAtmID = new HashMap<RingClosingAttractor,Integer>();
        findAttractors();
        this.allRCACombs = new ArrayList<Set<ObjectPair>>();
        this.mapDRingsRCACombs = new HashMap<ObjectPair,DENOPTIMRing>();
        if (this.attractors.size() != 0)
        {
            if (molGraph.hasRings())
            {
                // ring closures defined in the input
                convertDENOPTIMRingIntoRcaCombinationns();
            }
        }
        this.newRingClosures = new ArrayList<RingClosure>();
        this.overalRCScore = Double.NaN;
        this.atmOveralScore = Double.NaN;
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
                attToAtmID.put(rca, fmol.getAtomNumber(atm));
            }
        }
        
        // Assign the class of the related attachment point to each RCA
        for (RingClosingAttractor rca : attractors)
        {
            APClass apclass = getClassFromAttractor(rca);
            rca.setApClass(apclass);
        }

        // Report
        if (verbosity > 1)
        {
            System.out.println(" RingClosingAttractors on Molecule3DBuilder:");
            for (int i=0; i<attractors.size(); i++)
            {
                  System.out.println(" RCA: "+i+" "+attractors.get(i));
            }
        }
    }

//------------------------------------------------------------------------------

    private void convertDENOPTIMRingIntoRcaCombinationns()
    {
        Set<ObjectPair> singleRCAcomb = new HashSet<ObjectPair>();
        for (DENOPTIMRing dr : molGraph.getRings())
        {
            DENOPTIMVertex headVtx = dr.getHeadVertex();
            DENOPTIMVertex tailVtx = dr.getTailVertex();

            int iH = -1;
            int iT = -1;
            for (int i=0; i<attractors.size(); i++)
            {
                RingClosingAttractor rca = attractors.get(i);
                int vid = (Integer) rca.getIAtom().getProperty(
                                            DENOPTIMConstants.ATMPROPVERTEXID);
                
                if (vid == headVtx.getVertexId())
                {
                    iH = i;
                }
                if (vid == tailVtx.getVertexId())
                {
                    iT = i;
                }
                if (iT > -1 && iH > -1)
                {
                    break;
                }
            }

            ObjectPair op;
            if (iH > iT)
            {
                op = new ObjectPair(iT,iH);
            }
            else
            {
                op = new ObjectPair(iH,iT);
            }

            // Store this pair of RCAs
            singleRCAcomb.add(op);
            // and the relation between the pair and the associated DENOPTIMRing
            mapDRingsRCACombs.put(op,dr);
        }

        allRCACombs.add(singleRCAcomb);
    }

//------------------------------------------------------------------------------

    private APClass getClassFromAttractor(RingClosingAttractor rca)
    {
        APClass cls = null;
        IAtom atm = rca.getIAtom();
        int i = fmol.getAtomNumber(atm) + 1;
        TinkerAtom tatm = tmol.getAtom(i);
        int vtxId = tatm.getVertexId();
        int edgeId = molGraph.getIndexOfEdgeWithParent(vtxId);
        DENOPTIMEdge edge = molGraph.getEdgeAtPosition(edgeId);
        cls = edge.getSrcAPClass();
        return cls;
    }

//------------------------------------------------------------------------------

    /**
     * Converts currently loaded internal coordinates into Cartesian 
     * overwriting the current XYZ.
     */

    public void updateXYZFromINT() throws DENOPTIMException
    {
        ArrayList<Point3d> newCoords = new ArrayList<Point3d>();
        for (int i=0; i<fmol.getAtomCount(); i++)
        {
            int iTnk = i + 1;
            TinkerAtom tAtm = tmol.getAtom(iTnk);
            int ia = tAtm.getAtomNeighbours()[0];
            int ib = tAtm.getAtomNeighbours()[1];
            int ic = tAtm.getAtomNeighbours()[2];
            if (ia >  i || ib > i || ic > i)
            {
                String msg = "ERROR! Cannot convert internal coordinates of "
                                   + "atom " + i + " " + tAtm + ". Reference "
                                   + "atoms have higher ID. The atoms list in "
                                   + "Molecule3DBuilder needs to be reordered.";
                throw new DENOPTIMException(msg);
            }
            int angleFlag = tAtm.getAtomNeighbours()[3];
            double bond = tAtm.getDistAngle()[0];
            double angle1 = tAtm.getDistAngle()[1] * 2 * Math.PI / 360.0;
            double angle2 = tAtm.getDistAngle()[2] * 2 * Math.PI / 360.0;
            double[] newXYZ = {0.0, 0.0, 0.0};
            double smallDble = DENOPTIMConstants.FLOATCOMPARISONTOLERANCE;
            double s1 = Math.sin(angle1); 
            double s2 = Math.sin(angle2);
            double c1 = Math.cos(angle1);
            double c2 = Math.cos(angle2);
            if (ia == 0)
            {
                //first atom remains on the origin
            }
            else if (ib == 0)
            {
                newXYZ[2] = bond;
            }
            else if (ic == 0)
            {
                Vector3d nab = DENOPTIMMathUtils.normDist(
                                       newCoords.get(ia-1),newCoords.get(ib-1));
                double rab = DENOPTIMMathUtils.distance(
                                       newCoords.get(ia-1),newCoords.get(ib-1));
                newXYZ[0] = bond * s1;
                newXYZ[2] = newCoords.get(ib-1).z + (rab - bond*c1)*nab.z;
            }
            else if (angleFlag == 0)
            {
                Vector3d nab = DENOPTIMMathUtils.normDist(
                                       newCoords.get(ia-1),newCoords.get(ib-1));
                Vector3d nbc = DENOPTIMMathUtils.normDist(
                                       newCoords.get(ib-1),newCoords.get(ic-1));
                Vector3d t = new Vector3d();
                t.cross(nbc,nab);
                double c = nab.x*nbc.x + nab.y*nbc.y + nab.z*nbc.z;
                if (Math.abs(c) > (1.0 - smallDble))
                {
                    String msg = "ERROR! Linearity does not allow definition "
                        + "of the dihedral angle for atom " + i + " " + tAtm 
                        + " (Value of c="+c+" too close to unity; threshold is "
                        + (1.0 - smallDble) + "). "
                        + "You better use dummy atoms to avoid linearities.";
                    throw new DENOPTIMException(msg);
                }
                t.scale(1/Math.sqrt(Math.max(1.00 - c*c, smallDble)));
                Vector3d u = new Vector3d();
                u.cross(t,nab);
                newXYZ[0] = newCoords.get(ia-1).x
                                      + bond*(u.x*s1*c2 + t.x*s1*s2 - nab.x*c1);
                newXYZ[1] = newCoords.get(ia-1).y
                                      + bond*(u.y*s1*c2 + t.y*s1*s2 - nab.y*c1);
                newXYZ[2] = newCoords.get(ia-1).z
                                      + bond*(u.z*s1*c2 + t.z*s1*s2 - nab.z*c1);
            }
            else if (Math.abs(angleFlag) == 1)
            {
                Vector3d nba = DENOPTIMMathUtils.normDist(
                                       newCoords.get(ib-1),newCoords.get(ia-1));
                Vector3d nac = DENOPTIMMathUtils.normDist(
                                       newCoords.get(ia-1),newCoords.get(ic-1));
                Vector3d t = new Vector3d();
                t.cross(nac,nba);
                double c = nba.x*nac.x + nba.y*nac.y + nba.z*nac.z;
                if (Math.abs(c) > (1.0 - smallDble) && verbosity > 2)
                {
                    System.out.println("WARNING! close-to-linear system "
                        + "in the definition of atom " + i + " " + tAtm + ". "
                        + "You better use dummy atoms to avoid linearities.");
                }
                double s = Math.max(1.00 - c*c, smallDble);
                double a = (-c2 - c*c1) / s;
                double b = (c1 + c*c2) / s;
                double ci = (1.00 + a*c2 - b*c1) / s;
                if (ci > smallDble)
                {
                    ci = angleFlag * Math.sqrt(ci);
                } 
                else if (ci < -smallDble)
                {
                    ci = Math.sqrt((a*nac.x+b*nba.x)*(a*nac.x+b*nba.x) 
                                + (a*nac.y+b*nba.y)*(a*nac.y+b*nba.y)
                                + (a*nac.z+b*nba.z)*(a*nac.z+b*nba.z));
                    a = a / ci;
                    b = b / ci;
                    ci = 0.0;
                    if (verbosity > 2)
                    {
                        System.out.println("WARNING! close-to-linear system "
                        + "in the definition of atom " + i + " " + tAtm + ". "
                        + "You better use dummy atoms to avoid linearities.");
                    }
                }
                else 
                {
                   ci = 0.0;
                }
                newXYZ[0] = newCoords.get(ia-1).x 
                                            + bond*(a*nac.x + b*nba.x + ci*t.x);
                newXYZ[1] = newCoords.get(ia-1).y 
                                            + bond*(a*nac.y + b*nba.y + ci*t.y);
                newXYZ[2] = newCoords.get(ia-1).z 
                                            + bond*(a*nac.z + b*nba.z + ci*t.z);
            }
            Point3d p3d = new Point3d(newXYZ[0],newXYZ[1],newXYZ[2]);
            newCoords.add(p3d);
        }

        // Update Cartesian coordinates
        for (int i=0; i<fmol.getAtomCount(); i++)
        {
            fmol.getAtom(i).setPoint3d(newCoords.get(i));
            tmol.getAtom(i+1).moveTo(newCoords.get(i).x, 
                                     newCoords.get(i).y,
                                     newCoords.get(i).z);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Returns the graph representation of this molecule as it was originally
     * generated by DEOPTIM. No change is expected after ring-closing step
     */

    public DENOPTIMGraph getGraph()
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
     * Returns the Tinker Internal Coordination representation of the molecule
     */

    public TinkerMolecule getTinkerMolecule()
    {
        return tmol;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of RuingClosingAttractors.
     */

    public ArrayList<RingClosingAttractor> getAttractorsList()
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
     * Returns the CDK atom number, 0 to (n-1), of the given 
     * RingClosingAttractor
     */

    public int getAtmIdOfRCA(RingClosingAttractor rca)
    {
        return attToAtmID.get(rca);
    }

//------------------------------------------------------------------------------

    /**
     * Returns the Tinker atom number, 1 to n,of the given 
     * RingClosingAttractor
     */

    public int getTnkAtmIdOfRCA(RingClosingAttractor rca)
    {
        return attToAtmID.get(rca) + 1;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of combinations of RingClosingAttractors. This method
     * require that either the identifyRCACombinations method has been run 
     * or the RCA combinations were provided by means of DENOPTIMRings in the
     * DENOPTIMGraph
     */

    public ArrayList<Set<ObjectPair>> getRCACombinations()
    {
        return allRCACombs;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the DENOPTIMRing that corresponds to a given pair of
     * RingClosingAttractors.
     * @param pair the pair of RingClodingAttractors
     * @return the correspondence bewteen RCAs and DENOPTIMRings
     */

    public DENOPTIMRing getDRingFromRCAPair(ObjectPair pairRCAs)
    {
        return mapDRingsRCACombs.get(pairRCAs);
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of rotatable bonds
     */

    public ArrayList<ObjectPair> getRotatableBonds()
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

    public ArrayList<RingClosure> getNewRingClosures()
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

                if (!DummyAtomHandler.isElement(elA))
                    continue;

                IAtom[] toExclude = PathTools.findClosestByBond(fmol,atmA,4);
                for (IAtom atmB : fmol.atoms())
                {
                    if (atmA == atmB)
                        continue;

                    if (Arrays.asList(toExclude).contains(atmB))
                        continue;

                    String elB = atmB.getSymbol();
                    if (!DummyAtomHandler.isElement(elB))
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
     * the pair of involved atoms comes with the object RingClosure.
     * @param atmA the first atom
     * @param atmA the second atom
     * @param nRc the RingClosure object describing the ring-closing arrangement of
     * atoms
     */

    public void addBond(IAtom atmA, IAtom atmB, RingClosure nRc, 
            BondType bndTyp)
    {
        this.newRingClosures.add(nRc);
        this.overalRCScore = Double.NaN;

        int iA = fmol.getAtomNumber(atmA);
        int iB = fmol.getAtomNumber(atmB);
       
        if (bndTyp.hasCDKAnalogue())
        {
            this.fmol.addBond(iA, iB, bndTyp.getCDKOrder());
            if (verbosity > 2)
                System.out.println("ADDING BOND: "+iA+" "+iB);
        } else {
            System.out.println("WARNING! Attempt to add ring closing bond "
                    + "did not add any actual chemical bond because the "
                    + "bond type of the chord is '" + bndTyp +"'.");
        }

        if (iA < iB)
        {
            this.tmol.addBond(iA+1, iB+1);
        }
        else
        {
            this.tmol.addBond(iB+1, iA+1);
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
        ArrayList<ObjectPair> toRemove = new ArrayList<ObjectPair>();
        for (ObjectPair op : rotatableBnds)
        {
            int i1 = ((Integer)op.getFirst()).intValue();
            int i2 = ((Integer)op.getSecond()).intValue();

            IBond bnd = fmol.getBond(fmol.getAtom(i1), fmol.getAtom(i2));
            IRingSet rs = allRings.getRings(bnd);
            if (!rs.isEmpty())
            {
                toRemove.add(op);
                if (verbosity > 2)
                {
                    System.out.println("Bond " + i1 + "-" + i2 + " (TnkID:"
                                        + (i1+1) + "-" + (i2+1)
                                        + ") is not rotatable anymore");
                }
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

    public Molecule3DBuilder deepcopy() throws DENOPTIMException
    {
        String nMolName = this.molName;
        DENOPTIMGraph nMolGraph = 
               GraphConversionTool.getGraphFromString(this.molGraph.toString());
        IAtomContainer nFMol;
        TinkerMolecule nTMol;
        ArrayList<ObjectPair> nRotBnds;

        try 
        {
            nFMol = this.fmol.clone();
            nTMol = (TinkerMolecule) this.tmol.deepCopy();
            nRotBnds = (ArrayList<ObjectPair>) this.rotatableBnds.clone();
        }
        catch (CloneNotSupportedException cns) 
        {
            throw new DENOPTIMException(cns);
        }

        ArrayList<RingClosingAttractor> nAttractors = 
                                        new ArrayList<RingClosingAttractor>();
        Map<RingClosingAttractor,Integer> nAttToAtmID = 
                                new HashMap<RingClosingAttractor,Integer>();
        for (int iorca=0; iorca<attractors.size(); iorca++)
        {
            RingClosingAttractor oRca = attractors.get(iorca);
            int ioatm = this.getAtmIdOfRCA(oRca);
            IAtom atm = nFMol.getAtom(ioatm);
            RingClosingAttractor nRca = new RingClosingAttractor(atm,nFMol);
            TinkerAtom nTa = nTMol.getAtom(ioatm+1);
            int vtxId = nTa.getVertexId();
            int edgeId = nMolGraph.getIndexOfEdgeWithParent(vtxId);
            DENOPTIMEdge edge = nMolGraph.getEdgeAtPosition(edgeId);
            APClass cls = edge.getSrcAPClass();
            nRca.setApClass(cls);
            nAttractors.add(nRca);
            nAttToAtmID.put(nRca,ioatm);
        }

        ArrayList<Set<ObjectPair>> nAllRCACombs = 
                                        new ArrayList<Set<ObjectPair>>();
        for (Set<ObjectPair> sop : this.allRCACombs)
        {
            Set<ObjectPair> nSop = new HashSet<ObjectPair>();
            for (ObjectPair op : sop)
            {
                int fst = ((Integer)op.getFirst()).intValue();
                int scn = ((Integer)op.getSecond()).intValue();
                ObjectPair nOp = new ObjectPair(fst,scn);
                nSop.add(nOp);
            }
            nAllRCACombs.add(nSop);
        }

        ArrayList<RingClosure> nNewRingClosures = 
                                        new ArrayList<RingClosure>();
        for (RingClosure rc : this.newRingClosures)
        {
            nNewRingClosures.add(rc.deepCopy());
        }

        Molecule3DBuilder molClone = new Molecule3DBuilder(nMolGraph,
                                                    nFMol,
                                                    nTMol,
                                                    nMolName,
                                                    nRotBnds,
                                                    nAttractors,
                                                    nAttToAtmID,
                                                    nAllRCACombs,
                                                    nNewRingClosures);

        return molClone;
    }

//------------------------------------------------------------------------------
}
