package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JPanel;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.util.DefaultMouseManager;

public class GraphViewerPanel extends JPanel 
{

	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 6492255171927069190L;
	
	/**
	 * CSS string controlling the graphical style of the graph representation
	 */
	private static final String cssStyle = "graph {"
			+ "fill-color: #D9D9D9; "
			+ "} "
			+ "node {"
			+ "shape: rounded-box; "
			+ "size: 30px, 30px; "
			+ "fill-mode: plain; "
			+ "fill-color: grey; "
			+ "stroke-color: #1D2731; "
			+ "stroke-width: 2px; "
			+ "stroke-mode: plain;"
			+ "text-mode: normal;"
			+ "text-style: normal;"
			+ "} "
			+ "node.fragment {"
			+ "fill-color: #4484CE; "
			+ "} "
			+ "node.scaffold {"
			+ "fill-color: #F53240; "
			+ "} "
			+ "node.cap {"
			+ "size: 20px, 20px; "
			+ "fill-color: #57BC90; "
			+ "} "
			+ "node.rcv {"
			+ "size: 20px, 20px; "
			+ "fill-color: #F19F4D; "
			+ "}"
			+ "node.ap {"
			+ "size: 2px, 2px; "
			+ "stroke-width: 1px; "
			+ "text-mode: hidden;"
			+ "fill-color: #FECE00; "
			+ "}"
			+ "node:selected {"
			+ "stroke-color: darkgreen;"
			+ "fill-color: green;"
			+ "}"
			+ "edge {"
			+ "shape: line; "
			+ "size: 1.5px;"
			+ "fill-color: #1D2731; "
			+ "arrow-size: 7px, 5px; "
			+ "}"
			+ "edge.rc {"
			+ "fill-color: #F19F4D; "
			+ "}"
			+ "edge.ap {"
			+ "fill-color: #FECE00;"
			+ "}"
			+ "sprite.edgeLabel {"
			+ "shape: box; "
			+ "size: 0px;" 
			+ "text-mode: normal;"
			+ "text-style: normal;"
			+ "text-background-mode: rounded-box;"
			+ "text-background-color: #D9D9D9;"
			+ "text-padding: 1px;"
			+ "} "
			+ "sprite.apLabel {"
			+ "shape: box; "
			+ "size: 0px;" 
			+ "text-mode: normal;"
			+ "text-style: normal;"
			+ "text-background-mode: none;"
			+ "}"
			+ "sprite.molIdLabel {"
			+ "shape: box; "
			+ "size: 0px;" 
			+ "text-mode: normal;"
			+ "text-style: normal;"
			+ "text-background-mode: none;"
			+ "}"
			+ "sprite.bndTypLabel {"
			+ "shape: box; "
			+ "size: 0px;" 
			+ "text-mode: normal;"
			+ "text-style: normal;"
			+ "text-background-mode: rounded-box;"
			+ "text-background-color: #D9D9D9;"
			+ "text-padding: 1px;"
			+ "}";
	
	private ViewPanel viewpanel;
	private Viewer viewer;
	private Graph graph;
	private SpriteManager sman;
	private GraphMouseManager mouseManager;
	
