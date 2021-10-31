/*
 *   DENOPTIM
 *   Copyright (C) 2020 Marco Foscato <marco.foscato@uib.no>
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

package gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;

import com.google.common.base.Function;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.molecule.APClass;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMRing;
import denoptim.molecule.DENOPTIMTemplate;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.EmptyVertex;
import denoptim.molecule.SymmetricSet;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.molecule.DENOPTIMVertex.BBType;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.GraphMouseListener;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.picking.PickedInfo;
import edu.uci.ics.jung.visualization.renderers.BasicEdgeArrowRenderingSupport;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;


/**
 * A Panel that holds the JUNG representation of a graph.
 * 
 * @author Marco Foscato
 */
public class GraphViewerPanel2 extends JPanel 
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 2L;
	
	Graph<JVertex, JEdge> loadedGraph;
	VisualizationViewer<JVertex, JEdge>  viewer;
	private DefaultModalGraphMouse<JVertex, JEdge> gm;
	
	public enum LabelType {APC, BT, BBID};
	
	public static final String SPRITEATT_UICLASS_APCLASSSRC = "apLabelSRC";
	public static final String SPRITEATT_UICLASS_APCLASSTRG = "apLabelTRG";
	public static final String SPRITEATT_UICLASS_BNDORD = "bndTypLabel";
	public static final String SPRITEATT_UICLASS_FRGID = "molIdLabel";
	public static final String SPRITEATT_UICLASS_APID = "apIdLabel";
	
    public enum JVertexType {SCAF, FRAG, CAP, RCV, AP, NONE};
    
