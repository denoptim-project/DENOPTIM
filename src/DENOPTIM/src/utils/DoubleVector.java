package utils;

import java.io.Serializable;

/**
 * a class for manipulating an array of <code>double</code> values
 * @author Vishwesh Venkatraman
 */
public class DoubleVector implements Serializable
{
    private int N;         // length of the vector
    private double[] data;       // array of vector's components
	
//------------------------------------------------------------------------------

    // create the zero vector of length N
    public DoubleVector(int N) 
    {
        this.N = N;
        this.data = new double[N];
    }
	
//------------------------------------------------------------------------------

    // create a vector from an array
    public DoubleVector(double[] data)
    {
        N = data.length;

        // defensive copy so that client can't alter our copy of data[]
        this.data = new double[N];
        System.arraycopy(data, 0, this.data, 0, N);
    }

//------------------------------------------------------------------------------

    // return the length of the vector
    public int length() 
    {
        return N;
    }
	
//------------------------------------------------------------------------------

    public double[] getData()
    {
        return data;
    }
    
//------------------------------------------------------------------------------    
    
    protected void setArrayLength(int l) 
    {
         double[] newArray = new double[l];         
         int numToCopy = this.data.length;
         if (numToCopy > l) 
         {
             numToCopy = l;
         }
         System.arraycopy(this.data, 0, newArray, 0, numToCopy);
         N = l;
         this.data = newArray;
     }
	
//------------------------------------------------------------------------------

    // return a string representation of the vector
    // public String toString() 
	// {
        // String s = "(";
        // for (int i = 0; i < N; i++) 
		// {
            // s += data[i];
            // if (i < N-1) s+= ", ";
        // }
        // return s + ")";
    // }

//------------------------------------------------------------------------------
    
	// return the corresponding value at the index
    public double getValue(int i)
    {
        return data[i];
    }
    
//------------------------------------------------------------------------------
    
    public void setValue(int i, double v) 
    {
         if (i >= this.data.length) 
         {
            setArrayLength(i + 1);
         }
         this.data[i] = v;
     }
    
//------------------------------------------------------------------------------    
    
    public double[] getArrayCopy() 
    {
         double[] aCopy = new double[this.data.length];
         System.arraycopy(this.data, 0, aCopy, 0, this.data.length);
         return aCopy;
     }    
	
//------------------------------------------------------------------------------
	
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<N; i++)
        {
            sb.append(data[i]).append(" ");
        }
        return sb.toString();
    }

//------------------------------------------------------------------------------    
}
