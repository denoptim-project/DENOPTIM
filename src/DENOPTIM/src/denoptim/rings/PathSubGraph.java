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

package denoptim.rings;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import javax.vecmath.Point3d;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.threedim.TreeBuilder3D;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.graph.PathTools;


/**
 * This object represents a path in a <code>DENOPTIMGraph</code>. The path 
 * involving more than one <code>DENOPTIMVertex</code> and 
 * <code>DENOPTIMEdge</code>.
 *
 * @author Marco Foscato 
 */

public class PathSubGraph
{
    /**
     * The graph representation of this path. 
     * With respect to the extended <code>DENOPTIMGraph</code>, both
     * <code>DENOPTIMVertex</code> and <code>DENOPTIMEdge</code> 
     */
    private DENOPTIMGraph graph;

    /**
     * The string identifier of this path
     */
    private String chainID;
    private String revChainID;
    private ArrayList<String> allPossibleChainIDs = new ArrayList<String>();

    /**
     * The vertex representing the first RCA: head of the path
     */
    private DENOPTIMVertex vA;

    /**
     * The vertex representing the second RCA: the tail of the path
     */
    private DENOPTIMVertex vB;

    /**
     * The turning point in the graph: after this point the direction of 
     * <code>DENOPTIMEdge</code>s becomes opposite than before.
     */
    private DENOPTIMVertex turningPointVert;

    /**
     * The list of verteces involved
     */
    private List<DENOPTIMVertex> vertPathVAVB;

    /**
     * The list of edges involved
     */
    private List<DENOPTIMEdge> edgesPathVAVB;

    /**
     * The molecular representation of the fragment in the path
     */
    private IAtomContainer iacPathVAVB;

    /**
     * The list of atoms in the shortest path
     */
    private List<IAtom> atomsPathVAVB;

    /**
     * The list of bonds in the sortest path
     * Note that the <code>IBond</code>s are those of the entire molecule
     * not of the <code>IAtomContainer</code> representing only this path.
     */
    private List<IBond> bondsPathVAVB;

    /**
     * Per each bond the pair of point used to define the torsion
     */
    private ArrayList<ArrayList<Point3d>> dihedralRefPts;

    /**
     * The flag defining whether this object has already a molecular
     * representation or not
     */
    private boolean hasMolRepr = false; 

    /**
     * The string of atoms involved (atom numbers from full molecule list)
     */
    private String atmNumStr;

    /**
     * The list of closable conformations, if any is found
     */
    private RingClosingConformations rcc;

    // Set to true to print useful debug information
    private boolean debug = false;

//-----------------------------------------------------------------------------

    /**
     * Constructs a new PathSubGraph specifying the first and last vertex of 
     * the path
     */

