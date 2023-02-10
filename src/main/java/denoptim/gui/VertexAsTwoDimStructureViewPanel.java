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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import org.openscience.cdk.AtomRef;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.color.IAtomColorer;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicAtomGenerator;
import org.openscience.cdk.renderer.generators.BasicBondGenerator;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;

import denoptim.graph.AttachmentPoint;
import denoptim.graph.EmptyVertex;
import denoptim.graph.Fragment;
import denoptim.graph.Vertex;
import denoptim.utils.MoleculeUtils;


/**
 * A panel to visualize a vertex as two-dimensional chemical structure
 * with attachment point table.
 * 
 * @author Marco Foscato
 */

public class VertexAsTwoDimStructureViewPanel extends JSplitPane 
    implements IVertexAPSelection
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The currently loaded fragment
	 */
	private Vertex vertex;
	
	/**
	 * Temporary list of attachment points of the current fragment
	 */
	protected Map<Integer, AttachmentPoint> mapAPs = null;
	
	/**
	 * Flag signaling that data about APs has been changed in the GUI
	 */
	public boolean alteredAPData = false;
	
    private JScrollPane viewPanel;
	private JPanel twoDimView;
	private JScrollPane tabPanel;
	protected DefaultTableModel apTabModel;
	protected JTable apTable;
	
	// This is a global property of this instance
	private boolean editableAPTable = false;
	// this is vertex-specific, it does not overwrite editableAPTable, which is
	// more general. The overall editablility is given by general && local.
	private boolean vertexSpecificAPTabEditable = true;
	
	
//-----------------------------------------------------------------------------
	
    /**
     * Panel dealing with the painting of 2D chemical representation.
     */
    class TwoDimStructurePanel extends JPanel implements MouseWheelListener
    {
        IAtomContainer mol;
        AtomContainerRenderer renderer;

        private double zoom = 1;
        private boolean zooming = false;

        public TwoDimStructurePanel(Vertex vertex) 
        {
            StructureDiagramGenerator sdg = new StructureDiagramGenerator();
            try
            {
                IAtomContainer extendedMol = MoleculeUtils.makeSameAs(
                        vertex.getIAtomContainer());
                int apId = 0;
                int numAtoms = extendedMol.getAtomCount();
                for (AttachmentPoint ap : vertex.getAttachmentPoints())
                {
                    extendedMol.addAtom(new PseudoAtom(""+(apId+1)));
                    extendedMol.addBond(ap.getAtomPositionNumber(), 
                            apId+numAtoms, IBond.Order.SINGLE);
                    apId++;
                }
                
                sdg.setMolecule(extendedMol);
                sdg.generateCoordinates();
            } catch (Exception e)
            {
                e.printStackTrace();
                renderer = null;
                return;
            }
            this.mol = sdg.getMolecule();

            // These generate the graphical components
            List<IGenerator<IAtomContainer>> generators = 
                  new ArrayList<IGenerator<IAtomContainer>>();
            generators.add(new BasicSceneGenerator());
            generators.add(new BasicBondGenerator());
            generators.add(new AtomOrAPGenerator());
            renderer = new AtomContainerRenderer(generators, new AWTFontManager());
            
            addMouseWheelListener(this);
        }

        public void paint(Graphics graphics) 
        {
            if (renderer==null)
                return;
            
            renderer.setup(mol, new Rectangle(getWidth(), getHeight()));

            // paint the background
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, getWidth(), getHeight());
            
            Graphics2D g2d = (Graphics2D) graphics;
            
            if (zooming)
            {    
                double width = getWidth();
                double height = getHeight();
               
    
                double zoomWidth = width * zoom;
                double zoomHeight = height * zoom;
    
                double offSetX = (zoomWidth-width);
                double offSetY = (zoomHeight-height);
    
                AffineTransform at = new AffineTransform();
                at.translate(-offSetX, -offSetY);
                at.scale(g2d.getTransform().getScaleX()*zoom, 
                        g2d.getTransform().getScaleY()*zoom);
                g2d.setTransform(at);
                
                zooming = false;
            }
            renderer.paint(mol, new AWTDrawVisitor(g2d));
        }
        
        @Override
        public void mouseWheelMoved(MouseWheelEvent e)
        {   
            if (e.getWheelRotation() < 0) {
                zoom *= 1.1;
            }
            if (e.getWheelRotation() > 0) {
                zoom /= 1.1;
            }
            zooming = true;
            repaint();
        }

    }
    
