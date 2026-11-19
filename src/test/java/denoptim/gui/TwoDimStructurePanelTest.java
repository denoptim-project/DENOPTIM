/*
 *   DENOPTIM
 *   Copyright (C) 2026 Marco Foscato <marco.foscato@uib.no>
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.vecmath.Point2d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import denoptim.gui.TwoDimStructurePanel.AtomClickListener;

/**
 * Tests for picking atoms drawn by {@link TwoDimStructurePanel}.
 *
 * @author Marco Foscato
 */
public class TwoDimStructurePanelTest
{

//------------------------------------------------------------------------------

    @Test
    public void testClickSelectsDepictedAtom() throws Exception
    {
        IAtomContainer mol = new SmilesParser(SilentChemObjectBuilder.getInstance())
                .parseSmiles("CCO");
        TwoDimStructurePanel panel = new TwoDimStructurePanel(mol, true, true);
        paint(panel);
        IAtom target = panel.mol.getAtom(0);
        AtomicReference<IAtom> reported = new AtomicReference<IAtom>();
        AtomicReference<Integer> reportedIndex = new AtomicReference<Integer>();
        AtomicReference<Boolean> reportedSelected = new AtomicReference<Boolean>();
        panel.addAtomClickListener(new AtomClickListener()
        {
            @Override
            public void atomClicked(IAtom atom, int index, boolean selected)
            {
                reported.set(atom);
                reportedIndex.set(index);
                reportedSelected.set(selected);
            }
        });

        click(panel, centerOf(panel, target));

        assertSame(target, reported.get());
        assertEquals(0, reportedIndex.get().intValue());
        assertTrue(reportedSelected.get());
        assertEquals(1, panel.getSelectedAtoms().size());
        assertSame(target, panel.getSelectedAtoms().get(0));

        click(panel, centerOf(panel, target));
        assertFalse(reportedSelected.get());
        assertTrue(panel.getSelectedAtoms().isEmpty());
    }

//------------------------------------------------------------------------------

    @Test
    public void testClickAwayFromAtomsDoesNotSelect() throws Exception
    {
        IAtomContainer mol = new SmilesParser(SilentChemObjectBuilder.getInstance())
                .parseSmiles("CCO");
        TwoDimStructurePanel panel = new TwoDimStructurePanel(mol, true, true);
        paint(panel);

        assertNull(panel.atomAt(new Point(1, 1)));
        click(panel, new Point(1, 1));
        assertTrue(panel.getSelectedAtoms().isEmpty());
    }

//------------------------------------------------------------------------------

    @Test
    public void testNonSelectablePanelIgnoresClicks() throws Exception
    {
        IAtomContainer mol = new SmilesParser(SilentChemObjectBuilder.getInstance())
                .parseSmiles("CCO");
        TwoDimStructurePanel panel = new TwoDimStructurePanel(mol, true, false);
        paint(panel);
        assertFalse(panel.isSelectable());

        IAtom target = panel.mol.getAtom(0);
        click(panel, centerOf(panel, target));

        assertNull(panel.atomAt(centerOf(panel, target)));
        assertTrue(panel.getSelectedAtoms().isEmpty());
    }

//------------------------------------------------------------------------------

