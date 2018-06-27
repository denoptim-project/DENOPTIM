package molecule;

import java.io.Serializable;
import java.io.File;


/**
 *
 * @author Vishwesh Venkatraman
 */
public class DENOPTIMMolecule implements Comparable<DENOPTIMMolecule>, Serializable
{
    /*
     * molecule representation as a graph of vertices and edges
     */
    private DENOPTIMGraph molGraph;
    
    /*
     * INCHI representation of the generated molecule, we will stick to just the key
     */
    private String molUID;
	
    /*
     * smiles representation
     */
    private String molSmiles;
    
    /*
     * fitness of the molecule
     */
    private double molFitness;
    
    /*
     * name of the optimized molecule filename
     */
    private String molFile;

    /*
     * File containing the graphical depiction of the molecule
     */
    private String imgFile;
    
    /*
     * Any comments on the molecule
     */
    private String commments;
    
    
//------------------------------------------------------------------------------

    public DENOPTIMMolecule()
    {
        molUID = "UNDEFINED";
        molSmiles = "UNDEFINED";
        molFitness = 0;
    }
    
//------------------------------------------------------------------------------
    
    public DENOPTIMMolecule(DENOPTIMGraph m_molGraph, String m_molUID, 
                            String m_molSmiles, double m_molFitness)
    {
        molGraph = m_molGraph;
        molUID = m_molUID;
        molSmiles = m_molSmiles;
        molFitness = m_molFitness;
   }

//------------------------------------------------------------------------------
	
    public DENOPTIMMolecule(String m_molFile, DENOPTIMGraph m_molGraph, 
                            String m_molUID, String m_molSmiles, 
                            double m_molFitness)
    {
        molGraph = m_molGraph;
        molUID = m_molUID;
        molSmiles = m_molSmiles;
        molFitness = m_molFitness;
        molFile = m_molFile;
    }
    
//------------------------------------------------------------------------------
    
    public void setComments(String m_str)
    {
        commments = m_str;
    }
    
//------------------------------------------------------------------------------
    
    public String getComments()
    {
        return commments;
    }    
	
//------------------------------------------------------------------------------

    public void setMoleculeFile(String m_molFile)
    {
        molFile = m_molFile;
    }	
    
//------------------------------------------------------------------------------

    public void setImageFile(String m_imgFile)
    {
        imgFile = m_imgFile;
    }

//------------------------------------------------------------------------------

    public void setMoleculeGraph(DENOPTIMGraph m_molGraph)
    {
        molGraph = m_molGraph;
    }

//------------------------------------------------------------------------------

    public void setMoleculeUID(String m_UID)
    {
        molUID = m_UID;
    }

//------------------------------------------------------------------------------

    public void setMoleculeSmiles(String m_molSmiles)
    {
        molSmiles = m_molSmiles;
    }
   
//------------------------------------------------------------------------------

    public void setMoleculeFitness(double m_fitness)
    {
        molFitness = m_fitness;
    }
    
    
//------------------------------------------------------------------------------

    public String getMoleculeUID()
    {
        return molUID;
    }

//------------------------------------------------------------------------------

    public String getMoleculeSmiles()
    {
        return molSmiles;
    }
    
//------------------------------------------------------------------------------

    public String getMoleculeFile()
    {
        return molFile;
    }
    
//------------------------------------------------------------------------------

    public String getImageFile()
    {
        return imgFile;
    }

//------------------------------------------------------------------------------

    public DENOPTIMGraph getMoleculeGraph()
    {
        return molGraph;
    }

//------------------------------------------------------------------------------

    public double getMoleculeFitness()
    {
        return molFitness;
    }
    
//------------------------------------------------------------------------------

    // The compareTo method compares the receiving object with the specified 
    // object and returns a negative integer, 0, or a positive integer depending 
    // on whether the receiving object is less than, equal to, or greater than 
    // the specified object.

    @Override
    public int compareTo(DENOPTIMMolecule B)
    {
        if (this.molFitness > B.molFitness)
            return 1;
        else if (this.molFitness < B.molFitness)
            return -1;
        return 0;
    }

//------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(16);
        String mname = new File(molFile).getName();
        if (mname != null)
            sb.append(String.format("%-20s", mname));
        
        sb.append(String.format("%-20s", this.molGraph.getGraphId()));
        sb.append(String.format("%-30s", molUID));
        sb.append(String.format("%12.3f", molFitness));

        return sb.toString();
    }

//------------------------------------------------------------------------------    
    
    public void cleanup()
    {
        if (molGraph != null)
            molGraph.cleanup();
    }
    
//------------------------------------------------------------------------------        
    
}