    public PathSubGraph(DENOPTIMVertex vA, 
                   DENOPTIMVertex vB,
                   DENOPTIMGraph molGraph)
    {
        this.vA = vA;
        this.vB = vB;

        // Identify the path between vA and vB
        // Obtain path from vA to seed of the graph
        List<DENOPTIMVertex> seedToVA = new ArrayList<DENOPTIMVertex>();
        List<DENOPTIMEdge> seedToVAEdges = new ArrayList<DENOPTIMEdge>();
        seedToVA.add(vA);
        int currVert = vA.getVertexId();
        for (int i=-1; i<vA.getLevel(); i++)
        {
            DENOPTIMEdge edgeToParent = molGraph.getEdgeAtPosition(
                        molGraph.getIndexOfEdgeWithParent(currVert));
            seedToVAEdges.add(edgeToParent);
            DENOPTIMVertex parent = molGraph.getParent(currVert);
            seedToVA.add(parent);
            currVert = parent.getVertexId();
            // This is the bugfix that allows to handle graphs with wrong level
            // reported in the fragments
            if (parent.getLevel() == -1)
            {
                break;
            }
        }
        // Obtain path from vB to seed of the graph
        List<DENOPTIMVertex> seedToVB = new ArrayList<DENOPTIMVertex>();
        List<DENOPTIMEdge> seedToVBEdges = new ArrayList<DENOPTIMEdge>();
        seedToVB.add(vB);
        currVert = vB.getVertexId();
        for (int i=-1; i<vB.getLevel(); i++)
        {
            DENOPTIMEdge edgeToParent = molGraph.getEdgeAtPosition(
                        molGraph.getIndexOfEdgeWithParent(currVert));
            seedToVBEdges.add(edgeToParent);
            DENOPTIMVertex parent = molGraph.getParent(currVert);
            seedToVB.add(parent);
            currVert = parent.getVertexId();
            // This is the bugfix that allows to handle graphs with wrong level
            // reported in the fragments
            if (parent.getLevel() == -1)
            {
                break;
            }
        }

        // find XOR plus junction vertex
        vertPathVAVB = new ArrayList<DENOPTIMVertex>();
        edgesPathVAVB = new ArrayList<DENOPTIMEdge>();
        turningPointVert = null;
        for (int i=0; i<seedToVA.size(); i++)
        {
            if (seedToVB.contains(seedToVA.get(i)))
            {
                turningPointVert = seedToVA.get(i);
                vertPathVAVB.add(seedToVA.get(i));
                int idStart = seedToVB.indexOf(seedToVA.get(i))-1;
                for (int j=idStart; j>-1; j--)
                {
                    vertPathVAVB.add(seedToVB.get(j));
                    edgesPathVAVB.add(seedToVBEdges.get(j));
                }
                break;
            }
            else
            {
                vertPathVAVB.add(seedToVA.get(i));
                edgesPathVAVB.add(seedToVAEdges.get(i));
            }
        }

        // Build the DENOPTIMGraph and string representations of this sub graph
        chainID = "";
        revChainID = "";
        DENOPTIMVertex vertBack;
        DENOPTIMVertex vertHere;
        DENOPTIMVertex vertFrnt;
        boolean insideOut = false;
        ArrayList<DENOPTIMVertex> gVertices = new ArrayList<DENOPTIMVertex>();
        ArrayList<DENOPTIMEdge> gEdges = new ArrayList<DENOPTIMEdge>();
        for (int i=1; i < vertPathVAVB.size()-1; i++)
        {
            vertBack = vertPathVAVB.get(i-1);
            vertHere = vertPathVAVB.get(i);
            vertFrnt = vertPathVAVB.get(i+1);
            DENOPTIMEdge edgeToBack = edgesPathVAVB.get(i-1);
            DENOPTIMEdge edgeToFrnt = edgesPathVAVB.get(i);

            // The first and the last verteces will always be RCA, and they
            // may be from the capping library of from the fragments library
            // and this will make the chainID be different for otherwise equal
            // chains. Thus first and last vertex are not seen in the chainID
            
            
            //TODO-V3 chainID uses molId and fragType from DENOPTIMFragment
            // Therefore, vertexes that are not instances of DENOPTIMFragment
            // cannot yet be used.
            // When we'll have APs with owner we'll be able to build the path
            // following AP ownership and, thus, we should be able to get the
            // path from the Template's graph (recursion?).
            if (vertHere instanceof DENOPTIMFragment == false)
            {
                Exception e = new Exception("TODO: Upgrade code to include handling of Templates!!!");
                e.printStackTrace();
                System.err.println("ERROR! Current managment of chains cannot "
                        + "handle vertexes that are NOT intances of "
                        + "DENOPTIMFragment.");
                System.exit(-1);
            }
            
            DENOPTIMFragment vertFrgHere = (DENOPTIMFragment) vertHere;
            
            chainID = chainID + vertFrgHere.getMolId() + "/"
                              + vertFrgHere.getFragmentType() + "/" + "ap";
            String leftRevChainID = vertFrgHere.getMolId() + "/"
                                  + vertFrgHere.getFragmentType() + "/" + "ap";

            int apIdBack2Here = -1;
            int apIdHere2Back = -1;
            int apIdHere2Frnt = -1;
            int apIdFrnt2Here = -1;
            if (vertHere == turningPointVert)
            {
                insideOut = true;
                apIdBack2Here = edgeToBack.getTrgAPID();
                apIdHere2Back = edgeToBack.getSrcAPID();
                apIdHere2Frnt = edgeToFrnt.getSrcAPID();
                apIdFrnt2Here = edgeToFrnt.getTrgAPID();
            }
            else
            {
                if (insideOut)
                {
                    apIdBack2Here = edgeToBack.getSrcAPID();
                    apIdHere2Back = edgeToBack.getTrgAPID();
                    apIdHere2Frnt = edgeToFrnt.getSrcAPID();
                    apIdFrnt2Here = edgeToFrnt.getTrgAPID();
                }
                else
                {
                    apIdBack2Here = edgeToBack.getTrgAPID();
                    apIdHere2Back = edgeToBack.getSrcAPID();
                    apIdHere2Frnt = edgeToFrnt.getTrgAPID();
                    apIdFrnt2Here = edgeToFrnt.getSrcAPID();
                }
            }

            chainID = chainID + apIdHere2Back + "ap" + apIdHere2Frnt + "_";
            revChainID = leftRevChainID + apIdHere2Frnt + "ap"
			     + apIdHere2Back + "_" + revChainID; 

            // Build the DENOPTIMGraph with edges directed from VA to VB
            if (i == 1) {
                gVertices.add(vertBack);
            }
            gVertices.add(vertHere);
            gEdges.add(new DENOPTIMEdge(vertBack.getAP(apIdBack2Here),
                    vertHere.getAP(apIdHere2Back), vertBack.getVertexId(),
                    vertHere.getVertexId(), apIdBack2Here, apIdHere2Back,
                    edgeToBack.getBondType()));
            if (i == vertPathVAVB.size()-2)
            {
                gVertices.add(vertFrnt);
                gEdges.add(new DENOPTIMEdge(vertHere.getAP(apIdHere2Frnt),
                        vertFrnt.getAP(apIdFrnt2Here), vertHere.getVertexId(),
                        vertFrnt.getVertexId(), apIdHere2Frnt, apIdFrnt2Here,
                        edgeToFrnt.getBondType()));
            }
        }

        // Build the DENOPTIMGraph with edges directed from VA to VB
        this.graph = new DENOPTIMGraph(gVertices,gEdges);

    	// prepare alternative chain IDs
    	String[] pA = chainID.split("_");
    	String[] pB = revChainID.split("_");
    	allPossibleChainIDs.add(chainID);
    	allPossibleChainIDs.add(revChainID);
    	for (int i=1; i<pA.length; i++)
    	{
    	    String altrnA = "";
    	    String altrnB = "";
    	    for (int j=0; j<pA.length; j++)
    	    {
    		if ((i+j) < pA.length)
    		{
                       altrnA = altrnA + pA[i+j] + "_";
                       altrnB = altrnB + pB[i+j] + "_";
    		}
    		else
    		{
    		   altrnA = altrnA + pA[i+j-pA.length] + "_";
                       altrnB = altrnB + pB[i+j-pA.length] + "_";
    		}
    	    }
    	    allPossibleChainIDs.add(altrnA);
    	    allPossibleChainIDs.add(altrnB);
    	}
    }
    
//-----------------------------------------------------------------------------

