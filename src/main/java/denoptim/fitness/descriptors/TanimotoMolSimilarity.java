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

package denoptim.fitness.descriptors;

import java.lang.reflect.Constructor;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.IBitFingerprint;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.fingerprint.ShortestPathFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.qsar.AbstractMolecularDescriptor;
import org.openscience.cdk.qsar.DescriptorSpecification;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.DoubleResultType;
import org.openscience.cdk.qsar.result.IDescriptorResult;
import org.openscience.cdk.similarity.Tanimoto;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import denoptim.fitness.IDenoptimDescriptor;


/**
 * Calculates the molecular similarity against a target compound the
 * fingerprint of which is given as parameter.
 */

public class TanimotoMolSimilarity extends AbstractMolecularDescriptor 
implements IMolecularDescriptor, IDenoptimDescriptor
{
    //private static ILoggingTool logger = LoggingToolFactory.createLoggingTool(
    //        TanimotoMolSimilarity.class);
    private IBitFingerprint referenceFingerprint;
    private IFingerprinter fingerprinter;
    private String fingerprinterName = "none";
    private static final String[] PARAMNAMES = new String[] {
            "fingerprinterImplementation","referenceFingerprint"};

    private static final String[] NAMES  = {"TanimotoSimilarity"};
    
    /**
     * Utility for constructing CDK objects
     */
    private static IChemObjectBuilder cdkBuilder = 
            DefaultChemObjectBuilder.getInstance();

//------------------------------------------------------------------------------
    
    /**
     * Constructor for a TanimotoMolSimilarity object
     */
    public TanimotoMolSimilarity() {}

//------------------------------------------------------------------------------
      
    /**
     * Get the specification attribute of Tanimoto molecular similarity. Given 
     * the dependency on the fingerpringer and reference fingerprint, the
     * implementation identifier is made dependent on those two parameters. 
     * Consequently resetting the parameter with two new instances of the 
     * fingerpringer and reference fingerprint (even is effectively equal) will
     * result in two different DescriptorSpecification objects.
     * @return the specification of this descriptor.
     */
    @Override
    public DescriptorSpecification getSpecification()
    {
        String paramID = ""; 
        if (fingerprinter!=null && referenceFingerprint!=null)
        {
            paramID = "" + fingerprinterName + referenceFingerprint.hashCode();
        }
        return new DescriptorSpecification("Denoptim source code", 
                this.getClass().getName(), paramID, "DENOPTIM project");
    }

//------------------------------------------------------------------------------
    
    /**
     * Gets the parameterNames attribute of the TanimotoMolSimilarity object.
     * @return the parameterNames value
     */
    @Override
    public String[] getParameterNames() {
        return PARAMNAMES;
    }

//------------------------------------------------------------------------------
    
    /** {@inheritDoc} */
    @Override
    public Object getParameterType(String name)
    {
        if (name.equals(PARAMNAMES[1]))
        {
            return IBitFingerprint.class;
        } else if (name.equals(PARAMNAMES[0])) {
            return "";
        } else {
            throw new IllegalArgumentException("No parameter for name: "+name);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Set the parameters attribute of TanimotoMolSimilarity object.
     * The descriptor takes two parameters: the class name of the fingerprinter 
     * used to generate the fingerprints (e.g., <code>org.openscience.cdk.fingerprint.PubchemFingerprinter</code>), 
     * and the fingerprint against which we want to calculate similarity.
     * @param params the array of parameters
     */
    @Override
    public void setParameters(Object[] params) throws CDKException
    {
        if (params.length != 2)
        {
            throw new IllegalArgumentException("TanimotoMolSimilarity only "
                    + "expects two parameter");
        }
        if (!(params[1] instanceof IBitFingerprint))
        {
            throw new IllegalArgumentException("Parameter does not implemet "
                    + "IBitFingerprint.");
        }
        if (!(params[0] instanceof String))
        {
            throw new IllegalArgumentException("Parameter is not String (" 
                    + params[0].getClass().getName() + ").");
        }

        fingerprinterName = params[0].toString();
        fingerprinter = makeIFingerprinter(fingerprinterName);
        referenceFingerprint = (IBitFingerprint) params[1];
    }

//------------------------------------------------------------------------------
 
    /*
     * ASSUMPTION: implementation from package org.openscience.cdk.fingerprint
     */
    public static IFingerprinter makeIFingerprinter(String classShortName) 
            throws CDKException 
    {
        IFingerprinter fp = null;
        try
        {
            Class<?> cl = Class.forName("org.openscience.cdk.fingerprint." 
                    + classShortName);
            for (Constructor<?> constructor : cl.getConstructors()) 
            {
                Class<?>[] params = constructor.getParameterTypes();
                if (params.length == 0) 
                {
                    fp = (IFingerprinter) constructor.newInstance();
                } else if (params[0].equals(IChemObjectBuilder.class))
                {
                    //NB potential source of ambiguity on the builder class
                    fp = (IFingerprinter) constructor.newInstance(cdkBuilder);
                }
            }
        } catch (Throwable t)
        {
            
            throw new CDKException("Could not make new instance of '" 
                    + classShortName + "'.", t);
        }
        if (fp == null)
        {
            throw new CDKException("Could not make new instance of '" 
                    + classShortName + "'. No suitable constructor found.");
        }
        return fp;
    }
    
//------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public Object[] getParameters()
    {
        Object[] params = new Object[2];
        params[1] = referenceFingerprint;
        params[0] = fingerprinterName;
        return params;
    }

//------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String[] getDescriptorNames()
    {
        return NAMES;
    }

//------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public DescriptorValue calculate(IAtomContainer mol)
    {
        if (fingerprinter instanceof ShortestPathFingerprinter)
        {
            try
            {
                AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
            } catch (CDKException e1)
            {
                throw new IllegalStateException("Could not assign atom types "
                        + "to calculate fingerprint of input molecule.",
                        e1);
            }
        }
        
        DoubleResult result;
        if (referenceFingerprint==null)
        {
            throw new IllegalStateException("Reference fingerprint not set. "
                    + "Cannot calculate Tanimoto similarity.");
        }
        if (fingerprinter==null)
        {
            throw new IllegalStateException("Fingerprinter not set. "
                    + "Cannot calculate Tanimoto similarity.");
        }
        
        try
        {
            result = new DoubleResult(Tanimoto.calculate(referenceFingerprint, 
                    fingerprinter.getBitFingerprint(mol)));
        } catch (IllegalArgumentException e)
        {
            e.printStackTrace();
            result = new DoubleResult(Double.NaN);
        } catch (CDKException e)
        {
            e.printStackTrace();
            result = new DoubleResult(Double.NaN);
        }
        
        return new DescriptorValue(getSpecification(),
                getParameterNames(),
                getParameters(),
                result,
                getDescriptorNames());
    }

//------------------------------------------------------------------------------
   
    /** {@inheritDoc} */
    @Override
    public IDescriptorResult getDescriptorResultType()
    {
        return new DoubleResultType();
    }

//------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getDictionaryTitle()
    {
        return "Tanimoto Molecular Similarity";
    }
    
//------------------------------------------------------------------------------
    
    /** {@inheritDoc} */
    @Override
    public String getDictionaryDefinition()
    {
        return "The Tanimoto Molecular Similarity is calculated between the "
                + "reference fingerprint given upon definition of the "
                + "descriptor (see parameters), and a molecule given as "
                + "argument when calculating the value of the descriptor. "
                + "Fingerprints are obtained from a new instance of"
                + "<code>IFingerprinter</code>, which is created according to "
                + "the parameter <code>" + PARAMNAMES[0] + "</code>, "
                + "and take the form of "
                + "<code>IFingerprinter.getBitFingerprint(mol)</code>";
    }

//------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String[] getDictionaryClass()
    {
        return new String[] {"molecular"};
    }

//------------------------------------------------------------------------------

}
