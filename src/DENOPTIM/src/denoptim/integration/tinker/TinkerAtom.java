/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
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

package denoptim.integration.tinker;

import java.io.Serializable;

/**
 * Based on the code from ffx.kenai.com Michael J. Schnieders
 * @author Vishwesh Venkatraman
 */
public class TinkerAtom implements Serializable, Cloneable
{
    /*
     * tinker assigned atom type
     */
    private int ffAtomType;
    
    /*
     * tinker assigned atom string
     */
    private String atomStr;
    
    /*
     * the original 3d coordinates from which the IC representation is calculated
     */
    private double[] xyz; 
    
    /*
     * position of the atom in the list
     */
    private int xyzIndex;
    
    /*
     * store for distance, bond angle and the dihedral or third angle
     */
    private double[] distAngles;
    
    
    /*
     * atom neighbours, the first 3 members are the neighbours, the 4 position 
     * is reserved for the chiral definition (0/1/-1)
     */
    private int[] atmNb;
    
    /*
     * the vertex id with which the IC fragment is associated with
     */
    private int vtxId;

//------------------------------------------------------------------------------

    public TinkerAtom()
    {

    }

//------------------------------------------------------------------------------

    public TinkerAtom(int xyzIndex, String atomStr, int ffAtomType,
                      double[] xyz, int[] atmNb, double[] distAngles)
    {
        this.atomStr = atomStr;
        this.ffAtomType = ffAtomType;
        this.xyzIndex = xyzIndex;
        this.atmNb = new int[4];
        this.distAngles = new double[3];
        this.xyz = new double[3];
        
        for (int i=0; i<3; i++)
        {
            this.xyz[i] = xyz[i];
            this.distAngles[i] = distAngles[i];
        }
        System.arraycopy(atmNb, 0, this.atmNb, 0, 4);
    }
    
    
//------------------------------------------------------------------------------

    public void setAtomNeighbours(int[] m_nb)
    {
        this.atmNb = m_nb;
    }

//------------------------------------------------------------------------------

    public int[] getAtomNeighbours()
    {
        return this.atmNb;
    }

//------------------------------------------------------------------------------

    public void setDistAngle(double[] m_ang)
    {
        this.distAngles = m_ang;
    }    

//------------------------------------------------------------------------------

    public double[] getDistAngle()
    {
        return this.distAngles;
    }

//------------------------------------------------------------------------------

    /**
      * Add a vector to the Atom's current position vector
      *
      * @param d Vector to add to the current position
      */
     public void moveTo(double[] d)
     {
         xyz[0] = d[0];
         xyz[1] = d[1];
         xyz[2] = d[2];
     }

//------------------------------------------------------------------------------

    /**
     * <p>moveTo</p>
     *
     * @param a a double.
     * @param b a double.
     * @param c a double.
     */
    public void moveTo(double a, double b, double c)
    {
         xyz[0] = a;
         xyz[1] = b;
         xyz[2] = c;
     }

//------------------------------------------------------------------------------

     /**
      * <p>setXYZ</p>
      *
      * @param m_xyz an array of double.
      */

    public void setXYZ(double m_xyz[])
    {
         this.xyz = m_xyz;
    }

//------------------------------------------------------------------------------

    /**
     * <p>getXYZ</p>
     *
     * @return an array of double.
     */
    public double[] getXYZ()
    {
         return xyz;
    }

//------------------------------------------------------------------------------

    /**
     * <p>getXYZ</p>
     *
     * @param x an array of double.
     */
     public void getXYZ(double[] x)
     {
         x[0] = xyz[0];
         x[1] = xyz[1];
         x[2] = xyz[2];
     }

//------------------------------------------------------------------------------

     /**
      * Gets the XYZ Index
      *
      * @return XYZ Index
      */
     public int getXYZIndex()
     {
        return xyzIndex;
     }

//------------------------------------------------------------------------------

     public void setXYZIndex(int m_idx)
     {
         this.xyzIndex = m_idx;
     }


//------------------------------------------------------------------------------

     public String getAtomString()
     {
         return this.atomStr;
     }

//------------------------------------------------------------------------------

     public void setAtomString(String m_str)
     {
          this.atomStr = m_str;
     }

//------------------------------------------------------------------------------

     public int getAtomType()
     {
         return this.ffAtomType;
     }

//------------------------------------------------------------------------------
     
     public void setAtomType(int m_ffatomtype)
     {
          this.ffAtomType = m_ffatomtype;
     }

//------------------------------------------------------------------------------
     
     public void setVertexId(int m_vid)
     {
         this.vtxId = m_vid;
     }     
     
//------------------------------------------------------------------------------     
     
     public int getVertexId()
     {
         return this.vtxId;
     }


//------------------------------------------------------------------------------

    /**
     * Evaluates whether this TinkerAtom's line refers to a torsion (chiral flag
     * is 0) or to an inproper torsion (chiral flag is +/-1). For the
     * position to be defined by a proper torsion, all 3 neighbouring atoms has
     * to be defined (!= 0).
     * @return <code>true</code> if this TinkerAtom makes use of a proper 
     * torsion
     */
    public boolean usesProperTorsion()
    {
	if ((this.atmNb[0] != 0) && (this.atmNb[1] != 0) &&
              (this.atmNb[2] != 0) && (this.atmNb[3] == 0))
	{
	    return true;
	}

	return false;
    }

//------------------------------------------------------------------------------

    @Override
    public String toString()
    {
	String s = "TinkerAtom (ff: " + ffAtomType +
				" str: "+ atomStr +
				" xyz: [" + xyz[0] + "; " + xyz[1] +
				"; " + xyz[2] + "] " +
				" id: " + xyzIndex +
				" distAng: [" + distAngles[0] +
				"; " + distAngles[1] + "; " + distAngles[2] +
				"] " +
				" nb: [" + atmNb[0] + "; " + atmNb[1] +
				"; " + atmNb[2] + "; " + atmNb[3] + "] " +
				" vtx: " +vtxId + ")";
	return s;
    }
     
//------------------------------------------------------------------------------          

}


