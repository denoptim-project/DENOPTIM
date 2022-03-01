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

package denoptim.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import com.google.common.base.Function;

import denoptim.graph.APClass;
import denoptim.graph.DENOPTIMAttachmentPoint;
import denoptim.graph.DENOPTIMEdge;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMRing;
import denoptim.graph.DENOPTIMTemplate;
import denoptim.graph.DENOPTIMVertex;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractPopupGraphMousePlugin;
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
public class GraphViewerPanel extends JPanel 
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 2L;
	
	Graph<JVertex, JEdge> loadedGraph;
	DNPSpringLayout<JVertex, JEdge> layout;
	private double scaling = 1.0;
	VisualizationViewer<JVertex, JEdge>  viewer;
	private DefaultModalGraphMouse<JVertex, JEdge> gm;
	
	public enum LabelType {APC, BT, BBID, APID};
	
    public enum JVertexType {SCAF, FRAG, CAP, RCV, AP, NONE};
    
    // Properties fired y this class and intercepted in parent containers
    public static final String PROPERTYNODECLICKED = "NODECLICKED";
    public static final String PROPERTYMOUSEMODE = "SETMOUSEMODE";
    
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
         * The string used as label when graphically depicting this vertex.
         */
        String label = "NA";
        
        /**
         * The string used as identifier of this JVertex in different instances 
         * representing the same graph.
         */
        String idStr = "NA";
        
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
         * {@link GraphViewerPanel#convertDnGraphToGSGraph(DENOPTIMGraph, DENOPTIMTemplate)}
         * @param ap the {@link DENOPTIMAttachmentPoint}.
         */
        public JVertex(DENOPTIMAttachmentPoint ap) {
            this.ap = ap;
            idStr = Integer.toString(ap.getOwner().getVertexId()) 
                    + Integer.toString(ap.getID());
            //NB: in the GUI (like in the SDF files) we use 1-based indexing
            label = Integer.toString(ap.getIndexInOwner()+1);
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
            label = Integer.toString(v.getVertexId());
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
         * String identifying this edge within a graph. This is not meant
         * to be unique across graphs, but is unique within a graph,
         * provided that the vertex IDs are unique.
         */
        String id = "";
        
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
         * String representing the index of the source AP in the list of APs
         * of the vertex owning the source AP. This property allies only to
         * directed edges
         */
        String srcAPID = "";

        /**
         * String representing the index of the target AP in the list of APs
         * of the vertex owning the source AP. This property allies only to
         * directed edges
         */
        String trgAPID = "";
        
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
         * Flag requiring to display APClasses
         */
        boolean displayAPIDs = false;
        
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
        public JEdge(DENOPTIMAttachmentPoint srcAP) 
        {
            this.id = srcAP.getOwner().getVertexId()+"_"+srcAP.getIndexInOwner();
            if (srcAP.getAPClass() != null)
                this.srcAPC = srcAP.getAPClass().toString();
            else
                this.srcAPC = "null";
            this.trgAPC = "none";
            this.bt = "none";
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
        public JEdge(DENOPTIMAttachmentPoint srcAP, 
                DENOPTIMAttachmentPoint trgAP, String bt) 
        {
            this.id = srcAP.getOwner().getVertexId() 
                    + "-" 
                    + trgAP.getOwner().getVertexId();
            if (srcAP.getAPClass() != null)
                this.srcAPC = srcAP.getAPClass().toString();
            else
                this.srcAPC = "null";
            if (trgAP.getAPClass() != null)
                this.trgAPC = trgAP.getAPClass().toString();
            else
                this.trgAPC = "null";
            //NB: in GUI as well as in SDF files APs have 1-based index!
            this.srcAPID = "AP" + (srcAP.getIndexInOwner()+1);
            this.trgAPID = "AP" + (trgAP.getIndexInOwner()+1);
            this.bt = bt;
        }
    }
	
//-----------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	public GraphViewerPanel()
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
            JUNGGraphSnapshot prevStatus)
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
                    JEdge e = new JEdge(ap);
                    e.toAp = true;
                    g.addEdge(e, vMap.get(v), vap, EdgeType.DIRECTED);
                    vap.edgeToParent = e;
                }
            }
        }
        for (DENOPTIMEdge e : dnpGraph.getEdgeList())
        {
            g.addEdge(new JEdge(e.getSrcAP(),
                    e.getTrgAP(), 
                    e.getBondType().toString()), 
                    vMap.get(e.getSrcAP().getOwner()), 
                    vMap.get(e.getTrgAP().getOwner()), EdgeType.DIRECTED);
        }
        for (DENOPTIMRing r : dnpGraph.getRings())
        {   
            g.addEdge(new JEdge(
                        r.getHeadVertex().getEdgeToParent().getSrcAP(),
                        r.getTailVertex().getEdgeToParent().getSrcAP(), 
                        r.getBondType().toString()), 
                    vMap.get(r.getHeadVertex()), 
                    vMap.get(r.getTailVertex()), EdgeType.UNDIRECTED);
            vMap.get(r.getHeadVertex()).vtype = JVertexType.RCV;
            vMap.get(r.getTailVertex()).vtype = JVertexType.RCV;
        }
        
        if (tmpl != null)
        {
            renumberAPs(tmpl, g);
        }
        return g;
    }
    
