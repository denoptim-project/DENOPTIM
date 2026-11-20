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
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.vecmath.Point2d;

import org.openscience.cdk.AtomRef;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IStereoElement;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.color.CDK2DAtomColors;
import org.openscience.cdk.renderer.color.IAtomColorer;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import denoptim.exception.DENOPTIMException;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.Vertex;
import denoptim.utils.MoleculeUtils;


/**
 * Panel dealing with the painting of 2D chemical representation.
 * A panel built as selectable reports the depicted atom under a mouse click
 * and keeps that atom in the selection.
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

    /**
     * Property on a depicted atom whose value is the {@link IAtom} drawn for
     * the same nucleus in the 3D viewer. The buttons that read the selection
     * use that atom, not the copy drawn here.
     */
    public static final String ATOM_IN_3D_VIEW = "denoptim.atomIn3DView";
    
    /* 
     * The internal-only version of the atom container to visualize 
     */
    IAtomContainer mol;
    AtomContainerRenderer renderer;

    private double zoom = 1;
    AffineTransform initialTransform;
    AffineTransform at;   
    Point2D XFormedPoint;
    double referenceX;
    double referenceY;
    double translateX = 0;
    double translateY = 0;

    /**
     * When {@code true}, a click selects the depicted atom under the pointer.
     */
    private final boolean selectable;

    /**
     * Atoms of {@link #mol} chosen by clicks, in click order.
     */
    private final Set<IAtom> selectedAtoms = new LinkedHashSet<IAtom>();

    private final List<AtomClickListener> atomClickListeners =
            new ArrayList<AtomClickListener>();

    /**
     * How close a click must be to an atom centre, in screen pixels.
     */
    private static final double ATOM_PICK_RADIUS_PX = 20;

    /**
     * Movement below this, in pixels, is still a click. Past it, the gesture
     * pans the depiction.
     */
    private static final double CLICK_SLOP_PX = 4;

    /**
     * Space kept around the atom bounding box, in pixels, so element symbols
     * stay inside the panel when the depiction is fitted.
     */
    private static final int FIT_SYMBOL_PAD_PX = 18;

    /**
     * Smallest scale applied when zooming out. The depiction is not shrunk
     * further than this, even when the structure still overflows the panel.
     */
    private static final double MIN_FIT_ZOOM = 0.20;

    /**
     * When {@code true}, a paint at a new panel size fits the structure again.
     * Cleared once the user zooms or pans, so that adjustment is kept.
     */
    private boolean keepFittedZoom;

    /**
     * Panel size, in pixels, for which {@link #zoom} was last fitted.
     * {@code -1} until the structure has been fitted during a paint.
     */
    private int fittedWidth = -1;
    private int fittedHeight = -1;

    /**
     * Where the current gesture started, in this panel's coordinates.
     */
    private Point pressPoint;

    /**
     * {@code true} once the current gesture has moved far enough to pan.
     */
    private boolean panning;

    /**
     * Glow drawn around a selected atom. Visible on carbons, which have no
     * element symbol.
     */
    private static final Color SELECTION_COLOR = new Color(0x1E90FF);

//-----------------------------------------------------------------------------

    /**
     * Called when a click selects or deselects a depicted atom.
     */
    @FunctionalInterface
    public interface AtomClickListener
    {
        /**
         * @param atom the atom under the pointer. It belongs to the container
         * currently drawn by this panel.
         * @param index index of {@code atom} in that container.
         * @param selected {@code true} when the click added the atom to the
         * selection.
         */
        void atomClicked(IAtom atom, int index, boolean selected);
    }

