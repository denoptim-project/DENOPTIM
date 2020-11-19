package denoptim.fitness;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.openscience.cdk.qsar.DescriptorEngine;
import org.openscience.cdk.qsar.DescriptorSpecification;
import org.openscience.cdk.qsar.IDescriptor;

import denoptim.exception.DENOPTIMException;
import denoptim.logging.DENOPTIMLogger;

public class DescriptorUtils
{
	
//------------------------------------------------------------------------------
	
	private static List<String> getClassNamesToCDKDescriptors()
	{
		File mainClassPath = new File(System.getProperty("java.class.path"));
		
		//TODO: check if cdk is in mainClassPath, if not then try to force
		// with finding it using  mainClassPath.getParent()
		
		//From Eclipse has CDK System.out.println("CLASSPATH: "+mainClassPath);
		
		String FS = System.getProperty("file.separator");
		String NL = System.getProperty("line.separator");
		
		// WARNING! Hard-coded pathname of CDK jar file!
		// TOTO: get this from classpath.
		
		String cdkJarPathName = mainClassPath.getParent() + FS + "lib" + FS 
				+ "cdk-1.4.19.jar";
		//TODO change! this is very bad! Get rid of it ASAP!
		cdkJarPathName = "/Users/marco/tools/DENOPTIM_master/build/lib/cdk-1.4.19.jar";
		
		List<String> classNames = 
				DescriptorEngine.getDescriptorClassNameByPackage(null, 
						new String[]{cdkJarPathName});
		return classNames;
	}
//------------------------------------------------------------------------------

	public static DescriptorEngine getCDKDescriptorEngine()
	{
		return new DescriptorEngine(getClassNamesToCDKDescriptors());
	}
	
//------------------------------------------------------------------------------

	/**
	 * Searches for descriptor implementations.
	 * @param requiredDescriptors list of descriptor short names that we want
	 * to obtain. All the rest will be ignored. This parameter can be null, in 
	 * which case we'll return all the descriptors.
	 * @return the list of descriptor information bundles.
	 * @throws DENOPTIMException
	 */
	public static List<DescriptorForFitness> findAllDescriptorImplementations(
			List<String> requiredDescriptors) throws DENOPTIMException
	{
		List<String> classNames = new ArrayList<String>();
		classNames.addAll(getClassNamesToCDKDescriptors());
		// We might want to add more... one day
		
		//We use the engine to get the instances of descriptors calculators
		DescriptorEngine engine = new DescriptorEngine(classNames);
		List<IDescriptor> iDescs =  engine.instantiateDescriptors(classNames);
        
		List<DescriptorForFitness> chosenOnes = 
				new ArrayList<DescriptorForFitness>();
		Set<String> chosenOnesShortNames = new TreeSet<String>();
		// NB: Descriptors names are supposed to be unique in CDK, but we check
		// for duplicates. This, in case of additional descriptors that do not
		// belong to CDK and are added to the list.
        Map<String,String> unq = new HashMap<String,String>();
		for (int i=0; i<classNames.size(); i++)
		{
			String className = classNames.get(i);
			String[] descrNames = iDescs.get(i).getDescriptorNames();
			String simpleName = iDescs.get(i).getClass().getSimpleName();
			if (descrNames != null)
			{
				for (int j=0; j<descrNames.length;j++)
				{
					String descName = descrNames[j];
					if (unq.containsKey(descName))
					{
						String msg = "Descriptor '" + descName + "' in part of " 
								+ simpleName
								+ " but its name was already used in "
								+ unq.get(descName);
						throw new DENOPTIMException(msg);
					}
					unq.put(descName,simpleName);
					boolean isChosen = false;
					if (requiredDescriptors == null)
					{
						isChosen = true;
					} else {
						for (String requiredDescName : requiredDescriptors)
						{
							if (descName.equals(requiredDescName))
							{
								isChosen = true;
								break;
							}
						}
					}
					if (isChosen)
					{
						chosenOnesShortNames.add(simpleName);
						IDescriptor impl = iDescs.get(i);
						DescriptorSpecification ds = impl.getSpecification();
						DescriptorForFitness d = new DescriptorForFitness(
								descName, className,impl, j,
								engine.getDictionaryType(ds),
								engine.getDictionaryClass(ds),
								engine.getDictionaryDefinition(ds),
								engine.getDictionaryTitle(ds));
						chosenOnes.add(d);
					}
				}
			}
		}

		/*
		String log = "Found " + classNames.size() + " descriptor classes."
				+ " Will use " + chosenOnes.size() + "/" + unq.size() 
				+ "descriptor values taken from " + chosenOnesShortNames;
		DENOPTIMLogger.appLogger.info(log);
		*/
		
		return chosenOnes;
	}
	
//------------------------------------------------------------------------------

}
