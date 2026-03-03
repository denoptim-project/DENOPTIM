package denoptim.molecularmodeling.zmatrix;

import denoptim.constants.DENOPTIMConstants;

/**
 * Representation of an atom in the ZMatrix.
 */
public class ZMatrixAtom
{
    private int id;
    private String symbol;
    private String type;
    private ZMatrixAtom bondRefAtom;
    private ZMatrixAtom angleRefAtom;
    private ZMatrixAtom angle2RefAtom;
    private Double bondLength;
    private Double angleValue;
    private Double angle2Value;
    private Integer chiralFlag;

//--------------------------------------------------------------------------
    
    /**
     * Constructor for ZMatrixAtom
     * @param id The id of the atom
     * @param symbol The symbol of the atom
     * @param type The type of the atom
     * @param bondRefAtom The bond reference atom
     * @param angleRefAtom The angle reference atom
     * @param angle2RefAtom The angle2 reference atom
     * @param bondLength The bond length
     * @param angleValue The angle value
     * @param angle2Value The angle2 value
     * @param chiralFlag The chiral flag
     */
    public ZMatrixAtom(int id, String symbol, String type, 
        ZMatrixAtom bondRefAtom, ZMatrixAtom angleRefAtom,
        ZMatrixAtom angle2RefAtom, Double bondLength, Double angleValue, 
        Double angle2Value, Integer chiralFlag)
    {
        this.id = id;
        this.symbol = symbol;
        this.type = type;
        this.bondRefAtom = bondRefAtom;
        this.angleRefAtom = angleRefAtom;
        this.angle2RefAtom = angle2RefAtom;
        this.bondLength = bondLength;   
        this.angleValue = angleValue;
        this.angle2Value = angle2Value;
        this.chiralFlag = chiralFlag;
    }
    
//--------------------------------------------------------------------------

    /**
     * Get the id of the atom
     * @return The id of the atom
     */
    public int getId()
    {
        return id;
    }

//--------------------------------------------------------------------------

    /**
     * Set the id of the atom
     * @param id The id of the atom to set
     */
    public void setId(int id)
    {
        this.id = id;
    }

//--------------------------------------------------------------------------
    
    /**
     * Get the symbol of the atom
     * @return The symbol of the atom
     */
    public String getSymbol()
    {
        return symbol;
    }

//--------------------------------------------------------------------------

    /**
     * Set the symbol of the atom
     * @param symbol The symbol of the atom to set
     */
    public void setSymbol(String symbol)
    {
        this.symbol = symbol;
    }

//--------------------------------------------------------------------------
    
    /**
     * Get the type of the atom
     * @return The type of the atom
     */
    public String getType()
    {
        return type;
    }
    
//--------------------------------------------------------------------------

    /**
     * Set the type of the atom
     * @param type The type of the atom to set
     */
    public void setType(String type)
    {
        this.type = type;
    }

//--------------------------------------------------------------------------
    
    /**
     * Get the bond reference atom
     * @return The bond reference atom or null if not set
     */
    public ZMatrixAtom getBondRefAtom()
    {
        return bondRefAtom;
    }

//--------------------------------------------------------------------------
    
    /**
     * Get the angle reference atom
     * @return The angle reference atom or null if not set
     */
    public ZMatrixAtom getAngleRefAtom()
    {
        return angleRefAtom;
    }

//--------------------------------------------------------------------------
    
    /**
     * Get the angle2 reference atom
     * @return The angle2 reference atom or null if not set
     */
    public ZMatrixAtom getAngle2RefAtom()
    {
        return angle2RefAtom;
    }

//--------------------------------------------------------------------------
    
    /**
     * Get the bond length
     * @return The bond length or null if not set
     */
    public Double getBondLength()
    {
        return bondLength;
    }

//--------------------------------------------------------------------------
    
    /**
     * Get the angle value
     * @return The angle value or null if not set
     */
    public Double getAngleValue()
    {
        return angleValue;
    }

//--------------------------------------------------------------------------
    
    /**
     * Get the angle2 value
     * @return The angle2 value or null if not set
     */
    public Double getAngle2Value()
    {
        return angle2Value;
    }

//--------------------------------------------------------------------------

    /**
     * Set the bond length
     * @param bondLength The bond length to set
     */
    public void setBondLength(Double bondLength)
    {
        this.bondLength = bondLength;
    }

//--------------------------------------------------------------------------
    
    /**
     * Set the angle value
     * @param angleValue The angle value to set
     */
    public void setAngleValue(Double angleValue)
    {
        this.angleValue = angleValue;
    }

//--------------------------------------------------------------------------
    
    /**
     * Set the angle2 value
     * @param angle2Value The angle2 value to set
     */
    public void setAngle2Value(Double angle2Value)
    {
        this.angle2Value = angle2Value;
    }

//--------------------------------------------------------------------------

    /**
     * Set the chiral flag
     * @param chiralFlag The chiral flag to set
     */
    public void setChiralFlag(Integer chiralFlag)
    {
        this.chiralFlag = chiralFlag;
    }

//--------------------------------------------------------------------------

    /**
     * Get the chiral flag
     * @return The chiral flag or null if not set
     */
    public Integer getChiralFlag()
    {
        return chiralFlag;
    }

//--------------------------------------------------------------------------

    /**
     * Check if the atom uses proper torsion
     * @return True if the atom uses proper torsion, false otherwise
     */
    public boolean usesProperTorsion()
    {
        return chiralFlag != null && chiralFlag == 0;
    }

//--------------------------------------------------------------------------

