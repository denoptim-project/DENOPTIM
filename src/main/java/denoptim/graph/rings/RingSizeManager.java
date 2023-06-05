package denoptim.graph.rings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openscience.cdk.Bond;
import org.openscience.cdk.graph.matrix.TopologicalMatrix;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.graph.APClass;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.DGraph;
import denoptim.graph.Edge;
import denoptim.graph.EmptyVertex;
import denoptim.graph.Fragment;
import denoptim.graph.Ring;
import denoptim.graph.Vertex;
import denoptim.graph.Edge.BondType;
import denoptim.utils.MoleculeUtils;

/**
 * Utility class to calculate and manage the alternative ring sizes 
 * achievable by formation of {@link Ring}s.
 */

class RingSizeManager
{
    /**
     * Molecular representation of the current system
     */
    private IAtomContainer mol;

    /** 
     * Topological matrix: contains the number of bonds (shortest path) 
     * separating the two atoms having index i and j in the current system
     */
    private int[][] topoMat;
    
    /** 
     * List of Ring Closing Vertices (RCV as DENOPTIMVerex) each containing an 
     * available Ring Closing Attractor (RCA)
     */
    private ArrayList<Vertex> lstVert;
    
    /**
     * Size of the list of available RCAs
     */
    private int sz;

    /**
     * Compatibility matrix between pairs of RCAs in the current system.
     */
    private boolean[][] compatibilityOfPairs;

    /**
     * List of weight factors used to control the likeliness of choosing 
     * rings of a given size for the current system
     */
    private List<Double> weigths;

    /**
     * List of flags defining if an RCA has been "used", i.e., not used to
     * make an actual chord, but used to make a plan to make a chord and
     * update the molecular representation accordingly.
     */
    private List<Boolean> done;

    /**
     * Map linking the list of vertices and atoms
     */
    private Map<Vertex,ArrayList<Integer>> vIdToAtmId;

    /**
     * Parameters setting the bias for selecting rings of given size
     */
    private List<Integer> ringSizeBias;
    
    /**
     * Definition of the fragment space
     */
    private FragmentSpace fragSpace = null;
    
    /**
     * Parameters related to rings
     */
    private RingClosureParameters settings;
    
    /**
     * tool managing the logs
     */
    private Logger logger;
    
    /**
     * New line character
     */
    private static final String NL = DENOPTIMConstants.EOL; 

//-----------------------------------------------------------------------------

    /**
     * Creates a tool that will work according to the given system-aspecific 
     * settings
     * @param fragSpace the space of building blocks.
     * @param rcParams the parameters pertaining ring closures.
     */
    public RingSizeManager(FragmentSpace fragSpace, 
            RingClosureParameters rcParams)
    {
        this.fragSpace = fragSpace;
        this.settings = rcParams;
        this.logger = rcParams.getLogger();
        this.ringSizeBias = rcParams.getRingSizeBias();
    }

//-----------------------------------------------------------------------------

    /**
     * Makes this ring size manager work on a specific system that has a 
     * molecular representation and a DENOPTIM's graph representation.
     * @param origMol the molecular representation.
     * @param graph the graph representation.
     * @throws DENOPTIMException
     */
    public void initialize(IAtomContainer origMol, DGraph graph)
    {
        try
        {
            mol = (IAtomContainer) origMol.clone();
        } catch (Throwable t) {
            throw new IllegalArgumentException(t);
        }

        // Get the list of available RCAs
        lstVert = graph.getFreeRCVertices();
        sz = lstVert.size();

        // Define link between list of vertices and list of atoms
        vIdToAtmId = MoleculeUtils.getVertexToAtomIdMap(lstVert, mol);

        // Build topological matrix
        fillTopologicalMatrix(); 

        // Define compatibility of RCA pairs and weight factors
        calculateCompatibilityOfAllRCAPairs();

        // Initialize vector of flags
        done = new ArrayList<Boolean>(Collections.nCopies(sz,false));
    }

//-----------------------------------------------------------------------------

    // NOTE: we don't really need the whole topological matrix, so the 
    // calculation of the shortest paths between the RCAs could be more
    // efficient

//TODO evaluate use of ShortestPath to fill only certain entries of topoMat

