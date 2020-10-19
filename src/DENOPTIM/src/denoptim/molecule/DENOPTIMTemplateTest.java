package denoptim.molecule;

/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
 *   and Marco Foscato <marco.foscato@uib.no>
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.molecule.DENOPTIMFragment.BBType;
import denoptim.utils.GraphUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit test for DENOPTIMTemplate
 * 
 */

public class DENOPTIMTemplateTest
{
    private static DENOPTIMAttachmentPoint testAP = null;

    //TODO-V3: reactivate
    //@BeforeAll
    public static void SetUpClass() {
        testAP = new DENOPTIMAttachmentPoint(0, 1, 1);
    }
	
//------------------------------------------------------------------------------

/*
    @Test
    public void testSomething() throws Exception
    {

    }
*/
    
//------------------------------------------------------------------------------


    //TODO-V3: complete and reactivate
    //@Test
    public void testIfInteriorGraphDoesNotHaveVacantAPsEqualToExteriorAPsThrowIAE() {
        DENOPTIMTemplate template = new DENOPTIMTemplate(BBType.UNDEFINED);
        List<DENOPTIMAttachmentPoint> exteriorAPs = new ArrayList<>();
        exteriorAPs.add(testAP);
        template.setExteriorAPs(exteriorAPs);
        DENOPTIMGraph g = new DENOPTIMGraph();
        assertThrows(IllegalArgumentException.class,
                () -> template.setInteriorGraph(g)
        );
    }
}