    @Test
    public void testClickIgnoresGraphicsTranslation() throws Exception
    {
        IAtomContainer mol = new SmilesParser(SilentChemObjectBuilder.getInstance())
                .parseSmiles("CCO");
        TwoDimStructurePanel panel = new TwoDimStructurePanel(mol, true, true);
        panel.setSize(500, 500);
        BufferedImage image = new BufferedImage(700, 700,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.translate(120, 80);
        panel.paint(graphics);
        graphics.dispose();

        IAtom target = panel.mol.getAtom(0);
        click(panel, centerOf(panel, target));
        assertSame(target, panel.getSelectedAtoms().get(0));
    }

//------------------------------------------------------------------------------

    @Test
    public void testSelectionFollowsAtomsIn3DView() throws Exception
    {
        IAtomContainer mol = new SmilesParser(SilentChemObjectBuilder.getInstance())
                .parseSmiles("CCO");
        IAtom atomIn3DView = mol.getAtom(0);
        mol.getAtom(0).setProperty(TwoDimStructurePanel.ATOM_IN_3D_VIEW,
                atomIn3DView);
        TwoDimStructurePanel panel = new TwoDimStructurePanel(mol, true, true);

        IAtom depicted = null;
        for (IAtom atom : panel.mol.atoms())
        {
            if (atom.getProperty(TwoDimStructurePanel.ATOM_IN_3D_VIEW)
                    == atomIn3DView)
            {
                depicted = atom;
            }
        }
        assertNotNull(depicted);

        List<IAtom> selected = new ArrayList<IAtom>();
        selected.add(atomIn3DView);
        panel.setSelectionFromAtomsIn3DView(selected);
        assertEquals(1, panel.getSelectedAtoms().size());
        assertSame(depicted, panel.getSelectedAtoms().get(0));

        panel.setSelectionFromAtomsIn3DView(new ArrayList<IAtom>());
        assertTrue(panel.getSelectedAtoms().isEmpty());
    }

//------------------------------------------------------------------------------

    @Test
    public void testClickFollowsZoom() throws Exception
    {
        IAtomContainer mol = new SmilesParser(SilentChemObjectBuilder.getInstance())
                .parseSmiles("CCO");
        TwoDimStructurePanel panel = new TwoDimStructurePanel(mol, true, true);
        paint(panel);

        IAtom target = farthestAtom(panel);
        Point unzoomed = centerOf(panel, target);
        for (int i = 0; i < 8; i++)
        {
            panel.mouseWheelMoved(new MouseWheelEvent(panel,
                    MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), 0,
                    0, 0, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1));
        }
        paint(panel);
        paint(panel);

        double zoom = Math.pow(1.1, 8);
        double centerX = panel.getWidth() / 2.0;
        double centerY = panel.getHeight() / 2.0;
        Point zoomed = new Point(
                (int) Math.round(centerX + zoom * (unzoomed.x - centerX)),
                (int) Math.round(centerY + zoom * (unzoomed.y - centerY)));

        assertSame(target, panel.atomAt(zoomed));
        assertTrue(unzoomed.distance(zoomed) > 20);
        assertNull(panel.atomAt(unzoomed));
    }

//------------------------------------------------------------------------------

    private static void paint(TwoDimStructurePanel panel)
    {
        panel.setSize(500, 500);
        BufferedImage image = new BufferedImage(500, 500,
                BufferedImage.TYPE_INT_RGB);
        panel.paint(image.getGraphics());
    }

//------------------------------------------------------------------------------

    private static IAtom farthestAtom(TwoDimStructurePanel panel)
    {
        double centerX = panel.getWidth() / 2.0;
        double centerY = panel.getHeight() / 2.0;
        IAtom farthest = null;
        double farthestDistance = -1;
        for (IAtom atom : panel.mol.atoms())
        {
            Point center = centerOf(panel, atom);
            double distance = center.distance(centerX, centerY);
            if (distance > farthestDistance)
            {
                farthest = atom;
                farthestDistance = distance;
            }
        }
        return farthest;
    }

//------------------------------------------------------------------------------

    private static Point centerOf(TwoDimStructurePanel panel, IAtom atom)
    {
        Point2d modelPoint = atom.getPoint2d();
        Point2d rendered = panel.renderer.toScreenCoordinates(modelPoint.x,
                modelPoint.y);
        return new Point((int) Math.round(rendered.x),
                (int) Math.round(rendered.y));
    }

//------------------------------------------------------------------------------

    private static void click(TwoDimStructurePanel panel, Point point)
    {
        MouseEvent pressed = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, point.x, point.y, 1, false,
                MouseEvent.BUTTON1);
        MouseEvent released = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(), 0, point.x, point.y, 1, false,
                MouseEvent.BUTTON1);
        panel.mousePressed(pressed);
        panel.mouseReleased(released);
    }

//------------------------------------------------------------------------------

}