//-----------------------------------------------------------------------------
    
    class AtomOrAPGenerator extends BasicAtomGenerator
    {
        /**
         * Chooses default colors from CDK2DAtomColor for all but 
         * {@link PseudoAtom}s which we expect to be placeholder for
         * {@link AttachmentPoint}s. Such atoms are colored .
         */
        @Override
        protected Color getAtomColor(IAtom atom, RendererModel model) 
        {
            Color atomColor = model.get(AtomColor.class);
            if ((Boolean) model.get(ColorByType.class)) 
            {
                boolean changeColor = false;
                IAtom a = null;
                if (atom instanceof AtomRef) 
                {
                    a = ((AtomRef) atom).deref();
                    if (a instanceof PseudoAtom)
                        changeColor = true;
                } else if (atom instanceof PseudoAtom)
                {
                    changeColor = true;
                }
                if (changeColor)
                {
                    atomColor = new Color(0xFF00FF);
                } else {
                    atomColor = ((IAtomColorer) model.get(AtomColorer.class))
                            .getAtomColor(atom);
                }
            }
            return atomColor;
        }
    }
	
//-----------------------------------------------------------------------------

	/**
	 * Constructor that allows to specify whether the AP table is editable or 
	 * not.
	 * @param editableTable use <code>true</code> to make the AP table editable
	 */
	public VertexAsTwoDimStructureViewPanel(boolean editableTable)
	{
		this(editableTable, 340);
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Constructor that allows to specify whether the AP table is editable or 
	 * not.
	 * @param parent the parent component.
	 * @param editableTable use <code>true</code> to make the AP table editable
	 * @param dividerPosition allows to set the initial position of the divide
	 */
	public VertexAsTwoDimStructureViewPanel(boolean editableTable, 
	        int dividerPosition)
	{
		editableAPTable = editableTable;
		initialize(dividerPosition);
	}
	
//-----------------------------------------------------------------------------

	@SuppressWarnings("serial")
	private void initialize(int dividerPosition)
	{	
		this.setOrientation(JSplitPane.VERTICAL_SPLIT);
		this.setOneTouchExpandable(true);
		this.setDividerLocation(dividerPosition);
		this.setResizeWeight(0.5);
        
        // 2D mol Viewer
		clearPanel();
        
		// List of attachment points
		apTabModel = new DefaultTableModel() {
			@Override
		    public boolean isCellEditable(int row, int column) {
				if (column == 0)
				{
					return false;
				}
				else
			    {
					return editableAPTable && vertexSpecificAPTabEditable;
			    }
		    }
		};
		apTabModel.setColumnCount(2);
		String column_names[]= {"<html><b>AP#</b></html>", 
		        "<html><b>APClass</b></html>"};
		apTabModel.setColumnIdentifiers(column_names);
		apTable = new JTable(apTabModel);
		apTable.putClientProperty("terminateEditOnFocusLost", true);
		apTable.getColumnModel().getColumn(0).setMaxWidth(75);
		apTable.setGridColor(Color.LIGHT_GRAY);
        apTable.setPreferredScrollableViewportSize(apTable.getPreferredSize());
		JTableHeader apTabHeader = apTable.getTableHeader();
		apTabHeader.setPreferredSize(new Dimension(100, 20));
		apTabModel.addTableModelListener(new PausableTableModelListener());
		tabPanel = new JScrollPane(apTable);
		this.setBottomComponent(tabPanel);
	}
	
//-----------------------------------------------------------------------------
	
	public void setVertexSpecificEditableAPTable(boolean editable)
	{
	    vertexSpecificAPTabEditable = editable;
	}
	
//-----------------------------------------------------------------------------
    
    public void loadVertexToViewer(Vertex v)
    {
        setVertexSpecificEditableAPTable(true);
        clearAPTable();
        vertex = v;
        loadVertexStructure();
        updateAPsMapAndTable();
        preSelectAPs();
    }

//-----------------------------------------------------------------------------

    /*
     * Structure here means the single-node-graph-like visual depiction of the
     */
    private void loadVertexStructure()
    {
        if ((vertex instanceof EmptyVertex) 
                || (vertex instanceof Fragment))
        {
            clearPanel();
            twoDimView.add(new TwoDimStructurePanel(vertex));
        }
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * trashed the current panel displaying any 2D structure and replaces it 
     * with a new empty panel.
     */
    private void clearPanel()
    {
        twoDimView = new JPanel(new BorderLayout());
        twoDimView.add(new JLabel("Not available"));
        viewPanel = new JScrollPane(twoDimView);
        this.setTopComponent(viewPanel);
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
	
	/**
	 * Uses the AP of the {@link Fragment} to create a new map and 
	 * table of APs.
	 */
	private void updateAPsMapAndTable()
	{
		clearAPTable();
		mapAPs = new HashMap<Integer,AttachmentPoint>();
		
		ArrayList<AttachmentPoint> lstAPs = 
		        vertex.getAttachmentPoints();		
        if (lstAPs.size() == 0)
        {
			return;
        }
        
        activateTabEditsListener(false);
        int arrId = 0;
	    for (AttachmentPoint ap : lstAPs)
	    {
	    	arrId++;
	    	apTabModel.addRow(new Object[]{arrId, ap.getAPClass()});
	    	mapAPs.put(arrId,ap);
	    }
	    activateTabEditsListener(true);
	}
	
//-----------------------------------------------------------------------------
	
	private void preSelectAPs()
	{
		String PRESELPROP = GUIVertexSelector.PRESELECTEDAPSFIELD;
		String PRESELPROPSEP = GUIVertexSelector.PRESELECTEDAPSFIELDSEP;
		
		if (vertex.getProperty(PRESELPROP) == null)
		{
			return;
		}
		
		String prop = vertex.getProperty(PRESELPROP).toString();
		String[] parts =prop.split(PRESELPROPSEP);
		
		activateTabEditsListener(false);
		for (int i=0; i<parts.length; i++)
		{
			int apId = Integer.parseInt(parts[i]); //0-based
			apTable.getSelectionModel().addSelectionInterval(apId, apId);
		}
		activateTabEditsListener(true);
	}

//-----------------------------------------------------------------------------

	/**
	 * Removes the currently visualized molecule and AP table
	 */
	public void clearAll()
	{
		clearAPTable();
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Clears the table of attachment points
	 */
	public void clearAPTable()
	{
		activateTabEditsListener(false);
		int initRowCount = apTabModel.getRowCount();
        for (int i=0; i<initRowCount; i++) 
        {
        	//Always remove the first to avoid dealing with changing row ids
        	apTabModel.removeRow(0);
        }
        activateTabEditsListener(true);
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Identifies which attachment points are selected in the visualized table
	 * @return the list of attachment points
	 */
	public ArrayList<AttachmentPoint> getSelectedAPs()
	{
		ArrayList<AttachmentPoint> selected = 
				new ArrayList<AttachmentPoint>();
		
		for (int rowId : apTable.getSelectedRows())
		{
			selected.add(mapAPs.get(apTable.getValueAt(rowId, 0)));
		}
		return selected;
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Identifies which attachment points are selected in the visualized table
	 * @return the list of attachment points indexes
	 */
	public ArrayList<Integer> getSelectedAPIDs()
	{
		ArrayList<Integer> selected = new ArrayList<Integer>();
		for (int rowId : apTable.getSelectedRows())
		{
			selected.add(rowId);
		}
		return selected;
	}
 	
//-----------------------------------------------------------------------------
	
	private class PausableTableModelListener implements TableModelListener
	{	
		private boolean isActive = false;
		
		public PausableTableModelListener() 
		{};

		@Override
		public void tableChanged(TableModelEvent e) 
		{
            if (isActive && !alteredAPData 
            		&& e.getType() == TableModelEvent.UPDATE)
            {
                alteredAPData = true;
                firePropertyChange(IVertexAPSelection.APDATACHANGEEVENT, false, 
                        true);
            }
		}
        
		public void setActive(boolean var)
		{
			isActive = var;
		}
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Allows to activate and deactivate the listener.
	 * @param var use <code>true</code> to activate the listener
	 */
    public void activateTabEditsListener(boolean var)
    {
		try
		{
			PausableTableModelListener l = (PausableTableModelListener) 
					apTabModel.getTableModelListeners()[0];
    	    l.setActive(var);
		} catch (Throwable t) {
			System.out.println("Bad attempt to contro listener: " 
					+ t.getMessage());
			System.out.println(t.getCause());
		}
    }

//-----------------------------------------------------------------------------
	   
    @Override
    public Map<Integer, AttachmentPoint> getMapOfAPsInTable()
    {
        return mapAPs;
    }

//-----------------------------------------------------------------------------

    @Override
    public DefaultTableModel getAPTableModel()
    {
        return apTabModel;
    }
  	
//-----------------------------------------------------------------------------

}
