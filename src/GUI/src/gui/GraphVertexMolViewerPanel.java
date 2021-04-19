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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.fragspace.IdFragmentAndAP;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMTemplate;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.DENOPTIMVertex.BBType;
import denoptim.molecule.EmptyVertex;
import denoptim.threedim.ThreeDimTreeBuilder;
import denoptim.utils.DENOPTIMMoleculeUtils;


/**
 * A panel that collects three viewers: 
 * <ul>
 * <li>one for Graphs,</il>
 * <li>one for vertex content,</il>
 * <li>and one for molecular structures.</il>
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
	private DENOPTIMGraph dnGraph;
	
	/**
	 * The currently loaded graph as GraphStream object
	 */
	private Graph graphGS = null;
	
	/**
	 * The snapshot of the old (removed) visualized GraphStrem system. 
	 * Used only to remember stuff like sprites and node positions.
	 */
	private GSGraphSnapshot oldGSStatus;
	
	/**
	 * Flag signalling that there is a fully defined fragment space
	 */
	private boolean hasFragSpace = false;
	
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
	private double defDivLoc = 0.5;
	
	private static final  IChemObjectBuilder builder = 
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
						"NODECLICKED", new NodeClickedListener()));
		
		fragViewerPanel = new JPanel(new BorderLayout());
		fragViewerHeader = new JPanel();
		fragViewerHeader.add(new JLabel("Node content:"));
		String fragViewerToolTip = "<html>This viewer shows the "
				+ "chemical structure of the selected vertex.<br>"
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
				+ "of nodes, please load a fragment space.</center></html>";
		fragViewerNoFSCard.add(new JLabel(String.format(txtb, 120)));
		fragViewerNoFSCard.setToolTipText(fragViewerToolTip);
		fragViewerCardHolder.add(fragViewerNoFSCard, NOFSCARDNAME);
				
		fragViewer = new FragmentViewPanel(false);
		fragViewerCardHolder.add(fragViewer, FRAGVIEWERCARDNAME);
		
		((CardLayout) fragViewerCardHolder.getLayout()).show(
				fragViewerCardHolder, NOFSCARDNAME);
		
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
     * @param useFragSpace give <code>true</code> when a fragment space is 
     * loaded.
     */
    public void loadDnGraphToViewer(DENOPTIMGraph dnGraph, IAtomContainer mol, 
            boolean keepSprites, boolean useFragSpace)
    {
        loadDnGraphToViewer(dnGraph,keepSprites,useFragSpace);
        try {
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
	 * @param keepSprites if <code>true</code> we'll keep track of old labels.
	 * @param useFragSpace give <code>true</code> when a fragment space is 
	 * loaded.
	 */
	public void loadDnGraphToViewer(DENOPTIMGraph dnGraph, boolean keepSprites, 
	        boolean useFragSpace)
	{
	    hasFragSpace = useFragSpace;
	    this.dnGraph = dnGraph;
	    resetFragViewerCardDeck();
		
		// Keep a snapshot of the old data visualised
		if (keepSprites)
		{
			oldGSStatus = graphViewer.getStatusSnapshot();
		} else {
			oldGSStatus = null;
		}
		
		graphViewer.cleanup();
		graphViewer.loadGraphToViewer(dnGraph,oldGSStatus);
		graphGS = graphViewer.graph;
	}

//-----------------------------------------------------------------------------
    
    /**
     * Triggers the generation of the molecular representation of the loaded
     * graph.
     */
	
    public void renderMolVieverToNeedUpdate()
    {
        molViewer.clearAll();
        bringCardToTopOfMolViewer(UPDATETOVIEW);
    }
	
//-----------------------------------------------------------------------------
	
	/**
	 * Updates the molecular representation of the loaded
	 * graph.
	 */
	public IAtomContainer updateMolevularViewer()
	{
	    molViewer.clearAll();
	    
	    //TODO-V3
	    if (false) //if 3d is available from library
	    {
	        try {
	            /*
                molViewer.loadChemicalStructure( // get mol from library );
                */      
	            bringCardToTopOfMolViewer(MOLVIEWERCARDNAME);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Could not read molecular data: "+
                        e.getCause() + " " + e.getMessage());
                bringCardToTopOfMolViewer(EMPTYCARDNAME);
            }
	    } else {
            IAtomContainer mol = builder.newAtomContainer();
            if (hasFragSpace)
            {
                ThreeDimTreeBuilder tb = new ThreeDimTreeBuilder();
                try {
                    mol = tb.convertGraphTo3DAtomContainer(
                            dnGraph);
                    DENOPTIMMoleculeUtils.removeUsedRCA(mol,dnGraph);
                } catch (Throwable t) {
                    t.printStackTrace();
                    System.out.println("Couldn't make 3D-tree representation: "
                            + t.getMessage());
                    //molLibrary.set(currGrphIdx, builder.newAtomContainer());
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
            }
            return mol;
	    }
	    return null;
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
            fragViewer.clearAll();
            if (hasFragSpace)
            {
                bringCardToTopOfVertexViewer(EMPTYCARDNAME);
            } else {
                bringCardToTopOfVertexViewer(NOFSCARDNAME);
            }
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
        fragViewer.clearAll();
        bringCardToTopOfVertexViewer(EMPTYCARDNAME);
    }
	
//-----------------------------------------------------------------------------
    
    /**
     * Clears the molecular viewer and hides it behind the empty card.
     */
    public void clearMolecularViewer()
    {
        molViewer.clearAll();
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
			}
            
			// Otherwise try to load the clicked-on vertex into the viewer
			String nodeId = (String) evt.getNewValue();
			Node n = graphGS.getNode(nodeId);
			if (n == null || !hasFragSpace)
			{
				return;
			}
			
			DENOPTIMVertex v;
			try {
				v = dnGraph.getVertexWithId(
						Integer.parseInt(nodeId));
			} catch (NumberFormatException e1) {
			    //When we click on an AP node we get a nodeId that is like v1ap0
			    // and this triggers this exception, which we can ignore.
			    return;
			}

		    DENOPTIMVertex bb = v.clone();
			if (bb instanceof DENOPTIMFragment)
			{
			    removeNestedGraphViewer(); //Just is case we still have it
			    DENOPTIMFragment frag = (DENOPTIMFragment) bb;
			    fragViewer.loadFragmentToViewer(frag);
	            bringCardToTopOfVertexViewer(FRAGVIEWERCARDNAME);
			} else if (bb instanceof DENOPTIMTemplate) {
			    DENOPTIMTemplate t = (DENOPTIMTemplate) bb;
                fragViewer.clearAll();
                fragViewerTmplViewerCard = 
                        new GraphVertexMolViewerPanel();
                fragViewerCardHolder.add(fragViewerTmplViewerCard, 
                        TMPLVIEWERCARDNAME);
                fragViewerTmplViewerCard.loadDnGraphToViewer(
                        t.getInnerGraph(), false, hasFragSpace);
                fragViewerTmplViewerCard.updateMolevularViewer();
			    bringCardToTopOfVertexViewer(TMPLVIEWERCARDNAME);
			    fragViewerTmplViewerCard.setDividerLocation(defDivLoc);
			} else if (bb instanceof EmptyVertex) {
			    removeNestedGraphViewer(); //Just is case we still have it
                
                //TODO
                System.out.println("WARNING: Visualization of "
                        + "EmptyVertex is not implemented yet");
                
                fragViewer.clearAll();
                bringCardToTopOfVertexViewer(NOTDUABLECARDNAME);
            }
		}
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Moved the divider to the default location.
	 */
	public void setDefaultDividerLocation()
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
	 * Adds labels to the graph components that are presently selected
	 * @param labelName the string identifying which label to add
	 */
	public void addLabelsToGraph(String labelName)
	{
	    graphViewer.appendSprites(labelName);
	}
	
//-----------------------------------------------------------------------------

	/**
     * Removes labels to the graph  components that are presently selected
     * @param labelName the string identifying which label to add
     */
    public void removeLabelsToGraph(String labelName)
    {
        graphViewer.removeSprites(labelName);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Identifies which attachment points are selected in the graph viewer.
     * @return the list selected attachment points.
     */
    
    public ArrayList<DENOPTIMAttachmentPoint> getAPsSelectedInViewer()
    {
        ArrayList<DENOPTIMAttachmentPoint> aps = 
                new ArrayList<DENOPTIMAttachmentPoint>();
        for (Node n : graphViewer.getSelectedNodes())
        {
            if (!n.getAttribute("ui.class").equals("ap"))
            {
                continue;
            }
            int vId = n.getAttribute("dnp.srcVrtId");
            int apId = n.getAttribute("dnp.srcVrtApId");
            
            DENOPTIMVertex v = dnGraph.getVertexWithId(vId);
            DENOPTIMAttachmentPoint ap = v.getAP(apId);
            
            aps.add(ap);
        }
        return aps;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Identifies which APs are selected in the graph viewer.
     * @return the list of identifiers
     */
    
    //TODO-V3: with APs having an owner field, the IdFragmentAndAP is obsolete
    /*
    public ArrayList<IdFragmentAndAP> getAPsSelectedInViewer()
    {
        ArrayList<IdFragmentAndAP> allIDs = new ArrayList<IdFragmentAndAP>();
        for (Node n : graphViewer.getSelectedNodes())
        {
            if (!n.getAttribute("ui.class").equals("ap"))
            {
                continue;
            }
            int vId = n.getAttribute("dnp.srcVrtId");
            int apId = n.getAttribute("dnp.srcVrtApId");
            
            IdFragmentAndAP id = null;
            if (hasFragSpace)
            {
                DENOPTIMVertex v = dnGraph.getVertexWithId(vId);
                id = new IdFragmentAndAP(vId,
                                         v.getMolId(),
                                         v.getFragmentType(),
                                         apId,-99,-99);
            }
            else
            {
                id = new IdFragmentAndAP(vId,-99,BBType.UNDEFINED,apId,-99,-99);
            }
            
            allIDs.add(id);
        }
        return allIDs;
    }
    */
//-----------------------------------------------------------------------------
    
    /**
     * Identifies which vertices are selected in the graph viewer.
     * @return the list of identifiers
     */
    public ArrayList<DENOPTIMVertex> getSelectedNodesInViewer()
    {
        ArrayList<DENOPTIMVertex> selected = new ArrayList<DENOPTIMVertex>();
        for (Node n : graphViewer.getSelectedNodes())
        {
            if (n.getAttribute("ui.class").equals("ap"))
            {
                continue;
            }
            int vId = Integer.parseInt(n.getAttribute("dnp.VrtId"));
            selected.add(dnGraph.getVertexWithId(vId));
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
  	
}