    private void fillTopologicalMatrix()
    {
        long startTime = System.nanoTime();
        topoMat = TopologicalMatrix.getMatrix(mol);
        long endTime = System.nanoTime();
        long duration = (- startTime + endTime) / (long) 1000.0;

        if (logger.isLoggable(Level.FINE))
        {
            StringBuilder sb = new StringBuilder();
            sb.append("TopoMat N: " + mol.getAtomCount() + " " + duration 
                    + " microsec." + NL);
            int n = mol.getAtomCount();
            sb.append("Topological matrix (n=" + n + ")"+NL);
            for (int i=0; i<n; i++)
            {
                String l = " ";
                for (int j=0; j<n; j++)
                {
                    l = l + " " + topoMat[i][j];
                }
                sb.append(l+NL);
            }
            logger.log(Level.FINE, sb.toString());
        }
    }

//-----------------------------------------------------------------------------

    // NOTE: this method considers the ring size bias and sets also 
    // the weight factors

    private void calculateCompatibilityOfAllRCAPairs()
    {
        weigths = new ArrayList<Double>(Collections.nCopies(sz, 0.0));
        compatibilityOfPairs = new boolean[sz][sz];
        for (int i=0; i<sz; i++)
        {
            Vertex vI = lstVert.get(i);
            IAtom atmI = null;
            RingClosingAttractor rcaI = null;
            boolean isAtmI = vI instanceof Fragment;
            if (isAtmI)
            {
                atmI = mol.getAtom(vIdToAtmId.get(vI).get(0));
            
                // Dealing with the possibility that RCV is the scaffold
                if (mol.getConnectedAtomsList(atmI).size()==0)
                    continue;
                
                rcaI = new RingClosingAttractor(atmI,mol);
                if (!rcaI.isAttractor())
                {
                    String s = "Attempt to evaluate RCA pair compatibility "
                               + "with a non-RCA end (" + atmI + ").";
                    throw new IllegalStateException(s);
                }
            }   
            
            for (int j=i+1; j<sz; j++)
            {
                Vertex vJ = lstVert.get(j);
                IAtom atmJ = null;
                RingClosingAttractor rcaJ = null;
                boolean isAtmJ = vJ instanceof Fragment;
                if (isAtmJ)
                {
                    atmJ = mol.getAtom(vIdToAtmId.get(vJ).get(0));
                    
                    rcaJ = new RingClosingAttractor(atmJ,mol);
                    if (!rcaJ.isAttractor())
                    {
                        String s = "Attempt to evaluate RCA pair compatibility "
                                   + "with a non-RCA end (" + atmJ + ").";
                        throw new IllegalStateException(s);
                    }
                }
                
                if ((isAtmI && !isAtmJ) || (!isAtmI && isAtmJ))
                {
                    continue;
                }
                if (!isAtmI && !isAtmJ)
                {
                    // Ring size is ignored when RCVs are empty vertexes
                    compatibilityOfPairs[i][j] = true;
                    compatibilityOfPairs[j][i] = true;
                    weigths.set(i, weigths.get(i) + 1.0);
                    weigths.set(j, weigths.get(j) + 1.0);
                    continue;
                }
                
                if (rcaI.isCompatible(rcaJ) 
                        && evaluateRCVPair(vI,vJ,fragSpace))
                {
                    //TODO: evaluate the use of ShortestPath instead of this
                    int ringSize = topoMat[vIdToAtmId.get(vI).get(0)]
                                          [vIdToAtmId.get(vJ).get(0)] - 1;
                    int szFct = 0;
                    if (ringSize < settings.getMaxRingSize())
                    {
                        szFct = ringSizeBias.get(ringSize);
                    }
                    if (szFct > 0)
                    {
                        compatibilityOfPairs[i][j] = true;
                        compatibilityOfPairs[j][i] = true;
                        weigths.set(i, weigths.get(i) + szFct);
                        weigths.set(j, weigths.get(j) + szFct);
                    }

                    logger.log(Level.FINE, " i:" + i + " j:" + j + " size:" 
                            + ringSize + " factors:" + weigths);
                }
            }
        }
        
        if (logger.isLoggable(Level.FINE))
        {
            StringBuilder sb = new StringBuilder();
            sb.append("RCV pairs compatibility (ring size-biased):"+NL);
            for (int i=0; i<sz;i++)
            {
                String l = " ";
                for (int j=0; j<sz; j++)
                {
                    String p = "0";
                    if (compatibilityOfPairs[i][j])
                        p = "1";
                    l = l + " " + p;
                }
                sb.append(l+NL);
            }
            logger.log(Level.FINE, sb.toString());
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Method to analyze a pair of vertices and evaluate whether they respect 
     * the criteria for being the two ends of a candidate closable chain: a 
     * pair of Ring Closing Vertices (RCV).
     *
     * @param vI first vertex
     * @param vJ second vertex
     * @return <code>true</code> is the path satisfies the criteria
     */

    private boolean evaluateRCVPair(Vertex vI, Vertex vJ, 
            FragmentSpace fragSpace)
    {
        String s = "Evaluation of RCV pair " + vI + " " + vJ + ": ";

        // Get details on the first vertex (head)
        Edge edgeI = vI.getEdgeToParent();
        int srcApIdI = edgeI.getSrcAPID();
        Vertex pvI = vI.getParent();
        AttachmentPoint srcApI = pvI.getAttachmentPoints().get(srcApIdI);
        int srcAtmIdI = srcApI.getAtomPositionNumber();
        APClass parentAPClsI = edgeI.getSrcAPClass();

        // Get details on the second vertex (tail)
        Edge edgeJ = vJ.getEdgeToParent();
        int srcApIdJ = edgeJ.getSrcAPID();
        Vertex pvJ = vJ.getParent();
        AttachmentPoint srcApJ =pvJ.getAttachmentPoints().get(srcApIdJ);
        int srcAtmIdJ = srcApJ.getAtomPositionNumber();
        APClass parentAPClsJ = edgeJ.getSrcAPClass();
        
        // exclude if no entry in RC-Compatibility map
        if (!fragSpace.getRCCompatibilityMatrix().containsKey(parentAPClsI))
        {
            logger.log(Level.FINE, s + "RC-CPMap does not contain class (I) "
                        + parentAPClsI + " " + parentAPClsI.hashCode());
            return false;
        }
        ArrayList<APClass> compatClassesI = fragSpace.getRCCompatibilityMatrix()
                .get(parentAPClsI);

        // exclude if no entry in RC-Compatibility map
        if (!fragSpace.getRCCompatibilityMatrix().containsKey(parentAPClsJ))
        {
            logger.log(Level.FINE, s + "RC-CPMap does not contain class (J) "
                        + parentAPClsJ + " " + parentAPClsJ.hashCode());
            return false;
        }
        ArrayList<APClass> compatClassesJ = fragSpace.getRCCompatibilityMatrix()
                .get(parentAPClsJ);

        // exclude loops included within a single vertex 
        if (vI == vJ)
        {
            logger.log(Level.FINE, s + "vI same as vJ: loop not allowed!");
            return false;
        }

        // exclude pairs of RCA-vertices having same src atom
        if (pvI == pvJ && srcAtmIdI == srcAtmIdJ)
        {
            logger.log(Level.FINE, s + "Same src: " + pvI + " " + pvJ
                                  + " " + srcAtmIdI + " " + srcAtmIdJ);
            return false;
        }

        // exclude paths that do not connect APClass compatible ends
        // NOTE that in ring closures the CPMap is symmetric, this
        // also implies that CPMap for ring closure may be different
        // from standard CPMap        
        if (!(compatClassesI.contains(parentAPClsJ) ||
              compatClassesJ.contains(parentAPClsI)))
        {
            logger.log(Level.FINE,s + "APClass not compatible "
                                + parentAPClsJ);
            return false;
        }

        logger.log(Level.FINE, s + "all criteria satisfied.");
        
        return true;
    }

//-----------------------------------------------------------------------------

    public List<Vertex> getRSBiasedListOfCandidates()
    {
        List<Vertex> wLst = new ArrayList<Vertex>();
        for (int i=0; i<sz; i++)
        {
            if (done.get(i))
            {
                continue;
            }
            Vertex v = lstVert.get(i);
            for (int j=0; j<weigths.get(i); j++)
            {
                wLst.add(v);
            }
        }

        logger.log(Level.FINE, "Ring size-biased list of RCVs:" + wLst);
        
        return wLst;
    }

//-----------------------------------------------------------------------------

    public List<Vertex> getRSBiasedListOfCandidates(Vertex vI)
    {
        int i = lstVert.indexOf(vI);
        List<Vertex> wLst = new ArrayList<Vertex>();
        for (int j=0; j<sz; j++)
        {
            if (done.get(j) || !compatibilityOfPairs[i][j])
            {
                continue;
            }

            Vertex vJ = lstVert.get(j);
            
            if (vI instanceof EmptyVertex && vJ instanceof EmptyVertex)
            {
                wLst.add(vJ);
                continue;
            }

            int ringSize = topoMat[vIdToAtmId.get(vI).get(0)]
                                  [vIdToAtmId.get(vJ).get(0)] - 1;
            
            // The likeliness of picking this RCV is given by the ring-size
            // factor, which accounts for the requested bias towards specific
            // ring sizes. The larger the bias, the higher the number of
            // slots occupied by vJ in the list of possible partners, thus
            // the larger the likeliness of picking it when randomly
            // choosing the partner for vI.
            int szFct = 0; //This is the ring-size factor
            if (ringSize < settings.getMaxRingSize())
            {
                szFct = ringSizeBias.get(ringSize);
            }
            
            // TODO: here we could consider biasing against crowding, but
            // the parameters defining what crowding is are defined in 
            // GAParameters, so we would loose generality of this class.
            
            for (int z=0; z<szFct; z++)
            {
                wLst.add(vJ);
            }
        }

        logger.log(Level.FINE, "Ring size-biased list of RCVs for " + vI 
                + ": " + wLst);

        return wLst;
    }

//-----------------------------------------------------------------------------

    public void addRingClosingBond(Vertex vI, Vertex vJ)
    {
        // Check validity of assumption: RCV contain only one IAtom
        if (vIdToAtmId.get(vI).size()!=1 || vIdToAtmId.get(vJ).size()!=1)
        {
            String s = "Attempt to make ring closing bond between "
                       + "multi-atom Ring Closing Vertices (RCV). "
                       + "For now, only single-atom RCVs are expected in "
                       + "this implementation.";
            throw new IllegalStateException(s);
        }

        // Identify atoms to be bound and make the ring closing bond
        IAtom srcI = mol.getConnectedAtomsList(
                           mol.getAtom(vIdToAtmId.get(vI).get(0))).get(0);
        IAtom srcJ = mol.getConnectedAtomsList(
                           mol.getAtom(vIdToAtmId.get(vJ).get(0))).get(0);

        // Assuming that APClasses on both sides agree on bond order
        // and that vI and vJ are proper RCVs
        
        BondType bndTyp = vI.getEdgeToParent().getBondType();
        if (bndTyp.hasCDKAnalogue())
        {
            IBond bnd = new Bond(srcI,srcJ);
            bnd.setOrder(bndTyp.getCDKOrder());
            mol.addBond(bnd);
        } else {
            logger.log(Level.FINE,"WARNING! Attempt to add ring closing bond "
                    + "did not add any actual chemical bond because the "
                    + "bond type of the chord is '" + bndTyp +"'.");
        }
        
        logger.log(Level.FINEST, " ==> UPDATING " 
                + this.getClass().getSimpleName() + " <==");

        // Update this RingSizeManager
        fillTopologicalMatrix();
        calculateCompatibilityOfAllRCAPairs();
    }

//-----------------------------------------------------------------------------

    public void setVertexAsDone(Vertex v)
    {
        done.set(lstVert.indexOf(v),true);
    }

//-----------------------------------------------------------------------------

    public boolean getCompatibilityOfPair(Vertex vI, Vertex vJ)
    {
        int i = lstVert.indexOf(vI); 
        int j = lstVert.indexOf(vJ);
        return compatibilityOfPairs[i][j];
    }

//---------------------------------------------------------------------
}
