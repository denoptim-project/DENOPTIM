package denoptim.fitness;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.openscience.cdk.qsar.DescriptorEngine;
import org.openscience.cdk.qsar.DescriptorSpecification;
import org.openscience.cdk.qsar.IDescriptor;

import denoptim.exception.DENOPTIMException;
import denoptim.logging.DENOPTIMLogger;

public class DescriptorUtils
{
	
	private static final String FS = System.getProperty("file.separator");
	private static final String NL = System.getProperty("line.separator");
	
//------------------------------------------------------------------------------
	
	public static List<String> getClassNamesToCDKDescriptors()
	{
		// Search for CDK jar file
		String qsarDescPkg = "org.openscience.cdk.qsar.descriptors";
		qsarDescPkg = qsarDescPkg.replaceAll("\\.", FS);
		String cdkJarPathName  = "";
		String classPath = System.getProperty("java.class.path");
		String[] jarsAndDirs = classPath.split(File.pathSeparator);
		for (int i = 0; i < jarsAndDirs.length; i++)
		{
			String pathName = jarsAndDirs[i];
			if (!pathName.endsWith(".jar"))
			{
				continue;
			}
			JarFile jarFile;
			try {
                jarFile = new JarFile(pathName);
                Enumeration<JarEntry> enumeration = jarFile.entries();
                while (enumeration.hasMoreElements()) {
                    JarEntry jarEntry = enumeration.nextElement();
                    if (jarEntry.toString().contains(qsarDescPkg))
                    {
                    	cdkJarPathName = pathName;
                    	break;
                    }
                }
			} catch (IOException e) {
				e.printStackTrace();
            }
			
			if (cdkJarPathName.equals(""))
			{
				File parentFolder = new File(System.getProperty("user.dir"));
				if (pathName.endsWith("DenoptimGA.jar") 
						|| pathName.endsWith("FragSpaceExplorer.jar")
						|| pathName.endsWith("GUI.jar"))
				{
					File myClassPath = new File(pathName);
					if (pathName.contains(FS))
					{
						parentFolder = new File(myClassPath.getParent());
					}
				}
				File libFolder = new File(parentFolder.getAbsolutePath() 
						+ FS + "lib");
				FileFilter fileFilter = new WildcardFileFilter("cdk*.jar");
				File[] cands = libFolder.listFiles(fileFilter);
				if (cands.length == 1)
				{
				    cdkJarPathName = cands[0].getAbsolutePath();
				}
			} else {
				break;
			}
		}
		
		if (cdkJarPathName.equals(""))
		{
			//TODO change! this is very bad! 
			cdkJarPathName = "lib/cdk-1.4.19.jar";
			String msg = "WARNING: Could not locate the CDK classes! "
					+ " Trying to enforce use of " + cdkJarPathName;
			Exception e = new Exception(msg);
			e.printStackTrace();
		}
		
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
