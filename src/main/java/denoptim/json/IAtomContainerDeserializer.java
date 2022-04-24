/*
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.json;

import java.lang.reflect.Type;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;


/**
 * Deserialisation of collections of both light-weight atoms
 * and bonds into a CDK {@link IAtomContainer}.
 * 
 * @author Marco Foscato
 */

public class IAtomContainerDeserializer implements JsonDeserializer<IAtomContainer>
{
    
    private IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();

    @Override
    public IAtomContainer deserialize(JsonElement jsonEl, Type type,
            JsonDeserializationContext context) throws JsonParseException
    {
        JsonObject jo = jsonEl.getAsJsonObject();
        if (!jo.has(IAtomContainerSerializer.ATOMSKEY) 
                || !jo.has(IAtomContainerSerializer.BONDSKEY))
            return null;
        
        IAtomContainer iac = builder.newAtomContainer();
        
        JsonArray atomArr = jo.get(
                IAtomContainerSerializer.ATOMSKEY).getAsJsonArray();
        for (JsonElement e : atomArr)
        {
            LWAtom lwAtm = context.deserialize(e,LWAtom.class);
            iac.addAtom(lwAtm.toIAtom());
        }
        
        JsonArray bondArr = jo.get(
                IAtomContainerSerializer.BONDSKEY).getAsJsonArray();
        for (JsonElement e : bondArr)
        {
            LWBond lwBnd = context.deserialize(e,LWBond.class);
            iac.addBond(lwBnd.atomIds[0], lwBnd.atomIds[1], lwBnd.bo);
        }
        
        return iac;
    }
}
