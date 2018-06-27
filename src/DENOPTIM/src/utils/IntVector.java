package utils;

/**
 * a class for manipulating an array of <code>int</code> values
 * @author Vishwesh Venkatraman
 */
public class IntVector 
{ 
    private int N;         // length of the vector
    private int[] data;       // array of vector's components
	
//------------------------------------------------------------------------------

    // create the zero vector of length N
    public IntVector(int N) 
    {
        this.N = N;
        this.data = new int[N];
    }
	
//------------------------------------------------------------------------------

    // create a vector from an array
    public IntVector(int[] data)
    {
        N = data.length;

        // defensive copy so that client can't alter our copy of data[]
        this.data = new int[N];
        System.arraycopy(data, 0, this.data, 0, N);
    }

//------------------------------------------------------------------------------

    // return the length of the vector
    public int length() 
    {
        return N;
    }
    
//------------------------------------------------------------------------------        
    protected void setArrayLength(int l) 
    {
         int[] newArray = new int[l];
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

    public int[] getData()
    {
        return data;
    }
	
//------------------------------------------------------------------------------	

    // return a string representation of the vector
    @Override
    public String toString() 
    {
        String s = "(";
        for (int i = 0; i < N; i++) 
        {
            s += data[i];
            if (i < N-1) 
                s+= ", ";
        }
        return s + ")";
    }
    
//------------------------------------------------------------------------------

    public void setValue(int i, int value)    
    {
        if (i >= this.data.length) 
        {
            setArrayLength(i + 1);
        }
        this.data[i] = value;
    }    

//------------------------------------------------------------------------------
    
    // return the corresponding value at the index
    public int getValue(int i) 
    {
        return data[i];
    }
	
//------------------------------------------------------------------------------
    
    public int[] getArrayCopy() 
    {
         int[] aCopy = new int[this.data.length];
         System.arraycopy(this.data, 0, aCopy, 0, this.data.length);
         return aCopy;
     }
    
//------------------------------------------------------------------------------        
}