//-----------------------------------------------------------------------------    

    private static void renumberAPs(DENOPTIMTemplate tmpl, 
            Graph<JVertex, JEdge> graph)
    {
        for (int i=0; i<tmpl.getAttachmentPoints().size(); i++)
        {
            DENOPTIMAttachmentPoint outAP = tmpl.getAttachmentPoints().get(i);
            DENOPTIMAttachmentPoint inAP = tmpl.getInnerAPFromOuterAP(outAP);
            for (JVertex v : graph.getVertices())
            {
                if (v.vtype == JVertexType.AP)
                {
                    if (v.ap == inAP)
                    {
                        v.label = Integer.toString(i+1);
                        break;
                    }
                }
            }
        }
    }
	
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
     * remember previously chosen settings, such as the labels to be 
     * displayed, or the position of nodes.
     */
    public void loadGraphToViewer(Graph<JVertex, JEdge>  g, 
            JUNGGraphSnapshot prevStatus)
    {
        loadGraphToViewer(g, prevStatus, true);
    }

//-----------------------------------------------------------------------------
    
    /**
     * Load the given graph to the graph viewer.
     * @param g the graph to load
     * @param prevStatus the snapshot of the previous status. We use this to 
     * remember previously chosen settings, such as the labels to be 
     * displayed, or the position of nodes.
     * @param lock decides whether we lock the initial position of nodes or not.
     */
    public void loadGraphToViewer(Graph<JVertex, JEdge>  g, 
            JUNGGraphSnapshot prevStatus, boolean lock)
    {
        loadGraphToViewer(g, prevStatus, lock, 1.0);
    }
