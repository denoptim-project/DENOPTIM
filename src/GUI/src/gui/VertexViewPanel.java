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


import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.vecmath.Point3d;

import denoptim.utils.DENOPTIMMoleculeUtils;
import org.jmol.viewer.Viewer;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.atomtype.CDKAtomTypeMatcher;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomType;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.modeling.builder3d.ModelBuilder3D;
import org.openscience.cdk.modeling.builder3d.TemplateHandler3D;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.AtomTypeManipulator;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMFragment.BBType;
import denoptim.molecule.DENOPTIMTemplate;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.EmptyVertex;
import denoptim.utils.DENOPTIMMathUtils;


/**
 * A panel for visualising vertexes. This is a deck of cards that brings up a 
 * specific card depending on the type of vertex to visualize.
 * 
 * @author Marco Foscato
 */

public class VertexViewPanel extends JPanel
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The currently loaded vertex
	 */
	private DENOPTIMVertex vertex;
	
	/**
	 * Temporary list of attachment points of the current fragment
	 */
	protected Map<Integer,DENOPTIMAttachmentPoint> mapAPs = null;
	
	/**
	 * Flag signalling that data about APs has been changed in the GUI
	 */
	public boolean alteredAPData = false;
	protected DefaultTableModel apTabModel;
	
	private JPanel titlePanel;
	
    private JLabel labTitle;
    private JButton btnSwitchToNodeViewer;
    private JButton btnSwitchToMolViewer;
    
    private JPanel centralPanel;
	
    private JPanel emptyViewerCard;
	private FragmentViewPanel fragViewer;
	private VertexAsGraphViewPanel graphNodeViewer;
	private IVertexAPSelection activeViewer;
    protected final String EMPTYCARDNAME = "emptyCard";
    protected final String GRAPHVIEWERCARDNAME = "emptyVertesCard";
    protected final String MOLVIEWERCARDNAME = "fragViewerCard";
	    
	private boolean editableAPTable = false;
	
	private JComponent parent;
	