    /**
     * Package-private setter for bond reference atom (used for cloning).
     */
    void setBondRefAtom(ZMatrixAtom bondRefAtom)
    {
        this.bondRefAtom = bondRefAtom;
    }

//--------------------------------------------------------------------------

    /**
     * Package-private setter for angle reference atom (used for cloning).
     */
    void setAngleRefAtom(ZMatrixAtom angleRefAtom)
    {
        this.angleRefAtom = angleRefAtom;
    }

//--------------------------------------------------------------------------

    /**
     * Package-private setter for angle2 reference atom (used for cloning).
     */
    void setAngle2RefAtom(ZMatrixAtom angle2RefAtom)
    {
        this.angle2RefAtom = angle2RefAtom;
    }

//--------------------------------------------------------------------------

    /**
     * Get the string representation of the atom
     * @return The string representation of the atom
     */
//--------------------------------------------------------------------------

    @Override
    public boolean equals(Object o)
    {
        if (o == null)
            return false;
        
        if (o == this)
            return true;
       
        if (o.getClass() != getClass())
            return false;
        
        ZMatrixAtom other = (ZMatrixAtom) o;
        
        // Compare primitive and object fields
        if (this.id != other.id)
            return false;
        
        if (this.symbol == null ? other.symbol != null : !this.symbol.equals(other.symbol))
            return false;
        
        if (this.type == null ? other.type != null : !this.type.equals(other.type))
            return false;
        
        // Compare Double fields (handle null and use tolerance for comparison)
        // Both null -> equal, one null -> not equal, both not null -> compare with tolerance
        if (this.bondLength == null && other.bondLength == null)
        {
            // Both null, considered equal - continue
        }
        else if (this.bondLength == null || other.bondLength == null)
        {
            // One is null, the other is not - not equal
            return false;
        }
        else
        {
            // Both not null - compare with tolerance
            if (Math.abs(this.bondLength - other.bondLength) > 
                DENOPTIMConstants.FLOATCOMPARISONTOLERANCE)
                return false;
        }
        
        if (this.angleValue == null && other.angleValue == null)
        {
            // Both null, considered equal - continue
        }
        else if (this.angleValue == null || other.angleValue == null)
        {
            // One is null, the other is not - not equal
            return false;
        }
        else
        {
            // Both not null - compare with tolerance
            if (Math.abs(this.angleValue - other.angleValue) > 
                DENOPTIMConstants.FLOATCOMPARISONTOLERANCE)
                return false;
        }
        
        if (this.angle2Value == null && other.angle2Value == null)
        {
            // Both null, considered equal - continue
        }
        else if (this.angle2Value == null || other.angle2Value == null)
        {
            // One is null, the other is not - not equal
            return false;
        }
        else
        {
            // Both not null - compare with tolerance
            if (Math.abs(this.angle2Value - other.angle2Value) > 
                DENOPTIMConstants.FLOATCOMPARISONTOLERANCE)
                return false;
        }
        
        // Compare Integer field (handle null)
        if (this.chiralFlag == null ? other.chiralFlag != null 
                : !this.chiralFlag.equals(other.chiralFlag))
            return false;
        
        // Compare reference atoms by ID to avoid circular reference issues
        int thisBondRefId = (this.bondRefAtom != null) ? this.bondRefAtom.getId() : -1;
        int otherBondRefId = (other.bondRefAtom != null) ? other.bondRefAtom.getId() : -1;
        if (thisBondRefId != otherBondRefId)
            return false;
        
        int thisAngleRefId = (this.angleRefAtom != null) ? this.angleRefAtom.getId() : -1;
        int otherAngleRefId = (other.angleRefAtom != null) ? other.angleRefAtom.getId() : -1;
        if (thisAngleRefId != otherAngleRefId)
            return false;
        
        int thisAngle2RefId = (this.angle2RefAtom != null) ? this.angle2RefAtom.getId() : -1;
        int otherAngle2RefId = (other.angle2RefAtom != null) ? other.angle2RefAtom.getId() : -1;
        if (thisAngle2RefId != otherAngle2RefId)
            return false;
        
        return true;
    }

//--------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        int result = 17;
        result = 31 * result + id;
        result = 31 * result + (symbol != null ? symbol.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (bondRefAtom != null ? bondRefAtom.getId() : 0);
        result = 31 * result + (angleRefAtom != null ? angleRefAtom.getId() : 0);
        result = 31 * result + (angle2RefAtom != null ? angle2RefAtom.getId() : 0);
        result = 31 * result + (bondLength != null ? bondLength.hashCode() : 0);
        result = 31 * result + (angleValue != null ? angleValue.hashCode() : 0);
        result = 31 * result + (angle2Value != null ? angle2Value.hashCode() : 0);
        result = 31 * result + (chiralFlag != null ? chiralFlag.hashCode() : 0);
        return result;
    }

//--------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return id + " " + symbol + " " + type + " " 
            + (bondRefAtom != null ? bondRefAtom.getId() : "null") + " " 
            + (angleRefAtom != null ? angleRefAtom.getId() : "null") + " " 
            + (angle2RefAtom != null ? angle2RefAtom.getId() : "null") + " " 
            + (bondLength != null ? bondLength : "null") + " " 
            + (angleValue != null ? angleValue : "null") + " " 
            + (angle2Value != null ? angle2Value : "null") + " " 
            + (chiralFlag != null ? chiralFlag : "null");
    }

//--------------------------------------------------------------------------
}

