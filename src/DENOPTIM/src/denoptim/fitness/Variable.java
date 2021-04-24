package denoptim.fitness;

import java.util.ArrayList;

public class Variable
{

    private String varName;
    private String descName;
    protected ArrayList<String> smarts;
    protected String[] params;
    
//------------------------------------------------------------------------------
    
    public Variable(String varName, String descName)
    {
        this.varName = varName;
        this.descName = descName;
    }
 
//------------------------------------------------------------------------------
    
    public String getName()
    {
        return varName;
    }
    
//------------------------------------------------------------------------------
    
    public String getDescriptorName()
    {
        return descName;
    }

//------------------------------------------------------------------------------
 
    public void setSMARTS(ArrayList<String> smarts)
    {
        this.smarts = smarts;
    }
    
//------------------------------------------------------------------------------
    
    public void setDescriptorParameters(String[] params)
    {
        this.params = params;
    }
    
//------------------------------------------------------------------------------
    
    public String toString()
    {
        String s = "Variable [varName:"+varName+", descName:"+descName
                +", smarts:"+smarts+", pararams:";
        if (params == null)
        {
            s = s + "null";
        } else {
            s = s + "[";
            for (int i=0; i<(params.length-1); i++)
                s = s + params[i] + ", ";
            s = s + params[params.length-1] + "]";
        }
        s = s + "]";
        return s;
    }
    
//------------------------------------------------------------------------------
    
}