//-----------------------------------------------------------------------------
	
	/**
	 * Load the given graph to the graph viewer.
	 * @param g the graph to load
	 * @param prevStatus the snapshot of the previous status. We use this to 
	 * remember previously chosen settings, such as the labels to be 
	 * displayed, or the position of nodes.
	 * @param lock decides whether we lock the initial position of nodes or not.
	 * @param reScaling a factor to be multiplied to the current scaling of 
	 * the graph layout size. This allows to
	 * reduce/enlarge the space considered for plotting the graph.
	 */
	public void loadGraphToViewer(Graph<JVertex, JEdge>  g, 
	        JUNGGraphSnapshot prevStatus, boolean lock, double reScaling)
	{
	    loadedGraph = g;
	    scaling = scaling * reScaling;
	    layout = new DNPSpringLayout<>(g);
	    
	    //NB: the size depends on where this panel is used. In GUIVertexSelector
        // the size vanishes, so we need to set a decent value or the graph will
        // be displayed with 0:0 dimensions, i.e., all nodes on top of each other
	    Dimension dimLayout = null;
	    Dimension dimViewer = null;
        if (this.getSize().height<150)
        {
            dimLayout = new Dimension(150, 150);
            layout.setSize(dimLayout);
            dimViewer = new Dimension(150, 150);
        } else {
            double w = this.getSize().width * scaling
                    - GUIPreferences.graphNodeSize * 1.5;
            double h = this.getSize().height * scaling
                    - GUIPreferences.graphNodeSize * 1.5;
            dimLayout = new Dimension((int) w, (int) h);
            layout.setSize(dimLayout);
            dimViewer = this.getSize();
        }
	    
	    // Unless we have a snapshot from previous visualization, generate the
        // initial layout of nodes with ISOM layout
	    if (prevStatus != null)
	    {
	        // Here we set view features according to a previous graph view
	        inheritFeatures(prevStatus, lock);
	    }
	    
        viewer = new VisualizationViewer<>(layout,dimViewer);
		
	    // Listener for clicks on the graph nodes
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
                    firePropertyChange(PROPERTYNODECLICKED, null, v);
                } else {
                    firePropertyChange(PROPERTYNODECLICKED, null, null);
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
        viewer.getRenderContext().setEdgeFontTransformer(
                new Function<JEdge, Font>(){
            @Override
            public Font apply(JEdge e) {
                return new Font("Helvetica", Font.PLAIN, 
                        GUIPreferences.graphLabelFontSize);
            }
        });
        
        gm = new DefaultModalGraphMouse<JVertex, JEdge> ();
        gm.setMode(ModalGraphMouse.Mode.PICKING);
        gm.add(new PopupGraphMousePlugin());
        viewer.setGraphMouse(gm);
        viewer.addKeyListener(gm.getModeKeyListener());
		this.add(viewer);
        
        centerGraphLayout();
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Files a property change relative to the mode of mouse transformation.
	 * @param i use 0 for picking mode, 1 for move mode.
	 */
	public void fireMouseModePropertyChange(int i)
    {
        firePropertyChange(PROPERTYMOUSEMODE, null, i);
    }

//-----------------------------------------------------------------------------
	
	public void centerGraphLayout()
	{
        Point2D centerGraph = getLayoutCenter();
        Point2D centerViewer = viewer.getRenderContext()
                .getMultiLayerTransformer()
                .inverseTransform(Layer.LAYOUT, viewer.getCenter());
        viewer.getRenderContext().getMultiLayerTransformer()
        .getTransformer(Layer.LAYOUT).translate(
                          -(centerGraph.getX() - centerViewer.getX()),
                          -(centerGraph.getY() - centerViewer.getY()));
	}

//-----------------------------------------------------------------------------

    private Point2D getLayoutCenter()
    {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (JVertex v : loadedGraph.getVertices())
        {
            try
            {
                Point2D p = layout.getVertexPosition(v);
                if (p.getX() < minX)
                    minX = p.getX();
                if (p.getY() < minY)
                    minY = p.getY();
                if (p.getX() > maxX)
                    maxX = p.getX();
                if (p.getY() > maxY)
                    maxY = p.getY();
            } catch (ExecutionException e)
            {
                //ignore it
            }
        }
        return new Point2D.Double(minX + (maxX-minX)/2.0, 
                minY + (maxY-minY)/2.0);
    }

//-----------------------------------------------------------------------------
	
	private class PopupGraphMousePlugin extends AbstractPopupGraphMousePlugin 
	{
	    
        @Override
        protected void handlePopup(MouseEvent e)
        {
            Point p = e.getPoint();
            GraphOptsPopup popup = new GraphOptsPopup();
            popup.show((Component) e.getSource(), e.getX(), e.getY());
        }
	}
	
//-----------------------------------------------------------------------------
	
	@SuppressWarnings("serial")
    private class GraphOptsPopup extends JPopupMenu
	{
        public GraphOptsPopup() {
	        super();
	        
            JMenuItem mnuRelax = new JMenuItem("Refine node locations");
            mnuRelax.setToolTipText(String.format("<html><body width='%1s'>"
                    + "Relaxe the position of graph nodes as to avoid node "
                    + "overlap. Depending on the complexity of the graph, you "
                    + "might consider moving specific portions of the graph "
                    + "before attempting any refinement, or consider tunning "
                    + "this refinement multiple times</html>", 300));
            mnuRelax.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JUNGGraphSnapshot snpSht = getGraphStatusSnapshot();
                    cleanup();
                    loadGraphToViewer(loadedGraph,snpSht,false);
                }});
            this.add(mnuRelax);

            this.add(new JSeparator());
            
            JMenuItem mnuShrink = new JMenuItem("Shrink Plottable Area");
            mnuShrink.setToolTipText(String.format("<html><body width='%1s'>"
                    + "Re-plot the graph using a shrinked plottable region."
                    + "This will force nosed to fit into a smaller space."
                    + "</html>", 300));
            mnuShrink.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JUNGGraphSnapshot snpSht = getGraphStatusSnapshot();
                    cleanup();
                    loadGraphToViewer(loadedGraph,snpSht,false,0.75);
                }});
            this.add(mnuShrink);
            
            JMenuItem mnuEnlarge = new JMenuItem("Enlarge Plottable Area");
            mnuEnlarge.setToolTipText(String.format("<html><body width='%1s'>"
                    + "Re-plot the graph using an enlarged plottable region."
                    + "This will allow nodes to be places farther apart."
                    + "</html>", 300));
            mnuEnlarge.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JUNGGraphSnapshot snpSht = getGraphStatusSnapshot();
                    cleanup();
                    loadGraphToViewer(loadedGraph,snpSht,false,1.25);
                }});
            this.add(mnuEnlarge);
            
            this.add(new JSeparator());
            
            JMenuItem mnuCenterView = new JMenuItem("Center View");
            mnuCenterView.setToolTipText(String.format(
                    "<html><body width='%1s'>"
                    + "Center the graph to the vindow.</html>", 300));
            mnuCenterView.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    centerGraphLayout();
                }
            });
            this.add(mnuCenterView);
            
            JMenuItem mnuMoveMode = new JMenuItem("Mouse mode: move");
            mnuMoveMode.setToolTipText(String.format(
                    "<html><body width='%1s'>"
                    + "Makes mouse move the graph view.</html>", 300));
            mnuMoveMode.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    fireMouseModePropertyChange(1);
                }
            });
            this.add(mnuMoveMode);
            
            JMenuItem mnuPickMode = new JMenuItem("Mouse mode: pick");
            mnuPickMode.setToolTipText(String.format(
                    "<html><body width='%1s'>"
                    + "Makes the mouse select vertex on click.</html>", 300));
            mnuPickMode.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    fireMouseModePropertyChange(0);
                }
            });
            this.add(mnuPickMode);
            
            this.add(new JSeparator());
            
	        JMenuItem mnuShowAPC = new JMenuItem("Show APClasses");
	        mnuShowAPC.setToolTipText(String.format("<html><body width='%1s'>"
                    + "Add labels with "
                    + "attachment point classes on edges and attachment points "
                    + "related to "
                    + "the selected nodes.</html>", 300));
	        mnuShowAPC.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                alterLabels(LabelType.APC, true);
	            }});
            this.add(mnuShowAPC);
	        JMenuItem mnuHideAPC = new JMenuItem("Hide APClasses");
	        mnuHideAPC.setToolTipText(String.format("<html><body width='%1s'>"
                    + "Remove labels with "
                    + "attachment point classes from edges "
                    + "and attachment points related to "
                    + "the selected nodes.</html>", 300));
	        mnuHideAPC.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    alterLabels(LabelType.APC, false);
                }});
            this.add(mnuHideAPC);
            
            this.add(new JSeparator());
            
            JMenuItem mnuShowAPID = new JMenuItem("Show AP IDs");
            mnuShowAPID.setToolTipText(String.format("<html><body width='%1s'>"
                    + "Add labels declaring which attachment points are used "
                    + "to form an edge. Acts on any edge related to the "
                    + "selected nodes.</html>", 300));
            mnuShowAPID.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    alterLabels(LabelType.APID, true);
                }});
            this.add(mnuShowAPID);
            JMenuItem mnuHideAPID = new JMenuItem("Hide AP IDs");
            mnuHideAPID.setToolTipText(String.format("<html><body width='%1s'>"
                    + "Remove labales attachment point identifiers from the "
                    + "edges related to the selecgted nodes.</html>", 300));
            mnuHideAPID.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    alterLabels(LabelType.APID, false);
                }});
            this.add(mnuHideAPID);
            
	        this.add(new JSeparator());
	        
            JMenuItem mnuShowBT = new JMenuItem("Show Bond Types");
            mnuShowBT.setToolTipText(String.format("<html><body width='%1s'>"
                    + "Add labels  defining the bond type on "
                    + "the edges related to the selected nodes."
                    + "</html>", 300));
            mnuShowBT.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    alterLabels(LabelType.BT, true);
                }});
            this.add(mnuShowBT);
            JMenuItem mnuHideBT = new JMenuItem("Hide Bond Types");
            mnuHideBT.setToolTipText(String.format("<html><body width='%1s'>"
                    + "Remove labels defining the bond type from the edges"
                    + "related to the selected nodes.</html>", 300));
            mnuHideBT.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    alterLabels(LabelType.BT, false);
                }});
	        this.add(mnuHideBT);
	        
	        this.add(new JSeparator());
	        
            JMenuItem mnuShowBBID = new JMenuItem("Show Building Block IDs");
            mnuShowBBID.setToolTipText(String.format("<html><body width='%1s'>"
                    + "Add labels with "
                    + "building block IDs to the selected nodes.</html>", 300));
            mnuShowBBID.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    alterLabels(LabelType.BBID, true);
                }});
            this.add(mnuShowBBID);
            JMenuItem mnuHideBBID = new JMenuItem("Hide Building Block IDs");
            mnuHideBBID.setToolTipText(String.format("<html><body width='%1s'>"
                    + "Hide building block IDs in the selected nodes."
                    + "</html>", 300));
            mnuHideBBID.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    alterLabels(LabelType.BBID, false);
                }});
            this.add(mnuHideBBID);
            
            this.add(new JSeparator());
            
            JMenuItem mnuHideAll = new JMenuItem("Hide All Labels");
            mnuHideAll.setToolTipText(String.format("<html><body width='%1s'>"
                    + "Hide all labels related to the selected nodes.</html>", 
                    300));
            mnuHideAll.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    for (LabelType lt : LabelType.values())
                        alterLabels(lt, false);
                }});
            this.add(mnuHideAll);
	    }
	}
	
