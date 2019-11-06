package gui;

import java.awt.Component;
import java.awt.LayoutManager;

import javax.swing.JPanel;

/*
 * Trying to add references to toolbar, but is not working, so consider removing this and replace GUIMainPanel with plain JPanel
 */

public class GUIMainPanel extends JPanel 
{
	/**
	 * Reference to the tool bar of the GUI
	 */
	private ToolBar toolBar;
	
	public GUIMainPanel(LayoutManager layout, ToolBar toolBar) 
	{
		super(layout);
		this.toolBar = toolBar;
	}

	@Override 
	public Component add(Component comp)
	{
		super.add(comp);
		//NOT WORKING
		//toolBar.addRefTo((JPanel) comp);
		return comp;
	}

}
