/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.utils;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;

import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import org.openscience.cdk.Bond;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IAtomContainer;


/**
 * Toolbox for reorganizing the list of atoms of an <code>IAtomContainer</code>
 *
 * @author Marco Foscato
 */
public class AtomOrganizer
{
//------------------------------------------------------------------------------    
    
//indexes of atoms bearing  AP

    //Flags per atoms
    private ArrayList<ArrayList<Boolean>> flags;
    //Recursion flag for reporting info
    private int recNum;
    //Alternative atom order
    private Map<Integer, ArrayList<Integer>> atomOrders;
    private Map<Integer, ArrayList<Integer>> oldToNewOrder;
    //layers
    private Map<Integer, Map<Integer, Set<Integer>>> layers;
    //layer index
    private int lidx;
    //Type of reordering scheme:
    // 1 => branch-oriented approach
    // 2 => layer-oriented approach
    private int scheme = 1;

//------------------------------------------------------------------------------
    public AtomOrganizer()
    {
        flags = new ArrayList<>();
        atomOrders = new HashMap<>();
        oldToNewOrder = new HashMap<>();
        recNum = 1;
        lidx = 0;
        layers = new HashMap<>();
    }

//------------------------------------------------------------------------------
    public void setScheme(int m_scheme)
    {
        this.scheme = m_scheme;
    }

//------------------------------------------------------------------------------
    public Map<Integer, ArrayList<Integer>> getOldToNewOrder()
    {
        return oldToNewOrder;
    }

//------------------------------------------------------------------------------
    /**
     * Reorder the atoms in
     * <code>mol<code/> starting with atom
     * <code>seed<code/>
     *
     * @param seed index of the atom being the seed of the new order
     * @param mol molecule with the original order of atoms
     * @return an
     * <code>IAtomContainer<code/> with the new order of atoms
     */
    public IAtomContainer reorderStartingFrom(int seed, IAtomContainer mol)
            throws DENOPTIMException {
        AtomContainer newMol = new AtomContainer();
        int flag = getFreeAtomsFlag(mol);

        //find new order of atoms using starting from 'seed'
        if (!atomOrders.containsKey(seed)) {
            //create all empty vectors of proper size
            ArrayList<Integer> reorderedAtms = new ArrayList<>(mol.getAtomCount());
            atomOrders.put(seed, reorderedAtms);
            ArrayList<Integer> pointer = new ArrayList<>(mol.getAtomCount());
            oldToNewOrder.put(seed, pointer);
            for (int i = 0; i < mol.getAtomCount(); i++) {
                oldToNewOrder.get(seed).add(i, -1);
            }
            recNum = 1;

            //add atoms to new other
            switch (scheme) {
                case 1:
                    branchOrienterReordering(seed, flag, mol);
                    break;
                case 2:
                    layerOrientedReordering(seed, flag, mol);
                    break;
                default:
                    String str = "ERROR! Atom ordering: scheme not recognized";
                    throw new DENOPTIMException(str);
            }

            //delete temp vector of flags
            deleteAtomsFlag(flag);
        }

        //Now build the reordered system: regenerate atoms and bonds
        //Regenerate Atoms
        for (int i = 0; i < mol.getAtomCount(); i++) {
            try {
                IAtom atm = (IAtom) mol.getAtom(atomOrders.get(seed).get(i)).clone();
                newMol.addAtom(atm);
            } catch (CloneNotSupportedException cnse) {
                throw new DENOPTIMException(cnse);
            }
        }

        //Regenerate Bonds
        for (int i = 0; i < mol.getBondCount(); i++) {
            try {
                IBond oldBnd = mol.getBond(i);
                if (oldBnd.getAtomCount() != 2) {
                    System.err.println("\nMulticenter bond!!!");
                    String msg = "Multicenter bond not supported yet.";
                    throw new DENOPTIMException(msg);
                    //TODO in case of multicenter
                    //        IAtoms[] = ... ...
                }
                int atm1 = mol.getAtomNumber(oldBnd.getAtom(0));
                int atm2 = mol.getAtomNumber(oldBnd.getAtom(1));
                int newatm1 = oldToNewOrder.get(seed).get(atm1) - 1;
                int newatm2 = oldToNewOrder.get(seed).get(atm2) - 1;
                IBond.Order order = oldBnd.getOrder();
                IBond.Stereo stereo = oldBnd.getStereo();
                IBond bnd = new Bond(newMol.getAtom(newatm1), newMol.getAtom(newatm2), order, stereo);
                newMol.addBond(bnd);
            } catch (Throwable t) {
                String msg = "ERROR in making new BOND";
                throw new DENOPTIMException(msg);
            }
        }

        //The reordered system is the output of this method
        return newMol;
    }

//------------------------------------------------------------------------------
    /**
     * Reorders atoms according to the branch-oriented scheme. Atoms connected
     * to the seed (starting point) are listed according to the priority rules
     * defined in
     * <code>ConnectedLigandComparator<code/>.
     *
     * @param seed index of the first atom. The starting point.
     * @param flag index of the flag uset to specify that an atom was processed.
     * @param mol molecular object.
     */
    private void branchOrienterReordering(int seed, int flag, IAtomContainer mol)
    {
        int ap = seed;

        //add the seed to the new list
        processAtom(seed, ap, flag);

        //add all other atoms
        exploreMolecule(seed, flag, mol, ap);
    }

//------------------------------------------------------------------------------
    /**
     * Method meant for recursion on all atoms connected to a starting point
     */
    private void exploreMolecule(int seed, int flag, IAtomContainer mol, int ap)
    {
        // Get the list of neighbours (connected atoms)
        List<IAtom> neighbourAtoms = mol.getConnectedAtomsList(mol.getAtom(seed));

        // Throw away atoms already done
        List<IAtom> purgedList = new ArrayList<>();
        for (IAtom connectedAtom : neighbourAtoms)
        {
            int atmidx = mol.getAtomNumber(connectedAtom);
            if (!flags.get(flag).get(atmidx))
            {
                purgedList.add(connectedAtom);
            }
        }

        // In case nothing else to do
        if (purgedList.isEmpty())
        {
            return;
        }

        // Reorder according to priority
        if (purgedList.size() > 1)
        {
            purgedList = prioritizeAtomList(purgedList, mol);
        }

        // Add all the atoms connected
        for (IAtom connectedAtom : purgedList) {
            // Identify atom
            int atmidx = mol.getAtomNumber(connectedAtom);

            // Use flag to avoid giving a bite on your own tail!
            if (flags.get(flag).get(atmidx)) {
                continue;
            }

            // Add to lists
            processAtom(atmidx, ap, flag);
        }

        // Move to the next level
        for (IAtom connectedAtom : purgedList) {
            //get atom
            //identify atom
            int atmidx = mol.getAtomNumber(connectedAtom);

            // move to the next shell of atoms
            if (mol.getConnectedAtomsCount(connectedAtom) > 1) {
                recNum++;
                exploreMolecule(atmidx, flag, mol, ap);
                recNum--;
            }
        }
    }

//------------------------------------------------------------------------------
    /**
     * Reorders atoms according to the layer-oriented scheme. Atoms connected to
     * the seed (starting point) are listed according to the distance from the
     * seed atom (distance = number of bonds). All atoms directly connected to
     * the seed, belong to the 1st layer; all atoms connected to the atoms of
     * the 1st layer belong to the 2nd layer and so on. Within each layer atoms
     * are listed according to priority rules that are defined in the 
     * <code>ConnectedLigandComparator<code/>.
     *
     * @param seed index of the first atom. The starting point.
     * @param flag index of the flag uset to specify that an atom was processed.
     * @param mol molecular object.
     */
    private void layerOrientedReordering(int seed, int flag, IAtomContainer mol)
    {
        // Set flag for detecting layers
        int lyflg = getFreeAtomsFlag(mol);

        // Set seed as center of all layers
        lidx = 0;
        int ap = seed;
        addAtomToLayer(seed, ap, lidx, lyflg);

        // Loop over layers to find new layers
        boolean goon = true;
        Map<Integer, Set<Integer>> layersOfAP = layers.get(ap);
        while (goon)
        {
            // Get atoms in this layer
            Set<Integer> atmInLyr = layersOfAP.get(lidx);

            // get connected atoms for all atoms in this layer checking flag
            for (int atmIdx : atmInLyr)
            {
                List<IAtom> neighbourAtoms = mol.getConnectedAtomsList(mol.getAtom(atmIdx));
                // Set layer lidx+1 to all connected atoms
                for (IAtom ngbAtm : neighbourAtoms)
                {
                    int ngbAtmIdx = mol.getAtomNumber(ngbAtm);
                    if (!flags.get(lyflg).get(ngbAtmIdx))
                    {
                        int layerOfNgbAtm = lidx + 1;
                        addAtomToLayer(ngbAtmIdx, ap, layerOfNgbAtm, lyflg);
                    }
                }
            }

            // Update goon-condition
            goon = layersOfAP.containsKey(lidx + 1);

            // Update layer identifier
            lidx++;
        }

        // Explore layers and reorder atoms
        int numberOfLayers = layersOfAP.keySet().size();
        for (int i = 0; i < numberOfLayers; i++)
        {
            // Get atoms in this layer
            Set<Integer> idxInLyr = layersOfAP.get(i);
            List<IAtom> atmInLyr = new ArrayList<>();
            for (Integer idx : idxInLyr)
            {
                atmInLyr.add(mol.getAtom(idx));
            }

            // Get prioritized list of layer's members
            atmInLyr = prioritizeAtomList(atmInLyr, mol);

            // Report atoms to ordered list
            for (IAtom orderedAtm : atmInLyr) {
                // Identify atom
                int atmidx = mol.getAtomNumber(orderedAtm);

                // Add to ordered lists
                processAtom(atmidx, ap, flag);
            }
        }

        //delete temp vector of flags
        deleteAtomsFlag(lyflg);

    }

//------------------------------------------------------------------------------
    /**
     * Set the layer membership of an antom
     *
     * @param atmidx index of the atom.
     * @param ap index of the seed atom (starting point).
     * @param layer index of the leyer which
     * <code>atmidx<code/> belongs to.
     * @param doneFlag index of the flag reminding which atom is already done.
     */
    private void addAtomToLayer(int atmidx, int ap, int layer, int doneFlag)
    {
        // Check existence of AP-related layers
        if (!layers.containsKey(ap))
        {
            Map<Integer, Set<Integer>> layersForAP = new HashMap<>();
            layers.put(ap, layersForAP);
        }

        // check existence of this layer in the list of layers
        if (!layers.get(ap).containsKey(layer))
        {
            Set<Integer> atmInLayer = new HashSet<>();
            layers.get(ap).put(layer, atmInLayer);
        }

        // Add atom to layer
        layers.get(ap).get(layer).add(atmidx);

        //Set the doneFlag to true ( = 'DONE')
        flags.get(doneFlag).set(atmidx, true);
    }

//------------------------------------------------------------------------------
    /**
     * Perform all the operation to report an atom in the new order
     *
     * @param atmidx the index of the atom to be reported
     * @param ap the index of the seed atom (starting point) identifying the
     * reordered list of atoms
     * @param doneFlag index of the flag reminding which atom is already done.
     */
    private void processAtom(int atmidx, int ap, int doneFlag)
    {
        //Set the doneFlag to true ( = 'DONE')
        flags.get(doneFlag).set(atmidx, true);

        //Add OLD atom index to the NEW ORDER pointer
        atomOrders.get(ap).add(atmidx);

        //get new index of this atom
        int newIdx = atomOrders.get(ap).size();

        //Add NEW atom index to the OLD ORDER pointer
        oldToNewOrder.get(ap).set(atmidx, newIdx);
    }

//------------------------------------------------------------------------------
    /**
     * Change the order of atoms in a list according to the priority rules
     * defined in the comparator
     * <code>ConnectedLigandComparator<code/>.
     *
     * @param inList initial list of atoms.
     * @param mol molecular object which the atoms belongs to.
     * @return the ordered list of atoms.
     */
    private List<IAtom> prioritizeAtomList(List<IAtom> inList, IAtomContainer mol)
    {
        List<IAtom> outList = new ArrayList<>();

        //make a list of connected ligands
        List<ConnectedLigand> ligList = new ArrayList<>();
        for (IAtom seed : inList)
        {
            ConnectedLigand lig = new ConnectedLigand(seed, 
                                             mol.getConnectedAtomsCount(seed));
            ligList.add(lig);
        }

        ligList.sort(new ConnectedLigandComparator());

        for (ConnectedLigand connectedLigand : ligList) {
            IAtom a = connectedLigand.getAtom();
            outList.add(a);
        }
        return outList;
    }

//------------------------------------------------------------------------------
    /**
     * Generates a vector of boolean flags. The size of the vector equals the
     * number of atoms in the <code>IAtomContainer<code/>.
     * All flags are initialized to <code>false<code/>.
     *
     * @param mol molecular object for which the vector of flags has to be
     * generated.
     * @return an integer index referring to the vector of flags.
     */
    private int getFreeAtomsFlag(IAtomContainer mol)
    {
        int freeFlag = -1;
        //create a vector with false entries
        int atoms = mol.getAtomCount();
        ArrayList<Boolean> flg = new ArrayList<>();
        for (int i = 0; i < atoms; i++)
        {
            flg.add(false);
        }

        //add the vector to the list of flags
        flags.add(flg);
        freeFlag = flags.indexOf(flg);

        return freeFlag;
    }

//------------------------------------------------------------------------------
    /**
     * Destroy a vector of flags.
     *
     * @param flagID the index of the vector to be destroyed
     */
    private void deleteAtomsFlag(int flagID)
    {
        flags.remove(flagID);
    }

//------------------------------------------------------------------------------
}