//-----------------------------------------------------------------------------

    /**
     * Empty viewer. Clicks do not select atoms. Call
     * {@link #loadStructure(IAtomContainer, boolean)} to depict a structure
     * without replacing this panel.
     */
    public TwoDimStructurePanel()
    {
        this(null, false, false);
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor for a viewer that may collapse hydrogens and, when asked,
     * selects the atom under a click.
     * @param mol the atom container, possibly extended to include
     * {@link AttachmentPoint}s as pseudoatoms.
     * @param showHydrogens when {@code false}, explicit hydrogens that
     * can be stored as a count are removed and drawn on the parent symbol.
     * @param selectable when {@code true}, a click selects the depicted atom
     * nearest the pointer.
     */
    public TwoDimStructurePanel(IAtomContainer mol, boolean showHydrogens,
            boolean selectable)
    {
        this.selectable = selectable;
        installRenderer();
        if (selectable)
        {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            renderer.getRenderer2DModel().set(StandardGenerator.Highlighting.class,
                    StandardGenerator.HighlightStyle.OuterGlow);
        }
        if (mol!=null)
        {
            loadStructure(mol, showHydrogens);
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Replaces the depicted structure. Pan is reset, and the view is zoomed
     * out until the whole structure fits in the panel, but not below a scale
     * of 0.20. A structure that already fits is left at the normal zoom.
     * @param extendedMol the atom container, possibly extended to include
     * {@link AttachmentPoint}s as pseudoatoms.
     * @param showHydrogens when <code>false</code>, explicit hydrogens that
     * can be stored as a count are removed and drawn on the parent symbol.
     */
    public void loadStructure(IAtomContainer extendedMol, boolean showHydrogens)
    {
        resetView();
        clearAtomSelection();
        mol = layoutStructure(extendedMol, showHydrogens);
        // The size reported before the first paint is not the size the panel
        // is drawn at, so the fit is applied from paint once that size is known.
        keepFittedZoom = mol != null;
        fittedWidth = -1;
        fittedHeight = -1;
        repaint();
    }

//-----------------------------------------------------------------------------

    /**
     * Removes the depicted structure and restores the default view.
     */
    public void clearStructure()
    {
        resetView();
        clearAtomSelection();
        mol = null;
        keepFittedZoom = false;
        fittedWidth = -1;
        fittedHeight = -1;
        repaint();
    }

//-----------------------------------------------------------------------------

    /**
     * @return {@code true} when this panel was built to select atoms.
     */
    public boolean isSelectable()
    {
        return selectable;
    }

//-----------------------------------------------------------------------------

    /**
     * @return the depicted atoms currently selected, in the order they were
     * selected. The list is a copy.
     */
    public List<IAtom> getSelectedAtoms()
    {
        return new ArrayList<IAtom>(selectedAtoms);
    }

//-----------------------------------------------------------------------------

    /**
     * Drops every selected atom.
     */
    public void clearAtomSelection()
    {
        for (IAtom atom : selectedAtoms)
        {
            atom.removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
        }
        selectedAtoms.clear();
        repaint();
    }

//-----------------------------------------------------------------------------

    /**
     * @param listener notified after a click changes the selection. Ignored
     * when this panel is not selectable.
     */
    public void addAtomClickListener(AtomClickListener listener)
    {
        if (listener != null && selectable)
        {
            atomClickListeners.add(listener);
        }
    }

//-----------------------------------------------------------------------------

    /**
     * @param listener a listener previously passed to
     * {@link #addAtomClickListener(AtomClickListener)}.
     */
    public void removeAtomClickListener(AtomClickListener listener)
    {
        atomClickListeners.remove(listener);
    }

//-----------------------------------------------------------------------------

    /**
     * Builds the renderer once. Replacing the molecule must not replace the
     * panel, so the renderer and mouse listeners stay installed.
     */
    private void installRenderer()
    {
        // These generate the graphical components. StandardGenerator draws
        // implicit hydrogen counts as part of the atom symbol.
        List<IGenerator<IAtomContainer>> generators =
              new ArrayList<IGenerator<IAtomContainer>>();
        generators.add(new BasicSceneGenerator());
        generators.add(new StandardGenerator(
                new Font(Font.SANS_SERIF, Font.PLAIN, 18)));
        renderer = new AtomContainerRenderer(generators, new AWTFontManager());
        renderer.getRenderer2DModel().set(StandardGenerator.AtomColor.class,
                new AttachmentPointAtomColorer());

        addMouseWheelListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
    }

//-----------------------------------------------------------------------------

    /**
     * Prepares a copy of the given container for depiction.
     * @return the laid-out container, or <code>null</code> when it cannot be
     * drawn.
     */
    private IAtomContainer layoutStructure(IAtomContainer extendedMol,
            boolean showHydrogens)
    {
        IAtomContainer inputMol;
        try {
            inputMol = MoleculeUtils.makeSameAs(extendedMol);
        } catch (DENOPTIMException e) {
            e.printStackTrace();
            return null;
        }
        copyAtomsIn3DView(extendedMol, inputMol);
        if (!showHydrogens)
        {
            // Counts must come only from the explicit atoms being removed.
            MoleculeUtils.setZeroImplicitHydrogensToAllAtoms(inputMol);
            AtomContainerManipulator.suppressHydrogens(inputMol);
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
            inputMol.setStereoElements(new ArrayList<IStereoElement>());
            try {
                sdg.setMolecule(inputMol);
                sdg.generateCoordinates();
            } catch (Exception e2) {
                e2.printStackTrace();
                return null;
            }
        }
        return sdg.getMolecule();
    }

//-----------------------------------------------------------------------------

    private void resetView()
    {
        zoom = 1;
        translateX = 0;
        translateY = 0;
    }

//-----------------------------------------------------------------------------

    /**
     * Sets {@link #zoom} so the depiction, drawn at the normal bond length,
     * fits inside this panel. The scale is not reduced below
     * {@link #MIN_FIT_ZOOM}, and a structure that already fits stays at 1.
     * Does nothing when the panel has no size yet.
     */
    private void zoomOutToFit()
    {
        int width = getWidth();
        int height = getHeight();
        if (renderer == null || mol == null || width <= 0 || height <= 0)
        {
            return;
        }
        renderer.setup(mol, new Rectangle(width, height));
        Rectangle diagram = renderer.calculateDiagramBounds(mol);
        if (diagram == null || diagram.width <= 0 || diagram.height <= 0)
        {
            return;
        }
        double neededWidth = diagram.width + 2.0 * FIT_SYMBOL_PAD_PX;
        double neededHeight = diagram.height + 2.0 * FIT_SYMBOL_PAD_PX;
        double fit = Math.min(width / neededWidth, height / neededHeight);
        zoom = Math.min(1.0, Math.max(fit, MIN_FIT_ZOOM));
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
            stampAtomsIn3DView(extendedMol, vertex.getIAtomContainer());
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
     * Records, on each atom of {@code copy}, the corresponding atom in the
     * 3D viewer. Attachment-point pseudoatoms appended after the molecular
     * atoms are left without one. Indices match because {@code copy} was
     * built by copying {@code atomsIn3DView} in order.
     */
    public static void stampAtomsIn3DView(IAtomContainer copy,
            IAtomContainer atomsIn3DView)
    {
        if (copy == null || atomsIn3DView == null)
        {
            return;
        }
        int count = Math.min(copy.getAtomCount(), atomsIn3DView.getAtomCount());
        for (int i = 0; i < count; i++)
        {
            copy.getAtom(i).setProperty(ATOM_IN_3D_VIEW,
                    atomsIn3DView.getAtom(i));
        }
    }

//-----------------------------------------------------------------------------

    private static void copyAtomsIn3DView(IAtomContainer from, IAtomContainer to)
    {
        int count = Math.min(from.getAtomCount(), to.getAtomCount());
        for (int i = 0; i < count; i++)
        {
            Object atomIn3DView = from.getAtom(i).getProperty(ATOM_IN_3D_VIEW);
            if (atomIn3DView != null)
            {
                to.getAtom(i).setProperty(ATOM_IN_3D_VIEW, atomIn3DView);
            }
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Shows the depicted atoms whose {@link #ATOM_IN_3D_VIEW} is in
     * {@code atomsIn3DView}. Depicted atoms that have no atom in the 3D
     * view, such as attachment-point markers, keep whatever selection they
     * already have. Does not notify click listeners.
     */
    public void setSelectionFromAtomsIn3DView(Collection<IAtom> atomsIn3DView)
    {
        if (mol == null)
        {
            return;
        }
        Set<IAtom> wanted = new HashSet<IAtom>();
        if (atomsIn3DView != null)
        {
            wanted.addAll(atomsIn3DView);
        }
        List<IAtom> next = new ArrayList<IAtom>();
        for (IAtom atom : mol.atoms())
        {
            Object atomIn3DView = atom.getProperty(ATOM_IN_3D_VIEW);
            if (atomIn3DView instanceof IAtom)
            {
                if (wanted.contains(atomIn3DView))
                {
                    next.add(atom);
                }
            }
            else if (selectedAtoms.contains(atom))
            {
                next.add(atom);
            }
        }
        for (IAtom atom : selectedAtoms)
        {
            atom.removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
        }
        selectedAtoms.clear();
        for (IAtom atom : next)
        {
            selectedAtoms.add(atom);
            atom.setProperty(StandardGenerator.HIGHLIGHT_COLOR, SELECTION_COLOR);
        }
        repaint();
    }

//-----------------------------------------------------------------------------

    /**
     * Paint the panel
     * @param graphics the graphics object
     */

    @Override
    public void paint(Graphics graphics) 
    {
        // paint the background
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, getWidth(), getHeight());

        if (renderer == null || mol == null)
            return;

        if (keepFittedZoom && getWidth() > 0 && getHeight() > 0
                && (getWidth() != fittedWidth || getHeight() != fittedHeight))
        {
            zoomOutToFit();
            fittedWidth = getWidth();
            fittedHeight = getHeight();
        }

        renderer.setup(mol, new Rectangle(getWidth(), getHeight()));
        
        Graphics2D g2d = (Graphics2D) graphics;

        AffineTransform saveTransform = g2d.getTransform();
        at = new AffineTransform(saveTransform);
        at.concatenate(viewTransform());
        g2d.setTransform(at);
        renderer.paint(mol, new AWTDrawVisitor(g2d));
        g2d.setTransform(saveTransform);
    }

//-----------------------------------------------------------------------------

    /**
     * Zoom and pan in this panel's pixel coordinates. The same transform is
     * applied while painting and while deciding which atom a click hit.
     */
    private AffineTransform viewTransform()
    {
        AffineTransform view = new AffineTransform();
        view.translate(getWidth() / 2.0, getHeight() / 2.0);
        view.scale(zoom, zoom);
        view.translate(-getWidth() / 2.0, -getHeight() / 2.0);
        view.translate(translateX, translateY);
        return view;
    }
    
    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        keepFittedZoom = false;
        if (e.getWheelRotation() < 0) {
            zoom *= 1.1;
        }
        if (e.getWheelRotation() > 0) {
            zoom = Math.max(MIN_FIT_ZOOM, zoom / 1.1);
        }
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        pressPoint = e.getPoint();
        panning = false;
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
        if (pressPoint != null
                && pressPoint.distance(e.getPoint()) < CLICK_SLOP_PX)
        {
            return;
        }
        panning = true;
        keepFittedZoom = false;
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
 
        repaint();
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e)
    {
        boolean select = selectable && !panning;
        panning = false;
        pressPoint = null;
        if (select)
        {
            selectAtomAt(e.getPoint());
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Toggles the depicted atom under {@code point}. A listener is notified
     * when one is registered; the highlight is updated either way.
     */
    private void selectAtomAt(Point point)
    {
        IAtom atom = atomAt(point);
        if (atom == null || mol == null)
        {
            return;
        }
        boolean selected;
        if (selectedAtoms.remove(atom))
        {
            atom.removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
            selected = false;
        }
        else
        {
            selectedAtoms.add(atom);
            atom.setProperty(StandardGenerator.HIGHLIGHT_COLOR, SELECTION_COLOR);
            selected = true;
        }
        int index = mol.indexOf(atom);
        for (AtomClickListener listener : new ArrayList<AtomClickListener>(
                atomClickListeners))
        {
            listener.atomClicked(atom, index, selected);
        }
        repaint();
    }

//-----------------------------------------------------------------------------

    /**
     * @param point a point in this panel, in pixels.
     * @return the depicted atom whose centre is within
     * {@link #ATOM_PICK_RADIUS_PX} of {@code point}, choosing the nearest
     * when several qualify, or {@code null}.
     */
    IAtom atomAt(Point point)
    {
        if (!selectable || renderer == null || mol == null || point == null)
        {
            return null;
        }
        renderer.setup(mol, new Rectangle(getWidth(), getHeight()));
        IAtom closest = null;
        double closestDistance = ATOM_PICK_RADIUS_PX;
        for (IAtom atom : mol.atoms())
        {
            Point onPanel = depictedCenter(atom);
            if (onPanel == null)
            {
                continue;
            }
            double distance = onPanel.distance(point);
            if (distance <= closestDistance)
            {
                closest = atom;
                closestDistance = distance;
            }
        }
        return closest;
    }

//-----------------------------------------------------------------------------

    /**
     * @param atom an atom of the depicted container.
     * @return the atom centre in this panel, in pixels, after zoom and pan,
     * or {@code null} when the atom has no 2D coordinates.
     */
    Point depictedCenter(IAtom atom)
    {
        if (atom == null)
        {
            return null;
        }
        Point2d modelPoint = atom.getPoint2d();
        if (modelPoint == null || renderer == null)
        {
            return null;
        }
        Point2d rendered = renderer.toScreenCoordinates(modelPoint.x,
                modelPoint.y);
        Point2D onPanel = viewTransform().transform(
                new Point2D.Double(rendered.x, rendered.y), null);
        return new Point((int) Math.round(onPanel.getX()),
                (int) Math.round(onPanel.getY()));
    }

    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
    @Override
    public void mouseMoved(MouseEvent e) {}
    
//-----------------------------------------------------------------------------
    
    /**
     * Element colors from CDK, with attachment-point pseudoatoms in magenta.
     */
    private static class AttachmentPointAtomColorer implements IAtomColorer
    {
        private final IAtomColorer elementColors = new CDK2DAtomColors();
        private static final Color ATTACHMENT_POINT = new Color(0xFF00FF);

        @Override
        public Color getAtomColor(IAtom atom)
        {
            IAtom resolved = atom;
            if (atom instanceof AtomRef)
            {
                resolved = ((AtomRef) atom).deref();
            }
            if (resolved instanceof PseudoAtom)
            {
                return ATTACHMENT_POINT;
            }
            return elementColors.getAtomColor(atom);
        }
    }

//-----------------------------------------------------------------------------

}
