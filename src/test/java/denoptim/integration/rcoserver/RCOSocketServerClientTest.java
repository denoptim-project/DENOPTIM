package denoptim.integration.rcoserver;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import denoptim.molecularmodeling.zmatrix.ZMatrix;
import denoptim.molecularmodeling.zmatrix.ZMatrixAtom;

/**
 * Unit test for {@link RCOSocketServerClient}
 * 
 * @author Marco Foscato
 */


public class RCOSocketServerClientTest
{
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetZMatrixAsJsonArray() 
    {
        ZMatrix zMatrix = new ZMatrix();
        
        ZMatrixAtom atom0 = new ZMatrixAtom(
                0, "H", "H.1", null, null, null, 
                null, null, null, null);
        
        ZMatrixAtom atom1 = new ZMatrixAtom(
                1, "O", "O.2", atom0, null, null,
                1.5, null, null, null);
        
        ZMatrixAtom atom2 = new ZMatrixAtom(
                2, "C", "C.3", atom1, atom0, null,
                1.4, 109.5, null, null);
        
        ZMatrixAtom atom3 = new ZMatrixAtom(
                3, "H", "H.1", atom2, atom1, atom0,
                1.4, 109.5, 60.0, 0);
        
        ZMatrixAtom atom4 = new ZMatrixAtom(
                4, "H", "H.1", atom2, atom1, atom3,
                1.4, 109.5, 109.0, 1);

        ZMatrixAtom atom5 = new ZMatrixAtom(
                5, "H", "H.1", atom4, atom2, atom1,
                1.4, 109.5, 0.0, 0);

        zMatrix.addAtom(atom0);
        zMatrix.addAtom(atom1);
        zMatrix.addAtom(atom2);
        zMatrix.addAtom(atom3);
        zMatrix.addAtom(atom4);
        zMatrix.addAtom(atom5);

        zMatrix.addBond(atom0, atom1);
        zMatrix.addBond(atom1, atom2);
        zMatrix.addBond(atom2, atom3);
        zMatrix.addBond(atom2, atom4);
        zMatrix.addBond(atom2, atom5);

        
        JsonArray result = RCOSocketServerClient.getZMatrixAsJsonArray(zMatrix);
       
        assertEquals(6, result.size());

        Map<String,Integer> expectedNotNullFrom = new HashMap<String,Integer>();
        expectedNotNullFrom.put("id", 0);
        expectedNotNullFrom.put("element", 0);
        expectedNotNullFrom.put("bond_ref", 1);
        expectedNotNullFrom.put("bond_length", 1);
        expectedNotNullFrom.put("angle_ref", 2);
        expectedNotNullFrom.put("angle", 2);
        expectedNotNullFrom.put("dihedral_ref", 3);
        expectedNotNullFrom.put("dihedral", 3);
        expectedNotNullFrom.put("chirality", 3);

        int idx = 0;
        for (JsonElement je : result.asList())
        {
            JsonObject jo = je.getAsJsonObject();
            for (String key : expectedNotNullFrom.keySet())
            {
                if (idx >= expectedNotNullFrom.get(key))
                {
                    assertTrue(jo.has(key), "Missing key '" + key + "' in " + jo);
                    assertFalse(jo.get(key).isJsonNull(), "JsonNull for '" + key + "': " + jo);
                } else {
                    assertFalse(jo.has(key), "Unexpected key '" + key + "' in " + jo);
                }
            }
            idx++;
        }
    }

//------------------------------------------------------------------------------
    
}
