package gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.uci.ics.jung.graph.Graph;
import gui.GraphViewerPanel2.JEdge;
import gui.GraphViewerPanel2.JVertex;
import gui.GraphViewerPanel2.LabelType;

/**
 * This class collects information on how a graph was displayed in a JUNG
 * viewer (i.e., node positions and visible labels).
 */

public class JUNGGraphSnapshot
{
    /**
     * The collection of vertexes with labels (by label type)
     */
    private Map<LabelType,ArrayList<String>> vertexesWithLabels =
                    new HashMap<LabelType,ArrayList<String>>();
    
    /**
     * The collection of edges with labels (by label type)
     */
    private Map<LabelType,ArrayList<String>> edgesWithLabels =
                    new HashMap<LabelType,ArrayList<String>>();
    
//-----------------------------------------------------------------------------
    
    public JUNGGraphSnapshot(Graph<JVertex, JEdge> graph)
    {
        for (JEdge je : graph.getEdges())
        {
            if (je.displayAPCs)
            {
                processEdge(je,LabelType.APC);
            }   
        }
        
        for (JEdge je : graph.getEdges())
        {
            if (je.displayBndTyp)
            {
                processEdge(je,LabelType.BT);
            }   
        }
        
        for (JVertex jv : graph.getVertices())
        {
            if (jv.displayBBID)
            {
                processVertex(jv,LabelType.BBID);
            }   
        }
        
        //TODO-GG
        System.out.println("HERE_V: "+vertexesWithLabels.keySet());
        System.out.println("HERE_E: "+edgesWithLabels.keySet());
    }
    
//-----------------------------------------------------------------------------
    
    private void processVertex(JVertex v, LabelType lt)
    {
        if (vertexesWithLabels.containsKey(lt))
        {
            vertexesWithLabels.get(lt).add(v.idStr);
        } else {
            ArrayList<String> lst = new ArrayList<String>();
            lst.add(v.idStr);
            vertexesWithLabels.put(lt, lst);
        }
    }
    
//-----------------------------------------------------------------------------
    
    private void processEdge(JEdge e, LabelType lt)
    {
        if (edgesWithLabels.containsKey(lt))
        {
            edgesWithLabels.get(lt).add(e.id);
        } else {
            ArrayList<String> lst = new ArrayList<String>();
            lst.add(e.id);
            edgesWithLabels.put(lt, lst);
        }
    }
    
//-----------------------------------------------------------------------------

    public ArrayList<String> getVertexeIDsWithLabel(LabelType labelName)
    {
        if (vertexesWithLabels.containsKey(labelName))
            return vertexesWithLabels.get(labelName);
        else
            return new ArrayList<String>();
    }
    
//-----------------------------------------------------------------------------

    public ArrayList<String> getEdgeIDsWithLabel(LabelType labelName)
    {
        if (edgesWithLabels.containsKey(labelName))
            return edgesWithLabels.get(labelName);
        else
            return new ArrayList<String>();
    }
 
//-----------------------------------------------------------------------------

}