//------------------------------------------------------------------------------
    
    /**
     * a vertex in the JUNG Graph. It represents any kind of 
     * {@link DENOPTIMVertex} or an {@link DENOPTIMAttachmentPoint}.
     */
    public class JVertex 
    {
        /**
         * The reference to the corresponding {@link DENOPTIMVertex} or null.
         */
        DENOPTIMVertex dnpVertex;
        
        /**
         * The reference to the corresponding {@link DENOPTIMAttachmentPoint}
         * or null.
         */
        DENOPTIMAttachmentPoint ap;
        
        /**
         * A shortcut to record which type of DENOPTIM object this vertex 
         * represents.
         */
        JVertexType vtype = JVertexType.NONE;
        
        /**
         * The string used as label when graphycally depicting this vertex.
         */
        String idStr = "nan";
        
        /**
         * Flag requiring to display building block ID
         */
        boolean displayBBID = false;
        
        /**
         * Flag enabling opening vertex inner view (i.e., expand templates)
         * in graph viewer
         */
        boolean expandable = false;
        
        /**
         * Reference to the {@link JEdge} linking this vertex to its parent 
         * vertex. This is non-null only for vertexes that represent APs.
         */
        JEdge edgeToParent;
        
        /**
         * Constructor for vertex that represents a given 
         * {@link DENOPTIMAttachmentPoint}. Note that the reference to the JEdge
         * linking this JVertex to its parent is set when creating the JEdge
         * {@link GraphViewerPanel2#convertDnGraphToGSGraph(DENOPTIMGraph, DENOPTIMTemplate)}
         * @param ap the {@link DENOPTIMAttachmentPoint}.
         */
        public JVertex(DENOPTIMAttachmentPoint ap) {
            this.ap = ap;
            idStr = Integer.toString(ap.getID());
            vtype = JVertexType.AP;
        }
        
        /**
         * Constructor for vertex that represents a given {@link DENOPTIMVertex}.
         * @param ap the {@link DENOPTIMVertex}.
         */
        public JVertex(DENOPTIMVertex v) {
            this.dnpVertex = v;
            this.expandable = true;
            idStr = Integer.toString(v.getVertexId());
            switch (v.getBuildingBlockType())
            {
                case SCAFFOLD:
                    vtype = JVertexType.SCAF;
                    break;
                case FRAGMENT:
                    vtype = JVertexType.FRAG;
                    break;
                case CAP:
                    vtype = JVertexType.CAP;
                    break;
                default:
                    vtype = JVertexType.NONE;
                    break;
            }
            if (v.isRCV())
                vtype = JVertexType.RCV;
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * An edge in the JUNG Graph. It represents any kind of 
     * {@link DENOPTIMEdge}, a chord that closes a {@link DENOPTIMRing},
     * or a free {@link DENOPTIMAttachmentPoint}.
     */
    public class JEdge 
    {
        /**
         * String representing the {@link APClass} on the source AP of 
         * directed edges or on the AP this edge represents.
         */
        String srcAPC;

        /**
         * String representing the {@link APClass} on the target AP of 
         * directed edged this JUNG edge represents.
         */
        String trgAPC;
        
        /**
         * The bond type for edges that correspond to connections between 
         * {@link DENOPTIMVertex}s or <code>"none"</code> when for edges
         * representing {@link DENOPTIMAttachmentPoint}s.
         */
        String bt = "none";
        
        /**
         * Flag defining whether this edge is representing a
         * {@link DENOPTIMAttachmentPoint}.
         */
        boolean toAp = false;
        
        /**
         * Flag requiring to display APClasses
         */
        boolean displayAPCs = false;
        
        /**
         * Flag requiring to display Bond type
         */
        boolean displayBndTyp = false;
   
        /**
         * Constructor for a JUNG edge representing a 
         * {@link DENOPTIMAttachmentPoint}.
         * @param srcAPC the string representation of the {@link APClass} of the
         * AP.
         */
        public JEdge(String srcAPC) {
            this(srcAPC,"none","none");
        }
        
        /**
         * Constructor for a JUNG edge representing a {@link DENOPTIMEdge} or
         * a chord that closes a {@link DENOPTIMRing}.
         * @param srcAPC the string representation of the {@link APClass} of the
         * source AP of directed {@link DENOPTIMEdge}s.
         * @param trgAPC the string representation of the {@link APClass} of the
         * target AP of directed {@link DENOPTIMEdge}s.
         * @param bt the string representation of the bond type in the 
         * {@link DENOPTIMEdge} represented by this JUNG edge.
         */
        public JEdge(String srcAPC, String trgAPC, String bt){
            this.srcAPC = srcAPC;
            this.trgAPC = trgAPC;
            this.bt = bt;
        }
    }
	
//-----------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	public GraphViewerPanel2()
	{
		super();
		initialize();
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Builds the initialized component
	 */
	private void initialize()
	{
		this.setLayout(new BorderLayout());
		this.setBackground(Color.decode("#D9D9D9"));
		this.setToolTipText("No graph to visualize");
		
		//TODO-GG del
		/*
		prepareFragSpace();
        DENOPTIMGraph dnpGraph = null;
        try {
            dnpGraph = makeTestGraphD();
        } catch (DENOPTIMException e) {
            System.out.println("Could not make DENOPTIM graph");
            e.printStackTrace();
        }
        loadGraphToViewer(dnpGraph);
		*/
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Removes the currently loaded graph viewer
	 */
	public void cleanup()
	{
	    if (viewer != null)
	    {
	        viewer.removeAll();
	    }
	    for (Component c : this.getComponents())
		{
			this.remove(c);
		}
		this.repaint();
		this.revalidate();
	}
	
//-----------------------------------------------------------------------------

    /**
     * Load the given {@link DENOPTIMGraph} to this graph viewer.
     * @param dnGraph the graph to load.
     */
    public void loadGraphToViewer(DENOPTIMGraph dnGraph)
    {
        loadGraphToViewer(convertDnGraphToGSGraph(dnGraph),null);
    }
    
//-----------------------------------------------------------------------------

    /**
     * Load the {@link DENOPTIMGraph} contained in a {@link DENOPTIMTemplate}
     * into the graph viewer. 
     * @param tmpl the template containing the graph to visualise.
     */
    public void loadGraphToViewer(DENOPTIMTemplate tmpl)
    {
        Graph<JVertex, JEdge> graph = convertDnGraphToGSGraph(
                tmpl.getInnerGraph(), tmpl);
        loadGraphToViewer(graph,null);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Load the given {@link DENOPTIMGraph} to the graph viewer.
     * @param dnGraph the graph to load.
     * @param prevStatus the snapshot of the previous status. We use this to 
     * remember previously chosen settings, such as the labels to be 
     * displayed, or the position of nodes.
     */
    public void loadGraphToViewer(DENOPTIMGraph dnGraph, 
            GSGraphSnapshot prevStatus)
    {
        loadGraphToViewer(convertDnGraphToGSGraph(dnGraph),prevStatus);
    }

//-----------------------------------------------------------------------------
    
    /**
     * Created a JUNG graph object that represents a {@link DENOPTIMGraph}, and 
     * allows to load a graphical representation into the viewer.
     * @param dnG the {@link DENOPTIMGraph} to be converted.
     * @return the JUNG object
     */
    public Graph<JVertex, JEdge> convertDnGraphToGSGraph(DENOPTIMGraph dnG) 
    {
        return convertDnGraphToGSGraph(dnG, null);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Created a JUNG graph object that represents a {@link DENOPTIMGraph}, and 
     * allows to load a graphical representation into the viewer.
     * @param dnG the {@link DENOPTIMGraph} to be converted.
     * @param tmpl null, or the {@link DENOPTIMTemplate} that contains the 
     * {@link DENOPTIMGraph}. If this is
     * not null the numbering of the AP nodes will be based on the list of APs 
     * of the {@link DENOPTIMTemplate}, otherwise 
     * (if tmpl is null) the numbering refers to the
     * AP list of the vertex that is part of the dnG graph.
     * @return the JUNG object
     */
    public Graph<JVertex, JEdge> convertDnGraphToGSGraph(
            DENOPTIMGraph dnpGraph, DENOPTIMTemplate tmpl) 
    {
        Graph<JVertex, JEdge> g = new SparseMultigraph<>();
        Map<DENOPTIMVertex, JVertex> vMap = new HashMap<DENOPTIMVertex,JVertex>();
        for (DENOPTIMVertex v : dnpGraph.getVertexList())
        {
            JVertex jv = new JVertex(v);
            vMap.put(v, jv);
            g.addVertex(jv);
            
            for (DENOPTIMAttachmentPoint ap : v.getAttachmentPoints())
            {
                if (ap.isAvailableThroughout())
                {
                    JVertex vap = new JVertex(ap);
                    g.addVertex(vap);
                    JEdge e = new JEdge(ap.getAPClass().toString());
                    e.toAp = true;
                    g.addEdge(e, vMap.get(v), vap, EdgeType.DIRECTED);
                    vap.edgeToParent = e;
                }
            }
        }
        for (DENOPTIMEdge e : dnpGraph.getEdgeList())
        {
            g.addEdge(new JEdge(e.getSrcAP().getAPClass().toString(),
                    e.getTrgAP().getAPClass().toString(), 
                    e.getBondType().toString()), 
                    vMap.get(e.getSrcAP().getOwner()), 
                    vMap.get(e.getTrgAP().getOwner()), EdgeType.DIRECTED);
        }
        for (DENOPTIMRing r : dnpGraph.getRings())
        {
            g.addEdge(new JEdge(
                        r.getHeadVertex().getEdgeToParent().getSrcAP().getAPClass().toString(),
                        r.getTailVertex().getEdgeToParent().getSrcAP().getAPClass().toString(), 
                        r.getBondType().toString()), 
                    vMap.get(r.getHeadVertex()), 
                    vMap.get(r.getTailVertex()), EdgeType.UNDIRECTED);
            vMap.get(r.getHeadVertex()).vtype = JVertexType.RCV;
            vMap.get(r.getTailVertex()).vtype = JVertexType.RCV;
        }
        
        //TODO adapt to new type
        /*
        if (tmpl != null)
        {
            renumberAPs(tmpl, g);
        }
        */
        return g;
    }
    
//-----------------------------------------------------------------------------    

    //TODO
    /*
    private static void renumberAPs(DENOPTIMTemplate tmpl, Graph graph)
    {
        for (int i=0; i<tmpl.getAttachmentPoints().size(); i++)
        {
            DENOPTIMAttachmentPoint outAP = tmpl.getAttachmentPoints().get(i);
            DENOPTIMAttachmentPoint inAP = tmpl.getInnerAPFromOuterAP(outAP);
            for (Node n : graph.getNodeSet())
            {
                if (n.getAttribute("ui.class").equals("ap"))
                {
                    int gsVrtId = n.getAttribute("dnp.srcVrtId");
                    int gsApUID = n.getAttribute("dnp.ApUID");
                    int dnVrtId = inAP.getOwner().getVertexId();
                    int dnApUID = inAP.getID();
                    if (gsVrtId == dnVrtId && gsApUID == dnApUID)
                    {
                        n.setAttribute("dnp.ApId", i+1);
                        break;
                    }
                }
            }
        }
    }
    */
	
//-----------------------------------------------------------------------------

	/**
	 * Load the given graph to the graph viewer.
	 * @param g the graph to load
	 */
	public void loadGraphToViewer(Graph<JVertex, JEdge>  g)
	{
		loadGraphToViewer(g,null);
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Load the given graph to the graph viewer.
	 * @param g the graph to load
	 * @param prevStatus the snapshot of the previous status. We use this to 
	 * remember previously chosen settings, such as the sprites to be 
	 * displayed, or the position of nodes.
	 */
	public void loadGraphToViewer(Graph<JVertex, JEdge>  g, GSGraphSnapshot prevStatus)
	{
	    loadedGraph = g;
	    Layout<JVertex, JEdge>  layout = new ISOMLayout<>(g);
        layout.setSize(new Dimension(300, 300)); //TODO-GG: get size from parent component
        viewer = new VisualizationViewer<>(layout);
		
        viewer.addGraphMouseListener(new GraphMouseListener<JVertex>() {
            
            @Override
            public void graphReleased(JVertex v, MouseEvent me)
            {}
            
            @Override
            public void graphPressed(JVertex v, MouseEvent me)
            {}
            
            @Override
            public void graphClicked(JVertex v, MouseEvent me)
            {
                if (v.expandable)
                {
                    firePropertyChange("NODECLICKED", null, v);
                } else {
                    firePropertyChange("NODECLICKED", null, null);
                }
            }
        });
        viewer.getRenderer().getVertexLabelRenderer().setPosition(
                Position.CNTR);
        viewer.getRenderContext().setVertexShapeTransformer(
                new VertexShapePaintTransformer());
        viewer.getRenderContext().setVertexFillPaintTransformer(
                new VertexFillPaintTransformer(viewer.getPickedVertexState()));
        viewer.getRenderContext().setVertexLabelTransformer(
                new VertexLabelTransformer());
        viewer.getRenderContext().setVertexFontTransformer(
                new Function<JVertex, Font>(){
            @Override
            public Font apply(JVertex v) {
                return new Font("Helvetica", Font.PLAIN, 
                        GUIPreferences.graphLabelFontSize);
            }
        });
        
        viewer.getRenderContext().setEdgeShapeTransformer(EdgeShape.line(g));
        viewer.getRenderContext().setEdgeStrokeTransformer(
                new EdgeStrokeTransformer());
        viewer.getRenderContext().setEdgeDrawPaintTransformer(
                new EdgeDrawPaintTransformer());
        viewer.getRenderContext().setArrowFillPaintTransformer(
                new EdgeDrawPaintTransformer());
        viewer.getRenderContext().setArrowDrawPaintTransformer(
                new EdgeDrawPaintTransformer());
        viewer.getRenderer().getEdgeRenderer().setEdgeArrowRenderingSupport(
                new BasicEdgeArrowRenderingSupport<JVertex, JEdge>());
        viewer.getRenderContext().setEdgeLabelTransformer(
                new EdgeLabelTransformer());
        viewer.getRenderContext().setEdgeFontTransformer(new Function<JEdge, Font>(){
            @Override
            public Font apply(JEdge e) {
                return new Font("Helvetica", Font.PLAIN, 
                        GUIPreferences.graphLabelFontSize);
            }
        });
        
        gm = new DefaultModalGraphMouse<JVertex, JEdge> ();
        gm.setMode(ModalGraphMouse.Mode.TRANSFORMING);
        viewer.setGraphMouse(gm);
        viewer.addKeyListener(gm.getModeKeyListener());
        
		//TODO-GG appendSpritesFromSnapshot(prevStatus);
		
		//TODO-GG Not working. See comment in the method.
		//placeNodesAccordingToSnapshot(prevStatus);

        //TODO-GG addAllAPIdSprites();
	    
		this.add(viewer);
	}
	
//-----------------------------------------------------------------------------
	
	private class VertexLabelTransformer implements Function<JVertex,String>
	{
        @Override
        public String apply(JVertex v) {
            String label = "";
            if (v.vtype == JVertexType.AP)
            {
                label = "             AP" + v.idStr;
            } else {
                label = v.idStr;
                if (v.displayBBID)
                {
                    label = "<html><body style='text-align: center'>" 
                            + label + "<br>" + v.dnpVertex.getBuildingBlockId()
                            + "</body></html>";
                }
            }
            return label;
        }
    }

//-----------------------------------------------------------------------------
    
    private class VertexShapePaintTransformer implements Function<JVertex,Shape>
    {
        double szB = GUIPreferences.graphNodeSize;
        double szM = GUIPreferences.graphNodeSize*2/3;
        double szS = GUIPreferences.graphNodeSize*1/3;
        @Override
        public Shape apply(JVertex v) {
            switch (v.vtype) 
            {
                case CAP:
                    return new RoundRectangle2D.Double(
                            -szB/2, -szB/2, szB, szB, 5, 5);
                case RCV:
                    return new RoundRectangle2D.Double(
                            -szM/2, -szM/2, szM, szM, 5, 5);
                case AP:
                    return new Ellipse2D.Double(-szS/2,-szS/2,szS,szS);
                default:
                    return new RoundRectangle2D.Double(
                            -szB/2, -szB/2, szB, szB, 5, 5);
            }
        }
    }
    
//-----------------------------------------------------------------------------
    
    private class VertexFillPaintTransformer implements Function<JVertex,Paint>
    {
        private final PickedInfo<JVertex> pi;

        public VertexFillPaintTransformer(PickedInfo<JVertex> pi){
            this.pi = pi;
        }

        @Override
        public Paint apply(JVertex v) {
            if (pi.isPicked(v))
            {
                return Color.GREEN;
            } else {
                switch (v.vtype) 
                {
                    case FRAG:
                        return Color.decode("#4484CE");
                    case SCAF:
                        return Color.decode("#F53240");
                    case CAP:
                        return Color.decode("#57BC90");
                    case RCV:
                        return Color.decode("#F19F4D");
                    case AP:
                        return Color.decode("#FECE00");
                    default:
                        return Color.decode("#BF1EE3");
                }
            }
        }
    }
    
//-----------------------------------------------------------------------------
    
    private class EdgeStrokeTransformer implements Function<JEdge,Stroke>
    {
        @Override
        public Stroke apply(JEdge e) {
                return new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_ROUND, 1.5f);
        }
    }
    
//-----------------------------------------------------------------------------
    
    private class EdgeDrawPaintTransformer implements Function<JEdge,Paint>
    {
        @Override
        public Paint apply(JEdge e) {
            if (e.toAp)
                return Color.decode("#FECE00");
            else
                return Color.decode("#000000");
        }
    }
    
//-----------------------------------------------------------------------------

    private class EdgeLabelTransformer implements Function<JEdge,String>
    {
        @Override
        public String apply(JEdge e) {
            if (!e.displayAPCs && !e.displayBndTyp)
            {
                return null;
            }
            String label = "";
            if (e.toAp)
            {
                if (e.displayAPCs)
                {
                    label = e.srcAPC;
                }
            } else {
                if (e.displayBndTyp)
                {
                    label = label + e.bt;
                    if (e.displayAPCs)
                    {
                        label = label + "<br>";
                    }
                }
                if (e.displayAPCs)
                {
                    label = label + e.srcAPC + "<br>" + e.trgAPC;
                }
            }
            if (!label.isEmpty())
            {
                label = "<html><body style='text-align: center'>" 
                        + label + "</body></html>";
            }
            return label;
        }
    }
	
//-----------------------------------------------------------------------------

	/**
	 * Append sprites from snapshot.
	 * @param snapshot
	 */
    /*
	private void appendSpritesFromSnapshot(GSGraphSnapshot snapshot)
	{	
		if (snapshot == null)
		{
			return;
		}
		
		if (!snapshot.getGraphId().equals(graph.getId()))
		{
			return;
		}
		
		if (snapshot.hasSpritesOfType(SPRITEATT_UICLASS_APCLASSSRC))
		{
			for (Element el : snapshot.getSpritesOfType(SPRITEATT_UICLASS_APCLASSSRC))
			{
				if (el instanceof Edge)
				{
					Edge ed = graph.getEdge(el.getId());
					if (ed != null)
					{
						addSrcApClassSprite(ed);
					}
				}
			}
		}
		
		if (snapshot.hasSpritesOfType(SPRITEATT_UICLASS_APCLASSTRG))
		{
			for (Element el : snapshot.getSpritesOfType(SPRITEATT_UICLASS_APCLASSTRG))
			{
				if (el instanceof Edge)
				{
					Edge ed = graph.getEdge(el.getId());
					if (ed != null)
					{
						addTrgApClassSprite(ed);
					}
				}
			}
		}
		
		if (snapshot.hasSpritesOfType(SPRITEATT_UICLASS_BNDORD))
		{
			for (Element el : snapshot.getSpritesOfType(SPRITEATT_UICLASS_BNDORD))
			{
				if (el instanceof Edge)
				{
					Edge ed = graph.getEdge(el.getId());
					if (ed != null)
					{
						addBondSprite(ed);
					}
				}
			}
		}
		
		if (snapshot.hasSpritesOfType(SPRITEATT_UICLASS_FRGID))
		{
			for (Element el : snapshot.getSpritesOfType(SPRITEATT_UICLASS_FRGID))
			{
				if (el instanceof Node)
				{
					Node n = graph.getNode(el.getId());
					if (n != null)
					{
						addFrgIdSprite(n);
					}
				}
			}
		}
	}
	*/
	
//-----------------------------------------------------------------------------

	/**
	 * Adds or removes labels from the elements selected in the graph view. The
	 * logics is a bit complex in that the behaviour is special in case one/more 
	 * nodes are selected AND we want to alter the APClass labels: 
	 * if there is only one selected node, then we display the APClasses on
	 * all incident vertexes, if there is more than one,
	 * @param labelType the type of label to act on.
	 * @param show use <code>true</code> to display labels, or <code>false</code>
	 * to hide labels of the given kind.
	 */
	public void alterLabels(LabelType labelType, boolean show)
	{
		switch (labelType)
		{
			case APC:
			    for (JVertex v : getSelectedNodes())
		        {
			        for (JEdge e : loadedGraph.getIncidentEdges(v))
                    {
                        e.displayAPCs = show;
                    }
		        }
				break;
				
			case BT:
			    for (JVertex v : getSelectedNodes())
                {
                    for (JEdge e : loadedGraph.getIncidentEdges(v))
                    {
                        e.displayBndTyp = show;
                    }
                }
				break;
				
			case BBID:
				for (JVertex v : getSelectedNodes())
				{
					v.displayBBID = show;
				}
				break;
		}
		viewer.repaint();
	}

//-----------------------------------------------------------------------------

	/**
	 * Finds of selected edges from the viewer
	 * @return the set of edges
	 */
	public Set<JEdge> getSelectedEdges()
	{
	    Set<JEdge> selEdges = new HashSet<>(
	            viewer.getPickedEdgeState().getPicked());
	    List<JVertex> lstNodes = new ArrayList<JVertex>(
	            viewer.getPickedVertexState().getPicked());
	    for (int i=0; i<lstNodes.size(); i++)
	    {
	        for (int j=i+1; j<lstNodes.size(); j++)
	        {
	            JEdge e = loadedGraph.findEdge(lstNodes.get(i), 
	                    lstNodes.get(j));
	            if (e != null)
	            {
	                selEdges.add(e);
	            }
	            JEdge erev = loadedGraph.findEdge(lstNodes.get(j), 
	                    lstNodes.get(i));
                if (erev != null)
                {
                    selEdges.add(erev);
                }
	        }
	    }
		return selEdges;
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Finds selected nodes from the viewer.
	 * @return the set of nodes.
	 */
	public Set<JVertex> getSelectedNodes()
	{
	    return viewer.getPickedVertexState().getPicked();
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Check is there is any node selected in the viewer.
	 * @return <code>true</code> if there is at least one node selected.
	 */
	public boolean hasSelected()
	{
	    return !viewer.getPickedVertexState().getPicked().isEmpty();
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Returns a copy of the graph loaded into the viewer
	 */
	public GSGraphSnapshot getStatusSnapshot()
	{	
		GSGraphSnapshot snapshot = null;
		/*//TODO
		if (graph !=null && sman != null)
		{
			snapshot = new GSGraphSnapshot(graph,sman);
		}
		*/
		return snapshot;
	}

//-----------------------------------------------------------------------------
	
	public void dispose() 
	{
		cleanup();
	}
	
	//-----------------------------------------------------------------------------

//TODO-GG delete
	   private APClass APCA, APCB, APCC, APCD;
	    private String a="A", b="B", c="C", d="D";
	    
	   private void prepareFragSpace() {
	       
	        try {
	            APCA = APClass.make(a, 0);
	            APCB = APClass.make(b, 0);
	            APCC = APClass.make(c, 0);
	            APCD = APClass.make(d, 99);
	        } catch (DENOPTIMException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	        
	        HashMap<String,BondType> boMap = new HashMap<String,BondType>();
	        boMap.put(a,BondType.SINGLE);
	        boMap.put(b,BondType.SINGLE);
	        boMap.put(c,BondType.SINGLE);
	        boMap.put(d,BondType.DOUBLE);
	        
	        HashMap<APClass,ArrayList<APClass>> cpMap = 
	                new HashMap<APClass,ArrayList<APClass>>();
	        ArrayList<APClass> lstA = new ArrayList<APClass>();
	        lstA.add(APCA);
	        cpMap.put(APCA, lstA);
	        ArrayList<APClass> lstB = new ArrayList<APClass>();
	        lstB.add(APCB);
	        lstB.add(APCC);
	        cpMap.put(APCB, lstB);
	        ArrayList<APClass> lstC = new ArrayList<APClass>();
	        lstC.add(APCB);
	        lstC.add(APCC);
	        cpMap.put(APCC, lstC);
	        ArrayList<APClass> lstD = new ArrayList<APClass>();
	        lstD.add(APCD);
	        cpMap.put(APCD, lstD);
	        
	        /* Compatibility matrix
	         * 
	         *      |  A  |  B  |  C  | D |
	         *    -------------------------
	         *    A |  T  |     |     |   |
	         *    -------------------------
	         *    B |     |  T  |  T  |   |
	         *    -------------------------
	         *    C |     |  T  |  T  |   |
	         *    -------------------------
	         *    D |     |     |     | T |
	         */
	        
	        HashMap<APClass,APClass> capMap = new HashMap<APClass,APClass>();
	        HashSet<APClass> forbEnds = new HashSet<APClass>();
	        
	        FragmentSpace.setBondOrderMap(boMap);
	        FragmentSpace.setCompatibilityMatrix(cpMap);
	        FragmentSpace.setCappingMap(capMap);
	        FragmentSpace.setForbiddenEndList(forbEnds);
	        FragmentSpace.setAPclassBasedApproach(true);
	        
	        FragmentSpace.setScaffoldLibrary(new ArrayList<DENOPTIMVertex>());
	        FragmentSpace.setFragmentLibrary(new ArrayList<DENOPTIMVertex>());
	        
	        DENOPTIMVertex s0 = new EmptyVertex();
	        s0.setBuildingBlockType(BBType.SCAFFOLD);
	        s0.addAP(0, 1, 1, APCA);
	        s0.addAP(0, 1, 1, APCA);
	        FragmentSpace.appendVertexToLibrary(s0, BBType.SCAFFOLD,
	                FragmentSpace.getScaffoldLibrary());
	        
	        DENOPTIMVertex s1 = new EmptyVertex();
	        s1.setBuildingBlockType(BBType.SCAFFOLD);
	        s1.addAP(0, 1, 1, APCA);
	        s1.addAP(0, 1, 1, APCA);
	        s1.addAP(0, 1, 1, APCD);
	        s1.addAP(0, 1, 1, APCB);
	        s1.addAP(0, 1, 1, APCB);
	        FragmentSpace.appendVertexToLibrary(s1, BBType.SCAFFOLD,
	                FragmentSpace.getScaffoldLibrary());
	        
	        DENOPTIMVertex v0 = new EmptyVertex();
	        v0.setBuildingBlockType(BBType.FRAGMENT);
	        v0.addAP(0, 1, 1, APCA);
	        v0.addAP(0, 1, 1, APCB);
	        v0.addAP(0, 1, 1, APCA);
	        FragmentSpace.appendVertexToLibrary(v0, BBType.FRAGMENT,
	                FragmentSpace.getFragmentLibrary());
	        
	        DENOPTIMVertex v1 = new EmptyVertex();
	        v1.setBuildingBlockType(BBType.FRAGMENT);
	        v1.addAP(0, 1, 1, APCA);
	        v1.addAP(0, 1, 1, APCB);
	        v1.addAP(0, 1, 1, APCA);
	        v1.addAP(0, 1, 1, APCB);
	        v1.addAP(0, 1, 1, APCC);
	        FragmentSpace.appendVertexToLibrary(v1, BBType.FRAGMENT,
	                FragmentSpace.getFragmentLibrary());
	        
	        DENOPTIMVertex v2 = new EmptyVertex();
	        v2.setBuildingBlockType(BBType.FRAGMENT);
	        v2.addAP(0, 1, 1, APCB);
	        FragmentSpace.appendVertexToLibrary(v2, BBType.FRAGMENT,
	                FragmentSpace.getFragmentLibrary());
	        
	        DENOPTIMVertex v3 = new EmptyVertex();
	        v3.setBuildingBlockType(BBType.FRAGMENT);
	        v3.addAP(0, 1, 1, APCD);
	        v3.addAP(0, 1, 1, APCD);
	        FragmentSpace.appendVertexToLibrary(v3, BBType.FRAGMENT,
	                FragmentSpace.getFragmentLibrary());
	       
	        DENOPTIMVertex v4 = new EmptyVertex();
	        v4.setBuildingBlockType(BBType.FRAGMENT);
	        v4.addAP(0, 1, 1, APCC);
	        v4.addAP(0, 1, 1, APCB);
	        v4.addAP(0, 1, 1, APCB);
	        v4.addAP(0, 1, 1, APCA);
	        v4.addAP(0, 1, 1, APCA);
	        FragmentSpace.appendVertexToLibrary(v4, BBType.FRAGMENT,
	                FragmentSpace.getFragmentLibrary());
	        
	        DENOPTIMVertex v5 = new EmptyVertex();
	        v5.setBuildingBlockType(BBType.FRAGMENT);
	        v5.addAP(0, 1, 1, APCB);
	        v5.addAP(0, 1, 1, APCD);
	        FragmentSpace.appendVertexToLibrary(v5, BBType.FRAGMENT,
	                FragmentSpace.getFragmentLibrary());
	        
	        DENOPTIMVertex v6 = new EmptyVertex();
	        v6.setBuildingBlockType(BBType.FRAGMENT);
	        v6.addAP(0, 1, 1, APCC);
	        v6.addAP(0, 1, 1, APCD);
	        FragmentSpace.appendVertexToLibrary(v6, BBType.FRAGMENT,
	                FragmentSpace.getFragmentLibrary());
	        
	        DENOPTIMVertex v7 = new EmptyVertex();
	        v7.setBuildingBlockType(BBType.FRAGMENT);
	        v7.setAsRCV(true);
	        v7.addAP(0, 1, 1, APCB);
	        FragmentSpace.appendVertexToLibrary(v7, BBType.FRAGMENT,
	                FragmentSpace.getFragmentLibrary());
	    }
	    
	    private DENOPTIMGraph makeTestGraphD() throws DENOPTIMException
	    {
	        DENOPTIMGraph graph = new DENOPTIMGraph();
	        DENOPTIMVertex s = DENOPTIMVertex.newVertexFromLibrary(1,
	                BBType.SCAFFOLD);
	        s.setLevel(-1);
	        graph.addVertex(s);
	        DENOPTIMVertex v1a = DENOPTIMVertex.newVertexFromLibrary(1,
	                BBType.FRAGMENT);
	        v1a.setLevel(0);
	        graph.addVertex(v1a);
	        DENOPTIMVertex v6a = DENOPTIMVertex.newVertexFromLibrary(6,
	                BBType.FRAGMENT);
	        v6a.setLevel(1);
	        graph.addVertex(v6a);
	        DENOPTIMVertex v6a_bis = DENOPTIMVertex.newVertexFromLibrary(6,
	                BBType.FRAGMENT);
	        v6a_bis.setLevel(1);
	        graph.addVertex(v6a_bis);
	        DENOPTIMVertex v6a_tris = DENOPTIMVertex.newVertexFromLibrary(6,
	                BBType.FRAGMENT);
	        v6a_tris.setLevel(1);
	        graph.addVertex(v6a_tris);
	        DENOPTIMVertex v7a = DENOPTIMVertex.newVertexFromLibrary(7,
	                BBType.FRAGMENT);
	        v7a.setLevel(2);
	        graph.addVertex(v7a);
	        DENOPTIMVertex v7a_bis = DENOPTIMVertex.newVertexFromLibrary(7,
	                BBType.FRAGMENT);
	        v7a_bis.setLevel(2);
	        graph.addVertex(v7a_bis);
	        DENOPTIMVertex v7a_tris = DENOPTIMVertex.newVertexFromLibrary(7,
	                BBType.FRAGMENT);
	        v7a_tris.setLevel(2);
	        graph.addVertex(v7a_tris);
	        
	        DENOPTIMVertex v7a_quat = DENOPTIMVertex.newVertexFromLibrary(7,
	                BBType.FRAGMENT);
	        v7a_quat.setLevel(0);
	        graph.addVertex(v7a_quat);
	        
	        DENOPTIMVertex v1c = DENOPTIMVertex.newVertexFromLibrary(1,
	                BBType.FRAGMENT);
	        v1c.setLevel(0);
	        graph.addVertex(v1c);
	        
	        DENOPTIMVertex v1b = DENOPTIMVertex.newVertexFromLibrary(1,
	                BBType.FRAGMENT);
	        v1b.setLevel(1);
	        graph.addVertex(v1b);
	        DENOPTIMVertex v2b = DENOPTIMVertex.newVertexFromLibrary(2,
	                BBType.FRAGMENT);
	        v2b.setLevel(2);
	        graph.addVertex(v2b);
	        DENOPTIMVertex v2b_bis = DENOPTIMVertex.newVertexFromLibrary(2,
	                BBType.FRAGMENT);
	        v2b_bis.setLevel(2);
	        graph.addVertex(v2b_bis);
	        DENOPTIMVertex v3b = DENOPTIMVertex.newVertexFromLibrary(3,
	                BBType.FRAGMENT);
	        v3b.setLevel(2);
	        graph.addVertex(v3b);
	        graph.addEdge(new DENOPTIMEdge(s.getAP(0), v1a.getAP(0)));
	        graph.addEdge(new DENOPTIMEdge(v1a.getAP(1), v6a.getAP(0)));
	        graph.addEdge(new DENOPTIMEdge(v1a.getAP(3), v6a_bis.getAP(0)));
	        graph.addEdge(new DENOPTIMEdge(v1a.getAP(4), v6a_tris.getAP(0)));
	        graph.addEdge(new DENOPTIMEdge(v6a.getAP(1), v7a.getAP(0)));
	        graph.addEdge(new DENOPTIMEdge(v6a_bis.getAP(1), v7a_bis.getAP(0)));
	        graph.addEdge(new DENOPTIMEdge(v6a_tris.getAP(1), v7a_tris.getAP(0)));
	        graph.addEdge(new DENOPTIMEdge(s.getAP(2), v7a_quat.getAP(0)));
	        graph.addEdge(new DENOPTIMEdge(s.getAP(1), v1c.getAP(0)));
	        graph.addEdge(new DENOPTIMEdge(v1c.getAP(2), v1b.getAP(0)));
	        graph.addEdge(new DENOPTIMEdge(v1b.getAP(1), v2b.getAP(0)));
	        graph.addEdge(new DENOPTIMEdge(v1b.getAP(3), v2b_bis.getAP(0)));
	        graph.addEdge(new DENOPTIMEdge(v1b.getAP(4), v3b.getAP(0)));
	        
	        //graph.addRing(v7a, v7a_quat);
	        v7a.setAsRCV(true);
	        v7a_quat.setAsRCV(true);
	        graph.addRing(v7a_bis, v7a_tris);
	        
	        ArrayList<Integer> symA = new ArrayList<Integer>();
	        symA.add(v1a.getVertexId());
	        symA.add(v1c.getVertexId());
	        graph.addSymmetricSetOfVertices(new SymmetricSet(symA));
	        
	        ArrayList<Integer> symB = new ArrayList<Integer>();
	        symB.add(v2b.getVertexId());
	        symB.add(v2b_bis.getVertexId());
	        graph.addSymmetricSetOfVertices(new SymmetricSet(symB));
	        
	        graph.renumberGraphVertices();
	        return graph;
	    }
	
//-----------------------------------------------------------------------------
}
