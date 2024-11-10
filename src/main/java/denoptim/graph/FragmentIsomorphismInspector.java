package denoptim.graph;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jgrapht.GraphMapping;
import org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;

/**
 * <p>Inspector for isomorphism on fragments. The graph representation considered
 * is one where both atoms and {@link AttachmentPoint}s are represented as 
 * graph nodes, and edges are either bonds or atom-to-attachment point 
 * connections.</p>
 * <p>Effectively, this class considers the composition and connectivity 
 * (including that of {@link AttachmentPoint}s when comparing the fragments.
 * Geometry and stereochemistry are not considered.</p>
 * <p>This works with the {@link VF2GraphIsomorphismInspector}, see the
 * dedicated documentation for information of pathological cases.</p>
 * <p></p>
 * 
 * @author Marco Foscato
 */
public class FragmentIsomorphismInspector
{
    /**
     * The maximum time we give the VF2 algorithm (seconds)
     */
    private int timeout;
    
    /**
     * One fragment being analyzed
     */
    private Fragment fragA;

    /**
     * The other fragment being analyzed
     */
    private Fragment fragB;
    
    /**
     * Flag indicating if we print earning in case of timeout or not
     */
    protected boolean reportTimeoutIncidents = true;
    
    /**
     * Implementation of the Vento-Foggia 2 algorithm.
     */
    VF2GraphIsomorphismInspector<FragIsomorphNode, FragIsomorphEdge> vf2;

    
//------------------------------------------------------------------------------
      
    /**
     * Constructs a default inspector with a timeout runtime of 60 seconds.
     * @param fragA
     * @param fragB
     */
    public FragmentIsomorphismInspector(Fragment fragA, Fragment fragB)
    {
        this(fragA, fragB, 60000, false);
    }
      
//------------------------------------------------------------------------------
    
    /**
     * Constructs a default inspector with a timeout runtime of 60 seconds.
     * @param fragA
     * @param fragB
     * @param ignoreAPClasses
     */
    public FragmentIsomorphismInspector(Fragment fragA, Fragment fragB,
            boolean ignoreAPClasses)
    {
        this(fragA, fragB, 60000, ignoreAPClasses);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Constructs  an inspector with a custom timeout limit.
     * @param fragA one fragment to compare
     * @param fragB the other fragment to compare
     * @param timeout maximum amount of time (milliseconds) given to the 
     * inspector
     * for performing any task. associated with returning from any public 
     * method in this class.
     */
    public FragmentIsomorphismInspector(Fragment fragA, Fragment fragB,
            int timeout, boolean ignoreAPClasses)
    {
        this.timeout = timeout;
        Comparator<FragIsomorphNode> vComp = new Comparator<FragIsomorphNode>() 
        {
            @Override
            public int compare(FragIsomorphNode n1, FragIsomorphNode n2)
            {
                if (ignoreAPClasses)
                {
                    String tmpLbl1 = "ap";
                    String tmpLbl2 = "ap";
                    if (n1.isAtm)
                        tmpLbl1 = n1.label;
                    if (n2.isAtm)
                        tmpLbl2 = n2.label;
                    return tmpLbl1.compareTo(tmpLbl2);
                } else {
                    return n1.label.compareTo(n2.label);
                }
            }
        };
        
        Comparator<FragIsomorphEdge> eComp = new Comparator<FragIsomorphEdge>() 
        {
            @Override
            public int compare(FragIsomorphEdge e1, FragIsomorphEdge e2)
            {
                return e1.label.compareTo(e2.label);
            }
        };
        
        vf2 = new VF2GraphIsomorphismInspector<>(
                        fragA.getJGraphFragIsomorphism(), 
                        fragB.getJGraphFragIsomorphism(), vComp, eComp);
        this.fragA = fragA;
        this.fragB = fragB;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks if an isomorphism exists between the two fragments.
     * @return <code>true</code> is an isomorphism has been found.
     */
    public boolean isomorphismExists()
    {
        boolean result = false;
        final ExecutorService service = Executors.newSingleThreadExecutor();
        Future<Boolean> future = null;
        try {
            future = service.submit(() -> {
                return vf2.isomorphismExists();
            });
            result = future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            if (reportTimeoutIncidents)
            {
                String fileName = "denoptim_isomorphism_timedout_case.sdf";
                File file = new File(fileName);
                System.err.println("WARNING: timeout reached when attempting "
                    + "detection of isomerism between fragments saved to '"
                    + file.getAbsolutePath() + "'. "
                    + "When timeout is reaches, fragments are"
                    + "considered to be non-isomorphic.");
                List<Vertex> frags = new ArrayList<Vertex>();
                frags.add(fragA);
                frags.add(fragB);
                try
                {
                    DenoptimIO.writeVertexesToSDF(new File(fileName), frags, true);
                } catch (DENOPTIMException e1)
                {
                    System.err.println("WARNING: could not write to '" 
                            + fileName + "'. Reporting fragments to STDERR:."
                            + System.getProperty("line.separator") 
                            + fragA.toJson()
                            + System.getProperty("line.separator") 
                            + fragB.toJson());
                }
            }
            future.cancel(true);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            service.shutdown();
        }
        return result;
    }
    
//------------------------------------------------------------------------------
    
    public Iterator<GraphMapping<FragIsomorphNode, FragIsomorphEdge>> getMappings()
    {
        Iterator<GraphMapping<FragIsomorphNode, FragIsomorphEdge>> mapping = null;
        final ExecutorService service = Executors.newSingleThreadExecutor();
        Future<Iterator<GraphMapping<FragIsomorphNode, FragIsomorphEdge>>> fut =
                null;
        try {
            fut = service.submit(() -> {
                return vf2.getMappings();
            });
            mapping = fut.get(timeout, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            if (reportTimeoutIncidents)
            {
                String fileName = "denoptim_isomorphism_timedout_case.sdf";
                File file = new File(fileName);
                System.err.println("WARNING: timeout reached when attempting "
                    + "detection of isomerism between fragments saved to '"
                    + file.getAbsolutePath() + "'. "
                    + "When timeout is reaches, fragments are"
                    + "considered to be non-isomorphic.");
                List<Vertex> frags = new ArrayList<Vertex>();
                frags.add(fragA);
                frags.add(fragB);
                try
                {
                    DenoptimIO.writeVertexesToSDF(new File(fileName), frags, true);
                } catch (DENOPTIMException e1)
                {
                    System.err.println("WARNING: could not write to '" 
                            + fileName + "'. Reporting fragments to STDERR:."
                            + System.getProperty("line.separator") 
                            + fragA.toJson()
                            + System.getProperty("line.separator") 
                            + fragB.toJson());
                }
            }
            fut.cancel(true);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            service.shutdown();
        }
        return mapping;
    }
    
//------------------------------------------------------------------------------    
    
}
