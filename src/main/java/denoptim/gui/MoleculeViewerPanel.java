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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.graph.DGraph;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;


/**
 * A panel that allows to visualize 2D/3D molecular structures together with a 
 * list of selectable propertie in a table.
 * 
 * @author Marco Foscato
 */

public class MoleculeViewerPanel extends JPanel
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 1L;

	private Mol3DViewPanel threeDimMolViewer;
	private Mol2DViewPanel twoDimMolViewer;

	private JPanel molViewerHeader;
	private JPanel molViewerCardHolder;
	private JPanel molViewerEmptyCard;
	private JPanel molViewerNeedUpdateCard;
    private JButton btnSwitchTo2DViewer;
    private JButton btnSwitchTo3DViewer;
	private final String EMPTYCARDNAME = "emptyCard";
	private final String UPDATETOVIEW = "updateCard";
    private final String TWODVIEWERCARDNAME = "twoDimViewerCard";
    private final String THREEDVIEWERCARDNAME = "threeDimViewerCard";

    private static final IChemObjectBuilder builder = 
            SilentChemObjectBuilder.getInstance();
	
//-----------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	public MoleculeViewerPanel()
	{
		super(new BorderLayout());
        initialize();
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Initialize the panel and add buttons.
	 */
	private void initialize()
	{	
        // We have a header with buttons to switch between 2D and 3D views.
		molViewerHeader = new JPanel();
		this.add(molViewerHeader, BorderLayout.NORTH);
		molViewerHeader.add(new JLabel("Structure:"));
		String molViewerToolTip = "<html>This viewer shows the chemical "
				+ "structure associated with the current graph.</html>";
		molViewerHeader.setToolTipText(molViewerToolTip);

        btnSwitchTo2DViewer = new JButton("2D");
        btnSwitchTo2DViewer.setToolTipText("Switch to 2D molecular depiction.");
        btnSwitchTo2DViewer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                switchTo2DViewer();
            }});
        btnSwitchTo2DViewer.setEnabled(true); 
        molViewerHeader.add(btnSwitchTo2DViewer);

        btnSwitchTo3DViewer = new JButton("3D");
        btnSwitchTo3DViewer.setToolTipText("Switch to 3D molecular depiction.");
        btnSwitchTo3DViewer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                switchTo3DViewer();
            }});
        btnSwitchTo3DViewer.setEnabled(true);
        molViewerHeader.add(btnSwitchTo3DViewer);


		// The molecular viewers are embedded in a deck of cards that 
		// is meant to show/hide the viewers according to specific needs.
		molViewerCardHolder = new JPanel(new CardLayout());
		this.add(molViewerCardHolder, BorderLayout.CENTER);
		
        // An empty card to show when no structure is loaded.
		molViewerEmptyCard = new JPanel();
		String txt2 = "<html><body width='%1s'><center>No chemical "
				+ "structure.</center>"
				+ "</html>";
		molViewerEmptyCard.add(new JLabel(String.format(txt2, 120)));
		molViewerEmptyCard.setToolTipText(molViewerToolTip);
		molViewerCardHolder.add(molViewerEmptyCard, EMPTYCARDNAME);
		
        // A card to show when the structure needs to be updated.
		molViewerNeedUpdateCard = new JPanel();
		String txt2b = "<html><body width='%1s'><center>Save changes to "
				+ "update the molecular representation.</center>"
				+ "</html>";
		molViewerNeedUpdateCard.add(new JLabel(String.format(txt2b, 120)));
		molViewerNeedUpdateCard.setToolTipText(molViewerToolTip);
		molViewerCardHolder.add(molViewerNeedUpdateCard, UPDATETOVIEW);
		
        // A 3D molecular viewer.
		threeDimMolViewer = new Mol3DViewPanel();
		threeDimMolViewer.enablePartialData(true);
		molViewerCardHolder.add(threeDimMolViewer, THREEDVIEWERCARDNAME);
		
        // A 2D molecular viewer.
        twoDimMolViewer = new Mol2DViewPanel();
        twoDimMolViewer.enablePartialData(true);
        molViewerCardHolder.add(twoDimMolViewer, TWODVIEWERCARDNAME);

        switchToEmptyCard();
	}

