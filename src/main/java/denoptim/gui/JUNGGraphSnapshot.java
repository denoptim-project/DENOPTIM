/*
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.gui;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import denoptim.gui.GraphViewerPanel.JEdge;
import denoptim.gui.GraphViewerPanel.JVertex;
import denoptim.gui.GraphViewerPanel.LabelType;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.graph.Graph;

/**
 * This class collects information on how a graph was displayed in a JUNG
 * visialisation server (i.e., node positions and visible labels).
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
    
    /**
     * Positions of nodes
     */
    Map<String,Point2D> vertexPosition = new HashMap<String,Point2D>();
    
//-----------------------------------------------------------------------------
    
    public JUNGGraphSnapshot(Graph<JVertex, JEdge> graph, 
            DNPSpringLayout<JVertex, JEdge> layout)
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
            try
            {
                Point2D p = layout.getVertexPosition(jv);
                vertexPosition.put(jv.idStr, new Point2D.Double(p.getX(),p.getY()));
            } catch (ExecutionException e)
            {
                //e.printStackTrace();
            }
        } 
    }
    
//-----------------------------------------------------------------------------
    
    public JUNGGraphSnapshot(Graph<JVertex, JEdge> graph, 
            ISOMLayout<JVertex, JEdge> layout)
    {   
        for (JVertex jv : graph.getVertices())
        {
            Point2D p = layout.apply(jv);
            vertexPosition.put(jv.idStr, new Point2D.Double(p.getX(),p.getY()));
        } 
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