//-----------------------------------------------------------------------------
	
	public void setMouseMode(ModalGraphMouse.Mode mode)
	{
	    if (gm != null)
	        gm.setMode(mode);
	}
	
//-----------------------------------------------------------------------------
	
	private void inheritFeatures(JUNGGraphSnapshot prevStatus, boolean lock)
    {
	    // Set labels on vertexes
	    ArrayList<String> lst = prevStatus.getVertexeIDsWithLabel(
	            LabelType.BBID);
        for(JVertex jv : loadedGraph.getVertices())
        {
            if (lst.contains(jv.idStr))
            {
                jv.displayBBID = true;
            }
        }
        
        // Set labels on edges
        ArrayList<String> lstBT = prevStatus.getEdgeIDsWithLabel(LabelType.BT);
        ArrayList<String> lstAPC = prevStatus.getEdgeIDsWithLabel(LabelType.APC);
        ArrayList<String> lstAPID = prevStatus.getEdgeIDsWithLabel(
                LabelType.APID);
        for(JEdge je : loadedGraph.getEdges())
        {
            if (lstBT.contains(je.id))
            {
                je.displayBndTyp = true;
            }
            if (lstAPC.contains(je.id))
            {
                je.displayAPCs = true;
            }
            if (lstAPID.contains(je.id))
            {
                je.displayAPIDs = true;
            }
        }
        
        // Set the positions via initialising of the layout
        layout.setInitialLocations(prevStatus.vertexPosition, lock);
    }
	
