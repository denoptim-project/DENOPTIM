package denoptimcg;

/**
 *
 * @author Vishwesh Venkatraman
 */
public class APAtomHolder
{
    private int atm_num;
    private int ap_idx;
    private double[] dirVec;

//------------------------------------------------------------------------------

    public APAtomHolder()
    {

    }

//------------------------------------------------------------------------------

    public APAtomHolder(int m_atm_num, int m_ap_idx, double[] m_vec)
    {
        atm_num = m_atm_num;
        ap_idx = m_ap_idx;
        dirVec = m_vec;
    }

//------------------------------------------------------------------------------

    protected double[] getDirVec()
    {
        return dirVec;
    }

//------------------------------------------------------------------------------

    protected int getAtomNumber()
    {
        return atm_num;
    }

//------------------------------------------------------------------------------

    protected int getAPIndex()
    {
        return ap_idx;
    }

//------------------------------------------------------------------------------

}
