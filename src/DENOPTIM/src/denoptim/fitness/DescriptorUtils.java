package denoptim.fitness;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.openscience.cdk.qsar.DescriptorEngine;
import org.openscience.cdk.qsar.IDescriptor;

import denoptim.exception.DENOPTIMException;
import denoptim.logging.DENOPTIMLogger;

public class DescriptorUtils
{

//------------------------------------------------------------------------------

	public static List<DescriptorForFitness> findAllDescriptorImplementations(
			List<String> requiredDescriptors) throws DENOPTIMException
	{
		File mainClassPath = new File(System.getProperty("java.class.path"));
		String FS = System.getProperty("file.separator");
		String NL = System.getProperty("line.separator");
		
		// WARNING! Hard-coded pathname of CDK jar file!
		// TOTO: get this from classpath.
		
		String cdkJarPathName = mainClassPath.getParent() + FS + "lib" + FS 
				+ "cdk-1.4.19.jar";
		
		List<String> classNames = 
				DescriptorEngine.getDescriptorClassNameByPackage(null, 
						new String[]{cdkJarPathName});
		
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
			if (descrNames !=null)
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
					for (String requiredDescName : requiredDescriptors)
					{
						if (descName.equals(requiredDescName))
						{
							chosenOnesShortNames.add(simpleName);
							DescriptorForFitness d = new DescriptorForFitness(
									descName, className, iDescs.get(i), j);
							chosenOnes.add(d);
							break;
						}
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