    /**
     * Creates the molecular representation, list of atoms and bonds involved
     * in the path between the head and tail.
     * @param mol the full molecule
     * @param libScaff the library of scaffolds
     * @param libFrag the library of fragments
     * @param libCap the library of capping groups
     * @param make3D if <code>true</code> makes the method generate the 3D
     * coordinates of the chain of fragments by rototranslation of the
     * 3D fragments so that to align the APvectors
     * @throws DENOPTIMException
     */

    public void makeMolecularRepresentation(IAtomContainer mol,
                                            ArrayList<DENOPTIMVertex> libScaff,
                                            ArrayList<DENOPTIMVertex> libFrag,
                                            ArrayList<DENOPTIMVertex> libCap,
                                            boolean make3D)
                                                       throws DENOPTIMException
    {
        // Build molecular representation 
        TreeBuilder3D tb = new TreeBuilder3D(libScaff,libFrag,libCap);
        iacPathVAVB = tb.convertGraphTo3DAtomContainer(graph);
        // and get the information on APs
        Map<Integer,ArrayList<DENOPTIMAttachmentPoint>> apsPerVertexId =
                                                        tb.getApsPerVertexId();
        Map<DENOPTIMEdge,ArrayList<DENOPTIMAttachmentPoint>> apsPerEdge =
                                                            tb.getApsPerEdge();
        Map<IAtom,ArrayList<DENOPTIMAttachmentPoint>> apsPerAtom =
                                                            tb.getApsPerAtom();
        Map<IBond,ArrayList<DENOPTIMAttachmentPoint>> apsPerBond =
                                                            tb.getApsPerBond();

	if (debug)
	{
	    String f = "pathSubGraph.sdf";
	    System.out.println("Find SDF representation of path in: " + f);
	    DenoptimIO.writeMolecule(f,iacPathVAVB,false);
	}

        // Get shortest atom path between the two ends of the chain
        atomsPathVAVB = new ArrayList<IAtom>();
        try {
            IAtom head = iacPathVAVB.getAtom(0);
            IAtom tail = iacPathVAVB.getAtom(iacPathVAVB.getAtomCount()-1);
            atomsPathVAVB = PathTools.getShortestPath(iacPathVAVB, head, tail);
        } catch (Throwable t) {
            throw new DENOPTIMException("PathTools Exception: " + t);
        }

        // Identify which atoms in mol represent the RCA in the current chain.
        // Since we are looking for the verteces of the RCA atoms
        // there is only one atom per each of the of the two vertexID required
        List<IAtom> ends = new ArrayList<IAtom>();
        for (IAtom atm : mol.atoms())
        {
            int vrtId = (Integer) atm.getProperty(
                                            DENOPTIMConstants.ATMPROPVERTEXID);
            if (vrtId == vertPathVAVB.get(0).getVertexId() ||
                vrtId == vertPathVAVB.get(vertPathVAVB.size()-1).getVertexId())
            {
                ends.add(atm);
            }
        }

        // Get path in mol that corresponds to the shortest path in iacPathVAVB
        // This is done to get the bond properties from mol that is a 
        // fully defined molecule.
        // Note that multiple paths with length equal to the shortest length 
        // are possible if there are rings within fragments (vertices), 
        // but in such case the alternative paths involve only non-rotatable.
        // Therefore, the actual identity of the bond doesn't matter in
        // this particular context.
        List<IAtom> pathInFullMol = new ArrayList<IAtom>();
        try {
            pathInFullMol = PathTools.getShortestPath(mol, 
                                                      ends.get(0), 
                                                      ends.get(1));
        } catch (Throwable t) {
            throw new DENOPTIMException("PathTools Exception: " + t);
        }
        if (pathInFullMol.size() != atomsPathVAVB.size())
        {
            throw new DENOPTIMException("Paths have different size! Unable to "
                        + "proceed in the evaluation of ring closability. "
                        + "Please report this to the author.");
        }
        atmNumStr = " Atm in path: ";
        for (int ia=0; ia<pathInFullMol.size(); ia++)
        {
            IAtom a = pathInFullMol.get(ia);
            if (ia == 0 || ia == pathInFullMol.size()-1)
            {
                atmNumStr = atmNumStr + "RCA-" + (mol.getAtomNumber(a)+1)+"_";
            }
            else
            {
                atmNumStr = atmNumStr + a.getSymbol() + "-"
                                            + (mol.getAtomNumber(a) + 1) + "_";
            }
        }
        if (debug)
        {
            System.out.println("Path goes via " + atmNumStr + "(if unique)");
        }

        // Identify the path of bonds between head and tail
        // This is taken from the fully defined mol to inherit rotatability
        bondsPathVAVB = new ArrayList<IBond>();
        for (int i=0; i < pathInFullMol.size()-1; i++)
        {
            IBond bnd = mol.getBond(pathInFullMol.get(i), 
                                    pathInFullMol.get(i+1));
            bondsPathVAVB.add(bnd);
        }

        // Define points used to calculate dihedral angle of each bond
        // excluding the first and last
        dihedralRefPts = new ArrayList<ArrayList<Point3d>>();
        String keyPropVrtID = DENOPTIMConstants.ATMPROPVERTEXID;
        for (int it=3; it<atomsPathVAVB.size(); it++)
        {
            ArrayList<Point3d> fourPoints = new ArrayList<Point3d>();
            Point3d p0;
            Point3d p1;
            Point3d p2;
            Point3d p3;
            IAtom a0 = atomsPathVAVB.get(it-3);
            IAtom a1 = atomsPathVAVB.get(it-2);
            IAtom a2 = atomsPathVAVB.get(it-1);
            IAtom a3 = atomsPathVAVB.get(it);
            int vIdA0 = (Integer) a0.getProperty(keyPropVrtID);
            int vIdA1 = (Integer) a1.getProperty(keyPropVrtID);
            int vIdA2 = (Integer) a2.getProperty(keyPropVrtID);
            int vIdA3 = (Integer) a3.getProperty(keyPropVrtID);
            IBond bndA1A2 = iacPathVAVB.getBond(a1,a2);

            // trivial for points 1 and 2
            p0 = a0.getPoint3d(); // only initialization
            p1 = a1.getPoint3d();
            p2 = a2.getPoint3d();
            p3 = a3.getPoint3d(); // only initialization

            // chose point 0
            if (vIdA0 != vIdA1)
            {
                // use the point identified by the ap on atom a1 that 
                // has the lowest index in the list of aps on a1, 
                // but is not the AP used to bind a1 and a2
                for (DENOPTIMAttachmentPoint ap : apsPerAtom.get(a1))
                {
                    if (apsPerBond.keySet().contains(bndA1A2))
                    {
                        if (apsPerBond.get(bndA1A2).contains(ap))
                        {
                           continue;
                        }
                    }
                    p0 = new Point3d(ap.getDirectionVector());
                    break;
                }
            }
            else
            {
                // Choose the atom connected to a1 that has the lowest index
                // in the IAtomContainer and is not a2
                List<IAtom> nbrsOfA1 = iacPathVAVB.getConnectedAtomsList(a1);
                int lowIDs = 10000000;
                for (IAtom nbrOfA1 : nbrsOfA1)
                {
                    if (nbrOfA1 == a2)
                    {
                        continue;
                    }
                    int atmID = iacPathVAVB.getAtomNumber(nbrOfA1);
                    if (atmID < lowIDs)
                    {
                        lowIDs = atmID;
                    }
                }
                p0 = new Point3d(iacPathVAVB.getAtom(lowIDs).getPoint3d());
            }

            // choose point 3 (as done for point 0)
            if (vIdA2 != vIdA3)
            {
                // use the point identified by the ap on atom a2 that 
                // has the lowest index in the list of aps on a2, 
                // but is not the AP used to bind a1 and a1
                for (DENOPTIMAttachmentPoint ap : apsPerAtom.get(a2))
                {
                    if (apsPerBond.keySet().contains(bndA1A2))
                    {
                        if (apsPerBond.get(bndA1A2).contains(ap))
                        {
                           continue;
                        }
                    }
                    p3 = new Point3d(ap.getDirectionVector());
                    break;
                }
            }
            else
            {
                // Choose the atom connected to a2 that has the lowest index
                // in the IAtomContainer and is not a1.
                List<IAtom> nbrsOfA2 = iacPathVAVB.getConnectedAtomsList(a2);
                int lowIDs = 10000000;
                for (IAtom nbrOfA2 : nbrsOfA2)
                {
                    if (nbrOfA2 == a1)
                    {
                        continue;
                    }
                    int atmID = iacPathVAVB.getAtomNumber(nbrOfA2);
                    if (atmID < lowIDs)
                    {
                        lowIDs = atmID;
                    }
                }
                p3 = new Point3d(iacPathVAVB.getAtom(lowIDs).getPoint3d());
            }

            fourPoints.add(p0);
            fourPoints.add(p1);
            fourPoints.add(p2);
            fourPoints.add(p3);

            dihedralRefPts.add(fourPoints);
        }

        if (debug)
        {
            System.out.println("Points for dihedral angle definition: ");
            for (ArrayList<Point3d> fp : dihedralRefPts)
            {
                for (Point3d p : fp)
                {
                    System.out.println("  "+p);
                }
            System.out.println("  ");
            }
        }

        // set flag
        hasMolRepr = true;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the string representation of the path
     */

    public String getChainID()
    {
        return chainID;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns all the possible IDs for this chain. The alternatives differ in
     * the position of the chord.
     */

    public ArrayList<String> getAllAlternativeChainIDs()
    {
        return allPossibleChainIDs;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the vertex representing the head of the chain
     */

    public DENOPTIMVertex getHeadVertex()
    {
        return vA;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the vertex representing the tail of the chain
     */

    public DENOPTIMVertex getTailVertex()
    {
        return vB;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the list of verteces involved
     */

    public List<DENOPTIMVertex> getVertecesPath()
    {
        return vertPathVAVB;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the list of edges involved
     */

    public List<DENOPTIMEdge> getEdgesPath()
    {
        return edgesPathVAVB;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns true if the molecular representation has been set
     */

    public boolean hasMolecularRepresentation()
    {
        return hasMolRepr;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the molecular representation 
     */
    public IAtomContainer getMolecularRepresentation()
    {
        return iacPathVAVB;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the list of atoms in the path between the head and the tail
     */

    public List<IAtom> getAtomPath()
    {
        return atomsPathVAVB;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the string with atom symbols and number for an easy
     * identification of the path in a molecular structure
     */

    public String getAtomRefStr()
    {
        return atmNumStr;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the list of bonds in the path between the head and the tail.
     * Note that the <code>IBond</code>s are those of the entire molecule
     * not of the <code>IAtomContainer</code> representing only this path.
     */

    public List<IBond> getBondPath()
    {
        return bondsPathVAVB;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the list of point to be used to define the torsion of a bond
     * uniquely (independently on the substituents present in this chain) as
     * a dihedral angle.
     */

    public ArrayList<ArrayList<Point3d>> getDihedralRefPoints()
    {
        return dihedralRefPts;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the ring closing conformations
     */

    public RingClosingConformations getRCC()
    {
        return rcc;
    }

//-----------------------------------------------------------------------------

    /**
     * Set the ring closing conformations to this object
     */

    public void setRCC(RingClosingConformations rcc)
    {
        this.rcc = rcc;
    }

//-----------------------------------------------------------------------------
}
