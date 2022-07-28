package denoptim.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openscience.cdk.Isotope;
import org.openscience.cdk.formula.MolecularFormula;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;

/**
 * Utilities for manipulating molecular formulas
 * @author Marco Foscato
 *
 */
public class FormulaUtils
{
    
//------------------------------------------------------------------------------
    
    /**
     * Reads a molecular formula written as "H2 O" or "C6 H12 O6". Stochiometric
     * coefficients can be non-integer
     * @param formula
     */
    public static Map<String,Double> parseFormula(String formula)
    {
        Map<String,Double> elementsMap = new HashMap<String,Double>();
        String[] words = formula.split("\\s+");
        for (int i=0; i<words.length; i++)
        {
            String elSymbol = "";
            Double elCount = 0.0;
            int characterIdx = 0;
            while (characterIdx<words[i].length() 
                    && Character.isLetter(words[i].charAt(characterIdx)))
            {
                characterIdx++;
            }
            elSymbol = words[i].substring(0,characterIdx);
            if (characterIdx<words[i].length())
            {
                elCount = Double.parseDouble(words[i].substring(characterIdx));
            } else {
                elCount = 1.0;
            }
            elementsMap.put(elSymbol, elCount);
        }
        return elementsMap;
    }
 
//------------------------------------------------------------------------------
    
