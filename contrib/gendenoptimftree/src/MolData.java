/*******************************************************************************
 *
 * This file may be distributed and/or modified under the terms of the
 * GNU General Public License version 3 as published by the Free Software
 * Foundation and appearing in the file LICENSE.GPL included in the
 * packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
 * WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 ******************************************************************************/


package gendenoptimftree;

import org.openscience.cdk.interfaces.IAtomContainer;

/**
 *
 * @author Vishwesh Venkatraman
 */
public class MolData implements Comparable<MolData>
{
    private String molID;
    private String imgFile;
    private String htmlFile;
    private double fitness;
    private String parent_X_molID;
    private String parent_Y_molID;
    private int generation;
    private IAtomContainer cmol;


    public MolData(String p_molID, String p_imgFile, String p_htmlFile,
            double p_fitness, int p_gen, IAtomContainer p_mol)
    {
        molID = p_molID;
        imgFile = p_imgFile;
        fitness = p_fitness;
        generation = p_gen;
        htmlFile = p_htmlFile;
        parent_X_molID = "";
        parent_Y_molID = "";
        cmol = p_mol;
    }

//------------------------------------------------------------------------------

    public String getHtmlFile()
    {
        return htmlFile;
    }

//------------------------------------------------------------------------------

    public void setHtmlFile(String p_htmlFile)
    {
        this.htmlFile = p_htmlFile;
    }

//------------------------------------------------------------------------------

    public String getMolID()
    {
        return molID;
    }

//------------------------------------------------------------------------------

    public void setMolID(String p_molID)
    {
        this.molID = p_molID;
    }

//------------------------------------------------------------------------------

    public String getImgFile()
    {
        return imgFile;
    }

//------------------------------------------------------------------------------

    public void setImgFile(String p_imgFile)
    {
        this.imgFile = p_imgFile;
    }

//------------------------------------------------------------------------------

    public double getFitness()
    {
        return fitness;
    }

//------------------------------------------------------------------------------

    public void setFitness(double p_fitness)
    {
        this.fitness = p_fitness;
    }

//------------------------------------------------------------------------------

    public String getParent_X_molID()
    {
        return parent_X_molID;
    }

//------------------------------------------------------------------------------

    public void setParent_X_molID(String p_parent_X_molID)
    {
        this.parent_X_molID = p_parent_X_molID;
    }

//------------------------------------------------------------------------------

    public String getParent_Y_molID()
    {
        return parent_Y_molID;
    }

//------------------------------------------------------------------------------

    public void setParent_Y_molID(String p_parent_Y_molID)
    {
        this.parent_Y_molID = p_parent_Y_molID;
    }

//------------------------------------------------------------------------------

    public int getGeneration()
    {
        return generation;
    }

//------------------------------------------------------------------------------

    public void setGeneration(int p_gen)
    {
        this.generation = p_gen;
    }

//------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(512);
        
        
        return sb.toString().trim();
    }

//------------------------------------------------------------------------------

    public IAtomContainer getMolecule() 
    {
        return cmol;
    }
    
//------------------------------------------------------------------------------    

    public void setMolecule(IAtomContainer p_cmol) 
    {
        this.cmol = p_cmol;
    }

//------------------------------------------------------------------------------    

    /**
     *
     * @param B
     * @return
     */
    
    @Override
    public int compareTo(MolData B)
    {
        if (this.generation > B.generation)
            return 1;
        else if (this.generation < B.generation)
            return -1;
        return 0;
    }

//------------------------------------------------------------------------------        

}
