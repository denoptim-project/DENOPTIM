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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JPanel;

import org.openscience.cdk.AtomRef;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IStereoElement;
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

import denoptim.exception.DENOPTIMException;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.Vertex;
import denoptim.utils.MoleculeUtils;


/**
 * Panel dealing with the painting of 2D chemical representation.
 * 
 * @author Marco Foscato
 */

public class TwoDimStructurePanel extends JPanel implements MouseWheelListener, 
MouseListener, MouseMotionListener
{
    /**
     * Version ID
     */
    private static final long serialVersionUID = 1L;
    
    /* 
     * The internal-only version of the atom container to visualize 
     */
    IAtomContainer mol;
    AtomContainerRenderer renderer;

    private double zoom = 1;
    private boolean zoomingOrPanning = false;
    AffineTransform initialTransform;
    AffineTransform at;   
    Point2D XFormedPoint;
    double referenceX;
    double referenceY;
    double translateX = 0;
    double translateY = 0;

//-----------------------------------------------------------------------------

    /**
     * Constructor for a viewer that ignores H atoms.
     * @param extendenMol the atom container, possibly extended to include 
     * {@link AttachmentPoint}s as pseudoatoms.
     */

    public TwoDimStructurePanel(IAtomContainer extendedMol) 
    {
        this(extendedMol, false);
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor for a viewer that may ignore H atoms is requested.
     * @param extendenMol the atom container, possibly extended to include 
     * {@link AttachmentPoint}s as pseudoatoms.
     */

    public TwoDimStructurePanel(IAtomContainer extendedMol, boolean showHydrogens) 
    {
        IAtomContainer inputMol;
        try {
            inputMol = MoleculeUtils.makeSameAs(extendedMol);
        } catch (DENOPTIMException e) {
            e.printStackTrace();
            renderer = null;
            return;
        }
        if (!showHydrogens)
        {
            List<IAtom> atomsToRemove = new ArrayList<IAtom>();
            for (IAtom atom : inputMol.atoms())
            {
                if (MoleculeUtils.getSymbolOrLabel(atom).equals("H"))
                {
                    atomsToRemove.add(atom);
                }
            }
            for (IAtom atom : atomsToRemove)
            {
                inputMol.removeAtom(atom);
            }
        }
        Logger logger = Logger.getLogger(GUI.GUILOGGER);
        StructureDiagramGenerator sdg = new StructureDiagramGenerator();
        try
        {
            sdg.setMolecule(inputMol);
            sdg.generateCoordinates();
        } catch (Exception e)
        {
            logger.warning("Inconsistent stereochemistry in the molecule. " 
                + "Ignoring stereochemistry.");
                mol.setStereoElements(new ArrayList<IStereoElement>());
            try {
                sdg.setMolecule(mol);
                sdg.generateCoordinates();
            } catch (Exception e2) {
                e2.printStackTrace();
                renderer = null;
                return;
            }
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
        addMouseListener(this);
        addMouseMotionListener(this);
    }

//-----------------------------------------------------------------------------

    /**
     * Get the IAtomContainer from the vertex, so that all {@link AttachmentPoint}s
     * are represented as pseudoatoms.
     * @param vertex the vertex to get the IAtomContainer from
     * @return the IAtomContainer or null if an error occurs.
     */
    public static IAtomContainer getIACFromVertex(Vertex vertex)
    {
        IAtomContainer extendedMol = null;
        try
        {
            extendedMol = MoleculeUtils.makeSameAs(vertex.getIAtomContainer());
            int apId = 0;
            int numAtoms = extendedMol.getAtomCount();
            for (AttachmentPoint ap : vertex.getAttachmentPoints())
            {
                extendedMol.addAtom(new PseudoAtom(""+(apId+1)));
                extendedMol.addBond(ap.getAtomPositionNumber(), 
                        apId+numAtoms, IBond.Order.SINGLE);
                apId++;
            }
        }  catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
        return extendedMol;
    }

//-----------------------------------------------------------------------------

    /**
     * Paint the panel
     * @param graphics the graphics object
     */

    @Override
    public void paint(Graphics graphics) 
    {
        if (renderer==null)
            return;
        
        renderer.setup(mol, new Rectangle(getWidth(), getHeight()));

        // paint the background
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, getWidth(), getHeight());
        
        Graphics2D g2d = (Graphics2D) graphics;

        AffineTransform saveTransform = g2d.getTransform();
        at = new AffineTransform(saveTransform);
        if (zoomingOrPanning)
        {
            at.translate(getWidth()/2, getHeight()/2);
            at.scale(zoom,zoom);
            at.translate(-getWidth()/2, -getHeight()/2);
            at.translate(translateX, translateY);

            g2d.setTransform(at);
        }
            
        renderer.paint(mol, new AWTDrawVisitor(g2d));
            
        if (zoomingOrPanning)
        {
            // Is this needed? yes!
            g2d.setTransform(saveTransform);
            zoomingOrPanning = false;
        }
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
        zoomingOrPanning = true;
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        // first transform the mouse point to the pan and zoom
        // coordinates
        try {
            XFormedPoint = at.inverseTransform(e.getPoint(), null);
        }
        catch (Exception te) {
            te.printStackTrace();
        }

        // save the transformed starting point and the initial
        // transform
        referenceX = XFormedPoint.getX();
        referenceY = XFormedPoint.getY();
        initialTransform = at;
    }
    
    @Override
    public void mouseDragged(MouseEvent e)
    {
        try {
            XFormedPoint = initialTransform.inverseTransform(e.getPoint(), 
                    null);
        }
        catch (Exception te) {
            te.printStackTrace();
        }
        double deltaX = XFormedPoint.getX() - referenceX;
        double deltaY = XFormedPoint.getY() - referenceY; 
        referenceX = XFormedPoint.getX();
        referenceY = XFormedPoint.getY();
        
        translateX += deltaX;
        translateY += deltaY;
 
        // schedule a repaint.
        zoomingOrPanning = true;
        repaint();
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
    @Override
    public void mouseMoved(MouseEvent e) {}
    
//-----------------------------------------------------------------------------
    
    /**
     * Generator that chooses default colors from CDK2DAtomColor for all but 
     * {@link PseudoAtom}s which we expect to be placeholders for
     * {@link AttachmentPoint}s. Such atoms are colored magenta.
     */
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

}