//-----------------------------------------------------------------------------
    
    private class VertexLabelTransformer implements Function<JVertex,String>
	{
        @Override
        public String apply(JVertex v) {
            String label = "";
            if (v.vtype == JVertexType.AP)
            {
                label = "             AP" + v.label;
            } else {
                label = v.label;
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
            if (!e.displayAPCs && !e.displayBndTyp && !e.displayAPIDs)
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
                }
                if (e.displayAPCs)
                {
                    if (!label.isEmpty()) label = label + "<br>";
                    label = label + e.srcAPC + "<br>" + e.trgAPC;
                }
                if (e.displayAPIDs)
                {
                    if (!label.isEmpty()) label = label + "<br>";
                    label = label + e.srcAPID + ":" + e.trgAPID;
                }
            }
            
            if (!label.isEmpty())
            {
                // This is to place single-line labels above/below the edge
                // rather than on top of it.
                if (!label.contains("<br>"))
                {
                    label = " <br>" + label;
                }
                label = "<html><body style='text-align: center'>" 
                        + label + "</body></html>";
            }
            return label;
        }
    }
	
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
				
			case APID:
                for (JVertex v : getSelectedNodes())
                {
                    for (JEdge e : loadedGraph.getIncidentEdges(v))
                    {
                        e.displayAPIDs = show;
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
	public JUNGGraphSnapshot getGraphStatusSnapshot()
	{	
		JUNGGraphSnapshot snapshot = null;
		if (loadedGraph != null)
		{
			snapshot = new JUNGGraphSnapshot(loadedGraph,layout);
		}
		return snapshot;
	}

//-----------------------------------------------------------------------------
	
	public void dispose() 
	{
		cleanup();
	}
	
//-----------------------------------------------------------------------------

}
