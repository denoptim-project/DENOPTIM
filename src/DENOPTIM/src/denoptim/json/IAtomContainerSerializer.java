package denoptim.json;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import denoptim.utils.DENOPTIMMoleculeUtils;


/**
 * Class to serialise CDK's {@link IAtomContainer} in a simplified manner.
 * The simplification implies that only the coarse definition of atoms 
 * and bonds is serialised. Therefore, you must not expect to find all 
 * attributes or properties of the full IAtomContainer.
 * The goal of this class is to make the most light-weight JSON 
 * representation of an {@link IAtomContainer}.
 * 
 * @author Marco Foscato
 */
public class IAtomContainerSerializer implements JsonSerializer<IAtomContainer>
{
    /**
     * String used to identify the list of atoms in json map.
     */
    protected static final String ATOMSKEY = "atoms";

    /**
     * String used to identify the list of bonds in json map.
     */
    protected static final String BONDSKEY = "bonds";

    @Override
    public JsonElement serialize(IAtomContainer iac, Type typeOfSrc,
            JsonSerializationContext context)
    {
        JsonObject jsonObject = new JsonObject();
        List<LWAtom> atoms = new ArrayList<LWAtom>();
        for (IAtom atm : iac.atoms())
        {
            atoms.add(new LWAtom(DENOPTIMMoleculeUtils.getSymbolOrLabel(atm),
                    DENOPTIMMoleculeUtils.getPoint3d(atm)));
        }
        List<LWBond> bonds = new ArrayList<LWBond>();
        for (IBond bnd : iac.bonds())
        {
            bonds.add(new LWBond(iac.indexOf(bnd.getAtom(0)), 
                    iac.indexOf(bnd.getAtom(1)), bnd.getOrder()));
        }
        
        jsonObject.add(ATOMSKEY, context.serialize(atoms));
        jsonObject.add(BONDSKEY, context.serialize(bonds));
        
        return jsonObject;
    }
}
