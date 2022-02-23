package denoptim.gui;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Function;
import com.google.common.base.Functions;

import denoptim.gui.GraphViewerPanel.JVertex;
import edu.uci.ics.jung.algorithms.layout.SpringLayout2;
import edu.uci.ics.jung.algorithms.layout.util.RandomLocationTransformer;
import edu.uci.ics.jung.graph.Graph;

/**
 * This layout extends the SpringLayout to change its behaviour. The
 * differences are:
 * <ul>
 * <li>the initial position of some nodes can be set and locked,</li>
 * <li>a maximum number of relaxation iteration is set.</li>
 * </ul>
 * 
 * @author Marco Foscato
 */

public class DNPSpringLayout<V, E> extends SpringLayout2<V, E>
{
    private Map<String, Point2D> oldVertexPosition;
    private Dimension oldRange = new Dimension();
    
    private int iteration = 0;
    private int maxIterations = 500;
    private boolean lockInitialPositions = false;
    
//------------------------------------------------------------------------------
      
    @SuppressWarnings("unchecked")
    public DNPSpringLayout(Graph<V, E> g)
    {
        super(g, (Function<E,Integer>)Functions.<Integer>constant(60));
        setForceMultiplier(1);
        setRepulsionRange(200);
    }
    
//------------------------------------------------------------------------------
      
    public void setInitialLocations(Map<String, Point2D> vertexPosition, 
            boolean lock)
    {
        this.lockInitialPositions = lock;
        this.oldVertexPosition = vertexPosition;
        double maxX = -1.0;
        double maxY = -1.0;
        for (Point2D p : vertexPosition.values())
        {
            if (p.getX() > maxX)
                maxX = p.getX();

            if (p.getY() > maxY)
                maxY = p.getY();
        }
        this.oldRange.width = (int) maxX;
        this.oldRange.height = (int) maxY;
    }
    
//------------------------------------------------------------------------------
      
    @Override
    public void initialize() {
        super.initialize();
        if (oldVertexPosition != null)
        {
            // Defines the initial position of the vertexes according to the
            // Point2D stored in the local field, which comes from a previous
            // visualisation of a previous version of this graph.
            setInitializer(new RecreateKnownPositions(oldVertexPosition));
            
            if (lockInitialPositions)
            {
                // Freeze/lock the position of those nodes with known position
                for (V v : this.graph.getVertices())
                {
                    if (v instanceof JVertex)
                    {
                        if (oldVertexPosition.containsKey(((JVertex) v).idStr))
                            lock(v,true);
                    }
                }
            }
        }
    }
    
//------------------------------------------------------------------------------
      
    private class RecreateKnownPositions implements Function<V, Point2D> 
    {
        private Map<String, Point2D> vertexPosition;
        private RandomLocationTransformer<V> randomPosition = 
                new RandomLocationTransformer<>(new Dimension(oldRange));
        
        public RecreateKnownPositions(Map<String, Point2D> vertexPosition)
        {
            this.vertexPosition = vertexPosition;
        }

        @Override
        public Point2D apply(V v)
        {
            if (v instanceof JVertex)
            {
                Point2D p = vertexPosition.get(((JVertex)v).idStr);
                if (p != null)
                    return p;
            }
            return randomPosition.apply(v);
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the current value of the position of the given vertex.
     * @param vertex the vertex to get the position of.
     * @return the position of the given vertex.
     * @throws ExecutionException
     */
    public Point2D getVertexPosition(V vertex) throws ExecutionException
    {
        return locations.get(vertex);
    }

//------------------------------------------------------------------------------
    
    /**
     * Relaxation step. Moves all nodes
     */
    @Override
    public void step() {
        super.step();
        iteration++;
        testTermination();
    }
    
//------------------------------------------------------------------------------
    
    private void testTermination()
    {
        if (iteration > maxIterations)
        {
            done = true;
        }        
    } 
    
//------------------------------------------------------------------------------

}
