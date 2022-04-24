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
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.graph.AttachmentPoint;
import denoptim.graph.DGraph;
import denoptim.graph.EmptyVertex;
import denoptim.graph.Fragment;
import denoptim.graph.Template;
import denoptim.graph.Vertex;
import denoptim.gui.GraphViewerPanel.JVertex;
import denoptim.gui.GraphViewerPanel.JVertexType;
import denoptim.gui.GraphViewerPanel.LabelType;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.utils.MoleculeUtils;
import denoptim.utils.Randomizer;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;


/**
 * A panel that collects three viewers: 
 * <ul>
 * <li>one for Graphs,</li>
 * <li>one for vertex content,</li>
 * <li>and one for molecular structures.</li>
 * </ul>
 * 
 * @author Marco Foscato
 */

public class GraphVertexMolViewerPanel extends JSplitPane
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The unsaved version of the currently loaded graph
	 */
	private DGraph dnGraph;
	
	/**
	 * The snapshot of the old (removed) visualized GraphStrem system. 
	 * Used only to remember stuff like sprites and node positions.
	 */
	private JUNGGraphSnapshot oldGSStatus;
	
	private JSplitPane leftPane;
	protected FragmentViewPanel fragViewer;
	private JPanel fragViewerPanel;
	private JPanel fragViewerHeader;
	private JPanel fragViewerCardHolder;
	private JPanel fragViewerEmptyCard;
	private JPanel fragViewerNotDuableCard;
	private JPanel fragViewerNoFSCard;
    private GraphVertexMolViewerPanel fragViewerTmplViewerCard;
	protected MoleculeViewPanel molViewer;
	private JPanel molViewerPanel;
	private JPanel molViewerHeader;
	private JPanel molViewerCardHolder;
	private JPanel molViewerEmptyCard;
	private JPanel molViewerNeedUpdateCard;
	protected final String NOFSCARDNAME = "noFSCard";
	protected final String EMPTYCARDNAME = "emptyCard";
	protected final String NOTDUABLECARDNAME = "notDoableCard";
	protected final String UPDATETOVIEW = "updateCard";
	protected final String MOLVIEWERCARDNAME = "molViewerCard";
	protected final String FRAGVIEWERCARDNAME = "fragViewerCard";
	protected final String TMPLVIEWERCARDNAME = "tmplViewwerCard";
	protected GraphViewerPanel graphViewer;
	
	//Default divider location
	private final double DEFDIVLOC = 0.5;
	private double defDivLoc = DEFDIVLOC;
	
	private static final IChemObjectBuilder builder = 
	            SilentChemObjectBuilder.getInstance();
	