    /**
     * Reads a molecular formula formatted as from the Cambridge Structural 
     * Database and produces a profile of elemental analysis for possible 
     * chemical representations that may include all, some, or a minimum
     * number of all molecular objects in the system being represented. 
     * For example for the formula (H2 O)n,2n(Cl2) it may include one molecule 
     * of water or one of chlorine, or both, or one molecule of water and two
     * of chlorine, or two of water and four of chlorine, etc.
     * @param formula
     * @return a map of the elemental analysis for each element that may be 
     * present in the system. For each element, a list of possible values is
     * provided and, the n-th entry for one element is to be combined with the
     * n-th entry of all other elements.
     * @throws DENOPTIMException
     */
    public static Map<String, ArrayList<Double>> parseCSDFormula(String formula) 
            throws DENOPTIMException
    {
        //Parameter: maximum number of variable molecules
        int limSF = 6;
        
        //Element counts over the list of molecules in the formula
        ArrayList<Map<String,Integer>> allAtmCounts = 
                new ArrayList<Map<String,Integer>>();

        String[] mols = formula.split(",");

        //Deal with stoichiometric factors
        List<Double> stocFact = new ArrayList<Double>();
        List<Boolean> tuneStFact = new ArrayList<Boolean>();
        List<Boolean> tuneStFactToInt = new ArrayList<Boolean>();
        for (int i=0; i < mols.length; i++)
        {
            stocFact.add(i,1.0);
            tuneStFact.add(i,false);
            tuneStFactToInt.add(i,false);
            String locForm = mols[i].trim();
            boolean found = false;
            //Check for stoichiometric factor
            if (locForm.contains("("))
            {  
                //There is a stoichiometric factor
                if (locForm.lastIndexOf(")") == (locForm.length()-1))
                {  
                    // the Stoc. Fact. is at the beginning => nothing at the end!
                    String[] spt = locForm.split("[()]");
                    if (spt[0].contains("n"))
                    {  
                        //Tunable Stoic. Fact.
                        if (spt[0].lastIndexOf("n") == 0)
                        { 
                            //n-case 0: __n(formula)__
                            tuneStFact.set(i,true);
                            locForm = spt[1];
                            found = true;
                        } else if (spt[0].lastIndexOf("n") == 
                                (spt[0].length()-1))
                        {  
                            //n-case 1: __NUMn(formula)__
                            stocFact.set(i,Double.parseDouble(
                                    spt[0].substring(0,spt[0].length() -1)));
                            tuneStFact.set(i,true);
                            locForm = spt[1];
                            found = true;
                        }
                    } else if (spt[0].contains("x"))
                    {  
                        //Tunable Stoic. Fact. using x as variable
                        if (spt[0].lastIndexOf("x") == 0)
                        {  
                            //n-case 0: __x(formula)__
                            tuneStFact.set(i,true);
                            locForm = spt[1];
                            found = true;
                        } else if (spt[0].lastIndexOf("x") 
                                == (spt[0].length()-1))
                        {  
                            //n-case 1: __NUMx(formula)__
                            stocFact.set(i,Double.parseDouble(
                                    spt[0].substring(0,spt[0].length() -1)));
                            tuneStFact.set(i,true);
                            locForm = spt[1];
                            found = true;
                        }
                    } else {
                        stocFact.set(i,Double.parseDouble(spt[0]));
                        locForm = spt[1];
                        found = true;
                    }
                } else if (locForm.lastIndexOf("(") == 0)
                {  
                    //Stoic. Fact. at the end
                    String[] sptR = locForm.split("[()]");
                    locForm = sptR[1];
 
                    //n-case 2: __(formula)n__
                    if (sptR[2].length() == 1)
                    {
                        tuneStFact.set(i,true);
                        found = true;
                    } else if (sptR[2].lastIndexOf("x") 
                            == (sptR[0].length()-1)) {
                        //n-case 1: __(formula)NUMx__
                            stocFact.set(i,Double.parseDouble(
                                    sptR[2].substring(0,sptR[2].length() -1)));
                            tuneStFact.set(i,true);
                            locForm = sptR[1];
                            found = true;
                        }
    
                    }
 
                    if (!found)
                    {
                        throw new DENOPTIMException("Moleculer formula '" + 
                            formula + "' has a syntax that is not "
                                    + "recognized.");
                }

                //Deal with fractionary Stoich. Fact.
                double mod = stocFact.get(i) % 1.0;
                if (mod != 0.0)
                    tuneStFactToInt.set(i,true);
 
            } //end of stochiometric factor analysis
 
            //Identify elements to be counted
            Map<String,Integer> locCount = new HashMap<String,Integer>();
            String [] lmnts = locForm.split("\\s+");
            for (int l = 0; l < lmnts.length; l++)
            {
                //Get rid of the charge, if any
                if (lmnts[l].endsWith("+") || lmnts[l].endsWith("-"))
                    continue;
 
                //Count of a single element
                String elSymbol = "";
                int elCount = 0;
                int fd = 0;
                while (fd<lmnts[l].length() 
                        && Character.isLetter(lmnts[l].charAt(fd)))
                {
                    fd++;
                }
                elSymbol = lmnts[l].substring(0,fd);
                if (fd<lmnts[l].length())
                {
                    elCount = Integer.parseInt(lmnts[l].substring(fd));
                } else {
                    elCount = 1;
                }
 
                //add this element and its count to the map
                locCount.put(elSymbol,elCount);
            }
            //Move the atoms count to the general storage
            allAtmCounts.add(i,locCount);
 
        } //end of loop over mols
 
        //Find largest molecule (from formula weight)
        int largestMol = 0;
        double largestMass = 0.0;
        for (int i = 0; i < mols.length; i++)
        {
            MolecularFormula molForm = new MolecularFormula();
            for (String el : allAtmCounts.get(i).keySet())
            {
                IIsotope is = new Isotope(el);
                molForm.addIsotope(is,allAtmCounts.get(i).get(el));
            }
            double mass = MolecularFormulaManipulator.getMass(molForm);
            if (mass > largestMass)
            {
                largestMass = mass;
                largestMol = i;
            }
        }
   
        //Get all the elements 
        Set<String> allEl = new HashSet<String>();
        for (int i = 0; i < mols.length; i++)
            for (String el : allAtmCounts.get(i).keySet())
                allEl.add(el);

        //report counting for all SINGLE molecules
        Map<String,ArrayList<Double>> elemAnalFormula = 
                new HashMap<String,ArrayList<Double>>();
        for (int i = 0; i < mols.length; i++)
        {
            for (String el : allEl)
            {
                double num = 0.0;
                if (allAtmCounts.get(i).containsKey(el))
                {
                    num = allAtmCounts.get(i).get(el);
                }
                if (!elemAnalFormula.containsKey(el))
                    elemAnalFormula.put(el,new ArrayList<Double>(Arrays.asList(num)));
                else
                    elemAnalFormula.get(el).add(num);
            }
        }

        //report counting for all SINGLE unit [NUM(molecule)]
        for (int i = 0; i < mols.length; i++)
        {
            for (String el : allEl)
            {
                double num = 0.0;
                if (allAtmCounts.get(i).containsKey(el))
                {
                    double pf = 1.0;
                    if (tuneStFactToInt.get(i))
                        pf = pf * (1.0 / stocFact.get(i));
                        num = allAtmCounts.get(i).get(el) * pf * stocFact.get(i);
                }
                if (!elemAnalFormula.containsKey(el))
                    elemAnalFormula.put(el,new ArrayList<Double>(
                            Arrays.asList(num)));
                else
                    elemAnalFormula.get(el).add(num);
            }
        }
        
        //report counting for sums of the first n molecules up to all molecules
        if (mols.length > 1)
        {
            for (int n = 2; n <= mols.length; n++)
            {
                //check for tunable stoich. factors in all molecules 
                //(assuming ONLY ONE factor has to be tuned)
                boolean doTuning = true;
                for (int i = 0; i < mols.length; i++)
                {
                    if (!tuneStFact.get(i))
                    {
                        doTuning = false;
                    }
                }
                //Calculate number of atoms of per each element
                for (String el : allEl)
                {
                    double thisElCount = 0.0;
                    //sums of the first n molecules Ignoring stoichiometric factors
                    for (int i = 0; i < n; i++)
                    {
                        if (allAtmCounts.get(i).containsKey(el))
                        {
                            double pf = 1.0;
                            if (tuneStFactToInt.get(i))
                                pf = pf * (1.0 / stocFact.get(i));
                            thisElCount = thisElCount + allAtmCounts.get(i).get(el) * pf * stocFact.get(i);
                        }
                    }
                    elemAnalFormula.get(el).add(thisElCount);

                    //tune stoichiometric factors if required
                    if (doTuning)
                    {
                        //per each prefactor within the limits of the tuning procedure
                        for (int pf = 2; pf < limSF ; pf ++)
                        {
                            double pfd = pf;
                            double thisElCountTune = 0.0;
                            //sums of the first n molecules with stoichiometric factors
                            for (int i = 0; i < n; i++)
                            {
                                if (allAtmCounts.get(i).containsKey(el))
                                {   
                                    thisElCountTune = thisElCountTune + allAtmCounts.get(i).get(el) * pfd * stocFact.get(i);
                                }
                            }
                            elemAnalFormula.get(el).add(thisElCountTune);
                        }
                    }
                }
            }
        }
            
        return elemAnalFormula;
    }

//------------------------------------------------------------------------------
    