	public final String SPRITE_APCLASS = "sprite.apClass";
	public final String SPRITE_BNDORD = "sprite.bndOrd";
	public final String SPRITE_FRGID = "sprite.fragId";
	

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
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
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
		for (Component c : this.getComponents())
		{
			this.remove(c);
		}
		this.repaint();
		this.revalidate();
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Load the given graph to the graph viewer.
	 * @param g the graph to load
	 */
	public void loadGraphToViewer(Graph g)
	{
		graph = g;
		graph.addAttribute("ui.quality");
		graph.addAttribute("ui.antialias");
		graph.addAttribute("ui.stylesheet", cssStyle);
		
		viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
		viewer.addDefaultView(false); 
		viewer.enableAutoLayout();
		
		sman = new SpriteManager(graph);
	    
		viewpanel = viewer.getDefaultView();
		mouseManager = new GraphMouseManager();
		viewpanel.setMouseManager(mouseManager);
		this.add(viewpanel);
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Append new labels related to the selected elements in the graph view.
	 * @param sprType the class of labels chosen among the public fields of
	 * this GraphViewerPanel (fields beginning with <code>SPRITE_</code>).
	 */
	public void appendSprites(String sprType)
	{
		switch (sprType)
		{
			case SPRITE_APCLASS:
				for (Node n : getSelectedNodes())
				{
					for (Edge e : graph.getEdgeSet())
					{
						if (e.getSourceNode() == n)
						{
							String sId = "srcApClass-"+e.getId();
							if (!hasSprite(sId))
							{
								Sprite sSrc = sman.addSprite(sId);
								sSrc.setAttribute("ui.class", "apLabel");
								sSrc.addAttribute("ui.label", 
										e.getAttribute("dnp.srcAPClass"));
								sSrc.attachToEdge(e.getId());
								sSrc.setPosition(0.3);
							}
						}
						if (e.getTargetNode() == n)
						{
							String sId = "trgApClass-"+e.getId();
							if (!hasSprite(sId))
							{
								Sprite sSrc = sman.addSprite(sId);
								sSrc.setAttribute("ui.class", "apLabel");
								sSrc.addAttribute("ui.label", 
										e.getAttribute("dnp.trgAPClass"));
								sSrc.attachToEdge(e.getId());
								sSrc.setPosition(0.7);
							}
						}
					}
				}
				break;
				
			case SPRITE_BNDORD:
				for (Edge e : getSelectedEdges())
				{
					String sId = "bndTyp-"+e.getId();
					if (!hasSprite(sId))
					{
						Sprite s = sman.addSprite(sId);
						s.setAttribute("ui.class", "bndTypLabel");
						s.addAttribute("ui.label", e.getAttribute("dnp.bondType"));
						s.attachToEdge(e.getId());
						s.setPosition(0.5);
					}
				}
				break;
				
			case SPRITE_FRGID:
				for (Node n : getSelectedNodes())
				{
					String sId = "molID-"+n.getId();
					if (!hasSprite(sId))
					{
						Sprite s = sman.addSprite(sId);
						s.setAttribute("ui.class", "molIdLabel");
						s.addAttribute("ui.label",
								"MolID: " + n.getAttribute("dnp.molID"));
						s.attachToNode(n.getId());
						s.setPosition(0.5, 0, 0);
					}
				}
				break;
		}		
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Check if a sprite with the given identifier already exists.
	 * @param sprId the identifier
	 * @return <code>true</code> if a sprite with the given identifier is found
	 */
	public boolean hasSprite(String sprId)
	{
		boolean res = false;
		for (Sprite s : sman.sprites())
		{
			if (s.getId().equals(sprId))
			{
				res = true;
				break;
			}
		}
		return res;
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Remove labels related to the selected elements in the graph view.
	 * @param sprType the class of labels chosen among the public fields of
	 * this GraphViewerPanel (fields beginning with <code>SPRITE_</code>).
	 */
	public void removeSprites(String sprType)
	{
		ArrayList<String> sprToDel = new ArrayList<String>();
		switch (sprType)
		{
			case SPRITE_APCLASS:	
				for (Node n : getSelectedNodes())
				{
					for (Edge e : graph.getEdgeSet())
					{
						if (e.getSourceNode() == n)
						{
							Sprite s = sman.getSprite("srcApClass-"+e.getId());
							if (s != null)
							{
								sprToDel.add(s.getId());
							}
						}
						if (e.getTargetNode() == n)
						{
							Sprite s = sman.addSprite("trgApClass-"+e.getId());
							if (s != null)
							{
								sprToDel.add(s.getId());
							}
						}
					}
				}
				break;
				
			case SPRITE_BNDORD:
				for (Edge e : getSelectedEdges())
				{
					Sprite s = sman.addSprite("bndTyp-"+e.getId());
					if (s != null)
					{
						sprToDel.add(s.getId());
					}
				}
				break;
				
			case SPRITE_FRGID:
				for (Node n : getSelectedNodes())
				{
					Sprite s = sman.addSprite("molID-"+n.getId());
					if (s != null)
					{
						sprToDel.add(s.getId());
					}
				}
				break;
		}	
		
		// Finally remove the chosen sprites
		for (String id : sprToDel)
		{
			sman.removeSprite(id);
		}
	}

//-----------------------------------------------------------------------------

	/**
	 * Takes the list of selected edges from the viewer
	 * @return the list of edges
	 */
	public ArrayList<Edge> getSelectedEdges()
	{
		ArrayList<Edge> selectedEdges = new ArrayList<Edge>();
		ArrayList<String> selectedNodeIds = mouseManager.getSelectedNodes();
		for (Edge e : graph.getEdgeSet())
		{
			ArrayList<String> srcAndTrgNodeIDs = new ArrayList<String>();
			srcAndTrgNodeIDs.add(e.getSourceNode().getId());
			srcAndTrgNodeIDs.add(e.getTargetNode().getId());
			if (selectedNodeIds.containsAll(srcAndTrgNodeIDs))
			{
			    selectedEdges.add(e);
			}
		}
		return selectedEdges;
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Takes the list of selected nodes from the viewer
	 * @return the list of nodes
	 */
	public ArrayList<Node> getSelectedNodes()
	{
		ArrayList<Node> selectedNodes = new ArrayList<Node>();
		for (String id : mouseManager.getSelectedNodes())
		{
			selectedNodes.add(graph.getNode(id));
		}
		return selectedNodes;
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Check is there is anything selected in the viewer
	 * @return <code>true</code> if there is at least one element selected
	 */
	public boolean hasSelected()
	{
		return mouseManager.hasSelected();
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Manager class to handle mouse events on graph area
	 */

	private class GraphMouseManager extends DefaultMouseManager
	{
		private double ctrlYstart;
		private double ctrlZoomStart;
		
		private double altXstart;
		private double altYstart;
		
		private double altXCamStart;
		private double altYCamStart;
		
	//-------------------------------------------------------------------------
		
		/**
		 * Check if there are selected elements
		 * @return <code>true</code> if there is at least one element selected
		 */
		public boolean hasSelected()
		{
			for (Node n : this.graph.getNodeSet())
			{
				if (n.hasAttribute("ui.selected"))
				{
					return true;
				}
			}
			return false;
		}
		
	//-------------------------------------------------------------------------
		
		/**
		 * Finds the IDs of the selected nodes
		 * @return the list of IDs
		 */
		public ArrayList<String> getSelectedNodes()
		{
			ArrayList<String> selected = new ArrayList<String>();
			for (Node n : this.graph.getNodeSet())
			{
				if (n.hasAttribute("ui.selected"))
				{
					selected.add(n.getId());
				}
			}
			return selected;
		}
		
	//-------------------------------------------------------------------------

		@Override
		public void mouseClicked(MouseEvent e) 
		{
			double t = 7; // tolerance
			double xa = e.getX() - t;
			double ya = e.getY() - t;
			double xb = e.getX() + t;
			double yb = e.getY() + t;
			Iterable<GraphicElement> elements = view.allNodesOrSpritesIn(xa, ya,
					xb, yb);
			for (GraphicElement g : elements)
			{
				System.out.println("Clocked on element: "+g);
				//TODO: open dialog with details or tooltip
			}
		}
		
	//-------------------------------------------------------------------------

		@Override
		public void mouseDragged(MouseEvent e) 
		{
			if (e.isControlDown())
			{
				double curY = e.getY();
				double relDraggedY = (ctrlYstart - curY) / 
						(viewpanel.getHeight()*2.0);
				double newZoomFactor = ctrlZoomStart 
						+ ctrlZoomStart*relDraggedY;
				if (newZoomFactor>0.05 && newZoomFactor<2.00)
				{
					view.getCamera().setViewPercent(newZoomFactor);
				}
			}
			
			if (e.isAltDown())
			{
				double curX = e.getX();
				double curY = e.getY();
				double relDeltaX = (altXstart - curX) / (viewpanel.getWidth());
				double relDeltaY = (altYstart - curY) / (viewpanel.getHeight());
				double newX = altXCamStart + altXCamStart*relDeltaX;
				double newY = altYCamStart - altYCamStart*relDeltaY;
				double z = view.getCamera().getViewCenter().z;
				view.getCamera().setViewCenter(newX, newY, z);
			}
			
			if (!e.isControlDown() && !e.isAltDown())
			{
				super.mouseDragged(e);
			}
		}
		
	//-------------------------------------------------------------------------
		
		@Override
		public void mousePressed(MouseEvent e) 
		{
			if (e.isControlDown())
			{
				ctrlYstart = e.getY();
				ctrlZoomStart = view.getCamera().getViewPercent();
			}
			
			if (e.isAltDown())
			{
				altXstart = e.getX();
				altYstart = e.getY();
				altXCamStart = view.getCamera().getViewCenter().x;
				altYCamStart = view.getCamera().getViewCenter().y;
			}
			
			if (!e.isControlDown() && !e.isAltDown())
			{
				super.mousePressed(e);
			}
		}	
		
	//-------------------------------------------------------------------------
		
		@Override
		public void mouseReleased(MouseEvent e) 
		{
			if (!e.isControlDown() && !e.isAltDown())
			{
				super.mouseReleased(e);
			}
		}
	}

//-----------------------------------------------------------------------------
}