//-----------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	public GraphVertexMolViewerPanel()
	{
		initialize();
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Initialize the panel and add buttons.
	 */
	private void initialize()
	{
		setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		setOneTouchExpandable(true);
		// WARNING: setting the divider location has to be node after the 
		// split pane is visible
		//setDividerLocation(defDivLoc);
		setResizeWeight(0.5);
		
		// In the left part we have the mol and frag viewers
		leftPane = new JSplitPane();
		leftPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		leftPane.setOneTouchExpandable(true);
		leftPane.setResizeWeight(0.5);
		setLeftComponent(leftPane);
		
		graphViewer = new GraphViewerPanel();
		setRightComponent(graphViewer);
		graphViewer.addPropertyChangeListener(
				new PropertyChangeListenerProxy(
						GraphViewerPanel.PROPERTYNODECLICKED, 
						new NodeClickedListener()));
        graphViewer.addPropertyChangeListener(
                new PropertyChangeListenerProxy(
                        GraphViewerPanel.PROPERTYMOUSEMODE, 
                        new MouseModeChoiceListener()));
		
		fragViewerPanel = new JPanel(new BorderLayout());
		fragViewerHeader = new JPanel();
		fragViewerHeader.add(new JLabel("Node content:"));
		String fragViewerToolTip = "<html>This viewer shows the "
				+ "content of the selected vertex.<br>"
				+ "Click on a node to select it and display its "
				+ "content here.</html>";
		fragViewerHeader.setToolTipText(fragViewerToolTip);
		fragViewerPanel.add(fragViewerHeader, BorderLayout.NORTH);
		fragViewerCardHolder = new JPanel(new CardLayout());
		
		fragViewerPanel.add(fragViewerCardHolder, BorderLayout.CENTER);
		
		fragViewerEmptyCard = new JPanel();
		String txt = "<html><body width='%1s'><center>No chosen node.</center>"
		        + "</html>";
		fragViewerEmptyCard.add(new JLabel(String.format(txt, 120)));
		fragViewerEmptyCard.setToolTipText(fragViewerToolTip);
		fragViewerCardHolder.add(fragViewerEmptyCard, EMPTYCARDNAME);
		
		fragViewerNotDuableCard = new JPanel();
        String txtn = "<html><body width='%1s'><center>Content not visualizable"
                + "</center></html>";
        fragViewerNotDuableCard.add(new JLabel(String.format(txtn, 120)));
        fragViewerNotDuableCard.setToolTipText(fragViewerToolTip);
        fragViewerCardHolder.add(fragViewerNotDuableCard, NOTDUABLECARDNAME);
		
		fragViewerNoFSCard = new JPanel();
		String txtb = "<html><body width='%1s'><center>To inspect the content "
				+ "of nodes, please load a space of building blocks, "
				+ "and re-open this tab.</center></html>";
		fragViewerNoFSCard.add(new JLabel(String.format(txtb, 120)));
		fragViewerNoFSCard.setToolTipText(fragViewerToolTip);
		fragViewerCardHolder.add(fragViewerNoFSCard, NOFSCARDNAME);
				
		fragViewer = new FragmentViewPanel(false);
		fragViewerCardHolder.add(fragViewer, FRAGVIEWERCARDNAME);
		
		((CardLayout) fragViewerCardHolder.getLayout()).show(
				fragViewerCardHolder, EMPTYCARDNAME);
		
		leftPane.setTopComponent(fragViewerPanel);
		
		// The molecular viewer is embedded in a container structure that 
		// is meant to show/hide the molViewer according to specific needs.
		molViewerPanel = new JPanel(new BorderLayout());
		molViewerHeader = new JPanel();
		molViewerHeader.add(new JLabel("Associated Structure:"));
		String molViewerToolTip = "<html>This viewer shows the chemical "
				+ "structure associated with the current graph.</html>";
		molViewerHeader.setToolTipText(molViewerToolTip);
		molViewerPanel.add(molViewerHeader, BorderLayout.NORTH);
		molViewerCardHolder = new JPanel(new CardLayout());
		
		molViewerPanel.add(molViewerCardHolder, BorderLayout.CENTER);
		
		molViewerEmptyCard = new JPanel();
		String txt2 = "<html><body width='%1s'><center>No chemical "
				+ "structure.</center>"
				+ "</html>";
		molViewerEmptyCard.add(new JLabel(String.format(txt2, 120)));
		molViewerEmptyCard.setToolTipText(molViewerToolTip);
		molViewerCardHolder.add(molViewerEmptyCard, EMPTYCARDNAME);
		
		molViewerNeedUpdateCard = new JPanel();
		String txt2b = "<html><body width='%1s'><center>Save changes to "
				+ "update the molecular representation.</center>"
				+ "</html>";
		molViewerNeedUpdateCard.add(new JLabel(String.format(txt2b, 120)));
		molViewerNeedUpdateCard.setToolTipText(molViewerToolTip);
		molViewerCardHolder.add(molViewerNeedUpdateCard, UPDATETOVIEW);
		
		molViewer = new MoleculeViewPanel();
		molViewer.enablePartialData(true);
		molViewerCardHolder.add(molViewer, MOLVIEWERCARDNAME);
		((CardLayout) molViewerCardHolder.getLayout()).show(
				molViewerCardHolder, EMPTYCARDNAME);
		
		leftPane.setBottomComponent(molViewerPanel);
	}
	
//-----------------------------------------------------------------------------
    
    /**
     * Loads the given graph into the graph viewer.
     * @param dnGraph the graph to load.
     * @param mol the molecular representation of the graph. Use this to avoid
     * converting the graph into a molecular representation every time you load
     * the same graph.
     * @param keepSprites if <code>true</code> we'll keep track of old labels.
     */
    public void loadDnGraphToViewer(DGraph dnGraph, IAtomContainer mol, 
            boolean keepSprites)
    {
        try {
            loadDnGraphToViewer(dnGraph,keepSprites); 
            molViewer.loadChemicalStructure(mol);
            bringCardToTopOfMolViewer(MOLVIEWERCARDNAME);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not read molecular data: "+
                    e.getCause() + " " + e.getMessage());
            bringCardToTopOfMolViewer(EMPTYCARDNAME);
        }
    }
	
