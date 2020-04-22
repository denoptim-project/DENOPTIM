package gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.graphstream.graph.Element;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;


/**
 * This class collects the graph and sprite manager used to visualize a 
 * GraphStream graph. The use is to store a snapshot of a visualized graph.
 *   
 * @author Marco Foscato
 */

public class GSGraphSnapshot {

	/**
	 * the graph
	 */
	private Graph graph;
	
	/**
	 * The collection of elements with sprites (by sprite type)
	 */
	private Map<String,ArrayList<Element>> mapOfSprites = 
			new HashMap<String,ArrayList<Element>>();
	
//-----------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	public GSGraphSnapshot(Graph g, SpriteManager sm)
	{
		graph = g;
		
		Iterator<Sprite> spri = sm.iterator();
		while (spri.hasNext())
		{
			Sprite s = spri.next();
			Element el = s.getAttachment();
			String sprClass = s.getAttribute("ui.class");
			if (mapOfSprites.keySet().contains(sprClass))
			{
				mapOfSprites.get(sprClass).add(el);
			}
			else
			{
				ArrayList<Element> lst = new ArrayList<Element>();
				lst.add(el);
				mapOfSprites.put(sprClass,lst);
			}
		}
	}
	
//-----------------------------------------------------------------------------

	public String getGraphId()
	{
		return graph.getId();
	}
	
//-----------------------------------------------------------------------------
	
	public ArrayList<Element> getSpritesOfType(String sType)
	{
		return mapOfSprites.get(sType);
	}
	
//-----------------------------------------------------------------------------

	public boolean hasSpritesOfType(String sType)
	{
		return mapOfSprites.keySet().contains(sType);
	}
	
//-----------------------------------------------------------------------------
	
	public Iterator<Node> nodeIterator()
	{
		return graph.iterator();
	}
	
//-----------------------------------------------------------------------------
	
}