//-----------------------------------------------------------------------------

	/**
	 * Constructor that allows to specify whether the AP table is editable or 
	 * not.
	 * @param editableTable use <code>true</code> to make the AP table editable
	 */
	public VertexViewPanel(boolean editableTable)
	{
		this(null,editableTable);
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Constructor that allows to specify whether the AP table is editable or 
	 * not.
	 * @param editableTable use <code>true</code> to make the AP table editable
	 */
	public VertexViewPanel(JComponent parent, boolean editableTable)
	{
		this(parent,editableTable,340);
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Constructor that allows to specify whether the AP table is editable or 
	 * not.
	 * @param editableTable use <code>true</code> to make the AP table editable
	 * @param dividerPosition allows to set the initial position of the divide
	 */
	public VertexViewPanel(JComponent parent, boolean editableTable, int dividerPosition)
	{
	    super(new BorderLayout());
	    this.parent = parent;
		this.editableAPTable = editableTable;
		initialize(dividerPosition);
	}
	
//-----------------------------------------------------------------------------

	private void initialize(int dividerPosition)
	{
	    centralPanel = new JPanel(new CardLayout());
	    this.add(centralPanel, BorderLayout.CENTER);
	    
	    titlePanel = new JPanel();
	            
        labTitle = new JLabel("");
        titlePanel.add(labTitle);
        
        btnSwitchToNodeViewer = new JButton("Node View");
        btnSwitchToNodeViewer.setToolTipText("Switch to graph node depiction "
                + "of this vertex.");
        btnSwitchToNodeViewer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                switchToGraphNodeViewer();
            }
        });
        btnSwitchToNodeViewer.setEnabled(false);
        titlePanel.add(btnSwitchToNodeViewer);
        
        btnSwitchToMolViewer = new JButton("Molecule View");
        btnSwitchToMolViewer.setToolTipText("Switch to molecular depiction "
                + "of this vertex.");
        btnSwitchToMolViewer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                switchToMolecularViewer();
            }
        });
        btnSwitchToMolViewer.setEnabled(false);
        titlePanel.add(btnSwitchToMolViewer);
        
        this.add(titlePanel, BorderLayout.NORTH);
        
        emptyViewerCard = new JPanel();
        emptyViewerCard.setToolTipText("Vertexes are displayed here.");
        centralPanel.add(emptyViewerCard, EMPTYCARDNAME);
        
        graphNodeViewer = new VertexAsGraphViewPanel(this, editableAPTable, 300);
        centralPanel.add(graphNodeViewer, GRAPHVIEWERCARDNAME);
	        
        fragViewer = new FragmentViewPanel(false);
        centralPanel.add(fragViewer, MOLVIEWERCARDNAME);
        apTabModel = fragViewer.apTabModel;
        
        switchToEmptyCard();
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Check for unsaved edits to the AP data
	 * @return <code>true</code> if there are unsaved edits
	 */
	public boolean hasUnsavedAPEdits()
	{
		return alteredAPData;
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Overrides the flag signaling unsaved edits to saying that there are no
	 * altered data.
	 */
	public void deprotectEdits()
	{
		alteredAPData = false;
	}
	
//-----------------------------------------------------------------------------
	
	private void switchToEmptyCard()
	{
        btnSwitchToMolViewer.setEnabled(false);
        btnSwitchToNodeViewer.setEnabled(false);
	    ((CardLayout) centralPanel.getLayout()).show(centralPanel, 
	            EMPTYCARDNAME);
	    activeViewer = null;
	}
	
//-----------------------------------------------------------------------------
	
	private void switchToGraphNodeViewer()
	{
	    ((CardLayout) centralPanel.getLayout()).show(centralPanel, 
	            GRAPHVIEWERCARDNAME);
	    activeViewer = graphNodeViewer;
	}
	
//-----------------------------------------------------------------------------
    
    private void switchToMolecularViewer()
    {
        ((CardLayout) centralPanel.getLayout()).show(centralPanel, 
                MOLVIEWERCARDNAME);
        activeViewer = fragViewer;
    }

//-----------------------------------------------------------------------------
	
	/**
	 * Loads a molecule build from a smiles string. The 3D geometry is either 
	 * taken from remote CACTUS service (requires connection
	 * to the Internet) or built with CDK tools, as a fall-back. The CDK
	 * builder, however will produce a somewhat lower quality conformation than
	 * that obtained from on online generator.
	 * @param smiles the SMILES of the molecule to load
	 * @return <code>true</code> if the SMILES could be converted into a 3D 
	 * structure
	 * @throws DENOPTIMException 
	 */
	public boolean loadSMILES(String smiles)
	{	
	    switchToMolecularViewer();
	    return fragViewer.loadSMILES(smiles);
	}

//-----------------------------------------------------------------------------
	
	/**
	 * Returns the chemical representation of the currently loaded chemical
	 * object. In case of mismatch between the system loaded into the Jmol
	 * viewer and the one in the local memory, we take that from Jmol and
	 * made it be The 'current fragment'. Previously set references to the
	 * previous 'current fragment' will make no sense anymore.
	 * @return the chemical representation of what is currently visualised.
	 * Can be empty and null.
	 */
	public DENOPTIMFragment getLoadedStructure()
	{
	    switchToMolecularViewer();
	    return fragViewer.getLoadedStructure();
	}

//-----------------------------------------------------------------------------
	
	/**
	 * Loads a structure in the Jmol viewer.
	 * @param mol the structure to load
	 */
	public void loadPlainStructure(IAtomContainer mol)
	{
	    fragViewer.loadPlainStructure(mol);
	    switchToMolecularViewer();
	}

//-----------------------------------------------------------------------------
    
    /**
     * Loads the given vertex to this viewer.
     * The molecular data is loaded in the Jmol viewer,
     * and the attachment point (AP) information in the the list of APs.
     * Jmol is not aware of AP-related information, so this also launches
     * the generation of the graphical objects representing the APs.
     * @param frag the fragment to visualize
     */
    public void loadVertexToViewer(DENOPTIMVertex v)
    {
        if (v instanceof DENOPTIMFragment) {
            labTitle.setText("Fragment");
            DENOPTIMFragment frag = (DENOPTIMFragment) v;
            loadFragmentToViewer(frag);
        } else if (v instanceof EmptyVertex) {
            labTitle.setText("EmptyVertex");
            EmptyVertex ev = (EmptyVertex) v;
            loadEmptyVertexToViewer(ev);
        } else if (v instanceof DENOPTIMTemplate) {
            labTitle.setText("Template");
            DENOPTIMTemplate tmpl = (DENOPTIMTemplate) v;
            loadTemplateToViewer(tmpl);
        } else {
            System.err.println("Loading empty card as a result of vertex with " 
                    + "type " + v.getClass().getName());
            switchToEmptyCard();
        }
    }

//-----------------------------------------------------------------------------
    
    /**
     * Loads the given empty vertex to this viewer. This type of vertex does
     * not have any associated molecular data, but does have attachment points
     * (APs) that are listed in table of APs.
     * @param ev the vertex to visualize
     */
    private void loadEmptyVertexToViewer(EmptyVertex ev)
    {
        btnSwitchToMolViewer.setEnabled(false);
        btnSwitchToNodeViewer.setEnabled(false);
        graphNodeViewer.loadVertexToViewer(ev);
        switchToGraphNodeViewer();
    }
    
//-----------------------------------------------------------------------------
	
	/**
	 * Loads the given fragments to this viewer.
	 * The molecular data is loaded in the Jmol viewer,
	 * and the attachment point (AP) information in the the list of APs.
	 * Jmol is not aware of AP-related information, so this also launches
	 * the generation of the graphical objects representing the APs.
	 * @param frag the fragment to visualize
	 */
	private void loadFragmentToViewer(DENOPTIMFragment frag)
	{		
		fragViewer.loadFragmentToViewer(frag);
        btnSwitchToMolViewer.setEnabled(true);
        btnSwitchToNodeViewer.setEnabled(true);
        graphNodeViewer.loadVertexToViewer(frag);
		switchToMolecularViewer();
	}

//-----------------------------------------------------------------------------
    
    /**
     * Loads the given template to this viewer. The style of the depiction, 
     * which is plotted by different viewers, depends on the content of the
     * template. If the template contains a molecular representation, then a 
     * molecular viewer will be used. Otherwise a single-node graph will be 
     * used to represent a template with a partially defined content, but a
     * given set of attachment points.
     * @param tmpl the template to visualize
     */
    private void loadTemplateToViewer(DENOPTIMTemplate tmpl)
    {       
        if (tmpl.containsAtoms())
        {
            DENOPTIMFragment frag = new DENOPTIMFragment();
            loadFragmentToViewer(frag);
            btnSwitchToMolViewer.setEnabled(true);
            btnSwitchToNodeViewer.setEnabled(true);
        }
        graphNodeViewer.loadVertexToViewer(tmpl);
        switchToGraphNodeViewer();
    }
    
//-----------------------------------------------------------------------------

	/**
	 * Removes the currently visualized molecule and AP table
	 */
	public void clearCurrentSystem()
	{
	    fragViewer.mapAPs = null;
		fragViewer.clearAPTable();
		switchToEmptyCard();
	}
	
//-----------------------------------------------------------------------------
    
    /**
     * Clears the molecular viewer. This operation is slow! 
     * It usually a second or two.
     */
    public void clearMolecularViewer()
    {
        //TODO-V3: evaluate reducing to the minimum the need to run this: it is a slow command!
        fragViewer.clearMolecularViewer();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Identifies which attachment points are selected in the currently active 
     * viewer.
     * @return the list of attachment points indexes
     */
    public ArrayList<Integer> getSelectedAPIDs()
    {
        return activeViewer.getSelectedAPIDs();
    }
	
//-----------------------------------------------------------------------------
    
	/**
	 * Identifies the atoms that are selected in the Jmol viewer
	 * @return the list of selected atoms
	 */
    protected ArrayList<IAtom> getAtomsSelectedFromJMol()
    {
        return fragViewer.getAtomsSelectedFromJMol();
    }
 	
//-----------------------------------------------------------------------------

	/**
	 * Allows to activate and deactivate the listener.
	 * @param var use <code>true</code> to activate the listener
	 */
    protected void activateTabEditsListener(boolean var)
    {
        fragViewer.activateTabEditsListener(var);
    }

//-----------------------------------------------------------------------------
    
    /*
     * This is needed to stop Jmol threads
     */
	protected void dispose() 
	{
		fragViewer.dispose();
		
	}
  	
//-----------------------------------------------------------------------------

}