//-----------------------------------------------------------------------------
	
	/**
	 * Loads the given graph into the graph viewer.
	 * @param dnGraph the graph to load.
	 * @param keepLabels if <code>true</code> we'll keep track of old labels.
	 * loaded.
	 */
	public void loadDnGraphToViewer(DGraph dnGraph, boolean keepLabels)
	{
	    this.dnGraph = dnGraph;
	    resetFragViewerCardDeck();
		
		// Keep a snapshot of the old data visualized
		if (keepLabels)
		{
			oldGSStatus = graphViewer.getGraphStatusSnapshot();
		} else {
			oldGSStatus = null;
		}
		
		graphViewer.cleanup();
		graphViewer.loadGraphToViewer(dnGraph, oldGSStatus);
		
		if (dnGraph.getVertexCount()==1)
		{
		    defDivLoc = 0.85; //Rezising occures upon painting.
		    leftPane.setDividerLocation(1.0); // hide redundant jmol panel
		    visualizeVertexInNestedViewer(dnGraph.getVertexAtPosition(0));
		}
	}

//-----------------------------------------------------------------------------
    
    /**
     * Triggers the generation of the molecular representation of the loaded
     * graph.
     */
	
    public void renderMolVieverToNeedUpdate()
    {
        // We lease data in the viewer to increase speed of execution, but the
        // leftover is outdated! This is the meaning of 'true'
        molViewer.clearAll(true); 
        bringCardToTopOfMolViewer(UPDATETOVIEW);
    }
	
//-----------------------------------------------------------------------------
	
	/**
	 * Updates the molecular representation of the loaded graph. We rebuild 
	 * the molecular representation.
	 * This method is needed in case of changes to the loaded
	 * graph, to project those changes in the graph into the molecular
	 * representation.
	 */
	public IAtomContainer updateMolevularViewer()
	{
	    // We lease data in the viewer to increase speed of execution, but the
        // leftover is outdated! This is the meaning of 'true'
	    molViewer.clearAll(true);

        IAtomContainer mol = builder.newAtomContainer();
	    if (!dnGraph.containsAtoms())
	    {
            bringCardToTopOfMolViewer(EMPTYCARDNAME);
	        return mol;
	    }
	    
        Logger logger = Logger.getLogger(GUI.GUILOGGER);
        ThreeDimTreeBuilder tb = new ThreeDimTreeBuilder(logger, GUI.PRNG);
        try {
            mol = tb.convertGraphTo3DAtomContainer(dnGraph);
            MoleculeUtils.removeUsedRCA(mol,dnGraph, logger);
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("Couldn't make 3D-tree representation: "
                    + t.getMessage());
        }

        if (mol.getAtomCount() > 0)
        {
            try {
                molViewer.loadChemicalStructure(mol);
                bringCardToTopOfMolViewer(MOLVIEWERCARDNAME);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Could not read molecular data: "+
                        e.getCause() + " " + e.getMessage());
                bringCardToTopOfMolViewer(EMPTYCARDNAME);
            }
        } else {
            bringCardToTopOfMolViewer(EMPTYCARDNAME);
        }
        return mol;
	}

//-----------------------------------------------------------------------------
	
	/**
	 * Changes the appearance of the vertex visualisation panel to an empty
	 * card that is consistent with the presence or lack of a loaded fragment
	 * space.
	 */
	public void resetFragViewerCardDeck()
	{
	    if (fragViewer != null)
        {
	        // We lease data in the viewer to increase speed of execution, but the
	        // leftover is outdated! This is the meaning of 'true'
            fragViewer.clearAll(true);
            bringCardToTopOfVertexViewer(EMPTYCARDNAME);
            removeNestedGraphViewer();
        }
	}
	
