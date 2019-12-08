package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.Collection;

import javax.swing.JPanel;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Camera;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerPipe;
import org.graphstream.ui.view.util.DefaultMouseManager;
import org.graphstream.ui.view.util.MouseManager;
import org.graphstream.ui.view.util.ShortcutManager;

import signature.simple.SimpleGraph;

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
			+ "node.rca {"
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
			/*
			+ "text-mode: normal;"
			+ "text-style: normal;"
			+ "text-background-mode: rounded-box;"
			+ "text-background-color: #D9D9D9;"
			+ "text-padding: 1px;"
			*/
			+ "}"
			+ "edge.rc {"
			+ "fill-color: #F19F4D; "
			+ "}"
			+ "edge.ap {"
			+ "fill-color: #FECE00;"
			+ "}"
			+ "sprite.edgeLabel {"
			+ "shape: box; " // the box is actually hidden behind the label
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
	 * 
	 */
	private void initialize()
	{
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		this.setLayout(new BorderLayout());
		this.setBackground(Color.decode("#D9D9D9"));
		this.setToolTipText("No graph to visualize");
	}
	
//-----------------------------------------------------------------------------
	
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
		//TODO process edges to get sprites
		//TODO del tmp code
		Sprite se = sman.addSprite("S_rc1rc2");
		se.attachToEdge("rc1rc2");
		se.setPosition(0.5);
		se.addAttribute("ui.class", "edgeLabel");
	    for (Sprite s : sman) 
	    {
	        s.addAttribute("ui.label", s.getId());
	    }
		
		viewpanel = viewer.getDefaultView();
		viewpanel.setMouseManager(new GraphMouseManager());
		this.add(viewpanel);
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Manager class to handle mouse events on graph area
	 */

	public class GraphMouseManager extends DefaultMouseManager
	{
		private double ctrlYstart;
		private double ctrlZoomStart;
		
		private double altXstart;
		private double altYstart;
		
		private double altXCamStart;
		private double altYCamStart;

		@Override
		public void mouseClicked(MouseEvent e) 
		{
			double t = 7; // tolerance
			double xa = e.getX() - t;
			double ya = e.getY() - t;
			double xb = e.getX() + t;
			double yb = e.getY() + t;
			Iterable<GraphicElement> elements = view.allNodesOrSpritesIn(xa, ya, xb, yb);
			for (GraphicElement g : elements)
			{
				System.out.println("Clocked on element: "+g);
				//TODO: open dialog with details or tooltip
			}
		}

		@Override
		public void mouseDragged(MouseEvent e) 
		{
			if (e.isControlDown())
			{
				double curY = e.getY();
				double relDraggedY = (ctrlYstart - curY) / (viewpanel.getHeight()*2.0);
				double newZoomFactor = ctrlZoomStart + ctrlZoomStart*relDraggedY;
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
				double newY = altYCamStart + altYCamStart*relDeltaY;
				double z = view.getCamera().getViewCenter().z;
				view.getCamera().setViewCenter(newX, newY, z);
			}
			
			if (!e.isControlDown() && !e.isAltDown())
			{
				super.mouseDragged(e);
			}
		}
		
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
		
		@Override
		public void mouseReleased(MouseEvent e) 
		{
			if (!e.isControlDown() && !e.isAltDown())
			{
				super.mouseReleased(e);
			}
		}
	}
}