//-----------------------------------------------------------------------------
    
    private void switchToUpdateCard()
    {
        btnSwitchTo2DViewer.setEnabled(false);
        btnSwitchTo3DViewer.setEnabled(false);
        ((CardLayout) molViewerCardHolder.getLayout()).show(
            molViewerCardHolder, UPDATETOVIEW);
    }

//-----------------------------------------------------------------------------
	
    void switchToEmptyCard()
    {
        btnSwitchTo2DViewer.setEnabled(false);
        btnSwitchTo3DViewer.setEnabled(false);
        ((CardLayout) molViewerCardHolder.getLayout()).show(
            molViewerCardHolder, EMPTYCARDNAME);
    }

//-----------------------------------------------------------------------------

    private void switchTo2DViewer()
    {
        btnSwitchTo2DViewer.setEnabled(false);
        btnSwitchTo3DViewer.setEnabled(true);
        ((CardLayout) molViewerCardHolder.getLayout()).show(
            molViewerCardHolder, TWODVIEWERCARDNAME);
    }

//-----------------------------------------------------------------------------

    private void switchTo3DViewer()
    {
        btnSwitchTo2DViewer.setEnabled(true);
        btnSwitchTo3DViewer.setEnabled(false);
        ((CardLayout) molViewerCardHolder.getLayout()).show(
            molViewerCardHolder, THREEDVIEWERCARDNAME);
    }
	
//-----------------------------------------------------------------------------
    
    /**
     * Loads the given graph into the graph viewer.
     * @param dnGraph the graph to load.
     * @param mol the molecular representation of the graph. Use this to avoid
     * converting the graph into a molecular representation every time you load
     * the same graph.
     */
    public void loadContent(IAtomContainer mol)
    {
        try {
            threeDimMolViewer.loadContent(mol);
            twoDimMolViewer.loadContent(mol);
            switch(GUIPreferences.molDimDisplay) {
                case THREE_DIMENSIONAL:
                    switchTo3DViewer();
                    break;
                case TWO_DIMENSIONAL:
                    switchTo2DViewer();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not display molecular data: "+
                    e.getCause() + " " + e.getMessage());
            switchToEmptyCard();
        }
    }

//-----------------------------------------------------------------------------
	
	/**
	 * Updates the molecular representation of the loaded structure. We rebuild 
	 * the molecular representation.
	 */
	public IAtomContainer update(DGraph dnGraph)
	{
	    // We lease data in the viewer to increase speed of execution, but the
        // leftover is outdated! This is the meaning of 'true'
	    threeDimMolViewer.clearAll(true);
        twoDimMolViewer.clearAll(true);

        IAtomContainer mol = builder.newAtomContainer();
	    if (!dnGraph.containsAtoms())
	    {
            switchToEmptyCard();
	        return mol;
	    }
	    
        Logger logger = Logger.getLogger(GUI.GUILOGGER);
        ThreeDimTreeBuilder tb = new ThreeDimTreeBuilder(logger, GUI.PRNG);
        try {
            mol = tb.convertGraphTo3DAtomContainer(dnGraph, true);
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("Couldn't make 3D-tree representation: "
                    + t.getMessage());
        }

        if (mol.getAtomCount() > 0)
        {
            loadContent(mol);
        } else {
            switchToEmptyCard();
        }
        return mol;
	}
	
//-----------------------------------------------------------------------------
    
    /**
     * Triggers the generation of the molecular representation of the loaded
     * graph.
     */
	
    public void renderToNeedUpdate()
    {
        // We leave data in the viewer to increase speed of execution, but the
        // leftover is outdated! This is the meaning of 'dataIsComing=true'
        threeDimMolViewer.clearAll(true); 
        twoDimMolViewer.clearAll(true);
        switchToUpdateCard();
    }
	
//-----------------------------------------------------------------------------
    
    /**
     * Clears the molecular viewer and hides it behind the empty card.
     */
    public void clear()
    {
        // We lease data in the viewer to increase speed of execution, but the
        // leftover is outdated! This is the meaning of 'true'
        threeDimMolViewer.clearAll(true);
        twoDimMolViewer.clearAll(true);
        switchToEmptyCard();
    }
    
//-----------------------------------------------------------------------------

	/*
	 * This is needed to stop GraphStream and Jmol threads upon closure of this
	 * gui card.
	 */
	public void dispose() 
	{
		threeDimMolViewer.dispose();
		twoDimMolViewer.dispose();
	}
		
//-----------------------------------------------------------------------------
  	
}