//-----------------------------------------------------------------------------
	
	private void removeNestedGraphViewer()
	{
	    if (fragViewerTmplViewerCard != null)
        {
            ((CardLayout) fragViewerCardHolder.getLayout()).
            removeLayoutComponent(fragViewerTmplViewerCard);
            fragViewerTmplViewerCard.dispose();
            fragViewerTmplViewerCard = null;
        }
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Clears the current graph viewer but keeps track of the latest graph 
	 * loaded. 
	 */
	public void clearCurrentSystem()
	{
		dnGraph = null;
        graphViewer.cleanup();
        clearVertexViewer();
        clearMolecularViewer();
	}
	
//-----------------------------------------------------------------------------
    
    /**
     * Clears the molecular viewer and hides it behind the empty card.
     */
    public void clearVertexViewer()
    {
        // We lease data in the viewer to increase speed of execution, but the
        // leftover is outdated! This is the meaning of 'true'
        fragViewer.clearAll(true);
        bringCardToTopOfVertexViewer(EMPTYCARDNAME);
    }
	
//-----------------------------------------------------------------------------
    
    /**
     * Clears the molecular viewer and hides it behind the empty card.
     */
    public void clearMolecularViewer()
    {
        // We lease data in the viewer to increase speed of execution, but the
        // leftover is outdated! This is the meaning of 'true'
        molViewer.clearAll(true);
        bringCardToTopOfMolViewer(EMPTYCARDNAME);
    }
    
//-----------------------------------------------------------------------------
	
	/**
	 * Listener for identifying the node on which the user has clicked and 
	 * load the corresponding fragment into the fragment viewer pane.
	 */
	private class NodeClickedListener implements PropertyChangeListener
	{
		@Override
		public void propertyChange(PropertyChangeEvent evt) 
		{   
			// null is used to trigger cleanup
			if (evt.getNewValue() == null)
			{
			    resetFragViewerCardDeck();
			    return;
			}
			JVertex jv = (JVertex) evt.getNewValue();
			visualizeVertexInNestedViewer(jv.dnpVertex);
		}
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Makes a clone of the given vertex and loads that clone into the nested 
	 * visualization frame.
	 * @param v the original vertex
	 */
	private void visualizeVertexInNestedViewer(Vertex v)
	{
	    Vertex bb = v.clone();
        if (bb instanceof Fragment) {
            removeNestedGraphViewer(); //Just is case we still have it
            Fragment frag = (Fragment) bb;
            fragViewer.loadFragmentToViewer(frag);
            bringCardToTopOfVertexViewer(FRAGVIEWERCARDNAME);
        } else if (bb instanceof Template) {
            Template t = (Template) bb;

            // We lease data in the viewer to increase speed of execution, but the
            // leftover is outdated! This is the meaning of 'true'
            fragViewer.clearAll(true);
            fragViewerTmplViewerCard = new GraphVertexMolViewerPanel();
            
            //NB: this setting of the size is needed to allow generation
            // of the graph layout.
            Dimension d = new Dimension();
            d.height = (int) fragViewerCardHolder.getSize().height;
            d.width = (int) (fragViewerCardHolder.getSize().width*0.5);
            fragViewerTmplViewerCard.graphViewer.setSize(d);
            
            fragViewerCardHolder.add(fragViewerTmplViewerCard, 
                    TMPLVIEWERCARDNAME);
            fragViewerTmplViewerCard.setSize(
                    fragViewerCardHolder.getSize());

            fragViewerTmplViewerCard.loadDnGraphToViewer(t.getInnerGraph(), 
                    t.getIAtomContainer(), false);
            
            bringCardToTopOfVertexViewer(TMPLVIEWERCARDNAME);
            fragViewerTmplViewerCard.setDividerLocation(DEFDIVLOC);
        } else if (bb instanceof EmptyVertex) {
            removeNestedGraphViewer(); //Just is case we still have it
            
            //TODO
            System.out.println("WARNING: Visualization of "
                    + "EmptyVertex is not implemented yet");
            
            // We lease data in the viewer to increase speed of execution, but the
            // leftover is outdated! This is the meaning of 'true'
            fragViewer.clearAll(true);
            bringCardToTopOfVertexViewer(NOTDUABLECARDNAME);
        }
	}
	
//------------------------------------------------------------------------------
    
    /**
     * Listener for identifying the node on which the user has clicked and 
     * load the corresponding fragment into the fragment viewer pane.
     */
    private class MouseModeChoiceListener implements PropertyChangeListener
    {
        
        @Override
        public void propertyChange(PropertyChangeEvent evt) 
        { 
            switch ((Integer)evt.getNewValue())
            {
                case 0:
                    setMouseMode(ModalGraphMouse.Mode.PICKING);
                    break;
                    
                case 1:
                    setMouseMode(ModalGraphMouse.Mode.TRANSFORMING);
                    break;
                
                default:
                    System.out.println("WARNING: invalid mouse mode request!");
            }
        }
    }
	
//-----------------------------------------------------------------------------
	
	/**
	 * Moved the divider to the location configured by some content-based
	 * reasoning.
	 */
	public void moveDividerLocation()
	{
	    setDividerLocation(defDivLoc);
	}
	
//-----------------------------------------------------------------------------

	/**
	 * @return true is there is any selected node in the graph viewer
	 */
	public boolean hasSelectedNodes()
	{
	    return graphViewer.hasSelected();
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Adds/Removes labels to the graph components that are presently selected
	 * @param labelName the string identifying which label to add.
	 * @param show use <code>true</code> to display labels, or <code>false</code>
     * to hide labels of the given kind.
	 */
	public void alterLabels(LabelType labelName, boolean show)
	{
	    graphViewer.alterLabels(labelName, show);
	}
    
//-----------------------------------------------------------------------------
    
    /**
     * Identifies which attachment points are selected in the graph viewer.
     * @return the list selected attachment points.
     */
    
    public ArrayList<AttachmentPoint> getAPsSelectedInViewer()
    {
        ArrayList<AttachmentPoint> aps = 
                new ArrayList<AttachmentPoint>();
        for (JVertex jv : graphViewer.getSelectedNodes())
        {
            if (jv.vtype == JVertexType.AP)
            {   
                aps.add(jv.ap);
            }
        }
        return aps;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Identifies which vertices are selected in the graph viewer.
     * @return the list of identifiers
     */
    public ArrayList<Vertex> getSelectedNodesInViewer()
    {
        ArrayList<Vertex> selected = new ArrayList<Vertex>();
        for (JVertex jv : graphViewer.getSelectedNodes())
        {
            if (jv.vtype == JVertexType.AP)
            {
                continue;
            }
            selected.add(jv.dnpVertex);
        }
        return selected; 
    }
	
//-----------------------------------------------------------------------------

    /**
     * Allows to show the given card in the vertex viewer panel.
     * @param cardName the name of the card to display. The possible values
     * are defined as static, final fields: 
     * {@link GraphVertexMolViewerPanel.NOFSCARDNAME}, 
     * {@link GraphVertexMolViewerPanel.EMPTYCARDNAME}, 
     * {@link GraphVertexMolViewerPanel.NOTDUABLECARDNAME},
     * {@link GraphVertexMolViewerPanel.FRAGVIEWERCARDNAME}
     */
    public void bringCardToTopOfVertexViewer(String cardName)
    {
        ((CardLayout) fragViewerCardHolder.getLayout()).show(
                fragViewerCardHolder, cardName);
    }
    
//-----------------------------------------------------------------------------

    /**
     * Allows to show the given card in the molecular structure viewer.
     * @param cardName the name of the card to display. The possible values
     * are defined as static, final fields: 
     * {@link GraphVertexMolViewerPanel.UPDATETOVIEW}, 
     * {@link GraphVertexMolViewerPanel.MOLVIEWERCARDNAME}, 
     * {@link GraphVertexMolViewerPanel.TMPLVIEWERCARDNAME}
     */
    public void bringCardToTopOfMolViewer(String cardName)
    {
        ((CardLayout) molViewerCardHolder.getLayout()).show(
                molViewerCardHolder, cardName);
    }
    
//-----------------------------------------------------------------------------

	/*
	 * This is needed to stop GraphStream and Jmol threads upon closure of this
	 * gui card.
	 */
	public void dispose() 
	{
		graphViewer.dispose();
		fragViewer.dispose();
		molViewer.dispose();
		if (fragViewerTmplViewerCard != null)
		{
		    fragViewerTmplViewerCard.dispose();
		}
	}

//-----------------------------------------------------------------------------

	/**
	 * Alters the functionality of mouse in the graph visualization panel.
	 * @param mode the mode of action of the mouse.
	 */
    public void setMouseMode(ModalGraphMouse.Mode mode)
    {
        graphViewer.setMouseMode(mode);
    }
		
//-----------------------------------------------------------------------------
  	
}