    /**
     * Compares the molecular formula formatted as from the Cambridge Structural 
     * Database (CSD) against the elemental analysis of the given atom container.
     * @param formula the molecular formula in CSD format. Since this format
     * can include one or more molecular items that may or may not be present
     * in the chemical representation of the atom container, and it
     * can include tunable stoichiometric factors (e.g., 2n(H2O),n(Cl2)), a 
     * number of alternative elemental analysis are generated from the formula.
     * If any of these alternatives matches the elemental analysis of the
     * container, then we return <code>true</code>.
     * @param mol the atom container on which to perform elemental analysis.
     * @return <code>true</code> is any match is found between the molecular 
     * formula and the atom container.
     */
    public static boolean compareFormulaAndElementalAnalysis(String formula,
            IAtomContainer mol) throws DENOPTIMException
    {
        return compareFormulaAndElementalAnalysis(formula, mol, null);
    }
//------------------------------------------------------------------------------
    
    /**
     * Compares the molecular formula formatted as from the Cambridge Structural 
     * Database (CSD) against the elemental analysis of the given atom container.
     * @param formula the molecular formula in CSD format. Since this format
     * can include one or more molecular items that may or may not be present
     * in the chemical representation of the atom container, and it
     * can include tunable stoichiometric factors (e.g., 2n(H2O),n(Cl2)), a 
     * number of alternative elemental analysis are generated from the formula.
     * If any of these alternatives matches the elemental analysis of the
     * container, then we return <code>true</code>.
     * @param mol the atom container on which to perform elemental analysis.
     * @param logger where to log messages.
     * @return <code>true</code> is any match is found between the molecular 
     * formula and the atom container.
     */
    public static boolean compareFormulaAndElementalAnalysis(String formula,
            IAtomContainer mol, Logger logger) throws DENOPTIMException
    {
        // Elemental analysis of molecular formula (with possible variations)
        Map<String,ArrayList<Double>> elemAnalFormula = 
                FormulaUtils.parseCSDFormula(formula);
        if (logger!=null && logger.getLevel() == Level.FINEST)
        {
            StringBuilder sb = new StringBuilder();
            for (String el : elemAnalFormula.keySet())
                sb.append(DenoptimIO.NL).append(el+" "+elemAnalFormula.get(el));
            logger.log(Level.FINEST,"Elemental analysis from formula: " 
                    + sb.toString());
        }
        
        // Elemental analysis on structure
        Map<String,Double> elemAnalMolInfo = getElementalanalysis(mol);
        if (logger!=null && logger.getLevel() == Level.FINEST)
        {
            StringBuilder sb = new StringBuilder();
            for (String el : elemAnalMolInfo.keySet())
                sb.append(DenoptimIO.NL).append(el+" "+elemAnalMolInfo.get(el));
            logger.log(Level.FINEST,"Elemental analysis from atom structure: " 
                    + sb.toString());
        }
        
        // Compare the two elemental analysis. 
        
        // First, get the number of candidate guesses from the molecule. 
        // Each guess is the
        // result of including/excluding one/more isolated molecules
        // or using a different tunable stochiometric factor.
        int numCandidates = 0;
        for (String el : elemAnalFormula.keySet())
        {
            numCandidates = elemAnalFormula.get(el).size();
            break; // the size is equal for every value.
        }
        
        boolean foundMatch = false;
        loopOverCandidate:
        for (int i=0; i<numCandidates; i++)
        {
            for (String el : elemAnalFormula.keySet())
            {
                if (elemAnalFormula.get(el).get(i)>0.01 
                        && !elemAnalMolInfo.containsKey(el))
                {
                    continue loopOverCandidate;
                }
                if (elemAnalMolInfo.containsKey(el))
                {
                    if (Math.abs(
                            elemAnalFormula.get(el).get(i) 
                            - elemAnalMolInfo.get(el))
                            > 0.1)
                    {
                        continue loopOverCandidate;
                    }
                } else {
                    if (Math.abs(elemAnalFormula.get(el).get(i) - 0.0)
                            > 0.1)
                    {
                        continue loopOverCandidate;
                    }
                }
            }
            foundMatch = true;
            break;
        }
        return foundMatch;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Threads Deuterium as a different element than Hydrogen.
     * @param mol the system to analyze.
     * @return a map with the amount of each element.
     */
    public static Map<String,Double> getElementalanalysis(IAtomContainer mol)
    {
        Map<String,Double> elemAnalMolInfo = new HashMap<String,Double>();
        for (IAtom atm : mol.atoms())
        {
            String elSymbol = atm.getSymbol();
            //Deal with deuterium symbol
            if (atm.getMassNumber()!=null && atm.getMassNumber() == 2)
                elSymbol = "D";
            if (elemAnalMolInfo.keySet().contains(elSymbol))
            {
                double num = elemAnalMolInfo.get(elSymbol) + 1.0;
                elemAnalMolInfo.put(elSymbol,num);
            } else {
                elemAnalMolInfo.put(elSymbol,1.0);
            }
        }
        return elemAnalMolInfo;
    }
    
//------------------------------------------------------------------------------

}
