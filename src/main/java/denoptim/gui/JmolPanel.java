package gui;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolViewer;

public class JmolPanel extends JPanel
{
	
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 1699908697703788097L;

	protected JmolViewer viewer;

    private final Dimension hostPanelSize = new Dimension();

    public JmolPanel() {
        viewer = JmolViewer.allocateViewer(this, new SmarterJmolAdapter(), 
        null, null, null, null, null); //NB: can add listener here
    }
    
//------------------------------------------------------------------------------

	@Override
    public void paint(Graphics g) {
        getSize(hostPanelSize);
        viewer.renderScreenImage(g, hostPanelSize.width, hostPanelSize.height);
    }
	
//------------------------------------------------------------------------------

	public void dispose() {
		viewer.dispose();
	}

//------------------------------------------------------------------------------
	
}
